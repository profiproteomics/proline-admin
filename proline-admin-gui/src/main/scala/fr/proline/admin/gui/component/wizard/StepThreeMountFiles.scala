package fr.proline.admin.gui.component.wizard

import com.typesafe.scalalogging.LazyLogging

import scala.collection.mutable.ArrayBuffer

import scalafx.Includes._
import scalafx.collections.ObservableBuffer
import scalafx.geometry.Insets
import scalafx.geometry.Pos
import scalafx.scene.Cursor
import scalafx.scene.Cursor.sfxCursor2jfx
import scalafx.scene.control.Button
import scalafx.scene.control.CheckBox
import scalafx.scene.control.ComboBox
import scalafx.scene.control.Hyperlink
import scalafx.scene.control.Label
import scalafx.scene.control.PasswordField
import scalafx.scene.control.TextField
import scalafx.scene.layout.HBox
import scalafx.scene.layout.Priority
import scalafx.scene.layout.StackPane
import scalafx.scene.layout.VBox
import scalafx.stage.Screen
import scalafx.stage.Stage

import fr.proline.admin.gui.IconResource
import fr.proline.admin.gui.QuickStart
import fr.proline.admin.gui.process.DatabaseConnection
import fr.proline.admin.gui.process.ProlineAdminConnection
import fr.proline.admin.gui.process.config._
import fr.proline.admin.gui.util.FxUtils
import fr.proline.admin.gui.util.GetConfirmation
import fr.proline.repository.DriverType

import fr.profi.util.scala.ScalaUtils.doubleBackSlashes
import fr.profi.util.scala.ScalaUtils.isEmpty
import fr.profi.util.scala.ScalaUtils.stringOpt2string
import fr.profi.util.scalafx.BoldLabel
import fr.profi.util.scalafx.FileBrowsing
import fr.profi.util.scalafx.NumericTextField
import fr.profi.util.scalafx.ScalaFxUtils
import fr.profi.util.scalafx.ScalaFxUtils._
import fr.profi.util.scalafx.TitledBorderPane

/**
 * Create a modal window to edit Proline configuration's file.
 */
class ProlineMountFiles extends VBox with LazyLogging {

  maxHeight = Screen.primary.visualBounds.height - 20 // 

  /* Configuration files */

  private val adminConfigFile = new AdminConfigFile(QuickStart.adminConfPath)
  private val adminConfigOpt = adminConfigFile.read()
  require(adminConfigOpt.isDefined, "admin config is undefined")
  private val adminConfig = adminConfigOpt.get

  private val serverConfigFileOpt =
    if (isEmpty(QuickStart.serverConfPath)) None
    else Option(new ServerConfigFile(QuickStart.serverConfPath))

  private val serverConfigOpt = serverConfigFileOpt.map(_.read()).flatten

  /*
   * ********** *
   * COMPONENTS *
   * ********** *
   */

  /* Mount points */

  val disableMpNoteLabel = new Label() {
    text = "choose a validated Proline server configuration file to enable mount points setup.\n" +
      """See step 1."""
    style = "-fx-font-style: italic;-fx-font-weigth: bold;"
    visible = false
  }

  val rawFilesMountPoints = ArrayBuffer[MountPointPanelWizard]()
  val rawFilesMpLabel = new BoldLabel("Raw files path: ", upperCase = false)
  val addRawFilesMpButton = new Button("Add") {
    onAction = handle { _addRawFilesMountPoint() }
  }
  val rawFilesMpBox = new VBox { spacing = 10 }

  val mzdbFilesMountPoints = ArrayBuffer[MountPointPanelWizard]()
  val mzdbFilesMpLabel = new BoldLabel("mzDB files path:", upperCase = false)
  val addMzdbFilesMpButton = new Button("Add") {
    onAction = handle { _addMzdbFilesMountPoint() }
  }
  val mzdbFilesMpBox = new VBox { spacing = 10 }

  val resultFilesMountPoints = ArrayBuffer[MountPointPanelWizard]()
  val resultFilesMpLabel = new BoldLabel("Result files path:", upperCase = false)
  val addResultFilesMpButton = new Button("Add") {
    onAction = handle { _addResultFilesMountPoint() }
  }
  val resultFilesMpBox = new VBox { spacing = 10 }
  // Warning 
  val warningAboutExitText = "WARNING: Are you sure  to save and exit ? "
  /*
   * ****** *
   * LAYOUT *
   * ****** *
   */

  Seq(
    rawFilesMpLabel, mzdbFilesMpLabel, resultFilesMpLabel).foreach(_.minHeight = 25)

  //VBox & HBox spacing
  private val V_SPACING = 10
  private val H_SPACING = 5

  /* DB connection */
  //Set text- and password textfields at the same place in UI

  /* Mount points */
  val mountPointsSettings = new TitledBorderPane(
    title = "Step 3: define your file locations",
    titleTooltip = "Mount points as defined in Proline server configuration",
    contentNode = new VBox {
      spacing = 2 * V_SPACING
      content = List(
        new HBox {
          spacing = H_SPACING
          content = List(rawFilesMpLabel, addRawFilesMpButton)
        },
        rawFilesMpBox,

        new HBox {
          spacing = H_SPACING
          content = List(mzdbFilesMpLabel, addMzdbFilesMpButton)
        },
        mzdbFilesMpBox,

        new HBox {
          spacing = H_SPACING
          content = List(resultFilesMpLabel, addResultFilesMpButton)
        },
        resultFilesMpBox)
    })

