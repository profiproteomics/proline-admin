package fr.proline.admin.gui.wizard.service

import scalafx.stage.Stage
import scalafx.scene.Scene
import scalafx.scene.control.Label
import scalafx.scene.control.TextArea
import javafx.{ concurrent => jfxc }
import scalafx.concurrent.Service
import scalafx.scene.Cursor
import fr.proline.admin.service.db.SetupProline
import fr.proline.admin.gui.process._
import fr.proline.admin.gui.{ Wizard, Monitor }
import fr.proline.admin.gui.wizard.component.panel.bottom.InstallNavButtons
import fr.proline.admin.service.db.migration.UpgradeAllDatabases
import fr.proline.admin.gui.wizard.util.ShowDialog
import com.typesafe.scalalogging.LazyLogging
/**
 * Service to setup Proline databases. It used to install the required Proline  databases.
 * @author aromdhani
 *
 */

class SetupDbs(stage: Stage) extends Service(new jfxc.Service[TaskState]() {
  protected def createTask(): jfxc.Task[TaskState] = new jfxc.Task[TaskState] with LazyLogging {
    protected def call(): TaskState =
      {
        ProlineAdminConnection.loadProlineInstallConfig(Wizard.adminConfPath, verbose = false)
        val isUdsReachable = UdsRepository.isUdsDbReachable(false)
        val setupUpgradeDbsTaskState = if (!isUdsReachable) {
          // Setup
          val setupTaskState = try {
            logger.info("Start to set up Proline databases. Please wait...")
            synchronized {
              new SetupProline(SetupProline.getUpdatedConfig(), UdsRepository.getUdsDbConnector()).run()
              TaskState(true, "Proline has been successfully setup!")
            }
          } catch {
            case e: Exception =>
              logger.error("Error while trying to set up Proline!", e.getMessage)
              TaskState(false, s"Error while trying to set up Proline:\n ${e.getMessage()}")
          }
          setupTaskState
        } else {
          val upgradeDbsTaskState = try {
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
          upgradeDbsTaskState
        }
        setupUpgradeDbsTaskState
      }
    override def running(): Unit = {
      InstallNavButtons.progressPanel.visible_=(true)
      Wizard.configItemsPanel.disable = true
      InstallNavButtons.cancelButton.disable = true
      InstallNavButtons.cancelButton.disable = true
      InstallNavButtons.prevButton.disable = true
      InstallNavButtons.validateButton.disable = true
      Wizard.stage.getScene().setCursor(Cursor.WAIT)
    }
    override def succeeded(): Unit = {
      InstallNavButtons.progressPanel.visible_=(false)
      InstallNavButtons.cancelButton.disable = false
      InstallNavButtons.prevButton.disable = false
      InstallNavButtons.validateButton.disable = false

      Wizard.configItemsPanel.disable = false
      Wizard.stage.getScene().setCursor(Cursor.DEFAULT)
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
        "Proline databases setup and upgrade",
        Some(Wizard.stage),
        false)
    }
    override def failed: Unit = {
      InstallNavButtons.progressPanel.visible_=(false)
      InstallNavButtons.cancelButton.disable = false
      InstallNavButtons.prevButton.disable = false
      InstallNavButtons.validateButton.disable = false

      Wizard.configItemsPanel.disable = false
      Wizard.stage.getScene().setCursor(Cursor.DEFAULT)
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
        "Proline databases setup and upgrade",
        Some(Wizard.stage),
        false)
    }
  }
})

object SetupDbs {
  /**
   * Setup Proline .It install the required databases.
   * @param stage The stage parent of progressIndicator
   */
  def apply(stage: Stage): SetupDbs = {
    new SetupDbs(stage)
  }
}
