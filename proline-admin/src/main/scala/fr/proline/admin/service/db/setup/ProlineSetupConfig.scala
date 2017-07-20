package fr.proline.admin.service.db.setup

import java.io.File
import scala.collection.JavaConversions.collectionAsScalaIterable
import com.typesafe.config.{ Config, ConfigFactory, ConfigList }
import com.typesafe.scalalogging.LazyLogging
import fr.proline.core.orm.uds.{
  ExternalDb => UdsExternalDb,
  ExternalDbPropertiesSerializer => UdsExtDbPropsSerializer
}
import fr.proline.repository._

/** Configuration settings for Proline setup */
case class ProlineSetupConfig(
  dataDirectoryOpt: Option[File],
  udsDBConfig: DatabaseSetupConfig,
  pdiDBConfig: DatabaseSetupConfig,
  psDBConfig: DatabaseSetupConfig,
  msiDBConfig: DatabaseSetupConfig,
  lcmsDBConfig: DatabaseSetupConfig
)

object DatabaseSetupConfig {
  private val connectionProperties = new java.util.HashMap[String, String]()
  connectionProperties.put("dbName", "")
  connectionProperties.put("connectionMode", "")
  connectionProperties.put("user", "")
  connectionProperties.put("password", "")
  connectionProperties.put("host", "")
  connectionProperties.put("port", "")
  connectionProperties.put("driver", "")
  //connectionProperties.put("hibernate.dialect","")

  val connectionConfigSchema = ConfigFactory.parseMap(connectionProperties)
}

/** Configuration settings for database setup */
case class DatabaseSetupConfig(
  dbType: ProlineDatabaseType,
  driverType: DriverType,
  dbDirectoryOpt: Option[File],
  connectionConfig: Config
) extends LazyLogging {

  // Check that directories exists
  if (driverType != DriverType.POSTGRESQL) {
    require( dbDirectoryOpt.isDefined, s"The database directory must be provided for the $driverType driver" )
    
    val dbDirectory = dbDirectoryOpt.get
    require(dbDirectory.exists() && dbDirectory.isDirectory(), "missing database directory:" + dbDirectory)
  }

  // Check configuration validity
  connectionConfig.checkValid(DatabaseSetupConfig.connectionConfigSchema)

  var dbName = connectionConfig.getString("dbName")
  var connectionMode = ConnectionMode.valueOf(connectionConfig.getString("connectionMode"))
  var schemaVersion = "no.version"

  lazy val dbConnProperties = {
    this.toUdsExternalDb.toPropertiesMap()
  }

  def toNewConnector() = {
    
    // Instantiate a database connector
    val dbConnProps = dbConnProperties.asInstanceOf[java.util.Map[Object, Object]]
    val databaseConnector = DatabaseConnectorFactory.createDatabaseConnectorInstance(dbType, dbConnProps)
    
    logger.warn("Creation of raw DatabaseConnector [" + databaseConnector + "] without DataStoreConnectorFactory")
    
    databaseConnector
  }

  def toUdsExternalDb(): UdsExternalDb = {

    //val dbConnProps = this.dbConnProperties

    val udsExtDb = new UdsExternalDb()
    if (connectionMode == ConnectionMode.FILE) udsExtDb.setDbName(dbDirectoryOpt.get + "/" + dbName)
    else udsExtDb.setDbName(dbName)

    udsExtDb.setType(this.dbType)
    udsExtDb.setDbVersion(this.schemaVersion)
    udsExtDb.setIsBusy(false)
    udsExtDb.setConnectionMode(connectionMode)

    val userName = connectionConfig.getString("user")
    if (userName.length > 0) udsExtDb.setDbUser(userName)
    else if (driverType != DriverType.SQLITE) {
      throw new Exception("a user name is required in database authentification configuration")
    }

    val password = connectionConfig.getString("password")
    if (password.length > 0) udsExtDb.setDbPassword(password)

    val host = connectionConfig.getString("host")
    if (host.length > 0) udsExtDb.setHost(host)

    val port = connectionConfig.getString("port")
    if (port.length > 0) {
      val portAsInteger = port.toInt
      if( portAsInteger >= 0 && portAsInteger <= 65535 )
        udsExtDb.setPort(portAsInteger)
    }

    udsExtDb.setDriverType(driverType)

    // Serialize properties
    UdsExtDbPropsSerializer.serialize(udsExtDb)

    udsExtDb
  }

}

case class MsiDBDefaults(
  scorings: java.util.List[Config],
  schemata: java.util.List[Config]
)

object GetDataStoreDbNaming {
  def apply(driverType: DriverType): IDataStoreDbNaming = {
    driverType match {
      case DriverType.H2 => H2DataStoreDbNaming
      case DriverType.POSTGRESQL => PgDataStoreDbNaming
      case DriverType.SQLITE => SQLiteDataStoreDbNaming
    }
  }
}

sealed trait IDataStoreDbNaming {
  def udsDbName: String
  def pdiDbName: String
  def psDbName: String
  def msiDbName: String
  def lcMsDbName: String
}

case object H2DataStoreDbNaming extends IDataStoreDbNaming {
  val udsDbName = "uds-db"
  val pdiDbName = "pdi-db"
  val psDbName = "ps-db"
  val msiDbName = "msi-db"
  val lcMsDbName = "lcms-db"
}

case object PgDataStoreDbNaming extends IDataStoreDbNaming {
  val udsDbName = "uds_db"
  val pdiDbName = "pdi_db"
  val psDbName = "ps_db"
  val msiDbName = "msi_db"
  val lcMsDbName = "lcms_db"
}

case object SQLiteDataStoreDbNaming extends IDataStoreDbNaming {
  val udsDbName = "uds-db.sqlite"
  val pdiDbName = "pdi-db.sqlite"
  val psDbName = "ps-db.sqlite"
  val msiDbName = "msi-db.sqlite"
  val lcMsDbName = "lcms-db.sqlite"
}