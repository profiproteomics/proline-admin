package fr.proline.admin.gui.component.dialog

import com.typesafe.scalalogging.slf4j.Logging

import scala.collection.mutable.ArrayBuffer

import scalafx.Includes._
import scalafx.collections.ObservableBuffer
import scalafx.geometry.Insets
import scalafx.geometry.Pos
import scalafx.scene.Cursor
import scalafx.scene.Scene
import scalafx.scene.control.Button
import scalafx.scene.control.CheckBox
import scalafx.scene.control.ComboBox
import scalafx.scene.control.Hyperlink
import scalafx.scene.control.Label
import scalafx.scene.control.PasswordField
import scalafx.scene.control.ScrollPane
import scalafx.scene.control.ScrollPane.ScrollBarPolicy
import scalafx.scene.control.TextField
import scalafx.scene.input.KeyEvent
import scalafx.scene.layout.HBox
import scalafx.scene.layout.Priority
import scalafx.scene.layout.StackPane
import scalafx.scene.layout.VBox
import scalafx.stage.Modality
import scalafx.stage.Stage

import fr.profi.util.scala.ScalaUtils._
import fr.profi.util.scalafx.BoldLabel
import fr.profi.util.scalafx.NumericTextField
import fr.profi.util.scalafx.ScalaFxUtils._
import fr.profi.util.scalafx.TitledBorderPane
import fr.proline.admin.gui.Main
import fr.proline.admin.gui.process._
import fr.proline.admin.gui.process.config._
import fr.proline.admin.gui.util.FxUtils
import fr.proline.repository.DriverType

/**
 * Create a modal window to edit Proline configuration's file.
 */
class ProlineConfigForm extends Stage with Logging {

  val formEditor = this

  /* Stage's properties */
  title = s"Proline configuration editor -- ${Main.adminConfPath}"
  initModality(Modality.WINDOW_MODAL)
  initOwner(Main.stage)
  width = 560
  maxHeight = 816

  /* Configuration files */
  //this stage can't be opened if adminConfigFile is undefined in Main
  private val adminConfigFile = new AdminConfigFile(Main.adminConfPath)
  private val adminConfigOpt = adminConfigFile.read()
  require(adminConfigOpt.isDefined, "admin config is undefined")
  private val adminConfig = adminConfigOpt.get

  private val serverConfigFileOpt =
    if (isEmpty(Main.serverConfPath)) None //None or Some(null/empty String)
    else Option(new ServerConfigFile(Main.serverConfPath))

  private val serverConfigOpt = serverConfigFileOpt.map(_.read()).flatten


  /**
   * ********** *
   * COMPONENTS *
   * ********** *
   */

  /* Proline settings */
  val driverTypeLabel = new Label("Driver type :") { disable = true }
  //  val driverTypeBox = new ComboBox[String] {
  //    items = ObservableBuffer(
  //      "postgresql",
  //      "h2",
  //      "sqlite"
  //    )
  //    disable = true
  //  }
  
  val driverTypeBox = new ComboBox[DriverType] {
    items = ObservableBuffer(
      DriverType.POSTGRESQL,
      DriverType.H2,
      DriverType.SQLITE
    )
    disable = true
  }

  val dataDirLabel = new Label("Data directory :")
  val dataDirField = new TextField()
  val dataDirBrowse = new Button("Browse") {
    onAction = handle { _browseDataDir() }
  }

  /* DB connection */
  val userNameLabel = new Label("User name :")
  val userNameField = new TextField()

  val pwdLabel = new Label("Password :")
  val showPwdBox = new CheckBox("Show password") {
    selected = false
    vgrow = Priority.Always
    minWidth = 112
    maxWidth = 112
  }
  val passwordPWDField = new PasswordField() {
    visible <== !showPwdBox.selected
  }
  val passwordTextField = new TextField() {
    text <==> passwordPWDField.text
    promptText <==> passwordPWDField.promptText
    visible <== !passwordPWDField.visible
  }

  val hostNameLabel = new Label("Host name :")
  val hostNameField = new TextField()

  val portLabel = new Label("Port :")
  val portField = new NumericTextField()

  val testConnectionHyperlink = new Hyperlink("Test connection") {
    onAction = handle { _testDbConnection(_toAdminConfig()) }
  }

  /* Mount points */
  val disableMpNoteLabel = new Label() {
    text = "Proline server configuration file must be provided to enable mount points setup.\n" +
      """See menu "Select configuration files"."""
    style = "-fx-font-style: italic;-fx-font-weigth: bold;"
    visible = false
  }

