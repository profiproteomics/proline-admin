package fr.proline.admin.gui.process

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import java.io.File
import scalafx.application.Platform
import scalafx.scene.Cursor
import scalafx.scene.Cursor.sfxCursor2jfx
import fr.proline.admin.gui.Main
import fr.proline.admin.gui.Wizard
import fr.proline.admin.gui.Monitor
import fr.proline.admin.gui.component.ButtonsPanel
import fr.proline.admin.gui.util.GetConfirmation
import fr.proline.admin.service.db.SetupProline
import fr.proline.admin.gui.process.config.AdminConfigFile

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

    val actionString = "<b>>> Loading Proline configuration...</b>"
    var _isConfigValid = false

    synchronized {
      Main.stage.scene().setCursor(Cursor.WAIT)
      println("<br>" + actionString)

      try {
        // Parse configuration
        this._setNewProlineConfig()

        // Test connection to database
        val adminConfigOpt = new AdminConfigFile(Main.adminConfPath).read()
        require(adminConfigOpt.isDefined, "Can't load new admin config: undefined.")
        val connectionEstablished = DatabaseConnection.testDbConnection(adminConfigOpt.get, showSuccessPopup = false, showFailurePopup = true)
        require(connectionEstablished, "Can't load new admin config: database is unreachable.")

        logger.info(s"Action '$actionString' finished with success.")
        if (verbose) println(s"""<br/>[ $actionString : <b>success</b> ]""")
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

            println(s"[ $actionString : finished with <b>error</b> ]")
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

  def loadProlineInstallConfig(verbose: Boolean = true): Boolean = {
    //return isConfigValid

    //BLOCKING! otherwise update config before (conf file changes || user's choice) (are||is) effective
    //FIXME: freezing

    val actionString = "<b>>> Loading Proline configuration...</b>"
    var _isConfigValid = false

    synchronized {
      Wizard.stage.scene().setCursor(Cursor.WAIT)
      println("<br>" + actionString)
      try {
        // Parse configuration
        this._setNewProlineInstallConfig()

        // Test connection to database
        val adminConfigOpt = new AdminConfigFile(Wizard.adminConfPath).read()
        require(adminConfigOpt.isDefined, "Can't load new admin config: undefined.")
        val connectionEstablished = DatabaseConnection.testDbConnection(adminConfigOpt.get, showSuccessPopup = false, showFailurePopup = true)
        require(connectionEstablished, "Can't load new admin config: database is unreachable.")

        logger.info(s"Action '$actionString' finished with success.")
        if (verbose) println(s"""<br/>[ $actionString : <b>success</b> ]""")
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

            println(s"[ $actionString : finished with <b>error</b> ]")
          }
          // if re-thrown, system stops
        }

      } finally {
        //ButtonsPanel.computeButtonsAvailability(verbose = verbose, isConfigSemanticsValid = _isConfigValid)
        Wizard.stage.scene().setCursor(Cursor.DEFAULT)
      }
    }
    _isConfigValid

  }
  /**
   *  Update SetupProline config when CO{
   *  NF file changes
   */

  private def _setNewProlineInstallConfig() {

    /** Reload CONF file */
    val newConfigFile = ConfigFactory.parseFile(new File(Wizard.adminConfPath))

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

    /*
    // TODO: decide if we want to support other drivers than Pg in the GUI

    /** Update displayed window, and Proline configuration if data directory already exists */
    val dataDir = newConfigFile.getConfig("proline-config").getString("data-directory")

    if (new File(dataDir).exists()) {

      synchronized {
        SetupProline.setConfigParams(newConfigFile)
        UdsRepository.setUdsDbConfig(SetupProline.getUpdatedConfig.udsDBConfig)
        Platform.runLater(Main.stage.title = s"Proline Admin @ $dataDir")
      }

    } else {
      /** Allow to create data folder if it doesn't exist */
      synchronized {
        logger.warn(s"""Unknown data directory : $dataDir""")
        println(s"""WARN - Unknown data directory : $dataDir""")
      }

      val isConfirmed =
        if (dataDir == """<path/to/proline/data>""" && Main.firstCallToDataDir) {
          Main.firstCallToDataDir = false //don't ask to create default dataDir on application start 
          false

        } else {
          GetConfirmation(
            text = "The databases directory you specified doesn't exist. Do you want to create it?\n(This involves a new installation of Proline.)",
            title = s"Unknown directory : $dataDir",
            yesText = "Yes",
            cancelText = "No"
          )
        }

      if (isConfirmed == true) {
        logger.info(s"Creating data directory : $dataDir")
        println(s"Creating databases directory : $dataDir ...")

        val successfullyCreated = new File(dataDir).mkdirs()
        if (successfullyCreated == true) {
          logger.info("Data directory successfully created.")
          println("INFO - Databases directory successfully created.")

          SetupProline.setConfigParams(newConfigFile)
          UdsRepository.setUdsDbConfig(SetupProline.getUpdatedConfig.udsDBConfig)

          Platform.runLater(Main.stage.title = s"Proline Admin @ $dataDir")

        } else {
          /** If dataDir can't be created */
          Platform.runLater(Main.stage.title = s"Proline Admin (invalid configuration)")
          throw new Exception("Unknown data directory (problem in creation) " + dataDir)
        }

      } else {
        /** If user aborts dataDir creation */
        Platform.runLater(Main.stage.title = s"Proline Admin (invalid configuration)")
        throw new Exception("Unknown data directory (not created, user's choice) " + dataDir)
      }
    }*/
  }
   def _setNewProlineConfigMonitor() {
    /** Reload CONF file */
    val newConfigFileMonitor = ConfigFactory.parseFile(new File(Monitor.adminConfPath))
    synchronized {
      logger.debug("Set new config parameters in ProlineAdmin");
      SetupProline.setConfigParams(newConfigFileMonitor)
      logger.debug("Set new udsDB config");
      UdsRepository.setUdsDbConfig(SetupProline.getUpdatedConfig.udsDBConfig)

    }
  }

}