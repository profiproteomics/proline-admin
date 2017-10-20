package fr.proline.admin.gui.wizard.component.items.tab

import scalafx.Includes._
import scalafx.scene.layout.VBox
import scalafx.geometry.Insets
import scalafx.stage.Stage
import fr.proline.admin.gui.component.configuration.file._
import fr.proline.admin.gui.component.configuration.form.PgHbaConfigForm
import fr.profi.util.scalafx.CustomScrollPane

/**
 * builds a tab of postgreSQL rights access
 *
 */
class PgHbaConfigContent(pgHbaConfPath: String, stage: Stage) extends CustomScrollPane {
  val pgHbaConfigForm = new PgHbaConfigForm(pgHbaConfPath)
  pgHbaConfigForm.applyButton.visible = false
  setContentNode(
    new VBox {
      prefWidth <== stage.width - 85
      prefHeight <== stage.height - 45
      padding = Insets(5, 0, 0, 0)
      children = List(pgHbaConfigForm)
    })
  /* save settings  */
  def saveForm() {
    pgHbaConfigForm.saveForm()
  }
}