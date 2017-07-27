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
 * panel of postgreSQL configuration optimization(in scrollPane)
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
  //  def getInfos: String = {
  //    postgresForm.getProperties
  //  }
  def saveForm() {
    postgresForm.saveForm()
  }
}