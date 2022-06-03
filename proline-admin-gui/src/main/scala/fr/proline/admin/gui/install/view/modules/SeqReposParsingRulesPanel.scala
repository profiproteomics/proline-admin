package fr.proline.admin.gui.install.view.modules

import com.typesafe.scalalogging.LazyLogging

import scalafx.Includes._
import scalafx.stage.Stage
import scalafx.geometry.{ Pos, Insets }
import scalafx.scene.control.Button
import scalafx.scene.control.Label
import scalafx.scene.control.TextField
import scalafx.scene.layout.HBox
import scalafx.scene.layout.VBox
import scalafx.scene.control.Tooltip

import fr.proline.admin.gui.IconResource
import fr.proline.admin.gui.process.config._
import fr.proline.admin.gui.install.model.SeqReposModelView
import fr.proline.admin.gui.util.FxUtils
import fr.profi.util.scalafx.BoldLabel
import fr.profi.util.scalafx.FileBrowsing
import fr.profi.util.scalafx.ScalaFxUtils._
import fr.profi.util.scalafx.SimpleBorderPane
import fr.profi.util.scalafx.TitledBorderPane

import fr.profi.util.scala.ScalaUtils
import fr.profi.util.scalafx.CustomScrollPane

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.ListBuffer

/**
 * Creates and displays sequence repository parsing rules panel. It lets user to update, to add or to remove parsing rules from the parsing-rules.conf file using fields.
 * @author aromdhani
 *
 */
class SeqReposParsingRulesPanel(model: SeqReposModelView, stage: Stage) extends CustomScrollPane with LazyLogging {

  /* Load initial parsing rules from parsing-rules.conf file */

  private val parsingRuleOpt = model.parsigRulesConfig()
  require(parsingRuleOpt.isDefined, "parsing rules must be defined!")
  private val parsingRule = parsingRuleOpt.get

  var defaultProteinAccession = parsingRule.defaultProteinAccession.get

  /*
   * ********** *
   * COMPONENTS *
   * ********** *
   */

  /* Default accession protein */
  val fastaDirBox = new VBox { spacing = 10 }
  val RulesBox = new VBox { spacing = 10 }
  val defaultProteinAccessionTip = "Default Java Regex with capturing group for protein accession if fasta file name doesn't match parsing_rules RegEx \n >(\\S+) :  String after '>' and before first space"
  val defaultProteinAccessionLabel = new BoldLabel("Default Protein Accession: ", upperCase = false)
  val defaultProteinAccessionField = new TextField {
    if (defaultProteinAccession != null) text = defaultProteinAccession
    text.onChange { (_, oldText, newText) =>
      defaultProteinAccession = newText
    }
    prefWidth <== stage.width - 100
    promptText = "Default protein accession"
    tooltip = defaultProteinAccessionTip
  }
  val resetAccessionButton = new Button("Default") {
    minWidth = 80
    maxWidth = 80
    graphic = FxUtils.newImageView(IconResource.RESET)
    onAction = _ => {
      defaultProteinAccessionField.setText(">(\\S+)")
    }
  }
  val accessionAndResetBox = new HBox {
    spacing = 10
    children = Seq(defaultProteinAccessionField, resetAccessionButton)
  }
  val defaultAccessionBox = new VBox {
    spacing = V_SPACING
    children = List(defaultProteinAccessionLabel, accessionAndResetBox)
    prefWidth <== fastaDirBox.width
  }

  /* Local Fasta directories */

  val localFastaDirLablel = new BoldLabel("Local Fasta Directories: ", upperCase = false)
  val localFastaDirs = new ArrayBuffer[FastaDirectory]()
  val addLocalFastaDirectory = new Button("Add") {
    graphic = FxUtils.newImageView(IconResource.PLUS)
    onAction = _ => {
      _addFastaDirectory()
    }
  }

