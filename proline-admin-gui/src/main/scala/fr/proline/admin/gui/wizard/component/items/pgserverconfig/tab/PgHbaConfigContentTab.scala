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
import fr.proline.admin.gui.component.configuration.form.PgHbaConfigForm
/**
 * panel of postgreSQL right access (in scrollPane)
 *
 */
class PgHbaConfigContentTab extends PanelScorllPane {

  val workingFilePgHbaConf = Wizard.pgDataDirPath + File.separator + "pg_hba.conf"
  val pgHbaConfigForm = new PgHbaConfigForm(workingFilePgHbaConf)
  setContentNode(
    new VBox {
      prefWidth <== Wizard.configItemsPanel.width - 50
      prefHeight <== Wizard.configItemsPanel.height - 45
      padding = Insets(5, 0, 0, 0)
      alignment = Pos.TOP_RIGHT
      alignmentInParent = Pos.TOP_RIGHT
      children = List(pgHbaConfigForm)

    })

  /* save new settings  */
  def saveForm() {
    pgHbaConfigForm.saveForm()
  }
}