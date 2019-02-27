package fr.proline.admin.gui.process

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import java.io.File
import scalafx.application.Platform
import scalafx.scene.Cursor

import fr.proline.admin.gui.Main
import fr.proline.admin.gui.Wizard
import fr.proline.admin.gui.Monitor
import fr.proline.admin.gui.component.ButtonsPanel
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
   * Udapte Proline Admin config (processing + UI management)
   */
  def loadProlineConf(verbose: Boolean = true): Boolean = { //return isConfigValid

    //BLOCKING! otherwise update config before (conf file changes || user's choice) (are||is) effective
    //FIXME: freezing

    val actionString = "INFO - Loading Proline configuration..."
    var _isConfigValid = false

    synchronized {
      Main.stage.scene().setCursor(Cursor.WAIT)
      println("" + actionString)
      try {
        // Parse configuration
        this._setNewProlineConfig()

        // Test connection to database
        val adminConfigOpt = new AdminConfigFile(Main.adminConfPath).read()
        require(adminConfigOpt.isDefined, "Can't load new admin config: undefined.")
        val connectionEstablished = DatabaseConnection.testDbConnection(adminConfigOpt.get, showSuccessPopup = false, showFailurePopup = true)
        require(connectionEstablished, "Can't load new admin config: database is unreachable.")

        logger.info(s"Action '$actionString' finished with success.")
        if (verbose) println(s""" $actionString : success """)
        _isConfigValid = true

      } catch {

        case fxt: IllegalStateException => {
          if (verbose) logger.warn(fxt.getLocalizedMessage()) //useful?
        }

        case t: Throwable => {
          synchronized {
            logger.warn("Can't load Proline configuration: ", t)
            // Note: Logs are redirected => we thus print the error to be sure it is displayed in the console
            System.err.println("ERROR - Can't load Proline configuration :")
            if (verbose) System.err.println(t.getMessage())
            println(s"INFO - $actionString : finished withberror")
          }
          // if re-thrown, system stops
        }

      } finally {
        ButtonsPanel.computeButtonsAvailability(verbose = verbose, isConfigSemanticsValid = _isConfigValid)
        Main.stage.scene().setCursor(Cursor.DEFAULT)
      }
    }
    _isConfigValid

  } // ends loadProlineConf

  def loadProlineInstallConfig(confPath: String, verbose: Boolean = false): Boolean = {

    val actionString = "INFO - Loading Proline configuration..."
    var _isConfigValid = false
    synchronized {
      println(actionString)
      try {
        // Parse configuration
        this._setNewProlineInstallConfig(confPath)
        //Test connection to database
        val adminConfigOpt = new AdminConfigFile(confPath).read()
        require(adminConfigOpt.isDefined, "Can't load new admin config: undefined.")
        val connectionEstablished = DatabaseConnection.testDbConnection(adminConfigOpt.get, showSuccessPopup = false, showFailurePopup = true)
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

  def _setNewProlineInstallConfig(confPath: String) {

    /** Reload CONF file */
    val newConfigFile = ConfigFactory.parseFile(new File(confPath))
    synchronized {
      logger.debug("Set new config parameters in ProlineAdmin");
      SetupProline.setConfigParams(newConfigFile)
      logger.debug("Set new udsDB config");
      UdsRepository.setUdsDbConfig(SetupProline.getUpdatedConfig.udsDBConfig)
    }
  }

  private def _setNewProlineConfig() {
    /** Reload CONF file */
    val newConfigFile = ConfigFactory.parseFile(new File(Main.adminConfPath))
    synchronized {
      logger.debug("Set new config parameters in ProlineAdmin");
      SetupProline.setConfigParams(newConfigFile)
      logger.debug("Set new udsDB config");
      UdsRepository.setUdsDbConfig(SetupProline.getUpdatedConfig.udsDBConfig)
      Platform.runLater(Main.stage.title = "Proline Admin")
    }
  }
  def _setNewProlineConfigs(adminConfPath: String): Unit = {
    /** load CONF file */
    require(StringUtils.isEmpty(adminConfPath), "Configuration file could not be null nor empty!")
    val newConfigFileMonitor = ConfigFactory.parseFile(new File(adminConfPath))
    synchronized {
      logger.debug("Set new config parameters in Proline-Admin Monitor");
      SetupProline.setConfigParams(newConfigFileMonitor)
      logger.debug("Set new udsDB config");
      UdsRepository.setUdsDbConfig(SetupProline.getUpdatedConfig.udsDBConfig)
    }
  }
}