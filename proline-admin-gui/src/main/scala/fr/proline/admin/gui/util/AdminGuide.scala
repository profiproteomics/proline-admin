package fr.proline.admin.gui.util

import com.typesafe.scalalogging.LazyLogging
import scalafx.stage.Stage
import scalafx.scene.control.Label
import fr.proline.admin.gui.Install
import java.io.File

/**
 * Open Proline_AdminGuide.pdf in desktop .
 * @author aromdhani
 */
object AdminGuide extends LazyLogging {

  /** Open in desktop */
  def openUrl(path: String)(implicit stage: Stage): Unit =
    try {
      if (java.awt.Desktop.isDesktopSupported()) {
        java.awt.Desktop.getDesktop().browse(new File(path).toURI())
      }
    } catch {
      case t: Throwable => {
        logger.error(s"Error while trying to browse Proline_Admin guide file ${t.getMessage()}")
        ShowPopupWindow(
          node = new Label(s"Error while trying to browse Proline-Admin guide file!"),
          wTitle = "Proline-Admin guide",
          wParent = Option(stage),
          isResizable = true)
      }
    }
}