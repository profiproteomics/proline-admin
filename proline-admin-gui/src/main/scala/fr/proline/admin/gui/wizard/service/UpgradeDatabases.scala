package fr.proline.admin.gui.wizard.service

import com.typesafe.scalalogging.LazyLogging
import scalafx.stage.Stage
import scalafx.scene.Scene
import scalafx.scene.Cursor
import scalafx.scene.control.Label
import scalafx.scene.control.TextArea
import javafx.{ concurrent => jfxc }
import scalafx.concurrent.Service
import scala.util.{ Try, Success, Failure }
import fr.proline.admin.gui.wizard.util.ShowDialog
import fr.proline.admin.service.db.migration.UpgradeAllDatabases
import fr.proline.admin.service.db.SetupProline
import fr.proline.admin.gui.process._
import fr.proline.admin.gui.{ Wizard, Monitor }
import fr.proline.admin.gui.wizard.component.panel.bottom.MonitorBottomsPanel
import fr.proline.admin.gui.wizard.monitor.component.MainPanel

/**
 * Upgrade all Proline databases .
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
            val failedDbSet = upgradeProlineDbs.failedDbSet
            if (failedDbSet.isEmpty) { TaskState(true, "All Proline databases have been upgraded successfully!") }
            else {
              TaskState(true, s"Warning:\nProline databases upgrade has finished, but some databases has failed to migrate!\n${failedDbSet.mkString("\n")}")
            }
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
          case true => s"${this.get.message}"
          case _ => s"See proline_admin_gui_log for more details."
        }
        val infoTextArea = new TextArea {
          text = message
          prefHeight = 80
        }
        ShowDialog(
          infoTextArea,
          "Proline databases upgrade",
          Some(Monitor.stage),
          false)
      }
      override def failed(): Unit = {
        MainPanel.disable = false
        MonitorBottomsPanel.exitButton.disable = false
        MonitorBottomsPanel.progressBarPanel.visible = false
        Monitor.stage.getScene().setCursor(Cursor.DEFAULT)
        val message = this.get.isSucceeded match {
          case true => s"${this.get.message}"
          case _ => s"Error: see proline_admin_gui_log for more details."
        }
        val errorTextArea = new TextArea {
          text = message
          prefHeight = 80
        }
        ShowDialog(
          errorTextArea,
          "Proline databases upgrade",
          Some(Monitor.stage),
          false)

      }
    }
  })
}