package fr.proline.admin.gui.process.config.wizard

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigRenderOptions
import com.typesafe.scalalogging.LazyLogging
import fr.profi.util.scala.TypesafeConfigWrapper._
import fr.proline.repository.DriverType
import fr.profi.util.scala.ScalaUtils
import java.io.File
import java.io.FileWriter
import java.io.FileNotFoundException
import java.io.IOException

/**
 *
 * Sequence Repository configuration file
 *
 */

/** parse and write Sequence repository configuration file */

class SeqConfigFile(val path: String) extends LazyLogging {

  require(path != null && path.isEmpty() == false, "Sequence Repository file must not be null nor empty")
  private val seqConfigFile = new File(path)

  /** get config object from file */

  def getTypeSafeConfig(): Config = ConfigFactory.parseFile(seqConfigFile)
  def read: Option[SeqConfig] = {
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
  def write(seqConfig: SeqConfig): Unit = synchronized {
    val seqConfigTemplate = s"""
proline-config {
   driver-type = "${seqConfig.driverType.map(_.getJdbcURLProtocol()).getOrElse("postgresql")}" // valid values are: h2, postgresql 
   max-pool-connection=${seqConfig.maxPoolConnection.getOrElse(3)}//Beta properties : specify maximum number of pool connected to DB Server 
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
            logger.error("error in closing filewriter", ex)
          }
        } finally { out.close }
      } catch {
        case ex: FileNotFoundException => {
          logger.error("Missing file exception", ex)
        }
      }

    }
  }
}

/** model of Sequence repository configuration file */

case class SeqConfig(
  var driverType: Option[DriverType] = None,
  var maxPoolConnection: Option[Int] = None,
  var dbUserName: Option[String] = None,
  var dbPassword: Option[String] = None,
  var dbHost: Option[String] = None,
  var dbPort: Option[Int] = None,
  var dbUdsDb: Option[String] = None)