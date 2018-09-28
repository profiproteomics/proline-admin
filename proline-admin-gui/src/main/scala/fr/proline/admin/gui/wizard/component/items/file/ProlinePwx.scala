package fr.proline.admin.gui.wizard.component.items.file

import com.typesafe.scalalogging.LazyLogging

import scalafx.Includes._
import scalafx.stage.Stage
import scalafx.geometry.Pos
import scalafx.scene.control.Button
import scalafx.scene.control.Label
import scalafx.scene.control.TextField
import scalafx.scene.layout.VBox
import scalafx.scene.layout.HBox
import scalafx.scene.layout.Priority

import fr.proline.admin.gui.IconResource
import fr.proline.admin.gui.Wizard
import fr.proline.admin.gui.wizard.component.items.ServerConfig
import fr.proline.admin.gui.process.config._
import fr.profi.util.scalafx.BoldLabel
import fr.profi.util.scalafx.FileBrowsing
import fr.profi.util.scalafx.NumericTextField
import fr.profi.util.scalafx.ScalaFxUtils
import fr.profi.util.scalafx.ScalaFxUtils._
import fr.profi.util.scalafx.TitledBorderPane
import fr.profi.util.scala.ScalaUtils.isEmpty
import fr.proline.admin.gui.util.FxUtils

import scala.collection.mutable.ArrayBuffer

import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global

/**
 * build a VBox to edit Proline web configuration file.
 * @param stage The parent stage of the Layout
 *
 */
class ProlinePwx(stage: Stage) extends VBox with LazyLogging {

  //maxHeight = Screen.primary.visualBounds.height - 20 // 

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
  val disableMPLabelText = "Select a validated Proline server configuration file to enable mount points setup. "
  val infosImportMPLabelText = "File locations will be imported from Proline server configuration mount points. "
  val infosMergeMPLabelText = "New mount points will be merged with the existing mount points in proline web configuration file. "

  val disableMpNoteLabel = new Label() {
    graphic = FxUtils.newImageView(IconResource.INFORMATION)
    text = infosMergeMPLabelText
    style = TextStyle.BLUE_ITALIC
    visible = false
  }
  val warningLabel = new Label {
    graphic = FxUtils.newImageView(IconResource.INFORMATION)
    text = infosMergeMPLabelText
    style = TextStyle.BLUE_ITALIC
  }

  val rawFilesMountPoints = ArrayBuffer[MountPointPanelPwx]()
  val rawFilesMpLabel = new BoldLabel("Raw files path:   ", upperCase = false)
  val addRawFilesMpButton = new Button("Add") {
    graphic = FxUtils.newImageView(IconResource.PLUS)
    onAction = handle { _addRawFilesMountPoint() }
  }
  val rawFilesMpBox = new VBox { spacing = 10 }

  val mzdbFilesMountPoints = ArrayBuffer[MountPointPanelPwx]()
  val mzdbFilesMpLabel = new BoldLabel("mzDB files path: ", upperCase = false)
  val addMzdbFilesMpButton = new Button("Add") {
    graphic = FxUtils.newImageView(IconResource.PLUS)
    onAction = handle { _addMzdbFilesMountPoint() }
  }
  val mzdbFilesMpBox = new VBox { spacing = 10 }

  val resultFilesMountPoints = ArrayBuffer[MountPointPanelPwx]()
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

  /* Jms server settings */
  val pwxJmsSettings = new TitledBorderPane(
    title = "Jms Server",
    titleTooltip = "Jms Server Setting",
    contentNode = new VBox {
      spacing = 2 * V_SPACING
      children = Seq(
        new VBox {
          spacing = V_SPACING
          children = List(hostLabel,
            new HBox {
              spacing = H_SPACING
              children = Seq(hostField)
            },
            ScalaFxUtils.newVSpacer(minH = 10),
            portLabel,
            new HBox {
              spacing = H_SPACING
              children = Seq(portField)
            })
        })
    })

