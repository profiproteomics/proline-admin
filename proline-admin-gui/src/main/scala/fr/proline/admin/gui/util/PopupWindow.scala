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
import scalafx.scene.layout.VBox
import fr.proline.admin.gui.IconResource

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
   popup.getIcons.add(FxUtils.newImageView(IconResource.IDENTIFICATION).image.value)
  scene = new Scene {

    onKeyPressed = (ke: KeyEvent) => { ScalaFxUtils.closeIfEscapePressed(popup, ke) }

    root = new VBox {
      alignment = Pos.Center
      spacing = 15
      padding = Insets(10)

      children = List(

        new Label(wText) {
          wrapText = true
        },
        new Button("OK") {
          graphic = FxUtils.newImageView(IconResource.TICK)
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
