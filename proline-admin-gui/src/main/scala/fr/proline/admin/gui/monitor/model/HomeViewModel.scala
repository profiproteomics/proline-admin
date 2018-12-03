package fr.proline.admin.gui.monitor.model

import com.typesafe.scalalogging.LazyLogging

import scalafx.Includes._
import fr.proline.admin.gui.Monitor

import fr.proline.admin.gui.process.config.AdminConfig
import fr.proline.admin.gui.process.config.AdminConfigFile
import fr.proline.admin.gui.wizard.process.config.{ NodeConfig, ParsingRule }
import fr.proline.admin.gui.wizard.process.config.{ NodeConfigFile, ParsingRulesFile }
import fr.proline.admin.gui.process.{ UdsRepository, ProlineAdminConnection, DatabaseConnection }
import fr.proline.admin.gui.util.AdminGuide
import java.io.File

/**
 * Load Proline-Admin configurations and check UDS database connection.
 * @author aromdhani
 *
 */
class HomeViewModel(monitorConfPath: String) extends LazyLogging {

  /** Get Proline-Admin Config */
  def adminConfigOpt(): Option[AdminConfig] = {
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
  def serverNodeConfigOpt(): Option[NodeConfig] = {
    try {
      if (adminConfigOpt().isDefined) {
        if (adminConfigOpt().get.serverConfigFilePath.isDefined && new File(adminConfigOpt().get.serverConfigFilePath.get).exists) {
          val prolineServerConfigParent = new File(adminConfigOpt().get.serverConfigFilePath.get).getParent
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

  /** Check that UDS database is installed */
  def isUdsDbReachable() = UdsRepository.isUdsDbReachable()

  /** Check Proline-Admin GUI configurations */

  def isServerConfigFileOK(): Boolean = adminConfigOpt() match {
    case Some(AdminConfig(_, serverConfigFilePath, _, _, _, _, _, _, _, _, _)) if (serverConfigFilePath.isDefined && (new File(serverConfigFilePath.get).exists)) => true
    case _ => false
  }
  def isPgSQLDataDirOK(): Boolean = adminConfigOpt() match {
    case Some(AdminConfig(_, _, pgsqlDataDir, _, _, _, _, _, _, _, _)) if (pgsqlDataDir.isDefined && (new File(pgsqlDataDir.get).exists)) => true
    case _ => false
  }
  def isSeqRepoConfigFileOK(): Boolean = adminConfigOpt() match {
    case Some(AdminConfig(_, _, _, seqRepoConfigFilePath, _, _, _, _, _, _, _)) if (!seqRepoConfigFilePath.isDefined || !(new File(seqRepoConfigFilePath.get).exists)) => true
    case _ => false
  }

  /** Check Proline-Admin GUI UDS database connection */
  def isConnectionEstablished(): Boolean = {
    if (adminConfigOpt().isDefined)
      DatabaseConnection.testDbConnection(adminConfigOpt().get, false, false)
    else false
  }

  /** Open Proline-Admin guide */
  def openAdminGuide() {
    AdminGuide.openUrl(Monitor.targetPath + File.separator + "classes" + File.separator + "documentation" + File.separator + "Proline_AdminGuide_2.0.pdf")
  }

}