package fr.proline.admin.gui.wizard.component

import javafx.concurrent.{ Service => JService, Task => JTask }
import fr.proline.admin.service.db.migration.UpgradeAllDatabases
import fr.proline.admin.service.db.SetupProline
import fr.proline.admin.gui.process._

/**
 * service to setup/upgrade Proline databases
 *
 */
class DbMaintenance extends JService[Unit] {
  override protected def createTask() = new JTask[Unit]() {
    override protected def call() {

      /* Config is OK if udsDBConfig can be built with it */
      var _prolineConfIsOk = false
      try {
        val udsDBConfig = UdsRepository.getUdsDbConfig()
        println("INFO - Proline configuration is valid.")
        _prolineConfIsOk = true
      } catch {
        case e: Throwable => {
          println("Proline configuration is not valid.")
          _prolineConfIsOk = false
        }
      }
      if (_prolineConfIsOk == true) {
        val _prolineIsSetUp = UdsRepository.isUdsDbReachable(true)
        /* setup Proline */
        if (_prolineIsSetUp == true) {
          println("INFO - Proline is already setup.")
          val dsConnectorFactory = UdsRepository.getDataStoreConnFactory()
          new UpgradeAllDatabases(dsConnectorFactory).doWork()
          updateProgress(100, 100)
        } else {
          /* upgrade Proline dtabases */
          println("INFO -Start to setup proline Database.")
          new SetupProline(SetupProline.getUpdatedConfig(), UdsRepository.getUdsDbConnector()).run()
        }
      }
    }
  }
}
