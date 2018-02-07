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

/**
 * Graphical interface for Proline Admin Monitor .
 */

object Monitor extends LazyLogging {

  /* Panels */
  var itemsPanel: VBox = _
  var buttonsPanel: VBox = _
  var targetPath: String = _
  var adminConfPath: String = _
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
class Monitor extends Application {

  def start(stage: javafx.stage.Stage): Unit = {

    require(Monitor.stage == null, "stage is already instantiated")
    Monitor.itemsPanel = MonitorPane
    Monitor.buttonsPanel = MonitorBottomsPanel
    Monitor.stage = new Stage(stage) {
      width = 1024
      minWidth = 700
      height = 780
      minHeight = 650
      scene = new Scene(Monitor.root)
      title = s"${Module.name} ${Module.version}"
    }
    /* Locate 'config' folder */
    val srcPath = this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI()
    Monitor.targetPath = new File(srcPath).getParent().replaceAll("\\\\", "/")
    val configPath = Monitor.targetPath + """/config/"""

    /* Locate application.CONF file and update Proline config in consequence */
    val _appConfPath = configPath + "application.conf"

    /* Usual case : default conf file exists */
    if (new File(_appConfPath).exists()) {
      Monitor.adminConfPath = _appConfPath
      ProlineAdminConnection._setNewProlineConfigMonitor()
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