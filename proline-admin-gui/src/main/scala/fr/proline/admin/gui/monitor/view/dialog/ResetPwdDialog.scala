package fr.proline.admin.gui.monitor.view.dialog

import com.typesafe.scalalogging.LazyLogging

import scalafx.Includes._
import scalafx.stage.Window
import scalafx.stage.Modality
import scalafx.application.Platform
import scalafx.geometry.{ Insets, Pos }
import scalafx.scene.control._
import scalafx.scene.layout.{ VBox, HBox, Priority }
import fr.proline.admin.gui.util.FxUtils
import fr.proline.admin.gui.IconResource
import fr.profi.util.scalafx.ScalaFxUtils.TextStyle

/**
 * Creates and displays a Reset password dialog.
 * @author aromdhani
 *
 */

object ResetPwdDialog extends LazyLogging {

  case class Result(newPassword: Option[String] = None)

  def showAndWait(parentWindow: Window): Option[Result] = {

    // Create the custom dialog.
    val dialog = new Dialog[Result]() {
      initOwner(parentWindow)
      title = "Reset Password"
      headerText = "Reset user password"
      graphic = FxUtils.newImageView(IconResource.UNLOCK)
    }

    // Set the button types.
    dialog.dialogPane().buttonTypes = Seq(ButtonType.OK, ButtonType.Cancel)

    /* Component */
    // Notification labels
    val errorPwLabel = new Label {
      text = "New password and Confirm password do not match, Please try again."
      visible = false
    //  managed <== visible
      style = TextStyle.RED_ITALIC
    }

    // Dialog pane
    val firstPwLabel = new Label("New password")
    val firstPwTextField = new PasswordField()

    val secondPwLabel = new Label("Confirm password")
    val secondPwTextField = new PasswordField()

    // Layout

    val firstPwPanel = new HBox {
      spacing = 30
      children = Seq(firstPwLabel, firstPwTextField)
    }
    val secondPwPanel = new HBox {
      spacing = 30
      children = Seq(secondPwLabel, secondPwTextField)
    }
    val changePwdPanel = new VBox {
      spacing = 10
      children = Seq(errorPwLabel,
        firstPwPanel,
        secondPwPanel)
    }

    // Style

    Seq(firstPwTextField,
      secondPwTextField).foreach { component =>
        component.prefWidth = 200
        component.hgrow_=(Priority.Always)
      }

    Seq(firstPwLabel,
      secondPwLabel).foreach { component =>
        component.prefWidth = 200
      }

    val contentPane = new VBox {
      alignment = Pos.Center
      spacing = 5
      padding = Insets(10)
      children = Seq(changePwdPanel)
    }

    dialog.dialogPane().content = contentPane

    val okButton = dialog.dialogPane().lookupButton(ButtonType.OK)

    /* Check dialog input fields */

    // Passwords must be the same
    firstPwTextField.text.onChange { (_, oldValue, newValue) =>
      if (newValue != secondPwTextField.getText) errorPwLabel.visible = true else errorPwLabel.visible = false
    }
    secondPwTextField.text.onChange { (_, oldValue, newValue) =>
      if (newValue != firstPwTextField.getText) errorPwLabel.visible = true else errorPwLabel.visible = false
    }
    // Request focus on the first password field by default.
    Platform.runLater(firstPwTextField.requestFocus())
    // Enable/Disable OK button depending on whether all data was entered.
    okButton.disable <== (firstPwTextField.text.isEmpty || secondPwTextField.text.isEmpty || firstPwTextField.text.isNotEqualTo(secondPwTextField.text))

    dialog.resultConverter = dialogButton =>
      if (dialogButton == ButtonType.OK)
        Result(Option(secondPwTextField.text()))
      else
        null

    val result = dialog.showAndWait()

    result match {
      case Some(Result(newPassword)) => Some(Result(newPassword))
      case _ => None
    }
  }
}
