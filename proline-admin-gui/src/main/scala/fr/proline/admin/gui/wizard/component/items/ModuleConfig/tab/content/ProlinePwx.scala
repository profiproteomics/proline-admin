package fr.proline.admin.gui.wizard.component.items.ModuleConfig.tab.Content

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

/**
 * Create a modal window to edit Proline configuration's file.
 */
class ProlinePwx extends VBox with LazyLogging {

  maxHeight = Screen.primary.visualBounds.height - 20 // 

  //  private val serverConfigFileOpt =
  //    if (isEmpty(Wizard.serverConfPath)) None
  //    else Option(new ServerConfigFile(Wizard.serverConfPath))
  //  private val serverConfigOpt = serverConfigFileOpt.map(_.read()).flatten

  private val pwxConfigFileOpt =
    if (isEmpty(Wizard.webRootPath)) None
    else Option(new PwxConfigFile(Wizard.webRootPath))
  private val pwxMountPointConfigOpt = pwxConfigFileOpt.map(_.read()).flatten
  private val pwxJmsConfigOpt = pwxConfigFileOpt.map(_.getPwxJms()).flatten

  /*
   * ********** *
   * COMPONENTS *
   * ********** *
   */

  /* Jms settings  */

  private var pwxHostName: String = "localhost"
  private var pwxPort: Int = 5445

  if (pwxJmsConfigOpt.isDefined) {
    pwxHostName = pwxJmsConfigOpt.get.dbHost.get
    pwxPort = pwxJmsConfigOpt.get.dbPort.get
  }

  private val hostLabel = new Label("Host name: ")
  private val hostField = new TextField {
    text = pwxHostName
    text.onChange { (_, oldText, newText) =>
      pwxHostName = newText
    }
  }

  private val portLabel = new Label("Port number: ")
  private val portField = new NumericTextField {
    text = pwxPort.toString
    text.onChange {
      (_, oldText, newText) =>
        if ((newText != null) && !newText.equals("")) {
          pwxPort = newText.toInt
        }
    }
  }
  Seq(portLabel, hostLabel).foreach(_.minWidth = 60)
  Seq(hostField, portField).foreach {
    f => f.hgrow = Priority.Always
  }

  /* Mount points */

  val disableMpNoteLabel = new Label() {
    text = "choose a validated Proline server configuration file to enable mount points setup.\n"
    style = "-fx-font-style: italic;-fx-font-weigth: bold;"
    visible = false
  }

  val rawFilesMountPoints = ArrayBuffer[MountPointPanelPwx]()
  val rawFilesMpLabel = new BoldLabel("Raw files path: ", upperCase = false)
  val addRawFilesMpButton = new Button("Add") {
    onAction = handle { _addRawFilesMountPoint() }
  }
  val rawFilesMpBox = new VBox { spacing = 10 }

  val mzdbFilesMountPoints = ArrayBuffer[MountPointPanelPwx]()
  val mzdbFilesMpLabel = new BoldLabel("mzDB files path: ", upperCase = false)
  val addMzdbFilesMpButton = new Button("Add") {
    onAction = handle { _addMzdbFilesMountPoint() }
  }
  val mzdbFilesMpBox = new VBox { spacing = 10 }

  val resultFilesMountPoints = ArrayBuffer[MountPointPanelPwx]()
  val resultFilesMpLabel = new BoldLabel("Result files path: ", upperCase = false)
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

  /* Jms server settings */
  val pwxJmsSettings = new TitledBorderPane(
    title = "Jms Server",
    titleTooltip = "Jms Server Setting",
    contentNode = new VBox {
      spacing = 2 * V_SPACING
      content = Seq(
        new VBox {
          spacing = V_SPACING
          content = List(hostLabel,
            new HBox {
              spacing = H_SPACING
              content = Seq(hostField)
            },
            ScalaFxUtils.newVSpacer(minH = 10),
            portLabel,
            new HBox {
              spacing = H_SPACING
              content = Seq(portField)
            })
        })
    })

  /* Mount points */
  val mountPointsSettings = new TitledBorderPane(
    title = "File Locations",
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
    content = Seq(disableMpNoteLabel, pwxJmsSettings, mountPointsSettings)
  }

  /* VBox layout and content */
  alignment = Pos.Center
  alignmentInParent = Pos.Center
  spacing = 5
  content = List(
    mountPointsWithDisableNote)

