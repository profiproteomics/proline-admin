package fr.proline.admin.gui.wizard.util

import scalafx.Includes._
import scalafx.geometry.Insets
import scalafx.geometry.Pos
import scalafx.scene.Scene
import scalafx.scene.Node
import scalafx.scene.control.Button
import scalafx.scene.input.KeyEvent
import scalafx.scene.layout.StackPane
import scalafx.stage.Modality
import scalafx.stage.Stage
import fr.profi.util.scalafx.ScalaFxUtils
import fr.proline.admin.gui.util.FxUtils
import fr.proline.admin.gui.Wizard
import scalafx.scene.layout.VBox
import fr.proline.admin.gui.IconResource

/**
 * **************************************** *
 * Show a PopupWindow with given properties *
 * **************************************** *
 */
class ShowDialog(
    wTitle: String,
    node: Node,
    wParent: Option[Stage] = Option(Wizard.stage),
    isResizable: Boolean = false) extends Stage {

  val popup = this

  title = wTitle
  initModality(Modality.WINDOW_MODAL)
  resizable = isResizable
  if (wParent.isDefined) initOwner(wParent.get)
  popup.getIcons.add(FxUtils.newImageView(IconResource.IDENTIFICATION).image.value)
  scene = new Scene {

    onKeyPressed = (ke: KeyEvent) => { ScalaFxUtils.closeIfEscapePressed(popup, ke) }

    root = new VBox {
      alignment = Pos.Center
      spacing = 15
      padding = Insets(10)

      children = List(
        node,
        new Button("OK") {
          graphic = FxUtils.newImageView(IconResource.TICK)
          onAction = handle { popup.close() }
        })
    }
  }
}

/**
 * ********************************************************************** *
 * Companion object to PopupWindow, show it with more understandable name *
 * ********************************************************************** *
 */
object ShowDialog {

  def apply(
    node: Node,
    wTitle: String,
    wParent: Option[Stage] = Option(Wizard.stage),
    isResizable: Boolean = false) {
    new ShowDialog(wTitle, node, wParent, isResizable).showAndWait()
  }
}
