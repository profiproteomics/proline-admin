package fr.proline.admin.service

import java.io.File
import com.typesafe.config.ConfigFactory
import com.typesafe.config.Config
import fr.proline.repository.DatabaseConnector
import fr.proline.admin.service.db._
import com.typesafe.config.ConfigList

/**
 * @author David Bouyssie
 *
 */
class SetupProline( config: ProlineSetupConfig ) {
  
  def run() {
    //val usdDBConnector = config.udsDBConfig.connector
    //println( usdDBConnector.getProperty(DatabaseConnector.PROPERTY_URL) )
    val udsDbSetup = new SetupUdsDB( config.udsDBConfig, config.udsDBDefaults )
    udsDbSetup.run()
    
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
    val dataDir = this._pathToFileOrResourceToFile(dataDirStr)
    
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
      
      // Merge connection settings with shared settings
      val fullConnConfig = this._mergeConfigs(connectionConfig,authConfig,hostConfig,driverConfig.getConfig("connection-properties"))
      
      // Build the script directory corresponding to the current database configuration
      val scriptDirStr = prolineConfig.getString("db-script-root") +
                         dbConfig.getString("script-directory") + 
                         driverConfig.getString("script-directory")
      val scriptDir = this._pathToFileOrResourceToFile(scriptDirStr)
      
      // Build the database setup configuration object
      ( dbType-> DatabaseSetupConfig( dbType, driverType, scriptDir, dataDir, fullConnConfig ) )
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
                   ConfigFactory.load("./uds_db/spectrum_parsing_rules.conf")
                                .getConfigList("parsing_rules")
                                .asInstanceOf[java.util.List[Config]]
                 )
  }
  
  private def _pathToFileOrResourceToFile( path: String ): File = {
    var file = new File(path)
    if( file.exists() == false) {
      val resource = classOf[DatabaseConnector].getResource(path)
      if( resource != null ) {
        file = new File( resource.toURI() )
      }      
    }
    
    file
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