  /* Parsing rules List */
  val parsingRulesLablel = new BoldLabel("Parsing Rules: ", upperCase = false)
  val localRules = new ArrayBuffer[Rules]()
  val addRuleButton = new Button("Add") {
    graphic = FxUtils.newImageView(IconResource.PLUS)
    onAction = _ => {
      _addRule()
    }
  }
  private val EMPTY_FASTA_DIR = "Fasta directories are empty. You should have at least one local Fasta directory. Default value will be reset."
  val emptyFastaWarningLabel = new Label() {
    text = EMPTY_FASTA_DIR
    style = TextStyle.RED_ITALIC
    visible = false
    managed <== visible
  }
  private val EMPTY_PARSING_RULE = "Parsing rules are empty. You should have at least one parsing rule. Default values will be reset."
  val emptyRulesWarningLabel = new Label() {
    text = EMPTY_PARSING_RULE
    style = TextStyle.RED_ITALIC
    visible = false
    managed <== visible
  }

  /*
   * ****** *
   * LAYOUT *
   * ****** *
   */

  Seq(localFastaDirLablel, defaultProteinAccessionLabel).foreach(_.minHeight = 25)
  Seq(localFastaDirLablel, defaultProteinAccessionLabel, parsingRulesLablel).foreach(_.minWidth = 150)

  // VBox & HBox spacing

  private val V_SPACING = 10
  private val H_SPACING = 5

  /* Parsing rules panel */
  val parsingRules = new TitledBorderPane(
    title = "Parsing Rules",
    titleTooltip = "Extract accession numbers from Fasta files",
    contentNode = new VBox {
      prefHeight <== stage.height - 200
      prefWidth <== stage.width - 100
      spacing = V_SPACING
      children = List(
        new VBox {
          spacing = 0.5
          children = List(
            emptyFastaWarningLabel,
            emptyRulesWarningLabel)
        },
        defaultAccessionBox,
        new HBox {
          spacing = H_SPACING * 3
          children = List(localFastaDirLablel, addLocalFastaDirectory)
        },
        fastaDirBox,
        new HBox {
          spacing = H_SPACING * 3
          children = List(parsingRulesLablel, addRuleButton)
        },
        RulesBox)
    })

  /*
   * ************* *
   * INIT. CONTENT *
   * ************* *
   */
  def isPrompt(str: String): Boolean = str matches """<.*>"""

  /* Common settings */
  if (parsingRuleOpt.isEmpty) {

    /* Disable parsing rule is undefined */
    logger.error("Error while trying to read parsing rules files.")

  } else {
    val parsingRule = parsingRuleOpt.get
    /* Local Fasta directories  */
    val initFastaDirs = parsingRule.localFastaDirectories
    if (initFastaDirs.isEmpty) {
      _addFastaDirectory()
      warningFastaDir
    } else {
      initFastaDirs.foreach { v => _addFastaDirectory(v) }
      warningFastaDir
    }

    /* Parsing rules */
    val initParsingRules = parsingRule.parsingRules
    if (initParsingRules.isEmpty) {
      _addRule()
      warningParsingRules
    } else {
      val namesBuilder = new StringBuilder()
      initParsingRules.foreach(parsingRule => {
        parsingRule.fastaNames.foreach(name => {
          if (name.equals(parsingRule.fastaNames.last)) { namesBuilder.append(name) }
          else {
            namesBuilder.append(name).append(",")
          }
        })
        _addRule(parsingRule.name, namesBuilder.toString, parsingRule.fastaVersion, parsingRule.proteinAccession)
        namesBuilder.setLength(0)
      })
      warningParsingRules
    }
  }

  /** Add stuff to define another local fasta directory **/
  def _addFastaDirectory(value: String = "") {
    def _onFastaDirDelete(mp: FastaDirectory): Unit = {
      localFastaDirs -= mp
      fastaDirBox.children = localFastaDirs
      warningFastaDir
    }
    localFastaDirs += new FastaDirectory(
      parentStage = stage,
      onDeleteAction = _onFastaDirDelete, value = value)
    fastaDirBox.children = localFastaDirs
    warningFastaDir
  }

