package fr.proline.admin.gui.wizard.util

import java.awt.Desktop
import java.io.File
import com.typesafe.scalalogging.LazyLogging
/**
 * Helper to open your files .
 */
object Browser extends LazyLogging {

  def openUrl(path: String): Unit =
    if (java.awt.Desktop.isDesktopSupported()) {
      try {
        java.awt.Desktop.getDesktop().browse(new File(path).toURI());
      } catch {
        case t: Throwable => logger.error(s"Error while trying to open help file $t ")
      }
    }
}