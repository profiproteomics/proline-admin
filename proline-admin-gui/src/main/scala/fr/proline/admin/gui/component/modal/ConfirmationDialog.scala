package fr.proline.admin.gui.component.modal

import fr.proline.admin.gui.Main
import fr.proline.admin.gui.Util

import scalafx.Includes.handle
import scalafx.geometry.Insets
import scalafx.geometry.Pos
import scalafx.scene.Scene
import scalafx.scene.control.Button
import scalafx.scene.control.Label
import scalafx.scene.layout.HBox
import scalafx.scene.layout.VBox
import scalafx.stage.Modality
import scalafx.stage.Stage
import scalafx.stage.StageStyle

/**
 *  Create and display a modal window to ask user to confirm before running the action.
 */
class ConfirmationDialog(
  _text: String,
  _title: String) {

  var isActionConfirmed = false

  def showConfirmDialog() {

    /** Define modal window */
    val _stage = new Stage {

      confirmDialog =>
      title = _title
      initStyle(StageStyle.UTILITY)
      initModality(Modality.WINDOW_MODAL)
      initOwner(Main.stage)
      this.x = Util.getStartX()
      this.y = Util.getStartY()
      resizable = false

      scene = new Scene {

        /**
         * ********** *
         * COMPONENTS *
         * ********** *
         */

        val text = new Label(_text)

        val yesButton = new Button("Yes") {
          //styleClass += ("minorButtons")
          onAction = handle {
            isActionConfirmed = true
            confirmDialog.close()
          }
        }

        val cancelButton = new Button("Cancel") {
          //styleClass += ("minorButtons")
          onAction = handle { confirmDialog.close() } //isActionConfirmed = false
        }

        /**
         * ****** *
         * LAYOUT *
         * ****** *
         */

        //stylesheets = List(Main.CSS)

        val buttons = new HBox {
          alignment = Pos.BASELINE_CENTER
          padding = Insets(10)
          spacing = 20
          content = List(yesButton, cancelButton)
        }

        root = new VBox {
          padding = Insets(20, 20, 10, 20)
          spacing = 10
          content = List(text, buttons)
        }
      }
    }

    /** Display this window */
    _stage.showAndWait()
  }

}

object GetConfirmation {

  def apply(
    text: String,
    title: String = "Confirm your action"): Boolean = {

    val confDialog = new ConfirmationDialog(text, title)
    confDialog.showConfirmDialog()
    confDialog.isActionConfirmed
  }
}