package fr.proline.admin.gui.util

import com.typesafe.scalalogging.LazyLogging
import java.io.File

/**
 * Open Proline_AdminGuide.pdf in desktop .
 * @author aromdhani
 */
object AdminGuide extends LazyLogging {

  /** Open in desktop */
  def openUrl(path: String): Unit =
    if (java.awt.Desktop.isDesktopSupported()) {
      try {
        java.awt.Desktop.getDesktop().browse(new File(path).toURI());
      } catch {
        case t: Throwable => logger.error(s"Error while trying to browse Proline_AdminGuide file ${t.getMessage()}")
      }
    }
}