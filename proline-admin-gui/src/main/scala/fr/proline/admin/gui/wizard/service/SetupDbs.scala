package fr.proline.admin.gui.wizard.service

import scalafx.stage.Stage
import scalafx.scene.Scene
import scalafx.scene.control.Label
import javafx.{ concurrent => jfxc }
import scalafx.concurrent.Service
import scalafx.scene.Cursor
import fr.proline.admin.gui.wizard.util.HelpPopup
import fr.proline.admin.service.db.SetupProline
import fr.proline.admin.gui.process._
import fr.proline.admin.gui.{ Wizard, Monitor }
import fr.proline.admin.gui.wizard.component.panel.bottom.InstallNavButtons
import fr.proline.admin.service.db.migration.UpgradeAllDatabases
import com.typesafe.scalalogging.LazyLogging
/**
 * Service to setup Proline databases. It used to install the required Proline  databases.
 * @author aromdhani
 *
 */

class SetupDbs(stage: Stage) extends Service(new jfxc.Service[Boolean]() {
  var isCompleted = false
  protected def createTask(): jfxc.Task[Boolean] = new jfxc.Task[Boolean] with LazyLogging {
    protected def call(): Boolean =
      {
        if (ProlineAdminConnection.loadProlineInstallConfig(Wizard.adminConfPath, verbose = false)) {
          var isUpToDate = false
          var isSetup = UdsRepository.isUdsDbReachable(false)
          if (!isSetup) {
            //setup
            isSetup = try {
              logger.info("INFO - Start to set up proline Databases...")
              synchronized {
                new SetupProline(SetupProline.getUpdatedConfig()).run()
                true
              }
            } catch {
              case t: Throwable =>
                logger.error("Error while trying to setup Proline databases", t.getMessage)
                false
            }
          }
          //upgrade 
          if (isSetup) {
            isUpToDate = try {
              logger.info("INFO - Start to upgrade proline Databases...")
              synchronized {
                new UpgradeAllDatabases(UdsRepository.getDataStoreConnFactory()).doWork()
              }
              true
            } catch {
              case t: Throwable =>
                logger.error("Error while trying to setup Proline databases", t.getMessage)
                false
            }
          }
          isCompleted = isSetup && isUpToDate
        }
        isCompleted
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
      if (isCompleted) {
        HelpPopup("Setup Proline", s"Proline has been setup successfully.\n" +
          "See proline_admin_gui_log for more details.", Some(stage), false)
      } else {
        HelpPopup("Setup Proline", s"Error while trying to setup Proline.\n" +
          "See proline_admin_gui_log for more details.", Some(stage), false)
      }
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
