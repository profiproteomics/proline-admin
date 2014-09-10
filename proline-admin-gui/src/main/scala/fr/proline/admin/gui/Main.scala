package fr.proline.admin.gui

import fr.proline.admin.gui.component.panel.MenuPanel
import fr.proline.admin.service.db.SetupProline
import scalafx.geometry.Insets
import scalafx.scene.Scene
import scalafx.scene.control.ScrollPane
import scalafx.scene.web.WebView
import scalafx.scene.layout.HBox
import scalafx.scene.layout.Priority
import scalafx.scene.layout.VBox
import scalafx.stage.Stage
import javafx.application.Application
import fr.proline.admin.gui.component.panel.ConsolePanel
import fr.proline.admin.gui.component.panel.ButtonsPanel

/**
 * Graphical interface for Proline Admin.
 */
object Main {

  //TODO: splash screen

  //  /** CSS */
  //  val CSS = this.getClass().getResource("/ProlineAdminCSS.css").toExternalForm()

  /** Panels */
  val menuPanel = MenuPanel()

  //  var consolePanel: WebView = _
  import scalafx.scene.layout.StackPane
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

    /** Console and buttons */
    //from javaFX doc: “WebView objects must be created and accessed solely from the FX thread.”
    Main.consolePanel = ConsolePanel()
    Main.buttonsPanel = ButtonsPanel()

    /** Primary stage */
    Main.stage = new Stage(stage) {
      scene = new Scene(Main.root)

      width = 1224
      height = 400
      minWidth = 720
      minHeight = 384

      try {
        val dataDir = SetupProline.config.dataDirectory
        title = s"Proline Admin @ $dataDir"
      } catch {
        case e: Exception => title = s"Proline Admin (invalid configuration)"
      }
    }

    /** Show stage */
    Main.stage.show()

  }

}
