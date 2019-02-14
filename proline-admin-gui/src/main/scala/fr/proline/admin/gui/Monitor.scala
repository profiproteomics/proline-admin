package fr.proline.admin.gui

import com.typesafe.scalalogging.LazyLogging

import javafx.application.Application
import scalafx.geometry.Insets
import scalafx.scene.Scene
import scalafx.scene.layout.HBox
import scalafx.scene.layout.VBox
import scalafx.scene.layout.Priority
import scalafx.scene.layout.StackPane
import scalafx.stage.Stage

import fr.proline.admin.gui.wizard.component.panel.bottom.MonitorBottomsPanel
import fr.proline.admin.gui.wizard.component.panel.main.MonitorPane
import fr.proline.admin.gui.process.ProlineAdminConnection
import fr.proline.admin.gui.process.UdsRepository
import fr.proline.admin.gui.util.FxUtils
import fr.proline.admin.gui.wizard.util.Module
import fr.profi.util.StringUtils
import java.io.File

/**
 * Proline-Admin Monitor.
 *
 * @author aromdhani
 *
 */

object Monitor extends LazyLogging {

  /* Panels */
  var itemsPanel: VBox = _
  var buttonsPanel: VBox = _

  /* configuration file path */
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

  /** utilities */
  def adminConfPathIsEmpty(): Boolean = StringUtils.isEmpty(adminConfPath)

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
    /* Locate application.CONF file */
    val _appConfPath = configPath + "application.conf"
    /* Usual case : default conf file exists */
    require(new File(_appConfPath).exists(), "The configuration file application.conf does not exists!")
    Monitor.adminConfPath = _appConfPath

  }

  def start(stage: javafx.stage.Stage): Unit = {

    require(Monitor.stage == null, "stage is already instantiated")
    Monitor.itemsPanel = MonitorPane
    Monitor.buttonsPanel = MonitorBottomsPanel
    // Version 
    val module = new Version
    Monitor.stage = new Stage(stage) {
      width = 1050
      minWidth = 700
      height = 780
      minHeight = 680
      scene = new Scene(Monitor.root)
      title = s"${module.getModuleName} ${module.getVersion}"
    }
    /* Build and show stage */
    Monitor.stage.getIcons.add(FxUtils.newImageView(IconResource.IDENTIFICATION).image.value)
    Monitor.stage.scene.value.getStylesheets.add("/css/Style.css")
    Monitor.stage.show()
    /* load initial configurations */

  }

  /** Close UDSdb context on application close **/
  override def stop() {
    if (UdsRepository.getUdsDbContext() != null) UdsRepository.getUdsDbContext().close()
    if (UdsRepository.getDataStoreConnFactory() != null) UdsRepository.getDataStoreConnFactory().closeAll()
    super.stop()
  }

}