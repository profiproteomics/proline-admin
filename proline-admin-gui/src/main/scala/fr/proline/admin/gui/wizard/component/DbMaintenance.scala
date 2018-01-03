package fr.proline.admin.gui.wizard.component

import fr.proline.admin.service.db.migration.UpgradeAllDatabases
import fr.proline.admin.service.db.SetupProline
import fr.proline.admin.gui.process._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Setup/upgrade Proline databases
 *
 */
object DbMaintenance {

  def doWork(): Future[Unit] = Future {
    synchronized {
      var _prolineConfIsOk = false
      try {
        val udsDBConfig = UdsRepository.getUdsDbConfig()
        println("INFO - Proline configuration is valid.")
        _prolineConfIsOk = true
      } catch {
        case t: Throwable => {
          println("Proline configuration is not valid : ", t.printStackTrace())
          _prolineConfIsOk = false
        }
      }
      if (_prolineConfIsOk == true) {
        val _prolineIsSetUp = UdsRepository.isUdsDbReachable(true)
        /* upgrade Proline databases */
        if (_prolineIsSetUp == true) {
          println("INFO - Proline is already setup.")
          val dsConnectorFactory = UdsRepository.getDataStoreConnFactory()
          new UpgradeAllDatabases(dsConnectorFactory).doWork()
        } else {
          /* setup Proline */
          println("INFO -Start to setup proline Database.")
          new SetupProline(SetupProline.getUpdatedConfig(), UdsRepository.getUdsDbConnector()).run()
        }
      }
    }
  }

}
