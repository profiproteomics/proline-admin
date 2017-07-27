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
import scalafx.scene.text.{ Font, FontWeight, Text }
import fr.proline.admin.gui.util.FxUtils
import fr.proline.admin.gui.IconResource
import fr.proline.admin.gui.wizard.util._

import fr.proline.admin.gui.wizard.component.items.serverconfig.tab.JmsServerTab
import fr.proline.admin.gui.wizard.component.items.ModuleConfig.tab._
import fr.proline.admin.gui.Wizard
import fr.proline.admin.gui.wizard.component.Item

/**
 * panel edit/update Proline Module Config
 */

class ModuleConfig(val name: String) extends Item with LazyLogging {
  /**
   * component
   */

  val panelTitle = new Label("Proline Module Configuration") {
    font = Font.font("SanSerif", FontWeight.Bold, 14)
  }
  val headerHelpIcon = new Hyperlink {
    graphic = FxUtils.newImageView(IconResource.HELP)
    onAction = handle {
      _openHelpDialog()
    }
  }

  val tabPane = new TabPane()
  val PostGreSQLSeq = new PostGreSQLSeqTab(Wizard.seqRepoConfPath)
  val PostGresSeqTab = new Tab {
    text = "PostGreSQL"
    content = PostGreSQLSeq
    closable = false
  }
  val jmsServer = new JmsServerTab(Wizard.SeqJmsNodeConfPath)
  val serverJMSTab = new Tab {
    text = "JMS Server"
    content = jmsServer
    closable = false
  }
  val parsingRules = new ParsingRulesContentTab()
  val parsingRulesTab = new Tab {
    text = "Sequence Repository Specific"
    content = parsingRules
    disable = false
    closable = false
  }

  tabPane.tabs.addAll(PostGresSeqTab, serverJMSTab, parsingRulesTab)

  alignment = Pos.Center
  alignmentInParent = Pos.Center
  spacing = 1
  content = List(ScalaFxUtils.newVSpacer(minH = 20), new HBox {
    fillWidth = true
    content = Seq(new HBox {
      alignment = Pos.TOP_LEFT
      content = Seq(panelTitle)
    }, ScalaFxUtils.newHSpacer(minW = 60), new HBox {
      alignment = Pos.TOP_RIGHT
      content = Seq(headerHelpIcon)
    })
  }, ScalaFxUtils.newVSpacer(minH = 10), tabPane)

  // help function

  val helpTextBuilder = new StringBuilder()
  helpTextBuilder.append("PostgreSQL: required properties to connect the database server\n\n")
    .append("\tHost: host name\n").append("\tPort: port number(default: 5432)\n")
    .append("\tUser: user name\n").append("\tPassword \n\n").append("JMS Server: JMS Server properties.\n\n")
    .append("\tHost: host name\n").append("\tPort: port number(default: 5445)\n")
    .append("\tProline Queue Name: queue name\n\n").append("Parsing Rules : to add/edit parsing rules\n\n")
    .append("\tLocal Fasta Directories: Specify path to fasta files for SeqRepository daemon. Multiple path separated by ',' between []\n\n")
    .append("\tRules used for parsing fasta entries. Multiple rules could be specified\n\n").append("   Id/name : identifying rule definition\n")
    .append("\tFasta Patern/fasta-name: FASTA file name must match specified Java Regex CASE_INSENSITIVE. multiple Regex separated by ',' between []\n")
    .append("\tFasta File Version: Java Regex with capturing group for fasta release version string (CASE_INSENSITIVE)\n")
    .append("\tAccession Parse Rule: Java Regex with capturing group for protein accession\n\n")
    .append("Default Protein Accession: Default Java Regex with capturing group for protein accession if fasta file name doesn't match parsing_rules RegEx\n")
    .append("default value = >(\\S+) :  String after '>' and before first space\n\n")
  def _openHelpDialog() = PopupHelpWindow(
    wTitle = "Help",
    wText = helpTextBuilder.toString())
}