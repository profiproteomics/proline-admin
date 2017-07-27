package fr.proline.admin.gui.wizard.util

import scalafx.Includes._
import scalafx.geometry.Insets
import scalafx.geometry.Pos
import scalafx.scene.Scene
import scalafx.scene.control.Button
import scalafx.scene.control.Label
import scalafx.scene.input.KeyEvent
import scalafx.scene.layout.HBox
import scalafx.scene.layout.VBox
import scalafx.stage.Modality
import scalafx.stage.Stage

import fr.profi.util.scalafx.ScalaFxUtils
import fr.proline.admin.gui.Wizard

class ConfirmationDialog(
  dTitle: String,
  dText: String) {

  var isActionConfirmed = false

  /** BEGIN TMP CODE */
  private var _yesButtonText = "Yes"
  private var _cancelButtonText = "Cancel"

  def setYesButtonText(text: String) = _yesButtonText = text
  def setCancelButtonText(text: String) = _cancelButtonText = text

  /** END TMP CODE */

  def showIn(dInitOwner: Stage = Wizard.stage) {

    /** Define modal window */
    val _stage = new Stage {

      val confirmDialog = this

      title = dTitle
      initOwner(dInitOwner)
      initModality(Modality.WINDOW_MODAL)
      centerOnScreen()
      resizable = false

      scene = new Scene {

        /**
         * ********** *
         * COMPONENTS *
         * ********** *
         */

        val text = new Label(dText)

        val yesButton = new Button(_yesButtonText) {
          //styleClass += ("minorButtons")
          onKeyPressed = (ke: KeyEvent) => { ScalaFxUtils.fireIfEnterPressed(this, ke) }
          onAction = handle {
            isActionConfirmed = true
            confirmDialog.close()
          }
        }

        val cancelButton = new Button(_cancelButtonText) { //No
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
          alignment = Pos.BaselineCenter
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
    title: String = "Confirm your action",
    yesText: String = "Yes", //TODO: delete me
    cancelText: String = "Cancel",
    initOwner: Stage = Wizard.stage): Boolean = {

    val confDialog = new ConfirmationDialog(dTitle = title, dText = text)

    if (yesText != null && yesText != "Yes") confDialog.setYesButtonText(yesText)
    if (cancelText != null && cancelText != "Cancel") confDialog.setCancelButtonText(cancelText)
    confDialog.showIn(initOwner)

    confDialog.isActionConfirmed //when stage closed()
  }
}
