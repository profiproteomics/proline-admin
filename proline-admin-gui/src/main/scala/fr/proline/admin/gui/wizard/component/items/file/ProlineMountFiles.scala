package fr.proline.admin.gui.wizard.component.items.file

import com.typesafe.scalalogging.LazyLogging

import scala.collection.mutable.ArrayBuffer

import scalafx.Includes._
import scalafx.collections.ObservableBuffer
import scalafx.geometry.Insets
import scalafx.geometry.Pos
import scalafx.scene.Cursor
import scalafx.scene.Cursor.sfxCursor2jfx
import scalafx.scene.control.Button
import scalafx.scene.control.Hyperlink
import scalafx.scene.control.Label
import scalafx.scene.control.TextField
import scalafx.scene.layout.HBox
import scalafx.scene.layout.Priority
import scalafx.scene.layout.StackPane
import scalafx.scene.layout.VBox
import scalafx.stage.Screen
import scalafx.stage.Stage

import fr.proline.admin.gui.IconResource
import fr.proline.admin.gui.Wizard
import fr.proline.admin.gui.process.DatabaseConnection
import fr.proline.admin.gui.process.ProlineAdminConnection
import fr.proline.admin.gui.process.config._
import fr.proline.admin.gui.util.FxUtils
import fr.proline.admin.gui.wizard.util.GetConfirmation
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
import fr.proline.admin.gui.wizard.process.config._

import scala.concurrent._
import ExecutionContext.Implicits.global

/**
 * Create a modal window to edit Proline configuration's file.
 *
 */
class ProlineMountFiles extends VBox with LazyLogging {

  maxHeight = Screen.primary.visualBounds.height - 20 // 

  /* Configuration files */

  private val adminConfigFile = new AdminConfigFile(Wizard.adminConfPath)
  private val adminConfigOpt = adminConfigFile.read()
  require(adminConfigOpt.isDefined, "admin config is undefined .")
  private val adminConfig = adminConfigOpt.get

  private val serverConfigFileOpt =
    if (isEmpty(Wizard.serverConfPath)) None
    else Option(new ServerConfigFile(Wizard.serverConfPath))
  private val serverConfigOpt = serverConfigFileOpt.map(_.read()).flatten

  /*
   * ********** *
   * COMPONENTS *
   * ********** *
   */

  /* Mount points */
  val disableMpNoteLabel = new Label() {
    text = "Select a validated Proline server configuration file to enable mount points setup."
    style = TextStyle.RED_ITALIC
    visible = false
  }

  val rawFilesMountPoints = ArrayBuffer[MountPointPanelWizard]()
  val rawFilesMpLabel = new BoldLabel("Raw files path:   ", upperCase = false)
  val addRawFilesMpButton = new Button("Add") {
    graphic = FxUtils.newImageView(IconResource.PLUS)
    onAction = handle { _addRawFilesMountPoint() }
  }
  val rawFilesMpBox = new VBox { spacing = 10 }

  val mzdbFilesMountPoints = ArrayBuffer[MountPointPanelWizard]()
  val mzdbFilesMpLabel = new BoldLabel("mzDB files path: ", upperCase = false)
  val addMzdbFilesMpButton = new Button("Add") {
    graphic = FxUtils.newImageView(IconResource.PLUS)
    onAction = handle { _addMzdbFilesMountPoint() }
  }
  val mzdbFilesMpBox = new VBox { spacing = 10 }

