package fr.proline.admin.gui.wizard.monitor.component

import scalafx.Includes._
import scalafx.scene.layout.VBox
import scalafx.geometry.{ Insets, Pos }
import scalafx.scene.control.Tab
import scalafx.scene.control.TabPane
import scalafx.scene.control.Hyperlink
import scalafx.scene.layout.Priority
import scalafx.scene.layout.HBox
import fr.proline.admin.gui.util.FxUtils
import fr.proline.admin.gui.IconResource
import fr.proline.admin.gui.Monitor
import fr.proline.admin.gui.wizard.util.UserGuide
import fr.profi.util.scalafx.ScalaFxUtils
import java.io.File

/**
 * MainPanel builds a list of Tabpane of users, projects, databases and tasks.
 * @author aromdhani
 *
 */

object MainPanel extends VBox {
  val headerHelpIcon = new Hyperlink {
    graphic = FxUtils.newImageView(IconResource.HELP)
    alignmentInParent = Pos.BASELINE_RIGHT
    onAction = handle {
      _openUserGuide()
    }
  }
  val usersTabPane =
    new Tab {
      text = "Proline Users"
      closable_=(false)
      margin = Insets(5, 5, 5, 5)
      content_=(UsesrsPanel)
    }
  val projectTabPane =
    new Tab {
      text = "Proline Projects"
      closable_=(false)
      margin = Insets(5, 5, 5, 5)
      content_=(ProjectPane)
    }

  val dbTabPane =
    new Tab {
      text = "Proline Databases"
      closable_=(false)
      margin = Insets(5, 5, 5, 5)
      content_=(DatabasesPanel)
    }

  /* to do  monitor Proline tasks */
  val tasksTabPane =
    new Tab {
      text = "Proline Tasks"
      closable_=(false)
      margin = Insets(5, 5, 5, 5)
      content_=(TasksPanel)
    }

  /**
   * Create all Tabs
   */

  val tabPane = new TabPane {
    vgrow = Priority.ALWAYS
  }

  // to do add tasks panel
  tabPane.tabs.addAll(usersTabPane, projectTabPane, dbTabPane)
  val helepPane = new HBox {
    prefWidth_=(tabPane.getWidth)
    children = Seq(ScalaFxUtils.newHSpacer(minW = tabPane.getWidth - 50), headerHelpIcon)
  }

  children = {
    new VBox {
      spacing = 10
      children = Seq(helepPane, tabPane)
    }
  }

  def _openUserGuide() {
    UserGuide.openUrl(Monitor.targetPath + File.separator + "classes" + File.separator + "documentation" + File.separator + "Proline_AdminGuide_1.7.pdf")
  }
}