  val mountPointsWithDisableNote = new VBox {
    spacing = 20
    content = List(disableMpNoteLabel, mountPointsSettings)
  }

  /* VBox layout and content */
  alignment = Pos.Center
  alignmentInParent = Pos.Center
  //  padding = Insets(25, 20, 15, 30)
  spacing = 4 * V_SPACING
  content = List(
    //prolineSettings,

    mountPointsWithDisableNote)
  prefWidth <== QuickStart.stage.width

  /*
   * ************* *
   * INIT. CONTENT *
   * ************* *
   */
  def isPrompt(str: String): Boolean = str matches """<.*>"""

  /* Common settings */
  val driverTypeOpt = adminConfig.driverType
  //  if (driverTypeOpt.isDefined) driverTypeBox.selectionModel().select(driverTypeOpt.get)

  /* Mount points */
  if (serverConfigOpt.isEmpty) {

    /* Disable mount points if server config is undefined */
    disableMpNoteLabel.visible = true
    mountPointsSettings.disable = true

    //Don't screw up layout
    disableMpNoteLabel.minHeight = 34
    disableMpNoteLabel.maxHeight = 34

  } else {

    //Don't screw up layout
    disableMpNoteLabel.minHeight = 0
    disableMpNoteLabel.maxHeight = 0

    /* Fill fields */
    val serverConfig = serverConfigOpt.get

    val rawMp = serverConfig.rawFilesMountPoints
    if (rawMp.isEmpty) _addRawFilesMountPoint()
    else rawMp.foreach { case (k, v) => _addRawFilesMountPoint(k, v) }

    val mzdbMp = serverConfig.mzdbFilesMountPoints
    if (mzdbMp.isEmpty) _addMzdbFilesMountPoint()
    else mzdbMp.foreach { case (k, v) => _addMzdbFilesMountPoint(k, v) }

    val resultMp = serverConfig.resultFilesMountPoints
    if (resultMp.isEmpty) _addResultFilesMountPoint()
    else resultMp.foreach { case (k, v) => _addResultFilesMountPoint(k, v) }
  }

  /**
   * ******** *
   * FEATURES *
   * ******** *
   */

  /** Get GUI information to create new AdminConfig object **/
  private def _getTextFieldValue(txtField: TextField): Option[String] = textField2StringOpt(txtField, allowEmpty = false)

  private def _toAdminConfig() = AdminConfig(
    filePath = adminConfig.filePath,
    serverConfigFilePath = Option(QuickStart.serverConfPath).map(doubleBackSlashes), //FIXME: windows-specific
    pwxConfigFilePath = adminConfig.pwxConfigFilePath.map(doubleBackSlashes), //FIXME: windows-specific
    pgsqlDataDir = Option(QuickStart.postgresqlDataDir).map(doubleBackSlashes), //FIXME: windows-specific
    seqRepoConfigFilePath = Option(QuickStart.seqRepoConfPath).map(doubleBackSlashes),
    driverType = adminConfig.driverType, //
    prolineDataDir = Option(QuickStart.postgresqlDataDir).map(doubleBackSlashes), //FIXME: windows-specific
    dbUserName = Option(QuickStart.userName),
    dbPassword = Option(QuickStart.passwordUser),
    dbHost = Option(QuickStart.hostNameUser),
    dbPort = Option(QuickStart.port) //FIXME
    )

  /** Get GUI information to create new ServerConfig object **/
  private def _getMountPointsMap(mpArray: ArrayBuffer[MountPointPanelWizard]): Map[String, String] = {
    (
      for (
        mp <- mpArray.view;
        (k, v) = (mp.getKey, mp.getValue);
        if k.isEmpty == false && v.isEmpty == false
      ) yield k -> v //doubleQuoted(v)
      ).toMap
  }

  private def _toServerConfig() = ServerConfig(
    //serverConfFilePath = serverConfigOpt.get.filePath,
    rawFilesMountPoints = _getMountPointsMap(rawFilesMountPoints),
    mzdbFilesMountPoints = _getMountPointsMap(mzdbFilesMountPoints),
    resultFilesMountPoints = _getMountPointsMap(resultFilesMountPoints))

  /** Test connection with DB with provided parameters **/
  private def _testDbConnection(
    adminConfig: AdminConfig,
    showSuccessPopup: Boolean = true,
    showFailurePopup: Boolean = true): Boolean = { //return connectionEstablished

    DatabaseConnection.testDbConnection(adminConfig, showSuccessPopup, showFailurePopup)
  }

