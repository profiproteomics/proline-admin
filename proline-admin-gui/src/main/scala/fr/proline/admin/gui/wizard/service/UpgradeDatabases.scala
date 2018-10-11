package fr.proline.admin.gui.wizard.service

import com.typesafe.scalalogging.LazyLogging
import scalafx.stage.Stage
import scalafx.scene.Scene
import scalafx.scene.Cursor
import scalafx.scene.control.Label
import javafx.{ concurrent => jfxc }
import scalafx.concurrent.Service
import scala.util.{ Try, Success, Failure }
import fr.proline.admin.gui.wizard.util.HelpPopup
import fr.proline.admin.service.db.migration.UpgradeAllDatabases
import fr.proline.admin.service.db.SetupProline
import fr.proline.admin.gui.process._
import fr.proline.admin.gui.{ Wizard, Monitor }
import fr.proline.admin.gui.wizard.component.panel.bottom.MonitorBottomsPanel
import fr.proline.admin.gui.wizard.monitor.component.MainPanel

/**
 * Upgrade all Proline databases to the last version.
 * @author aromdhani
 *
 */

object UpgradeDatabases extends LazyLogging {
  object Worker extends Service(new jfxc.Service[Boolean]() {
    var isCompleted = false
    protected def createTask(): jfxc.Task[Boolean] = new jfxc.Task[Boolean] {
      protected def call(): Boolean =
        {
          val dsConnectorFactory = UdsRepository.getDataStoreConnFactory()
          /* Logback */
          try {
            new UpgradeAllDatabases(dsConnectorFactory).doWork()
            logger.info("All Databases successfully upgraded !")
            isCompleted = true
          } catch {
            case e: Exception =>
              {
                logger.error("All Databases upgrade failed", e)
              }
              isCompleted = false
          }
          isCompleted
        }
      override def running(): Unit = {
        MainPanel.disable = true
        MonitorBottomsPanel.exitButton.disable = true
        MonitorBottomsPanel.progressBarPanel.visible = true
        Monitor.stage.getScene().setCursor(Cursor.WAIT)
      }
      override def succeeded(): Unit = {
        MainPanel.disable = false
        MonitorBottomsPanel.exitButton.disable = false
        MonitorBottomsPanel.progressBarPanel.visible = false
        Monitor.stage.getScene().setCursor(Cursor.DEFAULT)
        if (isCompleted) {
          HelpPopup("Upgrade databases", s"All databases have been upgraded successfully.\n" +
            "See proline_admin_gui_log for more details.", Some(Monitor.stage), false)
        } else {
          HelpPopup("Upgrade databases", s"Error while trying to upgrade all databases.\n" +
            "See proline_admin_gui_log for more details.", Some(Monitor.stage), false)
        }
      }
    }
  })
}