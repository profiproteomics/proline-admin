package fr.proline.admin.gui.component.wizard

import fr.proline.admin.gui.component.configuration.form._
import scalafx.Includes._
import scalafx.geometry.Pos
import scalafx.scene.layout.VBox
import fr.proline.admin.gui.component.configuration.file._
//step 3 : add files
class MountFilesContent extends AbstractMountFilesWizard {

  var mountfiles = new ProlineMountFiles()
  setContentNode(
    new VBox {
      alignmentInParent = Pos.TOP_RIGHT
      minWidth = 530
      maxWidth = 730
      minHeight = 420
      maxHeight = 730
     
      content = List(
        mountfiles)
    })
  //save all parameters in the end : called in finish button
  def saveForm() {
    mountfiles.saveForm()
  }
}