package fr.proline.admin.service.db

import java.io.File
import javax.persistence.EntityManager
import com.weiglewilczek.slf4s.Logging
import fr.proline.admin.service.db.setup.{DatabaseSetupConfig,ProlineSetupConfig}
import fr.proline.core.dal.DatabaseManagement
import fr.proline.core.orm.uds.{Project => UdsProject}
import fr.proline.repository.ConnectionPrototype.{DatabaseProtocol => DbProtocols}
import setup.{SetupLcmsDB,SetupMsiDB}


/**
 * @author David Bouyssie
 *
 */
class CreateProjectDBs( dbManager: DatabaseManagement, config: ProlineSetupConfig, projectId: Int ) extends Logging {
  
  def run() {
    
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
    new SetupMsiDB( dbManager, msiDBConfig, projectId ).run()
    
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
    
  }
  
  private def _prepareDBCreation( dbConfig: DatabaseSetupConfig ): DatabaseSetupConfig = {
    
    config.msiDBConfig.dbConnPrototype.getProtocol match {
      case DbProtocols.FILE => {
        
        // Retrieve project directory
        val projectDir = CreateProjectDBs.getProjectDir( dbConfig.dbDirectory, this.projectId )
        if( projectDir.exists == false ) projectDir.mkdir()
        
        // Update database config directory
        dbConfig.copy( dbDirectory = projectDir )
      }
      case DbProtocols.HOST => {
        throw new Exception("NYI")
        dbConfig
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
    val dbManager = new DatabaseManagement(prolineConf.udsDBConfig.connector)
    
    // Create databases
    new CreateProjectDBs( dbManager, prolineConf, projectId ).run()
    
    // Close the database manager
    dbManager.closeAll()
    
  }
  
  def getProjectDir( dataDir: File, projectId: Int ): File = {
    new File( dataDir.toString + "/project_" + projectId )
  }
  
}