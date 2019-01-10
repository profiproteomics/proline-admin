package fr.proline.admin.gui.process.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigRenderOptions
import com.typesafe.scalalogging.LazyLogging

import scala.collection.JavaConversions._
import scala.collection.mutable.StringBuilder
import scala.io.Source
import java.io.File
import java.io.FileWriter
import java.io.FileNotFoundException

import java.io.IOException
import fr.proline.admin.gui.util.ShowPopupWindow
import fr.proline.repository.DriverType

import fr.profi.util.StringUtils.LINE_SEPARATOR
import fr.profi.util.scala.ScalaUtils
import fr.profi.util.scala.TypesafeConfigWrapper._

////////////////////////////////////////////////////////////////
// FIXME! write/set methods in this file are incredibly ugly. //
// Learn to use TypeSafe Config properly !                    //
////////////////////////////////////////////////////////////////

/**
 * *********************************
 * PROLINE ADMIN GUI CONFIGURATION *
 * *********************************
 */

/** Model what is in the Proline-Admin configuration file, corresponding to GUI fields **/
case class AdminConfig(
  filePath: String,
  var serverConfigFilePath: Option[String] = None,
  var pwxConfigFilePath: Option[String] = None,
  var pgsqlDataDir: Option[String] = None,
  var seqRepoConfigFilePath: Option[String] = None,
  var driverType: Option[DriverType] = None,
  var prolineDataDir: Option[String] = None,
  var dbUserName: Option[String] = None,
  var dbPassword: Option[String] = None,
  var dbHost: Option[String] = None,
  var dbPort: Option[Int] = None)

/** Parse and write Proline-Admin configuration file */
class AdminConfigFile(val path: String) extends LazyLogging {
  //TODO: object?

  require(path != null && path.isEmpty() == false, "Configuration file path must not be null nor empty.")
  private val adminConfigFile = new File(path)

  /** Get Config object from file **/
  def getTypesafeConfig(): Config = ConfigFactory.parseFile(adminConfigFile)

  /** Read config file **/
  def read(): Option[AdminConfig] = {

    try {
      val config = getTypesafeConfig()

      Some(
        AdminConfig(
          filePath = path,
          serverConfigFilePath = config.getStringOpt("server-config-file"),
          pwxConfigFilePath = config.getStringOpt("pwx-config-file"),
          pgsqlDataDir = config.getStringOpt("postgresql-data-dir"),
          seqRepoConfigFilePath = config.getStringOpt("seq-repo-config-file"),
          driverType = config.getStringOpt("proline-config.driver-type").map(dt => DriverType.valueOf(dt.toUpperCase())),
          prolineDataDir = config.getStringOpt("proline-config.data-directory"),
          dbUserName = config.getStringOpt("auth-config.user"),
          dbPassword = config.getStringOpt("auth-config.password"),
          dbHost = config.getStringOpt("host-config.host"),
          dbPort = config.getIntOpt("host-config.port")))
    } catch {
      case t: Throwable => {
        logger.error("Error occured while reading server configuration file", t.getMessage())
        None

      }
    }
  }

  /** Look for server-config-file only **/
  def getServerConfigPath(): Option[String] = {
    //Reload config in case there are some (hand-made) changes
    getTypesafeConfig().getStringOpt("server-config-file")
  }

  /** Look for pwx-config-file only **/
  def getPwxConfigPath(): Option[String] = {
    //Reload config in case there are some (hand-made) changes
    getTypesafeConfig().getStringOpt("pwx-config-file")
  }

  /** Look for postgresql-data-dir only **/
  def getPostgreSqlDataDir(): Option[String] = {
    //Reload config in case there are some (hand-made) changes
    getTypesafeConfig().getStringOpt("postgresql-data-dir")
  }

  /** Look for seq-repo-config-file only **/
  def getSeqRepoConfigPath(): Option[String] = {
    //Reload config in case there are some (hand-made) changes
    getTypesafeConfig().getStringOpt("seq-repo-config-file")
  }

