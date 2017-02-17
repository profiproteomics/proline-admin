package fr.proline.admin.gui.component.wizard

import com.typesafe.scalalogging.LazyLogging
import scalafx.Includes._
import scalafx.geometry.Pos
import scalafx.scene.control.Button
import scalafx.scene.control.Label
import scalafx.scene.control.TextField
import scalafx.scene.layout.HBox
import scalafx.scene.layout.Priority
import scalafx.scene.layout.StackPane
import scalafx.scene.layout.VBox
import scalafx.stage.Stage
import fr.proline.admin.gui.QuickStart
import fr.proline.admin.gui.process.ProlineAdminConnection
import fr.proline.admin.gui.process.config.AdminConfigFile
import fr.proline.admin.gui.util.ShowPopupWindow
import fr.profi.util.scala.ScalaUtils._
import fr.profi.util.scala.ScalaUtils
import fr.profi.util.scalafx.BoldLabel
import fr.profi.util.scalafx.ScalaFxUtils
import fr.profi.util.scalafx.ScalaFxUtils.TextStyle._
import fr.proline.admin.gui.QuickStart
import scalafx.stage.FileChooser
import scalafx.stage.FileChooser.ExtensionFilter
import fr.profi.util.scalafx.TitledBorderPane
import javafx.scene.control.Tooltip
import fr.proline.admin.gui.util.FxUtils
import scalafx.scene.control.Button.sfxButton2jfx
import scalafx.scene.control.TextField.sfxTextField2jfx
import fr.proline.admin.gui.component.configuration.file._
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

/**
 *********************************************************
 * Step 1 : panel to add configurations files
 *********************************************************
 */
class ProlineConfigFilesPanelQStart(onAdminConfigChange: AdminConfigFile => Unit = null) extends VBox with IConfigFilesPanel with LazyLogging {

  /* Proline config */

  private var serverConfigPathInAdminConfig: String = _
  private var pwxConfigPathInAdminConfig: String = _
  private var adminConfigFile: AdminConfigFile = _
  adminConfigFile = QuickStart.getAdminConfigFile().getOrElse(null)
  QuickStart.panelState = "panelConfig"
  private val V_SPACING = 10

  /*
   * ********** *
   * COMPONENTS *
   * ********** *
   */

  /* Proline Admin configuration file */

  val adminConfigLabel = new HBox {
    content = List(
      new Label("Full path to "),
      new BoldLabel("ProlineAdmin", upperCase = false),
      new Label(" configuration file application.conf :"))
  }
  val adminConfigField = new TextField {
    if (QuickStart.adminConfPath != null) text = QuickStart.adminConfPath
    else
      text.onChange { (_, oldText, newText) =>
        updateAdminConf(newText)
      }
  }
  adminConfigField.setTooltip(new Tooltip("full path to proline admin configuration file."));
  val adminConfigBrowse = new Button("Browse...") {
    onAction = handle {
      _browseAdminConfigFile()
    }
  }

  /* Proline Server configuration file */

  val serverConfigLabel = new HBox {
    content = List(
      new Label("Full path to "),
      new BoldLabel("Proline server", upperCase = false),
      new Label(" configuration file application.conf :"))
  }
  val serverConfigField = new TextField() {
    if (QuickStart.serverConfPath != null) text = QuickStart.serverConfPath
    text.onChange { (_, oldText, newText) =>
      updateServerConf(newText)
    }
  }
  serverConfigField.setTooltip(new Tooltip("full path to proline server configuration file."));
  val serverConfigBrowse = new Button("Browse...") {
    onAction = handle {
      _browseServerConfigFile
    }
  }
  /* Sequence Repository configuration file will be selected automatically */
  val seqReposConfigLabel = new HBox {
    content = List(
      new Label("Full path to "),
      new BoldLabel("Sequence Repository", upperCase = false),
      new Label(" configuration file application.conf ( optional ):"))
  }
  val seqReposConfigField = new TextField() {
    if (QuickStart.seqRepoConfPath != null) text = QuickStart.seqRepoConfPath
    text.onChange { (_, oldText, newText) =>
      updateSeqReposConf(newText)
    }
  }
  seqReposConfigField.setTooltip(new Tooltip("full path to sequence repository configuration file."));
  val pwxConfigBrowse = new Button("Browse...") {
    onAction = handle {
      _browseSequenceRepositoryFile
    }
  }

