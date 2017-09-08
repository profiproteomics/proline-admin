package fr.proline.admin.gui.util

import com.typesafe.scalalogging.LazyLogging
import javafx.scene.layout.Priority
import scalafx.Includes._
import scalafx.geometry.Insets
import scalafx.geometry.Pos
import scalafx.scene.Scene
import scalafx.scene.control.Button
import scalafx.scene.control.TabPane
import scalafx.scene.input.KeyEvent
import scalafx.scene.layout.HBox
import scalafx.scene.layout.VBox
import scalafx.stage.Modality
import scalafx.stage.Stage
import fr.proline.admin.gui.Main
import fr.proline.admin.gui.component.configuration.tab._
import fr.profi.util.scalafx.ScalaFxUtils

/**
 * ******************************* *
 * Common model for tabbed windows *
 * ******************************* *
 */
abstract class AbstractTabbedWindow extends Stage with LazyLogging {

  /* Stage's properties */
  implicit val thisWindow = this

  initModality(Modality.WINDOW_MODAL)
  initOwner(Main.stage)
  width = 1068
  height = 768
  /*
   * ********** *
   * COMPONENTS *
   * ********** *
   */

  val tabPanel = new TabPane {
    vgrow = Priority.ALWAYS
  }

  protected val okButton = new Button("OK") {
    onAction = handle {
      try { runOnOkPressed() }
      catch {
        case ade: java.nio.file.AccessDeniedException => System.out.println("[Error] - Access denied, you should have administrator rights to edit configuration files")
      }
    }
  }
  protected val cancelButton = new Button("Cancel") { onAction = handle { thisWindow.close() } }

  /*
   * ****** *
   * LAYOUT *
   * ****** *
   */
  scene = new Scene {
    onKeyPressed = (ke: KeyEvent) => { ScalaFxUtils.closeIfEscapePressed(thisWindow, ke) }

    root = new VBox {
      hgrow = Priority.ALWAYS
      vgrow = Priority.ALWAYS
      children = Seq(
        tabPanel,
        new HBox {
          padding = Insets(5)
          alignment = Pos.BottomRight
          spacing = 15
          children = Seq(okButton, cancelButton)
        })
    }
  }

  /** Action to run when "OK" is pressed **/
  protected def runOnOkPressed(): Unit
}