package fr.profi.util.scalafx

import scalafx.geometry.Insets
import scalafx.geometry.Pos
import scalafx.scene.Node
import scalafx.scene.control.ScrollPane
import scalafx.scene.control.ScrollPane.ScrollBarPolicy
import scalafx.scene.layout.VBox
import scalafx.stage.Stage

class CustomScrollPane(
    contentNode: Node = null,
    hBarPolicy: ScrollBarPolicy = ScrollBarPolicy.Never) extends ScrollPane {

  hbarPolicy = hBarPolicy
  vbarPolicy = ScrollBarPolicy.AsNeeded
  val SCROLL_BAR_PUTATIVE_WIDTH = 40

  val contentPane = new VBox {
    alignment = Pos.TopCenter
    padding = Insets(5)
    content = contentNode
  }

  content = contentPane

  def setContentNode(newContentNode: Node) {
    contentPane.children = newContentNode
    this.content = contentPane
  }

  def bindSizeToParent(parentStage: Stage) {
    contentPane.prefWidth <== parentStage.width - SCROLL_BAR_PUTATIVE_WIDTH
    contentPane.minHeight <== parentStage.height - SCROLL_BAR_PUTATIVE_WIDTH
  }
}