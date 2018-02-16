package fr.proline.admin.gui.wizard.component.panel.bottom

import scalafx.Includes._
import scalafx.scene.layout.VBox
import fr.proline.admin.gui.util._
import fr.profi.util.scala.ScalaUtils._
import fr.profi.util.scalafx.ScalaFxUtils.TextStyle._
import fr.proline.admin.gui.Monitor
import fr.proline.admin.gui.wizard.util.ExitPopup
import fr.proline.admin.gui.wizard.monitor.component._
import fr.proline.admin.gui.wizard.component.panel.main.MonitorPane

/**
 * builds bottom home panel: cancel and go buttons
 * @author aromdhani
 *
 */
object MonitorBottomsPanel extends VBox with IButtons {

  def go() {
    Monitor.itemsPanel.getChildren.clear()
    Monitor.itemsPanel.getChildren.addAll(MainPane)
    this.goButton.visible_=(false)
  }
  def exit() {
    ExitPopup("Exit Setup", "Are you sure that you want to exit Proline-Admin Monitor ?", Some(Monitor.stage), false)
  }
  children = component
}