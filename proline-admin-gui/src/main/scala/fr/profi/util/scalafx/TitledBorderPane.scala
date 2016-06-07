package fr.profi.util.scalafx

import scalafx.Includes._
import scalafx.geometry.Insets
import scalafx.geometry.Pos
import scalafx.scene.Node
import scalafx.scene.control.Label
import scalafx.scene.layout.StackPane
import scalafx.scene.layout.VBox
import scalafx.scene.layout.Priority

/**
 * ******************************************************************** *
 * TitledBorderPane:                                                    *
 * StackPane surrounded with titled border (emulate Swing TitledBorder) *
 * ******************************************************************** *
 */
/*class TitledBorderPane(
  title: String,
  contentNode: Node,
  titleTooltip: String = "",
  colorStr: String = "slategrey"
) extends VBox {*/

class TitledBorderPane(
  protected val title: String,
  protected val contentNode: Node,
  override val titleTooltip: String = "",
  override val colorStr: String = "slategrey"
) extends ITitledBorderPane {
  this.initContent()
}

trait IContentAutoInit {
  protected def initContent()
  initContent()
}
  
trait ITitledBorderPane extends VBox {
  
  protected def title: String
  protected def contentNode: Node
  //protected def paddingValue: Any
  
  protected val contentPadding: Insets = Insets(10)
  protected val titleTooltip: String = ""
  protected val colorStr: String = "slategrey"
  
  vgrow = Priority.Always
  
  /* Border style */
  style() += s"""
    -fx-content-display: center;
    -fx-border-color: ${colorStr};
    -fx-border-width: 1;
    -fx-border-radius: 2;
    """

  /* Title text and style */
  private lazy val titleLabel = new Label(" " + title + " ") {
    if (titleTooltip != "") tooltip = titleTooltip

    alignmentInParent = Pos.TopLeft
    style() += s"""
        -fx-font-size: 14;
        -fx-font-weight: bold;
        -fx-font-style: italic;
        -fx-text-fill: ${colorStr};
        -fx-translate-y: -20;
        -fx-translate-x: 15;
      """
     //translate-y: -20: title will appear above the border
  }
  
 /* Wrap content to force padding */
  private lazy val paddedContent = new VBox {
    padding = contentPadding
    content = contentNode
  }
  
  //content = Seq(titleLabel, paddedContent)
  protected def initContent() {
    content = Seq(titleLabel, paddedContent)
  }

  /** Update titled bordered panel content **/
  def setContentNode(newContentNode: Node, contentPadding: Insets = Insets(10)) {
    paddedContent.content = newContentNode
    paddedContent.padding = contentPadding
    content = Seq(titleLabel, paddedContent)
  }
}