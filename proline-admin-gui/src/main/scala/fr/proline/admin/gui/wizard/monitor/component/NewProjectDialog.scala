package fr.proline.admin.gui.wizard.monitor.component

import com.typesafe.scalalogging.LazyLogging

import scalafx.Includes._
import scalafx.stage.Stage
import scalafx.stage.Modality
import scalafx.geometry.{ Pos, Insets }
import scalafx.scene.Scene
import scalafx.scene.control.Button
import scalafx.scene.control.Label
import scalafx.scene.control.ComboBox
import scalafx.scene.control.TextField
import scalafx.scene.control.TitledPane
import scalafx.scene.control.ProgressBar
import scalafx.scene.input.KeyEvent
import scalafx.scene.layout.StackPane
import scalafx.scene.layout.Priority
import scalafx.scene.layout.{ VBox, HBox }
import scalafx.collections.ObservableBuffer
import scalafx.util.StringConverter

import fr.proline.core.orm.uds.UserAccount

import fr.proline.admin.gui.process.UdsRepository
import fr.proline.admin.gui.wizard.service.AddProject
import fr.proline.admin.gui.util.FxUtils
import fr.proline.admin.gui.IconResource
import fr.profi.util.scalafx.ScalaFxUtils
import fr.profi.util.scalafx.ScalaFxUtils.TextStyle

/**
 * NewProjectDialog build a dialog to create a new project
 * @author aromdhani
 * @param wTitle The dialog title
 * @param wParent The parent of this dialog.
 * @param isResizable is the dialog resizable.
 */

class NewProjectDialog(
    wTitle: String,
    wParent: Option[Stage],
    isResizable: Boolean = true) extends Stage with IMonitorForm with LazyLogging {
  val newProjectPane = this
  title = wTitle
  width_=(600)
  height_=(300)
  this.setResizable(isResizable)
  initModality(Modality.WINDOW_MODAL)
  if (wParent.isDefined) initOwner(wParent.get)
  newProjectPane.getIcons.add(FxUtils.newImageView(IconResource.IDENTIFICATION).image.value)
  // Component
  val warningNameLabel = new Label {
    text = "The project name must not be empty."
    visible = false
    managed <== visible
    style = TextStyle.RED_ITALIC
  }
  val warningUserLabel = new Label {
    text = "The project owner must not be empty. Select a project owner from the list."
    visible = false
    managed <== visible
    style = TextStyle.RED_ITALIC
  }
  val warningDefinedProjectLabel = new Label {
    text = "This project name is already used by this user. Please rename your project."
    visible = false
    managed <== visible
    style = TextStyle.RED_ITALIC
  }
  val warningPanel = new VBox {
    managed <== visible
    spacing = 5
    children = Seq(warningNameLabel, warningUserLabel, warningDefinedProjectLabel)
  }
  val projectNameLabel = new Label("Project name")
  val projectNameTextField = new TextField()

  val projectDescLabel = new Label("Project description")
  val projectDescTextField = new TextField()

  val ownerLabel = new Label("Project owner")
  val ownerList = new ComboBox[UserAccount](UdsRepository.getAllUserAccounts()) {
    converter = StringConverter.toStringConverter((user: UserAccount) => user.getLogin)
  }
  val addButton = new Button("Add") {
    graphic = FxUtils.newImageView(IconResource.TICK)
    onAction = handle {
      validate()
    }
  }
  val exitButton = new Button("Exit") {
    graphic = FxUtils.newImageView(IconResource.CANCEL)
    onAction = handle {
      exit()
    }
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
    prefWidth = newProjectPane.width.get
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
  //layout
  val projectNamePanel = new HBox {
    spacing = 30
    children = Seq(projectNameLabel, projectNameTextField)
  }
  val projectDescPanel = new HBox {
    spacing = 30
    children = Seq(projectDescLabel, projectDescTextField)
  }
  val projectOwnerPanel = new HBox {
    spacing = 30
    children = Seq(ownerLabel, ownerList)
  }
  val projectPanel = new VBox {
    spacing = 10
    children = Seq(warningPanel, projectNamePanel, projectDescPanel, projectOwnerPanel)
  }

  Seq(addButton,
    exitButton).foreach { b =>
      b.prefHeight = 20
      b.prefWidth = 120
      b.styleClass += ("mainButtons")
    }
  Seq(projectNameTextField,
    projectDescTextField, ownerList).foreach { component =>
      component.prefWidth = 200
      component.hgrow_=(Priority.ALWAYS)
    }

  Seq(projectNameLabel,
    projectDescLabel, ownerLabel).foreach { label =>
      label.prefWidth = 200
    }

  val contentPane = new VBox {
    alignment = Pos.Center
    spacing = 5
    padding = Insets(5)
    children = List(
      projectPanel,
      new HBox {
        alignment = Pos.Center
        padding = Insets(5)
        spacing = 30
        children = Seq(addButton,
          exitButton)
      }, informationPanel)
  }
  scene = new Scene {
    onKeyPressed = (ke: KeyEvent) => { ScalaFxUtils.closeIfEscapePressed(newProjectPane, ke) }
    root = new TitledPane {
      text = "New project"
      expanded_=(true)
      collapsible_=(false)
      margin = Insets(5, 5, 5, 5)
      content_=(contentPane)
    }
  }

  /** check new project panel fields */
  def checkFields(): Boolean = {
    if (!projectNameTextField.getText.isEmpty()) {
      warningNameLabel.visible_=(false)
    } else {
      warningNameLabel.visible_=(true)
    }
    if (!ownerList.selectionModel.apply().isEmpty()) {
      warningUserLabel.visible_=(false)
    } else {
      warningUserLabel.visible_=(true)
    }
    Seq(!projectNameTextField.getText.isEmpty(), !ownerList.selectionModel.apply().isEmpty()).forall(_.==(true))
  }

  /** add new project task */
  def validate(): Unit = {
    if (checkFields()) {
      try {
        var ownerId: Long = ownerList.getValue.getId
        var projectName = Some(projectNameTextField.getText)
        var projecDesc = Option(projectDescTextField.getText)
        if (isValidProjectName(projectName, ownerId)) {
          val createProject = new AddProject(projectNameTextField.getText, projectDescTextField.getText, ownerList.getValue.getId, newProjectPane)
          createProject.restart()
        }
      } catch {
        case t: Throwable => logger.error("Error while trying to execute task create project: ", t.printStackTrace())
      }
    }
  }
  /** check if project name is already defined */
  def isValidProjectName(projectName: Option[String], projectOwnerId: Long): Boolean =
    if (UdsRepository.findProjectsByOwnerId(projectOwnerId).find(_.getName == (projectName.get)).isDefined) {
      warningDefinedProjectLabel.setVisible(true)
      false
    } else {
      warningDefinedProjectLabel.setVisible(false)
      true
    }

  /** exit and close this dialog */
  def exit(): Unit = {
    newProjectPane.close()
  }
}
object NewProjectDialog {
  /**
   * @param wTitle The dialog title
   * @param wParent The parent of this dialog.
   * @param isResizable is the dialog resizable.
   */
  def apply(
    wTitle: String,
    wParent: Option[Stage],
    isResizable: Boolean = false) { new NewProjectDialog(wTitle, wParent, isResizable).showAndWait() }
}