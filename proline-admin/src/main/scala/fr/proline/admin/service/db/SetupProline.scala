package fr.proline.admin.service.db

import java.io.File
import com.weiglewilczek.slf4s.Logging
import com.typesafe.config.{Config,ConfigFactory,ConfigList}
import fr.proline.admin.service.db.setup._
import fr.proline.admin.utils.resources._
import fr.proline.core.dal.DatabaseManagement
import fr.proline.repository.DatabaseConnector
import fr.proline.admin.service.db.setup.SetupPsDB

/**
 * @author David Bouyssie
 *
 */
class SetupProline( config: ProlineSetupConfig ) extends Logging {
  
  def run() {
    
    // Instantiate a database manager
    val dbManager = new DatabaseManagement(config.udsDBConfig.connector)
    
    // Set Up the UDSdb
    this.logger.info("setting up the 'User Data Set' database...")
    new SetupUdsDB( dbManager, config.udsDBConfig, config ).run()
    
    // Set Up the PSdb
    this.logger.info("setting up the 'Peptide Sequence' database...")
    new SetupPsDB( dbManager, config.psDBConfig ).run()
    
    // Close PSdb connections
    // TODO: add this to the db manager closeAll method ?
    if( dbManager.psEMF.isOpen ) {
      dbManager.psEMF.close()
      dbManager.psDBConnector.closeAll()
    }
    /*
    // Set Up the PDIdb
    this.logger.info("setting up the 'Protein Database Index' database...")
    new SetupPdiDB( dbManager, config.pdiDBConfig ).run()
    
    // Close PDIdb connections
    // TODO: add this to the db manager closeAll method ?
    if( dbManager.pdiEMF.isOpen ) {
      dbManager.pdiEMF.close()
      dbManager.pdiDBConnector.closeAll()
    }*/
    
    // Release database manager connections and resources
    dbManager.closeAll()

  }

}


/** Static Proline setup. 
 * Setup will be performed using application defaults stored in "application.conf"
 */
object SetupProline {
  
  val appConf = ConfigFactory.load("application")
  
  def parseProlineSetupConfig( config: Config ): ProlineSetupConfig = {
    
    // Load proline main settings
    val prolineConfig = config.getConfig("proline-config")
    val dataDirStr = prolineConfig.getString("data-directory")
    val dataDir = pathToFileOrResourceToFile(dataDirStr,this.getClass())
    
    // Load shared settings
    val authConfig = config.getConfig("auth-config")
    val hostConfig = config.getConfig("host-config")
    val driverType = prolineConfig.getString("driver-type")
    val driverConfig = config.getConfig(driverType + "-config")
    val appDriverSpecificConf = ConfigFactory.load("application-"+driverType)
    
    // Load database specific settings
    val dbList = List("uds-db","pdi-db","ps-db","msi-db","lcms-db")
    val dbSetupConfigByType = dbList.map { dbType =>
      
      // Retrieve settings relative to database connection
      val dbDriverSpecificConf = appDriverSpecificConf.getConfig(dbType)
      val dbConfig = config.getConfig(dbType).withFallback(dbDriverSpecificConf)
      val connectionConfig = dbConfig.getConfig("connection-properties")
      val schemaVersion = dbConfig.getString("version")  
      
      // Merge connection settings with shared settings
      val fullConnConfig = this._mergeConfigs(connectionConfig,authConfig,hostConfig,driverConfig.getConfig("connection-properties"))
      
      // Build the script directory corresponding to the current database configuration
      val scriptDirStr = prolineConfig.getString("db-script-root") +
                         dbConfig.getString("script-directory") + 
                         driverConfig.getString("script-directory")
      val scriptDir = pathToFileOrResourceToFile(scriptDirStr,classOf[DatabaseConnector])
      
      // Build the database setup configuration object
      ( dbType-> DatabaseSetupConfig( dbType, driverType, schemaVersion, scriptDir, dataDir, fullConnConfig ) )
    } toMap
    
    ProlineSetupConfig(
      dataDirectory = dataDir,
      udsDBConfig = dbSetupConfigByType("uds-db"),
      udsDBDefaults = retrieveUdsDBDefaults(),
      pdiDBConfig = dbSetupConfigByType("pdi-db"),
      psDBConfig = dbSetupConfigByType("ps-db"),
      msiDBConfig = dbSetupConfigByType("msi-db"),
      lcmsDBConfig = dbSetupConfigByType("lcms-db")
    )
    
  }
  
  def retrieveUdsDBDefaults(): UdsDBDefaults = {
    
    UdsDBDefaults( ConfigFactory.load("./uds_db/resources.conf"),
                   ConfigFactory.load("./uds_db/instruments.conf")
                                .getConfigList("instruments")
                                .asInstanceOf[java.util.List[Config]],
                   ConfigFactory.load("./uds_db/quant_methods.conf")
                                .getConfigList("quant_methods")
                                .asInstanceOf[java.util.List[Config]],
                   ConfigFactory.load("./uds_db/peaklist_software.conf")
                                .getConfigList("peaklist_software")
                                .asInstanceOf[java.util.List[Config]]
                 )
  }
  
  /** Merge Config objects consecutively.
   * 
   */
  private def _mergeConfigs( configs: Config* ): Config = {
    var mergedConfig = configs.first
    
    configs.tail.foreach { config =>
      mergedConfig = mergedConfig.withFallback(config)
    }
    
    mergedConfig
  }
  
  /** Instantiates a SetupProline object and call the run() method. */
  def apply() {
    new SetupProline( parseProlineSetupConfig(appConf) ).run()    
  }
  
}



