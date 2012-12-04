package fr.proline.admin.service.db

import java.io.File
import javax.persistence.EntityManager
import com.weiglewilczek.slf4s.Logging

import fr.proline.admin.service.db.setup.{DatabaseSetupConfig,ProlineSetupConfig}
import fr.proline.admin.helper.sql._
import fr.proline.core.dal.DatabaseManagement
import fr.proline.core.dal.UdsDb
import fr.proline.core.orm.uds.{Project => UdsProject}
import fr.proline.repository.ConnectionPrototype.{DatabaseProtocol => DbProtocols}
import setup.{SetupLcmsDB,SetupMsiDB}

/**
 * @author David Bouyssie
 *
 */
class CreateProjectDBs( dbManager: DatabaseManagement, config: ProlineSetupConfig, projectId: Int ) extends Logging {
  
  /*def run() {
    
    val udsEMF = dbManager.udsEMF
    val udsEM = udsEMF.createEntityManager()    
    
    // Retrieve the project => we assume it has been already created
    val query = udsEM.createQuery("Select prj from Project prj where prj.id = :id", classOf[UdsProject])
    query.setParameter("id", projectId)
    
    val udsProject = query.getSingleResult()
    
    // Check that there are no external DBs attached to this project
    val extDbs = udsProject.getExternalDatabases()
    if( extDbs != null && extDbs.size() > 0 )
      throw new Exception("project of id='%d' is already associated to external databases !".format(projectId))
    
    // Prepare MSIdb creation
    val msiDBConfig = this._prepareDBCreation( config.msiDBConfig )
    
    // Begin transaction
    udsEM.getTransaction().begin()
    
    // Store MSIdb connection settings
    val udsMsiDbSettings = msiDBConfig.toUdsExternalDb()
    udsEM.persist( udsMsiDbSettings )
    udsProject.addExternalDatabase(udsMsiDbSettings)
    udsEM.persist( udsProject )
    udsEM.getTransaction().commit()
    
    // Create MSI database
    new SetupMsiDB( dbManager, msiDBConfig, config.msiDBDefaults, projectId ).run()
    
    // Prepare LCMSdb creation
    val lcmsDBConfig = this._prepareDBCreation( config.lcmsDBConfig )
    
    udsEM.getTransaction().begin()
    
    // Store LCMSdb connection settings
    val udsLcmsDbSettings = lcmsDBConfig.toUdsExternalDb()
    udsEM.persist( udsLcmsDbSettings )
    udsProject.addExternalDatabase(udsLcmsDbSettings)
    udsEM.persist( udsProject )
    
    // Commit transaction
    udsEM.getTransaction().commit()
    
    // Create LCMS database
    new SetupLcmsDB( dbManager, lcmsDBConfig, projectId ).run()
    
    // Close entity manager
    udsEM.close()
    
  }*/
  
  def run() {
    
    // Retrieve UDSdb connection
    val udsDb = new UdsDb( dbManager.udsDBConnector )    
    val udsDbTx = udsDb.getOrCreateTransaction()
    
    // Check that there are no external DBs attached to this project
    val nbExtDbs = udsDbTx.selectInt( "SELECT count(*) FROM external_db, project_db_map " +
                                      "WHERE project_db_map.external_db_id = external_db.id " +
                                      "AND project_db_map.project_id = " + projectId )
    if( nbExtDbs > 0 )
      throw new Exception("project of id='%d' is already associated to external databases !".format(projectId))
    
    // Close connection to avoid any conflict
    udsDb.commitTransaction()
    udsDb.closeConnection()
    
    // Prepare MSIdb creation
    val msiDBConfig = this._prepareDBCreation( config.msiDBConfig )
    
    // Store MSIdb connection settings
    this._insertExtDb( udsDb, msiDBConfig.toUdsExternalDb )
    
    // Create MSI database
    new SetupMsiDB( dbManager, msiDBConfig, config.msiDBDefaults, projectId ).run()
    
    // Prepare LCMSdb creation
    val lcmsDBConfig = this._prepareDBCreation( config.lcmsDBConfig )
    
    // Store LCMSdb connection settings
    this._insertExtDb( udsDb, lcmsDBConfig.toUdsExternalDb )
    
    // Create LCMS database
    new SetupLcmsDB( dbManager, lcmsDBConfig, projectId ).run()
    
  }
  
