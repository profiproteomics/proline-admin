package fr.proline.admin.gui.component.dialog

import scalafx.Includes._
import scalafx.geometry.Insets
import scalafx.scene.Scene
import scalafx.scene.control.TextArea
import scalafx.scene.input.KeyCode
import scalafx.scene.input.KeyEvent
import scalafx.scene.layout.StackPane
import scalafx.stage.Modality
import scalafx.stage.Stage

import fr.proline.admin.gui.Main

class PopupWindow(

  wTitle: String,
  wText: String,
  wParent: Option[Stage] = Option(Main.stage),
  isResizable: Boolean = false //    X: Option[Double] = Some(Main.stage.width() / 2),
  //    Y: Option[Double] = Some(Main.stage.width() / 2)
  ) {
  //TODO: rename package into window, for this is no dialog
  // TODO: see scalafx.stage.PopupWindow

  new Stage {

    val popup = this

    title = wTitle
    initModality(Modality.WINDOW_MODAL)
    resizable = isResizable
    if (wParent.isDefined) initOwner(wParent.get)

    height = 200
    //      if (X.isDefined) this.x = X.get
    //      if (Y.isDefined) this.y = Y.get

    scene = new Scene {

      onKeyPressed = (ke: KeyEvent) => { if (ke.code == KeyCode.ESCAPE) popup.close() }

      root = new StackPane {
        padding = Insets(5)
        content = new TextArea {
          text = wText
          wrapText = true
          editable = false
          style = "-fx-border-style: none;-fx-focus-color: transparent;"
        }
      }
    }
  }.showAndWait()

}