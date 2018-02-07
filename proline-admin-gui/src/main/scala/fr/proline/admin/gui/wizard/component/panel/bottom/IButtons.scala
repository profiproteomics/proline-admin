package fr.proline.admin.gui.wizard.component.panel.bottom

import scalafx.Includes._
import scalafx.geometry.Pos
import scalafx.geometry.Insets
import scalafx.scene.layout.HBox
import scalafx.scene.control.Button
import fr.profi.util.scalafx.ScalaFxUtils
import fr.profi.util.scala.ScalaUtils._
import fr.proline.admin.gui.IconResource
import fr.proline.admin.gui.util._
/**
 * trait of home bottom panel: cancel and go buttons.
 *
 */
trait IButtons {
  //Buttons 
  val cancelButt = new Button("Exit") {
    graphic = FxUtils.newImageView(IconResource.CANCEL)
    onAction = handle {
      exit()
    }
  }
  val goButt = new Button(" Go ") {
    graphic = FxUtils.newImageView(IconResource.EXECUTE)
    onAction = handle {
      go()
    }
  }
  //layout
  Seq(
    cancelButt,
    goButt).foreach { b =>
      b.prefHeight = 20
      b.prefWidth = 120
      b.styleClass += ("mainButtons")
    }
  val component = List(ScalaFxUtils.newVSpacer(20), new HBox {
    alignment = Pos.BaselineRight
    padding = Insets(10)
    spacing = 10
    children = Seq(
      goButt,
      cancelButt)
  })

  def exit(): Unit
  def go(): Unit
}