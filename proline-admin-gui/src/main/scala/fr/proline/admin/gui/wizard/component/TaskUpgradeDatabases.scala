package fr.proline.admin.gui.wizard.component
import scalafx.concurrent.Service
import javafx.concurrent.{ Service => JService, Task => JTask }
import fr.proline.admin.gui.wizard.util.GetConfirmation
import fr.proline.admin.service.db.migration.UpgradeAllDatabases
import fr.proline.admin.gui.wizard.util.PopupHelpWindow
import fr.proline.admin.gui.process._
/**
 * Service to upgrade all databases
 */
class TaskUpgradeDatabases extends JService[Unit] {
  override protected def createTask() = new JTask[Unit]() {
    override protected def call() {
      val dsConnectorFactory = UdsRepository.getDataStoreConnFactory()
      try {
        new UpgradeAllDatabases(dsConnectorFactory).doWork()
        updateProgress(100, 100)
      } catch {
        case e: Exception => {
          PopupHelpWindow(
            wTitle = "Databases update Error",
            wText = "Error while trying to update databases.")
        }
      }
    }
  }

}