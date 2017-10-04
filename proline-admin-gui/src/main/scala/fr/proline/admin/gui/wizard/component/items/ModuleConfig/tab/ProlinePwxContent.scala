package fr.proline.admin.gui.wizard.component.items.ModuleConfig.tab

import scalafx.Includes._
import scalafx.geometry.Pos
import scalafx.scene.layout.VBox
import scalafx.geometry.Insets
import scalafx.scene.layout.Priority
import scalafx.stage.Stage

import fr.proline.admin.gui.Wizard
import fr.proline.admin.gui.component.configuration.file._
import fr.proline.admin.gui.wizard.component.items.ModuleConfig.tab.Content.ProlinePwx
import fr.proline.admin.gui.wizard.util.PanelScorllPane

/**
 * builds a panel with the mount points of  Proline Web
 *
 */
class ProlinePwxContent(stage: Stage) extends PanelScorllPane {

  val prolinePwx = new ProlinePwx(stage)
  setContentNode(
    new VBox {
      prefWidth <== stage.width - 85
      prefHeight <== stage.height - 45
      padding = Insets(5, 0, 0, 0)
      children = List(prolinePwx)
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