  val rawFilesMountPoints = ArrayBuffer[MountPointPanel]()
  val rawFilesMpLabel = new BoldLabel("RAW files :", upperCase = false)
  val addRawFilesMpButton = new Button("Add") {
    onAction = handle { _addRawFilesMountPoint() }
  }
  val rawFilesMpBox = new VBox { spacing = 10 }

  val mzdbFilesMountPoints = ArrayBuffer[MountPointPanel]()
  val mzdbFilesMpLabel = new BoldLabel("mzDB files :", upperCase = false)
  val addMzdbFilesMpButton = new Button("Add") {
    onAction = handle { _addMzdbFilesMountPoint() }
  }
  val mzdbFilesMpBox = new VBox { spacing = 10 }

  val resultFilesMountPoints = ArrayBuffer[MountPointPanel]()
  val resultFilesMpLabel = new BoldLabel("Result files :", upperCase = false)
  val addResultFilesMpButton = new Button("Add") {
    onAction = handle { _addResultFilesMountPoint() }
  }
  val resultFilesMpBox = new VBox { spacing = 10 }

  /* "Save" and "Cancel" buttons */
  val saveButton = new Button("Save") {
    onAction = handle { _onSavePressed() }
  }
  val cancelButton = new Button("Cancel") {
    onAction = handle { formEditor.close() }
  }

  /**
   * ****** *
   * LAYOUT *
   * ****** *
   */

  /* Size & Resize properties */
  // Labels' width
  Seq(
    driverTypeLabel, dataDirLabel,
    userNameLabel, pwdLabel, hostNameLabel, portLabel
  //rawFilesMpLabel, mzdbFilesMpLabel, resultFilesMpLabel
  ).foreach(_.minWidth = 88)

  Seq(
    rawFilesMpLabel, mzdbFilesMpLabel, resultFilesMpLabel
  ).foreach(_.minHeight = 25)

  // Buttons' width
  Seq(dataDirBrowse, saveButton, cancelButton).foreach { b =>
    b.minWidth = 55
    b.maxWidth = 55
  }

  // Make all text fields grow the max. they can
  Seq(
    driverTypeBox, dataDirField,
    userNameField, passwordPWDField, passwordTextField, hostNameField, portField
  ).foreach { node =>
      node.minWidth = 150
      node.prefWidth <== formEditor.width
    }

  //VBox & HBox spacing
  private val V_SPACING = 10
  private val H_SPACING = 5

  /* Proline settings */
  val prolineSettings = new TitledBorderPane(
    titleString = "Proline",
    contentNode = new VBox {
      padding = Insets(5)
      //      padding = Insets(5, 5, 4 * V_SPACING, 5)
      spacing = V_SPACING
      content = List(
        new HBox {
          spacing = H_SPACING
          content = List(driverTypeLabel, driverTypeBox)
        },
        new HBox {
          spacing = H_SPACING
          content = List(dataDirLabel, dataDirField, dataDirBrowse)
        }
      )
    }
  )

  /* DB connection */
  //Set text- and password textfields at the same place in UI
  val dbPwdPane = new StackPane {
    alignmentInParent = Pos.BottomLeft
    content = List(passwordPWDField, passwordTextField)
  }

  val dbConnectionSettings = new TitledBorderPane(
    titleString = "Database connection",
    contentNode = new VBox {
      padding = Insets(5)
      spacing = V_SPACING
      alignment = Pos.BaselineRight
      content = List(
        new HBox {
          spacing = H_SPACING
          content = List(userNameLabel, userNameField)
        },
        new HBox {
          spacing = H_SPACING
          content = List(pwdLabel, dbPwdPane, showPwdBox)
        },
        new HBox {
          spacing = H_SPACING
          content = List(hostNameLabel, hostNameField)
        },
        new HBox {
          spacing = H_SPACING
          content = List(portLabel, portField)
        },
        testConnectionHyperlink
      )
    }
  )

