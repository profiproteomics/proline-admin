package fr.proline.admin.gui.wizard.component

import com.typesafe.scalalogging.LazyLogging
import scalafx.Includes._
import scalafx.geometry.Pos
import scalafx.application.Platform
import scalafx.geometry.Insets
import scalafx.scene.control.Button
import scalafx.scene.layout.HBox
import scalafx.scene.layout.Priority
import scalafx.scene.Scene
import fr.proline.admin.gui.wizard.util.GetConfirmation
import fr.profi.util.scalafx.ScalaFxUtils
import fr.profi.util.scala.ScalaUtils._
import fr.profi.util.scalafx.ScalaFxUtils
import fr.profi.util.scalafx.ScalaFxUtils.TextStyle._
import scalafx.scene.control.Label
import scala.collection.Iterator
import javafx.scene.control.ContentDisplay
import fr.proline.admin.gui.util.FxUtils
import fr.proline.admin.gui.Wizard
import fr.proline.admin.gui.IconResource
import fr.proline.admin.gui.wizard.component.items._
import fr.proline.admin.gui.wizard.util.ShowPopupWindow
import fr.proline.admin.gui.wizard.util.FieldProperties

/**
 * Create panel contains navigation buttons : previous next and cancel .
 */

object NavigationButtonsPanel extends LazyLogging {

  /**
   * ******* *
   * BUTTONS *
   * ******* *
   */
  //var postgresConfig: PostgresConfig = _
  val cancelButton = new Button("Cancel") {
    graphic = FxUtils.newImageView(IconResource.CANCEL)
    onAction = handle {
      ShowPopupWindow("Exit Setup", "Are you sure you want to exit Proline Setup ?", Option(Wizard.stage), false)
    }
  }

  val nextButton = new Button("Next") {
    graphic = FxUtils.newImageView(IconResource.ARRAOWLEFT)
    contentDisplay = ContentDisplay.RIGHT
    onAction = handle {
      onNext()
    }
  }

  val prevButton = new Button("Previous") {
    graphic = FxUtils.newImageView(IconResource.ARROWRIGHT)
    onAction = handle {
      onPrev()
    }
  }
  val validateButton = new Button("Validate") {
    graphic = FxUtils.newImageView(IconResource.SAVE)
    onAction = handle {
      // validate modifications 

      val confirmed = GetConfirmation("Are you sure you want to save the new Proline configurations ?")
      if (confirmed) {
        Wizard.items.toSeq.foreach {
          case (_1, _2) => SelectedItem.saveForms(_2)
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
    content = Seq(prevButton)
  }
  val nextBox = new HBox {
    content = Seq(nextButton)
  }
  val cancelBox = new HBox {
    content = Seq(cancelButton)
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
      content = Seq(
        prevBox, nextBox, cancelBox)
    }
  }

  /* next and previous functions  */

  def onNext() {

    NavigationButtonsPanel.prevButton.visible = true
    /* change items */
    if (Wizard.nodeIndex < Wizard.items.toSeq.size - 1) {
      Wizard.nodeIndex = Wizard.nodeIndex + 1
      Wizard.currentNode = Wizard.items.toSeq.apply(Wizard.nodeIndex)._2
      Wizard.configItemsPanel.getChildren().clear()
      Wizard.configItemsPanel.getChildren().add(Wizard.currentNode)
    } else {
      if (Wizard.nodeIndex == Wizard.items.toSeq.size - 1) {
        /* Summary panel */
        Wizard.nodeIndex = Wizard.nodeIndex + 1
        Wizard.items.toSeq.foreach {
          case (_1, _2) => SelectedItem.getItem(_2)
        }
        Wizard.currentNode = summaryPanel
        Wizard.configItemsPanel.getChildren().clear()
        Wizard.configItemsPanel.getChildren().add(Wizard.currentNode)
        /* set button Validate to save modifications */
        NavigationButtonsPanel.nextBox.getChildren().clear()
        NavigationButtonsPanel.nextBox.getChildren().add(validateButton)
      }
    }
  }

  def onPrev() {
    if (Wizard.nodeIndex > 0) {
      NavigationButtonsPanel.nextBox.getChildren().clear()
      NavigationButtonsPanel.nextBox.getChildren().add(nextButton)
      Wizard.nodeIndex = Wizard.nodeIndex - 1
      Wizard.currentNode = Wizard.items.toSeq.apply(Wizard.nodeIndex)._2
      Wizard.configItemsPanel.getChildren().clear()
      Wizard.configItemsPanel.getChildren().add(Wizard.currentNode)
      if (Wizard.items.head._2.equals(Wizard.currentNode)) {
        NavigationButtonsPanel.prevButton.visible = false
      }
    }
  }
}