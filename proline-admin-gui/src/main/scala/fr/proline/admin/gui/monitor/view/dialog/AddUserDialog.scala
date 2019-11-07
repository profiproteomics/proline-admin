package fr.proline.admin.gui.monitor.view.dialog

import com.typesafe.scalalogging.LazyLogging

import scalafx.Includes._
import scalafx.stage.Window
import scalafx.stage.Modality
import scalafx.application.Platform
import scalafx.scene.control._
import scalafx.scene.layout.{ VBox, HBox, Priority }
import scalafx.geometry.{ Insets, Pos }
import scalafx.beans.property.BooleanProperty
import fr.proline.admin.gui.util.FxUtils
import fr.proline.admin.gui.IconResource
import fr.profi.util.scalafx.ScalaFxUtils.TextStyle

/**
 * Creates and displays an Add user dialog.
 * @author aromdhani
 *
 */

object AddUserDialog extends LazyLogging {

  case class Result(login: String, pswd: Option[String] = None, user: Option[Boolean] = None, passwdEncrypted: Option[Boolean] = None)

  def showAndWait(parentWindow: Window): Option[Result] = {

    // Create the custom dialog.
    val dialog = new Dialog[Result]() {
      initOwner(parentWindow)
      title = "Add User"
      headerText = "Enter user details"
      graphic = FxUtils.newImageView(IconResource.USER)
    }

    // Set the button types.
    dialog.dialogPane().buttonTypes = Seq(ButtonType.OK, ButtonType.Cancel)

    /* Component */
    // Notification labels
    val errorLoginLabel = new Label {
      text = "Login must not be empty. Only letters, numbers, white spaces in the middle, '-', '.' and '_' may be used."
      visible = true
      style = TextStyle.RED_ITALIC
    }
    val errorLoginExistLabel = new Label {
      text = s"This login has already been taken. Choose a new login."
      visible = false
      style = TextStyle.RED_ITALIC
    }

    val errorPwLabel = new Label {
      text = "Password and Confirm Password do not match, Please try again."
      visible = true
      style = TextStyle.RED_ITALIC
    }

    val notificationPanel = new VBox {
      spacing = 5
      children = Seq(errorLoginLabel, errorPwLabel, errorLoginExistLabel)
    }

    // Dialog pane
    val loginLabel = new Label("Login")
    val loginTextField = new TextField()

    val firstPwLabel = new Label("Password")
    val firstPwTextField = new PasswordField()

    val secondPwLabel = new Label("Confirm password")
    val secondPwTextField = new PasswordField()

    val isInUserGrpChbox = new CheckBox {
      text = "Add user to administrator group"
      selected = false
    }
    // Layout
    val userLoginPanel = new HBox {
      spacing = 30
      children = Seq(loginLabel, loginTextField)
    }
    val userFirstPwPanel = new HBox {
      spacing = 30
      children = Seq(firstPwLabel, firstPwTextField)
    }
    val userSecondPwPanel = new HBox {
      spacing = 30
      children = Seq(secondPwLabel, secondPwTextField)
    }
    val userPanel = new VBox {
      spacing = 10
      vgrow = Priority.Always
      children = Seq(notificationPanel, userLoginPanel, userFirstPwPanel, userSecondPwPanel, isInUserGrpChbox)
    }

    // Style
    isInUserGrpChbox.setPrefWidth(200)
    Seq(
      firstPwTextField,
      secondPwTextField,
      loginTextField).foreach { node =>
        node.prefWidth = 200
        node.hgrow_=(Priority.ALWAYS)
      }

    Seq(
      loginLabel,
      firstPwLabel,
      secondPwLabel,
      isInUserGrpChbox).foreach { node =>
        node.prefWidth = 200
      }
    val contentPane = new VBox {
      alignment = Pos.Center
      spacing = 5
      padding = Insets(10)
      children = Seq(userPanel)
    }
    dialog.dialogPane().content = contentPane

    val okButton = dialog.dialogPane().lookupButton(ButtonType.OK)

    /* Check dialog input fields */

    // Login must be letters , numbers and  '_ . - '
    val isValidLogin = BooleanProperty(false)
    loginTextField.text.onChange { (_, _, newValue) =>
      newValue match {
        case login if login matches ("^([a-zA-Z0-9_.-]+)$") =>
          errorLoginLabel.visible = false; isValidLogin.value = true
        case _ => errorLoginLabel.visible = true; isValidLogin.value = false
      }
    }

    // Passwords must be the same
    firstPwTextField.text.onChange { (_, oldValue, newValue) =>
      if (newValue != secondPwTextField.getText) errorPwLabel.visible = true else errorPwLabel.visible = false
    }
    secondPwTextField.text.onChange { (_, oldValue, newValue) =>
      if (newValue != firstPwTextField.getText) errorPwLabel.visible = true else errorPwLabel.visible = false
    }
    // Enable/Disable OK button depending on whether all data was entered.
    okButton.disable <== (!isValidLogin || loginTextField.text.isEmpty
      || firstPwTextField.text.isEmpty || secondPwTextField.text.isEmpty
      || firstPwTextField.text.isNotEqualTo(secondPwTextField.text))

    // Request focus on the first name field by default.
    Platform.runLater(loginTextField.requestFocus())

    dialog.resultConverter = dialogButton =>
      if (dialogButton == ButtonType.OK)
        Result(loginTextField.text(), Option(firstPwTextField.text()), Option(!isInUserGrpChbox.isSelected), None)
      else
        null

    val result = dialog.showAndWait()

    result match {
      case Some(Result(login, pw, isInUserGrp, passwdEncrypted)) => Some(Result(login, pw, isInUserGrp, passwdEncrypted))
      case _ => None
    }

  }

}