  private def _insertExtDb( udsDb: UdsDb, extDb: fr.proline.core.orm.uds.ExternalDb ) {
    
    import net.noerd.prequel.SQLFormatterImplicits._
    import fr.proline.core.dal.SQLFormatterImplicits._
    import fr.proline.core.dal.{UdsDbExternalDbTable,UdsDbProjectDbMapTable}
    
    val extDbInsertQuery = UdsDbExternalDbTable.makeInsertQuery(
                             UdsDbExternalDbTable.getColumnsAsStrList().filter( _ != "id" )
                           )
    val projectDbMapInsertQuery = UdsDbProjectDbMapTable.makeInsertQuery()
    
    val udsDbTx = udsDb.getOrCreateTransaction()
    val extDbId = udsDbTx.executeBatch( extDbInsertQuery, true ) { stmt =>
      stmt.executeWith( extDb.getDbName,
                        extDb.getConnectionMode,
                        Option(extDb.getDbUser),
                        Option(extDb.getDbPassword),
                        Option(extDb.getHost),
                        Option(extDb.getPort.toInt),
                        extDb.getType,
                        extDb.getDbVersion(),
                        false,
                        Option.empty[String]
                       )
      /*stmt.executeWith( "D:/proline/data/test/projects/project_1/msi-db.sqlite",
                        "FILE",
                        Option.empty[String],
                        Option.empty[String],
                        Option.empty[String],
                        Option.empty[Int],
                        "msi",
                        "0.1",
                        false,
                        Option.empty[String]
                       )*/
      udsDb.extractGeneratedInt( stmt.wrapped )
    }
    
    // Link external db to the project
    udsDbTx.execute( projectDbMapInsertQuery, extDbId, projectId )
    
    // Commit the transaction
    udsDb.commitTransaction()
    udsDb.closeConnection()
    
  }

  private def _prepareDBCreation( dbConfig: DatabaseSetupConfig ): DatabaseSetupConfig = {
    
    config.msiDBConfig.dbConnPrototype.getProtocol match {
      case DbProtocols.FILE => {
        
        // Create projects directory if not exists
        val projectsDir = CreateProjectDBs.getProjectsDir( dbConfig.dbDirectory )
        if( projectsDir.exists == false ) projectsDir.mkdir()
        
        // Retrieve project directory
        val projectDir = CreateProjectDBs.getProjectDir( projectsDir, this.projectId )
        if( projectDir.exists == false ) projectDir.mkdir()
        
        // Update database config directory
        dbConfig.copy( dbDirectory = projectDir )
      }
      case DbProtocols.HOST => {
        
        val newDbConfig = dbConfig.copy()
        newDbConfig.dbName = dbConfig.dbName + "_project_" + this.projectId
        
        if( dbConfig.driverType == "postgresql" ) {
          val pgDbConnector = newDbConfig.dbConnPrototype.toConnector("postgres")
          createPgDatabase( pgDbConnector, newDbConfig.dbName, Some(this.logger) )
        } else {
          throw new Exception("NYI")
        }
        
        newDbConfig
      }
      case DbProtocols.MEMORY => {
        throw new Exception("NYI")
        dbConfig
      }
    }
    
    
  }
  
}

object CreateProjectDBs {
  
  def apply( projectId: Int ) {
    
    // Retrieve Proline configuration
    val prolineConf = SetupProline.parseProlineSetupConfig( SetupProline.appConf )
    
    // Instantiate a database manager
    val dbManager = new DatabaseManagement(prolineConf.udsDBConfig.toNewConnector)
    
    // Create databases
    new CreateProjectDBs( dbManager, prolineConf, projectId ).run()
    
    // Close the database manager
    dbManager.closeAll()
    
  }
  
  def getProjectsDir( dataDir: File ): File = {
    new File( dataDir.toString + "/projects" )
  }
  
  def getProjectDir( projectsDir: File, projectId: Int ): File = {    
    new File( projectsDir.toString + "/project_" + projectId )
  }
  
}