  val seqReposConfigWarningLabel = new Label {}

  /* Select data directory */

  val dataDirectoryLabel = new HBox {
    content = List(
      new Label("Full path to "),
      new BoldLabel("postgreSQL data directory", upperCase = false),
      new Label(" to optimize yout database server ( optional ) : "))
  }
  val dataDirectoryField = new TextField() {
    //initialise data directory from 
    if (getPgData("PG_DATA") != null) {
      QuickStart.postgresqlDataDir = getPgData("PG_DATA")
    }
    if (QuickStart.postgresqlDataDir != null) text = QuickStart.postgresqlDataDir
    text.onChange { (_, oldText, newText) =>
      QuickStart.postgresqlDataDir = ""
      updateDataDirectoryPath(newText)
      // ButtonsPanelQStart.prolineConfigFilesPanel.setDataDirectoryPath(QuickStart.postgresqlDataDir)
    }
  }
  dataDirectoryField.setPromptText("Example : ..\\PostgreSQL\\9.x\\data")
  dataDirectoryField.setTooltip(new Tooltip("full path to postgreSQL data Directory .Example : ..\\PostgreSQL\\9.x\\data"));
  val dataDirectoryBrowse = new Button("Browse...") {
    onAction = handle {

    }
  }
  dataDirectoryBrowse.setTooltip(new Tooltip("Browse postgreSQL data Directory. "));

  /*
   * ****** *
   * LAYOUT *
   * ****** *
   */

  Seq(
    adminConfigField,
    serverConfigField,
    seqReposConfigField,
    dataDirectoryField).foreach { f =>
      f.hgrow = Priority.Always
    }
  val disableNoteLabel = new Label() {
    text = "Proline server and Proline admin configuration file cannot be empty.\n" +
      """ """
    style = "-fx-font-style: italic;-fx-font-weigth: bold;"
    visible = false
  }
  /* Organize and render */
  val configurationsFiles = new TitledBorderPane(

    title = "Step 1 : select proline configuration file",
    contentNode = new VBox {
      minWidth = 360
      prefWidth = 360
      spacing = 5

      content = Seq(
        disableNoteLabel,
        adminConfigLabel,
        new HBox {
          spacing = 5
          content = Seq(adminConfigField, adminConfigBrowse)
        },
        ScalaFxUtils.newVSpacer(minH = 10),

        serverConfigLabel,
        new HBox {
          spacing = 5
          content = Seq(serverConfigField, serverConfigBrowse)
        },
        ScalaFxUtils.newVSpacer(minH = 10),
        seqReposConfigLabel,
        new HBox {
          spacing = 5
          content = Seq(seqReposConfigField, pwxConfigBrowse)
        },
        ScalaFxUtils.newVSpacer(minH = 10),
        ScalaFxUtils.newVSpacer(minH = 10),
        ScalaFxUtils.newVSpacer(minH = 10),
        ScalaFxUtils.newVSpacer(minH = 10),
        ScalaFxUtils.newVSpacer(minH = 10),
        ScalaFxUtils.newVSpacer(minH = 10))
    })
  alignment = Pos.Center
  alignmentInParent = Pos.Center
  spacing = 4 * V_SPACING
  content = List(
    ScalaFxUtils.newVSpacer(minH = 1),
    configurationsFiles)

  /*
   * ******** *
   * FEATURES *
   * ******** *
   */

  def showIn(stage: Stage): String = {
    try {
      val fc = new FileChooser {

        title = "Select Proline configuration file"
        extensionFilters.add(new ExtensionFilter("Configuration files", "*.conf"))
        extensionFilters.add(new ExtensionFilter("All files", "*.*"))
      }

      val confPath = fc.showOpenDialog(stage).getPath()

      /* Validate path */
      require(confPath matches """.+\.conf$""", "invalid path for configuration file")

      confPath

    } catch {

      case jfx: java.lang.NullPointerException => {
        logger.debug("configuration file selected.")
        null
      }

      case t: Throwable => {
        throw t
        null
      }
    }
  }
  // Browse ProlineAdmin configuration file: set global variable and update field 

