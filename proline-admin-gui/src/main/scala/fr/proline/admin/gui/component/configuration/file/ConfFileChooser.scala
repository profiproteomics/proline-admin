package fr.proline.admin.gui.component.configuration.file

import com.typesafe.scalalogging.LazyLogging

import java.io.File

import scalafx.stage.FileChooser
import scalafx.stage.FileChooser.ExtensionFilter
import scalafx.stage.Stage

import fr.profi.util.scala.ScalaUtils
import fr.proline.admin.gui.Install

/**
 * Create and show a file chooser customed for single configuration file's selection.
 * Used by MenuPanel.scala and Main.scala .
 */
object ProlineConfigFileChooser extends LazyLogging {
  //TODO move to configuration.file

  // NB: Defaults values are designed for ProlineAdmin config file (not server config file)

  //private var configPath: String = Main.adminConfPath
  private var initDir: String = Install.targetPath + "/config"
  private var configName: String = "ProlineAdmin"
  private var loadConfigWhenChosen: Boolean = true

  /* Define file chooser */
  val fc = new FileChooser {

    title = "Select Proline configuration file"
    extensionFilters.add(new ExtensionFilter("Configuration files", "*.conf"))
    extensionFilters.add(new ExtensionFilter("All files", "*.*"))
  }

  private def _setFChooserInitDir() {

    if (ScalaUtils.isEmpty(initDir) == false) {
      val configFile = new File(initDir)

      if (configFile.exists()) {
        fc.initialDirectory =
          if (configFile.isFile()) new File(configFile.getParent())
          else configFile
      }

    } else {
      fc.initialDirectory = new File(Install.targetPath + "/config")
    }
  }

  /**
   *  Show file chooser in given stage (new/main) and update configuration if valid
   */
  def showIn(stage: Stage): String = {

    try {
      val confPath = fc.showOpenDialog(stage).getPath()

      /* Validate path */
      require(confPath matches """.+\.conf$""", "invalid path for configuration file")

      confPath

    } catch {

      case jfx: java.lang.NullPointerException => {
        logger.debug(s"No $configName configuration file selected.")
        null
      }

      case t: Throwable => {
        throw t
        null
      }
    }
  }

  /**
   *  Update properties : adapt to desired config file ( ProlineAdmin / server )
   */
  def setForProlineAdminConf(initDir: String) {
    _updateChooser(initDir, configName = "ProlineAdmin", loadConfigWhenChosen = true)
  }
  def setForProlineServerConf(initDir: String) {
    _updateChooser(initDir, configName = "Proline server", loadConfigWhenChosen = true)
  }
  def setForPwxConf(initDir: String) {
    _updateChooser(initDir, configName = "Proline Web Extension (PWX)", loadConfigWhenChosen = false)
  }
  def setForSeqReposConf(initDir: String) {
    _updateChooser(initDir, configName = "Sequence Repository", loadConfigWhenChosen = false)
  }
  private def _updateChooser(initDir: String, configName: String, loadConfigWhenChosen: Boolean) {

    this.initDir = initDir

    this.configName = configName

    fc.title = s"Select $configName configuration file"
    _setFChooserInitDir()

    this.loadConfigWhenChosen = loadConfigWhenChosen
  }
}
