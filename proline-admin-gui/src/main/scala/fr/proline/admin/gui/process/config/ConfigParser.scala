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

import fr.proline.admin.gui.util.ShowPopupWindow
import fr.proline.repository.DriverType

import fr.profi.util.StringUtils.LINE_SEPARATOR
import fr.profi.util.scala.ScalaUtils
import fr.profi.util.scala.TypesafeConfigWrapper._

////////////////////////////////////////////////////////////////
// FIXME! write/set methods in this file are incredibly ugly. //
// Learn to use Typesage Config properly !                    //
////////////////////////////////////////////////////////////////

/**
 * *************************** *
 * PROLINE ADMIN CONFIGURATION *
 * *************************** *
 */

/** Model what is in the ProlineAdmin configuration file, corresponding to ProlineSettingsForm fields **/
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

/** Parse and write ProlineAdmin configuration file */
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

  /** Write config file **/
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
 * *************************** *
 * PROLINE SERVER CONFIGURATION *
 * *************************** *
 */

/** Model what is in the Proline server (WebCore) configuration file, corresponding to ProlineSettingsForm fields **/
case class ServerConfig(
    //serverConfFilePath: String,
    rawFilesMountPoints: Map[String, String] = Map(),
    mzdbFilesMountPoints: Map[String, String] = Map(),
    resultFilesMountPoints: Map[String, String] = Map()) {

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

    mpStrBuilder ++= s"  result_files {$LINE_SEPARATOR"
    _addMountPointsToStringBuilder(resultFilesMountPoints)

    mpStrBuilder ++= s"  }$DOUBLE_LINE_SEPARATOR  raw_files {$LINE_SEPARATOR"
    _addMountPointsToStringBuilder(rawFilesMountPoints)

    mpStrBuilder ++= s"  }$DOUBLE_LINE_SEPARATOR  mzdb_files {$LINE_SEPARATOR"
    _addMountPointsToStringBuilder(mzdbFilesMountPoints)

    mpStrBuilder ++= "  }"

    //return string
    s"""mount_points {

  ${mpStrBuilder.result()}

}"""
  }
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

    mpStrBuilder ++= "  }"

    //return string
    s"""mount_points = {

  ${mpStrBuilder.result()}

}"""
  }
}

/** Parse and write Proline server configuration file */
class ServerConfigFile(val path: String) extends LazyLogging {

  require(path != null && path.isEmpty() == false, "Configuration file path must not be null nor empty.")
  val serverConfigFile = new File(path)

  /** Read config file **/
  def read(): Option[ServerConfig] = {

    try {

      /* Create config */
      val config = ConfigFactory.parseFile(serverConfigFile)

      /* Retrieve mount points */
      def getMountPoints(key: String): Map[String, String] = {
        //config.getObject(s"mount_points.$key").map { case (k, v) => k -> v.render() }.toMap
        config.getObject(s"mount_points.$key").map { case (k, v) => k -> v.unwrapped().toString() }.toMap
      }

      val rawFilesMp = getMountPoints("raw_files")
      val mzdbFilesMp = getMountPoints("mzdb_files")
      val resultFilesMp = getMountPoints("result_files")
      Some(
        ServerConfig(
          //serverConfFilePath = path,
          rawFilesMountPoints = rawFilesMp,
          mzdbFilesMountPoints = mzdbFilesMp,
          resultFilesMountPoints = resultFilesMp))

    } catch {
      case t: Throwable => {
        logger.error("Error occured while reading server configuration file", t)
        None
      }
    }
  }

  /** Write config file **/
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

    /* Print in file (overwrite old config) */
    synchronized {
      //val out = new FileWriter(serverConfigFile, false)
      val out = new FileWriter(serverConfigFile)
      try { out.write(serverConfigTemplate) }
      finally { out.close }
    }
  }
}

/** Jms-server Pwx config */
case class PwxJmsServer(
    val dbHost: Option[String] = None,
    val dbPort: Option[Int] = None) {

  def toTypeSafePwxJms: String = {
    val pwxJmsStrBuilder = new StringBuilder()
    pwxJmsStrBuilder ++= s"""jms-server = {""" + LINE_SEPARATOR
    pwxJmsStrBuilder ++= s"""host = ${dbHost.getOrElse("localhost")}""" + LINE_SEPARATOR
    pwxJmsStrBuilder ++= s"""port = ${dbPort.getOrElse("5445")}""" + LINE_SEPARATOR
    pwxJmsStrBuilder ++= s"""}"""
    pwxJmsStrBuilder.result()
  }
}

/** Update configuration in PWX configuration file **/
class PwxConfigFile(val path: String) extends LazyLogging {

  require(path != null && path.isEmpty() == false, "Configuration file path must not be null nor empty.")
  val pwxConfigFile = new File(path)

