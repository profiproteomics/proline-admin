package fr.profi.util.scalafx

import com.typesafe.scalalogging.LazyLogging
import scala.reflect.ClassTag
import scalafx.Includes._
import scalafx.collections.ObservableBuffer
import scalafx.scene.control.Button
import scalafx.scene.control.ComboBox
import scalafx.scene.control.TableView
import scalafx.scene.control.TextField
import scalafx.scene.image.Image
import scalafx.scene.image.ImageView
import scalafx.scene.input.KeyCode
import scalafx.scene.input.KeyEvent
import scalafx.scene.layout.GridPane
import scalafx.scene.layout.Priority
import scalafx.scene.layout.Region
import scalafx.stage.Stage
import scalafx.scene.control.TableColumn
import scalafx.scene.Node

object ScalaFxUtils extends LazyLogging {

  /** Vertical & horizontal spacers */
  def newHSpacer(
    minW: Double = Double.MinPositiveValue,
    maxW: Double = Double.MaxValue) = new Region {
    hgrow = Priority.Always
    minWidth = minW
    maxWidth = maxW
  }

  def newHSpacer(w: Double) = new Region {
    minWidth = w
    maxWidth = w
  }

  def newVSpacer(
    minH: Double = Double.MinPositiveValue,
    maxH: Double = Double.MaxValue) = new Region {
    vgrow = Priority.Always
    minHeight = minH
    maxHeight = maxH
  }

  def newVSpacer(h: Double) = new Region {
    minHeight = h
    maxHeight = h
  }

  /** Close a stage, fire a button **/
  def closeIfEscapePressed(stage: Stage, ke: KeyEvent) {
    if (ke.code == KeyCode.ESCAPE) stage.close()
  }
  def fireIfEnterPressed(button: Button, ke: KeyEvent) {
    if (ke.code == KeyCode.ENTER) button.fire()
  }

  /** Grid content formatting utilities **/
  def getFormattedGridContent3(seq: Seq[Tuple3[scalafx.scene.Node, Int, Int]]): Seq[scalafx.scene.Node] = {
    seq.map {
      case (child, col, row) =>
        GridPane.setConstraints(child, col, row)
        child
    }.distinct
  }
  def getFormattedGridContent5(seq: Seq[Tuple5[scalafx.scene.Node, Int, Int, Int, Int]]): Seq[scalafx.scene.Node] = {
    seq.map {
      case (child, col, row, colSpan, rowSpan) =>
        GridPane.setConstraints(child, col, row, colSpan, rowSpan)
        child
    }.distinct
  }

  /** Get new image / image view from enumeration value as path */
  def newImageView(path: String): ImageView = {
    new ImageView(newImage(path))
  }
  def newImage(path: String): Image = {
    new Image(this.getClass().getResourceAsStream(path.toString()))
  }

  /**
   * ********* *
   * IMPLICITS *
   * ********* *
   */

  /** Get text field content as string or integer option **/
  implicit def getComboBoxSelectedItem[T](cb: ComboBox[T]): T = cb.selectionModel().selectedItem()
  implicit def textField2String(txtField: TextField): String = txtField.text()

  implicit def textField2StringOpt(txtField: TextField, allowEmpty: Boolean = false): Option[String] = {
    val str = txtField.text()
    if (allowEmpty) Some(str)
    else {
      if (str.isEmpty()) None else Some(str)
    }
  }

  implicit def numTextField2Int(numTxtField: NumericTextField): Int = numTxtField.getInt()

  implicit def numTextField2IntOpt(numTxtField: NumericTextField): Option[Int] = {
    try { Some(numTxtField.getInt()) }
    catch {
      case t: Throwable => {
        logger.trace("Unable to get Int from empty text field")
        None
      }
    }
  }

  /** Conversions from/to ObservableBuffer **/
  implicit def seq2obsBuff[T](seq: Seq[T]): ObservableBuffer[T] = {
    if (seq == null) null
    else ObservableBuffer.from(seq)
  }
  implicit def seq2array[T: ClassTag](seq: Seq[T]): Array[T] = {
    Array[T](seq: _*)
  }
  implicit def array2seq[T: ClassTag](arr: Array[T]): Seq[T] = {
    val s = Seq.empty[T]
    s ++= (arr: scala.collection.mutable.ArrayOps[T]).toSeq

  }
  implicit def array2obsBuff[T: ClassTag](arr: Array[T]): ObservableBuffer[T] = {
    array2seq(arr) //array to seq to obs buff
  }
  implicit def iterable2seq[T: ClassTag](it: Iterable[T]): Seq[T] = {
    it.toSeq
  }

  /**
   * ******** *
   * WRAPPERS *
   * ******** *
   */
  implicit class EnhancedComboBox[T](cb: ComboBox[T]) {

    /** Same as select[T], but get rid of ambiguity when T is Int: select(index: Int) **/
    def selectItem(item: T) {
      cb.selectionModel().select(item)
    }
  }

  implicit class EnhancedTableView[T](tv: TableView[T]) {

    /** Apply width to columns as a percentage of table width **/
    def applyPercentWidth(columnsWithPercent: List[(TableColumn[T, _], Int)]) {
      columnsWithPercent.foreach {
        case (col, percent) =>
          if (percent < 0) col.prefWidth = 30 //default
          else col.prefWidth <== tv.width * percent / 100
      }
    }
  }

  /**
   * ****** *
   * STYLES *
   * ****** *
   */
  object TextStyle {
    val ITALIC = "-fx-font-style: italic;"

    val BLUE_HYPERLINK = "-fx-color:#66CCFF;"
    val BLUE = "-fx-text-fill: mediumblue;"
    val RED = "-fx-text-fill: red;"
    val GREY = "-fx-text-fill: grey;"
    val ORANGE = "-fx-text-fill: orange;"
    val GREEN = "-fx-text-fill: green;"

    val RED_ITALIC = RED ++ ITALIC
    val ORANGE_ITALIC = ORANGE ++ ITALIC
    val GREY_ITALIC = GREY ++ ITALIC
    val BLUE_ITALIC = BLUE ++ ITALIC
    val GREEN_ITALIC = GREEN ++ ITALIC
  }

  object NodeStyle {

    def set(field: Node) {
      field.setStyle("-fx-text-box-border: red  ; -fx-focus-color: red ;")
    }
    def remove(field: Node) {
      field.setStyle("")
    }
    def hide(label: Node) {
      label.visible = false
    }
    def show(label: Node) {
      label.visible = true
    }
  }
}