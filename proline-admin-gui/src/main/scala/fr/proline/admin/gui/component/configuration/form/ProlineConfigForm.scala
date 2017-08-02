package fr.proline.admin.gui.component.configuration.form

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
import fr.proline.admin.gui.Main
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
class ProlineConfigForm()(implicit val parentStage: Stage) extends VBox with IConfigFilesForm with LazyLogging {

  maxHeight = Screen.primary.visualBounds.height - 20 // arbitrary margin //816

  /* Configuration files */
  //this stage can't be opened if adminConfigFile is undefined in Main
  private val adminConfigFile = new AdminConfigFile(Main.adminConfPath)
  private val adminConfigOpt = adminConfigFile.read()
  require(adminConfigOpt.isDefined, "admin config is undefined")
  private val adminConfig = adminConfigOpt.get

  private val serverConfigFileOpt =
    if (isEmpty(Main.serverConfPath)) None
    else Option( new ServerConfigFile(Main.serverConfPath) )

  private val serverConfigOpt = serverConfigFileOpt.map(_.read()).flatten

  private val pwxConfigFileOpt =
    if (isEmpty(Main.pwxConfPath)) None
    else Option(new PwxConfigFile(Main.pwxConfPath))


  /*
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
    minWidth = 55
    maxWidth = 55
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
  val hostNameWarning = new Label{
    graphic = ScalaFxUtils.newImageView(IconResource.WARNING)
    text = "Don't use the term 'localhost', but the real IP address or fully qualified name of the server."
    wrapText = true
  }

  val portLabel = new Label("Port :")
  val portField = new NumericTextField()

  val testConnectionHyperlink = new Hyperlink("Test connection") {
    onAction = handle { _testDbConnection(_toAdminConfig()) }
  }

//  /* DB names */
//  val udsLabel = new Label("UDS database:")
//  val pdiLabel = new Label("PDI database:")
//  val psLabel = new Label("PS database:")
//  val msiLabel = new Label("MSI database:")
//  val lcmsLabel = new Label("LCMS database:")

  /* Mount points */
  val disableMpNoteLabel = new Label() {
    text = "Proline server configuration file must be provided to enable mount points setup.\n" +
      """See menu "Select configuration files"."""
    style = "-fx-font-style: italic;-fx-font-weigth: bold;"
    visible = false
  }

  val rawFilesMountPoints = ArrayBuffer[MountPointPanel]()
  val rawFilesMpLabel = new BoldLabel("Raw files :", upperCase = false)
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

  /*
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

  // Make all text fields grow the max. they can
  Seq(
    driverTypeBox, dataDirField,
    userNameField, passwordPWDField, passwordTextField, hostNameField, hostNameWarning, portField
  ).foreach { node =>
      node.minWidth = 150
      node.prefWidth <== parentStage.width
    }

  //VBox & HBox spacing
  private val V_SPACING = 10
  private val H_SPACING = 5

  /* Proline settings */
  //TODO: properly delete me if useless
  /*val prolineSettings = new TitledBorderPane(
    title = "Proline",
    contentNode = new VBox {
      padding = Insets(5)
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
  )*/

  /* DB connection */
  //Set text- and password textfields at the same place in UI
  val dbPwdPane = new StackPane {
    alignmentInParent = Pos.BottomLeft
    content = List(passwordPWDField, passwordTextField)
  }

