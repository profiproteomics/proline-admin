package fr.proline.admin.gui.wizard.component.items.file

import com.typesafe.scalalogging.LazyLogging
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.ListBuffer
import scalafx.Includes._
import scalafx.geometry.Pos
import scalafx.scene.control.Button
import scalafx.scene.control.Label
import scalafx.scene.control.TextField
import scalafx.scene.layout.HBox
import scalafx.scene.layout.VBox
import scalafx.stage.Stage
import fr.proline.admin.gui.IconResource
import fr.proline.admin.gui.process.config._
import fr.proline.admin.gui.util.FxUtils
import fr.profi.util.scalafx.FileBrowsing
import fr.profi.util.scalafx.ScalaFxUtils._
import fr.profi.util.scalafx.TitledBorderPane
import fr.profi.util.scalafx.SimpleBorderPane
import fr.profi.util.scalafx.BoldLabel
import fr.proline.admin.gui.wizard.process.config._
import fr.profi.util.scala.ScalaUtils
import scala.concurrent._
import ExecutionContext.Implicits.global


/**
 * ParsingRules create a modal window to edit/add parsing rules file.
 */
class ParsingRules(path:String,stage:Stage) extends VBox with LazyLogging {

  /* parsing rules file */
  private val parsigRuleFile = new ParsingRulesFile(path)
  private val parsingRuleOpt = parsigRuleFile.read()
  require(parsingRuleOpt.isDefined, "parsing rules is undefined")
  private val parsingRule = parsingRuleOpt.get

  var defaultProteinAccession = parsingRule.defaultProteinAccession.get

  /*
   * ********** *
   * COMPONENTS *
   * ********** *
   */

