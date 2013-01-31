package fr.proline.admin.service.db.setup

import java.io.File
import scala.collection.JavaConversions.collectionAsScalaIterable
import com.typesafe.config.{Config,ConfigFactory,ConfigList}
import fr.proline.core.orm.uds.{ExternalDb => UdsExternalDb,
                                ExternalDbPropertiesSerializer => UdsExtDbPropsSerializer }
import fr.proline.repository._

/** Configuration settings for Proline setup */
case class ProlineSetupConfig(
             dataDirectory: File,
             udsDBConfig: DatabaseSetupConfig,
             udsDBDefaults: UdsDBDefaults,
             pdiDBConfig: DatabaseSetupConfig,
             pdiDBDefaults: PdiDBDefaults,
             psDBConfig: DatabaseSetupConfig,
             msiDBConfig: DatabaseSetupConfig,
             msiDBDefaults: MsiDBDefaults,
             lcmsDBConfig: DatabaseSetupConfig
             ) {
  
  // Check that directory exists
  require( dataDirectory.exists() && dataDirectory.isDirectory() )
  
}

object DatabaseSetupConfig {
  private val connectionProperties = new java.util.HashMap[String,String]()
  connectionProperties.put("dbName","")
  connectionProperties.put("connectionMode","")
  connectionProperties.put("user","")
  connectionProperties.put("password","")
  connectionProperties.put("host","")
  connectionProperties.put("port","")
  connectionProperties.put("driver","")
  //connectionProperties.put("hibernate.dialect","")
  
  val connectionConfigSchema = ConfigFactory.parseMap(connectionProperties)
}

/** Configuration settings for database setup */
case class DatabaseSetupConfig( dbType: ProlineDatabaseType,
                                driverType: DriverType,
                                dbDirectory: File,
                                connectionConfig: Config
                                ) {
  
  // Check that directories exists
  //require( scriptDirectory.exists() && scriptDirectory.isDirectory(), "missing script directory:"+scriptDirectory )
  require( dbDirectory.exists() && dbDirectory.isDirectory(), "missing database directory:"+dbDirectory )
  
  // Check configuration validity
  connectionConfig.checkValid(DatabaseSetupConfig.connectionConfigSchema)
  
  var dbName = connectionConfig.getString("dbName")
  var connectionMode = ConnectionMode.valueOf( connectionConfig.getString("connectionMode") )
  var schemaVersion = "no.version"
  
  lazy val dbConnProperties = {    
    this.toUdsExternalDb.toPropertiesMap()
    
    /*// Parse properties from Config object
    val dbConnProps = new java.util.HashMap[String,String]()
    for( entry <- connectionConfig.entrySet ) {
      val key = entry.getKey
      dbConnProps.put("javax.persistence.jdbc."+ key,connectionConfig.getString(key) )
    }
    
    val urlKey = AbstractDatabaseConnector.PERSISTENCE_JDBC_URL_KEY
    val url = "jdbc:"+driverType+":"
    //dbConnProps.put("database.drivertype",driverType.toUpperCase )
    
    val protocol = dbConnProps.get("database.connectionMode").toUpperCase
    val protocolValue = if( protocol == "HOST" ) dbConnProps.get("database.host") + ":" +
                                                 dbConnProps.get("database.port")
                        else if( protocol == "FILE" ) dbDirectory.toString()
                        else ""
    
    dbConnProps.put("database.protocol",protocol )
    dbConnProps.put("database.protocolValue",protocolValue )
    
    dbConnProps*/
  }
  
  /*lazy val dbConnPrototype = {
    new ConnectionPrototype(dbConnProperties).namePrefix("")
  }*/
  
  lazy val connector = this.toNewConnector
  
  def toNewConnector() = {
    // Instantiate the database connector
    //val db = ProlineRepository.Databases.valueOf(dbType.toUpperCase)
    //dbConnPrototype.toConnector( dbName )
    
    val dbConnProps = dbConnProperties.asInstanceOf[java.util.Map[Object,Object]]
    DatabaseConnectorFactory.createDatabaseConnectorInstance( dbType, dbConnProps)
  }
  
  def toUdsExternalDb(): UdsExternalDb = {
    
    //val dbConnProps = this.dbConnProperties
    
    val udsExtDb = new UdsExternalDb()
    if( connectionMode == ConnectionMode.FILE ) udsExtDb.setDbName( dbDirectory + "/" + dbName )
    else udsExtDb.setDbName( dbName )
    
    udsExtDb.setType( this.dbType )
    udsExtDb.setDbVersion( this.schemaVersion )
    udsExtDb.setIsBusy( false )
    udsExtDb.setConnectionMode(connectionMode)
    
    val userName = connectionConfig.getString("user")
    if( userName.length > 0 ) udsExtDb.setDbUser(userName)
    else if( driverType != DriverType.SQLITE ) {
      throw new Exception("a user name is required in database authentification configuration")
    }
    
    val password = connectionConfig.getString("password")
    if( password.length > 0 ) udsExtDb.setDbPassword(password)
    
    val host = connectionConfig.getString("host")
    if( host.length > 0 ) udsExtDb.setHost(host)
    
    val port = connectionConfig.getString("port")
    if( port.length > 0 ) udsExtDb.setPort(port.toInt)
    
    udsExtDb.setDriverType(driverType)
    
    // Serialize properties
    UdsExtDbPropsSerializer.serialize(udsExtDb)
    
    udsExtDb
  }
  
  /*lazy val dbConnectorProps = {
    
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
    
    dbConnProps
  }
  
  lazy val connectorV1 = {
    // Instantiate the database connector
    new DatabaseConnector(dbConnectorProps)
  }
  
  def toUdsExternalDb(): UdsExternalDb = {
    
    val udsExtDb = new UdsExternalDb()
    udsExtDb.setDbName( dbConnectorProps.get("database.dbName") )
    udsExtDb.setType( this.dbType )
    udsExtDb.setDbVersion( this.schemaVersion )
    udsExtDb.setIsBusy( false )
    
    udsExtDb.setConnectionMode(dbConnectorProps.get("database.connectionMode"))
    
    val userName = dbConnectorProps.get(DatabaseConnector.PROPERTY_USERNAME)
    if( userName.length > 0 ) udsExtDb.setDbUser(userName)
    
    val password = dbConnectorProps.get(DatabaseConnector.PROPERTY_PASSWORD)
    if( password.length > 0 ) udsExtDb.setDbPassword(password)
    
    val host = dbConnectorProps.get("database.host")
    if( host.length > 0 ) udsExtDb.setHost(host)
    
    val port = dbConnectorProps.get("database.port")
    if( port.length > 0 ) udsExtDb.setPort(port.toInt)    
    
    udsExtDb
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
  }*/
  
}

case class MsiDBDefaults(
             scorings: java.util.List[Config],
             schemata: java.util.List[Config]
            )
            
case class PdiDBDefaults(
             resources: Config
            )
            
case class UdsDBDefaults(
             resources: Config,
             instruments: java.util.List[Config],             
             peaklistSoftware: java.util.List[Config],
             quantMethods: java.util.List[Config]
            )
            
