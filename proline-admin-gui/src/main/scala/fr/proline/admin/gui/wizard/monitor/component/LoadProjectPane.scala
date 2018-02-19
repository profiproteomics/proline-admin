package fr.proline.admin.gui.wizard.monitor.component

import com.typesafe.scalalogging.LazyLogging
import scalafx.Includes._
import scalafx.stage.Stage
import scalafx.scene.Scene
import scalafx.stage.Modality
import scalafx.geometry.Insets
import scalafx.geometry.Pos
import scalafx.scene.layout.{ VBox, HBox }
import scalafx.scene.control.Label
import scalafx.scene.control.ComboBox
import scalafx.scene.control.TextField
import scalafx.scene.control.Button
import scalafx.util.StringConverter
import scalafx.scene.layout.Priority

import scalafx.scene.input.KeyEvent
import fr.profi.util.scalafx.ScalaFxUtils
import fr.proline.admin.gui.util.FxUtils
import fr.profi.util.scalafx.ScalaFxUtils.TextStyle
import fr.proline.admin.gui.IconResource
import fr.proline.core.orm.uds.UserAccount

/**
 * builds load project pane
 * @author aromdhani
 *
 */

class LoadProjectPane(
    wTitle: String,
    wParent: Option[Stage],
    isResizable: Boolean = false) extends Stage with LazyLogging {
  val saveProjectPane = this
  title = wTitle
  minWidth_=(400)
  minHeight_=(200)
  width_=(650)
  height_=(300)
  initModality(Modality.WINDOW_MODAL)
  if (wParent.isDefined) initOwner(wParent.get)
  saveProjectPane.getIcons.add(FxUtils.newImageView(IconResource.IDENTIFICATION).image.value)

  // Component 
  val projectPathLabel = new Label("Select project diretcory")
  val projectNameLabel = new Label("Project name")
  val projectDescLabel = new Label("Project description")
  val projectOwnerLabel = new Label("Project owner")
  val projectPathTextField = new TextField()
  val projecNameTextField = new TextField {
    disable = true
  }
  val projectDescTextField = new TextField {
    disable = true
  }
  val ownerList = new ComboBox[UserAccount](ProjectPane.userList) {
    converter = StringConverter.toStringConverter((user: UserAccount) => user.getLogin)
  }
  val loadButton = new Button {
    text = "Restore"
    graphic = FxUtils.newImageView(IconResource.TICK)
    onAction = handle {
      if (!ownerList.selectionModel.get.isEmpty()) {
        warningOwnerLabel.visible_=(false)
        //restore project task 
      } else {
        warningOwnerLabel.visible_=(true)
      }
    }
  }
  val cancelButton = new Button {
    text = "Cancel"
    graphic = FxUtils.newImageView(IconResource.CANCEL)
    onAction = handle {
      cancel()
    }
  }
  val browseButton = new Button {
    text = "Browse..."
    graphic = FxUtils.newImageView(IconResource.LOAD)
    onAction = handle {
      _browseProjectDir()
    }
  }
  val warningPathLabel = new Label {
    text = "Enter a validated project location.The project folder must contains SQL backup and project_properties.json files. "
    style = TextStyle.RED_ITALIC
    visible = false
  }
  val warningOwnerLabel = new Label {
    text = "Select a project owner."
    style = TextStyle.RED_ITALIC
    visible = false
  }
  Seq(projectNameLabel, projectOwnerLabel, projectDescLabel, projectPathLabel, projecNameTextField, projectDescTextField, projectPathTextField, ownerList).foreach { component =>
    component.prefWidth = 200
    component.hgrow_=(Priority.ALWAYS)
  }

  Seq(loadButton,
    cancelButton).foreach { b =>
      b.prefHeight = 20
      b.prefWidth = 120
      b.styleClass += ("mainButtons")
    }

  //Layout
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
    children = Seq(warningPathLabel, warningOwnerLabel, browseProjectPanel, projectNamePanel, projectDescPanel, projectOwnerPanel)
  }
  scene = new Scene {
    onKeyPressed = (ke: KeyEvent) => { ScalaFxUtils.closeIfEscapePressed(saveProjectPane, ke) }
    root = new VBox {
      alignment = Pos.Center
      spacing = 15
      padding = Insets(10)
      children = List(
        loadProjectPanel,
        new HBox {
          alignment = Pos.Center
          padding = Insets(10)
          spacing = 30
          children = Seq(loadButton,
            cancelButton)
        })
    }
  }

  /** browse a project location */
  def _browseProjectDir() {
    val file = FxUtils.browseDirectory(
      dcTitle = "Select project directrory",
      dcInitialDir = projectPathTextField.text(),
      dcInitOwner = saveProjectPane)
    try {
      if (!isValidProject(file)) {
        warningPathLabel.visible_=(true)
      } else {
        warningPathLabel.visible_=(false)
        val newPath = file.getPath()
        if (file != null) {
          projectPathTextField.text = newPath
        }
      }
    } catch {
      case t: Throwable => logger.error("error while trying to select a project location", t)
    }
  }
  /** check  list of files in project folder */
  def isValidProject(projectPath: java.io.File): Boolean = {
    projectPath.listFiles().foldLeft(true) {
      case (isValid, file) => {
        isValid && file.getName.matches("(lcms_db_project_[0-9]+.bak){1}|(msi_db_project_[0-9]+.bak){1}|(uds_db_schema.bak){1}|(project_properties.json){1}")
      }
    }
  }
  /** cancel and close load project popup */
  def cancel(): Unit = {
    saveProjectPane.close()
  }
}
object LoadProjectPane {
  def apply(
    wTitle: String,
    wParent: Option[Stage],
    isResizable: Boolean = false) { new LoadProjectPane(wTitle, wParent, isResizable).showAndWait() }
}