  /* default accession protein */
  val fastaDirBox = new VBox { spacing = 10 }
  val RulesBox = new VBox { spacing = 10 }
  val defaultProteinAccessionLabel = new Label("Default Protein Accession: ")
  val defaultProteinAccessionField = new TextField {
    if (defaultProteinAccession != null) text = defaultProteinAccession
    text.onChange { (_, oldText, newText) =>
      defaultProteinAccession = newText
    }
    prefWidth <== stage.width - 60
    promptText = "Default protein accession"
  }
  val resetAccessionButton = new Button("Default") {
    minWidth = 80
    maxWidth = 80
    graphic = FxUtils.newImageView(IconResource.RESET)
    onAction = handle {
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

  /* local Fasta directories */
  val localFastaDirLablel = new BoldLabel("Local Fasta Directories: ", upperCase = false)
  val localFastaDirs = new ArrayBuffer[FastaDirectory]()
  val addLocalFastaDirectory = new Button("Add") {
    graphic = FxUtils.newImageView(IconResource.PLUS)
    onAction = handle {
      _addFastaDirectory()
    }
  }

  /* parsing rules List */
  val parsingRulesLablel = new BoldLabel("Parsing Rules: ", upperCase = false)
  val localRules = new ArrayBuffer[Rules]()
  val addRuleButton = new Button("Add") {
    graphic = FxUtils.newImageView(IconResource.PLUS)
    onAction = handle {
      _addRule()
    }
  }
  private val EMPTY_FASTA_DIR = "Fasta directories are empty. You should have at least one local Fasta directory. Default value will be reset."
  val emptyFastaWarningLabel = new Label() {
    visible = false
    text = EMPTY_FASTA_DIR
    style = TextStyle.RED_ITALIC
  }
  private val EMPTY_PARSING_RULE = "Parsing rules are empty. You should have at least one parsing rule. Default values will be reset."
  val emptyRulesWarningLabel = new Label() {
    visible = false
    text = EMPTY_PARSING_RULE
    style = TextStyle.RED_ITALIC
  }

  /*
   * ****** *
   * LAYOUT *
   * ****** *
   */

  Seq(localFastaDirLablel, defaultProteinAccessionLabel).foreach(_.minHeight = 25)
  Seq(localFastaDirLablel, defaultProteinAccessionLabel, parsingRulesLablel).foreach(_.minWidth = 150)

  //VBox & HBox spacing

  private val V_SPACING = 10
  private val H_SPACING = 5

  /* parsing rules panel */
  val parsingRules = new TitledBorderPane(
    title = "Parsing Rules",
    titleTooltip = "Extract accession numbers from Fasta files",
    contentNode = new VBox {
      spacing = V_SPACING
      children = List(
        new VBox {
          spacing = 0.5
          children = List(emptyFastaWarningLabel,
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
    /* local Fasta directories  */
    val initFastaDirs = parsingRule.localFastaDirectories
    if (initFastaDirs.isEmpty) {
      _addFastaDirectory()
      warningFastaDir
    } else {
      initFastaDirs.foreach { v => _addFastaDirectory(v) }
      warningFastaDir
    }

    /* parsing rules */
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
    localFastaDirs += new FastaDirectory(parentStage = stage,
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
    localRules += new Rules(parentStage = stage,
      onDeleteAction = _onRulesDelete,
      name = name, fastaName = fastaName,
      fastaVersion = fastaVersion,
      proteinAccession = proteinAccession)
    RulesBox.children = localRules
    warningParsingRules
  }
  val mountPointsWithDisableNote = new VBox {
    spacing = 15
    children = Seq(parsingRules)
  }
  alignment = Pos.Center
  alignmentInParent = Pos.Center
  spacing = 5
  children = List(mountPointsWithDisableNote)

  /** get GUI information to set default protein accession **/
  private def _getDefaultProteinAccesion(): Option[String] = {
    Some(defaultProteinAccession)
  }

  /** get GUI information to create list Fasta data directories **/
  private def _getFastaDirectories(fastaDirArray: ArrayBuffer[FastaDirectory]): ListBuffer[String] = {
    (fastaDirArray.view.map(_.getValue)).to[ListBuffer]
  }

  /** get GUI information  to create a parsing rule  **/
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

  private def _toParsingRule() =
    ParsingRule(_getFastaDirectories(localFastaDirs),
      _getListPasrsingRules(localRules),
      _getDefaultProteinAccesion())

  /** save all changed parameters **/
  def save() {
    val newConfig = Future {
      val newParsingRules = _toParsingRule()
      parsigRuleFile.write(newParsingRules)
    }
    newConfig onFailure {
      case (t) => logger.error(s"An error has occured: ${t.getMessage}")
    }
  }

  /* get properties */
  def getProperties: String = {
    s"Specific module:\n\tSequence Repository: Parsing Rules: ${localRules.size} rule(s)"
  }
  /* check Fields */

  def warningFastaDir() {
    if (localFastaDirs.isEmpty) emptyFastaWarningLabel.visible = true else emptyFastaWarningLabel.visible = false
  }
  def warningParsingRules() {
    if (localRules.isEmpty) emptyRulesWarningLabel.visible = true else emptyRulesWarningLabel.visible = false
  }
}

// a Fasta directory 

class FastaDirectory(
  parentStage: Stage,
  onDeleteAction: (FastaDirectory) => Unit,
  key: String = "",
  value: String = "") extends HBox {

  val thisFastaDir = this

  /* component */

  val valueField = new TextField {
    prefWidth <== thisFastaDir.width
    promptText = "Full path"
    text = value
  }
  val browseButton = new Button("Browse") {
    minWidth = 80
    maxWidth = 80
    graphic = FxUtils.newImageView(IconResource.LOAD)
    onAction = handle {
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
    onAction = handle { onDeleteAction(thisFastaDir) }
  }
  /* Layout */

  spacing = 10
  alignment = Pos.Center
  children = List(valueField, browseButton, removeButton)

  def getValue = valueField.text()

}

/* Rule */

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
    text = "Fill the following properties of Fasta rule to be considered."
    style = TextStyle.RED_ITALIC
    visible = false
  }
  val nameLabel = new Label("Id")
  val nameText = new TextField {
    prefWidth <== thisrule.width
    promptText = "Id"
    text = name
    text.onChange { (_, oldText, newText) =>
      checkForm()
    }
  }

  val fastaNameLabel = new Label("Fasta Pattern")
  val fastaNameText = new TextField {
    prefWidth <== thisrule.width
    promptText = "All files matching this Java Regex will be concerned"
    text = fastaName
    text.onChange { (_, oldText, newText) =>
      checkForm()
    }
  }

  val fastaVersionLabel = new Label("Fasta File Version")
  val fastaVersionText = new TextField {
    prefWidth <== thisrule.width
    promptText = "Java Regex with capturing group for fasta release version string (CASE_INSENSITIVE)"
    text = fastaVersion
    text.onChange { (_, oldText, newText) =>
      checkForm()
    }
  }

  val proteinAccessionLabel = new Label("Accession Parse Rule")
  val proteinAccessionText = new TextField {
    prefWidth <== thisrule.width
    promptText = "Java Regex with capturing group for protein accession"
    text = proteinAccession
    text.onChange { (_, oldText, newText) =>
      checkForm()
    }
  }

  val removeButton = new Button("Remove") {
    minWidth = 80
    maxWidth = 80
    graphic = FxUtils.newImageView(IconResource.TRASH)
    onAction = handle { onDeleteAction(thisrule) }
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
  alignment = Pos.CENTER
  children = List(ruleBox, removeButton)
  def checkForm() {
    if (ScalaUtils.isEmpty(nameText.getText) || ScalaUtils.isEmpty(fastaNameText.getText)
      || ScalaUtils.isEmpty(fastaVersionText.getText) || ScalaUtils.isEmpty(proteinAccessionText.getText)) {
      warningDatalabel.visible = true
    } else {
      warningDatalabel.visible = false
    }
  }
  def getName = nameText.text()
  def getFastaName = fastaNameText.text()
  def getFastaVersion = fastaVersionText.text()
  def getproteinAccession = proteinAccessionText.text()

}


