package fr.proline.admin.gui.wizard.component.items.ModuleConfig.tab

import scalafx.Includes._
import scalafx.geometry.Pos
import scalafx.scene.layout.VBox
import scalafx.geometry.Insets
import scalafx.stage.Stage

import fr.proline.admin.gui.Wizard
import fr.proline.admin.gui.component.configuration.file._
import fr.proline.admin.gui.wizard.util.PanelScorllPane
import fr.proline.admin.gui.wizard.component.items.ModuleConfig.tab.Content.ParsingRules
/**
 * builds a tab with the properties of parsing rules
 *
 */
class ParsingRulesContent(configPath: String, stage: Stage) extends PanelScorllPane {
  val rules = new ParsingRules(configPath, stage)
  setContentNode(
    new VBox {
      prefWidth <== stage.width - 85
      prefHeight <== stage.height - 45
      padding = Insets(5, 0, 0, 0)
      children = Seq(rules)
    })

  /* properties of Parsing rules  */
  def getInfos: String = {
    rules.getProperties
  }
  /* save all parameters on validate */
  def saveForm() {
    rules.save()
  }
}