  val dbConnectionSettings = new TitledBorderPane(
    title = "Database connection",
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
          content = List(
            hostNameLabel,
            new VBox {
              content = List(hostNameField, hostNameWarning)
            }
          )
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
    title = "Mount points",
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
        resultFilesMpBox
      )
    }
  )

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
    dbConnectionSettings,
    mountPointsWithDisableNote,
    wrappedApplyButton
  )
  prefWidth <== parentStage.width

  /*
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
  private def _getTextFieldValue(txtField: TextField): Option[String] = textField2StringOpt(txtField, allowEmpty = false)

  private def _toAdminConfig() = AdminConfig(
    filePath = adminConfig.filePath,
    serverConfigFilePath = adminConfig.serverConfigFilePath.map(doubleBackSlashes), //FIXME: windows-specific
    pwxConfigFilePath = adminConfig.pwxConfigFilePath.map(doubleBackSlashes), //FIXME: windows-specific
    pgsqlDataDir = adminConfig.pgsqlDataDir.map(doubleBackSlashes), //FIXME: windows-specific
    driverType = adminConfig.driverType, //immutable in UI
    prolineDataDir = _getTextFieldValue(dataDirField).map(doubleBackSlashes), //FIXME: windows-specific
    dbUserName = _getTextFieldValue(userNameField),
    dbPassword = _getTextFieldValue(passwordTextField),
    dbHost = _getTextFieldValue(hostNameField),
    dbPort = portField //FIXME
  )

  /** Get GUI information to create new ServerConfig object **/
  private def _getMountPointsMap(mpArray: ArrayBuffer[MountPointPanel]): Map[String, String] = {
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
    resultFilesMountPoints = _getMountPointsMap(resultFilesMountPoints)
  )

  /** Test connection with DB with provided parameters **/
  private def _testDbConnection(
    adminConfig: AdminConfig,
    showSuccessPopup: Boolean = true,
    showFailurePopup: Boolean = true
  ): Boolean = { //return connectionEstablished

    DatabaseConnection.testDbConnection(adminConfig, showSuccessPopup, showFailurePopup)
  }

  /** Add stuff to define another raw_files mount point **/
  private def _addRawFilesMountPoint(
    key: String = "",
    value: String = ""
  ) {

    def _onRawFileMpDelete(mp: MountPointPanel): Unit = {
      rawFilesMountPoints -= mp
      rawFilesMpBox.content = rawFilesMountPoints
    }

    rawFilesMountPoints += new MountPointPanel(
      key = key,
      value = value,
      onDeleteAction = _onRawFileMpDelete,
      parentStage = parentStage
    )
    rawFilesMpBox.content = rawFilesMountPoints
  }

  /** Add stuff to define another mzdb_files mount point **/
  private def _addMzdbFilesMountPoint(
    key: String = "",
    value: String = ""
  ) {

    def _onMzdbFileMpDelete(mp: MountPointPanel): Unit = {
      mzdbFilesMountPoints -= mp
      mzdbFilesMpBox.content = mzdbFilesMountPoints
    }

    mzdbFilesMountPoints += new MountPointPanel(
      key = key,
      value = value,
      onDeleteAction = _onMzdbFileMpDelete,
      parentStage = parentStage
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
      onDeleteAction = _onResultFileMpDelete,
      parentStage = parentStage
    )
    resultFilesMpBox.content = resultFilesMountPoints
  }
  
  /** Check form **/
  private def _checkForm(): String = {
    
    //TODO: finish me
    
    val errString = new StringBuilder()

    /* Mount points keys */
//    val spaceCharPattern = """\s""".r
//    val corruptedKeys = (rawFilesMountPoints ++ mzdbFilesMountPoints ++ resultFilesMountPoints).toArray
//      .map(_.getKey)
//      .filter { key =>
//        // no space in key
//        spaceCharPattern.findFirstIn(key).isDefined
//      }
//    
//    if (corruptedKeys.isEmpty == false){
//      errString ++= "The following mount point names are incorrect: " + corruptedKeys.mkString(", ") + "\n"
//    }
    
    
    //return 
    errString.result()
  }

  /** Action run when "Save" button is pressed **/

  def checkForm(): Boolean = {
    //TODO
    true
  }
  
  def saveForm() {
    
    //TODO

    Main.stage.scene().setCursor(Cursor.WAIT)

    val isPwxConfigDefined = pwxConfigFileOpt.isDefined
    var continue: Boolean = false
    
    /* If PWX config is defined, make sure user understand that values will be overwritten */
    if (isPwxConfigDefined) {
      continue = GetConfirmation(
        text = "Proline Web Extension (PWX) mount points will be overwritten with these values.\nDo you want to continue?",
        title = "Warning"
      )
    }
    
    /* If PWX config is undefined, warn user that changes will not be written in it */
    else {
      continue = GetConfirmation(
        text = "Proline Web Extension (PWX) mount points will not be updated since its configuration file is not defined "+
        "\n(see Menu -> Select configuration files -> Full path to PWX configuration file)." + 
        "\nDo you want to continue?",
        title = "Warning"
      )
    }

    if (continue) {
      /* New AdminConfig*/
      val newAdminConfig = _toAdminConfig()
      adminConfigFile.write(newAdminConfig)

      if (serverConfigOpt.isDefined) {

        /* New ServerConfig */
        val newServerConfig = _toServerConfig()
        serverConfigFileOpt.get.write(newServerConfig, newAdminConfig)

        /* New PWX config */
        //if (isPwxConfigDefined) pwxConfigFileOpt.get.write(newServerConfig)
      }

      /* Test connection to database */
      // Don't try to reach database. Here we just want to know if config is valid, not if Proline is set up
      val connectionEstablished = _testDbConnection(newAdminConfig, false, false)

      if (connectionEstablished) {

        /* Log and close dialog if config is valid */
        logger.info("Configuration file(s) successfully updated !")
        parentStage.close()

        /* Then compute if Proline is already set up */
        ProlineAdminConnection.loadProlineConf(verbose = true)

      }
      else {

        /* If DB can't be reached, allow to save configuration anyway */
        val isConfirmed = GetConfirmation(
          title = "Invalid configuration",
          text = "The connection to the database can't be established with these settings.\n" +
            "Do you want to save this configuration anyway?"
        )

        if (isConfirmed) {
          parentStage.close()
          ProlineAdminConnection.loadProlineConf(verbose = true)
        }
      }
      //}
      Main.stage.scene().setCursor(Cursor.DEFAULT)
    }
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
  parentStage: Stage,
  onDeleteAction: (MountPointPanel) => Unit,
  key: String = "",
  value: String = ""
) extends HBox {

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
        dcInitOwner = parentStage
      )
      
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