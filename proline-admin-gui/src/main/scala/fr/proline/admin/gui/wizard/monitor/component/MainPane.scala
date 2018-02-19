package fr.proline.admin.gui.wizard.monitor.component

import scalafx.scene.layout.VBox
import scalafx.scene.control.Label

/**
 * Builds Monitor main panel users, projects and tasks views
 * @author aromdhani
 */

object MainPane extends VBox {
  spacing = 20
  children = Seq(new Label(""), UsesrsPane, ProjectPane)
}