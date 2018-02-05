package fr.proline.admin.gui.wizard.monitor.component

import scalafx.scene.control.{ TableColumn, TableView }
import scalafx.scene.control.TableColumn._
import scalafx.scene.layout.VBox

/**
 * Builds Monitor main panel :users,projects and tasks pane
 * @aromdhani
 */

object MainPane extends VBox {

  spacing = 10
  children = Seq(UsesrsPane, ProjectPane, TasksPane)
}