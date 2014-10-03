package fr.proline.admin.gui

import java.io.File
import com.typesafe.scalalogging.slf4j.Logging
import fr.proline.admin.gui.component.dialog.ConfFileChooser
import fr.proline.admin.gui.component.dialog.ConfirmationDialog
import fr.proline.admin.gui.component.panel.ButtonsPanel
import fr.proline.admin.gui.component.panel.ConsolePanel
import fr.proline.admin.gui.component.panel.MenuPanel
import fr.proline.admin.gui.process.ProlineAdminConnection
import fr.proline.admin.gui.process.UdsRepository
import scalafx.geometry.Insets
import scalafx.scene.Scene
import scalafx.scene.layout.HBox
import scalafx.scene.layout.Priority
import scalafx.scene.layout.StackPane
import scalafx.scene.layout.VBox
import scalafx.stage.Stage
import javafx.application.Application


/**
 * Graphical interface for Proline Admin.
 */
object Main extends Logging {

  //TODO: splash screen

  //  /** CSS */
  //  val CSS = this.getClass().getResource("/ProlineAdminCSS.css").toExternalForm()

  /** PAdmin configuration file */
  var targetPath: String = _
  var confPath: String = _
  var firstCallToDataDir = true 
  
  /** Panels */
  val menuPanel = MenuPanel()
  //    val menuPanel = MenuPanel2()
  var consolePanel: StackPane = _
  var buttonsPanel: VBox = _
  
  /** Primary stage's root */
  lazy val root = new VBox {
    id = "root"
    content = List(
      menuPanel,
      new HBox {
        vgrow = Priority.ALWAYS
        padding = Insets(10)
        spacing = 20
        content = List(buttonsPanel, consolePanel)
      }
    )
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

    /** Create custom console and redirect system outputs on it */
    Main.consolePanel = ConsolePanel() //javaFX doc: “WebView objects must be created and accessed solely from the FX thread.”
    Main.buttonsPanel = ButtonsPanel()

    Main.stage = new Stage(stage) {
      scene = new Scene(Main.root)
      width = 1224
      height = 400
      minWidth = 720
      minHeight = 384
    }

    /** Locate 'config' folder */
    val srcPath = this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI()
    Main.targetPath = new File(srcPath).getParent().replaceAll("\\\\", "/")
    val configPath = Main.targetPath + """/config/"""

    /** Build and show stage (in any case) */
    Main.stage.show()

    /** Locate CONF file and update Proline config in consequence */
    val _appConfPath = configPath + "application.conf"

    // Usual case : default conf file exists
    if (new File(_appConfPath).exists()) {
      Main.confPath = _appConfPath
      ProlineAdminConnection.loadProlineConf()

      // Choose one if not
    } else {
      var isFileChosen = false

      while (isFileChosen == false) {

        try {
          ConfFileChooser.showIn(new Stage) //auto-updates proline conf  
          isFileChosen = true

        } catch {

          /** If the user doesn't select any file ('cancel' or 'close' button) */
          case e: Exception =>

            //TODO: use ChoiceDialog
            val exitOrContinueDialog = new ConfirmationDialog(
              "Configuration file required to continue",
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
    //    /** Build and show stage */
    //    Main.stage.show()

  }

  override def stop() {
    super.stop()
    if (UdsRepository.getUdsDbContext() != null) UdsRepository.getUdsDbContext().close
  }

}
