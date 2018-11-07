package fr.proline.admin.gui.wizard.component.panel.bottom

import com.typesafe.scalalogging.LazyLogging
import scalafx.Includes._
import scalafx.geometry.{ Pos, Insets }
import scalafx.scene.control.Button
import scalafx.scene.layout.HBox

import fr.proline.admin.gui.Monitor
import fr.proline.admin.gui.Wizard
import fr.proline.admin.gui.wizard.component.items.SummaryConfig
import fr.proline.admin.gui.wizard.component.panel.main.Summary
import fr.proline.admin.gui.wizard.util.ExitPopup

/**
 * Panel contains navigation buttons : previous next and cancel
 *
 */

object MonitorNavButtons extends LazyLogging with INavButtons {

  var summaryPanel = new SummaryConfig(4)

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

  /** exit proline Admin */
  def cancel() {
    ExitPopup("Exit Setup", "Are you sure that you want to exit Proline Admin Monitor? ", Some(Monitor.stage), false)
  }

  /** get next item */
  def next() {
    InstallNavButtons.prevButton.visible = true
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
          case (order, itemOpt) => Summary.getItem(itemOpt.get)
        }
        Wizard.currentNode = summaryPanel
        Wizard.configItemsPanel.getChildren().clear()
        Wizard.configItemsPanel.getChildren().add(Wizard.currentNode)
        /* set button Validate to save modifications */
        InstallNavButtons.nextBox.getChildren().clear()
        InstallNavButtons.nextBox.getChildren().add(validateButton)
      }
    }
  }

  /** get previous item */
  def previous() {
    if (Wizard.nodeIndex > 0) {
      InstallNavButtons.nextBox.getChildren().clear()
      InstallNavButtons.nextBox.getChildren().add(nextButton)
      Wizard.nodeIndex = Wizard.nodeIndex - 1
      Wizard.currentNode = Wizard.items.toSeq.apply(Wizard.nodeIndex)._2.get
      Wizard.configItemsPanel.getChildren().clear()
      Wizard.configItemsPanel.getChildren().add(Wizard.currentNode)
      if (Wizard.items.head._2.get.equals(Wizard.currentNode)) {
        InstallNavButtons.prevButton.visible = false
      }
    }
  }

  /** save all parameters */
  def validate() {
    Wizard.items.toSeq.foreach {
      case (_1, _2) => Summary.saveItem(_2.get)
    }
  }
}