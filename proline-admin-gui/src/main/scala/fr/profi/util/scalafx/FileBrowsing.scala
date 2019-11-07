package fr.profi.util.scalafx

import java.io.File

import scalafx.stage.DirectoryChooser
import scalafx.stage.FileChooser
import scalafx.stage.FileChooser.ExtensionFilter
import scalafx.stage.Stage

object FileBrowsing {
  
  // TODO: deal with options
  // use in PAdmin-GUI

    /**
   * File browsing
   */
  def browseFile(
    fcTitle: String,
    fcInitialDir: String = "",
    fcExtFilters: Array[ExtensionFilter] = Array(),
    fcInitOwner: Stage, // = GUI.stage,
    multipleSelection: Boolean = false
  ): Array[File] = {

    /** Define multiple file chooser */
    val fc = new FileChooser {
      // Title
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
      if (multipleSelection) fc.showOpenMultipleDialog(fcInitOwner).toArray
      else Array(fc.showOpenDialog(fcInitOwner))
    } catch {
      case e: Exception => Array() //{Logger.debug(e); Array();}
    }
  }

  def browseFiles(
    fcTitle: String,
    fcInitialDir: String = "",
    fcExtFilters: Array[ExtensionFilter] = Array(),
    fcInitOwner: Stage // = GUI.stage
  ): Array[File] = {
    
    this.browseFile(
      fcTitle,
      fcInitialDir,
      fcExtFilters,
      fcInitOwner,
      multipleSelection = true
    )
  }

  /**
   * Directory browsing
   */
  def browseDirectory(
    dcTitle: String,
    dcInitialDir: String = "",
    dcInitOwner: Stage // = GUI.stage
  ): File = {

    /** Define directory chooser */
    val dc = new DirectoryChooser {
      title = dcTitle
      if (dcInitialDir != null && dcInitialDir != "") {
        val _initFile = new File(dcInitialDir)
        if (_initFile.isDirectory()) initialDirectory = _initFile
      }
    }

    /** Show directory chooser and return selected directory */
    dc.showDialog(dcInitOwner)
  }
}