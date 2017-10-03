package fr.proline.admin.gui.wizard.component.items.pgserverconfig.tab

import scalafx.Includes._
import scalafx.geometry.Pos
import scalafx.scene.layout.VBox
import scalafx.geometry.Insets

import java.io.File
import java.io.File.separator

import fr.proline.admin.gui.Wizard
import fr.proline.admin.gui.component.configuration.file._
import fr.proline.admin.gui.wizard.component.items.pgserverconfig._
import fr.proline.admin.gui.wizard.util.PanelScorllPane
/**
 * builds a tab of postgreSQL optimization
 *
 */
class PostgresConfigContentTab extends PanelScorllPane {

  val workingFilePostgresConf = Wizard.pgDataDirPath + File.separator + "postgresql.conf"
  val postgresForm = new PostgresConfigForm(workingFilePostgresConf)(Wizard.stage)
  postgresForm.applyButton.visible = false
  setContentNode(
    new VBox {
      prefWidth <== Wizard.configItemsPanel.width - 50
      prefHeight <== Wizard.configItemsPanel.height - 45
      padding = Insets(5, 0, 0, 0)
      children = List(postgresForm)

    })
  /* save new settings  */
  def saveForm() {
    postgresForm.saveForm()
  }
}