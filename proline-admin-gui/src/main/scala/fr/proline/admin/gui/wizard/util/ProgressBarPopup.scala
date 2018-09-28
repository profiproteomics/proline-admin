package fr.proline.admin.gui.wizard.util

import com.typesafe.scalalogging.LazyLogging
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
 * Build progressBar Window
 * @aromdhani
 *
 */
class ProgressBarPopup(
    wTitle: String,
    wText: String,
    wParent: Option[Stage],
    isResizable: Boolean = false) extends Stage with LazyLogging {
  val popup = this
  val text = new Label(wText)
  val progress = new ProgressIndicator() {
    minWidth = 250
  }
  title = wTitle
  initModality(Modality.WINDOW_MODAL)
  if (wParent.isDefined) initOwner(wParent.get)
  popup.getIcons.add(FxUtils.newImageView(IconResource.IDENTIFICATION).image.value)
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
}

object ProgressBarPopup {

  def apply(
    wTitle: String,
    wText: String,
    wParent: Option[Stage],
    isResizable: Boolean = false) = {
    new ProgressBarPopup(wTitle, wText, wParent, isResizable)

  }
}