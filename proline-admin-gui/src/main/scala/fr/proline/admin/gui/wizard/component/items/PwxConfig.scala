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
import fr.proline.admin.gui.wizard.component.items.ModuleConfig.tab._
import fr.proline.admin.gui.wizard.component.items.serverconfig.tab._
import fr.proline.admin.gui.wizard.component.Item
import fr.proline.admin.gui.wizard.util.ItemName._
import fr.proline.admin.gui.process.config._
import fr.proline.admin.gui.wizard.util._

import fr.profi.util.scalafx.ScalaFxUtils
import fr.profi.util.scala.ScalaUtils

/**
 *  builds a Panel with  PWX properties: mount points
 *
 */

class PwxConfig(val name: ItemName) extends Item with LazyLogging {

  /* Proline Web components */
  val panelTitle = new Label("Proline Web Configuration") {
    styleClass = Seq("item")
  }
  val headerHelpIcon = new Hyperlink {
    graphic = FxUtils.newImageView(IconResource.HELP)
    onAction = handle {
      _openUserGuide()
    }
  }
  val tabPane = new TabPane()

  /* Proline Web Tab */
  val prolinePwx = new ProlinePwxContent(Wizard.stage)
  val prolinePwxTab = new Tab {
    text = " Proline Web "
    content = prolinePwx
    closable = false
  }

  tabPane.tabs.addAll(prolinePwxTab)
  /* Layout */
  alignment = Pos.Center
  alignmentInParent = Pos.Center
  prefHeight <== Wizard.stage.height - 50
  prefHeight <== Wizard.stage.width - 50
  spacing = 1
  children = List(ScalaFxUtils.newVSpacer(minH = 20), new HBox {
    fillWidth = true
    children = Seq(new HBox {
      spacing = 15
      children = Seq(new HBox {
        children = Seq(FxUtils.newImageView(IconResource.SETTING))
      }, panelTitle)
    }, ScalaFxUtils.newHSpacer(minW = 45), new HBox {
      children = Seq(headerHelpIcon)
    })
  }, ScalaFxUtils.newVSpacer(minH = 10), tabPane)
}