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

import fr.proline.admin.gui.component.ButtonsPanelQStart
import fr.proline.admin.gui.component.configuration.file.ProlineConfigFileChooser
import fr.proline.admin.gui.component.wizard.ProlineConfigFileChooserWizard
import fr.proline.admin.gui.process.ProlineAdminConnectionWizard
import fr.proline.admin.gui.process.UdsRepository
import fr.proline.admin.gui.process.config.AdminConfigFile
import fr.proline.admin.gui.util.ConfirmationDialog
import fr.proline.admin.gui.component.wizard._

/**
 * Graphical interface for Proline-Admin wizard:quick edit for admin and server file (.conf).
 */
object QuickStart extends LazyLogging {

  /* Configuration files and dirs */
  var targetPath: String = _
  var adminConfPath: String = _
  var serverConfPath: String = _
  var postgresqlDataDir: String = _
  var seqRepoConfPath: String = _
  var userName: String = _
  var passwordUser: String = _
  var hostNameUser: String = _
  var port: Int = 5432
  var firstCallToDataDir = true

  def adminConfPathIsEmpty(): Boolean = StringUtils.isEmpty(adminConfPath)
  def serverConfPathIsEmpty(): Boolean = StringUtils.isEmpty(serverConfPath)
  def postgresDataDirIsEmpty(): Boolean = StringUtils.isEmpty(postgresqlDataDir)

  /* parse Admin config file */

  def getAdminConfigFile(): Option[AdminConfigFile] = {
    if (adminConfPathIsEmpty()) return None
    else Option(new AdminConfigFile(QuickStart.adminConfPath))
  }

  /*
   * Panels:
   * panel of buttons :next ,previous and cancel
   * panel of views   
   */
  var buttonsPanel: HBox = _
  var mainPanel: VBox = _
  var panelState: String = null

  /*Primary stage's root */
  lazy val root = new VBox {
    id = "root"
    content = new VBox {
      vgrow = Priority.Always
      padding = Insets(10)
      spacing = 40
      content = Seq(mainPanel, buttonsPanel)
    }
  }

  var stage: scalafx.stage.Stage = null

  /* Launch application and display the first  window. */
  def main(args: Array[String]) = {
    Application.launch(classOf[QuickStart])
  }
}

class QuickStart extends Application {

  def start(stage: javafx.stage.Stage): Unit = {

    /* Locate 'config' folder */

    val srcPath = this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI()
    QuickStart.targetPath = new File(srcPath).getParent().replaceAll("\\\\", "/")
    val configPath = QuickStart.targetPath + """/config/"""

    // Locate application.CONF file and update Proline config in consequence 

    val _appConfPath = configPath + "application.conf"
    QuickStart.adminConfPath = _appConfPath

    // locate application .CONF file of proline server 

    require(QuickStart.stage == null, "stage is already instantiated")
    val prolineConfigFilesPanel = new ProlineConfigFilesPanelQStart()
    prolineConfigFilesPanel.validStep()
    QuickStart.mainPanel = prolineConfigFilesPanel
    QuickStart.buttonsPanel = ButtonsPanelQStart()
    QuickStart.panelState = "panelConfig"
    QuickStart.stage = new Stage(stage) {
      scene = new Scene(QuickStart.root)
      width = 850
      height = 570
      minWidth = 620
      minHeight = 570
      title = "Proline Admin wizard"
    }

    QuickStart.stage.show()

    // .conf files exists 

    if (new File(_appConfPath).exists()) {
      QuickStart.adminConfPath = _appConfPath
      val adminConfigOpt = new AdminConfigFile(QuickStart.adminConfPath).read()
    } else {
      var isFileChosen = false

      while (isFileChosen == false) {
        try {
          ProlineConfigFileChooserWizard.showIn(new Stage) //auto-updates proline .conf  
          isFileChosen = true

        } catch {

          // If the user doesn't select any file ('cancel' or 'close' button) 
          case e: Exception =>

            //TODO: use ChoiceDialog
            val exitOrContinueDialog = new ConfirmationDialog(
              "Configuration file is required to go further",
              "You must specify Proline's configuration file to run Proline Admin.\nDo you want to exit Proline Admin?")
            exitOrContinueDialog.setYesButtonText("Exit Proline Admin")
            exitOrContinueDialog.setCancelButtonText("Continue")
            exitOrContinueDialog.showIn(new Stage)
            val quitApp = exitOrContinueDialog.isActionConfirmed

            if (quitApp == true) {
              System.exit(1)
            }
        }
      }
    }
  }

}