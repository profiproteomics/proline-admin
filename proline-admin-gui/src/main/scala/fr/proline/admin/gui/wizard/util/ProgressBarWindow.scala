package fr.proline.admin.gui.wizard.util

import scalafx.Includes._
import scalafx.geometry.Insets
import scalafx.geometry.Pos
import scalafx.scene.Scene
import scalafx.scene.control.Button
import scalafx.scene.control.Label
import scalafx.scene.input.KeyEvent
import scalafx.scene.layout.StackPane
import scalafx.scene.layout.VBox
import scalafx.scene.layout.HBox
import scalafx.scene.control.ProgressIndicator
import scalafx.stage.Modality
import scalafx.stage.Stage
import fr.proline.admin.gui.Wizard
import fr.proline.admin.gui.wizard.util._
import fr.proline.admin.gui.wizard.component.DbMaintenance
import fr.profi.util.scalafx.ScalaFxUtils
import fr.profi.util.scalafx.ScalaFxUtils.newVSpacer
import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.util.{ Success, Failure }
/**
 * builds progress bar window
 *
 */
class ProgressBarWindow(
    wTitle: String,
    wParent: Option[Stage] = Option(Wizard.stage),
    isResizable: Boolean = false,
    f: Future[Unit]) extends Stage {
  val popup = this
  val progressIndic = new ProgressIndicator()
  title = wTitle
  initModality(Modality.WINDOW_MODAL)
  if (wParent.isDefined) initOwner(wParent.get)
  scene = new Scene {
    onKeyPressed = (ke: KeyEvent) => { ScalaFxUtils.closeIfEscapePressed(popup, ke) }
    root = new VBox {
      alignment = Pos.Center
      spacing = 15
      padding = Insets(10)
      children = List(
        new VBox {
          alignment = Pos.Center
          padding = Insets(10)
          spacing = 30
          children = Seq(
            new Label("Setup / Update Proline databases\n  \t\t in progress..."),
            progressIndic, newVSpacer(5))
        })
    }
  }
  f.onComplete {
    case Success(value) => {
      progressIndic.progress_=(100D)
      popup.close()
    }
    case Failure(error) => {
      popup.close()
      System.out.println("Error while trying to setup/update PRoline databases: ", error)
    }
  }
}
object ProgressBarWindow {
  def apply(
    wTitle: String,
    wParent: Option[Stage] = Option(Wizard.stage),
    isResizable: Boolean = false,
    f: Future[Unit]) { new ProgressBarWindow(wTitle, wParent, isResizable, f).showAndWait() }
}