  /** Add stuff to define another Rule **/

  def _addRule(name: String = "", fastaName: String = "", fastaVersion: String = "", proteinAccession: String = "") {
    def _onRulesDelete(r: Rules): Unit = {
      localRules -= r
      RulesBox.children = localRules
      warningParsingRules
    }
    localRules += new Rules(
      parentStage = stage,
      onDeleteAction = _onRulesDelete,
      name = name, fastaName = fastaName,
      fastaVersion = fastaVersion,
      proteinAccession = proteinAccession)
    RulesBox.children = localRules
    warningParsingRules
  }
  val parsingRulesPane = new VBox {
    children = Seq(parsingRules)
  }

  /* Set panel content */
  setContentNode(
    new VBox {
      alignment = Pos.Center
      alignmentInParent = Pos.Center
      prefWidth <== stage.width - 90
      prefHeight <== stage.height - 45
      padding = Insets(0, 0, 5, 5)
      children = List(parsingRulesPane)
    })

  /** Return default protein accession **/
  private def _getDefaultProteinAccesion(): Option[String] = {
    Some(defaultProteinAccession)
  }

  /** Return Fasta data directories list **/
  private def _getFastaDirectories(fastaDirArray: ArrayBuffer[FastaDirectory]): ListBuffer[String] = {
    (fastaDirArray.view.map(_.getValue)).to[ListBuffer]
  }

  /** Create a parsing rule  **/
  private def _getListPasrsingRules(ruleArray: ArrayBuffer[Rules]): ListBuffer[Rule] = {
    val listRules: ListBuffer[Rule] = new ListBuffer()
    ruleArray.foreach(rule => {
      if ((!rule.getName.isEmpty) && (!rule.getFastaName.isEmpty) && (!rule.getFastaVersion.isEmpty) && (!rule.getproteinAccession.isEmpty)) {
        val fastNamesList = rule.getFastaName.split(",").map(_.trim).to[ListBuffer]
        listRules += Rule(rule.getName, fastNamesList, rule.getFastaVersion, rule.getproteinAccession)
      } else {
        logger.error("Parsing rule fields must not be empty")
      }
    })
    listRules
  }

  /** Return sequence repository parsing rules */
  def toParsingRule() =
    ParsingRule(
      _getFastaDirectories(localFastaDirs),
      _getListPasrsingRules(localRules),
      _getDefaultProteinAccesion())

  /** Return parsing rules number */
  def getProperties(): String = {
    s"${localRules.size} Parsing rule(s)"
  }
  /* check Fields */

  def warningFastaDir() {
    if (localFastaDirs.isEmpty) emptyFastaWarningLabel.visible = true else emptyFastaWarningLabel.visible = false
  }
  def warningParsingRules() {
    if (localRules.isEmpty) emptyRulesWarningLabel.visible = true else emptyRulesWarningLabel.visible = false
  }
}

// Fasta directory

class FastaDirectory(
    parentStage: Stage,
    onDeleteAction: (FastaDirectory) => Unit,
    key: String = "",
    value: String = "") extends HBox {

  val thisFastaDir = this

  /* Component */
  val localFastaDirTip = """Specify path to fasta files for Sequence Repository daemon. Multiple path separated by ',' between []. For example local-fasta-directories =["S:\\sequence"]"""
  val valueField = new TextField {
    prefWidth <== thisFastaDir.width
    promptText = "Full path"
    text = value
    tooltip = localFastaDirTip
  }
  val browseButton = new Button("Browse") {
    minWidth = 80
    maxWidth = 80
    graphic = FxUtils.newImageView(IconResource.LOAD)
    onAction = _ => {
      val dir = FileBrowsing.browseDirectory(
        dcTitle = "Select local fasta directory",
        dcInitialDir = valueField.text(),
        dcInitOwner = parentStage)
      if (dir != null) valueField.text = dir.getAbsolutePath()
    }
  }
  val removeButton = new Button("Remove") {
    minWidth = 80
    maxWidth = 80
    graphic = FxUtils.newImageView(IconResource.TRASH)
    onAction = _ => { onDeleteAction(thisFastaDir) }
  }
  /* Layout */

  spacing = 10
  alignment = Pos.Center
  children = List(valueField, browseButton, removeButton)

  def getValue = valueField.text()

}

