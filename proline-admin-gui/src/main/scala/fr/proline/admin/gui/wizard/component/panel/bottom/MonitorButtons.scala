package fr.proline.admin.gui.wizard.component.panel.bottom

import com.typesafe.scalalogging.LazyLogging
import scalafx.Includes._
import scalafx.scene.layout.VBox
import fr.proline.admin.gui.util._
import fr.profi.util.scala.ScalaUtils._
import fr.profi.util.scalafx.ScalaFxUtils.TextStyle._
import fr.proline.admin.gui.Monitor
import fr.proline.admin.gui.wizard.util.ExitPopup
import fr.proline.admin.gui.wizard.monitor.component._
import fr.proline.admin.gui.wizard.component.panel.main.MonitorPane
import fr.proline.admin.gui.process.config.AdminConfig
import java.io.File

/**
 * builds the bottom home panel: cancel and go buttons
 * @author aromdhani
 *
 */
object MonitorBottomsPanel extends VBox with IButtons with LazyLogging {

  /** show the main panel  (the list of TableView) */
  def go() {
    Monitor.itemsPanel.getChildren.clear()
    Monitor.itemsPanel.getChildren.addAll(MainPanel)
    goButton.visible = false
  }

  /** Exit and close Proline-admin GUI monitor*/
  def exit() {
    ExitPopup("Exit Setup", "Are you sure that you want to exit Proline-Admin Monitor ?", Some(Monitor.stage), false)
  }

  /** check the initial settings of Proline-Admin */
  def isInitialSettingsOk: Boolean = {
    try {
      MonitorPane.getAdminConfigOpt().map(adminConfig => MonitorPane.isAdminConfigsOk(adminConfig).forall { _.==(true) }).getOrElse(false)
    } catch {
      case t: Throwable =>
        logger.error("Error while trying to check initial Proline-Admin settings.", t.getMessage)
        false
    }
  }

  //disable go button when there is a problem of connectivity.
  goButton.disable = !isInitialSettingsOk
  //components
  children = component
}