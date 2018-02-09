package fr.proline.admin.gui.wizard.component.panel.bottom

import com.typesafe.scalalogging.LazyLogging
import scalafx.Includes._
import scalafx.geometry.Pos
import scalafx.geometry.Insets
import scalafx.scene.control.Button
import scalafx.scene.layout.HBox
import fr.proline.admin.gui.wizard.util.GetConfirmation
import fr.profi.util.scala.ScalaUtils._
import fr.profi.util.scalafx.ScalaFxUtils.TextStyle._
import javafx.scene.control.ContentDisplay
import fr.proline.admin.gui.util.FxUtils
import fr.proline.admin.gui.Wizard
import fr.proline.admin.gui.IconResource
import fr.proline.admin.gui.wizard.component.items._
import fr.proline.admin.gui.wizard.util.ExitPopup
import fr.proline.admin.gui.wizard.component.panel.main.Summary
import scalafx.scene.control.Button.sfxButton2jfx
import fr.proline.admin.gui.Wizard

/**
 * Panel contains navigation buttons : previous next and cancel
 *
 */
trait INavButtons extends LazyLogging {

  /**
   * ******* *
   * BUTTONS *
   * ******* *
   */
  val cancelButton = new Button("Exit") {
    graphic = FxUtils.newImageView(IconResource.CANCEL)
    onAction = handle {
      cancel()
    }
  }
  val nextButton = new Button("Next") {
    graphic = FxUtils.newImageView(IconResource.ARROWRIGHT)
    contentDisplay = ContentDisplay.RIGHT
    onAction = handle {
      next()
    }
  }
  val prevButton = new Button("Previous") {
    graphic = FxUtils.newImageView(IconResource.ARROWLEFT)
    onAction = handle {
      previous()
    }
  }
  val validateButton = new Button("Validate") {
    graphic = FxUtils.newImageView(IconResource.SAVE)
    onAction = handle {
      // validate modifications 
      val confirmed = GetConfirmation("Are you sure that you want to save the new Proline configurations? ", "Confirm your action", "Yes", "Cancel", Wizard.stage)
      if (confirmed) {
        validate()
      }
    }
  }

  def cancel(): Unit
  def next(): Unit
  def previous(): Unit
  def validate(): Unit

  /**
   * ***** *
   * LAYOUT *
   * **** *
   */

  val prevBox = new HBox {
    children = Seq(prevButton)
  }
  val nextBox = new HBox {
    children = Seq(nextButton)
  }
  val cancelBox = new HBox {
    children = Seq(cancelButton)
  }

  Seq(
    cancelButton, nextButton, prevButton, validateButton).foreach { b =>
      b.prefHeight = 20
      b.prefWidth = 120
      b.styleClass += ("mainButtons")
    }
}