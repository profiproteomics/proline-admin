package fr.proline.admin.gui.wizard.component

import com.typesafe.scalalogging.LazyLogging
import scalafx.Includes._
import scalafx.geometry.Pos
import scalafx.application.Platform
import scalafx.geometry.Insets
import scalafx.scene.control.Button
import scalafx.scene.layout.HBox
import scalafx.scene.layout.VBox
import scalafx.scene.layout.Priority
import scalafx.scene.Scene
import fr.proline.admin.gui.util._
import fr.profi.util.scalafx.ScalaFxUtils
import fr.profi.util.scala.ScalaUtils._
import fr.profi.util.scalafx.ScalaFxUtils
import fr.profi.util.scalafx.ScalaFxUtils.TextStyle._
import scalafx.scene.control.Label

import fr.proline.admin.gui.{ Wizard, PostInstall }
import fr.proline.admin.gui.IconResource
import fr.proline.admin.gui.wizard.component.items._
import fr.proline.admin.gui.wizard.util.ExitPopup

/**
 * builds bottom home panel: cancel and go buttons
 *
 */
class HomeButtons(pane: String) extends VBox {

  /**
   * ******* *
   * BUTTONS *
   * ******* *
   */

  val cancelButton = new Button("Cancel") {
    graphic = FxUtils.newImageView(IconResource.CANCEL)
    onAction = handle {
      pane match {
        case "install" => ExitPopup("Exit Setup", "Are you sure you want to exit Proline Install ?", Option(Wizard.stage), false)
        case "post_install" => ExitPopup("Exit Setup", "Are you sure you want to exit Proline Post Install ?", Option(PostInstall.stage), false)
        case _ => println("Error while trying to close home panel! ")
      }
    }
  }
  val goButton = new Button(" Go ") {
    graphic = FxUtils.newImageView(IconResource.EXECUTE)
    onAction = handle {
      pane match {
        case "install" => setInstallItems()
        case "post_install" => setPostInstallItems()
        case _ => println("Error while trying to select items!")
      }
    }
  }

  private def setInstallItems() {
    InstallPanel.getSelectedItems
    if (Wizard.items.isEmpty || !InstallPanel.setStyleSelectedItems) {
      // ItemsPanel.warningNotValidServerFile.visible = true
    } else {
      InstallPanel.warningCorruptedFile.visible = false
      Wizard.currentNode = Wizard.items.head._2.get
      //hide the  previous button for the first panel 
      NavigationButtonsPanel.prevButton.visible = false
      //set the first panel
      Wizard.configItemsPanel.getChildren().clear()
      Wizard.configItemsPanel.getChildren().add(Wizard.currentNode)
      //set buttons panel
      Wizard.buttonsPanel.getChildren().clear()
      Wizard.buttonsPanel.getChildren().add(NavigationButtonsPanel())
    }
  }

  private def setPostInstallItems() {
    PostInstallPanel.getSelectedItems
    if (PostInstall.items.isEmpty || !PostInstallPanel.setStyleSelectedItems) {
      // ItemsPanel.warningNotValidServerFile.visible = true
    } else {
     println("test")
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