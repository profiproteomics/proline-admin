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
import fr.proline.admin.gui.util.FxUtils
import fr.proline.admin.gui.IconResource
import fr.profi.util.scalafx.ScalaFxUtils.TextStyle
import java.io.File

/**
 * Creates and displays an Archive project dialog.
 * @author aromdhani
 *
 */

object ArchiveProjectDialog extends LazyLogging {

  case class Result(binDirPath: String, archiveLocationPath: String)

  def showAndWait(parentWindow: Window): Option[Result] = {

    // Create the custom dialog.
    val dialog = new Dialog[Result]() {
      initOwner(parentWindow)
      title = "Archive Project"
      headerText = "Archive project details"
      graphic = FxUtils.newImageView(IconResource.SAVE)
    }

    // Set the button types.
    dialog.dialogPane().buttonTypes = Seq(ButtonType.OK, ButtonType.Cancel)

    /* Component */
    // Notification labels
    val errorBinPathLabel = new Label {
      text = "Select a validated PostgreSQL bin directory. It must contains pg_dump appliaction."
      managed <== visible
      style = TextStyle.RED_ITALIC
    }
    val errorArchivePathLabel = new Label {
      text = "Select an archive location. It must not be empty."
      managed <== visible
      style = TextStyle.RED_ITALIC
    }
    val errorPanel = new VBox {
      spacing = 5
      children = Seq(errorBinPathLabel, errorArchivePathLabel)
    }

    // Dialog pane
    val projectBinDirLabel = new Label("Select a PostgreSQL bin directory")
    val projectBinDirTextField = new TextField {}
    /** Browse a PostgreSQL bin directory */
    def browseBinDir(): Unit = {
      val fileOp = Option(FxUtils.browseDirectory(
        dcTitle = "Select a PostgreSQL bin directory",
        dcInitialDir = projectBinDirTextField.getText,
        dcInitOwner = parentWindow.asInstanceOf[Stage]))
      fileOp.foreach { file =>
        val newPath = file.getPath()
        projectBinDirTextField.text = newPath
      }
    }

    val browseBinButton = new Button {
      text = "Browse..."
      graphic = FxUtils.newImageView(IconResource.LOAD)
      onAction = _ => {
        browseBinDir()
      }
    }
    val archivePathLabel = new Label("Select an archive location")
    val archivePathTextField = new TextField {}

    /** Browse an archive location directory */
    def browseArchiveDir(): Unit = {
      val fileOp = Option(FxUtils.browseDirectory(
        dcTitle = "Select a project archive location",
        dcInitialDir = archivePathTextField.getText,
        dcInitOwner = parentWindow.asInstanceOf[Stage]))
      fileOp.foreach { file =>
        val newPath = file.getPath()
        archivePathTextField.text = newPath
      }
    }

    val browseButton = new Button {
      text = "Browse..."
      graphic = FxUtils.newImageView(IconResource.LOAD)
      onAction =  _ => {
        browseArchiveDir()
      }
    }

    // Layout
    val browseBinDiPanel = new HBox {
      spacing = 30
      children = Seq(projectBinDirLabel, projectBinDirTextField, browseBinButton)
    }
    val browseProjectPanel = new HBox {
      spacing = 30
      children = Seq(archivePathLabel, archivePathTextField, browseButton)
    }

    val archiveProjectPanel = new VBox {
      spacing = 10
      children = Seq(errorPanel,
        browseBinDiPanel,
        browseProjectPanel)
    }

    // Style
    Seq(projectBinDirTextField,
      archivePathTextField).foreach { node =>
        node.prefWidth = 200
        node.hgrow_=(Priority.Always)
      }

    Seq(projectBinDirLabel,
      archivePathLabel).foreach { node =>
        node.prefWidth = 200
      }

    val contentPane = new VBox {
      alignment = Pos.Center
      spacing = 5
      padding = Insets(10)
      children = Seq(archiveProjectPanel)
    }

    dialog.dialogPane().content = contentPane

    val okButton = dialog.dialogPane().lookupButton(ButtonType.OK)

    /* Check dialog input fields */

    // Bin directory path must have pg_dump application
    val isValidBinDirectory = BooleanProperty(false)
    projectBinDirTextField.text.onChange { (_, _, newValue) =>
      if (new File(newValue).exists && new File(newValue).listFiles().find(_.getName.matches("^pg_dump.exe$")).isDefined)
        isValidBinDirectory.value = true else isValidBinDirectory.value = false
    }
    errorBinPathLabel.visible <== (projectBinDirTextField.text.isEmpty || !isValidBinDirectory)
    // Archive location must not be empty
    val isValidArchiveLocation = BooleanProperty(false)
    archivePathTextField.text.onChange { (_, _, newValue) =>
      if (new File(newValue).exists) isValidArchiveLocation.value = true else isValidArchiveLocation.value = false
    }
    errorArchivePathLabel.visible <== (archivePathTextField.text.isEmpty || !isValidArchiveLocation)

    // Enable/Disable OK button depending on whether all data was entered.
    okButton.disable <== (!isValidBinDirectory || !isValidArchiveLocation)

    // Request focus on the bin directory path field by default.
    Platform.runLater(projectBinDirTextField.requestFocus())

    dialog.resultConverter = dialogButton =>
      if (dialogButton == ButtonType.OK)
        Result(projectBinDirTextField.text(), archivePathTextField.text())
      else
        null

    val result = dialog.showAndWait()

    result match {
      case Some(Result(binDirPath, archiveLocationPath)) => Some(Result(binDirPath, archiveLocationPath))
      case _ => None
    }
  }
}
