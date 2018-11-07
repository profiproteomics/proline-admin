package fr.proline.admin.gui.wizard.monitor.component

import com.typesafe.scalalogging.LazyLogging
import scalafx.Includes._
import scalafx.stage.Stage
import scalafx.stage.Modality
import scalafx.geometry.Insets
import scalafx.geometry.Pos
import scalafx.scene.Scene
import scalafx.scene.layout.{ VBox, HBox }
import scalafx.scene.control.Label
import scalafx.scene.control.{ ComboBox, CheckBox }
import scalafx.scene.control.TextField
import scalafx.scene.control.Button
import scalafx.scene.control.TitledPane
import scalafx.scene.layout.Priority
import scalafx.scene.input.KeyEvent
import scalafx.util.StringConverter
import scalafx.scene.control.ProgressBar

import java.io.File

import fr.proline.core.orm.uds.UserAccount
import fr.proline.admin.gui.IconResource
import fr.proline.admin.gui.util.FxUtils
import fr.proline.admin.gui.process.UdsRepository
import fr.proline.admin.gui.wizard.service.RestoreProject
import fr.profi.util.scalafx.ScalaFxUtils
import fr.profi.util.scalafx.ScalaFxUtils.TextStyle

/**
 * RestoreProjectDialog builds a dialog to restore a project
 * @author aromdhani
 * @param wTitle The dialog title
 * @param wParent The parent of this dialog.
 * @param isResizable is the dialog resizable.
 *
 */

