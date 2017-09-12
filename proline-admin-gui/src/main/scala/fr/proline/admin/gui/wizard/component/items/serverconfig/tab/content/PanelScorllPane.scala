package fr.proline.admin.gui.wizard.component.items.serverconfig.tab.content

import scalafx.Includes._
import scalafx.scene.control.ScrollPane.ScrollBarPolicy
import fr.profi.util.scalafx.CustomScrollPane
import fr.proline.admin.gui.Wizard

abstract class PanelScorllPane extends CustomScrollPane(hBarPolicy = ScrollBarPolicy.AS_NEEDED) {
  prefWidth <== Wizard.configItemsPanel.width - 30
  prefHeight <== Wizard.configItemsPanel.height - 30
}