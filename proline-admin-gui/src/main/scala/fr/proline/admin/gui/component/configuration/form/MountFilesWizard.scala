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

import javafx.scene.control.ScrollPane
import javafx.scene.control.Tooltip
/**
 * Step 3 : window to add mount files :RAW files ,MZDB files and Resultfiles  .
 */
class MonutFiles extends VBox  with LazyLogging {

  private val serverConfigFileOpt =
    if (isEmpty(QuickStart.serverConfPath)) None
    else Option( new ServerConfigFile(QuickStart.serverConfPath))
  private val serverConfigOpt = serverConfigFileOpt.map(_.read()).flatten
 

  /*
   * ********** *
   * COMPONENTS *
   * ********** *
   */

 /* Mount points */
  val disableMpNoteLabel = new Label() {
    text = "Proline server configuration file must be provided to enable mount points setup.\n" +
      """See menu "Select configuration files"."""
    style = "-fx-font-style: italic;-fx-font-weigth: bold;"
    visible = false
  }
  
  /* to add rawfiles */
  val rawFilesMountPoints = ArrayBuffer[MountPointPanelObservable]()
  val rawFilesMpLabel = new BoldLabel("Raw files :", upperCase = false)
  val addRawFilesMpButton = new Button("Add") {
    onAction = handle {
       onAction = handle { 
         _addRawFilesMountPoint() 
         QuickStart.rawFiles=_getMountPointsMap(rawFilesMountPoints)
           
       }
    }
  }
  addRawFilesMpButton.setTooltip(new Tooltip("click to add raw file."))
  val rawFilesMpBox = new VBox { spacing = 10 }
  
  /* to add mzdb files */
  
  val mzdbFilesMountPoints = ArrayBuffer[MountPointPanelObservable]()
  val mzdbFilesMpLabel = new BoldLabel("mzDB files :", upperCase = false)
  val addMzdbFilesMpButton = new Button("Add") {
    onAction = handle { 
      _addMzdbFilesMountPoint()
       QuickStart.mzdbFiles=_getMountPointsMap(mzdbFilesMountPoints)
    }
  }
  addMzdbFilesMpButton.setTooltip(new Tooltip("click to add mzdb file."))
  /*to add result files */
  
  val mzdbFilesMpBox = new VBox { spacing = 10 }
  val resultFilesMountPoints = ArrayBuffer[MountPointPanelObservable]()
  val resultFilesMpLabel = new BoldLabel("Result files :", upperCase = false)
  val addResultFilesMpButton = new Button("Add") {
    onAction = handle { 
      
     _addResultFilesMountPoint()
     QuickStart.resultFiles=_getMountPointsMap(resultFilesMountPoints)
    
    }
  }
  addResultFilesMpButton.setTooltip(new Tooltip("click to add resultfile."))
  val resultFilesMpBox = new VBox { spacing = 10 }

  /*
   * ****** *
   * LAYOUT *
   * ****** *
   */

  /* Size & Resize properties */

  Seq(
    rawFilesMpLabel, mzdbFilesMpLabel, resultFilesMpLabel
  ).foreach(_.minHeight = 25)

  /* VBox & HBox spacing */
  
  private val V_SPACING = 10
  private val H_SPACING = 5

   /* Mount points */
  
  val mountPointsSettings = new TitledBorderPane(
  title = "Step 3 : add mounts points", 
  contentNode= new VBox {
    spacing = 2 * V_SPACING
    content = List(
    ScalaFxUtils.newVSpacer(minH = 5),
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
        resultFilesMpBox,
   
        ScalaFxUtils.newVSpacer(minH = 15),
        ScalaFxUtils.newVSpacer(minH = 10),
        ScalaFxUtils.newVSpacer(minH = 15),
        ScalaFxUtils.newVSpacer(minH = 10)
        
      )
  })
  val scrollPane = new ScrollPane();
  scrollPane.setContent(mountPointsSettings);
  scrollPane.setFitToWidth(true);
  val t=new VBox()
  t.getChildren().addAll(scrollPane)
  val mountPointsWithDisableNote = new VBox {
    spacing = 20
    content = List(disableMpNoteLabel, t)
  }

  /* VBox layout and content */
  
  alignment = Pos.Center
  alignmentInParent = Pos.Center
  spacing = 4 * V_SPACING
  content = List(
   ScalaFxUtils.newVSpacer(minH =1),
   mountPointsSettings
  )
  
  /* Mount points */
  
  if (serverConfigOpt.isEmpty) {

    /* Disable mount points if server config is undefined */
    disableMpNoteLabel.visible = false
    mountPointsSettings.disable = false

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
    else resultMp.foreach { case (k, v) => _addResultFilesMountPoint(k, v)
     
       }
    
  }
  

  private def _getMountPointsMap(mpArray: ArrayBuffer[MountPointPanelObservable]): Map[String, String] = {
    (
      for (
        mp <- mpArray.view;
        (k, v) = (mp.getKey, mp.getValue);
        if k.isEmpty == false && v.isEmpty == false
      ) yield k -> v //doubleQuoted(v)

    ).toMap
  }
  private def _toServerConfig() = ServerConfig(
    rawFilesMountPoints = _getMountPointsMap(rawFilesMountPoints),
    mzdbFilesMountPoints = _getMountPointsMap(mzdbFilesMountPoints),
    resultFilesMountPoints = _getMountPointsMap(resultFilesMountPoints)
  )
  
  /* Add stuff to define another raw_files mount point */
  
  private def _addRawFilesMountPoint(
    key: String = "",
    value: String = ""
  ) {
      def _onRawFileMpDelete(mp: MountPointPanelObservable): Unit = {
        rawFilesMountPoints -= mp
        rawFilesMpBox.content = rawFilesMountPoints
      }
    val mp= new MountPointPanelObservable(
      key = key,
      value = value,
      onDeleteAction = _onRawFileMpDelete,
      parentStage = QuickStart.stage   
    )
    rawFilesMountPoints +=mp
    rawFilesMpBox.content = rawFilesMountPoints
  }
  
  /* Add stuff to define another mzdb_files mount point */ 
  
  private def _addMzdbFilesMountPoint(
    key: String = "",
    value: String = ""
  ){
      def _onMzdbFileMpDelete(mp: MountPointPanelObservable): Unit = {
        mzdbFilesMountPoints -= mp
        mzdbFilesMpBox.content = mzdbFilesMountPoints
      }
      mzdbFilesMountPoints += new MountPointPanelObservable(
        key = key,
        value = value,
        onDeleteAction = _onMzdbFileMpDelete,
        parentStage = QuickStart.stage
      )
      mzdbFilesMpBox.content = mzdbFilesMountPoints

  }
   /* Add stuff to define another result_files mount point */
  
  private def _addResultFilesMountPoint(
    key: String = "",
    value: String = ""
  ){
     def _onResultFileMpDelete(mp: MountPointPanelObservable): Unit = {
       resultFilesMountPoints -= mp
       resultFilesMpBox.content = resultFilesMountPoints
     }
     resultFilesMountPoints += new MountPointPanelObservable(
       key = key,
       value = value,
       onDeleteAction = _onResultFileMpDelete,
       parentStage = QuickStart.stage
     )
     resultFilesMpBox.content = resultFilesMountPoints
   }
 }
 
class MountPointPanelObservable(
  parentStage: Stage,
  onDeleteAction: (MountPointPanelObservable) => Unit,
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
  valueField.setTooltip(new Tooltip("click on add to save the path."))
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
  browseButton.setTooltip(new Tooltip("browse the path."))
  
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


