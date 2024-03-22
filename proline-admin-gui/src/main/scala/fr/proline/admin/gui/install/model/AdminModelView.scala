package fr.proline.admin.gui.install.model

import com.typesafe.scalalogging.LazyLogging
import fr.proline.admin.gui.Install
import fr.proline.admin.gui.process.DatabaseConnection
import fr.proline.admin.gui.process.config._
import fr.proline.repository.DriverType

/**
 * The admin model view. Defines UI actions: read, update and write properties from Proline server configuration file.
 *
 * @author aromdhani
 *
 */

class AdminModelView(serverConfigFilePath: String) extends LazyLogging {

  /** Return Proline server postgreSQL configuration  */
  def serverConfigOpt(): Option[SimpleConfig] = {
    try {
      val serverConfigFile = new ServerConfigFile(serverConfigFilePath)
      serverConfigFile.simpleConfig()
    } catch {
      case ex: Exception =>
        logger.error("Error while trying to retrieve Proline server properties from configuartion file!", ex.getMessage())
        None
    }
  }

  /** Return Proline server mount points  */
  def serverConfigFileOpt(): Option[ServerConfigFile] = {
    try {
      Option(new ServerConfigFile(serverConfigFilePath))
    } catch {
      case ex: Exception =>
        logger.error("Error while trying to retrieve Proline server mount points", ex.getMessage())
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

  /** Save Proline admin configurations */
  def onSaveAdminConfig(adminConfig: AdminConfig): Unit = {
    try {
      logger.debug("Update Proline admin configurations...")
      val adminConfigFile = new AdminConfigFile(Install.adminConfPath)
      adminConfigFile.write(adminConfig)
    } catch {
      case ex: Exception => logger.error("Error while trying to update Proline admin configurations", ex.getMessage())
    }
  }

  /** Save Proline server configurations */
  def onSaveServerConfig(adminConfig: AdminConfig, serverConfig: ServerMountPointsConfig): Unit = {
    try {
      logger.debug("Update Proline server configurations...")
      serverConfigFileOpt().get.write(serverConfig, adminConfig)
    } catch {
      case ex: Exception => logger.error("Error while trying to update Proline server configurations", ex.getMessage())
    }
  }
}

