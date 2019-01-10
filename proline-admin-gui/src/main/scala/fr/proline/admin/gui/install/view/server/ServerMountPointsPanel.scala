package fr.proline.admin.gui.install.view.server

import com.typesafe.scalalogging.LazyLogging

import scalafx.Includes._
import scalafx.stage.Stage
import scalafx.geometry.{ Insets, Pos }
import scalafx.scene.control.Button
import scalafx.scene.control.Label
import scalafx.scene.control.TextField
import scalafx.scene.layout.VBox
import scalafx.scene.layout.HBox
import scalafx.collections.ObservableBuffer

import fr.proline.admin.gui.Install
import fr.proline.admin.gui.IconResource
import fr.proline.admin.gui.install.model.AdminModelView
import fr.proline.admin.gui.process.config._
import fr.proline.admin.gui.util.FxUtils
import fr.proline.repository.DriverType

import fr.profi.util.scala.ScalaUtils.doubleBackSlashes
import fr.profi.util.scala.ScalaUtils.isEmpty
import fr.profi.util.scalafx.BoldLabel
import fr.profi.util.scalafx.FileBrowsing
import fr.profi.util.scalafx.ScalaFxUtils._
import fr.profi.util.scalafx.TitledBorderPane
import fr.profi.util.scalafx.CustomScrollPane
import fr.proline.admin.gui.wizard.process.config._

import scala.collection.mutable.ArrayBuffer

/**
 * Creates and displays Proline server mount points panel.
 * @author aromdhani
 *
 */
class ServerMountPointsPanel(model: AdminModelView) extends CustomScrollPane with LazyLogging {

  /* Configuration files */
  private val serverConfigOpt = model.serverConfigFileOpt().map(_.read()).flatten
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

  val rawFilesMountPoints = ArrayBuffer[MountPointPane]()
  val rawFilesMpLabel = new BoldLabel("Raw files path:   ", upperCase = false)
  val addRawFilesMpButton = new Button("Add") {
    graphic = FxUtils.newImageView(IconResource.PLUS)
    onAction = handle { _addRawFilesMountPoint() }
  }
  val rawFilesMpBox = new VBox { spacing = 10 }

  val mzdbFilesMountPoints = ArrayBuffer[MountPointPane]()
  val mzdbFilesMpLabel = new BoldLabel("mzDB files path: ", upperCase = false)
  val addMzdbFilesMpButton = new Button("Add") {
    graphic = FxUtils.newImageView(IconResource.PLUS)
    onAction = handle { _addMzdbFilesMountPoint() }
  }
  val mzdbFilesMpBox = new VBox { spacing = 10 }

  val resultFilesMountPoints = ArrayBuffer[MountPointPane]()
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
  /* Mount points */
  val mountPointsSettings = new TitledBorderPane(
    title = "File Locations",
    titleTooltip = "Mount points as defined in Proline server configuration",
    contentNode = new VBox {
      prefHeight <== Install.stage.height - 300
      prefWidth <== Install.stage.width - 95
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

  /* Set panel content */
  setContentNode(
    new VBox {
      alignmentInParent = Pos.Center
      prefWidth <== Install.stage.width - 85
      prefHeight <== Install.stage.height - 45
      padding = Insets(5, 0, 5, 5)
      children = List(mountPointsWithDisableNote)
    })

  /*
   * ************* *
   * INIT. CONTENT *
   * ************* *
   */
  def isPrompt(str: String): Boolean = str matches """<.*>"""

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
  /** Get GUI information to create new ServerConfig object **/
  private def _getMountPointsMap(mpArray: ArrayBuffer[MountPointPane]): Map[String, String] = {
    (
      for (
        mp <- mpArray.view;
        (k, v) = (mp.getKey, mp.getValue);
        if k.isEmpty == false && v.isEmpty == false
      ) yield k -> v //doubleQuoted(v)
      ).toMap
  }

  /** Return entred Proline mount points as ServerConfig */
  def toServerConfig() = ServerConfig(
    rawFilesMountPoints = _getMountPointsMap(rawFilesMountPoints),
    mzdbFilesMountPoints = _getMountPointsMap(mzdbFilesMountPoints),
    resultFilesMountPoints = _getMountPointsMap(resultFilesMountPoints))

  /** Add stuff to define another raw_files mount point **/
  private def _addRawFilesMountPoint(
    key: String = "",
    value: String = "") {

    def _onRawFileMpDelete(mp: MountPointPane): Unit = {
      rawFilesMountPoints -= mp
      rawFilesMpBox.children = rawFilesMountPoints
    }

    rawFilesMountPoints += new MountPointPane(
      key = key,
      value = value,
      onDeleteAction = _onRawFileMpDelete,
      parentStage = Install.stage)
    rawFilesMpBox.children = rawFilesMountPoints
  }

  /** Add stuff to define another mzdb_files mount point **/
  private def _addMzdbFilesMountPoint(
    key: String = "",
    value: String = "") {

    def _onMzdbFileMpDelete(mp: MountPointPane): Unit = {
      mzdbFilesMountPoints -= mp
      mzdbFilesMpBox.children = mzdbFilesMountPoints
    }

    mzdbFilesMountPoints += new MountPointPane(
      key = key,
      value = value,
      onDeleteAction = _onMzdbFileMpDelete,
      parentStage = Install.stage)
    mzdbFilesMpBox.children = mzdbFilesMountPoints
  }

  /** Add stuff to define another result_files mount point **/
  private def _addResultFilesMountPoint(
    key: String = "",
    value: String = "") {

    def _onResultFileMpDelete(mp: MountPointPane): Unit = {
      resultFilesMountPoints -= mp
      resultFilesMpBox.children = resultFilesMountPoints
    }

    resultFilesMountPoints += new MountPointPane(
      key = key,
      value = value,
      onDeleteAction = _onResultFileMpDelete,
      parentStage = Install.stage)
    resultFilesMpBox.children = resultFilesMountPoints
  }

  /** Return the number of selected of mount points */
  def getProperties(): String = {
    val mountPointsBuilder = new StringBuilder()
    if (Seq(rawFilesMountPoints, mzdbFilesMountPoints, resultFilesMountPoints).forall(_.isEmpty)) { mountPointsBuilder.toString }
    else {
      mountPointsBuilder.append("\t")
      mountPointsBuilder.append(rawFilesMountPoints.size).append("  raw files\n\t")
        .append(mzdbFilesMountPoints.size).append("  Mzdb files\n\t")
        .append(resultFilesMountPoints.size).append("  Result Files\t")
      mountPointsBuilder.toString
    }
  }
}

/**
 * Build a mount point panel
 *
 */
class MountPointPane(
    parentStage: Stage,
    onDeleteAction: (MountPointPane) => Unit,
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