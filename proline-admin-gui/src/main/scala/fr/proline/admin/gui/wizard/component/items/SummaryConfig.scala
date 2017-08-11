package fr.proline.admin.gui.wizard.component.items

import com.typesafe.scalalogging.LazyLogging
import scalafx.Includes._
import scalafx.scene.layout.VBox
import scalafx.scene.layout.HBox
import scalafx.scene.control.Tab
import scalafx.scene.control.TabPane
import scalafx.scene.control.Label
import scalafx.scene.control.Hyperlink
import javafx.scene.layout.Priority
import fr.profi.util.scalafx.ScalaFxUtils
import fr.profi.util.scalafx.ScalaFxUtils._
import scalafx.geometry.Pos

import fr.proline.admin.gui.Wizard
import fr.proline.admin.gui.util.FxUtils
import fr.proline.admin.gui.IconResource
import fr.proline.admin.gui.wizard.util._
import fr.proline.admin.gui.wizard.component.items.serverconfig.tab._
import fr.proline.admin.gui.wizard.component.Item

/**
 * SummaryConfig contains summary configuration
 *
 */

class SummaryConfig(val name: String) extends Item with LazyLogging {

  /**
   * component
   */
  val panelTitle = new Label("Summary") {
    styleClass = List("item")
  }
  val headerHelpIcon = new Hyperlink {
    graphic = FxUtils.newImageView(IconResource.HELP)
    onAction = handle {
      _openHelpDialog()
    }
  }

  /* initialize summary panels */
  
  var prolineServerBox = new VBox {}
  var prolineModuleBox = new VBox {}
  var prolineWebBox = new VBox {}
  var prolinePgServerBox = new VBox {}
  alignment = Pos.Center
  alignmentInParent = Pos.Center
  prefHeight <== Wizard.stage.height - 50
  spacing = 1
  content = List(ScalaFxUtils.newVSpacer(minH = 20), new HBox {
    fillWidth = true
    content = Seq(new HBox {
      spacing = 15
      content = Seq(new HBox {
        content = Seq(FxUtils.newImageView(IconResource.BIGINFOS))
      }, panelTitle)
    }, ScalaFxUtils.newHSpacer(minW = 45), new HBox {
      content = Seq(headerHelpIcon)
    })
  }, ScalaFxUtils.newVSpacer(minH = 30), new VBox {
    prefWidth <== Wizard.configItemsPanel.width - 30
    prefHeight <== Wizard.configItemsPanel.height - 30
    spacing = 50
    content = List(prolineServerBox, prolineModuleBox, prolineWebBox, prolinePgServerBox)
  })

  /* help function */
  def _openHelpDialog() = PopupHelpWindow(
    wTitle = "Help",
    wText = "The summary of the configuration items")

}