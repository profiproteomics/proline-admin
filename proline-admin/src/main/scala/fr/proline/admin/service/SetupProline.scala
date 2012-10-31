package fr.proline.admin.service

import java.io.File
import scala.collection.JavaConversions.collectionAsScalaIterable
import com.typesafe.config.ConfigFactory
import com.typesafe.config.Config
import fr.proline.repository.DatabaseConnector
import fr.proline.admin.service.db._

/**
 * @author David Bouyssie
 *
 */
class SetupProline( config: ProlineSetupConfig ) {
  
  def run() {
    //val usdDBConnector = config.udsDBConfig.connector
    //println( usdDBConnector.getProperty(DatabaseConnector.PROPERTY_URL) )
    val udsDbSetup = new SetupUdsDB( config.udsDBConfig )
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
    
    // Load database specific settings
    val dbList = List("uds-db","pdi-db","ps-db","msi-db","lcms-db")
    val dbSetupConfigByType = dbList.map { dbType =>
      
      // Retrieve settings relative to database connection
      val dbConfig = config.getConfig(dbType)
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
      pdiDBConfig = dbSetupConfigByType("pdi-db"),
      psDBConfig = dbSetupConfigByType("ps-db"),
      msiDBConfig = dbSetupConfigByType("msi-db"),
      lcmsDBConfig = dbSetupConfigByType("lcms-db")
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

/** Configuration settings for Proline setup */
case class ProlineSetupConfig(
             dataDirectory: File,
             udsDBConfig: DatabaseSetupConfig,
             pdiDBConfig: DatabaseSetupConfig,
             psDBConfig: DatabaseSetupConfig,
             msiDBConfig: DatabaseSetupConfig,
             lcmsDBConfig: DatabaseSetupConfig
             ) {
  
  // Check that directory exists
  require( dataDirectory.exists() && dataDirectory.isDirectory() )
  
}

object DatabaseSetupConfig {
  private val connectionProperties = new java.util.HashMap[String,String]()
  connectionProperties.put("dbName","")
  connectionProperties.put("connectionMode","")
  connectionProperties.put("userName","")
  connectionProperties.put("password","")
  connectionProperties.put("host","")
  connectionProperties.put("port","")
  connectionProperties.put("driverClassName","")
  connectionProperties.put("hibernate.dialect","")
  
  val connectionConfigSchema = ConfigFactory.parseMap(connectionProperties)
}

/** Configuration settings for database setup */
case class DatabaseSetupConfig( dbType: String,
                                driverType: String,
                                scriptDirectory: File,
                                dataDirectory: File,
                                connectionConfig: Config
                                ) {
  
  // Check that directories exists
  require( scriptDirectory.exists() && scriptDirectory.isDirectory(), "missing script directory:"+scriptDirectory )
  require( dataDirectory.exists() && dataDirectory.isDirectory(), "missing data directory:"+dataDirectory )
  
  // Check configuration validity
  connectionConfig.checkValid(DatabaseSetupConfig.connectionConfigSchema)
  
  lazy val connector = {
    
    // Parse properties from Config object
    val dbConnProps = new java.util.HashMap[String,String]()
    for( entry <- connectionConfig.entrySet ) {
      val key = entry.getKey
      dbConnProps.put("database."+ key,connectionConfig.getString(key) )
    }
    
    // Prepend data directory to database name if connection mode is FILE
    if( dbConnProps.get("database.connectionMode") == "FILE" ) {
      val dbName = dbConnProps.get("database.dbName")
      dbConnProps.put("database.dbName",dataDirectory + "/"+ dbName)
    }
    
    // Create the DB connection URL
    dbConnProps.put(DatabaseConnector.PROPERTY_URL, createURL(driverType,dbConnProps) )
    
    // Instantiate the database connector
    new DatabaseConnector(dbConnProps)
  }
  
  private def createURL(driverType: String, dbConnProps: java.util.HashMap[String,String]) : String = {
    
    val URLbuilder = new StringBuilder()
    val driverClassName = dbConnProps.get(DatabaseConnector.PROPERTY_DRIVERCLASSNAME)
    val dbName = dbConnProps.get("database.dbName")
    
    URLbuilder.append("jdbc:").append( driverType.toLowerCase() ).append(':')
    dbConnProps.get("database.connectionMode") match {
      case "HOST" => {
        require( driverClassName == "org.postgresql.Driver" || driverClassName == "org.h2.Driver",
                 "unhandled database driver for HOST connection mode" )
        
        // Append host
        URLbuilder.append("//").append(dbConnProps.get("database.host"))
        
        // Append port (optional)
        val port = dbConnProps.get("database.port")
        if( port != null && port.length > 0 )
           URLbuilder.append(":").append(port)
        
        // Append database name
        URLbuilder.append("/").append(dbName)
      }
      
      case "MEMORY" => {
        driverClassName match {
          case "org.h2.Driver" =>  URLbuilder.append("mem:").append(dbName)
          case "org.sqlite.JDBC" => URLbuilder.append(":memory:")          
          case _ => throw new Exception("unhandled database driver for MEMORY connection mode")
        }
      }
      
      case "FILE" => {
        driverClassName match {
          case "org.h2.Driver" => URLbuilder.append("file:").append(dbName)
          case "org.sqlite.JDBC" => URLbuilder.append(dbName)
          case _ => throw new Exception("unhandled database driver for FILE connection mode")
        }
      }    
    }
    
    URLbuilder.toString   
  }
  
}


