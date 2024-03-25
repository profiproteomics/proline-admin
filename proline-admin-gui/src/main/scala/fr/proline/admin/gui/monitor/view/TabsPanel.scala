package fr.proline.admin.gui.monitor.view

import fr.profi.util.scalafx.ScalaFxUtils
import fr.proline.admin.gui.{IconResource, Monitor}
import fr.proline.admin.gui.monitor.model._
import fr.proline.admin.gui.util.{AdminGuide, FxUtils}
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control.{Hyperlink, Tab, TabPane}
import scalafx.scene.layout.{HBox, Priority, VBox}

import java.io.File

/**
 * TabsPanel Create and display a Tabspane of Proline users, projects and external databases.
 * @author aromdhani
 *
 */

object TabsPanel extends VBox {

  //Help Icon 
  private val headerHelpIcon = new Hyperlink {
    graphic = FxUtils.newImageView(IconResource.HELP)
    alignmentInParent = Pos.BaselineRight
    onAction =  _ => {
      openAdminGuide()
    }
  }
  // Create Proline users TabPane
  private val taskRunner = Monitor.taskRunner
  private val userViewModel = new UserViewModel()
  userViewModel.onInitialize()
  userViewModel.taskRunner = taskRunner
  private val usesrsView = new UsersPanel(userViewModel)
  private val usersTabPane =
    new Tab {
      text = "Proline Users"
      closable_=(false)
      margin = Insets(5, 5, 5, 5)
      content_=(usesrsView)
    }
  // Create Proline projects TabPane
  private val projectViewModel = new ProjectViewModel()
  projectViewModel.onInitialize()
  projectViewModel.taskRunner = Monitor.taskRunner

  private val projectsView = new ProjectsPanel(projectViewModel)
  private val projectsTabPane =
    new Tab {
      text = "Proline Projects"
      closable_=(false)
      margin = Insets(5, 5, 5, 5)
      content_=(projectsView)
    }

  // Create ExternalDb TabPane
  private val externalDbViewModel = new ExternalDbViewModel()
  externalDbViewModel.onInitialize()
  externalDbViewModel.taskRunner = Monitor.taskRunner

  private val externalDbsPanel = new ExternalDbPanel(externalDbViewModel)
  private val externalDbsTabPane =
    new Tab {
      text = "External Databases"
      closable_=(false)
      margin = Insets(5, 5, 5, 5)
      content_=(externalDbsPanel)
    }
  private val tabPane = new TabPane {
    vgrow = Priority.Always
  }

  //TODO add tasks panel
  tabPane.tabs.addAll(usersTabPane, projectsTabPane, externalDbsTabPane)
  private val helpPane = new HBox {
    prefWidth_=(tabPane.getWidth)
    children = Seq(ScalaFxUtils.newHSpacer(minW = tabPane.getWidth - 50), headerHelpIcon)
  }
  hgrow = Priority.Always
  vgrow = Priority.Always
  prefHeight <== (Monitor.stage.height /2)-100
  children = Seq(
    new VBox {
      spacing = 10
      children = Seq(helpPane, tabPane)
    })

  /** Open Proline Admin Guide */
  private def openAdminGuide(): Unit = {
    AdminGuide.openUrl(Monitor.targetPath + File.separator + "classes" + File.separator + "documentation" + File.separator + "Proline_AdminGuide_2.0.pdf")(Monitor.stage)
  }
}