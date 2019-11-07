package fr.proline.admin.gui.install.model

import com.typesafe.scalalogging.LazyLogging

import fr.proline.admin.gui.Install
import fr.proline.admin.gui.install.view.jms.ServerJmsPanel
import fr.proline.admin.gui.process.config.NodeConfig
import fr.proline.admin.gui.process.config.NodeConfigFile
import java.io.File

/**
 * The JMS server model view. Defines UI actions to update jms-node parameters.
 *
 * @author aromdhani
 *
 */

class JmsModelView(jmsConfigFilePath: String) extends LazyLogging {

  /** Return jms-node server configuration  */
  def nodeConfig(): Option[NodeConfig] = {
    try {
      val nodeConfigFile = new NodeConfigFile(jmsConfigFilePath)
      val nodeConfigOpt: Option[NodeConfig] = nodeConfigFile.read
      nodeConfigOpt
    } catch {
      case ex: Exception =>
        logger.error("Error while trying to retrieve jms server parameters from configuartion file", ex.getMessage())
        None
    }
  }
  /** Save modification in jms-node configuration file */
  def onSaveJmsConfig(nodeConfig: NodeConfig): Unit = {
    try {
      logger.debug("Update jms-node configurations...")
      val nodeConfigFile = new NodeConfigFile(jmsConfigFilePath)
      nodeConfigFile.write(nodeConfig)
    } catch {
      case ex: Exception => logger.error("Error while trying to update jmls-node configurations", ex.getMessage)
    }
  }
}