  /** Add stuff to define another raw_files mount point **/
  private def _addRawFilesMountPoint(
    key: String = "",
    value: String = "") {

    def _onRawFileMpDelete(mp: MountPointPanelWizard): Unit = {
      rawFilesMountPoints -= mp
      rawFilesMpBox.content = rawFilesMountPoints
    }

    rawFilesMountPoints += new MountPointPanelWizard(
      key = key,
      value = value,
      onDeleteAction = _onRawFileMpDelete,
      parentStage = QuickStart.stage)
    rawFilesMpBox.content = rawFilesMountPoints
  }

  /** Add stuff to define another mzdb_files mount point **/
  private def _addMzdbFilesMountPoint(
    key: String = "",
    value: String = "") {

    def _onMzdbFileMpDelete(mp: MountPointPanelWizard): Unit = {
      mzdbFilesMountPoints -= mp
      mzdbFilesMpBox.content = mzdbFilesMountPoints
    }

    mzdbFilesMountPoints += new MountPointPanelWizard(
      key = key,
      value = value,
      onDeleteAction = _onMzdbFileMpDelete,
      parentStage = QuickStart.stage)
    mzdbFilesMpBox.content = mzdbFilesMountPoints
  }

  /** Add stuff to define another result_files mount point **/
  private def _addResultFilesMountPoint(
    key: String = "",
    value: String = "") {

    def _onResultFileMpDelete(mp: MountPointPanelWizard): Unit = {
      resultFilesMountPoints -= mp
      resultFilesMpBox.content = resultFilesMountPoints
    }

    resultFilesMountPoints += new MountPointPanelWizard(
      key = key,
      value = value,
      onDeleteAction = _onResultFileMpDelete,
      parentStage = QuickStart.stage)
    resultFilesMpBox.content = resultFilesMountPoints
  }

  /** Check form **/
  private def _checkForm(): String = {

    //TODO: finish me

    val errString = new StringBuilder()

    errString.result()
  }

  /** Action run when "Save" button is pressed **/

  def checkForm(): Boolean = {
    //TODO
    true
  }

  def saveForm() {

    //TODO

    QuickStart.stage.scene().setCursor(Cursor.WAIT)

    var continue: Boolean = true

    /* If PWX config is undefined, warn user that changes will not be written in it */

    if (continue) {

      /* New AdminConfig*/
      val newAdminConfig = _toAdminConfig()
      adminConfigFile.write(newAdminConfig)

      /* edit seqRepository  .conf is optionnal */
      if ((QuickStart.seqRepoConfPath != null) && (!QuickStart.seqRepoConfPath.isEmpty)) {
        val seqConfigFile = new AdminConfigFile(QuickStart.seqRepoConfPath)
        val seqConfigOpt = seqConfigFile.read()
        require(seqConfigOpt.isDefined, "SeqRepository config file is undefined")
        val seqConfig = seqConfigOpt.get
        seqConfigFile.write(newAdminConfig)
      }
      if (serverConfigOpt.isDefined) {
        
        /* New ServerConfig */
        
        val newServerConfig = _toServerConfig()
        serverConfigFileOpt.get.write(newServerConfig, newAdminConfig)
        
      }
      
      /* Test connection to database */
      val connectionEstablished = _testDbConnection(newAdminConfig, false, false)

      if (connectionEstablished) {

        /* Log and close dialog if config is valid */

        ShowConfirmWindow(
          wTitle = "Warning",
          wText = warningAboutExitText,
          wParent = Option(QuickStart.stage))
          
      } else {

        /* If DB can't be reached, allow to save configuration anyway */
          val isConfirmed = GetConfirmation(
          title = "Invalid configuration",
          text = "The connection to the database can't be established with these settings.\n" +
            "Do you want to save this configuration anyway?")

        if (isConfirmed) {
          QuickStart.stage.close()
          ProlineAdminConnection.loadProlineConf(verbose = true)
        }
      }
      QuickStart.stage.scene().setCursor(Cursor.DEFAULT)
    }
  }
}

/**
 * Build 1 mount point panel
 */
class MountPointPanelWizard(
  parentStage: Stage,
  onDeleteAction: (MountPointPanelWizard) => Unit,
  key: String = "",
  value: String = "") extends HBox {

  val thisMountPoint = this

  /* Components */
  val keyField = new TextField {
    minWidth = 144
    maxWidth = 144
    promptText = "Alias"
    text = key
  }
  val equalLabel = new Label("=") {
    minWidth = 15
    maxWidth = 15
  }
  val valueField = new TextField {
    prefWidth <== thisMountPoint.width
    promptText = "Full path"
    text = value
  }
  val browseButton = new Button("Browse") {
    minWidth = 56
    maxWidth = 56
    onAction = handle {
      val dir = FileBrowsing.browseDirectory(
        dcTitle = "Select mount point directory",
        dcInitialDir = valueField.text(),
        dcInitOwner = parentStage)

      if (dir != null) valueField.text = dir.getAbsolutePath()
    }
  }
  val removeButton = new Button("Remove") {
    minWidth = 60
    maxWidth = 60
    onAction = handle { onDeleteAction(thisMountPoint) }
  }
  /* Layout */
  spacing = 10
  alignment = Pos.Center
  content = List(keyField, equalLabel, valueField, browseButton, removeButton)

  /* Features */
  def getKey = keyField.text()
  def getValue = valueField.text()
}