  /** Set server-config-file only **/
  def setServerConfigPath(newPath: String) = {
    /* Don't change if it's the same as the old one */
    if (Option(newPath) != getServerConfigPath()) {
      _updateKey(configKey = "server-config-file", configValue = newPath)
    }
  }

  /** Set pwx-config-file only **/
  def setPwxConfigPath(newPath: String) = {
    /* Don't change if it's the same as the old one */
    if (Option(newPath) != getPwxConfigPath()) {
      _updateKey(configKey = "pwx-config-file", configValue = newPath)
    }
  }

  /** Set postgresql-data-dir only **/
  def setPostgreSqlDataDir(newPath: String) = {
    /* Don't change if it's the same as the old one */
    if (Option(newPath) != getPostgreSqlDataDir()) {
      _updateKey(configKey = "postgresql-data-dir", configValue = newPath)
    }
  }

  /** Set seq-repo-config-file only **/
  def setSeqRepoConfigPath(newPath: String) = {
    /* Don't change if it's the same as the old one */
    if (Option(newPath) != getSeqRepoConfigPath()) {
      _updateKey(configKey = "seq-repo-config-file", configValue = newPath)
    }
  }

  /** Set server-config-file or postgresql-data-dir only **/
  private def _updateKey(configKey: String, configValue: String) = synchronized {

    /* Read config file and and parse it, change only server-config-file */
    val src = Source.fromFile(adminConfigFile)

    val newLines: String =
      try {
        src.getLines().map { line =>

          if (line matches s""".*$configKey.*""") {
            val correctNewPath = ScalaUtils.doubleBackSlashes(configValue)
            s"""$configKey = "$correctNewPath""""
          } else line

        }.mkString(LINE_SEPARATOR)
      } finally { src.close() }

    /* Write updated config */
    val out = new FileWriter(adminConfigFile)
    try { out.write(newLines) }
    finally { out.close() }
  }

  /** Overwrite Proline-Admin configuration file **/
  def write(adminConfig: AdminConfig): Unit = synchronized {

    /* Fill template */
    //TODO: write ConfigObject
    val adminConfigTemplate = s"""
server-config-file = "${adminConfig.serverConfigFilePath.getOrElse("")}"
pwx-config-file = "${adminConfig.pwxConfigFilePath.getOrElse("")}"
postgresql-data-dir = "${adminConfig.pgsqlDataDir.getOrElse("")}"
seq-repo-config-file = "${adminConfig.seqRepoConfigFilePath.getOrElse("")}"

proline-config {
  driver-type = "${adminConfig.driverType.map(_.getJdbcURLProtocol()).getOrElse("")}" // valid values are: h2, postgresql or sqlite
  data-directory = "${adminConfig.prolineDataDir.getOrElse("<path/to/proline/data>")}"
}

auth-config {
  user="${adminConfig.dbUserName.getOrElse("<db_user>")}"
  password="${adminConfig.dbPassword.getOrElse("<db_password>")}"
}

host-config {
  host="${adminConfig.dbHost.getOrElse("<db_host>")}"
  port="${adminConfig.dbPort.getOrElse("5432")}"
}

uds-db { }

pdi-db { }

ps-db { }

msi-db { }

lcms-db { }

h2-config {
  script-directory = "/h2"
  connection-properties {
    connectionMode = "FILE"
    driver = "org.h2.Driver"
    //hibernate.dialect = "org.hibernate.dialect.H2Dialect"
  }
}

postgresql-config {
  script-directory = "/postgresql"
  connection-properties {
    connectionMode = "HOST"
    driver = "org.postgresql.Driver"
    //hibernate.dialect = "fr.proline.core.orm.utils.TableNameSequencePostgresDialect"
  }
}

sqlite-config {
  script-directory = "/sqlite"
  connection-properties {
    connectionMode = "FILE"
    driver = "org.sqlite.JDBC"
    //hibernate.dialect = "fr.proline.core.orm.utils.SQLiteDialect"
  }
}
"""

    /* Print in file (overwrite old config) */
    synchronized {
      //val out = new FileWriter(adminConfigFile, false)
      val out = new FileWriter(adminConfigFile)
      try { out.write(adminConfigTemplate) }
      finally { out.close }
    }
  }
}

