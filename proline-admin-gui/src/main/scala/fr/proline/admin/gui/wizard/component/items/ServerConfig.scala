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
import fr.proline.admin.gui.wizard.component.items.tab._
import fr.proline.admin.gui.wizard.util.ItemName._
import fr.proline.admin.gui.wizard.util._
import fr.proline.admin.gui.wizard.component.items.file.{Server,JmsServer}
import fr.proline.admin.gui.wizard.component.items.tab.MountPointsContent

import fr.profi.util.scalafx.TitledBorderPane
import fr.profi.util.scalafx.ScalaFxUtils
import fr.profi.util.scalafx.ScalaFxUtils._


/**
 * builds a panel with the server properties : database server properties, JMS properties and mount Points
 *
 */

class ServerConfig(val name: ItemName) extends Item with LazyLogging {

  /* Proline server components  */
  val panelTitle = new Label("Proline Server Configuration") {
    styleClass = List("item")
  }
  val headerHelpIcon = new Hyperlink {
    graphic = FxUtils.newImageView(IconResource.HELP)
    onAction = handle {
      _openUserGuide()
    }
  }
  /*  database server properties tab */
  val postgres = new Server(Wizard.adminConfPath)
  val pgAccessTab = new Tab {
    text = "PostGreSQL"
    content = postgres
    closable = false
  }
  /* JMS server tab */
  val jmsServer = new JmsServer(Wizard.jmsNodeConfPath)
  val jmsServerTab = new Tab {
    text = "JMS Server"
    content = jmsServer
    closable = false
  }
  /* mount points tab*/
  val mountsPoint = new MountPointsContent(Wizard.stage)
  val mountPointsTab = new Tab {
    text = "Mount Points"
    content = mountsPoint
    closable = false
  }

  tabPane.tabs.addAll(pgAccessTab, jmsServerTab, mountPointsTab)

  /*
   * ************** *
   * INITIALIZATION *
   * ************** *
   */

  alignment = Pos.Center
  alignmentInParent = Pos.Center
  prefHeight <== Wizard.stage.height - 50
  spacing = 1
  children = List(ScalaFxUtils.newVSpacer(minH = 20), new HBox {
    fillWidth = true
    children = Seq(new HBox {
      spacing = 15
      children = Seq(
        new HBox {
          children = Seq(FxUtils.newImageView(IconResource.SETTING))
        }, panelTitle)
    }, ScalaFxUtils.newHSpacer(minW = 45), new HBox {
      children = Seq(headerHelpIcon)
    })
  }, ScalaFxUtils.newVSpacer(minH = 10), tabPane)
}