package fr.proline.admin.gui.wizard.monitor.component

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

import fr.proline.admin.gui.Wizard
import fr.profi.util.scalafx.ScalaFxUtils
/**
 * builds new Project panel
 *
 */

class NewProjectPane(
    wTitle: String,
    wParent: Option[Stage],
    isResizable: Boolean = false) extends Stage {
  val popup = this
  title = wTitle
  initModality(Modality.WINDOW_MODAL)
  if (wParent.isDefined) initOwner(wParent.get)
  popup.getIcons.add(FxUtils.newImageView(IconResource.IDENTIFICATION).image.value)
  // component
  val projectNameLabel = new Label("Project name")
  val projectNameTextField = new TextField()
  val projectDescLabel = new Label("Project description")
  val projectDescTextField = new TextField()
  val ownerLabel = new Label("Project owner")
  val ownerList = new ComboBox {
  }
  val addButton = new Button("Add") {
    graphic = FxUtils.newImageView(IconResource.TICK)
    onAction = handle {
      //create project task
      popup.close()
    }
  }
  val cancelButton = new Button("Cancel") {
    graphic = FxUtils.newImageView(IconResource.CANCEL)
    onAction = handle {
      popup.close()
    }
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
    children = Seq(projectNamePanel, projectDescPanel, projectOwnerPanel)
  }

  Seq(addButton,
    cancelButton).foreach { b =>
      b.prefHeight = 20
      b.prefWidth = 120
      b.styleClass += ("mainButtons")
    }
  Seq(projectNameTextField,
    projectDescTextField, ownerList).foreach { component =>
      component.prefWidth = 120
      component.hgrow_=(Priority.ALWAYS)
    }
  Seq(projectNameLabel,
    projectDescLabel, ownerLabel).foreach { label =>
      label.prefWidth = 120
    }
  scene = new Scene {
    onKeyPressed = (ke: KeyEvent) => { ScalaFxUtils.closeIfEscapePressed(popup, ke) }
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