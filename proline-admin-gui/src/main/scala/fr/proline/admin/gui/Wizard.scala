package fr.proline.admin.gui

import com.typesafe.scalalogging.LazyLogging

import java.io.File
import java.io.FilenameFilter

import javafx.application.Application

import scalafx.geometry.Insets
import scalafx.scene.Scene
import scalafx.scene.layout.HBox
import scalafx.scene.layout.Priority
import scalafx.scene.layout.StackPane
import scalafx.scene.layout.VBox
import scalafx.stage.Stage
import fr.profi.util.StringUtils
import scala.util.matching.Regex
import scala.collection.mutable.Set
import scalafx.scene.Node
import scala.collection.mutable.LinkedHashMap
import fr.proline.admin.gui.wizard.component.HomeButtonsPanel
import fr.proline.admin.gui.wizard.component.ItemsPanel
import fr.proline.admin.gui.wizard.component.Item
import fr.proline.admin.gui.util.FxUtils
import fr.proline.admin.gui.wizard.util.version.Module

/**
 * Graphical interface to setup/update Proline configuration files (application.conf)
 *
 */
object Wizard extends LazyLogging {

  /* Configuration files and dirs */

  var targetPath: String = _
  var adminConfPath: String = _
  var serverConfPath: String = _
  var jmsNodeConfPath: String = _
  var webRootPath: String = _
  var pgDataDirPath: String = _
  var seqRepoConfPath: String = _
  var SeqJmsNodeConfPath: String = _
  var parsingRulesPath: String = _
  var userName: String = _
  var passWord: String = _
  var hostName: String = _
  var jmsHostName: String = _
  var prolineQueueName: String = _
  var port: Int = 5432
  var jmsPort: Int = 5445
  var nodeIndex = 0
  var currentNode: Item = _
  var items: LinkedHashMap[String, Option[Item]] = LinkedHashMap.empty[String, Option[Item]]

  /*
   *  main panel contains :
   * 	panel of buttons :go and cancel
   * 	panel of items
   * 
   */

  var configItemsPanel: VBox = _
  var buttonsPanel: VBox = _

  /*Primary stage's root */
  lazy val root = new VBox {
    id = "root"
    content = new VBox {
      vgrow = Priority.Always
      padding = Insets(10)
      spacing = 40
      content = Seq(configItemsPanel, buttonsPanel)
    }
  }

  var stage: scalafx.stage.Stage = null

  /* Launch application and display the first  window. */
  def main(args: Array[String]) = {
    Application.launch(classOf[Wizard])
  }
}

class Wizard extends Application {

  def start(stage: javafx.stage.Stage): Unit = {

    /* Locate Proline Admin 'config' folder */

    val srcPath = this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI()
    Wizard.targetPath = new File(srcPath).getParent().replaceAll("\\\\", "/")
    val configPath = Wizard.targetPath + """/config/"""

    // Locate application.CONF file and update Proline config in consequence 

    val _appConfPath = configPath + "application.conf"
    Wizard.adminConfPath = _appConfPath

    // locate application .CONF file of proline server 

    require(Wizard.stage == null, "stage is already instantiated")

    Wizard.configItemsPanel = ItemsPanel
    Wizard.buttonsPanel = HomeButtonsPanel()
    Wizard.stage = new Stage(stage) {
      scene = new Scene(Wizard.root)
      minWidth = 750
      width = 1050
      minHeight = 600
      height = 850
      title = s"${Module.name} ${Module.version}"
    }

    Wizard.stage.getIcons.add(FxUtils.newImageView(IconResource.IDENTIFICATION).image.value)
    Wizard.stage.scene.value.getStylesheets.add("/css/Style.css")
    Wizard.stage.show()
  }
}