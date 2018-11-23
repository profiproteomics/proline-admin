package fr.proline.admin.gui.wizard.component.panel.bottom

import com.typesafe.scalalogging.LazyLogging
import scalafx.Includes._
import scalafx.scene.layout.VBox
import fr.proline.admin.gui.util._
import fr.profi.util.scala.ScalaUtils._
import fr.profi.util.scalafx.ScalaFxUtils.TextStyle._
import fr.proline.admin.gui.Monitor
import fr.proline.admin.gui.wizard.util.ExitPopup
import fr.proline.admin.gui.process.config.AdminConfig
import java.io.File
import fr.proline.admin.gui.monitor.view.HomePanel


/**
 * builds the bottom home panel: cancel and go buttons
 * @author aromdhani
 *
 */
object MonitorBottomsPanel extends VBox with IButtons with LazyLogging {

  /** show the main panel  (the list of TableView) */
  def go() {
    Monitor.mainPanel.getChildren.clear()
    //Monitor.mainPanel.getChildren.addAll(MainView)
    goButton.visible = false
  }

  /** exit and close Proline-admin GUI monitor*/
  def exit() {
    ExitPopup("Exit Setup", "Are you sure that you want to exit Proline-Admin Monitor ?", Some(Monitor.stage), false)
  }

  children = component
}