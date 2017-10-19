package fr.proline.admin.gui.wizard.component.panel.bottom

import scalafx.Includes._
import scalafx.geometry.Pos
import scalafx.geometry.Insets
import scalafx.scene.control.Button
import scalafx.scene.layout.HBox
import scalafx.scene.layout.VBox
import fr.proline.admin.gui.util._
import fr.profi.util.scalafx.ScalaFxUtils
import fr.profi.util.scala.ScalaUtils._
import fr.profi.util.scalafx.ScalaFxUtils
import fr.profi.util.scalafx.ScalaFxUtils.TextStyle._
import fr.proline.admin.gui.Wizard
import fr.proline.admin.gui.IconResource
import fr.proline.admin.gui.wizard.component.items._
import fr.proline.admin.gui.wizard.util.ExitPopup
import fr.proline.admin.gui.wizard.component.panel.main.Install

/**
 * builds bottom home panel: cancel and go buttons
 *
 */
class HomeButtons extends VBox {

  /**
   * ******* *
   * BUTTONS *
   * ******* *
   */

  val cancelButton = new Button("Cancel") {
    graphic = FxUtils.newImageView(IconResource.CANCEL)
    onAction = handle {
      ExitPopup("Exit Setup", "Are you sure you want to exit Proline Install ?", Some(Wizard.stage), false)
    }
  }
  val goButton = new Button(" Go ") {
    graphic = FxUtils.newImageView(IconResource.EXECUTE)
    onAction = handle {
      setInstallItems()
    }
  }

  private def setInstallItems() {
    Install.getSelectedItems
    if (Wizard.items.isEmpty || !Install.setStyleSelectedItems) {
      // ItemsPanel.warningNotValidServerFile.visible = true
    } else {
      Install.warningCorruptedFile.visible = false
      Wizard.currentNode = Wizard.items.head._2.get
      //hide the  previous button for the first panel 
      NavButtons.prevButton.visible = false
      //set the first panel
      Wizard.configItemsPanel.getChildren().clear()
      Wizard.configItemsPanel.getChildren().add(Wizard.currentNode)
      //set buttons panel
      Wizard.buttonsPanel.getChildren().clear()
      Wizard.buttonsPanel.getChildren().add(NavButtons())
    }
  }

  /**
   * ***** *
   * LAYOUT *
   * **** *
   */

  Seq(
    cancelButton,
    goButton).foreach { b =>
      b.prefHeight = 20
      b.prefWidth = 120
      b.styleClass += ("mainButtons")
    }

  /**
   * ****** *
   * APPLY() *
   * ***** *
   */
  children = List(ScalaFxUtils.newVSpacer(20), new HBox {
    alignment = Pos.BaselineRight
    padding = Insets(10)
    spacing = 10
    children = Seq(
      goButton,
      cancelButton)
  })
}