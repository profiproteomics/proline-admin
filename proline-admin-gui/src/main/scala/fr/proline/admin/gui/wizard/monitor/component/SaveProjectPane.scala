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
import fr.proline.admin.gui.IconResource
import fr.proline.core.orm.uds.UserAccount

/**
 * builds save project pane
 * @author aromdhani
 *
 */

class SaveProjectPane(
    wTitle: String,
    wParent: Option[Stage],
    isResizable: Boolean = false) extends Stage with LazyLogging {
  val saveProjectPane = this
  title = wTitle
  minWidth_=(400)
  minHeight_=(200)
  width_=(600)
  height_=(200)
  initModality(Modality.WINDOW_MODAL)
  if (wParent.isDefined) initOwner(wParent.get)
  saveProjectPane.getIcons.add(FxUtils.newImageView(IconResource.IDENTIFICATION).image.value)
  // Component 
  val projectPathLabel = new Label("Select project save location")
  val projectPathTextField = new TextField()
  val saveButton = new Button {
    text = "Save"
    graphic = FxUtils.newImageView(IconResource.TICK)
    onAction = handle {

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
      _browseProjectSaveDir()
    }
  }

  //Style 
  Seq(projectPathLabel, projectPathTextField).foreach { component =>
    component.prefWidth = 200
    component.hgrow_=(Priority.ALWAYS)
  }

  Seq(saveButton,
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
  val loadProjectPanel = new VBox {
    spacing = 10
    children = Seq(browseProjectPanel)
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
          children = Seq(saveButton,
            cancelButton)
        })
    }
  }

  /** browse a project save location */
  def _browseProjectSaveDir() {
    val file = FxUtils.browseDirectory(
      dcTitle = "Select save project location",
      dcInitialDir = projectPathTextField.text(),
      dcInitOwner = saveProjectPane)
    try {
      val newPath = file.getPath()
      if (file != null) {
        projectPathTextField.text = newPath
      }
    } catch {
      case t: Throwable => logger.error("error while trying to select a project save location", t)
    }
  }
  /** cancel and close load project popup */
  def cancel(): Unit = {
    saveProjectPane.close()
  }
}
object SaveProjectPane {
  def apply(
    wTitle: String,
    wParent: Option[Stage],
    isResizable: Boolean = false) { new SaveProjectPane(wTitle, wParent, isResizable).showAndWait() }
}