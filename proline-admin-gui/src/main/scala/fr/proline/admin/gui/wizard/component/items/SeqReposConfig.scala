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
import scalafx.geometry.Pos
import fr.profi.util.scalafx.ScalaFxUtils
import fr.profi.util.scala.ScalaUtils
import fr.proline.admin.gui.util.FxUtils
import fr.proline.admin.gui.IconResource
import fr.proline.admin.gui.Wizard
import fr.proline.admin.gui.wizard.component.items.tab._
import fr.proline.admin.gui.wizard.util.ItemName._
import fr.proline.admin.gui.process.config._
import fr.proline.admin.gui.wizard.util._
import fr.proline.admin.gui.wizard.component.items.file.JmsServer
import fr.proline.admin.gui.wizard.component.items.tab.ParsingRulesContent
import fr.proline.admin.gui.wizard.component.items.file.SeqRepos

/**
 * builds a panel with  Sequence Repository  properties database server properties, JMS properties and parsing rules properties
 *
 */

class SeqReposConfig(val name: ItemName) extends Item with LazyLogging {

  /* Sequence repository panel components */
  val panelTitle = new Label("Proline Module Configuration") {
    styleClass = List("item")
  }
  val headerHelpIcon = new Hyperlink {
    graphic = FxUtils.newImageView(IconResource.HELP)
    onAction = handle {
      _openUserGuide()
    }
  }

  /* database server properties tab */
  val PostGreSQLSeq = new SeqRepos(Wizard.seqRepoConfPath, Wizard.stage)
  val PostGresSeqTab = new Tab {
    text = "PostGreSQL"
    content = PostGreSQLSeq
    closable = false
  }
  /* JMS server tab */
  val jmsServer = new JmsServer(Wizard.SeqJmsNodeConfPath)
  val serverJMSTab = new Tab {
    text = "JMS Server"
    content = jmsServer
    closable = false
  }

  /* parsing rules tab */
  val parsingRules = new ParsingRulesContent(Wizard.parsingRulesPath, Wizard.stage)
  val parsingRulesTab = new Tab {
    text = "Sequence Repository Specific"
    content = parsingRules
    closable = false
  }
  tabPane.tabs.addAll(PostGresSeqTab, serverJMSTab, parsingRulesTab)

  /* Layout */
  alignment = Pos.Center
  alignmentInParent = Pos.Center
  prefHeight <== Wizard.stage.height - 50
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