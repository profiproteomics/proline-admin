package fr.proline.admin.gui.process.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.slf4j.Logging

import java.io.File
import java.io.FileWriter

import scala.collection.JavaConversions._
import scala.collection.mutable.StringBuilder
import scala.io.Source

import fr.profi.util.scala.ScalaUtils
import fr.profi.util.scala.TypesafeConfigWrapper._
import fr.proline.repository.DriverType


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
case class AdminConfig (
  filePath: String,
  var serverConfigFilePath: Option[String] = None,
  var driverType: Option[DriverType] = None,
  var dataDir: Option[String] = None,
  var dbUserName: Option[String] = None,
  var dbPassword: Option[String] = None,
  var dbHost: Option[String] = None,
  var dbPort: Option[Int] = None
)

/** Parse and write ProlineAdmin configuration file */
class AdminConfigFile(val path: String) extends Logging {
  //TODO: object?

  require(
    path != null && path.isEmpty() == false,
    "Configuration file path must not be null nor empty."
  )

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
          driverType = config.getStringOpt("proline-config.driver-type").map(dt => DriverType.valueOf(dt.toUpperCase())),
          dataDir = config.getStringOpt("proline-config.data-directory"),
          dbUserName = config.getStringOpt("auth-config.user"),
          dbPassword = config.getStringOpt("auth-config.password"),
          dbHost = config.getStringOpt("host-config.host"),
          dbPort = config.getIntOpt("host-config.port")
        )
      )
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

  /** Set server-config-file only **/
  def setServerConfigPath(newPath: String) = synchronized {

    //TODO: improve / go back to config

    /* Don't change if it's the same as the old one */
    if (newPath != getServerConfigPath().getOrElse("")) {

      /* Read config file and and parse it, change only server-config-file */
      val src = Source.fromFile(adminConfigFile)

      val newLines: String =
        try {
          src.getLines().map { line =>

            if (line matches """.*server-config-file.*""") {
              val correctNewPath = ScalaUtils.doubleBackSlashes(newPath)
              s"""server-config-file = "$correctNewPath""""
            } 
            
            else line

          }.mkString("\n")
        }

      finally { src.close() }

      /* Write updated config */
      val out = new FileWriter(adminConfigFile)
      try { out.write(newLines) }
      finally { out.close() }

    }
    
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

proline-config {
  driver-type = "${adminConfig.driverType.map(_.getJdbcURLProtocol()).getOrElse("")}" // valid values are: h2, postgresql or sqlite
  data-directory = "${adminConfig.dataDir.getOrElse("<path/to/proline/data>")}"
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
  filePath: String,
  rawFilesMountPoints: Map[String, String] = Map(),
  mzdbFilesMountPoints: Map[String, String] = Map(),
  resultFilesMountPoints: Map[String, String] = Map()
)

/** Parse and write Proline server configuration file */
class ServerConfigFile(val path: String) extends Logging {

  require(
    path != null && path.isEmpty() == false,
    "Configuration file path must not be null nor empty."
  )

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
          filePath = path,
          rawFilesMountPoints = rawFilesMp,
          mzdbFilesMountPoints = mzdbFilesMp,
          resultFilesMountPoints = resultFilesMp
        )
      )

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

    /* Mount points strings */
    val mpStrBuilder = new StringBuilder()

    def _addMpToStringBuilder(mpMap: Map[String, String]) {
      mpMap.foreach {
        case (k, v) =>
          mpStrBuilder ++= s"""    $k = "$v"""" + "\n"
      }
    }

    mpStrBuilder ++= "  result_files {\n"
    _addMpToStringBuilder(serverConfig.resultFilesMountPoints)

    mpStrBuilder ++= "  }\n\n" + "  raw_files {\n"
    _addMpToStringBuilder(serverConfig.rawFilesMountPoints)

    mpStrBuilder ++= "  }\n\n" + "  mzdb_files {\n"
    _addMpToStringBuilder(serverConfig.mzdbFilesMountPoints)

    mpStrBuilder ++= "  }"

    /* Fill template */
    //TODO: improve (write config)
    val serverConfigTemplate = s"""
proline-config {
  driver-type = "${adminConfig.driverType.map(_.getJdbcURLProtocol()).getOrElse("")}" // valid values are: h2, postgresql or sqlite
  data-directory = "${adminConfig.dataDir.getOrElse("<path/to/proline/data>")}"
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
mount_points {

${mpStrBuilder.result()}

}

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