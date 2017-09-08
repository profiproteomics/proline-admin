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
import fr.profi.util.scala.ScalaUtils
import scalafx.geometry.Pos
import fr.proline.admin.gui.util.FxUtils
import fr.proline.admin.gui.IconResource

import fr.proline.admin.gui.Wizard
import fr.proline.admin.gui.wizard.component.items.ModuleConfig.tab._
import fr.proline.admin.gui.wizard.component.items.serverconfig.tab._
import fr.proline.admin.gui.wizard.component.Item
import fr.proline.admin.gui.process.config._
import fr.proline.admin.gui.wizard.util._
/**
 * panel edit/update Proline Module Config
 */

class ModuleConfig(val name: String) extends Item with LazyLogging {

  /* Proline Module components */
  val panelTitle = new Label("Proline Module Configuration") {
    styleClass = List("item")
  }
  val headerHelpIcon = new Hyperlink {
    graphic = FxUtils.newImageView(IconResource.HELP)
    onAction = handle {
      _openHelpDialog()
    }
  }

  val tabPane = new TabPane()
  /* Proline Sequence Repository database properties tab */
  val PostGreSQLSeq = new PostGreSQLSeqTab(Wizard.seqRepoConfPath)
  val PostGresSeqTab = new Tab {
    text = "PostGreSQL"
    content = PostGreSQLSeq
    closable = false
  }

  /* Proline Jms server tab */
  val jmsServer = new JmsServerTab(Wizard.SeqJmsNodeConfPath)
  val serverJMSTab = new Tab {
    text = "JMS Server"
    content = jmsServer
    closable = false
  }

  /* Sequence Repository Tab*/
  val parsingRules = new ParsingRulesContentTab()
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

  /* help text */
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