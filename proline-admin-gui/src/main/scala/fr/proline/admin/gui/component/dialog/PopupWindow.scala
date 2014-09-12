package fr.proline.admin.gui.component.dialog

import fr.proline.admin.gui.Main

import scalafx.geometry.Insets
import scalafx.scene.Scene
import scalafx.scene.control.TextArea
import scalafx.scene.layout.StackPane
import scalafx.stage.Modality
import scalafx.stage.Stage
import scalafx.stage.StageStyle

class PopupWindow(

  wTitle: String,
  wText: String,
  wParent: Option[Stage] = Option(Main.stage) //,
  //    X: Option[Double] = Some(Main.stage.width() / 2),
  //    Y: Option[Double] = Some(Main.stage.width() / 2)
  ) {
  //TODO: rename package into window, for this is no dialog
  // TODO: see scalafx.stage.PopupWindow

  new Stage {

    popup =>
    title = wTitle
    initStyle(StageStyle.UTILITY)
    initModality(Modality.WINDOW_MODAL)
    resizable = false
    if (wParent.isDefined) initOwner(wParent.get)
    //      if (X.isDefined) this.x = X.get
    //      if (Y.isDefined) this.y = Y.get

    scene = new Scene {
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