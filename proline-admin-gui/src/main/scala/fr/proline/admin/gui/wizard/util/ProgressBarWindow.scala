package fr.proline.admin.gui.wizard.util

import scalafx.Includes._
import scalafx.geometry.Insets
import scalafx.geometry.Pos
import scalafx.scene.Scene
import scalafx.scene.control.Button
import scalafx.scene.control.Label
import scalafx.scene.input.KeyEvent
import scalafx.scene.layout.StackPane
import scalafx.stage.Modality
import scalafx.stage.Stage
import fr.profi.util.scalafx.ScalaFxUtils
import fr.proline.admin.gui.Wizard
import scalafx.scene.layout.VBox
import scalafx.scene.layout.HBox
import scalafx.scene.control.ProgressIndicator
import fr.proline.admin.gui.wizard.component.DbMaintenance
import javafx.concurrent.WorkerStateEvent
import javafx.event.EventHandler
import fr.profi.util.scalafx.ScalaFxUtils.newVSpacer
import fr.proline.admin.gui.wizard.util._
/**
 * progress bar window on upgrade Proline databases
 */

class ProgressBarWindow(
  wTitle: String,
  task: DbMaintenance,
  wParent: Option[Stage] = Option(Wizard.stage),
  isResizable: Boolean = false) extends Stage {
  val popup = this
  title = wTitle
  initModality(Modality.WINDOW_MODAL)
  if (wParent.isDefined) initOwner(wParent.get)
  task.start()
  scene = new Scene {
    onKeyPressed = (ke: KeyEvent) => { ScalaFxUtils.closeIfEscapePressed(popup, ke) }
    root = new VBox {
      alignment = Pos.Center
      spacing = 15
      padding = Insets(10)
      content = List(
        new VBox {
          alignment = Pos.Center
          padding = Insets(10)
          spacing = 30
          content = Seq(
            new Label("Setup/Update Proline databases\n  \t\t in progress ..."),
            new ProgressIndicator {
              progress <== task.progress
            }, newVSpacer(5))
        })
    }
  }
  task.setOnSucceeded(new EventHandler[WorkerStateEvent] {
    override def handle(event: WorkerStateEvent) {
      popup.close()
    }
  })
  task.setOnFailed(new EventHandler[WorkerStateEvent] {
    override def handle(event: WorkerStateEvent) {
      PopupHelpWindow("Setup/Update", "Setup/Update Proline databases failed !")
      popup.close()
    }
  })
}
object ProgressBarWindow {
  def apply(
    wTitle: String,
    task: DbMaintenance,
    wParent: Option[Stage] = Option(Wizard.stage),
    isResizable: Boolean = false) { new ProgressBarWindow(wTitle, task, wParent, isResizable).showAndWait() }
}