  val resultFilesMountPoints = ArrayBuffer[MountPointPanelWizard]()
  val resultFilesMpLabel = new BoldLabel("Result files path: ", upperCase = false)
  val addResultFilesMpButton = new Button("Add") {
    graphic = FxUtils.newImageView(IconResource.PLUS)
    onAction = handle { _addResultFilesMountPoint() }
  }
  val resultFilesMpBox = new VBox { spacing = 10 }

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
    title = "File Locations",
    titleTooltip = "Mount points as defined in Proline server configuration",
    contentNode = new VBox {
      spacing = 2 * V_SPACING
      children = List(
        new HBox {
          spacing = H_SPACING
          children = List(rawFilesMpLabel, addRawFilesMpButton)
        },
        rawFilesMpBox,

        new HBox {
          spacing = H_SPACING
          children = List(mzdbFilesMpLabel, addMzdbFilesMpButton)
        },
        mzdbFilesMpBox,

        new HBox {
          spacing = H_SPACING
          children = List(resultFilesMpLabel, addResultFilesMpButton)
        },
        resultFilesMpBox)
    })

  val mountPointsWithDisableNote = new VBox {
    spacing = 15
    children = Seq(disableMpNoteLabel, mountPointsSettings)
  }

  /* VBox layout and content */
  alignment = Pos.Center
  alignmentInParent = Pos.Center
  spacing = 5
  children = List(
    mountPointsWithDisableNote)

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
    filePath = Wizard.adminConfPath,
    serverConfigFilePath = Option(Wizard.serverConfPath).map(doubleBackSlashes), //FIXME: windows-specific
    pwxConfigFilePath = Option(Wizard.webRootPath).map(doubleBackSlashes), //FIXME: windows-specific
    pgsqlDataDir = Option(Wizard.pgDataDirPath).map(doubleBackSlashes), //FIXME: windows-specific
    seqRepoConfigFilePath = Option(Wizard.seqRepoConfPath).map(doubleBackSlashes),
    driverType = Option(DriverType.POSTGRESQL),
    prolineDataDir = Option(Wizard.pgDataDirPath).map(doubleBackSlashes), //FIXME: windows-specific
    dbUserName = Option(Wizard.userName),
    dbPassword = Option(Wizard.passWord),
    dbHost = Option(Wizard.hostName),
    dbPort = Option(Wizard.port) //FIXME
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

  def _toServerConfig() = ServerConfig(
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
      rawFilesMpBox.children = rawFilesMountPoints
    }

    rawFilesMountPoints += new MountPointPanelWizard(
      key = key,
      value = value,
      onDeleteAction = _onRawFileMpDelete,
      parentStage = Wizard.stage)
    rawFilesMpBox.children = rawFilesMountPoints
  }

  /** Add stuff to define another mzdb_files mount point **/
  private def _addMzdbFilesMountPoint(
    key: String = "",
    value: String = "") {

    def _onMzdbFileMpDelete(mp: MountPointPanelWizard): Unit = {
      mzdbFilesMountPoints -= mp
      mzdbFilesMpBox.children = mzdbFilesMountPoints
    }

    mzdbFilesMountPoints += new MountPointPanelWizard(
      key = key,
      value = value,
      onDeleteAction = _onMzdbFileMpDelete,
      parentStage = Wizard.stage)
    mzdbFilesMpBox.children = mzdbFilesMountPoints
  }

  /** Add stuff to define another result_files mount point **/
  private def _addResultFilesMountPoint(
    key: String = "",
    value: String = "") {

    def _onResultFileMpDelete(mp: MountPointPanelWizard): Unit = {
      resultFilesMountPoints -= mp
      resultFilesMpBox.children = resultFilesMountPoints
    }

    resultFilesMountPoints += new MountPointPanelWizard(
      key = key,
      value = value,
      onDeleteAction = _onResultFileMpDelete,
      parentStage = Wizard.stage)
    resultFilesMpBox.children = resultFilesMountPoints
  }

  /** Action run when "Save" button is pressed **/

  def checkForm(): Boolean = {
    //TODO
    true
  }

  def save() {
    val newConfig = Future {
      val newAdminConfig = _toAdminConfig()
      /* Test connection to database */
      if (serverConfigOpt.isDefined) {
        val newServerConfig = _toServerConfig()
        serverConfigFileOpt.get.write(newServerConfig, newAdminConfig)
      }
    }
    newConfig onFailure {
      case (t) => logger.error(s"An error has occured: ${t.getMessage}")
    }
  }

  def getProperties(): String = {
    val montPointsBuilder = new StringBuilder("Mount Points:\n\t")
    montPointsBuilder.append(rawFilesMountPoints.size).append("  raw files\n\t")
      .append(mzdbFilesMountPoints.size).append("  Mzdb files\n\t")
      .append(resultFilesMountPoints.size).append("  Result Files\t")
    return montPointsBuilder.toString
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
    minWidth = 80
    maxWidth = 80
    graphic = FxUtils.newImageView(IconResource.LOAD)
    onAction = handle {
      val dir = FileBrowsing.browseDirectory(
        dcTitle = "Select mount point directory",
        dcInitialDir = valueField.text(),
        dcInitOwner = parentStage)

      if (dir != null) valueField.text = dir.getAbsolutePath()
    }
  }
  val removeButton = new Button("Remove") {
    minWidth = 80
    maxWidth = 80
    graphic = FxUtils.newImageView(IconResource.TRASH)
    onAction = handle { onDeleteAction(thisMountPoint) }
  }
  /* Layout */
  spacing = 10
  alignment = Pos.Center
  children = List(keyField, equalLabel, valueField, browseButton, removeButton)

  /* Features */
  def getKey = keyField.text()
  def getValue = valueField.text()
}