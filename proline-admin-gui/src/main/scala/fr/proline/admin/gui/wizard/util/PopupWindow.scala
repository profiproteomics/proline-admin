package fr.proline.admin.gui.wizard.util

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
import fr.proline.admin.gui.Wizard
import scalafx.scene.layout.VBox
import scalafx.scene.layout.HBox

/**
 * a confirm popUp on cancel/exit button
 */

class PopupHelpWindow(
  wTitle: String,
  wText: String,
  wParent: Option[Stage] = Option(Wizard.stage),
  isResizable: Boolean = false) extends Stage {
  val popup = this
  title = wTitle
  initModality(Modality.WINDOW_MODAL)
  if (wParent.isDefined) initOwner(wParent.get)
  scene = new Scene {
    onKeyPressed = (ke: KeyEvent) => { ScalaFxUtils.closeIfEscapePressed(popup, ke) }
    root = new VBox {
      alignment = Pos.Center
      spacing = 15
      padding = Insets(10)
      content = List(
        new Label(wText) {
          wrapText = true
        },
        new HBox {
          alignment = Pos.Center
          padding = Insets(10)
          spacing = 30
          content = Seq(
            new Button("  Ok  ") {
              minWidth = 20
              onAction = handle {
                popup.close()
              }
            })
        })
    }
  }
}
object PopupHelpWindow {
  def apply(
    wTitle: String,
    wText: String,
    wParent: Option[Stage] = Option(Wizard.stage),
    isResizable: Boolean = false) { new PopupHelpWindow(wTitle, wText, wParent, isResizable).showAndWait() }
}