  /** Read config file **/
  def read(): Option[ServerConfig] = {

    try {

      /* Create config */
      val config = ConfigFactory.parseFile(pwxConfigFile)

      /* Retrieve mount points */
      def getMountPoints(key: String): Map[String, String] = {
        //config.getObject(s"mount_points.$key").map { case (k, v) => k -> v.render() }.toMap
        config.getObject(s"mount_points.$key").map { case (k, v) => k -> v.unwrapped().toString() }.toMap
      }

      val rawFilesMp = getMountPoints("raw_files")
      val mzdbFilesMp = getMountPoints("mzdb_files")
      val resultFilesMp = getMountPoints("result_files")

      /* Return AdminConfig */
      Some(
        ServerConfig(
          rawFilesMountPoints = rawFilesMp,
          mzdbFilesMountPoints = mzdbFilesMp,
          resultFilesMountPoints = resultFilesMp))

    } catch {
      case t: Throwable => {
        logger.error("Error occured while reading server configuration file", t)
        None
      }
    }
  }
  //return Tuple(host,port) of jms-server and maybe for host-config later 
  def getPwxSimpleConfig(config: Config): (Option[String], Option[Int]) = {
    (config.getStringOpt("hostname"), config.getIntOpt("port"))
  }
  /** Read config file **/
  def getPwxJms(): Option[PwxJmsServer] = {
    try {
      /* Create config */
      val config = ConfigFactory.parseFile(pwxConfigFile)
      val jmsConfig = config.getConfig("jms_server")
      Some(
        PwxJmsServer(jmsConfig.getStringOpt("hostname"),
          jmsConfig.getIntOpt("port")))
    } catch {
      case t: Throwable => {
        logger.error("Error occured while reading Pwx configuration file", t)
        None
      }
    }
  }

