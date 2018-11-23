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
 * Creates and displays a Change externalDB parameters dialog.
 * @author aromdhani
 *
 */

object ChangeExtDbParamsDialog extends LazyLogging {
  case class Result(host: String, port: Int)

  def showAndWait(parentWindow: Window): Option[Result] = {

    // Create the custom dialog.
    val dialog = new Dialog[Result]() {
      initOwner(parentWindow)
      title = "Change parameters"
      headerText = "Enter External database parameters"
      graphic = FxUtils.newImageView(IconResource.EDITSMALL)
    }

    // Set the button types.
    dialog.dialogPane().buttonTypes = Seq(ButtonType.OK, ButtonType.Cancel)

    /* component */
    // Notification labels
    val errorHostLabel = new Label {
      text = "Host must not be empty, Please enter Host value."
      style = TextStyle.RED_ITALIC
    }
    val errorPortLabel = new Label {
      text = s"Port must not be empty, Please enter Port value."
      style = TextStyle.RED_ITALIC
    }
    val errorPanel = new VBox {
      spacing = 5
      children = Seq(errorHostLabel, errorPortLabel)
    }

    // Dialog pane
    val hostLabel = new Label("Host")
    val hostTextField = new TextField()

    val portLabel = new Label("Port")
    val portTextField = new TextField

    // Layout
    val hostPanel = new HBox {
      spacing = 30
      children = Seq(hostLabel, hostTextField)
    }
    val portPanel = new HBox {
      spacing = 30
      children = Seq(portLabel, portTextField)
    }
    val extDbPanel = new VBox {
      spacing = 10
      children = Seq(errorPanel, hostPanel, portPanel)
    }

    // Style
    Seq(hostTextField,
      portTextField).foreach { node =>
        node.prefWidth = 200
        node.hgrow_=(Priority.ALWAYS)
      }

    Seq(hostLabel,
      portLabel).foreach { node =>
        node.prefWidth = 200
      }
    val contentPane = new VBox {
      alignment = Pos.Center
      spacing = 5
      padding = Insets(10)
      children = Seq(extDbPanel)
    }

    dialog.dialogPane().content = contentPane
    val okButton = dialog.dialogPane().lookupButton(ButtonType.OK)
    errorHostLabel.visible <== hostTextField.text.isEmpty
    errorPortLabel.visible <== portTextField.text.isEmpty
    
    // Enable/Disable OK button depending on whether all data was entered.
    okButton.disable <== (hostTextField.text.isEmpty || portTextField.text.isEmpty)

    // Request focus on the host field by default.
    Platform.runLater(hostTextField.requestFocus())
    
    dialog.resultConverter = dialogButton =>
      if (dialogButton == ButtonType.OK)
        Result(hostTextField.text(), portTextField.text().toInt)
      else
        null

    val result = dialog.showAndWait()

    result match {
      case Some(Result(host, port)) => Some(Result(host, port))
      case _ => None
    }
  }

}
