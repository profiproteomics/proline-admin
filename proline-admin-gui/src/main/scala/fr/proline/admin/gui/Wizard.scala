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
import fr.proline.admin.gui.wizard.component.ButtonsPanel
import fr.proline.admin.gui.wizard.component.ItemsPanel
import fr.proline.admin.gui.wizard.component.Item
import fr.proline.admin.gui.util.FxUtils

/**
 * Graphical interface for Proline-Admin wizard:quick edit for admin and server file (.conf).
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
  var items: LinkedHashMap[String, Item] = LinkedHashMap.empty[String, Item]

  /*
   * Panels:
   * 	panel of buttons :go and cancel
   * 	panel of views
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

    /* Locate 'config' folder */

    val srcPath = this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI()
    Wizard.targetPath = new File(srcPath).getParent().replaceAll("\\\\", "/")
    val configPath = Wizard.targetPath + """/config/"""

    // Locate application.CONF file and update Proline config in consequence 

    val _appConfPath = configPath + "application.conf"
    Wizard.adminConfPath = _appConfPath

    // locate application .CONF file of proline server 

    require(Wizard.stage == null, "stage is already instantiated")

    Wizard.configItemsPanel = ItemsPanel
    Wizard.buttonsPanel = ButtonsPanel()

    Wizard.stage = new Stage(stage) {
      scene = new Scene(Wizard.root)
      width = 1050
      height = 850
      minWidth = 650
      minHeight = 800
      title = "Proline Admin wizard"
    }
    Wizard.stage.getIcons.add(FxUtils.newImageView(IconResource.IDENTIFICATION).image.value)
    Wizard.stage.scene.value.getStylesheets.add("/css/Style.css")
    Wizard.stage.show()
  }
}