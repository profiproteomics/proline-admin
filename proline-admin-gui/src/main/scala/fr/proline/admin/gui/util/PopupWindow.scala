package fr.proline.admin.gui.util

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
import fr.proline.admin.gui.Main
import fr.proline.admin.gui.QuickStart
import scalafx.scene.layout.VBox

/**
 * **************************************** *
 * Show a PopupWindow with given properties *
 * **************************************** *
 */
class PopupWindow(

  wTitle: String,
  wText: String,
  wParent: Option[Stage] = Option(Main.stage),
  isResizable: Boolean = false 
  //X: Option[Double] = Some(Main.stage.width() / 2),
  //Y: Option[Double] = Some(Main.stage.width() / 2)

) extends Stage {

  //TODO: rename package into window, for this is no dialog
  // TODO: see scalafx.stage.PopupWindow

  val popup = this

  title = wTitle
  initModality(Modality.WINDOW_MODAL)
  resizable = isResizable
  if (wParent.isDefined) initOwner(wParent.get)

  //height = 200
  //      if (X.isDefined) this.x = X.get
  //      if (Y.isDefined) this.y = Y.get

  scene = new Scene {

    onKeyPressed = (ke: KeyEvent) => { ScalaFxUtils.closeIfEscapePressed(popup, ke) }

    root = new VBox {
      alignment = Pos.Center
      spacing = 15
      padding = Insets(10)
      //content = new TextArea {
      content = List(

        new Label(wText) {
          wrapText = true
          //editable = false
          //style = "-fx-border-style: none;-fx-focus-color: transparent;"
        },

        new Button("OK") {
          onAction = handle { popup.close() }
        }
      )
    }
  }
}

/**
 * ********************************************************************** *
 * Companion object to PopupWindow, show it with more understandable name *
 * ********************************************************************** *
 */
object ShowPopupWindow {

  def apply(
    wText: String,
    wTitle: String = "",
    wParent: Option[Stage] = Option(Main.stage),
    isResizable: Boolean = false
  ) {
    new PopupWindow(wTitle, wText, wParent, isResizable).showAndWait()
  }

}
object ShowPopupWindowWizard {

  def apply(
    wText: String,
    wTitle: String = "",
    wParent: Option[Stage] = Option(QuickStart.stage),
    isResizable: Boolean = false
  ) {
    new PopupWindow(wTitle, wText, wParent, isResizable).showAndWait()
  }

}