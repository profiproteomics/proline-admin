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
import fr.proline.admin.gui.monitor.model.HomePanelViewModel
import fr.proline.admin.gui.monitor.database._
import fr.proline.admin.gui.process.UdsRepository
import fr.proline.admin.gui.task.TaskRunner
import fr.proline.admin.gui.util.FxUtils
import fr.profi.util.StringUtils
import java.io.File

/**
 *  Graphical user interface of Proline-Admin Monitor.
 * @author aromdhani
 *
 */

object Monitor extends LazyLogging {

  /* Panels */
  var mainPanel: VBox = _

  /* Configuration file path */
  var targetPath: String = _
  var adminConfPath: String = _
  /* Task runner */
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
    val homeViewModel = new HomePanelViewModel(Monitor.adminConfPath)
    val homePanel = new HomePanel(homeViewModel)

    require(Monitor.stage == null, "Stage is already instantiated!")
    Monitor.mainPanel = homePanel
    val admin = new Version()
    Monitor.stage = new Stage(stage) {
      width = 1040
      minWidth = 700
      height = 750
      minHeight = 700
      scene = new Scene(Monitor.root)
      title = s"${admin.getModuleName} ${admin.getVersion.split("_").apply(0)}"
    }

    /* Build and display stage */
    Monitor.stage.getIcons.add(FxUtils.newImageView(IconResource.IDENTIFICATION).image.value)
    Monitor.stage.scene.value.getStylesheets.add("/css/Style.css")
    Monitor.stage.show()
    // Initialize task runner
    Monitor.taskRunner = homePanel.taskRunner
    //TODO Load initial tables rows from UDS database

  }

  /** Close UDSdb context on close application **/
  override def stop() {
    if (UdsRepository.getUdsDbContext() != null) UdsRepository.getUdsDbContext().close()
    if (UdsRepository.getDataStoreConnFactory() != null) UdsRepository.getDataStoreConnFactory().closeAll()
    super.stop()
  }

}