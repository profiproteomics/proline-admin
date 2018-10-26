package fr.proline.admin.gui.wizard.monitor.component

import com.typesafe.scalalogging.LazyLogging

import scalafx.Includes._
import scalafx.stage.Stage
import scalafx.stage.Modality
import scalafx.geometry.{ Insets, Pos }
import scalafx.scene.Scene
import scalafx.scene.layout.{ VBox, HBox, Priority }
import scalafx.scene.control.Label
import scalafx.scene.control.ComboBox
import scalafx.scene.control.TextField
import scalafx.scene.control.TitledPane
import scalafx.scene.control.Button
import scalafx.scene.input.KeyEvent
import scalafx.scene.control.ProgressBar
import java.io.File
import fr.proline.admin.gui.util.FxUtils
import fr.proline.admin.gui.IconResource
import fr.proline.core.orm.uds.UserAccount
import fr.proline.admin.gui.wizard.service.ArchiveProject
import fr.profi.util.scalafx.ScalaFxUtils
import fr.profi.util.scalafx.ScalaFxUtils.TextStyle

/**
 * ArchiveProjDialog build the archive project dialog.
 * @author aromdhani
 * @param wTitle The dialog title
 * @param wParent The parent of this dialog.
 * @param isResizable is the dialog resizable.
 *
 */

