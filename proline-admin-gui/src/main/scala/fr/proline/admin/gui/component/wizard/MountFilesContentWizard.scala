package fr.proline.admin.gui.component.wizard

import fr.proline.admin.gui.component.configuration.form._
import scalafx.Includes._
import scalafx.geometry.Pos
import scalafx.scene.layout.VBox
import fr.proline.admin.gui.component.configuration.file._

class MountFilesContent extends AbstractMountFilesWizard  {
 
 var mountfiles=new MonutFiles()
 setContentNode(
    new VBox {
      alignmentInParent = Pos.TOP_RIGHT
      minWidth = 730
      maxWidth = 730
      maxHeight =400
      content = List(
        mountfiles
      )
    }
  )
  
}