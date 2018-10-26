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
  object Worker extends Service(new jfxc.Service[TaskState]() {
    protected def createTask(): jfxc.Task[TaskState] = new jfxc.Task[TaskState] {
      protected def call(): TaskState =
        {
          val taskState = try {
            logger.info("Start to upgrade all Proline databases. Please wait...")
            val upgradeProlineDbs = new UpgradeAllDatabases(UdsRepository.getDataStoreConnFactory())
            upgradeProlineDbs.doWork()
            logger.info("All Proline databases have been upgraded successfully!")
            TaskState(true, "All Proline databases have been upgraded successfully!")
          } catch {
            case e: Exception =>
              {
                logger.error("Error while trying to upgrade Proline databases!", e.getMessage())
                TaskState(false, s"Error while trying to upgrade Proline databases:\n ${e.getMessage()}")
              }
          }
          taskState
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
        val message = this.get.isSucceeded match {
          case true => s"${this.get.message}\nSee proline_admin_gui_log for more details."
          case _ => s"${this.get.message}\nSee proline_admin_gui_log for more details."
        }
        HelpPopup("Upgrade databases", message, Some(Monitor.stage), false)
      }
    }
  })
}