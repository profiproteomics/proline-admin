package fr.proline.admin.gui.component.dialog

import java.io.File

import scalafx.stage.FileChooser
import scalafx.stage.FileChooser.ExtensionFilter
import scalafx.stage.Stage

import com.typesafe.scalalogging.slf4j.Logging

import fr.proline.admin.gui.Main
import fr.proline.admin.gui.process.ProlineAdminConnection

/**
 * Create and show a file chooser customed for single configuration file's selection.
 * Used by MenuPanel.scala and Main.scala .
 */
object ConfFileChooser extends Logging {

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
        new PopupWindow(
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

    try {
      val confPath = fc.showOpenDialog(stage).getPath()
      println("<br><b>Selected configuration file : " + confPath + "</b>")

      /** Validate path */
      require(confPath matches """.+\.conf$""", "invalid path for configuration file")

      /** Update global variable */
      Main.confPath = confPath

      /** Update main stage's title with newly selected configuration file */
      ProlineAdminConnection.loadProlineConf()

    } catch {
      case jfx: java.lang.NullPointerException => logger.debug("No file selected")
      case t: Throwable                        => throw t
    }
  }
} 