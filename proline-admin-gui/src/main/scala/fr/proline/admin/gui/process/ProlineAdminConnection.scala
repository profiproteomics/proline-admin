package fr.proline.admin.gui.process

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging

import java.io.File

import scalafx.application.Platform
import scalafx.scene.Cursor
import scalafx.scene.Cursor.sfxCursor2jfx

import fr.proline.admin.gui.Main
import fr.proline.admin.gui.component.ButtonsPanel
import fr.proline.admin.gui.util.GetConfirmation
import fr.proline.admin.service.db.SetupProline

/**
 * All utilities to modify ProlineAdmin configuration
 */
object ProlineAdminConnection extends LazyLogging {

  /**
   * Test if data directory in provided config exists
   **/
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
   **/
  def loadProlineConf(verbose: Boolean = true): Boolean = { //return isConfigValid
    
    //BLOCKING! otherwise update config before (conf file changes || user's choice) (are||is) effective
    //FIXME: freezing

    val actionString = "<b>>> Loading Proline configuration...</b>"
    var _isConfigValid = false

    synchronized {
      Main.stage.scene().setCursor(Cursor.WAIT)
      println("<br>" + actionString)
      

      try {
        this._setNewProlineConfig()

        logger.info(s"Action '$actionString' finished with success.")
        if (verbose) println(s"""[ $actionString : <b>success</b> ]""")
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

  /**
   *  Update SetupProline config when CONF file changes
   */
  private def _setNewProlineConfig() {

    /** Reload CONF file */
    val newConfigFile = ConfigFactory.parseFile(new File(Main.adminConfPath))
    
    synchronized {
      SetupProline.setConfigParams(newConfigFile)
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
}