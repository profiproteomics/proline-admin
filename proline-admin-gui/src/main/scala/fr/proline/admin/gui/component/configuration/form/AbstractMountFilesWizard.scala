package fr.proline.admin.gui.component.configuration.form
import scalafx.Includes._
import scalafx.scene.control.ScrollPane.ScrollBarPolicy

import scalafx.stage.Stage
import fr.profi.util.scalafx.CustomScrollPane
import fr.profi.util.scalafx.TitledBorderPane
import scalafx.scene.Node

abstract class AbstractMountFilesWizard extends CustomScrollPane(hBarPolicy = ScrollBarPolicy.AS_NEEDED) {

  /* Initialize form to null */
  maxHeight = 391
}