class ArchiveProjDialog(
    wTitle: String,
    wParent: Option[Stage],
    isResizable: Boolean = true) extends Stage with IMonitorForm with LazyLogging {
  val archiveProjectPane = this
  title = wTitle
  width_=(600)
  height_=(270)
  this.setResizable(isResizable)
  initModality(Modality.WINDOW_MODAL)
  if (wParent.isDefined) initOwner(wParent.get)
  archiveProjectPane.getIcons.add(FxUtils.newImageView(IconResource.IDENTIFICATION).image.value)
  //warning label 
  val warningSelectdProjectLabel = new Label {
    text = "There is no selected project. Please select a project from the table."
    style = TextStyle.RED_ITALIC
    visible = false
    managed <== visible
  }
  val warningBinPathLabel = new Label {
    text = "Select a validated PostgreSQL bin directory. It must contain pg_dump.exe file."
    style = TextStyle.RED_ITALIC
    visible = false
    managed <== visible
  }
  val warningLocationPathLabel = new Label {
    text = "Select a validated archive location. It must not be empty."
    style = TextStyle.RED_ITALIC
    visible = false
    managed <== visible
  }
  val warningPanel = new VBox {
    spacing = 5
    children = Seq(warningSelectdProjectLabel, warningBinPathLabel, warningLocationPathLabel)
  }

  //components 
  val projectBinDirLabel = new Label("Select a PostgreSQL bin directory")
  val projectBinDirTextField = new TextField {
    if (ArchiveProjDialog.iniBinDirPath.isDefined) text = ArchiveProjDialog.iniBinDirPath.get
    text.onChange { (_, oldText, newText) =>
      ArchiveProjDialog.iniBinDirPath = Some(newText)
    }
  }
  val browseBinButton = new Button {
    text = "Browse..."
    graphic = FxUtils.newImageView(IconResource.LOAD)
    onAction = handle {
      _browseBinDir()
    }
  }

  val projectPathLabel = new Label("Select a project archive location")
  val projectPathTextField = new TextField {
    if (ArchiveProjDialog.iniArchiveProjectPath.isDefined) text = ArchiveProjDialog.iniArchiveProjectPath.get
    text.onChange { (_, oldText, newText) =>
      ArchiveProjDialog.iniArchiveProjectPath = Some(newText)
    }
  }

  val browseButton = new Button {
    text = "Browse..."
    graphic = FxUtils.newImageView(IconResource.LOAD)
    onAction = handle {
      _browseProjectArchiveDir()
    }
  }
  //main buttons  
  val archiveButton = new Button {
    text = "Archive"
    graphic = FxUtils.newImageView(IconResource.TICK)
    onAction = handle {
      validate()
    }
  }

  val exitButton = new Button {
    text = "Exit"
    graphic = FxUtils.newImageView(IconResource.CANCEL)
    onAction = handle {
      exit()
    }
  }

  //Style 
  Seq(projectPathLabel, projectPathTextField, projectBinDirLabel, projectBinDirTextField).foreach { component =>
    component.prefWidth = 200
    component.hgrow_=(Priority.ALWAYS)
  }
  Seq(archiveButton,
    exitButton).foreach { b =>
      b.prefHeight = 20
      b.prefWidth = 120
      b.styleClass += ("mainButtons")
    }
  val informationLabel = new Label {
    text = ""
    visible = false
    managed <== visible
    alignmentInParent = Pos.BOTTOM_CENTER
    style = TextStyle.BLUE_ITALIC
  }
  val progressBar = new ProgressBar {
    visible = false
    managed <== visible
    prefWidth = archiveProjectPane.width.get
  }
  val informationPanel = new VBox {
    alignmentInParent = Pos.BOTTOM_CENTER
    spacing = 5
    children = Seq(new HBox {
      alignment = Pos.Center
      padding = Insets(10)
      children = Seq(new Label(""), informationLabel)
    }, progressBar)
  }
  //Layout
  val browseBinDiPanel = new HBox {
    spacing = 30
    children = Seq(projectBinDirLabel, projectBinDirTextField, browseBinButton)
  }
  val browseProjectPanel = new HBox {
    spacing = 30
    children = Seq(projectPathLabel, projectPathTextField, browseButton)
  }
  val loadProjectPanel = new VBox {
    spacing = 10
    children = Seq(warningPanel, browseBinDiPanel, browseProjectPanel)
  }
  val contentPane = new VBox {
    alignment = Pos.Center
    spacing = 5
    padding = Insets(5)
    children = List(
      loadProjectPanel,
      new HBox {
        alignment = Pos.Center
        padding = Insets(5)
        spacing = 30
        children = Seq(archiveButton,
          exitButton)
      }, informationPanel)
  }
  scene = new Scene {
    onKeyPressed = (ke: KeyEvent) => { ScalaFxUtils.closeIfEscapePressed(archiveProjectPane, ke) }
    root = new TitledPane {
      text = "Archive project"
      expanded_=(true)
      collapsible_=(false)
      margin = Insets(5, 5, 5, 5)
      content_=(contentPane)
    }
  }

  /** browse postgreSQL bin directory path */
  def _browseBinDir() {
    val fileOp = Option(FxUtils.browseDirectory(
      dcTitle = "Select a PostgreSQL bin directory",
      dcInitialDir = projectBinDirTextField.getText,
      dcInitOwner = archiveProjectPane))
    fileOp.foreach { file =>
      val newPath = file.getPath()
      projectBinDirTextField.text = newPath
    }
  }

  /** browse a project archive location directory */
  def _browseProjectArchiveDir() {
    val fileOp = Option(FxUtils.browseDirectory(
      dcTitle = "Select a project archive location",
      dcInitialDir = projectPathTextField.getText,
      dcInitOwner = archiveProjectPane))
    fileOp.foreach { file =>
      val newPath = file.getPath()
      projectPathTextField.text = newPath
    }
  }

  /**
   *  Check postgreSQL bin directory
   *  @param BinPath the bin directory path.
   */
  def isValidBinDir(BinPath: String): Boolean = {
    try {
      new File(new File(BinPath), "pg_dump.exe").exists
    } catch {
      case t: Throwable =>
        logger.error("Error while trying to check PostgreSQL bin directory: ", t.printStackTrace())
        false
    }
  }

  /** Check all required fields and show warning/error labels */
  def checkFields(): Boolean = {
    if (!projectBinDirTextField.getText.isEmpty && isValidBinDir(projectBinDirTextField.getText)) {
      warningBinPathLabel.visible_=(false)
    } else {
      warningBinPathLabel.visible_=(true)
    }
    if (!projectPathTextField.getText.isEmpty && new File(projectPathTextField.getText).exists()) {
      warningLocationPathLabel.visible_=(false)
    } else {
      warningLocationPathLabel.visible_=(true)
    }
    if (ProjectPane.selectedProject.isDefined) {
      warningSelectdProjectLabel.visible_=(false)
    } else {
      warningSelectdProjectLabel.visible_=(true)
    }
    Seq(!projectBinDirTextField.getText.isEmpty && isValidBinDir(projectBinDirTextField.getText),
      !projectPathTextField.getText.isEmpty && new File(projectPathTextField.getText).exists(),
      ProjectPane.selectedProject.isDefined).forall(_.==(true))
  }

  /** Create new archive project task   */
  def validate(): Unit = {
    try {
      if (checkFields()) {
        val projectId = ProjectPane.selectedProject.get.id.getValue
        val binDirValue = projectBinDirTextField.getText
        val archivDirValue = projectPathTextField.getText
        val archiveProject = ArchiveProject(projectId, binDirValue, archivDirValue, archiveProjectPane)
        archiveProject.restart()
      }
    } catch {
      case t: Throwable => {
        logger.error("Error invalid parameters to archive the project: ", t.printStackTrace())

      }
    }
  }

  /**
   *  Exit and close the dialog
   */
  def exit(): Unit = {
    archiveProjectPane.close()
  }
}

object ArchiveProjDialog {
  var iniArchiveProjectPath: Option[String] = None
  var iniBinDirPath: Option[String] = None
  /**
   * @param wTitle The dialog title
   * @param wParent The parent of this dialog.
   * @param isResizable is the dialog resizable.
   */
  def apply(
    wTitle: String,
    wParent: Option[Stage],
    isResizable: Boolean = false) { new ArchiveProjDialog(wTitle, wParent, isResizable).showAndWait() }
}