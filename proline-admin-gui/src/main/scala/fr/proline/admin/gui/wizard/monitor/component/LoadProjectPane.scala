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
 * builds load project pane
 * @author aromdhani
 *
 */

class LoadProjectPane(
    wTitle: String,
    wParent: Option[Stage],
    isResizable: Boolean = false) extends Stage with LazyLogging {
  val loadProjectPane = this
  title = wTitle
  minWidth_=(400)
  minHeight_=(200)
  width_=(450)
  height_=(350)
  initModality(Modality.WINDOW_MODAL)
  if (wParent.isDefined) initOwner(wParent.get)
  loadProjectPane.getIcons.add(FxUtils.newImageView(IconResource.LOAD).image.value)

  //Component 
  val projectNameLabel = new Label("Project name")
  val projectDescLabel = new Label("Project description")
  val projectOwnerLabel = new Label("Project owner")
  val projecNameTextField = new TextField()
  val projectDescTextField = new TextField()
  val ownerList = new ComboBox[UserAccount](ProjectPane.userList) {
    converter = StringConverter.toStringConverter((user: UserAccount) => user.getLogin)
  }
  val loadButton = new Button {
    text = "Load"
    graphic = FxUtils.newImageView(IconResource.TICK)
    onAction = handle {
      load()
    }
  }
  val cancelButton = new Button {
    text = "Cancel"
    graphic = FxUtils.newImageView(IconResource.CANCEL)
    onAction = handle {
      cancel()
    }
  }
  //Style 
  Seq(projectNameLabel, projectOwnerLabel, projectDescLabel, projecNameTextField, projectDescTextField, ownerList).foreach { component =>
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
    children = Seq(projectNamePanel, projectDescPanel, projectOwnerPanel)
  }
  scene = new Scene {
    onKeyPressed = (ke: KeyEvent) => { ScalaFxUtils.closeIfEscapePressed(loadProjectPane, ke) }
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

  /** load project to Proline databases */
  def load(): Unit = {

  }
  /** cancel and close load project popup */
  def cancel(): Unit = {
    loadProjectPane.close()
  }
}
object LoadProjectPane {
  def apply(
    wTitle: String,
    wParent: Option[Stage],
    isResizable: Boolean = false) { new LoadProjectPane(wTitle, wParent, isResizable).showAndWait() }
}