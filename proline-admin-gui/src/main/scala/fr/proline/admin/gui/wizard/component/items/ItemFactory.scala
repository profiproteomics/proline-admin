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
 * create items via item name 
 *
 */
object ItemFactory {
  def apply(item: ItemName): Item = {
    item match {
      case PGSERVER => {
        new PgServerConfig(1)
      }
      case PWX => {
        new PwxConfig(3)
      }
      case SERVER => {
        new ServerConfig(2)
      }
      case SEQREPOS => {
        new SeqReposConfig(4)
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
