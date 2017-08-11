package fr.proline.admin.gui.wizard.component.items.serverconfig.tab

import fr.proline.admin.gui.component.configuration.form._
import scalafx.Includes._
import scalafx.geometry.Pos
import scalafx.scene.layout.VBox
import scalafx.geometry.Insets

import fr.proline.admin.gui.Wizard
import fr.proline.admin.gui.component.configuration.file._
import fr.proline.admin.gui.wizard.component.items.serverconfig.tab.content.PanelScorllPane
import fr.proline.admin.gui.wizard.component.items.serverconfig.tab.content.ProlineMountFiles
/**
 * MountPointsContentTab contains Panel of mount points Proline server (in ScorllPane)
 *
 */
class MountPointsContentTab extends PanelScorllPane {

  var mountfiles = new ProlineMountFiles()
  setContentNode(
    new VBox {
      prefWidth <== Wizard.configItemsPanel.width - 50
      prefHeight <== Wizard.configItemsPanel.height - 45
      padding = Insets(5, 0, 0, 0)
      alignment = Pos.TOP_RIGHT
      alignmentInParent = Pos.TOP_RIGHT
      content = List(mountfiles)

    })

  def getInfos: String = {
    mountfiles.getInfos
  }

  /* save all parameters on next button */
  def saveForm() {
    mountfiles.saveForm()
  }
}