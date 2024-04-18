package fr.proline.admin.gui.monitor.view.dialog

import com.typesafe.scalalogging.LazyLogging

import scalafx.Includes._
import scalafx.stage.Window
import scalafx.stage.Stage
import scalafx.stage.Modality
import scalafx.application.Platform
import scalafx.geometry.{ Insets, Pos }
import scalafx.scene.control._
import scalafx.scene.layout.{ VBox, HBox, Priority }
import scalafx.beans.property.BooleanProperty
import scalafx.util.StringConverter

import fr.proline.admin.gui.util.FxUtils
import fr.proline.admin.gui.IconResource
import fr.profi.util.scalafx.ScalaFxUtils.TextStyle

import fr.proline.core.orm.uds.UserAccount
import fr.proline.admin.gui.process.UdsRepository
import java.io.File

/**
 * Creates and displays change project owner dialog.
 * @author aromdhani
 *
 */

object ChangeProjOwnerDialog extends LazyLogging {

  case class Result(ownerId: Long)

  def showAndWait(parentWindow: Window): Option[Result] = {

    // Create the custom dialog.
    val dialog = new Dialog[Result]() {
      initOwner(parentWindow)
      title = "Change Project Owner"
      headerText = "Project owner details"
      graphic = FxUtils.newImageView(IconResource.EDITSMALL)
    }

    // Set the button types.
    dialog.dialogPane().buttonTypes = Seq(ButtonType.OK, ButtonType.Cancel)

    /* Component */
    // Notification labels
       val errorProjOwnerLabel = new Label {
      text = "The new project owner must not be empty. Select an owner from the list."
      managed <== visible
      style = TextStyle.RED_ITALIC
    }
    val notificationsPanel = new VBox {
      spacing = 5
      children = Seq(errorProjOwnerLabel)
    }
    // Dialog pane
    val ownerLabel = new Label("Project owner")
    val ownerList = new ComboBox[UserAccount](UdsRepository.getAllUserAccounts()) {
      converter = StringConverter.toStringConverter((user: UserAccount) => {
        if (user != null)
          user.getLogin
        else
          ""
      })
    }
    val projOwnerPanel = new HBox {
      spacing = 30
      children = Seq(ownerLabel, ownerList)
    }
    val restoreProjectPanel = new VBox {
      spacing = 10
      children = Seq(notificationsPanel,
        projOwnerPanel
        )
    }
    
    // Style
    Seq(
      ownerList).foreach { node =>
        node.prefWidth = 200
        node.hgrow_=(Priority.Always)
      }
    val contentPane = new VBox {
      alignment = Pos.Center
      spacing = 5
      padding = Insets(10)
      children = Seq(restoreProjectPanel)
    }
    dialog.dialogPane().content = contentPane
    val okButton = dialog.dialogPane().lookupButton(ButtonType.OK)
    //Project owner must not be empty
    errorProjOwnerLabel.visible <== ownerList.getSelectionModel().selectedItemProperty.isNull
    // Enable/Disable OK button depending on whether all data was entered.
    okButton.disable <== (ownerList.getSelectionModel().selectedItemProperty.isNull)
    // Request focus on the owner list .
    Platform.runLater(ownerList.requestFocus())
    dialog.resultConverter = dialogButton =>
      if (dialogButton == ButtonType.OK)
        Result(ownerList.getValue.getId)
      else
        null
    val result = dialog.showAndWait()
    result match {
      case Some(Result(ownerId)) =>
        Some(Result(ownerId))
      case _ => None
    }
  }
}
