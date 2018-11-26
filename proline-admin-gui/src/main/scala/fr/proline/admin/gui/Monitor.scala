package fr.proline.admin.gui

import com.typesafe.scalalogging.LazyLogging

import javafx.application.Application
import scalafx.geometry.Insets
import scalafx.scene.Scene
import scalafx.scene.layout.HBox
import scalafx.scene.layout.VBox
import scalafx.scene.layout.Priority
import scalafx.stage.Stage

import fr.proline.admin.gui.monitor.view.HomePanel
import fr.proline.admin.gui.monitor.model.HomeViewModel
import fr.proline.admin.gui.monitor.database._
import fr.proline.admin.gui.process.UdsRepository
import fr.proline.admin.gui.task.TaskRunner
import fr.proline.admin.gui.wizard.util.Module
import fr.proline.admin.gui.util.FxUtils
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
  var mainPanel: VBox = _

  /* configuration file path */
  var targetPath: String = _
  var adminConfPath: String = _
  var taskRunner: TaskRunner = _

  /* Primary stage's root */
  lazy val root = new VBox {
    id = "root"
    vgrow = Priority.Always
    padding = Insets(10)
    children =
      Seq(mainPanel)
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
    Monitor.adminConfPath = _appConfPath
  }

  def start(stage: javafx.stage.Stage): Unit = {

    /* Create main window */
    val homeViewModel = new HomeViewModel(Monitor.adminConfPath)
    val homePanel = new HomePanel(homeViewModel)

    require(Monitor.stage == null, "stage is already instantiated")
    Monitor.mainPanel = homePanel
    Monitor.stage = new Stage(stage) {
      width = 1040
      height = 800
      scene = new Scene(Monitor.root)
      title = s"${Module.name} ${Module.version}"
    }

    /* Build and show stage */
    Monitor.stage.getIcons.add(FxUtils.newImageView(IconResource.IDENTIFICATION).image.value)
    Monitor.stage.scene.value.getStylesheets.add("/css/Style.css")
    Monitor.stage.show()
    /* Show notifications */
    Monitor.taskRunner = homePanel.taskRunner
    /* Load initial tables rows from UDS database */

  }

  /** Close UDSdb context on close application **/
  override def stop() {
    if (UdsRepository.getUdsDbContext() != null) UdsRepository.getUdsDbContext().close()
    if (UdsRepository.getDataStoreConnFactory() != null) UdsRepository.getDataStoreConnFactory().closeAll()
    super.stop()
  }

}