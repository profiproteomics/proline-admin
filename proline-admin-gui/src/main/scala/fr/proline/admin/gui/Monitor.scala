package fr.proline.admin.gui

import com.typesafe.scalalogging.LazyLogging

import java.io.File

import javafx.application.Application

import scalafx.geometry.Insets
import scalafx.scene.Scene
import scalafx.scene.layout.HBox
import scalafx.scene.layout.Priority
import scalafx.scene.layout.StackPane
import scalafx.scene.layout.VBox
import scalafx.stage.Stage

import fr.proline.admin.gui.process.UdsRepository
import fr.proline.admin.gui.wizard.util.Module
import fr.proline.admin.gui.util.FxUtils

import fr.proline.admin.gui.wizard.component.panel.bottom.MonitorBottomsPanel
import fr.proline.admin.gui.wizard.component.panel.main.MonitorPane
import fr.proline.admin.gui.wizard.util.WindowSize
import fr.proline.admin.gui.process.ProlineAdminConnection
import fr.proline.admin.gui.process.UdsRepository
import javafx.beans.property.DoubleProperty
import javafx.beans.property.ReadOnlyDoubleProperty

import fr.proline.admin.gui.process.config.AdminConfigFile
import fr.proline.admin.gui.process.config.AdminConfig
import fr.proline.admin.gui.wizard.process.config.NodeConfigFile
import fr.proline.admin.gui.wizard.process.config.NodeConfig

import scala.concurrent.{ Future }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{ Failure, Success }

/**
 * Graphical interface for Proline Admin Monitor .
 */

object Monitor extends LazyLogging {

  /* Panels */
  var itemsPanel: VBox = _
  var buttonsPanel: VBox = _
  var targetPath: String = _
  var adminConfPath: String = _
  var jmsConfigPath: String = _
  var serverInitialConfing: Option[AdminConfig] = _
  var serverJmsInitialConfig: Option[NodeConfig] = _

  /* Primary stage's root */
  lazy val root = new VBox {
    id = "root"
    children = new VBox {
      vgrow = Priority.Always
      padding = Insets(10)
      spacing = 20
      children = List(itemsPanel, buttonsPanel)
    }
  }
  var stage: scalafx.stage.Stage = null
  /** Launch application and display main window. */
  def main(args: Array[String]) = {
    Application.launch(classOf[Monitor])
  }

}
class Monitor extends Application with LazyLogging {

  override def init: Unit = {
    /* Locate 'config' folder */
    val srcPath = this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI()
    Monitor.targetPath = new File(srcPath).getParent().replaceAll("\\\\", "/")
    val configPath = Monitor.targetPath + File.separator + """config""" + File.separator
    /* Locate application.CONF file and update Proline config in consequence */
    val _appConfPath = configPath + "application.conf"
    /* Usual case : default conf file exists */
    if (new File(_appConfPath).exists()) {
      Monitor.adminConfPath = _appConfPath
      try {
        Monitor.serverInitialConfing = getProlineServerInitialConfig
        Monitor.serverJmsInitialConfig = getJmsServerInitialConfig
        prolineAdminConnection onSuccess {
          case result => logger.trace("Connection to Proline Database Server is valid.")
        }
        prolineAdminConnection onFailure {
          case error => logger.error("Connection to Proline Database Server is invalid.")
        }
      } catch {
        case t: Throwable => logger.error("Error while trying to get Proline server initial configurations.")
      }
    } else {
      logger.warn("application.conf file does not exist!")
    }
  }
  val prolineAdminConnection: Future[Unit] = Future { ProlineAdminConnection._setNewProlineConfigMonitor() }

  /** get Proline Server and Proline JMS Server properties */
  def getProlineServerInitialConfig(): Option[AdminConfig] = {
    val adminConfFile = new AdminConfigFile(Monitor.adminConfPath)
    val adminConfigOpt = adminConfFile.read()
    require(adminConfigOpt.isDefined, "admin config is undefined.Make sure that Proline configuration file(application.conf) exists.")
    adminConfigOpt
  }

  /**get Proline Jms server initial properties */

  def getJmsServerInitialConfig(): Option[NodeConfig] = {
    val initialAdminConfigOpt = getProlineServerInitialConfig()
    if (initialAdminConfigOpt.isDefined) {
      if (initialAdminConfigOpt.get.serverConfigFilePath.isDefined && initialAdminConfigOpt.get.serverConfigFilePath.get.trim() != "") {
        val prolineServerConfigParent = new File(initialAdminConfigOpt.get.serverConfigFilePath.get).getParent
        Monitor.jmsConfigPath = new File(prolineServerConfigParent + File.separator + "jms-node.conf").getCanonicalPath
      } else {
        logger.error("the path to proline server configuration file is empty!")
      }
    }
    //Jms Server properties
    val nodeConfigFile = new NodeConfigFile(Monitor.jmsConfigPath)
    val nodeConfigOpt: Option[NodeConfig] = nodeConfigFile.read
    require(nodeConfigOpt.isDefined, "Jms-node Config is undefined. Make sure that Proline JMS server configuration file exists.")
    nodeConfigOpt
  }

  def start(stage: javafx.stage.Stage): Unit = {

    require(Monitor.stage == null, "stage is already instantiated")
    Monitor.itemsPanel = MonitorPane
    Monitor.buttonsPanel = MonitorBottomsPanel
    Monitor.stage = new Stage(stage) {
      width = 1024
      minWidth = 700
      height = 780
      minHeight = 680
      scene = new Scene(Monitor.root)
      title = s"${Module.name} ${Module.version}"
    }
    /* Build and show stage (in any case) */
    Monitor.stage.getIcons.add(FxUtils.newImageView(IconResource.IDENTIFICATION).image.value)
    Monitor.stage.scene.value.getStylesheets.add("/css/Style.css")
    Monitor.stage.show()
  }

  /** Close UDSdb context on application close **/
  override def stop() {
    super.stop()
  }

}