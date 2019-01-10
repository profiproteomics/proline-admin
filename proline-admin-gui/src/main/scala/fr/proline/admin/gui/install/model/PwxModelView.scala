package fr.proline.admin.gui.install.model

import com.typesafe.scalalogging.LazyLogging

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import fr.proline.admin.gui.Install
import fr.proline.repository.DriverType
import fr.proline.admin.gui.process.config._
import fr.profi.util.scala.TypesafeConfigWrapper._
import fr.proline.admin.gui.process.DatabaseConnection
import java.io.File

/**
 * The Proline web extension model view. Defines UI actions: read and write configurations from configuration file.
 *
 * @author aromdhani
 *
 */

class PwxModelView(pwxConfigFilePath: String) extends LazyLogging {

  /** Return PWX database connection parameters  */
  def pwxConfigOpt(): Option[SimpleConfig] = {
    try {
      val pwxConfigFile = new PwxConfigFile(pwxConfigFilePath)
      pwxConfigFile.simpleConfig()
    } catch {
      case ex: Exception =>
        logger.error("Error while trying to retrieve PWX properties from configuartion file!", ex.getMessage())
        None
    }
  }

  /** Return Pwx mount points  */
  def pwxConfigFileOpt(): Option[PwxConfigFile] = {
    try {
      Option(new PwxConfigFile(pwxConfigFilePath))
    } catch {
      case ex: Exception =>
        logger.error("Error while trying to retrieve Proline web extension mount points", ex.getMessage())
        None
    }
  }

  /** Test database connection parameters from GUI fields */
  def onTestDbConn(driverType: DriverType, user: String, password: String, host: String, port: Int) = {
    DatabaseConnection.testDbConnection(
      driverType,
      user,
      password,
      host,
      port,
      showSuccessPopup = true,
      showFailurePopup = true,
      Option(Install.stage))
  }

  /** Save PWX configurations */
  def onSavePwxConfig(simpleConfig: SimpleConfig, serverConfig: ServerConfig): Unit = {
    try {
      logger.debug("Update PWX configurations...")
      pwxConfigFileOpt().get.write(simpleConfig, serverConfig)
    } catch {
      case ex: Exception => logger.error("Error while trying to update Proline web extension configurations", ex.getMessage)
    }
  }
}