  def write(serverConfig: ServerConfig, pwxJmsServer: PwxJmsServer) {
    try {

      // Merge configs
      val currentConfig: Config = ConfigFactory.parseFile(pwxConfigFile)
      /*val superMap = Map(
        "mount_points" -> (
          "result_files" -> serverConfig.resultFilesMountPoints,
          "raw_files" -> serverConfig.rawFilesMountPoints,
          "mzdb_files" -> serverConfig.mzdbFilesMountPoints
        )
      )
      ShowPopupWindow("superMap:\n" + scala.runtime.ScalaRunTime.stringOf(superMap))
      val newConfig = ConfigFactory.parseMap(superMap)*/

      //      val newConfig = ConfigFactory.parseString(serverConfig.toTypeSafePwxConfigString())
      //      val mergedConfig = newConfig.withFallback(currentConfig).resolve()
      lazy val pwxConfConfigContent =
        """
# This is the main configuration file for the application.
# ~~~~~

# Secret key
# ~~~~~~~~~~
# The secret key is used to secure cryptographics functions.
# If you deploy your application to several instances be sure to use the same key!
play.crypto.secret=":cjVy`<DW9=UDd3krrLMOrfhWR:3fJe2drLlQRrq7[X9G<F8DUpdIGV]H;:?H:[i"

# The application languages
# ~~~~~~~~~~~~~~~~~~~~~~~~~
application.langs="en"

# Global object class
# ~~~~~~~~~~~~~~~~~~~
# Define the Global object class for this application.
# Default to Global in the root package.
application.global=application.PWX

# Enabled modules
# ~~~~~~~~~~~~~~~
# TODO: enable me when PWX code has been be updated to support dependency injection
#play.modules.enabled += "play.modules.reactivemongo.ReactiveMongoModule"

# Router
# ~~~~~
# Define the Router object to use for this application.
# This router will be looked up first when the application is starting up,
# so make sure this is the entry point.
# Furthermore, it's assumed your route file is named properly.
# So for an application router like `my.application.Router`,
# you may need to define a router file `conf/my.application.routes`.
# Default to Routes in the root package (and conf/routes)
# application.router=my.application.Routes
# application.context="/PWX"

# MongoDB configuration
# ~~~~~~~~~~~~~~~~~~~~~~
mongodb.server = "localhost:27017"
mongodb.servers = [${mongodb.server}]
mongodb.db = "pwx_scala"

# MongoDB cache configuration
# ~~~~~~~~~~~~~~~~~~~~~~~~~~~
cache.mongodb.servers = ["localhost:27017"]
cache.mongodb.db = "pwx_scala_cache"

# MongoDB evolution configuration
# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
mongodb.evolution.enabled = false
mongodb.evolution.mongoCmd = "mongo_no_warn" ${mongodb.server}"/"${mongodb.db}

# Logger
# ~~~~~
# You can also configure logback (http://logback.qos.ch/),
# by providing an application-logger.xml file in the conf directory.

# Root logger:
logger.root=ERROR

# Logger used by the framework:
logger.play=INFO

# Logger provided to your application:
logger.application=DEBUG

# Default password for auto-created "admin" user
admin.password = "proline"

# To be used to iterate over external apps views during the PWX startup
#external_apps = ["mfpaq"]

# JMS server configuration
# ~~~~~~~~~~~~~~~~~~~~~~~~
""" + s"""${pwxJmsServer.toTypeSafePwxJms}""" + """
# MS-Angel configuration
# ~~~~~~~~~~~~~~~~~~~~~~~
msangel = {

  # Errors handling for MSI search monitor.
  errors = {

    # After server shutdown, try to restart interrupted tasks (optional, default = false)
    enable-error-recovery = false

    # Time interval in milliseconds during which the MSI search monitor will be stopped if at least 'max-count' errors occured.
    max-time-interval = 25000

    # The max. count of errors allowed in 'max-time-interval' milliseconds.
    # Optionnal. Default value = contexts.core-pool-size-max + 1
    #max-count = 1
  }

  contexts = {

    # There will be as much threads allocated to the workflow-job-operation pool as to the workflow-job-handler pool.
    # Thus the min/factor/max threads for each one is defined here with 3 common variables.
    # 1 thread for workflow-job-handler and 1 thread for workflow-job-operation are needed for the execution of one Job.

    # minimum number of threads to cap factor-based core number to
    core-pool-size-min = 1
    # No of core threads ... ceil(available processors * factor)
    core-pool-size-factor = 1.0
    # maximum number of threads to cap factor-based number to
    core-pool-size-max = 4

    # Execution contexts used to execute workflow jobs

    workflow-job-handler {
      executor = "thread-pool-executor"
      # Configuration for the thread pool
      thread-pool-executor {

        # minimum number of threads to cap factor-based core number to
        core-pool-size-min = ${msangel.contexts.core-pool-size-min}

        # No of core threads ... ceil(available processors * factor)
        core-pool-size-factor = ${msangel.contexts.core-pool-size-factor}

        # maximum number of threads to cap factor-based number to
        core-pool-size-max = ${msangel.contexts.core-pool-size-max}
      }
    }

    workflow-job-operation {
      executor = "thread-pool-executor"
      # Configuration for the thread pool
      thread-pool-executor {

        # minimum number of threads to cap factor-based core number to
        core-pool-size-min = ${msangel.contexts.core-pool-size-min}

        # No of core threads ... ceil(available processors * factor)
        core-pool-size-factor = ${msangel.contexts.core-pool-size-factor}

        # maximum number of threads to cap factor-based number to
        core-pool-size-max =  ${msangel.contexts.core-pool-size-max}
      }
    }

    # Execution context used to monitor the creation of files in a given directory (blocking operation)
    # httpj-request {
    #   fork-join-executor {
    #     # Min number of threads to cap factor-based parallelism number to
    #     parallelism-min = 8
    #
    #     # The parallelism factor is used to determine thread pool size using the
    #     # following formula: ceil(available processors * factor). Resulting size
    #     # is then bounded by the parallelism-min and parallelism-max values.
    #     parallelism-factor = 4.0
    #
    #     # Max number of threads to cap factor-based parallelism number to
    #     parallelism-max = 64
    # 
    #     # Setting to "FIFO" to use queue like peeking mode which "poll" 
    #     # or "LIFO" to use stack like peeking mode which "pop".
    #     task-peeking-mode = "FIFO"
    #   }
    # }

    # Execution context used to monitor the creation of files in a given directory (blocking operation)
    file-watcher {
      fork-join-executor {
        # Min number of threads to cap factor-based parallelism number to
        parallelism-min = 8

        # The parallelism factor is used to determine thread pool size using the
        # following formula: ceil(available processors * factor). Resulting size
        # is then bounded by the parallelism-min and parallelism-max values.
        parallelism-factor = 4.0

        # Max number of threads to cap factor-based parallelism number to
        parallelism-max = 64

        # Setting to "FIFO" to use queue like peeking mode which "poll" 
        # or "LIFO" to use stack like peeking mode which "pop".
        task-peeking-mode = "FIFO"
      }
    }
  }
}

# DSE configuration
# ~~~~~~~~~~~~~~~~~
""" + s"""
${serverConfig.toTypeSafeConfigString()}

# SpecLight configuration
# ~~~~~~~~~~~~~~~~~~~~~~~

# Define a gateway to access mzdb files remotely
# If set, mount_points are not used

speclight = {
  #	mzdb_gateway = "localhost:9000"
}
"""
      synchronized {
        val out = new FileWriter(pwxConfigFile)
        try { out.write(pwxConfConfigContent) }
        finally { out.close }
      }
    } catch {
      case e: Exception => {
        logger.error("Error while trying to write in PWX configuration file.");
        ShowPopupWindow("Can't save settings in PWX application.conf\n\n " + e.getMessage())
      }
    }
  }
}