  /*
   * ************* *
   * INIT. CONTENT *
   * ************* *
   */
  def isPrompt(str: String): Boolean = str matches """<.*>"""

  /* Common settings */

  /* Mount points */
  if (pwxMountPointConfigOpt.isEmpty) {

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
    val pwxMountPointConfig = pwxMountPointConfigOpt.get

    val rawMp = pwxMountPointConfig.rawFilesMountPoints
    if (rawMp.isEmpty) _addRawFilesMountPoint()
    else rawMp.foreach { case (k, v) => _addRawFilesMountPoint(k, v) }

    val mzdbMp = pwxMountPointConfig.mzdbFilesMountPoints
    if (mzdbMp.isEmpty) _addMzdbFilesMountPoint()
    else mzdbMp.foreach { case (k, v) => _addMzdbFilesMountPoint(k, v) }

    val resultMp = pwxMountPointConfig.resultFilesMountPoints
    if (resultMp.isEmpty) _addResultFilesMountPoint()
    else resultMp.foreach { case (k, v) => _addResultFilesMountPoint(k, v) }
  }

  /**
   * ******** *
   * FEATURES *
   * ******** *
   */

  /** Get GUI information to create new ServerConfig object **/
  private def _getMountPointsMap(mpArray: ArrayBuffer[MountPointPanelPwx]): Map[String, String] = {
    (
      for (
        mp <- mpArray.view;
        (k, v) = (mp.getKey, mp.getValue);
        if k.isEmpty == false && v.isEmpty == false
      ) yield k -> v //doubleQuoted(v)
      ).toMap
  }

  private def _toPwxJmsConfig() = PwxJmsServer(Option(pwxHostName),
    Option(pwxPort))

  private def _toServerConfig() = ServerConfig(
    rawFilesMountPoints = _getMountPointsMap(rawFilesMountPoints),
    mzdbFilesMountPoints = _getMountPointsMap(mzdbFilesMountPoints),
    resultFilesMountPoints = _getMountPointsMap(resultFilesMountPoints))

  /** Add stuff to define another raw_files mount point **/
  private def _addRawFilesMountPoint(
    key: String = "",
    value: String = "") {

    def _onRawFileMpDelete(mp: MountPointPanelPwx): Unit = {
      rawFilesMountPoints -= mp
      rawFilesMpBox.content = rawFilesMountPoints
    }

    rawFilesMountPoints += new MountPointPanelPwx(
      key = key,
      value = value,
      onDeleteAction = _onRawFileMpDelete,
      parentStage = Wizard.stage)
    rawFilesMpBox.content = rawFilesMountPoints
  }

  /** Add stuff to define another mzdb_files mount point **/
  private def _addMzdbFilesMountPoint(
    key: String = "",
    value: String = "") {

    def _onMzdbFileMpDelete(mp: MountPointPanelPwx): Unit = {
      mzdbFilesMountPoints -= mp
      mzdbFilesMpBox.content = mzdbFilesMountPoints
    }

    mzdbFilesMountPoints += new MountPointPanelPwx(
      key = key,
      value = value,
      onDeleteAction = _onMzdbFileMpDelete,
      parentStage = Wizard.stage)
    mzdbFilesMpBox.content = mzdbFilesMountPoints
  }

  /** Add stuff to define another result_files mount point **/
  private def _addResultFilesMountPoint(
    key: String = "",
    value: String = "") {

    def _onResultFileMpDelete(mp: MountPointPanelPwx): Unit = {
      resultFilesMountPoints -= mp
      resultFilesMpBox.content = resultFilesMountPoints
    }

    resultFilesMountPoints += new MountPointPanelPwx(
      key = key,
      value = value,
      onDeleteAction = _onResultFileMpDelete,
      parentStage = Wizard.stage)
    resultFilesMpBox.content = resultFilesMountPoints
  }

  /* save Proline PWX configuration  file */

  def saveForm() {

    Wizard.stage.scene().setCursor(Cursor.WAIT)
    if (pwxMountPointConfigOpt.isDefined) {
      val newPwxJmsConfiog = _toPwxJmsConfig()
      val newPwxConfig = _toServerConfig()
      new PwxConfigFile(Wizard.webRootPath).write(newPwxConfig, newPwxJmsConfiog)
    }
    Wizard.stage.scene().setCursor(Cursor.DEFAULT)
  }

  /* return number of mount points (files) */
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
class MountPointPanelPwx(
  parentStage: Stage,
  onDeleteAction: (MountPointPanelPwx) => Unit,
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