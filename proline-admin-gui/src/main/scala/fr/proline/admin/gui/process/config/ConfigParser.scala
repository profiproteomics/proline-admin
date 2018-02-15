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

    //          /* Create new config (merge with old) */
    //          //val newConfig = ConfigFactory.parseMap(Map("server-config-file" -> newPath))
    //          //      println("newPath: " + newPath + "**")
    //          val correctNewPath = newPath.replaceAll("""\\""", """\\\\""")
    //          //      println("correctNewPath: " + correctNewPath + "**")
    //          val toParse = s"""server-config-file = "$correctNewPath""""
    //          //      println("toParse: " + toParse + "**")
    //          val newConfig = ConfigFactory.parseString(toParse)
    //          val mergedConfig = newConfig.withFallback(getTypeSafeConfig())
    //    
    //          /* Write in file */
    //          synchronized {
    //            val renderOptions = ConfigRenderOptions.concise().setComments(true).setFormatted(true).setJson(false)
    //            val render = mergedConfig.root().render(renderOptions)
    //            val fileWriter = new FileWriter(adminConfigFile)
    //            try { fileWriter.write(render) }
    //            finally { fileWriter.close() }
    //          }
    //        }
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

      /* Return AdminConfig */
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

/**case class of Jms server pws config file */
case class PwxJmsServer(
    val dbHost: Option[String] = None,
    val dbPort: Option[Int] = None) {

  def toTypeSafePwxJms: String = {
    val pwxJmsStrBuilder = new StringBuilder()
    pwxJmsStrBuilder ++= s""""jms_server"{""" + LINE_SEPARATOR
    pwxJmsStrBuilder ++= s"""hostname=${dbHost.getOrElse("localhost")}""" + LINE_SEPARATOR
    pwxJmsStrBuilder ++= s"""port=${dbPort.getOrElse("5445")}""" + LINE_SEPARATOR
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

  /** Read config file **/
  def getPwxJms(): Option[PwxJmsServer] = {
    try {
      /* Create config */
      val config = ConfigFactory.parseFile(pwxConfigFile)
      Some(
        PwxJmsServer(config.getStringOpt("jms_server.hostname"),
          config.getIntOpt("jms_server.port")))
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
      val newPwxJms = ConfigFactory.parseString(pwxJmsServer.toTypeSafePwxJms)
      val mergedpwxJmsConfig = newPwxJms.withFallback(currentConfig)
      val newConfig = ConfigFactory.parseString(serverConfig.toTypeSafeConfigString())
      val mergedConfig = newConfig.withFallback(mergedpwxJmsConfig)

      //      logger.warn("mergedConfig")
      //      logger.warn(scala.runtime.ScalaRunTime.stringOf(mergedConfig))
      //      logger.warn(path)
      /*ShowPopupWindow(
        wText = "mergedConfig:\n" + scala.runtime.ScalaRunTime.stringOf(mergedConfig) +
          "\n\npath: " + path,
        wTitle = "yo"
      )*/

      synchronized {
        val renderOptions = ConfigRenderOptions.concise().setComments(true).setFormatted(true).setJson(false)
        val render = mergedConfig.root().render(renderOptions)
        val fileWriter = new FileWriter(pwxConfigFile)
        fileWriter.write(render)
        fileWriter.close()
        logger.warn(" fileWriter.close()")
      }
    } catch {
      //case e: Exception => logger.warn("Can't save settings in PWX application.conf : ", e.getMessage())
      case e: Exception => ShowPopupWindow("Can't save settings in PWX application.conf\n\n " + e.getMessage())
    }
  }
}