/* Model of a parsing rule defined with GUI fields */

class Rules(
    parentStage: Stage,
    onDeleteAction: (Rules) => Unit,
    name: String,
    fastaName: String,
    fastaVersion: String,
    proteinAccession: String) extends HBox {
  val thisrule = this

  /* Component */
  val warningDatalabel: Label = new Label {
    text = """Fill the following properties of Fasta rule to be considered."""
    style = TextStyle.RED_ITALIC
    visible = false
    managed <== visible
  }
  val nameTip = "identifying rule definition"
  val nameLabel = new Label("Id")
  val nameText = new TextField {
    prefWidth <== thisrule.width
    promptText = "Id"
    text = name
    text.onChange { (_, oldText, newText) =>
      checkForm()
    }
    tooltip = nameTip
  }
  val fastaNameTip = """FASTA file name must match specified Java Regex CASE_INSENSITIVE. multiple Regex separated by ',' between []"""
  val fastaNameLabel = new Label("Fasta Pattern")
  val fastaNameText = new TextField {
    prefWidth <== thisrule.width
    promptText = "All files matching this Java Regex will be concerned"
    text = fastaName
    text.onChange { (_, oldText, newText) =>
      checkForm()
    }
    tooltip = fastaNameTip
  }
  val fastaVersionTip = """Java Regex with capturing group for fasta release version string (CASE_INSENSITIVE)"""
  val fastaVersionLabel = new Label("Fasta File Version")
  val fastaVersionText = new TextField {
    prefWidth <== thisrule.width
    promptText = "Java Regex with capturing group for fasta release version string (CASE_INSENSITIVE)"
    text = fastaVersion
    text.onChange { (_, oldText, newText) =>
      checkForm()
    }
    tooltip = fastaVersionTip
  }
  val proteinAccessionTip = """Java Regex with capturing group for protein accession"""
  val proteinAccessionLabel = new Label("Accession Parse Rule")
  val proteinAccessionText = new TextField {
    prefWidth <== thisrule.width
    promptText = "Java Regex with capturing group for protein accession"
    text = proteinAccession
    text.onChange { (_, oldText, newText) =>
      checkForm()
    }
    tooltip = proteinAccessionTip
  }

  val removeButton = new Button("Remove") {
    minWidth = 80
    maxWidth = 80
    graphic = FxUtils.newImageView(IconResource.TRASH)
    onAction = _ => { onDeleteAction(thisrule) }
  }

  Seq(nameLabel, fastaNameLabel, fastaVersionLabel, proteinAccessionLabel).foreach(_.minWidth = 150)
  /* Layout */
  val ruleBox = new SimpleBorderPane(
    contentNode = new VBox {
      spacing = 5
      children = Seq(warningDatalabel, new HBox {
        spacing = 5
        children = List(nameLabel, nameText, fastaNameLabel, fastaNameText)
      }, new HBox {
        spacing = 5
        children = List(fastaVersionLabel, fastaVersionText, proteinAccessionLabel, proteinAccessionText)
      })
    })

  spacing = 10
  alignment = Pos.Center
  children = List(ruleBox, removeButton)
  def checkForm(): Boolean = {
    val isValidatedFields = Seq(nameText, fastaNameText, fastaVersionText, proteinAccessionText).forall(!_.getText.trim.isEmpty())
    if (isValidatedFields) warningDatalabel.visible = false else warningDatalabel.visible = true
    isValidatedFields
  }
  def getName = nameText.text()
  def getFastaName = fastaNameText.text()
  def getFastaVersion = fastaVersionText.text()
  def getproteinAccession = proteinAccessionText.text()

}