/**
 * **************************** *
 * PROLINE SERVER CONFIGURATION *
 * **************************** *
 */

/** Model what is in the Proline server configuration file (db connection parameters), corresponding to GUI fields **/
case class SimpleConfig(
  var driverType: Option[DriverType] = None,
  var prolineDataDir: Option[String] = None,
  var dbUserName: Option[String] = None,
  var dbPassword: Option[String] = None,
  var dbHost: Option[String] = None,
  var dbPort: Option[Int] = None)
/** Model what is in the Proline server configuration file(mount points), corresponding to GUI fields **/
case class ServerConfig(
  //serverConfFilePath: String,
  rawFilesMountPoints: Map[String, String] = Map(),
  mzdbFilesMountPoints: Map[String, String] = Map(),
  resultFilesMountPoints: Map[String, String] = Map()) {

  /** Return mount points as a string to write in config template */
  def toTypeSafeConfigString(): String = {
    /* Mount points strings */
    val mpStrBuilder = new StringBuilder()

    def _addMountPointsToStringBuilder(mpMap: Map[String, String]) {
      mpMap.foreach {
        case (k, v) =>
          mpStrBuilder ++= s"""    $k = "${ScalaUtils.doubleBackSlashes(v)}"""" + LINE_SEPARATOR
      }
    }

    val DOUBLE_LINE_SEPARATOR = LINE_SEPARATOR + LINE_SEPARATOR

    mpStrBuilder ++= s"  result_files {$DOUBLE_LINE_SEPARATOR"
    _addMountPointsToStringBuilder(resultFilesMountPoints)

    mpStrBuilder ++= s"  }$DOUBLE_LINE_SEPARATOR  raw_files {$LINE_SEPARATOR"
    _addMountPointsToStringBuilder(rawFilesMountPoints)

    mpStrBuilder ++= s"  }$DOUBLE_LINE_SEPARATOR  mzdb_files {$LINE_SEPARATOR"
    _addMountPointsToStringBuilder(mzdbFilesMountPoints)

    mpStrBuilder ++= "  }"

    //return mount points as a string
    s"""mount_points {
     ${mpStrBuilder.result()}
    }"""
  }
  /** Return mount points as a string to write in config template */
  def toTypeSafePwxConfigString(): String = {
    /* Mount points strings */
    val mpStrBuilder = new StringBuilder()

    def _addMountPointsToStringBuilder(mpMap: Map[String, String]) {
      mpMap.foreach {
        case (k, v) =>
          mpStrBuilder ++= s"""    $k = "${ScalaUtils.doubleBackSlashes(v)}"""" + LINE_SEPARATOR
      }
    }

    val DOUBLE_LINE_SEPARATOR = LINE_SEPARATOR + LINE_SEPARATOR

    mpStrBuilder ++= s"  result_files = {$LINE_SEPARATOR"
    _addMountPointsToStringBuilder(resultFilesMountPoints)

    mpStrBuilder ++= s"  }$DOUBLE_LINE_SEPARATOR  raw_files = {$LINE_SEPARATOR"
    _addMountPointsToStringBuilder(rawFilesMountPoints)

    mpStrBuilder ++= s"  }$DOUBLE_LINE_SEPARATOR  mzdb_files = {$LINE_SEPARATOR"
    _addMountPointsToStringBuilder(mzdbFilesMountPoints)

    mpStrBuilder ++= """    #mzdb_root = "\\\\haldir\\d$\\raw_files" 
    }"""

    //return mount points as a string
    s"""mount_points = {
     ${mpStrBuilder.result()}
}"""
  }
}

/** Parse and write Proline server configuration file */
class ServerConfigFile(val path: String) extends LazyLogging {

  require(path != null && path.isEmpty() == false, "Configuration file path must not be null nor empty!")
  val serverConfigFile = new File(path)

  /** Get Config object from file **/
  def getTypesafeConfig(): Config = ConfigFactory.parseFile(serverConfigFile)

