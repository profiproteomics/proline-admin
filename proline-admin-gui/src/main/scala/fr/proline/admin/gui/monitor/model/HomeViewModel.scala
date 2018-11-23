package fr.proline.admin.gui.monitor.model

import com.typesafe.scalalogging.LazyLogging

import scalafx.Includes._
import scalafx.application.Platform
import fr.proline.admin.gui.Monitor
import fr.proline.admin.gui.process.{ UdsRepository, DatabaseConnection }
import fr.proline.admin.gui.util.FxUtils
import fr.profi.util.scala.ScalaUtils._
import fr.proline.admin.gui.process.config.AdminConfig
import fr.proline.admin.gui.process.config.AdminConfigFile
import fr.proline.admin.gui.wizard.process.config.{ NodeConfig, ParsingRule }
import fr.proline.admin.gui.wizard.process.config.{ NodeConfigFile, ParsingRulesFile }
import fr.proline.admin.gui.process.{ UdsRepository, ProlineAdminConnection }
import fr.proline.admin.gui.wizard.util.UserGuide
import java.io.File

/**
 * HomeViewModel check home panel operations
 * @author aromdhani
 *
 */
class HomeViewModel(monitorConfPath: String) extends LazyLogging {

  /** Get Proline-Admin Config */
  def getAdminConfigOpt(): Option[AdminConfig] = {
    try {
      if (Monitor.adminConfPathIsEmpty()) return None
      else {
        val adminConfFile = new AdminConfigFile(monitorConfPath)
        adminConfFile.read()
      }
    } catch {
      case t: Throwable => {
        logger.error("Error occured while trying to get Proline-Admin configurations", t.getMessage())
        None
      }
    }
  }

  /** Get Proline Server JMS-node  */
  def getServerNodeConfigOpt(): Option[NodeConfig] = {
    try {
      if (getAdminConfigOpt().isDefined) {
        if (getAdminConfigOpt().get.serverConfigFilePath.isDefined && new File(getAdminConfigOpt().get.serverConfigFilePath.get).exists) {
          val prolineServerConfigParent = new File(getAdminConfigOpt().get.serverConfigFilePath.get).getParent
          val nodeConfigPath = new File(prolineServerConfigParent + File.separator + "jms-node.conf").getCanonicalPath
          val nodeConfigFile = new NodeConfigFile(nodeConfigPath)
          nodeConfigFile.read
        } else {
          logger.warn("Cannot find jms-node configurations file.")
          None
        }
      } else {
        None
      }
    } catch {
      case t: Throwable => {
        logger.error("Error occured while trying to get server jms-node properties", t.getMessage())
        None
      }
    }
  }

  /** Reload Proline-Admin Config */
  def setNewConfig() {
    try {
      ProlineAdminConnection._setNewProlineInstallConfig(monitorConfPath)
    } catch {
      case t: Throwable => logger.error("Error while trying to set the new configurations", t.getMessage())
    }
  }

  /** Check that UDS database installed and the connection is established  */
  def isUdsDbReachable() = UdsRepository.isUdsDbReachable()

  /** Check Proline-Admin GUI configurations */
  def isAdminConfigsOk(adminConfig: AdminConfig): Map[Option[String], Boolean] = adminConfig match {
    case adminConfigValue @ AdminConfig(filePath, serverConfigFilePath, pwxConfigFilePath, pgsqlDataDir, seqRepoConfigFilePath, _, _, _, _, _, _) => {
      Map(
        Option(filePath) -> !(new File(filePath).exists),
        serverConfigFilePath -> (!serverConfigFilePath.isDefined || !(new File(serverConfigFilePath.get).exists)),
        pgsqlDataDir -> (!pgsqlDataDir.isDefined || !(new File(pgsqlDataDir.get).exists)),
        seqRepoConfigFilePath -> (!seqRepoConfigFilePath.isDefined || !(new File(seqRepoConfigFilePath.get).exists)))
    }
    case _ => logger.error("Proline-Admin config is not valid file!"); Map.empty
  }

  /** Check Proline-Admin GUI connection */
  def isConnectionEstablished(): Boolean = {
    DatabaseConnection.testDbConnection(getAdminConfigOpt().get, false, false)
  }

  /** Open Proline-Admin guide pdf file */
  def openAdminGuide() {
    UserGuide.openUrl(Monitor.targetPath + File.separator + "classes" + File.separator + "documentation" + File.separator + "Proline_AdminGuide_2.0.pdf")
  }

}