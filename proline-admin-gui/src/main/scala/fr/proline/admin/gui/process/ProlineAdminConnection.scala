package fr.proline.admin.gui.process

import java.io.File
import com.typesafe.config.ConfigFactory
import fr.proline.admin.gui.Main
import fr.proline.admin.gui.Util
import fr.proline.admin.gui.component.panel.ButtonsPanel
import fr.proline.admin.service.db.SetupProline
import scalafx.application.Platform
import scalafx.beans.property.BooleanProperty.sfxBooleanProperty2jfx
import fr.proline.admin.gui.component.dialog.ConfirmationDialog
import fr.proline.admin.gui.component.dialog.GetConfirmation

/**
 * All utilities to modify ProlineAdmin configuration
 */
object ProlineAdminConnection {
  //TODO: make it a class?

  def updateProlineConf() = LaunchAction(
    actionButton = ButtonsPanel.editConfButton,
    actionString = "<b>>> Updating Proline configuration...</b>",
    action = () => {

      try {
        this._setNewProlineConfig()
        ButtonsPanel.updateBooleans()

      } catch {
        case e: Exception => {
          Platform.runLater {
            println("  ERROR - Could not set new Proline configuration : " + e)
            e.printStackTrace() //TODO: add all stack traces to console log
            ButtonsPanel.dbCanBeUsed.set(false)
            ButtonsPanel.prolineMustBeSetUp.set(false)
          }
          //throw new Exception("Could noooot update proline configuration")
        }
      }
    }
  )

  /**
   *  Update SetupProline config when CONF file changes
   */
  private def _setNewProlineConfig() {

    /** Reload CONF file */
    val newConfigFile = ConfigFactory.parseFile(new File(Main.confPath))

    /** Update displayed window, and Proline configuration if data directory exists */
    val dataDir = newConfigFile.getConfig("proline-config").getString("data-directory")

    if (new File(dataDir).exists()) {
      println("dataDir exists") //TODO: delete me

      println("already exists, set conf")
      SetupProline.setConfigParams(newConfigFile)
      Platform.runLater(Main.stage.title = s"Proline Admin @ $dataDir")

      /** Allow to create data folder if it doesn't exist */
    } else {
      println("dataDir doesn't exist")

      Platform.runLater {
        val isConfirmed =
          GetConfirmation(
            "The databases directory you specified does not exist. Do you want to create it?\n(This involves the creation of a new installation of Proline.)",
            s"Unknown directory : $dataDir"
          )
        if (isConfirmed == true) {
          val successfullyCreated = new File(dataDir).mkdir()

          if (successfullyCreated == true) {
            println("Databases directory successfully created.")
            println("confirmed, set conf")
            SetupProline.setConfigParams(newConfigFile)
            Platform.runLater(Main.stage.title = s"Proline Admin @ $dataDir")

            /** If it can't be created */
          } else {
            Platform.runLater(Main.stage.title = s"Proline Admin (invalid configuration)")
            throw new Exception("Unknown data directory " + dataDir)
          }

        } else {
          Platform.runLater(Main.stage.title = s"Proline Admin (invalid configuration)")
          throw new Exception("Unknown data directory " + dataDir)
        }
      }
    }
  }
}