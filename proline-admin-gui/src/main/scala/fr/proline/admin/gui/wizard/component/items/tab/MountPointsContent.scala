package fr.proline.admin.gui.wizard.component.items.tab

import fr.proline.admin.gui.component.configuration.form._
import scalafx.Includes._
import scalafx.scene.layout.VBox
import scalafx.geometry.Insets
import scalafx.stage.Stage
import fr.proline.admin.gui.component.configuration.file._
import fr.proline.admin.gui.wizard.component.items.file.ProlineMountFiles
import fr.profi.util.scalafx.CustomScrollPane
/**
 * builds a scrollable panel with the mount points of Proline server
 *
 */
class MountPointsContent(stage: Stage) extends CustomScrollPane {

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