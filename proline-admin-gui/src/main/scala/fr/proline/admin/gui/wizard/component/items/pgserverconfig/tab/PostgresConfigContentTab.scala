package fr.proline.admin.gui.wizard.component.items.pgserverconfig.tab

import scalafx.Includes._
import scalafx.geometry.Pos
import scalafx.scene.layout.VBox
import scalafx.geometry.Insets
import java.io.File
import java.io.File.separator
import fr.proline.admin.gui.Wizard
import fr.proline.admin.gui.component.configuration.file._
import fr.proline.admin.gui.wizard.component.items.serverconfig.tab.content.PanelScorllPane
import fr.proline.admin.gui.wizard.component.items.pgserverconfig._
/**
 * PostgresConfigContentTab contains panel of postgreSQL optimization(PanelScorllPane)
 * 
 */
class PostgresConfigContentTab extends PanelScorllPane {

  val workingFilePostgresConf = Wizard.pgDataDirPath + File.separator + "postgresql.conf"
  val postgresForm = new PostgresConfigForm(workingFilePostgresConf)(Wizard.stage)
  setContentNode(
    new VBox {
      padding = Insets(5, 0, 0, 0)
      alignment = Pos.TOP_RIGHT
      alignmentInParent = Pos.TOP_RIGHT
      content = List(postgresForm)
      prefWidth <== Wizard.configItemsPanel.width - 45
      prefHeight <== Wizard.configItemsPanel.height - 45
    })
 /* save new settings  */
  def saveForm() {
    postgresForm.saveForm()
  }
}