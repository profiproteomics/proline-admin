package fr.proline.admin.gui.wizard.util

import scalafx.Includes._
import scalafx.scene.control.ScrollPane.ScrollBarPolicy
import fr.profi.util.scalafx.CustomScrollPane

/**
 * builds a custom scroll panel
 *
 */
abstract class PanelScorllPane extends CustomScrollPane(hBarPolicy = ScrollBarPolicy.AS_NEEDED) {
}