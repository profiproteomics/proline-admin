package fr.proline.admin.gui.util

import com.typesafe.scalalogging.LazyLogging
import java.io.File
import scalafx.beans.binding.NumberBinding.sfxNumberBinding2jfx
import scalafx.scene.image.Image
import scalafx.scene.image.ImageView
import scalafx.stage.DirectoryChooser
import scalafx.stage.FileChooser
import scalafx.stage.FileChooser.ExtensionFilter
import scalafx.stage.Stage
import fr.proline.admin.gui.IconResource
import fr.proline.admin.gui.Main
import fr.profi.util.scala.ScalaUtils

/**
 * GRAPHICAL UTILITIES
 */
object FxUtils extends LazyLogging {

  /** Modal windows location, relative to main window **/
  def getStartX(mainStage: Stage = Main.stage, div: Int = 2): Double = {
    val stageX = mainStage.x
    val stageWidth = mainStage.width
    (stageX + (stageWidth / div)).toDouble
  }

  def getStartY(mainStage: Stage = Main.stage, div: Int = 4): Double = {
    val stageY = mainStage.y
    val stageHeight = mainStage.height
    (stageY + (stageHeight / div)).toDouble
  }

  /** Get image view from resources */
  def newImageView(path: IconResource.Value): ImageView = {
    new ImageView(newImage(path))
  }
  def newImage(path: IconResource.Value): Image = {
    new Image(this.getClass().getResourceAsStream(path.toString()))
  }
  /* directory browsing in Wizard */

  /** File browsing **/
  def browseFile(
    fcTitle: String,
    fcInitialDir: String = "",
    fcExtFilters: Array[ExtensionFilter] = Array(),
    fcInitOwner: Stage = Main.stage //multipleSelection: Boolean = false
    ): File = {

    //fr.profi.util.scalafx.FileBrowsing.browseFile(fcTitle, fcInitialDir, fcExtFilters, fcInitOwner, multipleSelection = true)

    /** Define multiple file chooser */
    val fc = new FileChooser {
      title = fcTitle

      // Initial directory
      if (fcInitialDir != null && fcInitialDir != "") {
        val initDir = new File(fcInitialDir)
        if (initDir.exists()) {
          if (initDir.isDirectory()) initialDirectory = initDir
          else initialDirectory = new File(initDir.getParent())
        }
      }
      // Extension filter(s)
      extensionFilters.add(new ExtensionFilter("All files", "*"))
      fcExtFilters.foreach(extensionFilters.add(_))
    }

    /** Show file chooser and return selected file(s) */
    try {
      fc.showOpenDialog(fcInitOwner)
    } catch {
      case e: Exception => {
        logger.debug("No file selected.")
        null
      }
    }
  }

  /** Directory browsing */
  def browseDirectory(
    dcTitle: String,
    dcInitialDir: String = "",
    dcInitOwner: Stage = Main.stage): File = {

    /** Define directory chooser */
    val dc = new DirectoryChooser {
      title = dcTitle

      if (ScalaUtils.isEmpty(dcInitialDir) == false) {
        val _initFile = new File(dcInitialDir)
        if (_initFile.isDirectory()) initialDirectory = _initFile
        else initialDirectory = _initFile.getParentFile
      }
    }

    /** Show directory chooser and return selected directory */
    dc.showDialog(dcInitOwner)
  }

  /**
   * ******** *
   * WRAPPERS *
   * ******** *
   */

}