  /** Read and Return database connection parameters */
  def simpleConfig(): Option[SimpleConfig] = {
    try {
      /* Create config */
      val config = getTypesafeConfig()
      Some(
        SimpleConfig(
          driverType = config.getStringOpt("proline-config.driver-type").map(dt => DriverType.valueOf(dt.toUpperCase())),
          prolineDataDir = config.getStringOpt("proline-config.data-directory"),
          dbUserName = config.getStringOpt("auth-config.user"),
          dbPassword = config.getStringOpt("auth-config.password"),
          dbHost = config.getStringOpt("host-config.host"),
          dbPort = config.getIntOpt("host-config.port")))
    } catch {
      case t: Throwable => {
        logger.error("Error occured while trying to read Proline server configuration file", t.getMessage())
        None
      }
    }
  }

  /** Read config file **/
  def read(): Option[ServerConfig] = {
    try {
      /* Create config */
      val config = ConfigFactory.parseFile(serverConfigFile)

      /* Retrieve mount points */
      def getMountPoints(key: String): Map[String, String] = {
        config.getObject(s"mount_points.$key").map { case (k, v) => k -> v.unwrapped().toString() }.toMap
      }

      val rawFilesMp = getMountPoints("raw_files")
      val mzdbFilesMp = getMountPoints("mzdb_files")
      val resultFilesMp = getMountPoints("result_files")

      /* Return ServerConfig */
      Some(
        ServerConfig(
          rawFilesMountPoints = rawFilesMp,
          mzdbFilesMountPoints = mzdbFilesMp,
          resultFilesMountPoints = resultFilesMp))

    } catch {
      case t: Throwable => {
        logger.error("Error occured while trying to read server configuration file", t.getMessage())
        None
      }
    }
  }

  /** Overwrite Proline server configuration file **/
  def write(serverConfig: ServerConfig, adminConfig: AdminConfig): Unit = {
    //TODO: write ConfigObject, use from config

    /* Fill template */
    //TODO: improve (write config)
    val serverConfigTemplate = s"""
proline-config {
  driver-type = "${adminConfig.driverType.map(_.getJdbcURLProtocol()).getOrElse("")}" // valid values are: h2, postgresql or sqlite
  data-directory = "${adminConfig.prolineDataDir.getOrElse("<path/to/proline/data>")}"
}

auth-config {
  user="${adminConfig.dbUserName.getOrElse("<db_user>")}"
  password="${adminConfig.dbPassword.getOrElse("<db_password>")}"
}

host-config {
  host="${adminConfig.dbHost.getOrElse("<db_host>")}"
  port="${adminConfig.dbPort.getOrElse("5432")}"
}

uds-db { }

pdi-db { }

ps-db { }

msi-db { }

lcms-db { }

h2-config {
  script-directory = "/h2"
  connection-properties {
    connectionMode = "FILE"
    driver = "org.h2.Driver"
    //hibernate.dialect = "org.hibernate.dialect.H2Dialect"
  }
}

postgresql-config {
  script-directory = "/postgresql"
  connection-properties {
    connectionMode = "HOST"
    driver = "org.postgresql.Driver"
    //hibernate.dialect = "fr.proline.core.orm.utils.TableNameSequencePostgresDialect"
  }
}

sqlite-config {
  script-directory = "/sqlite"
  connection-properties {
    connectionMode = "FILE"
    driver = "org.sqlite.JDBC"
    //hibernate.dialect = "fr.proline.core.orm.utils.SQLiteDialect"
  }
}

// Absolute path to directories on Proline-Core (Server side) for types result_files, raw_files, mzdb_files
// label = "<absolute/directory/path>
${serverConfig.toTypeSafeConfigString()}

authentication {
  method = "UDS_hash" //Authentication method. Valid values: UDS_hash
}
"""

    /* Overwrite old config */
    synchronized {
      val out = new FileWriter(serverConfigFile)
      try { out.write(serverConfigTemplate) }
      finally { out.close }
    }
  }
}

