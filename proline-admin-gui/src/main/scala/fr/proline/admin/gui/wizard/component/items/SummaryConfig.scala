package fr.proline.admin.gui.wizard.component.items

import com.typesafe.scalalogging.LazyLogging
import scalafx.Includes._
import scalafx.scene.layout.VBox
import scalafx.scene.layout.HBox
import scalafx.scene.control.Tab
import scalafx.scene.control.TabPane
import scalafx.scene.control.Label
import scalafx.scene.control.Hyperlink
import scalafx.scene.layout.Priority
import scalafx.geometry.Pos

import fr.proline.admin.gui.Wizard
import fr.proline.admin.gui.util.FxUtils
import fr.proline.admin.gui.IconResource
import fr.proline.admin.gui.wizard.util.ItemName._
import fr.proline.admin.gui.wizard.util._

import fr.profi.util.scalafx.ScalaFxUtils
import fr.profi.util.scalafx.ScalaFxUtils._

/**
 * builds a panel to summarize the configurations
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
      _openUserGuide()
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
  children = List(ScalaFxUtils.newVSpacer(minH = 20), new HBox {
    fillWidth = true
    children = Seq(new HBox {
      spacing = 15
      children = Seq(new HBox {
        children = Seq(FxUtils.newImageView(IconResource.BIGINFOS))
      }, panelTitle)
    }, ScalaFxUtils.newHSpacer(minW = 45), new HBox {
      children = Seq(headerHelpIcon)
    })
  }, ScalaFxUtils.newVSpacer(minH = 30), new VBox {
    prefWidth <== Wizard.configItemsPanel.width - 30
    prefHeight <== Wizard.configItemsPanel.height - 30
    spacing = 50
    children = List(prolineServerBox, prolineModuleBox, prolineWebBox, prolinePgServerBox)
  })
}