  private def _browseAdminConfigFile() {
    ProlineConfigFileChooserWizard.setForProlineAdminConf(adminConfigField.text())
    val filePath = ProlineConfigFileChooserWizard.showIn(QuickStart.stage)
    if (filePath != null) adminConfigField.text = filePath
  }

  // Browse Proline server configuration file: set global variable and update field

  private def _browseServerConfigFile() {
    ProlineConfigFileChooserWizard.setForProlineServerConf(serverConfigField.text())
    val filePath = ProlineConfigFileChooserWizard.showIn(QuickStart.stage)
    if (filePath != null) serverConfigField.text = filePath
  }

  // browse Sequence Repository configuration file: set global variable and update field 

  private def _browseSequenceRepositoryFile() {
    ProlineConfigFileChooserWizard.setForProlineServerConf(seqReposConfigField.text())
    val filePath = ProlineConfigFileChooserWizard.showIn(QuickStart.stage)
    if (filePath != null) seqReposConfigField.text = filePath
  }

  // browse data directory of PostgreSQL

  private def _browseDataDir() {
    val file = FxUtils.browseDirectoryWizard(
      dcTitle = "Select PostgreSQL data directory",
      dcInitialDir = dataDirectoryField.text())

    if (file != null) {
      val newPath = file.getPath()
      dataDirectoryField.text = newPath
      QuickStart.postgresqlDataDir = newPath
    }
  }

  // update adminConf  global variables

  def updateAdminConf(newText: String) {
    if (isEmpty(newText) == false) {
      QuickStart.adminConfPath = normalizeFilePath(newText)

    }
  }
  // update server global variables  

  def updateServerConf(newText: String) {
    if (isEmpty(newText) == false) {
      QuickStart.serverConfPath = normalizeFilePath(newText)
    }
  }
  // update sequence repository global variable

  def updateSeqReposConf(newText: String) {
    if (isEmpty(newText) == false) {
      QuickStart.seqRepoConfPath = normalizeFilePath(newText)
    }
  }
  // update data directory global variable 
  def updateDataDirectoryPath(newText: String) {
    if (isEmpty(newText) == false) {
      QuickStart.postgresqlDataDir = normalizeFilePath(newText)
    }
  }

  // normalize file path 

  def normalizeFilePath(path: String): String = {
 //      return Paths.get(path).normalize().toString()
    var filePath = path.replaceAll("\\\\", "/")
    filePath = new File(path).getCanonicalPath()
    return (filePath)
  }

  // update textfields 

  def setAdminfield(text: String) {
    adminConfigField.setText(text)
  }
  def setServerfield(text: String) {
    serverConfigField.setText(text)
  }
  def setSeqfield(text: String) {
    seqReposConfigField.setText(text)
  }
  // get environment var PG_DATA 
  def getPgData(env: String): String = {
    return System.getenv(env)
  }
  def saveForm() {}
  // valid parameters before to pass to next step
  def validStep() {
    if ((QuickStart.adminConfPath != null) && (!QuickStart.adminConfPath.isEmpty) && (QuickStart.serverConfPath != null) && (!QuickStart.serverConfPath.isEmpty)) {
      disableNoteLabel.visible = false
    } else {
      disableNoteLabel.visible = true
    }
  }
  // Getters/Setters for textFields 

  def getProlineAdminConfFile(): String = adminConfigField.text()
  def setProlineAdminConfFile(newPath: String) { adminConfigField.text = newPath }
  def getProlineServerConfFile(): String = serverConfigField.text()
  def setProlineServerConfFile(newPath: String) { serverConfigField.text = newPath }
  def setDataDirectoryPath(path: String) { dataDirectoryField.text = path }
  // Check the form
  def checkForm(allowEmptyPaths: Boolean = true): Boolean = Seq(
    (seqReposConfigField, seqReposConfigWarningLabel)).forall { case (f, w) => this.checkFileFromField(f, w, allowEmptyPaths) }

}