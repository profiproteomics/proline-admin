package fr.proline.admin.gui.component.configuration.form

import com.typesafe.scalalogging.LazyLogging

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.collection.mutable.ListBuffer
import scala.util.control.Breaks._

import java.io.File
import java.io.FileWriter

import scalafx.Includes._
import scalafx.beans.property.LongProperty
import scalafx.geometry.Insets
import scalafx.geometry.Pos
import scalafx.scene.Cursor
import scalafx.scene.Node
import scalafx.scene.control.Button
import scalafx.scene.control.CheckBox
import scalafx.scene.control.ComboBox
import scalafx.scene.control.Label
import scalafx.scene.control.Tooltip
import scalafx.scene.layout.GridPane
import scalafx.scene.layout.HBox
import scalafx.scene.layout.StackPane
import scalafx.scene.layout.VBox
import scalafx.stage.Stage

import fr.proline.admin.gui.IconResource
import fr.proline.admin.gui.Main
import fr.proline.admin.gui.process.config.ConfigFileKVLine
import fr.proline.admin.gui.process.config.postgres._
import fr.proline.admin.gui.process.config.postgres.PgParamType._
import fr.proline.admin.gui.util.ConfigParamSlider
import fr.proline.admin.gui.util.ShowPopupWindow

import fr.profi.util.StringUtils.LINE_SEPARATOR
import fr.profi.util.primitives._
import fr.profi.util.scala.ByteUtils._
import fr.profi.util.scala.ScalaUtils
import fr.profi.util.scalafx.IntegerTextField
import fr.profi.util.scalafx.NumericTextField
import fr.profi.util.scalafx.ScalaFxUtils
import fr.profi.util.scalafx.ScalaFxUtils.seq2obsBuff

/**
 * *********************************** *
 * All components for one Config param *
 * *********************************** *
 */
//case class PgFormLine(
//  checkBox: CheckBox,
//  nameLabel: Label,
//  slider: ConfigParamSlider,
//  valueNode: Node
//)

/**
 * ***************************************************************** *
 * Form to edit and update PostgreSQL configuration (postresql.conf) *
 * ***************************************************************** *
 */
class SeqRepoConfigForm() extends VBox with IConfigFilesForm with LazyLogging {
  //class SeqRepoConfigForm(SeqRepoConfigFilePath: String)(implicit val parentStage: Stage) extends VBox with IConfigFilesForm with LazyLogging {

  def checkForm(): Boolean = {
    //TODO
    true
  }

  def saveForm() {

  }

}