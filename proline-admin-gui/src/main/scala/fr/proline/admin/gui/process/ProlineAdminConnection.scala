package fr.proline.admin.gui.process

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import java.io.File
import scalafx.application.Platform
import scalafx.stage.Stage
import scalafx.scene.Cursor
//import fr.proline.admin.gui.Main
import fr.proline.admin.gui.Monitor
//import fr.proline.admin.gui.component.ButtonsPanel
import fr.proline.admin.gui.util.GetConfirmation
import fr.proline.admin.service.db.SetupProline
import fr.proline.admin.gui.process.config.AdminConfigFile
import fr.profi.util.StringUtils

/**
 * All utilities to modify ProlineAdmin configuration
 */
object ProlineAdminConnection extends LazyLogging {

  /**
   * Test if data directory in provided config exists
   */
  def dataDirExists(config: Config): (Boolean, String) = { //exists, path

    try {
      val dataDir = config.getConfig("proline-config").getString("data-directory")
      (new File(dataDir).exists(), dataDir)

    } catch {
      case t: Throwable => {
        logger.warn("Can't find data-directory in config: ", t)
        (false, "")
      }
    }
  }

  /**
   * Update Proline-Admin config (processing + UI management)
   */

  def loadProlineInstallConfig(confPath: String, verbose: Boolean = false, parent: Option[Stage]): Boolean = {

    val actionString = "INFO - Loading Proline configuration..."
    var _isConfigValid = false
    synchronized {
      println(actionString)
      try {
        // Parse configuration
        this.setNewProlineInstallConfig(confPath)
        //Test database connection 
        val adminConfigOpt = new AdminConfigFile(confPath).read()
        require(adminConfigOpt.isDefined, "Can't load new admin config: undefined.")
        val connectionEstablished = DatabaseConnection.testDbConnection(adminConfigOpt.get, showSuccessPopup = false, showFailurePopup = false, parent)
        require(connectionEstablished, "Can't load new admin config: database is unreachable.")
        logger.info(s"INFO - Action '$actionString' finished with success.")
        _isConfigValid = true

      } catch {
        case t: Throwable => {
          synchronized {
            logger.warn("Can't load Proline configuration: ", t)
            // Note: Logs are redirected => we thus print the error to be sure it is displayed in the console
            System.err.println("ERROR - Can't load Proline configuration :")
            if (verbose) System.err.println(t.getMessage())

            println(s"INFO - $actionString : finished with error")
          }
          _isConfigValid = false
        }
      }
    }
    _isConfigValid

  }

  /**
   *  Update SetupProline config when CONF
   *  file changes
   */

  def setNewProlineInstallConfig(confPath: String) {
    /** Load CONF file */
    val newConfigFile = ConfigFactory.parseFile(new File(confPath))
    synchronized {
      logger.debug("Set new config parameters in Proline-Admin GUI");
      SetupProline.setConfigParams(newConfigFile)
      logger.debug("Set new udsDB config");
      UdsRepository.setUdsDbConfig(SetupProline.getUpdatedConfig.udsDBConfig)
    }
  }

}