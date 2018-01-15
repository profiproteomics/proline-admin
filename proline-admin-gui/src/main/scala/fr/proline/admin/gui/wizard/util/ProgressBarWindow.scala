package fr.proline.admin.gui.wizard.util

import scalafx.Includes._
import scalafx.geometry.Insets
import scalafx.geometry.Pos
import scalafx.scene.Scene
import scalafx.scene.control.{ Button, Label }
import scalafx.scene.layout.{ VBox, HBox, StackPane }
import scalafx.scene.input.KeyEvent
import scalafx.scene.control.ProgressIndicator
import scalafx.stage.Modality
import scalafx.stage.Stage
import scalafx.concurrent.{ Task, Worker }
import fr.proline.admin.gui.Wizard
import fr.proline.admin.gui.wizard.util._
import fr.profi.util.scalafx.ScalaFxUtils
import fr.profi.util.scalafx.ScalaFxUtils.newVSpacer
import fr.proline.admin.gui.util.FxUtils
import fr.proline.admin.gui.IconResource

/**
 * builds progressBar Window
 *
 */
class ProgressBarWindow(
    wTitle: String,
    wText: String,
    wParent: Option[Stage],
    isResizable: Boolean = false,
    task: Task[_]) extends Stage {
  val popup = this
  val text = new Label(wText)
  val progress = new ProgressIndicator() {
    minWidth = 250
    progress <== task.progress
  }
  title = wTitle
  initModality(Modality.WINDOW_MODAL)
  if (wParent.isDefined) initOwner(wParent.get)
  popup.getIcons.add(FxUtils.newImageView(IconResource.IDENTIFICATION).image.value)
  //start task 
  new Thread(task).start()
  //create scene 
  scene = new Scene {
    onKeyPressed = (ke: KeyEvent) => { ScalaFxUtils.closeIfEscapePressed(popup, ke) }
    root = new VBox {
      minWidth = 250
      alignment = Pos.Center
      spacing = 15
      padding = Insets(10)
      children = List(
        new VBox {
          alignment = Pos.Center
          padding = Insets(10)
          spacing = 30
          children = Seq(
            text,
            progress, newVSpacer(5))
        })
    }
  }
  task.state.onChange { (_, _, newState) =>
    newState match {
      case Worker.State.Succeeded.delegate => {
        println("task succeeded.")
        popup.close()
      }
      case Worker.State.Failed.delegate => {
        println("task failed.")
        popup.close()
      }
      case Worker.State.Running.delegate => {
        println("task running.")
      }
      case Worker.State.Scheduled.delegate => {
        println("task scheduled.")
      }
      case Worker.State.Ready.delegate => {
        println("task ready.")
      }
      case _ => { println("Error: task has another state.") }
    }
  }
}

object ProgressBarWindow {
  def apply(
    wTitle: String,
    wText: String,
    wParent: Option[Stage],
    isResizable: Boolean = false,
    task: Task[_]) { new ProgressBarWindow(wTitle, wText, wParent, isResizable, task).showAndWait() }
}