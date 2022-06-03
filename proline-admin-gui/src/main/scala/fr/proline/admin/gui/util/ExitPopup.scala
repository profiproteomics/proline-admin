package fr.proline.admin.gui.util

import scalafx.Includes._
import scalafx.stage.Modality
import scalafx.stage.Stage
import scalafx.geometry.{ Insets, Pos }
import scalafx.scene.Scene
import scalafx.scene.control.{ Button, Label }
import scalafx.scene.layout.StackPane
import scalafx.scene.input.KeyEvent
import scalafx.scene.layout.{ VBox, HBox }

import scalafx.application.Platform
import fr.profi.util.scalafx.ScalaFxUtils
import fr.proline.admin.gui.IconResource

/**
 * Creates and displays Exit Windows Popup
 *
 */

class ExitPopup(
    wTitle: String,
    wText: String,
    wParent: Option[Stage],
    isResizable: Boolean = false) extends Stage {
  val popup = this
  title = wTitle
  initModality(Modality.WindowModal)
  if (wParent.isDefined) initOwner(wParent.get)
  popup.getIcons.add(FxUtils.newImageView(IconResource.IDENTIFICATION).image.value)
  val yesButton = new Button("Yes") {
    graphic = FxUtils.newImageView(IconResource.TICK)
    onAction =  _ => {
      Platform.exit()
      System.exit(0)
    }
  }
  val noButton = new Button("No") {
    graphic = FxUtils.newImageView(IconResource.CANCEL)
    onAction =  _ => {
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
object ExitPopup {
  def apply(
    wTitle: String,
    wText: String,
    wParent: Option[Stage],
    isResizable: Boolean = false) { new ExitPopup(wTitle, wText, wParent, isResizable).showAndWait() }
}