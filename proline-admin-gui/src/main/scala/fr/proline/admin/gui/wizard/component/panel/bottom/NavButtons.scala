package fr.proline.admin.gui.wizard.component.panel.bottom

import com.typesafe.scalalogging.LazyLogging
import scalafx.Includes._
import scalafx.geometry.Pos
import scalafx.geometry.Insets
import scalafx.scene.control.Button
import scalafx.scene.layout.HBox
import fr.proline.admin.gui.wizard.util.GetConfirmation
import fr.profi.util.scala.ScalaUtils._
import fr.profi.util.scalafx.ScalaFxUtils.TextStyle._
import javafx.scene.control.ContentDisplay
import fr.proline.admin.gui.util.FxUtils
import fr.proline.admin.gui.Wizard
import fr.proline.admin.gui.IconResource
import fr.proline.admin.gui.wizard.component.items._
import fr.proline.admin.gui.wizard.util.ExitPopup
import fr.proline.admin.gui.wizard.component.panel.main.Summary
import scalafx.scene.control.Button.sfxButton2jfx

/**
 * Panel contains navigation buttons : previous next and cancel
 *
 */

object NavButtons extends LazyLogging {

  /**
   * ******* *
   * BUTTONS *
   * ******* *
   */

  val cancelButton = new Button("Cancel") {
    graphic = FxUtils.newImageView(IconResource.CANCEL)
    onAction = handle {
      ExitPopup("Exit Setup", "Are you sure you want to exit Proline Install? ", Some(Wizard.stage), false)
    }
  }
  val nextButton = new Button("Next") {
    graphic = FxUtils.newImageView(IconResource.ARROWRIGHT)
    contentDisplay = ContentDisplay.RIGHT
    onAction = handle {
      getNextItem()
    }
  }
  val prevButton = new Button("Previous") {
    graphic = FxUtils.newImageView(IconResource.ARROWLEFT)
    onAction = handle {
      getPrevItem()
    }
  }
  val validateButton = new Button("Validate") {
    graphic = FxUtils.newImageView(IconResource.SAVE)
    onAction = handle {
      // validate modifications 
      val confirmed = GetConfirmation("Are you sure you want to save the new Proline configurations? ")
      if (confirmed) {
        Wizard.items.toSeq.foreach {
          case (_1, _2) => Summary.saveItem(_2.get)
        }
      }
    }
  }
  var summaryPanel = new SummaryConfig("summary")

  /**
   * ***** *
   * LAYOUT *
   * **** *
   */

  val prevBox = new HBox {
    children = Seq(prevButton)
  }
  val nextBox = new HBox {
    children = Seq(nextButton)
  }
  val cancelBox = new HBox {
    children = Seq(cancelButton)
  }
  Seq(
    cancelButton, nextButton, prevButton, validateButton).foreach { b =>
      b.prefHeight = 20
      b.prefWidth = 120
      b.styleClass += ("mainButtons")
    }

  /**
   * ****** *
   * APPLY() *
   * ***** *
   */
  /** Display buttons in a HBox **/

  def apply(): HBox = {
    new HBox {
      alignment = Pos.BaselineRight
      padding = Insets(10)
      spacing = 10
      children = Seq(
        prevBox, nextBox, cancelBox)
    }
  }

  /* get next item  */
  def getNextItem() {
    NavButtons.prevButton.visible = true
    /* change items */
    if (Wizard.nodeIndex < Wizard.items.toSeq.size - 1) {
      Wizard.nodeIndex = Wizard.nodeIndex + 1
      Wizard.currentNode = Wizard.items.toSeq.apply(Wizard.nodeIndex)._2.get
      Wizard.configItemsPanel.getChildren().clear()
      Wizard.configItemsPanel.getChildren().add(Wizard.currentNode)
    } else {
      if (Wizard.nodeIndex == Wizard.items.toSeq.size - 1) {
        /* Summary panel */
        Wizard.nodeIndex = Wizard.nodeIndex + 1
        Wizard.items.toSeq.foreach {
          case (_1, _2) => Summary.getItem(_2.get)
        }
        Wizard.currentNode = summaryPanel
        Wizard.configItemsPanel.getChildren().clear()
        Wizard.configItemsPanel.getChildren().add(Wizard.currentNode)
        /* set button Validate to save modifications */
        NavButtons.nextBox.getChildren().clear()
        NavButtons.nextBox.getChildren().add(validateButton)
      }
    }
  }
  /* get previous item  */
  def getPrevItem() {
    if (Wizard.nodeIndex > 0) {
      NavButtons.nextBox.getChildren().clear()
      NavButtons.nextBox.getChildren().add(nextButton)
      Wizard.nodeIndex = Wizard.nodeIndex - 1
      Wizard.currentNode = Wizard.items.toSeq.apply(Wizard.nodeIndex)._2.get
      Wizard.configItemsPanel.getChildren().clear()
      Wizard.configItemsPanel.getChildren().add(Wizard.currentNode)
      if (Wizard.items.head._2.get.equals(Wizard.currentNode)) {
        NavButtons.prevButton.visible = false
      }
    }
  }
}