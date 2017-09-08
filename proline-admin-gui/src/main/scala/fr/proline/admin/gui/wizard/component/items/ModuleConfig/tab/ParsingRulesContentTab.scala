package fr.proline.admin.gui.wizard.component.items.ModuleConfig.tab

import scalafx.Includes._
import scalafx.geometry.Pos
import scalafx.scene.layout.VBox
import scalafx.geometry.Insets

import fr.proline.admin.gui.Wizard
import fr.proline.admin.gui.component.configuration.file._
import fr.proline.admin.gui.wizard.component.items.serverconfig.tab.content.PanelScorllPane
import fr.proline.admin.gui.wizard.component.items.ModuleConfig.tab.Content.ParsingRules
/**
 * ParsingRulesContentTab contains parsing rules PanelScorllPane
 *
 */
class ParsingRulesContentTab extends PanelScorllPane {

  val rules = new ParsingRules()
  setContentNode(
    new VBox {
      prefWidth <== Wizard.configItemsPanel.width - 50
      prefHeight <== Wizard.configItemsPanel.height - 45
      padding = Insets(5, 0, 0, 0)
      alignment = Pos.TOP_RIGHT
      alignmentInParent = Pos.TOP_RIGHT
      children = Seq(rules)

    })

  /* properties of ParsingRules  */
  def getInfos: String = {
    rules.getProperties
  }

  /* save all parameters on next button */
  def saveForm() {
    rules.saveForm()
  }
}