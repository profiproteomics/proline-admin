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
      children = List(mountfiles)
    })
  /**show list of files in summary panel */
  def getInfos: String = {
    mountfiles.getInfos
  }

  /** save parameters on validate button */
  def saveForm() {
    mountfiles.saveForm()
  }
}