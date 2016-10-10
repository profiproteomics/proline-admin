package fr.proline.admin.gui.component.configuration.form

import fr.proline.admin.gui.component.configuration.form._
import scalafx.Includes._
import scalafx.geometry.Insets
import scalafx.geometry.Pos
import scalafx.scene.layout.VBox
import scalafx.stage.Stage
import fr.proline.admin.gui.QuickStart
import fr.proline.admin.gui.component.configuration.file._

class MountFilesContent extends AbstractMountFilesWizard  {
 
 var mountfiles=new MonutFiles()
 setContentNode(
    new VBox {
      alignmentInParent = Pos.TOP_RIGHT
      spacing = 5
      minWidth = 730
      maxWidth = 730
      content = List(
        mountfiles
      )
    }
  )
  
}