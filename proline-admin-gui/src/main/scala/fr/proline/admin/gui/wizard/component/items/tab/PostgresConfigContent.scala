package fr.proline.admin.gui.wizard.component.items.tab

import scalafx.Includes._
import scalafx.scene.layout.VBox
import scalafx.geometry.Insets
import scalafx.stage.Stage
import fr.proline.admin.gui.component.configuration.file._
import fr.proline.admin.gui.component.configuration.form._
import fr.profi.util.scalafx.CustomScrollPane
/**
 * builds a tab of postgreSQL optimization
 *
 */

class PostgresConfigContent(dataDir: String, stage: Stage) extends CustomScrollPane {

  val postgresForm = new PostgresConfigForm(dataDir)(stage)
  postgresForm.applyButton.visible = false
  setContentNode(
    new VBox {
      prefWidth <== stage.width - 85
      prefHeight <== stage.height - 45
      padding = Insets(5, 0, 0, 0)
      children = List(postgresForm)
    })
  /* save new settings  */
  def saveForm() {
    postgresForm.saveForm()
  }
}