package fr.proline.admin.gui.process

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import java.io.File
import scalafx.application.Platform
import scalafx.scene.Cursor
import scalafx.scene.Cursor.sfxCursor2jfx
import fr.proline.admin.gui.QuickStart
import fr.proline.admin.gui.component.ButtonsPanel
import fr.proline.admin.gui.util.GetConfirmation
import fr.proline.admin.service.db.SetupProline
import fr.proline.admin.gui.process.config.AdminConfigFile

/**
 * All utilities to modify ProlineAdmin configuration
 */
object ProlineAdminConnectionWizard extends LazyLogging {

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
   * Udapte Proline Admin config (processing)
   **/
  def loadProlineConf(verbose: Boolean = true): Boolean = { //return isConfigValid
    
    var _isConfigValid = false

    synchronized {
      QuickStart.stage.scene().setCursor(Cursor.WAIT)
      try {
        // Parse configuration
        this._setNewProlineConfig()
        
        // Test connection to database
        val adminConfigOpt = new AdminConfigFile(QuickStart.adminConfPath).read()
        val connectionEstablished = DatabaseConnection.testDbConnection(adminConfigOpt.get, showSuccessPopup = false, showFailurePopup = true)
        _isConfigValid = true

      } catch {

        case fxt: IllegalStateException => {
          if (verbose) logger.warn(fxt.getLocalizedMessage()) //useful?
        }

        case t: Throwable => {
          synchronized {
            
            if (verbose) System.err.println(t.getMessage())
          }
        }

      } finally {
       
        QuickStart.stage.scene().setCursor(Cursor.DEFAULT)
      }
    }
    _isConfigValid

  } // ends loadProlineConf

  /**
   *  Update SetupProline config when CONF file changes
   */
  private def _setNewProlineConfig() {

    /** Reload CONF file */
    val newConfigFile = ConfigFactory.parseFile(new File(QuickStart.adminConfPath))
    
    synchronized {
      logger.debug("Set new config parameters in ProlineAdmin");
      SetupProline.setConfigParams(newConfigFile)
      logger.debug("Set new udsDB config");
      UdsRepository.setUdsDbConfig(SetupProline.getUpdatedConfig.udsDBConfig)
      Platform.runLater(QuickStart.stage.title = "Proline Admin")
    }

  }
}