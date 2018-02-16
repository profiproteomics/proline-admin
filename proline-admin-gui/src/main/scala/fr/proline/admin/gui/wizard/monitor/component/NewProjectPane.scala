package fr.proline.admin.gui.wizard.monitor.component

import com.typesafe.scalalogging.LazyLogging
import scalafx.Includes._
import scalafx.geometry.Insets
import scalafx.geometry.Pos
import scalafx.scene.Scene
import scalafx.scene.control.Button
import scalafx.scene.control.Label
import scalafx.scene.control.ComboBox
import scalafx.scene.input.KeyEvent
import scalafx.scene.layout.StackPane
import scalafx.stage.Modality
import scalafx.stage.Stage
import fr.profi.util.scalafx.ScalaFxUtils
import scalafx.scene.layout.Priority
import scalafx.scene.layout.{ VBox, HBox }
import scalafx.scene.control.TextField
import com.sun.javafx.css.StyleClass
import fr.proline.admin.gui.util.FxUtils
import fr.proline.admin.gui.IconResource
import fr.proline.core.orm.uds.UserAccount
import fr.proline.admin.gui.Monitor
import fr.profi.util.scalafx.ScalaFxUtils
import fr.profi.util.scalafx.ScalaFxUtils.TextStyle
import fr.proline.admin.gui.wizard.service.NewProject
import fr.proline.admin.gui.wizard.util.ProgressBarPopup
import scalafx.collections.ObservableBuffer
import scalafx.util.StringConverter

/**
 * build new Project panel
 * @author aromdhani
 *
 */

class NewProjectPane(
    wTitle: String,
    wParent: Option[Stage],
    isResizable: Boolean = false) extends Stage with LazyLogging {
  val newProjectPane = this
  title = wTitle
  minWidth_=(400)
  minHeight_=(200)
  initModality(Modality.WINDOW_MODAL)
  if (wParent.isDefined) initOwner(wParent.get)
  newProjectPane.getIcons.add(FxUtils.newImageView(IconResource.IDENTIFICATION).image.value)

  // Component

  val projectNameLabel = new Label("Project name")
  val projectNameTextField = new TextField()
  val projectDescLabel = new Label("Project description")
  val projectDescTextField = new TextField()
  val ownerLabel = new Label("Project owner")
  val ownerList = new ComboBox[UserAccount](ProjectPane.userList) {
    converter = StringConverter.toStringConverter((user: UserAccount) => user.getLogin)
  }

  val addButton = new Button("Add") {
    graphic = FxUtils.newImageView(IconResource.TICK)
    onAction = handle {
      if (!projectNameTextField.getText.isEmpty()) {
        if (!ownerList.selectionModel.apply().isEmpty()) {
          try {
            val projectTask = new NewProject(projectNameTextField.getText, projectDescTextField.getText, ownerList.getValue.getId)
            ProgressBarPopup("New project", "Creating project in progress ...", Some(newProjectPane), true, projectTask.Worker)
            newProjectPane.close()
            //ProjectPane.refreshTableView()
          } catch {
            case t: Throwable => logger.error("Error while trying to execute task create project")
          }
        } else {
          warningUserLabel.visible_=(true)
        }
      }
    }
  }
  val cancelButton = new Button("Cancel") {
    graphic = FxUtils.newImageView(IconResource.CANCEL)
    onAction = handle {
      newProjectPane.close()
    }
  }
  val warningNameLabel = new Label {
    text = "Project name must not be empty"
    visible <== projectNameTextField.text.isEmpty()
    prefWidth = 200
    style = TextStyle.RED_ITALIC
  }

  val warningUserLabel = new Label {
    text = "Project owner must not be empty"
    visible = false
    prefWidth = 200
    style = TextStyle.RED_ITALIC
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
    children = Seq(warningNameLabel, warningUserLabel, projectNamePanel, projectDescPanel, projectOwnerPanel)
  }

  Seq(addButton,
    cancelButton).foreach { b =>
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
  scene = new Scene {
    onKeyPressed = (ke: KeyEvent) => { ScalaFxUtils.closeIfEscapePressed(newProjectPane, ke) }
    root = new VBox {
      alignment = Pos.Center
      spacing = 15
      padding = Insets(10)
      children = List(
        projectPanel,
        new HBox {
          alignment = Pos.Center
          padding = Insets(10)
          spacing = 30
          children = Seq(addButton,
            cancelButton)
        })
    }
  }
}
object NewProjectPane {
  def apply(
    wTitle: String,
    wParent: Option[Stage],
    isResizable: Boolean = false) { new NewProjectPane(wTitle, wParent, isResizable).showAndWait() }
}