  /* Mount points */
  val mountPointsSettings = new TitledBorderPane(
    title = "File Locations",
    titleTooltip = "Mount points as defined in Proline web configuration",
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
  val labelWarningPanel = new VBox {
    spacing = 2
    children = Seq(disableMpNoteLabel, warningLabel)
  }
  val mountPointsWithDisableNote = new VBox {
    spacing = 20
    children = Seq(pwxJmsSettings, ScalaFxUtils.newVSpacer(3), labelWarningPanel, ScalaFxUtils.newVSpacer(3), mountPointsSettings)
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

  /* Mount points */
  if (pwxMountPointConfigOpt.isEmpty) {

    /* hide mount points if server config is undefined */
    disableMpNoteLabel.text_=(disableMPLabelText)
    disableMpNoteLabel.visible = true
    warningLabel.visible = false
    mountPointsSettings.disable = true
    //Don't screw up layout
    disableMpNoteLabel.minHeight = 34
    disableMpNoteLabel.maxHeight = 34

  } else {
    // hide this section ,mount points will be copied from server
    disableMpNoteLabel.minHeight = 0
    disableMpNoteLabel.maxHeight = 0
    disableMpNoteLabel.visible = false
    if (Wizard.items.contains(2)) {
      disableMpNoteLabel.minHeight = 34
      disableMpNoteLabel.maxHeight = 34
      disableMpNoteLabel.text_=(infosImportMPLabelText)
      disableMpNoteLabel.visible = true
      warningLabel.visible = false
      mountPointsSettings.disable = true

    } else {

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
      rawFilesMpBox.children = rawFilesMountPoints
    }

    rawFilesMountPoints += new MountPointPanelPwx(
      key = key,
      value = value,
      onDeleteAction = _onRawFileMpDelete,
      parentStage = stage)
    rawFilesMpBox.children = rawFilesMountPoints
  }

  /** Add stuff to define another mzdb_files mount point **/
  private def _addMzdbFilesMountPoint(
    key: String = "",
    value: String = "") {

    def _onMzdbFileMpDelete(mp: MountPointPanelPwx): Unit = {
      mzdbFilesMountPoints -= mp
      mzdbFilesMpBox.children = mzdbFilesMountPoints
    }

    mzdbFilesMountPoints += new MountPointPanelPwx(
      key = key,
      value = value,
      onDeleteAction = _onMzdbFileMpDelete,
      parentStage = stage)
    mzdbFilesMpBox.children = mzdbFilesMountPoints
  }

  /** Add stuff to define another result_files mount point **/
  private def _addResultFilesMountPoint(
    key: String = "",
    value: String = "") {

    def _onResultFileMpDelete(mp: MountPointPanelPwx): Unit = {
      resultFilesMountPoints -= mp
      resultFilesMpBox.children = resultFilesMountPoints
    }

    resultFilesMountPoints += new MountPointPanelPwx(
      key = key,
      value = value,
      onDeleteAction = _onResultFileMpDelete,
      parentStage = stage)
    resultFilesMpBox.children = resultFilesMountPoints
  }

  /* save Proline PWX configuration  file */

  def saveForm() {

    if (pwxMountPointConfigOpt.isDefined) {

      val newConfig = Future {
        if (Wizard.items.contains(2)) {

          //import mount points from proline server configuration 
          val serverConfigOpt = Wizard.items.get(2).get
          val serverItem = serverConfigOpt.get.asInstanceOf[ServerConfig]
          val mountPointsServerConfig = serverItem.mountsPoint.mountfiles._toServerConfig()
          val newPwxJmsConfiog = _toPwxJmsConfig()
          new PwxConfigFile(Wizard.webRootPath).write(mountPointsServerConfig, newPwxJmsConfiog)
        } else {
          val newPwxJmsConfiog = _toPwxJmsConfig()
          val newPwxConfig = _toServerConfig()
          new PwxConfigFile(Wizard.webRootPath).write(newPwxConfig, newPwxJmsConfiog)
        }

      }
      newConfig onFailure {
        case (t) => logger.error(s"An error has occured: ${t.getMessage}")
      }
    }
  }

  /* return number of mount points (files) */
  def getProperties(): String = {
    if (Wizard.items.contains(2)) {
      s" Mount Points:\n\tSame mount points as Proline server"
    } else {
      val montPointsBuilder = new StringBuilder("Mount Points:\n\t")
      montPointsBuilder.append(rawFilesMountPoints.size).append("  raw files\n\t")
        .append(mzdbFilesMountPoints.size).append("  Mzdb files\n\t")
        .append(resultFilesMountPoints.size).append("  Result Files\t")
      return montPointsBuilder.toString
    }
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