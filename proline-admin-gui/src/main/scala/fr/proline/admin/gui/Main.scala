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

import fr.proline.admin.gui.component.ButtonsPanel
import fr.proline.admin.gui.component.ConsolePanel
import fr.proline.admin.gui.component.configuration.file.ProlineConfigFileChooser
import fr.proline.admin.gui.process.ProlineAdminConnection
import fr.proline.admin.gui.process.UdsRepository
import fr.proline.admin.gui.process.config.AdminConfigFile
import fr.proline.admin.gui.util.ConfirmationDialog

import fr.profi.util.StringUtils

/**
 * Graphical interface for Proline Admin.
 */
object Main extends LazyLogging {

  //TODO: splash screen

  //  /** CSS */
  //  val CSS = this.getClass().getResource("/ProlineAdminCSS.css").toExternalForm()

  /* Configuration files and dirs */
  var targetPath: String = _
  var adminConfPath: String = _
  var serverConfPath: String = _
  var pwxConfPath: String = _
  var postgresqlDataDir: String = _
  var seqRepoConfPath: String = _
  
  var firstCallToDataDir = true

  /** Utilities **/
  def adminConfPathIsEmpty(): Boolean = StringUtils.isEmpty(adminConfPath)
  def serverConfPathIsEmpty(): Boolean = StringUtils.isEmpty(serverConfPath)
  def pwxConfPathIsEmpty(): Boolean = StringUtils.isEmpty(pwxConfPath)
  def postgresDataDirIsEmpty(): Boolean = StringUtils.isEmpty(postgresqlDataDir)

  def getAdminConfigFile(): Option[AdminConfigFile] = {
    if (adminConfPathIsEmpty()) return None
    else Option(new AdminConfigFile(Main.adminConfPath))
  }
  
  /* Panels */
  var consolePanel: StackPane = _
  var buttonsPanel: VBox = _

  /* Primary stage's root */
  lazy val root = new VBox {
    id = "root"
    children = new HBox {
      vgrow = Priority.Always
      padding = Insets(10)
      spacing = 20
      children = List(buttonsPanel, consolePanel)
    }
  }

  var stage: scalafx.stage.Stage = null

  /** Launch application and display main window. */
  def main(args: Array[String]) = {
    Application.launch(classOf[Main])
  }

}

class Main extends Application {

  def start(stage: javafx.stage.Stage): Unit = {

    require(Main.stage == null, "stage is already instantiated")

    /* Create custom console and redirect system outputs on it */
    Main.consolePanel = ConsolePanel() //javaFX doc: “WebView objects must be created and accessed solely from the FX thread.”
    Main.buttonsPanel = ButtonsPanel()

    Main.stage = new Stage(stage) {
      scene = new Scene(Main.root)
      width = 1224
      height = 400
      minWidth = 720
      minHeight = 384
      title = "Proline Admin"
    }

    /* Locate 'config' folder */
    val srcPath = this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI()
    Main.targetPath = new File(srcPath).getParent().replaceAll("\\\\", "/")
    val configPath = Main.targetPath + """/config/"""

    /* Build and show stage (in any case) */
    Main.stage.show()

    /* Locate application.CONF file and update Proline config in consequence */
    val _appConfPath = configPath + "application.conf"

    /* Usual case : default conf file exists */
    if (new File(_appConfPath).exists()) {
      Main.adminConfPath = _appConfPath
      ProlineAdminConnection.loadProlineConf(verbose = false)
    }
    
    /* Choose one if not */
    else {
      var isFileChosen = false

      while (isFileChosen == false) {

        try {
          ProlineConfigFileChooser.showIn(new Stage) //auto-updates proline conf  
          isFileChosen = true

        } catch {

          /* If the user doesn't select any file ('cancel' or 'close' button) */
          case e: Exception =>

            //TODO: use ChoiceDialog
            val exitOrContinueDialog = new ConfirmationDialog(
              "Configuration file is required to go further",
              "You must specify Proline's configuration file to run Proline Admin.\nDo you want to exit Proline Admin?"
            )
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

    /* Try to find server config file path and PostgreSQL data dir from config */
    // can't reach this code if adminConfPath isn't set 
    //if (ScalaUtils.isEmpty(Main.adminConfPath) == false) {
    val adminConfigFile = new AdminConfigFile(Main.adminConfPath)
    adminConfigFile.getServerConfigPath().map{ Main.serverConfPath = _ }
    adminConfigFile.getPwxConfigPath().map{ Main.pwxConfPath = _ }
    adminConfigFile.getPostgreSqlDataDir().map{ Main.postgresqlDataDir = _ }
    adminConfigFile.getSeqRepoConfigPath().map{ Main.seqRepoConfPath = _ }
    //}
  }

  /** Close UDSdb context on application close **/
  override def stop() {
    super.stop()
    if (UdsRepository.getUdsDbContext() != null) UdsRepository.getUdsDbContext().close
  }

}