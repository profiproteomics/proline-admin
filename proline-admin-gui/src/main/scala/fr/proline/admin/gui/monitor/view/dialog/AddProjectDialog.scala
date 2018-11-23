package fr.proline.admin.gui.monitor.view.dialog

import com.typesafe.scalalogging.LazyLogging

import scalafx.Includes._
import scalafx.stage.Window
import scalafx.stage.Modality
import scalafx.application.Platform
import scalafx.geometry.{ Insets, Pos }
import scalafx.scene.control._
import scalafx.scene.layout.{ VBox, HBox, Priority }
import scalafx.util.StringConverter

import fr.proline.core.orm.uds.UserAccount
import fr.proline.admin.gui.process.UdsRepository

import fr.proline.admin.gui.util.FxUtils
import fr.proline.admin.gui.IconResource
import fr.profi.util.scalafx.ScalaFxUtils.TextStyle

/**
 * Creates and displays an Add project dialog.
 * @author aromdhani
 *
 */

object AddProjectDialog extends LazyLogging {

  case class Result(name: String, description: String, ownerId: Long)

  def showAndWait(parentWindow: Window): Option[Result] = {

    // Create the custom dialog.
    val dialog = new Dialog[Result]() {
      initOwner(parentWindow)
      title = "Add Project"
      headerText = "Enter project details"
      graphic = FxUtils.newImageView(IconResource.PLUS)
    }

    // Set the button types.
    dialog.dialogPane().buttonTypes = Seq(ButtonType.OK, ButtonType.Cancel)

    /* Component */

    // Notification labels
    val errorProjNameLabel = new Label {
      text = "The project name must not be empty."
      visible = false
      style = TextStyle.RED_ITALIC
    }

    val errorProjOwnerLabel = new Label {
      text = "The project owner must not be empty. Select a project owner from the list."
      visible = false
      style = TextStyle.RED_ITALIC
    }

    val errorTakenProjNameLabel = new Label {
      text = "The project name is already taken by this user. Please rename your project."
      visible = false
      style = TextStyle.RED_ITALIC
    }

    val notificationPanel = new VBox {
      spacing = 5
      children = Seq(errorProjNameLabel, errorProjOwnerLabel, errorTakenProjNameLabel)
    }

    // Dialog pane
    val projNameLabel = new Label("Project Name")
    val projNameTextField = new TextField()

    val projectDescLabel = new Label("Project Description")
    val projectDescTextAraea = new TextArea {
      prefHeight = 100
    }

    val ownerLabel = new Label("Project Owner")
    val ownerList = new ComboBox[UserAccount](UdsRepository.getAllUserAccounts()) {
      converter = StringConverter.toStringConverter((user: UserAccount) => user.getLogin)
    }

    // Layout
    val projNamePanel = new HBox {
      spacing = 30
      children = Seq(projNameLabel, projNameTextField)
    }
    val projDescPanel = new HBox {
      spacing = 30
      children = Seq(projectDescLabel, projectDescTextAraea)
    }
    val projOwnerPanel = new HBox {
      spacing = 30
      children = Seq(ownerLabel, ownerList)
    }
    val projectPanel = new VBox {
      spacing = 10
      children = Seq(notificationPanel, projNamePanel, projDescPanel, projOwnerPanel)
    }

    //Style

    Seq(
      projNameTextField,
      projectDescTextAraea,
      ownerList).foreach { node =>
        node.prefWidth = 200
        node.hgrow_=(Priority.ALWAYS)
      }

    Seq(projNameLabel,
      projectDescLabel,
      ownerLabel).foreach { node =>
        node.prefWidth = 200
      }
    val contentPane = new VBox {
      alignment = Pos.Center
      spacing = 5
      padding = Insets(10)
      children = Seq(projectPanel)
    }
    dialog.dialogPane().content = contentPane

    val okButton = dialog.dialogPane().lookupButton(ButtonType.OK)

    /* Check dialog input fields */
    // Project name must not be empty
    errorProjNameLabel.visible <== projNameTextField.text.isEmpty
    // Project owner must not be empty
    errorProjOwnerLabel.visible <== ownerList.getSelectionModel().selectedItemProperty.isNull
    //TODO Project description must not be empty?
    //TODO Project name must not be taken
    // Enable/Disable OK button depending on whether all data was entered.
    okButton.disable <== (projNameTextField.text.isEmpty || ownerList.getSelectionModel().selectedItemProperty.isNull)

    // Request focus on project name field by default.
    Platform.runLater(projNameTextField.requestFocus())

    dialog.resultConverter = dialogButton =>
      if (dialogButton == ButtonType.OK)
        Result(projNameTextField.text(), projectDescTextAraea.text(), ownerList.getValue.getId)
      else
        null

    val result = dialog.showAndWait()

    result match {
      case Some(Result(name, description, ownerId)) => Some(Result(name, description, ownerId))
      case _ => None
    }

  }

}