class RestoreProjectDialog(
    wTitle: String,
    wParent: Option[Stage],
    isResizable: Boolean = false) extends Stage with IMonitorForm with LazyLogging {
  val archiveProjectPane = this
  title = wTitle
  width_=(650)
  height_=(400)
  this.setResizable(isResizable)
  initModality(Modality.WINDOW_MODAL)
  if (wParent.isDefined) initOwner(wParent.get)
  archiveProjectPane.getIcons.add(FxUtils.newImageView(IconResource.IDENTIFICATION).image.value)
  //Component
  //warning labels
  val warningBinPathLabel = new Label {
    text = "Select a validated PostgreSQL bin directory.It must contain pg_restore.exe file."
    style = TextStyle.RED_ITALIC
    visible = false
    managed <== visible
  }
  val warningPathLabel = new Label {
    text = "Select a validated project location.The project folder must contains backup and project_properties.json files."
    style = TextStyle.RED_ITALIC
    visible = false
    managed <== visible
  }
  val warningOwnerLabel = new Label {
    text = "Select a project owner."
    style = TextStyle.RED_ITALIC
    visible = false
    managed <== visible
  }
  val warningDefinedProjectLabel = new Label {
    text = "This project name already defined for this user. Please rename your project."
    visible = false
    style = TextStyle.RED_ITALIC
    managed <== visible
  }

  val warningPanel = new VBox {
    spacing = 5
    children = Seq(warningBinPathLabel, warningPathLabel, warningOwnerLabel, warningDefinedProjectLabel)
  }

  //bin directory 
  val projectBinDirLabel = new Label("Select a PostgreSQL bin directory")
  val projectBinDirTextField = new TextField {
    if (RestoreProjectDialog.iniBinDirPath.isDefined) text = RestoreProjectDialog.iniBinDirPath.get
    text.onChange { (_, oldText, newText) =>
      RestoreProjectDialog.iniBinDirPath = Some(newText)
    }
  }
  val browseBinButton = new Button {
    text = "Browse..."
    graphic = FxUtils.newImageView(IconResource.LOAD)
    onAction = handle {
      _browseBinDir()
    }
  }

  //project directory
  val projectPathLabel = new Label("Select a project directory")
  val projectPathTextField = new TextField {}
  val browseButton = new Button {
    text = "Browse..."
    graphic = FxUtils.newImageView(IconResource.LOAD)
    onAction = handle {
      _browseProjectDir()
    }
  }

  //rename project 
  val renameProjectChbx = new CheckBox {
    text = "Rename Project"
    selected = false
  }
  val projectNameLabel = new Label("Project name")
  val projecNameTextField = new TextField {
    disable <== !renameProjectChbx.selected
  }
  val projectDescLabel = new Label("Project description")
  val projectDescTextField = new TextField {
    disable <== !renameProjectChbx.selected
  }

  //project owner 
  val projectOwnerLabel = new Label("Project owner")
  val ownerList = new ComboBox[UserAccount](UdsRepository.getAllUserAccounts()) {
    converter = StringConverter.toStringConverter((user: UserAccount) => user.getLogin)
  }

  //main buttons 
  val restoreButton = new Button {
    text = "Restore"
    graphic = FxUtils.newImageView(IconResource.TICK)
    onAction = handle {
      //check form and start task to restore project 
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
  Seq(projectBinDirLabel,
    projectNameLabel,
    projectOwnerLabel,
    projectDescLabel,
    projectPathLabel,
    projectBinDirTextField,
    projecNameTextField,
    projectDescTextField,
    projectPathTextField,
    ownerList,
    renameProjectChbx).foreach { component =>
      component.prefWidth = 200
      component.hgrow_=(Priority.ALWAYS)
    }

  Seq(restoreButton,
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
  val projectNamePanel = new HBox {
    spacing = 30
    children = Seq(projectNameLabel, projecNameTextField)
  }
  val projectDescPanel = new HBox {
    spacing = 30
    children = Seq(projectDescLabel, projectDescTextField)
  }
  val projectOwnerPanel = new HBox {
    spacing = 30
    children = Seq(projectOwnerLabel, ownerList)
  }
  val loadProjectPanel = new VBox {
    spacing = 10
    children = Seq(warningPanel, browseBinDiPanel, browseProjectPanel, renameProjectChbx, projectNamePanel, projectDescPanel, projectOwnerPanel)
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
        children = Seq(restoreButton,
          exitButton)
      }, informationPanel)
  }
  scene = new Scene {
    onKeyPressed = (ke: KeyEvent) => { ScalaFxUtils.closeIfEscapePressed(archiveProjectPane, ke) }
    root = new TitledPane {
      text = "Restore project"
      expanded_=(true)
      collapsible_=(false)
      margin = Insets(5, 5, 5, 5)
      content_=(contentPane)
    }
  }

  /** browse bin directory path */
  def _browseBinDir() {
    val fileOp = Option(FxUtils.browseDirectory(
      dcTitle = "Select a PostgrSQL bin directory",
      dcInitialDir = projectBinDirTextField.getText,
      dcInitOwner = archiveProjectPane))
    fileOp.foreach { file =>
      val newPath = file.getPath()
      projectBinDirTextField.text = newPath
    }
  }

  /** browse a project location */
  def _browseProjectDir() {
    val fileOp = Option(FxUtils.browseDirectory(
      dcTitle = "Select a project directrory",
      dcInitialDir = projectPathTextField.getText,
      dcInitOwner = archiveProjectPane))
    fileOp.foreach { file =>
      val newPath = file.getPath()
      projectPathTextField.text = newPath
    }
  }

  /** check if project name is already defined */
  def isDefinedProjName(projectName: Option[String] = None, projectOwnerId: Long): Boolean = {
    var isExistedProjName = false
    projectName.foreach(name => if (UdsRepository.findProjectsByOwnerId(projectOwnerId).find(_.getName == (name)).isDefined) {
      warningDefinedProjectLabel.setVisible(true)
      isExistedProjName = false
    } else {
      warningDefinedProjectLabel.setVisible(false)
      isExistedProjName = true
    })
    isExistedProjName
  }

  /** Check  that's a validated archive folder directory */
  def isValidatedProjectDir(projectPath: File): Boolean = {
    val projectFiles = projectPath.listFiles.toList
    Map("UDS" -> projectFiles.find(_.getName.matches("(project_properties.json){1}")),
      "MSI" -> projectFiles.find(_.getName.matches("(msi_db_project_[0-9]+.bak){1}")),
      "LCMS" -> projectFiles.find(_.getName.matches("(lcms_db_project_[0-9]+.bak){1}"))).values.forall(_.isDefined)
  }

  /** Check that postgreSQL bin directory is valid */
  def isValidatedBinDir(BinPath: String): Boolean = {
    try {
      new File(new File(BinPath), "pg_restore.exe").exists
    } catch {
      case t: Throwable =>
        logger.error("Error while trying to check PostgreSQL bin directory: ", t.printStackTrace())
        false
    }
  }

  /** check fields and show the warning/error messages */
  def checkFields(): Boolean = {
    if (!projectBinDirTextField.getText.isEmpty && isValidatedBinDir(projectBinDirTextField.getText)) {
      warningBinPathLabel.visible_=(false)
    } else {
      warningBinPathLabel.visible_=(true)
    }
    if ((new File(projectPathTextField.getText).exists) && isValidatedProjectDir(new File(projectPathTextField.getText))) {
      warningPathLabel.visible_=(false)
    } else {
      warningPathLabel.visible_=(true)
    }
    if (!ownerList.selectionModel.get.isEmpty) {
      warningOwnerLabel.visible_=(false)
    } else {
      warningOwnerLabel.visible_=(true)
    }
    //isRestoreProject is true only when all fields are valid 
    Seq(!projectBinDirTextField.getText.isEmpty && isValidatedBinDir(projectBinDirTextField.getText),
      new File(projectPathTextField.getText).exists && isValidatedProjectDir(new File(projectPathTextField.getText)),
      !ownerList.selectionModel.get.isEmpty).forall(_ == (true))
  }

  /** Validate and create restore project task */
  def validate(): Unit = {
    if (checkFields()) {
      var binDirPath = projectBinDirTextField.getText
      var projectOwner = ownerList.getValue.getId
      var projectDirPath = projectPathTextField.getText
      //project name must not be empty 
      var projectName: Option[String] = if (projecNameTextField.getText.isEmpty) None else Some(projecNameTextField.getText)
      if (projectName.isDefined) {
        //the project name is defined from the dialog 
        logger.debug(s"The project name used is =#$projectName ")
        if (isDefinedProjName(projectName, projectOwner)) RestoreProject(projectOwner, projectDirPath, binDirPath, projectName, archiveProjectPane).restart()
      } else {
        // the project name will be imported from project_properties.json file 
        logger.debug(s"The project name will be imported from the json file.")
        RestoreProject(projectOwner, projectDirPath, binDirPath, projectName, archiveProjectPane).restart()
      }
    }
  }

  /** exit and close restore project dialog */
  def exit(): Unit = {
    archiveProjectPane.close()
  }
}
object RestoreProjectDialog {
  var iniBinDirPath: Option[String] = None
  /**
   * @param wTitle The dialog title
   * @param wParent The parent of this dialog.
   * @param isResizable is the dialog resizable.
   */
  def apply(
    wTitle: String,
    wParent: Option[Stage],
    isResizable: Boolean = false) { new RestoreProjectDialog(wTitle, wParent, isResizable).showAndWait() }
}