/**
 * *********************************** *
 * PROLINE WEB EXTENSION CONFIGURATION *
 * *********************************** *
 */

/** Parse and write PWX configuration file */
class PwxConfigFile(val path: String) extends LazyLogging {

  require(path != null && path.isEmpty() == false, "Configuration file path must not be null nor empty.")
  val pwxConfigFile = new File(path)

  /** Return config object from file **/
  def getTypesafeConfig(): Config = ConfigFactory.parseFile(pwxConfigFile)

  /** Read and Return database connection parameters */
  def simpleConfig(): Option[SimpleConfig] = {
    try {
      /* Create config */
      val config = getTypesafeConfig()
      Some(
        SimpleConfig(
          driverType = config.getStringOpt("proline-config.driver-type").map(dt => DriverType.valueOf(dt.toUpperCase())),
          prolineDataDir = config.getStringOpt("proline-config.data-directory"),
          dbUserName = config.getStringOpt("auth-config.user"),
          dbPassword = config.getStringOpt("auth-config.password"),
          dbHost = config.getStringOpt("host-config.host"),
          dbPort = config.getIntOpt("host-config.port")))
    } catch {
      case t: Throwable => {
        logger.error("Error occured while trying to read Proline server configuration file", t.getMessage())
        None
      }
    }
  }

  /** Read PWX configuration file and return mount points **/
  def read(): Option[ServerConfig] = {
    try {
      /* Create config */
      val config = ConfigFactory.parseFile(pwxConfigFile)

      /* Retrieve mount points */
      def getMountPoints(key: String): Map[String, String] = {
        config.getObject(s"mount_points.$key").map { case (k, v) => k -> v.unwrapped().toString() }.toMap
      }

      val rawFilesMp = getMountPoints("raw_files")
      val mzdbFilesMp = getMountPoints("mzdb_files")
      val resultFilesMp = getMountPoints("result_files")

      /* Return ServerConfig */
      Some(
        ServerConfig(
          rawFilesMountPoints = rawFilesMp,
          mzdbFilesMountPoints = mzdbFilesMp,
          resultFilesMountPoints = resultFilesMp))
    } catch {
      case t: Throwable => {
        logger.error("Error occured while reading PWX configuration file", t.getMessage())
        None
      }
    }
  }

  /** Overwrite PWX configuration file **/
  def write(simpleConfig: SimpleConfig, serverConfig: ServerConfig): Unit = {
    //TODO: write ConfigObject, use from config

    /* Fill template */
    //TODO: improve (write config)
    val pwxConfigTemplate =
      s"""# JMS server access
# Mandatory
jms-server = {
  host = "localhost" 
  port = 5445
}

# Proline database type
# Mandatory
proline-config {
  driver-type = "${simpleConfig.driverType.map(_.getJdbcURLProtocol()).getOrElse("")}" # valid values are: h2, postgresql or sqlite
  data-directory = "${simpleConfig.prolineDataDir.getOrElse("./data/proline")}"
}

# Proline database authentication
# Mandatory
auth-config {
  user = "${simpleConfig.dbUserName.getOrElse("postgres")}"
  password = "${simpleConfig.dbPassword.getOrElse("postgres")}"
}


# Proline database connection
# For embedded Cortex mode only
host-config {
  host = "${simpleConfig.dbHost.getOrElse("localhost")}"
  port = "${simpleConfig.dbPort.getOrElse("5432")}"
}

# h2 -specific configuration
# For embedded Cortex mode only
### TODO: embed in code
h2-config {
  script-directory = "/h2" 
  connection-properties {
    connectionMode = "FILE" 
    driver = "org.h2.Driver" 
  }
}

# PostgreSQL -specific configuration
# For embedded Cortex mode only
### TODO: embed in code
postgresql-config {
  script-directory = "/postgresql" 
  connection-properties {
    connectionMode = "HOST" 
    driver = "org.postgresql.Driver" 
  }
}

# SQLite -specific configuration
# For embedded Cortex mode only
### TODO: embed in code
sqlite-config {
  script-directory = "/sqlite" 
  connection-properties {
    connectionMode = "FILE" 
    driver = "org.sqlite.JDBC" 
  }
}

${serverConfig.toTypeSafePwxConfigString()}
"""
    /* Overwrite old config */
    synchronized {
      val out = new FileWriter(pwxConfigFile)
      try { out.write(pwxConfigTemplate) }
      finally { out.close }
    }
  }
}

