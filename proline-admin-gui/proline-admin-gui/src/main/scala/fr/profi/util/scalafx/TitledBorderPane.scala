package fr.profi.util.scalafx

import scalafx.Includes._
import scalafx.geometry.Insets
import scalafx.geometry.Pos
import scalafx.scene.Node
import scalafx.scene.control.Label
import scalafx.scene.layout.StackPane

/**
 * ******************************************************************** *
 * TitledBorderPane:                                                    *
 * StackPane surrounded with titled border (emulate Swing TitledBorder) *
 * ******************************************************************** *
 */
class TitledBorderPane(
  titleString: String,
  contentNode: Node,
  colorStr: String = "slategrey"
) extends StackPane {

  val title = new Label(" " + titleString + " ") {
    alignmentInParent = Pos.TopLeft
    style() += s"""
        -fx-font-size: 14;
        -fx-font-weight: bold;
        -fx-font-style: italic;
        -fx-text-fill: ${colorStr};
        -fx-translate-y: -20;
        -fx-translate-x: 15;
      """
  }

  val contentPane = new StackPane() {
    padding = Insets(10)
    content = contentNode
  }

  style() += s"""
    -fx-content-display: center;
    -fx-border-color: ${colorStr};
    -fx-border-width: 1;
    -fx-border-radius: 2;
    """
  content = Seq(contentPane, title)

  def setContentNode(newContentNode: Node) {
    contentPane.content = newContentNode
    content = Seq(contentPane, title)
  }
}