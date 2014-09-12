package fr.proline.admin.gui

import scalafx.beans.binding.NumberBinding.sfxNumberBinding2jfx
import scalafx.geometry.Insets
import scalafx.scene.Scene
import scalafx.scene.control.TextArea
import scalafx.scene.layout.GridPane
import scalafx.scene.layout.Priority
import scalafx.scene.layout.Region
import scalafx.scene.layout.StackPane
import scalafx.stage.Modality
import scalafx.stage.Stage
import scalafx.stage.StageStyle

/**
 * GRAPHICAL UTILITIES
 */
object Util {

  /** Emulate command line */
  def mkCmd(cmd: String) = "<b>> run_cmd " + cmd + "</b>"

  /** Vertical & horizontal spacers */
  def newVSpacer = new Region {
    vgrow = Priority.ALWAYS
  }

  def newHSpacer = new Region {
    hgrow = Priority.ALWAYS
  }

  /** Set elements position in grid pane from a Seq[Tuple5] */
  def setGridContent5(
    seq: Seq[Tuple5[scalafx.scene.Node, Int, Int, Int, Int]],
    grid: GridPane) = {
    seq.foreach { constraints =>
      val (child, col, row, colSpan, rowSpan) = constraints
      grid.add(child, col, row, colSpan, rowSpan)
    }
  }

  /** Modal windows location, relative to main window */
  def getStartX(mainStage: Stage = Main.stage, div: Int = 2): Double = {
    val stageX = mainStage.x
    val stageWidth = mainStage.width
    (stageX + (stageWidth / div)).toDouble
  }

  def getStartY(mainStage: Stage = Main.stage, div: Int = 4): Double = {
    val stageY = mainStage.y
    val stageHeight = mainStage.height
    (stageY + (stageHeight / div)).toDouble
  }

  //TODO:  get from fr.profi.util.scalafx  || move to component.modal
  def showPopup(

    wTitle: String,
    wText: String,
    wParent: Option[Stage] = Some(Main.stage)//,
//    X: Option[Double] = Some(Main.stage.width() / 2),
//    Y: Option[Double] = Some(Main.stage.width() / 2)
    
  ) = {
    new Stage {

      popup =>
      title = wTitle
      initStyle(StageStyle.UTILITY)
      initModality(Modality.WINDOW_MODAL)
      resizable = false
      if (wParent.isDefined) initOwner(wParent.get)
//      if (X.isDefined) this.x = X.get
//      if (Y.isDefined) this.y = Y.get

      scene = new Scene {
        root = new StackPane {
          padding = Insets(5)
          content = new TextArea {
            text = wText
            wrapText = true
            editable = false
            style = "-fx-border-style: none;-fx-focus-color: transparent;"
          }
        }
      }
    }.showAndWait()
  }
}