/**
 * *******************************************
 * PROLINE SEQUENCE REPOSITORY CONFIGURATION *
 * *******************************************
 */

/** Model what is in the Proline sequence repository configuration file, corresponding to GUI fields **/
case class SeqConfig(
  var driverType: Option[DriverType] = None,
  var maxPoolConnection: Option[Int] = None,
  var dbUserName: Option[String] = None,
  var dbPassword: Option[String] = None,
  var dbHost: Option[String] = None,
  var dbPort: Option[Int] = None,
  var dbUdsDb: Option[String] = None)
/** Parse and write Proline sequence repository configuration file(application.conf) */
class SeqConfigFile(val path: String) extends LazyLogging {

  require(path != null && path.isEmpty() == false, "Sequence Repository file must not be null nor empty")
  private val seqConfigFile = new File(path)

  /** Parse configuration file */
  def getTypeSafeConfig(): Config = ConfigFactory.parseFile(seqConfigFile)

  /** Read and Return Config object from configuration file */
  def read(): Option[SeqConfig] = {
    try {
      val seqConfig = getTypeSafeConfig()
      Some(
        SeqConfig(
          driverType = seqConfig.getStringOpt("proline-config.driver-type").map(dt => DriverType.valueOf(dt.toUpperCase())),
          maxPoolConnection = seqConfig.getIntOpt("proline-config.max-pool-connection"),
          dbUserName = seqConfig.getStringOpt("auth-config.user"),
          dbPassword = seqConfig.getStringOpt("auth-config.password"),
          dbHost = seqConfig.getStringOpt("host-config.host"),
          dbPort = seqConfig.getIntOpt("host-config.port"),
          dbUdsDb = seqConfig.getStringOpt("uds-db.connection-properties.dbName")))

    } catch {
      case t: Throwable => {
        logger.error("Error occured while reading Sequence Repository configuration file", t.getMessage())
        None
      }
    }
  }
  /** Write configuration file */
  def write(seqConfig: SeqConfig): Unit = synchronized {
    val seqConfigTemplate = s"""
proline-config {
   driver-type = "${seqConfig.driverType.map(_.getJdbcURLProtocol()).getOrElse("postgresql")}" // valid values are: h2, postgresql 
   max-pool-connection=${seqConfig.maxPoolConnection.getOrElse(3)} //Beta properties : specify maximum number of pool connected to DB Server 
}
//User and Password to connect to databases server
auth-config {
  user="${seqConfig.dbUserName.getOrElse("proline")}"
  password="${seqConfig.dbPassword.getOrElse("proline")}"
}

//Databases server Host
 
host-config {
  host="${seqConfig.dbHost.getOrElse("localhost")}"
  port="${seqConfig.dbPort.getOrElse("5432")}"
}
 
uds-db { 
 connection-properties {
    dbName = "${seqConfig.dbUdsDb.getOrElse("uds_db")}"
  }
}

h2-config {
  script-directory = "/h2"
  connection-properties {
    connectionMode = "FILE"
    driver = "org.h2.Driver"
  }
}

postgresql-config {
  script-directory = "/postgresql"
  connection-properties {
    connectionMode = "HOST"
    driver = "org.postgresql.Driver"
  }
}
 """
    /* overwrite old config */
    synchronized {
      try {
        val out = new FileWriter(seqConfigFile)
        try {
          out.write(seqConfigTemplate)
        } catch {
          case ex: IOException => {
            logger.error("error in closing filewriter", ex.getMessage())
          }
        } finally { out.close }
      } catch {
        case ex: FileNotFoundException => {
          logger.error("Missing file exception", ex.getMessage())
        }
      }
    }
  }
}

