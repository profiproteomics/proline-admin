package fr.proline.admin.gui.wizard.component.items.ModuleConfig.tab

import scalafx.Includes._
import scalafx.geometry.Pos
import scalafx.scene.layout.VBox
import scalafx.geometry.Insets

import fr.proline.admin.gui.Wizard
import fr.proline.admin.gui.component.configuration.file._
import fr.proline.admin.gui.wizard.component.items.serverconfig.tab.content.PanelScorllPane
import fr.proline.admin.gui.wizard.component.items.ModuleConfig.tab.Content.ProlinePwx
import scalafx.scene.layout.Priority

/**
 * ParsingRulesContentTab contains Proline PWX  PanelScorllPane
 *
 */
class ProlinePwxContentTab extends PanelScorllPane {

  val prolinePwx = new ProlinePwx()
  setContentNode(
    new VBox {
      prefWidth <== Wizard.configItemsPanel.width - 45
      prefHeight <== Wizard.configItemsPanel.height
      padding = Insets(5, 0, 0, 0)
      alignment = Pos.TOP_RIGHT
      alignmentInParent = Pos.TOP_RIGHT
      content = List(prolinePwx)
    })

  /* properties of ProlinePwx */
  def getInfos: String = {
    prolinePwx.getProperties()
  }

  /* save all parameters */
  def saveForm() {
    prolinePwx.saveForm()
  }
}