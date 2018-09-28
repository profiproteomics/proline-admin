package fr.proline.admin.gui.wizard.component.panel.bottom

import scalafx.Includes._
import scalafx.geometry.Pos
import scalafx.geometry.Insets
import scalafx.scene.layout.{ HBox, Region, VBox }
import scalafx.scene.layout.Priority
import scalafx.scene.control.Button
import scalafx.scene.control.Label
import scalafx.scene.control.ProgressIndicator
import fr.profi.util.scalafx.ScalaFxUtils
import fr.profi.util.scala.ScalaUtils._
import fr.proline.admin.gui.IconResource
import fr.proline.admin.gui.util._

/**
 * Home bottom panel exit and go buttons.
 * @author aromdhani
 *
 */
trait IButtons {
  //Buttons 
  val exitButton = new Button("Exit") {
    graphic = FxUtils.newImageView(IconResource.CANCEL)
    onAction = handle {
      exit()
    }
  }
  val goButton = new Button(" Go ") {
    graphic = FxUtils.newImageView(IconResource.EXECUTE)
    onAction = handle {
      go()
    }
  }

  lazy val progressBar = new ProgressIndicator() {
    prefHeight = 25
    prefWidth = 25
  }
  lazy val progressBarPanel = new VBox {
    spacing = 10
    visible = false
    children = Seq(progressBar,
      new Label {
        text = "Please wait ..."
        alignment = Pos.BASELINE_LEFT
      })
  }

  val buttonsPanel = new HBox {

    padding = Insets(10)
    spacing = 10
    children = Seq(
      goButton,
      exitButton)
  }
  val space = new Region {
    prefWidth = 200
    hgrow = Priority.ALWAYS
  }
  // components
  val componentsPanel = new HBox {
    children = Seq(progressBarPanel,
      space,
      buttonsPanel)
  }

  //layout
  Seq(
    exitButton,
    goButton).foreach { b =>
      b.prefHeight = 20
      b.prefWidth = 120
      b.styleClass += ("mainButtons")
    }
  val component = List(ScalaFxUtils.newVSpacer(20),
    componentsPanel)

  def exit(): Unit
  def go(): Unit
}