package fr.proline.admin.gui.wizard.component.panel.bottom

import scalafx.Includes._
import scalafx.geometry.Pos
import scalafx.geometry.Insets
import scalafx.scene.control.Button
import scalafx.scene.layout.HBox
import scalafx.scene.layout.VBox

import fr.proline.admin.gui.util._
import fr.proline.admin.gui.Wizard
import fr.proline.admin.gui.IconResource
import fr.proline.admin.gui.wizard.component.items._
import fr.proline.admin.gui.wizard.util.ExitPopup
import fr.proline.admin.gui.wizard.component.panel.main.InstallPane

import fr.profi.util.scalafx.ScalaFxUtils
import fr.profi.util.scala.ScalaUtils._
import fr.profi.util.scalafx.ScalaFxUtils
import fr.profi.util.scalafx.ScalaFxUtils.TextStyle._
/**
 * builds bottom home panel: cancel and go buttons
 *
 */
object InstallButtons extends VBox with IButtons {

  def go() {
    InstallPane.getSelectedItems
     Wizard.items = Wizard.items.toSeq.sortBy(_._1).toMap
    if (Wizard.items.isEmpty || !InstallPane.setStyleSelectedItems) {
      // ItemsPanel.warningNotValidServerFile.visible = true
    } else {
      InstallPane.warningCorruptedFile.visible = false
      Wizard.currentNode = Wizard.items.head._2.get

      //hide the  previous button for the first panel 
      InstallNavButtons.prevButton.visible = false
      //set the first panel
      Wizard.configItemsPanel.getChildren().clear()
      Wizard.configItemsPanel.getChildren().add(Wizard.currentNode)
      //set buttons panel
      Wizard.buttonsPanel.getChildren().clear()
      Wizard.buttonsPanel.getChildren().add(InstallNavButtons())
    }
  }
  def exit() {
    ExitPopup("Exit Install", "Are you sure you want to exit Proline Install ?", Some(Wizard.stage), false)
  }
  children = component
}