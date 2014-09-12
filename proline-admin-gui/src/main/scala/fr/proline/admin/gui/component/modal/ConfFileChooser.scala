package fr.proline.admin.gui.component.modal

import java.io.File

import fr.proline.admin.gui.Main
import fr.proline.admin.gui.Util
import fr.proline.admin.gui.process.ProlineAdminConnection

import scalafx.stage.FileChooser
import scalafx.stage.FileChooser.ExtensionFilter
import scalafx.stage.Stage

/**
 * Create and show a file chooser customed for single configuration file's selection.
 * Used by MenuPanel.scala and Main.scala .
 */
object ConfFileChooser {

  /** Define file chooser */
  val fc = new FileChooser {

    title = "Select Proline configuration file"
    extensionFilters.add(new ExtensionFilter("Configuration files", "*.conf"))
    extensionFilters.add(new ExtensionFilter("All files", "*.*"))

    // initial directory

    if (Main.confPath != null && Main.confPath != "") {
      initialDirectory = new File(new File(Main.confPath).getParent()) //TODO: improve =>  ?new File(Main.jarPath + "/config")

    } else {
      val configFolder = new File(Main.targetPath + "/config")

      if (configFolder.exists()) {
        initialDirectory = configFolder
      } else {
        Util.showPopup(
          "Cannot find configuration files",
          "There is no folder named 'config' near this jar.",
          None
        )
      }
    }

  }

  /**
   *  Show file chooser in given stage (new/main) and update configuration if valid
   */
  def showIn(stage: Stage) {

    val confPath = fc.showOpenDialog(stage).getPath()
    println("Selected configuration file : " + confPath)

    /** Validate path */
    require(confPath matches """.+\.conf$""", "invalid path for configuration file")

    /** Update global variables */
    Main.confPath = confPath

    /** Update main stage's title with newly selected configuration file */
    ProlineAdminConnection.updateProlineConf()
  }
} 