package fr.proline.admin.gui.component.wizard

import scalafx.Includes._
import scalafx.geometry.Insets
import scalafx.geometry.Pos
import scalafx.scene.Scene
import scalafx.scene.control.Button
import scalafx.scene.control.Label
import scalafx.scene.input.KeyEvent
import scalafx.scene.layout.StackPane
import scalafx.stage.Modality
import scalafx.stage.Stage
import fr.profi.util.scalafx.ScalaFxUtils
import fr.proline.admin.gui.QuickStart
import scalafx.scene.layout.VBox
import scalafx.scene.layout.HBox

/**
 * **************************************** *
 * Show a confirmWindow with given properties *
 * **************************************** *
 */
class ConfirmWindowWizard(

  wTitle: String,
  wText: String,
  wParent: Option[Stage] = Option(QuickStart.stage),
  isResizable: Boolean = false) extends Stage {

  val popup = this

  title = wTitle
  initModality(Modality.WINDOW_MODAL)
  resizable = isResizable
  if (wParent.isDefined) initOwner(wParent.get)

  scene = new Scene {

    onKeyPressed = (ke: KeyEvent) => { ScalaFxUtils.closeIfEscapePressed(popup, ke) }

    root = new VBox {
      alignment = Pos.Center
      spacing = 35
      padding = Insets(10)

      content = List(

        new Label(wText) {
          wrapText = true

        },
        new HBox {
          spacing = 175
          content = Seq(
            new Button("Cancel") {
              onAction = handle { popup.close() }
            },
            new Button("  Ok  ") {
              onAction = handle { QuickStart.stage.close() }
            })

        })
    }
  }
}

/**
 * ********************************************************************** *
 * Companion object to ConfirmWindow, show it with more understandable name *
 * ********************************************************************** *
 */
object ShowConfirmWindow {

  def apply(
    wText: String,
    wTitle: String = "",
    wParent: Option[Stage] = Option(QuickStart.stage),
    isResizable: Boolean = false) {
    new ConfirmWindowWizard(wTitle, wText, wParent, isResizable).showAndWait()
  }

}