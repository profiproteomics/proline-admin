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
import fr.profi.util.scalafx.TitledBorderPane
import java.io.File
import fr.proline.admin.gui.util.FxUtils
import fr.proline.admin.gui.IconResource
import fr.proline.admin.gui.wizard.util._
import fr.proline.admin.gui.wizard.component.items.serverconfig.tab._
import fr.proline.admin.gui.Wizard
import fr.proline.admin.gui.wizard.component.Item

/**
 * ServerConfig edit/update database server parameters :host name,user name ,password and port
 * 
 */

class ServerConfig(val name: String) extends Item with LazyLogging {

  /**
   * component
   */
  val panelTitle = new Label("Proline Server Configuration") {
    styleClass = List("item")
  }
  val headerHelpIcon = new Hyperlink {
    graphic = FxUtils.newImageView(IconResource.HELP)
    onAction = handle {
      _openHelpDialog()
    }
  }

  val tabPane = new TabPane()
  val postgres = new PostGreSQLTab(Wizard.adminConfPath)
  val pgAccessTab = new Tab {
    text = "PostGreSQL"
    content = postgres
    closable = false
  }

  val jmsServer = new JmsServerTab(Wizard.jmsNodeConfPath)
  val jmsServerTab = new Tab {
    text = "JMS Server"
    content = jmsServer
    closable = false
  }

  val mountsPoint = new MountPointsContentTab()
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

  // content = List(tabPane)
  alignment = Pos.Center
  alignmentInParent = Pos.Center
  spacing = 1
  content = List(ScalaFxUtils.newVSpacer(minH = 20), new HBox {
    fillWidth = true
    content = Seq(new HBox {
      spacing = 15
      content = Seq(
        new HBox {
        content = Seq(FxUtils.newImageView(IconResource.SETTING))
      }, panelTitle)
    }, ScalaFxUtils.newHSpacer(minW = 45), new HBox {
      content = Seq(headerHelpIcon)
    })
  }, ScalaFxUtils.newVSpacer(minH = 10), tabPane)

  // Help 
  val helpTextBuilder = new StringBuilder()

  helpTextBuilder.append("PostgreSQL: required properties to connect the database server.\n\n")
    .append("\tHost: host name\n").append("\tPort: port number(default: 5432)\n")
    .append("\tUser: user name\n").append("\tPassword \n\n")
    .append("Mount Points: to select file locations\n\n")
    .append("\tResut files : the locations of result files\n")
    .append("\tRaw files : the locations of Raw files\n")
    .append("\tmzDB files : the locations of mzDB files\n\n")
    .append("JMS Server: JMS Server properties.\n\n")
    .append("\tHost: host name\n")
    .append("\tPort: port number(default: 5445)\n")
    .append("\tProline Queue Name: queue name\n\n")

  def _openHelpDialog() = PopupHelpWindow(
    wTitle = "Help",
    wText = helpTextBuilder.toString)
}