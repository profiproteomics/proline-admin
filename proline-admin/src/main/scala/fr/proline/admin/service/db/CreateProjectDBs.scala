package fr.proline.admin.service.db

import java.io.File
import javax.persistence.EntityManager
import com.weiglewilczek.slf4s.Logging
import fr.profi.jdbc.easy.EasyDBC
import fr.proline.admin.service.db.setup.{DatabaseSetupConfig,ProlineSetupConfig}
import fr.proline.admin.helper.sql._
import fr.proline.core.orm.uds.{Project => UdsProject}
import fr.proline.core.orm.util.DataStoreConnectorFactory
import fr.proline.repository.ConnectionMode
import setup.{SetupLcmsDB,SetupMsiDB}
import fr.proline.repository.DriverType

/**
 * @author David Bouyssie
 *
 */
class CreateProjectDBs( dbContext: ProlineDatabaseContext, config: ProlineSetupConfig, projectId: Int ) extends Logging {
  
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
    //val dbManager = dbContext.dbManager
    val udsDbContext = dbContext.udsDbContext
    val wasUdsDbConnectionOpened = udsDbContext.isConnectionOpened
    val udsEzDBC = udsDbContext.ezDBC
    
    // Check that there are no external DBs attached to this project
    val nbExtDbs = udsEzDBC.selectInt( "SELECT count(*) FROM external_db, project_db_map " +
                                       "WHERE project_db_map.external_db_id = external_db.id " +
                                       "AND project_db_map.project_id = " + projectId )
    if( nbExtDbs > 0 )
      throw new Exception("project of id='%d' is already associated to external databases !".format(projectId))
    
    // Close connection to avoid any conflict
    //udsEzDBC.commitTransaction()
    
    // Prepare MSIdb creation
    val msiDBConfig = this._prepareDBCreation( config.msiDBConfig )
    
    // Store MSIdb connection settings
    this._insertExtDb( udsEzDBC, msiDBConfig.toUdsExternalDb )
    
    // Prepare LCMSdb creation
    val lcmsDBConfig = this._prepareDBCreation( config.lcmsDBConfig )
    
    // Store LCMSdb connection settings
    this._insertExtDb( udsEzDBC, lcmsDBConfig.toUdsExternalDb )
    
    // Release UDSdb connection
    if( wasUdsDbConnectionOpened == false ) udsDbContext.closeConnection()
    
    // Create MSI database
    val msiDbContext = new DatabaseConnectionContext( msiDBConfig.connector )
    new SetupMsiDB( msiDbContext, msiDBConfig, config.msiDBDefaults ).run()
    msiDbContext.closeAll()
    
    // Create LCMS database
    val lcmsDbContext = new DatabaseConnectionContext( lcmsDBConfig.connector )
    new SetupLcmsDB( lcmsDbContext, lcmsDBConfig ).run()
    lcmsDbContext.closeAll()
    
  }
  
  private def _insertExtDb( udsEzDBC: EasyDBC, extDb: fr.proline.core.orm.uds.ExternalDb ) {
    
    import fr.profi.jdbc.easy._
    import fr.proline.core.dal.tables.uds.{UdsDbExternalDbTable,UdsDbProjectDbMapTable}
    
    val extDbInsertQuery = UdsDbExternalDbTable.mkInsertQuery( (c,colsList) =>
                             colsList.filter( _ != c.ID )
                           )
    val projectDbMapInsertQuery = UdsDbProjectDbMapTable.mkInsertQuery()
    
    udsEzDBC.beginTransaction()
    
    val extDbId = udsEzDBC.executePrepared( extDbInsertQuery, true ) { stmt =>
      stmt.executeWith( extDb.getDbName,
                        extDb.getConnectionMode.toString(),
                        Option(extDb.getDbUser),
                        Option(extDb.getDbPassword),
                        Option(extDb.getHost),
                        if( extDb.getPort != null) Some(extDb.getPort.toInt) else Option.empty[Int],
                        extDb.getType.toString(),
                        extDb.getDbVersion(),
                        false,
                        Option(extDb.getSerializedProperties)
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
      stmt.generatedInt
    }
    
    // Link external db to the project
    udsEzDBC.execute( projectDbMapInsertQuery, extDbId, projectId )
    
    // Commit the transaction
    udsEzDBC.commitTransaction()
    
  }

  private def _prepareDBCreation( dbConfig: DatabaseSetupConfig ): DatabaseSetupConfig = {
   
   dbConfig.connectionMode match {
      case ConnectionMode.FILE => {
        
        // Create projects directory if not exists
        val projectsDir = CreateProjectDBs.getProjectsDir( dbConfig.dbDirectory )
        if( projectsDir.exists == false ) projectsDir.mkdir()
        
        // Retrieve project directory
        val projectDir = CreateProjectDBs.getProjectDir( projectsDir, this.projectId )
        if( projectDir.exists == false ) projectDir.mkdir()
        
        // Update database config directory
        dbConfig.copy( dbDirectory = projectDir )
      }
      case ConnectionMode.HOST => {
        
        val newDbConfig = dbConfig.copy()
        newDbConfig.dbName = dbConfig.dbName + "_project_" + this.projectId
        
        if( dbConfig.driverType == DriverType.POSTGRESQL ) {
          //val pgDbConnector = newDbConfig.toNewConnector()
          //createPgDatabase( pgDbConnector, newDbConfig.dbName, Some(this.logger) )
        } else {
          throw new Exception("NYI")
        }
        
        newDbConfig
      }
      case ConnectionMode.MEMORY => {
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
    val dsConnectorFactory = DataStoreConnectorFactory.getInstance()
    if( dsConnectorFactory.isInitialized == false ) dsConnectorFactory.initialize(prolineConf.udsDBConfig.toNewConnector)
    
    // Instantiate a database context
    val dbContext = new ProlineDatabaseContext( dsConnectorFactory )
    
    // Create databases
    new CreateProjectDBs( dbContext, prolineConf, projectId ).run()
    
    // Close the database manager
    //dbManager.closeAll()
    
  }
  
  def getProjectsDir( dataDir: File ): File = {
    new File( dataDir.toString + "/projects" )
  }
  
  def getProjectDir( projectsDir: File, projectId: Int ): File = {    
    new File( projectsDir.toString + "/project_" + projectId )
  }
  
}