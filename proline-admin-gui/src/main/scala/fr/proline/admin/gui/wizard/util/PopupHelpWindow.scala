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
import fr.proline.admin.gui.util.FxUtils
import fr.proline.admin.gui.IconResource
import com.sun.javafx.css.StyleClass

/**
 * a confirm popUp on cancel/exit button
 */

class PopupWindow(
  wTitle: String,
  wText: String,
  wParent: Option[Stage] = Option(Wizard.stage),
  isResizable: Boolean = false) extends Stage {
  val popup = this
  title = wTitle
  initModality(Modality.WINDOW_MODAL)
  if (wParent.isDefined) initOwner(wParent.get)
  val yesButton = new Button("Yes") {
    graphic = FxUtils.newImageView(IconResource.TICK)
    onAction = handle {
      Wizard.stage.close()
    }
  }
  val noButton = new Button("No") {
    graphic = FxUtils.newImageView(IconResource.CANCEL)
    onAction = handle {
      popup.close()
    }
  }
  Seq(yesButton, noButton).foreach(_.minWidth(20))
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
        new HBox {
          alignment = Pos.Center
          padding = Insets(10)
          spacing = 30
          children = Seq(yesButton,
            noButton)
        })
    }
  }
}
object ShowPopupWindow {
  def apply(
    wTitle: String,
    wText: String,
    wParent: Option[Stage] = Option(Wizard.stage),
    isResizable: Boolean = false) { new PopupWindow(wTitle, wText, wParent, isResizable).showAndWait() }
}