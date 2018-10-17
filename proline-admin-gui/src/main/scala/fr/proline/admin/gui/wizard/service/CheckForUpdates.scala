package fr.proline.admin.gui.wizard.service

import com.typesafe.scalalogging.LazyLogging
import scalafx.stage.Stage
import scalafx.scene.{ Scene, Cursor }
import scalafx.scene.control.Label
import javafx.{ concurrent => jfxc }
import scalafx.concurrent.Service
import scala.util.{ Try, Success, Failure }
import fr.proline.admin.gui.wizard.util.HelpPopup
import fr.proline.admin.service.user.{ CheckForUpdates => CheckDbsUpdates }
import fr.proline.admin.service.db.SetupProline
import fr.proline.admin.gui.process._
import fr.proline.admin.gui.Monitor
import fr.proline.admin.gui.wizard.monitor.component.MainPanel
import fr.proline.admin.gui.wizard.component.panel.bottom.MonitorBottomsPanel
import scala.collection.Map
import java.util.concurrent.atomic.AtomicBoolean
/**
 * Service to setup and upgrade Proline databases.
 * @author aromdhani
 *
 */

object CheckForUpdates extends LazyLogging {
  val shouldThrow = new AtomicBoolean(false)
  object Worker extends Service(new jfxc.Service[Map[String, Int]]() {
    protected def createTask(): jfxc.Task[Map[String, Int]] = new jfxc.Task[Map[String, Int]] {
      protected def call(): Map[String, Int] =
        {
          val dsConnectorFactory = UdsRepository.getDataStoreConnFactory()
          val checkDbsUpdates = new CheckDbsUpdates(dsConnectorFactory)
          checkDbsUpdates.doWork()
          val allUndoneMigrations = checkDbsUpdates.undoneMigrationsByDb
          Map("MSIdb" -> allUndoneMigrations.keysIterator.find(_.matches("^MSIdb_project_[0-9]+$")).size,
            "LCMSdb" -> allUndoneMigrations.keysIterator.find(_.matches("^LCMSdb_project_[0-9]+$")).size,
            "UDSdb" -> allUndoneMigrations.keysIterator.find(_.matches("UDSdb")).size)
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
        val undoneMigrations = this.get
        HelpPopup("Check for updates", "Checking for updates has finished successfully: \n" +
          s"-There are ${undoneMigrations.get("MSIdb").getOrElse("0")} migrations to apply to MSI databases.\n" +
          s"-There are ${undoneMigrations.get("LCMSdb").getOrElse("0")} migrations to apply to LCMS databases.\n" +
          s"-There are ${undoneMigrations.get("UDSdb").getOrElse("0")} migrations to apply to UDS database.\n" +
          "To check each Proline database migration state: \n" +
          "See proline_admin_gui_log for more details.", Some(Monitor.stage), false)
      }
    }
  })
}

