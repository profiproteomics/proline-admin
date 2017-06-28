package fr.proline.admin.gui.component.wizard
import scalafx.Includes._
import scalafx.scene.control.ScrollPane.ScrollBarPolicy
import fr.profi.util.scalafx.CustomScrollPane
import scalafx.geometry.Pos
import fr.proline.admin.gui.QuickStart
//make panel in step 3 scrollable 
abstract class AbstractMountFilesWizard extends CustomScrollPane(hBarPolicy = ScrollBarPolicy.AS_NEEDED) {
  /* Initialize form to null */
  prefWidth <== QuickStart.mainPanel.width - 150
  prefHeight <== QuickStart.mainPanel.height - 50
}