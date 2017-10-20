package fr.proline.admin.gui.wizard.component.items.tab

import scalafx.Includes._
import scalafx.scene.layout.VBox
import scalafx.geometry.Insets
import scalafx.stage.Stage
import fr.proline.admin.gui.component.configuration.file._
import fr.proline.admin.gui.wizard.component.items.file.ProlinePwx
import fr.profi.util.scalafx.CustomScrollPane

/**
 * builds a panel with the mount points of  Proline Web
 *
 */
class ProlinePwxContent(stage: Stage) extends CustomScrollPane {

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