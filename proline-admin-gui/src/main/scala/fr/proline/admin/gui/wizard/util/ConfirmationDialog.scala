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
import fr.proline.admin.gui.util.FxUtils
import fr.proline.admin.gui.IconResource

class ConfirmationDialog(
    dTitle: String,
    dText: String,
    stage: Stage) {

  var isActionConfirmed = false

  /** BEGIN TMP CODE */
  private var _yesButtonText = "Yes"
  private var _cancelButtonText = "Cancel"

  def setYesButtonText(text: String) = _yesButtonText = text
  def setCancelButtonText(text: String) = _cancelButtonText = text

  /** END TMP CODE */

  def showIn(dInitOwner: Stage = stage) {

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
          graphic = FxUtils.newImageView(IconResource.TICK)
          onKeyPressed = (ke: KeyEvent) => { ScalaFxUtils.fireIfEnterPressed(this, ke) }
          onAction = handle {
            isActionConfirmed = true
            confirmDialog.close()
          }
        }

        val cancelButton = new Button(_cancelButtonText) { //No
          graphic = FxUtils.newImageView(IconResource.CANCEL)
          onAction = handle { confirmDialog.close() } //isActionConfirmed = false
        }

        Seq(yesButton, cancelButton).foreach(_.minWidth(50))
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
          children = List(yesButton, cancelButton)
        }
        root = new VBox {
          padding = Insets(20, 20, 10, 20)
          spacing = 10
          children = List(text, buttons)
        }
      }
    }
    _stage.getIcons.add(FxUtils.newImageView(IconResource.IDENTIFICATION).image.value)
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
    initOwner: Stage): Boolean = {

    val confDialog = new ConfirmationDialog(dTitle = title, dText = text, stage = initOwner)

    if (yesText != null && yesText != "Yes") confDialog.setYesButtonText(yesText)
    if (cancelText != null && cancelText != "Cancel") confDialog.setCancelButtonText(cancelText)
    confDialog.showIn(initOwner)
    confDialog.isActionConfirmed //when stage closed()
  }
}
