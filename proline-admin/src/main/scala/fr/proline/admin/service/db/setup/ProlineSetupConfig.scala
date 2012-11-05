package fr.proline.admin.service

import java.io.File
import scala.collection.JavaConversions.collectionAsScalaIterable
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigList
import fr.proline.repository.DatabaseConnector


/** Configuration settings for Proline setup */
case class ProlineSetupConfig(
             dataDirectory: File,
             udsDBConfig: DatabaseSetupConfig,
             udsDBDefaults: UdsDBDefaults,
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

case class UdsDBDefaults(
             resources: Config,
             instruments: java.util.List[Config],
             quantMethods: java.util.List[Config],
             spectrumParsingRules: java.util.List[Config]
            )