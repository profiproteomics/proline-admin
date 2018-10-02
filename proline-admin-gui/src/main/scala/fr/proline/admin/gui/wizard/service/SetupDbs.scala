package fr.proline.admin.gui.wizard.service

import scalafx.stage.Stage
import scalafx.scene.Scene
import scalafx.scene.control.Label
import javafx.{ concurrent => jfxc }
import scalafx.concurrent.Service
import scala.util.{ Try, Success, Failure }
import fr.proline.admin.gui.wizard.util.HelpPopup
import fr.proline.admin.service.db.SetupProline
import fr.proline.admin.gui.process._
import fr.proline.admin.gui.{ Wizard, Monitor }
import fr.proline.admin.gui.wizard.component.panel.bottom.InstallNavButtons
/**
 * Service to setup Proline databases. It used to install the required Proline  databases.
 * @author aromdhani
 *
 */

class SetupDbs(stage: Stage) extends Service(new jfxc.Service[Boolean]() {
  var isCompleted = false
  protected def createTask(): jfxc.Task[Boolean] = new jfxc.Task[Boolean] {
    protected def call(): Boolean =
      {
        val setupDbs = Try {
          val confPath = Wizard.adminConfPath
          ProlineAdminConnection.loadProlineInstallConfig(confPath, verbose = false)
          println("INFO - Proline configuration is valid.")
        } flatMap { udsConfig =>
          Try {
            println("INFO - Start to set up proline Databases.")
            synchronized {
              new SetupProline(SetupProline.getUpdatedConfig()).run()
            }
          }
        }
        setupDbs match {
          case Success(s) => {
            println("Proline has been setup successfully.")
            isCompleted = true
          }
          case Failure(t) => {
            println("Error occured while trying to setup Proline : ", t.printStackTrace())
          }
        }
        isCompleted
      }
    override def running(): Unit = {
      InstallNavButtons.progressPanel.visible_=(true)
    }
    override def succeeded(): Unit = {
      InstallNavButtons.progressPanel.visible_=(false)
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
