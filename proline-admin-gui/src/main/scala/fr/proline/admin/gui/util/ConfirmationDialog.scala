package fr.proline.admin.gui.util

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
import fr.proline.admin.gui.Main

class ConfirmationDialog( //TODO: finish ChoiceDialog then ConfirmationDialog extends ChoiceDialog
  dTitle: String,
  dText: String
) {

  var isActionConfirmed = false

  /** BEGIN TMP CODE */
  private var _yesButtonText = "Yes"
  private var _cancelButtonText = "Cancel"

  def setYesButtonText(text: String) = _yesButtonText = text
  def setCancelButtonText(text: String) = _cancelButtonText = text

  /** END TMP CODE */

  def showIn(dInitOwner: Stage = Main.stage) {

    /** Define modal window */
    val _stage = new Stage {

      val confirmDialog = this

      title = dTitle
      initOwner(dInitOwner)
//      initStyle(StageStyle.UTILITY)
      initModality(Modality.WINDOW_MODAL)
      centerOnScreen()
      //      this.x = Util.getStartX(mainStage = confirmDialog)
      //      this.y = Util.getStartY(mainStage = confirmDialog)
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
          onKeyPressed = (ke: KeyEvent ) => {ScalaFxUtils.fireIfEnterPressed(this, ke)}
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
          children = List(yesButton, cancelButton)
        }

        root = new VBox {
          padding = Insets(20, 20, 10, 20)
          spacing = 10
          children = List(text, buttons)
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
    initOwner: Stage = Main.stage
  ): Boolean = {

    val confDialog = new ConfirmationDialog(dTitle = title, dText = text)

    if (yesText != null && yesText != "Yes") confDialog.setYesButtonText(yesText)
    if (cancelText != null && cancelText != "Cancel") confDialog.setCancelButtonText(cancelText)
    confDialog.showIn(initOwner)
    
    confDialog.isActionConfirmed //when stage closed()
  }
}


///**
// *  Create and display a modal window to ask user to confirm before running the action.
// */
//class ChoiceDialog(dInitOwner: Stage, dTitle: String, dText: String) { //, button: String = "Yes", button2String = "No")
//
//  var isConfirmed = false
//
//  //defaults 
//  private var _button1 = new Button("Yes") 
//  private var _button2 = new Button("No")
//
//  def setButton1Text(text: String) = _button1.text = text
//  def setButton2Text(text: String) = _button2.text = text
//
//  def setButton1Action(action: Unit) = _button1.onAction = handle { action }
//  def setButton2Action(action: Unit) = _button2.onAction = handle { action }
//
//  /** Define modal window */
//  val _stage = new Stage {
//
//    val choiceDialog = this
//
//    title = dTitle
//    initOwner(dInitOwner)
//    initStyle(StageStyle.UTILITY)
//    initModality(Modality.WINDOW_MODAL)
//
//    this.x = Util.getStartX()
//    this.y = Util.getStartY()
//    resizable = false
//
//    scene = new Scene {
//
//      /**
//       * ********** *
//       * COMPONENTS *
//       * ********** *
//       */
//
//      val text = new Label(dText)
//
//      val button1 = _button1
//      
//
//      new Button("Yes") {
//        //styleClass += ("minorButtons")
//        onAction = handle {
//          isActionConfirmed = true
//          confirmDialog.close()
//        }
//      }
//
//      val cancelButton = new Button("Cancel") { //No
//        //styleClass += ("minorButtons")
//        onAction = handle { confirmDialog.close() } //isActionConfirmed = false
//      }
//
//      /**
//       * ****** *
//       * LAYOUT *
//       * ****** *
//       */
//
//      //stylesheets = List(Main.CSS)
//
//      val buttons = new HBox {
//        alignment = Pos.BASELINE_CENTER
//        padding = Insets(10)
//        spacing = 20
//        content = List(yesButton, cancelButton)
//      }
//
//      root = new VBox {
//        padding = Insets(20, 20, 10, 20)
//        spacing = 10
//        content = List(text, buttons)
//      }
//    }
//  }
//
//}