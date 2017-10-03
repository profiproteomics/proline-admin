package fr.proline.admin.gui.wizard.component.items.serverconfig.tab

import fr.proline.admin.gui.component.configuration.form._
import scalafx.Includes._
import scalafx.geometry.Pos
import scalafx.scene.layout.VBox
import scalafx.geometry.Insets
import scalafx.stage.Stage

import fr.proline.admin.gui.Wizard
import fr.proline.admin.gui.component.configuration.file._
import fr.proline.admin.gui.wizard.component.items.serverconfig.tab.content.ProlineMountFiles
import fr.proline.admin.gui.wizard.util.PanelScorllPane
/**
 * builds a Tab with the mount points in Proline server
 *
 */
class MountPointsContent(stage: Stage) extends PanelScorllPane {

  var mountfiles = new ProlineMountFiles()

  setContentNode(
    new VBox {
      prefWidth <== stage.width - 85
      prefHeight <== stage.height - 45
      padding = Insets(5, 0, 0, 0)
      children = List(mountfiles)
    })

  /** show list of files in summary panel */
  def getInfos: String = {
    mountfiles.getProperties()
  }

  /** save parameters on validate button */
  def saveForm() {
    mountfiles.save()
  }
}