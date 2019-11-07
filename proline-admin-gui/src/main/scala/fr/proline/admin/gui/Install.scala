

package fr.proline.admin.gui

import com.typesafe.scalalogging.LazyLogging

import scalafx.stage.Stage
import scalafx.scene.Scene
import javafx.application.Application
import scalafx.geometry.{ Insets, Pos }
import scalafx.scene.layout.{ HBox, VBox, Priority }

import fr.proline.admin.gui.install.view.HomePanel
import fr.proline.admin.gui.install.model.HomePanelViewModel
import fr.proline.admin.gui.process.UdsRepository
import fr.proline.admin.gui.util.FxUtils
import fr.proline.admin.gui.task.TaskRunner

import fr.profi.util.StringUtils
import java.io.File

/**
 * Graphical user interface of Proline-Admin Install.
 * @author aromdhani
 *
 */
object Install extends LazyLogging {

  /* Panels */
  var mainPanel: VBox = _
  /* Configuration file path */
  var targetPath: String = _
  var adminConfPath: String = _
  var serverConfPath: String = _
  var serverJmsPath: String = _
  var seqReposConfPath: String = _
  var seqReposParsigRulesPath: String = _
  var seqReposJmsPath: String = _
  var pwxConfPath: String = _
  var pgDataDirPath: String = _
  /* Task runner */
  var taskRunner: TaskRunner = _

  /*Primary stage's root */
  lazy val root = new VBox {
    id = "root"
    children = new VBox {
      alignment = Pos.CENTER
      vgrow = Priority.Always
      padding = Insets(10)
      children = Seq(mainPanel)
    }
  }

  var stage: scalafx.stage.Stage = null

  /** utilities */
  def adminConfPathIsEmpty(): Boolean = StringUtils.isEmpty(adminConfPath)
  /** Launch application and display main window. */
  def main(args: Array[String]) = {
    Application.launch(classOf[Install])
  }
}

class Install extends Application {

  override def init: Unit = {
    /* Locate 'config' folder */
    val srcPath = this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI()
    Install.targetPath = new File(srcPath).getParent().replaceAll("\\\\", "/")
    val configPath = Install.targetPath + File.separator + """config""" + File.separator

    /* Locate application.CONF file */
    val _appConfPath = configPath + "application.conf"
    Install.adminConfPath = _appConfPath
  }

  def start(stage: javafx.stage.Stage): Unit = {

    // Create Home panel
    val homeViewModel = new HomePanelViewModel(Install.adminConfPath)
    val homePanel = new HomePanel(homeViewModel)
    // Create stage
    require(Install.stage == null, "Stage is already instantiated!")
    Install.mainPanel = homePanel
    val admin = new Version()
    Install.stage = new Stage(stage) {
      width = 1040
      minWidth = 700
      height = 750
      minHeight = 700
      scene = new Scene(Install.root)
      title = s"${admin.getModuleName} ${admin.getVersion.split("_").apply(0)}"
    }
    // Set icon and style
    Install.stage.getIcons.add(FxUtils.newImageView(IconResource.IDENTIFICATION).image.value)
    Install.stage.scene.value.getStylesheets.add("/css/Style.css")
    Install.stage.show()
    // Initialize task runner

  }

  override def stop() {
    // Close UDS db context
    if (UdsRepository.getUdsDbContext() != null) UdsRepository.getUdsDbContext().close()
    if (UdsRepository.getDataStoreConnFactory() != null) UdsRepository.getDataStoreConnFactory().closeAll()
    super.stop()
  }
}