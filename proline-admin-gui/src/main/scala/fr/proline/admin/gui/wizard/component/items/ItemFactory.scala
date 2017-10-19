package fr.proline.admin.gui.wizard.component.items
import fr.proline.admin.gui.wizard.util.ItemName._
import scalafx.scene.layout.VBox
import scalafx.scene.control.TabPane
import scalafx.scene.control.Hyperlink
import scalafx.scene.control.Label
import fr.proline.admin.gui.Wizard
import fr.proline.admin.gui.wizard.util.UserGuideView

import java.io.File
import java.io.File.separator

/**
 * Factory of items
 *
 */
object ItemFactory {
  def apply(item: ItemName): Item = {
    item match {
      case SERVER => {
        new ServerConfig(item)
      }
      case SEQREPOS => {
        new SeqReposConfig(item)
      }
      case PWX => {
        new PwxConfig(item)
      }
      case PGSERVER => {
        new PgServerConfig(item)
      }
    }
  }
}

trait Item extends VBox {
  val panelTitle: Label
  val headerHelpIcon: Hyperlink
  val tabPane = new TabPane()
  def _openUserGuide() {
    UserGuideView.openUrl(Wizard.targetPath + File.separator + "classes" + File.separator + "documentation" + File.separator + "user_guide.pdf")
  }
}