  /* Mount points */
  val mountPointsSettings = new TitledBorderPane(
    titleString = "Mount points",
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
        resultFilesMpBox
      )
    }
  )

  val mountPointsWithDisableNote = new VBox {
    spacing = 20
    content = List(disableMpNoteLabel, mountPointsSettings)
  }

  /* "Save" and "Cancel" buttons */
  val buttons = new HBox {
    hgrow = Priority.Always
    alignment = Pos.Center
    spacing = 20
    content = List(saveButton, cancelButton)
  }

  /* Scene */
  scene = new Scene {
    onKeyPressed = (ke: KeyEvent) => {
      closeIfEscapePressed(formEditor, ke)
      fireIfEnterPressed(saveButton, ke)
    }
    
    root = new ScrollPane {
      hbarPolicy = ScrollBarPolicy.AS_NEEDED
      vbarPolicy = ScrollBarPolicy.AS_NEEDED
      val SCROLL_BAR_PUTATIVE_WIDTH = 40

      content = new VBox {
        alignment = Pos.Center
        alignmentInParent = Pos.Center
        padding = Insets(25, 20, 15, 30)

        minWidth <== formEditor.width - SCROLL_BAR_PUTATIVE_WIDTH
        maxWidth <== formEditor.width - SCROLL_BAR_PUTATIVE_WIDTH
        minHeight <== formEditor.height - SCROLL_BAR_PUTATIVE_WIDTH

        content = new VBox {
          spacing = 4 * V_SPACING
          content = List(
            prolineSettings,
            dbConnectionSettings,
            mountPointsWithDisableNote,
            buttons
          )
        }
      }
    }
  }

  /**
   * ************* *
   * INIT. CONTENT *
   * ************* *
   */
  def isPrompt(str: String): Boolean = str matches """<.*>"""

  /* Common settings */
  val driverTypeOpt = adminConfig.driverType
  if (driverTypeOpt.isDefined) driverTypeBox.selectionModel().select(driverTypeOpt.get)

  val dataDir = adminConfig.prolineDataDir //implicit getOrElse("")
  if (isPrompt(dataDir)) dataDirField.promptText = dataDir
  else dataDirField.text = dataDir

  val dbUserName = adminConfig.dbUserName
  if (isPrompt(dbUserName)) userNameField.promptText = dbUserName
  else userNameField.text = dbUserName

  val dbPassword = adminConfig.dbPassword
  if (isPrompt(dbPassword)) passwordPWDField.promptText = dbPassword
  else passwordPWDField.text = dbPassword

  val dbHost = adminConfig.dbHost
  if (isPrompt(dbHost)) hostNameField.promptText = dbHost
  else hostNameField.text = dbHost

  if (adminConfig.dbPort.isDefined) portField.setValue(adminConfig.dbPort.get)

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

  /** Browse Proline data directory **/
  private def _browseDataDir() {
    val dataDir = FxUtils.browseDirectory(
      dcTitle = "Proline data directory",
      dcInitialDir = dataDirField.text()
    )
    if (dataDir != null) dataDirField.text = dataDir.getPath()
  }

  /** Get GUI information to create new AdminConfig object **/
  private def _getValue(txtField: TextField): Option[String] = textField2StringOpt(txtField, allowEmpty = false)

  private def _toAdminConfig() = AdminConfig(
    filePath = adminConfig.filePath,
    serverConfigFilePath = adminConfig.serverConfigFilePath.map(doubleBackSlashes), //FIXME: windows-specific
    pgsqlDataDir = adminConfig.pgsqlDataDir.map(doubleBackSlashes), //FIXME: windows-specific
    driverType = adminConfig.driverType, //immutable in UI
    prolineDataDir = _getValue(dataDirField).map(doubleBackSlashes), //FIXME: windows-specific
    dbUserName = _getValue(userNameField),
    dbPassword = _getValue(passwordTextField),
    dbHost = _getValue(hostNameField),
    dbPort = portField //FIXME
  )

  /** Get GUI information to create new ServerConfig object **/
  def _getMountPointsMap(mpArray: ArrayBuffer[MountPointPanel]): Map[String, String] = {
    (
      for (
        mp <- mpArray.view;
        (k, v) = (mp.getKey, mp.getValue);
        if k.isEmpty == false && v.isEmpty == false
      ) yield k -> v //doubleQuoted(v)

    ).toMap
  }

  private def _toServerConfig() = ServerConfig(
    filePath = serverConfigOpt.get.filePath,
    rawFilesMountPoints = _getMountPointsMap(rawFilesMountPoints),
    mzdbFilesMountPoints = _getMountPointsMap(mzdbFilesMountPoints),
    resultFilesMountPoints = _getMountPointsMap(resultFilesMountPoints)
  )

  /** Test connection with DB with provided parameters **/
  private def _testDbConnection(adminConfig: AdminConfig, showPopup: Boolean = true): Boolean = { //return connectionEstablished
    DatabaseConnection.testDbConnection(adminConfig, showPopup)
  }

  /** Add stuff to define another raw_files mount point **/
  private def _addRawFilesMountPoint(
    key: String = "",
    value: String = "") {

    def _onRawFileMpDelete(mp: MountPointPanel): Unit = {
      rawFilesMountPoints -= mp
      rawFilesMpBox.content = rawFilesMountPoints
    }

    rawFilesMountPoints += new MountPointPanel(
      key = key,
      value = value,
      onDeleteAction = _onRawFileMpDelete
    )
    rawFilesMpBox.content = rawFilesMountPoints
  }

  /** Add stuff to define another mzdb_files mount point **/
  private def _addMzdbFilesMountPoint(
    key: String = "",
    value: String = "") {

    def _onMzdbFileMpDelete(mp: MountPointPanel): Unit = {
      mzdbFilesMountPoints -= mp
      mzdbFilesMpBox.content = mzdbFilesMountPoints
    }

    mzdbFilesMountPoints += new MountPointPanel(
      key = key,
      value = value,
      onDeleteAction = _onMzdbFileMpDelete
    )
    mzdbFilesMpBox.content = mzdbFilesMountPoints
  }

  /** Add stuff to define another result_files mount point **/
  private def _addResultFilesMountPoint(
    key: String = "",
    value: String = ""
  ) {

    def _onResultFileMpDelete(mp: MountPointPanel): Unit = {
      resultFilesMountPoints -= mp
      resultFilesMpBox.content = resultFilesMountPoints
    }

    resultFilesMountPoints += new MountPointPanel(
      key = key,
      value = value,
      onDeleteAction = _onResultFileMpDelete
    )
    resultFilesMpBox.content = resultFilesMountPoints
  }

  /** Action run when "Save" button is pressed **/
  private def _onSavePressed() {

    Main.stage.scene().setCursor(Cursor.WAIT)

    /* New AdminConfig*/
    val newAdminConfig = _toAdminConfig()
    adminConfigFile.write(newAdminConfig)

    if (serverConfigOpt.isDefined) {

      /* New ServerConfig */
      val newServerConfig = _toServerConfig()
      serverConfigFileOpt.get.write(newServerConfig, newAdminConfig)
    }

    /* Test connection to database */
    // Don't try to reach UDSdb. Here we just want to know if config is valid, not if Proline is set up
    val connectionEstablished = _testDbConnection(newAdminConfig, showPopup = false)
    
    if (connectionEstablished) {

      /* Log and close dialog if config is valid */
      logger.info("Configuration file(s) successfully updated !")
      formEditor.close()

      /* Then compute if Proline is already set up */
      ProlineAdminConnection.loadProlineConf(verbose = true)
    
    } else {
      
      /* If DB can't be reached, allow to save configuration anyway */
      val isConfirmed = GetConfirmation(
        title = "Invalid configuration",
        text = "The connection to the database can't be established with these settings.\n" +
        "Do you want to save this configuration anyway?"
      )

      if (isConfirmed) {
        formEditor.close()
        ProlineAdminConnection.loadProlineConf(verbose = true)
      }
    }
    
    Main.stage.scene().setCursor(Cursor.DEFAULT)
  }
}

/**
 * List mount points types
 **/
object MountPointType {
  val RAW_FILES = "RAW_FILES"
  val MZDB_FILES = "MZDB_FILES"
  val RESULT_FILES = "RESULT_FILES"
}

/**
 * Build 1 mount point panel
 **/
class MountPointPanel(
  onDeleteAction: (MountPointPanel) => Unit,
  key: String = "",
  value: String = ""
) extends HBox {

  val thisMountPoint = this

  /* Components */
  val keyField = new TextField {
    minWidth = 144
    maxWidth = 144
    promptText = "key"
    text = key
  }
  val equalLabel = new Label("=") {
    minWidth = 15
    maxWidth = 15
  }
  val valueField = new TextField {
    prefWidth <== thisMountPoint.width
    promptText = "value"
    text = value
  }
  val removeButton = new Button("Remove") {
    minWidth = 88
    maxWidth = 88
    onAction = handle { onDeleteAction(thisMountPoint) }
  }

  /* Layout */
  spacing = 10
  alignment = Pos.Center
  content = List(keyField, equalLabel, valueField, removeButton)

  /* Features */
  def getKey = keyField.text()
  def getValue = valueField.text()
}