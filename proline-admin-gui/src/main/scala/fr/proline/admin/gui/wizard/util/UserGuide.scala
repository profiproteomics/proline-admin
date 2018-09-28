package fr.proline.admin.gui.wizard.util

import java.awt.Desktop
import java.io.File
import com.typesafe.scalalogging.LazyLogging

/**
 * load user_guide.pdf file.
 *
 */
object UserGuide extends LazyLogging {

  /**
   * Open the file in desktop
   * @param path the path of the user guide file
   */
  def openUrl(path: String): Unit =
    if (java.awt.Desktop.isDesktopSupported()) {
      try {
        java.awt.Desktop.getDesktop().browse(new File(path).toURI());
      } catch {
        case t: Throwable => logger.error(s"Error while trying to open the user guide file: ${t.printStackTrace()}")
      }
    }
}