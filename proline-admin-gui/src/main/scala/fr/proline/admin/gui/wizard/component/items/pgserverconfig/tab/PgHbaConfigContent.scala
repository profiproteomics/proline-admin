package fr.proline.admin.gui.wizard.component.items.pgserverconfig.tab

import scalafx.Includes._
import scalafx.geometry.Pos
import scalafx.scene.layout.VBox
import scalafx.geometry.Insets
import scalafx.stage.Stage

import fr.proline.admin.gui.component.configuration.file._
import fr.proline.admin.gui.wizard.component.items.pgserverconfig._
import fr.proline.admin.gui.component.configuration.form.PgHbaConfigForm
import fr.proline.admin.gui.wizard.util.PanelScorllPane

/**
 * builds a tab of postgreSQL rights access
 *
 */
class PgHbaConfigContent(pgHbaConfPath: String, stage: Stage) extends PanelScorllPane {
  val pgHbaConfigForm = new PgHbaConfigForm(pgHbaConfPath)
  pgHbaConfigForm.applyButton.visible = false
  setContentNode(
    new VBox {
      prefWidth <== stage.width - 85
      prefHeight <== stage.height - 45
      padding = Insets(5, 0, 0, 0)
      alignment = Pos.TOP_RIGHT
      alignmentInParent = Pos.TOP_RIGHT
      children = List(pgHbaConfigForm)
    })
  /* save settings  */
  def saveForm() {
    pgHbaConfigForm.saveForm()
  }
}