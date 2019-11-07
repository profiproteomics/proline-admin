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
 * Creates and displays a Restore project dialog.
 * @author aromdhani
 *
 */

object RestoreProjectDialog extends LazyLogging {

  case class Result(ownerId: Long, binDirPath: String, archivedProjDirPath: String, projectName: Option[String] = None)

  def showAndWait(parentWindow: Window): Option[Result] = {

    // Create the custom dialog.
    val dialog = new Dialog[Result]() {
      initOwner(parentWindow)
      title = "Restore Project"
      headerText = "Restore project details"
      graphic = FxUtils.newImageView(IconResource.LOAD)
    }

    // Set the button types.
    dialog.dialogPane().buttonTypes = Seq(ButtonType.OK, ButtonType.Cancel)

    /* Component */

    // Notification labels
    val errorBinPathLabel = new Label {
      text = "Select a validated PostgreSQL bin directory. It must contains pg_restore appliaction."
      managed <== visible
      style = TextStyle.RED_ITALIC
    }
    val errorArchivePathLabel = new Label {
      text = "Select a project location. It must contains backup files."
      managed <== visible
      style = TextStyle.RED_ITALIC
    }
    val errorProjOwnerLabel = new Label {
      text = "The project owner must not be empty. Select an owner from the list."
      managed <== visible
      style = TextStyle.RED_ITALIC
    }
    val notificationsPanel = new VBox {
      spacing = 5
      children = Seq(errorBinPathLabel, errorArchivePathLabel, errorProjOwnerLabel)
    }

    // Dialog pane
    val projectBinDirLabel = new Label("Select a PostgreSQL bin directory")
    val binDirPathTextField = new TextField {}
    /** Browse a PostgreSQL bin directory */
    def browseBinDir(): Unit = {
      val fileOp = Option(FxUtils.browseDirectory(
        dcTitle = "Select a PostgreSQL bin directory",
        dcInitialDir = binDirPathTextField.getText,
        dcInitOwner = parentWindow.asInstanceOf[Stage]))
      fileOp.foreach { file =>
        val newPath = file.getPath()
        binDirPathTextField.text = newPath
      }
    }

    val browseBinButton = new Button {
      text = "Browse..."
      graphic = FxUtils.newImageView(IconResource.LOAD)
      onAction = handle {
        browseBinDir()
      }
    }
    val archivePathLabel = new Label("Select a project directory")
    val projDirPathTextField = new TextField {}

    /** Browse an archive location directory */
    def browseArchiveDir(): Unit = {
      val fileOp = Option(FxUtils.browseDirectory(
        dcTitle = "Select a project directory",
        dcInitialDir = projDirPathTextField.getText,
        dcInitOwner = parentWindow.asInstanceOf[Stage]))
      fileOp.foreach { file =>
        val newPath = file.getPath()
        projDirPathTextField.text = newPath
      }
    }

    val browseButton = new Button {
      text = "Browse..."
      graphic = FxUtils.newImageView(IconResource.LOAD)
      onAction = handle {
        browseArchiveDir()
      }
    }

    val ownerLabel = new Label("Project owner")
    val ownerList = new ComboBox[UserAccount](UdsRepository.getAllUserAccounts()) {
      converter = StringConverter.toStringConverter((user: UserAccount) => user.getLogin)
    }
    val projOwnerPanel = new HBox {
      spacing = 30
      children = Seq(ownerLabel, ownerList)
    }
    val renameProjChBox = new CheckBox {
      text = "Rename project"
      prefWidth = 200
      selected = false
    }
    val projNameLabel = new Label("Project name")
    val projNameTextFiled = new TextField {}

    val projNamePanel = new VBox {
      spacing = 10
      children = Seq(renameProjChBox,
        new HBox {
          spacing = 30
          disable <== !renameProjChBox.selected
          children = Seq(projNameLabel, projNameTextFiled)
        })
    }

    // Layout
    val browseBinDirPanel = new HBox {
      spacing = 30
      children = Seq(projectBinDirLabel, binDirPathTextField, browseBinButton)
    }
    val browseProjectPanel = new HBox {
      spacing = 30
      children = Seq(archivePathLabel, projDirPathTextField, browseButton)
    }
    val restoreProjectPanel = new VBox {
      spacing = 10
      children = Seq(notificationsPanel,
        browseBinDirPanel,
        browseProjectPanel,
        projOwnerPanel,
        projNamePanel)
    }

    // Style
    Seq(binDirPathTextField,
      projDirPathTextField,
      projNameTextFiled,
      ownerList).foreach { node =>
        node.prefWidth = 200
        node.hgrow_=(Priority.ALWAYS)
      }

    Seq(projectBinDirLabel,
      archivePathLabel,
      projNameLabel,
      ownerLabel).foreach { node =>
        node.prefWidth = 200
      }

    val contentPane = new VBox {
      alignment = Pos.Center
      spacing = 5
      padding = Insets(10)
      children = Seq(restoreProjectPanel)
    }

    dialog.dialogPane().content = contentPane

    val okButton = dialog.dialogPane().lookupButton(ButtonType.OK)

    /* Check dialog input fields */

    // Bin directory path must have pg_dump application
    val isValidBinDirectory = BooleanProperty(false)
    binDirPathTextField.text.onChange { (_, _, newValue) =>
      if (new File(newValue).exists && new File(newValue).listFiles().find(_.getName.matches("^pg_dump.exe$")).isDefined)
        isValidBinDirectory.value = true else isValidBinDirectory.value = false
    }
    errorBinPathLabel.visible <== (binDirPathTextField.text.isEmpty || !isValidBinDirectory)
    // Archive location must not be empty
    val isValidProjectDir = BooleanProperty(false)
    projDirPathTextField.text.onChange { (_, _, newValue) =>
      if (new File(newValue).exists) isValidProjectDir.value = true else isValidProjectDir.value = false
    }
    errorArchivePathLabel.visible <== (projDirPathTextField.text.isEmpty || !isValidProjectDir)
    //project owner must not be empty
    errorProjOwnerLabel.visible <== ownerList.getSelectionModel().selectedItemProperty.isNull
    var projectName: String = null
    projNameTextFiled.text.onChange { (_, _, newValue) =>
      if (newValue.trim.isEmpty) projectName = null else projectName = newValue
    }
    // Enable/Disable OK button depending on whether all data was entered.
    okButton.disable <== (!isValidBinDirectory || !isValidProjectDir || ownerList.getSelectionModel().selectedItemProperty.isNull)

    // Request focus on the bin directory path field by default.
    Platform.runLater(binDirPathTextField.requestFocus())

    dialog.resultConverter = dialogButton =>
      if (dialogButton == ButtonType.OK)
        Result(ownerList.getValue.getId, binDirPathTextField.text(), projDirPathTextField.text(), Option(projectName))
      else
        null

    val result = dialog.showAndWait()

    result match {
      case Some(Result(ownerId, binDirPath, archivedProjDirPath, projectName)) =>
        Some(Result(ownerId, binDirPath, archivedProjDirPath, projectName))
      case _ => None
    }
  }
}
