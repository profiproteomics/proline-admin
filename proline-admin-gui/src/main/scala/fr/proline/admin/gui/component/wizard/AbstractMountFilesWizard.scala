package fr.proline.admin.gui.component.wizard
import scalafx.Includes._
import scalafx.scene.control.ScrollPane.ScrollBarPolicy
import fr.profi.util.scalafx.CustomScrollPane
//make panel in step 3 scrollable 
abstract class AbstractMountFilesWizard extends CustomScrollPane(hBarPolicy = ScrollBarPolicy.AS_NEEDED) {
  /* Initialize form to null */
  maxHeight = 393
}