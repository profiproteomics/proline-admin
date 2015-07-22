package fr.profi.util.scalafx

import com.typesafe.scalalogging.slf4j.Logging

import scalafx.Includes._
import scalafx.scene.control.TextField
import scalafx.scene.input.KeyEvent

/**
 * ****************************************************** *
 * NumericTextField :                                     *
 * TextField accepting numbers(interger or decimal) only  *
 * (obsolete when Spinner is ok, scalafx-8.0.40-SNAPSHOT) *
 * ****************************************************** *
 */
class NumericTextField extends TextField with Logging{

  /** Secondary constructor **/
  def this(bigDecimal: BigDecimal) = {
    this
    this.setValue(bigDecimal)
  }

  /** Choose if only integers shall be taken **/
  private var integerOnly = false
  def setIntergersOnly(bool: Boolean) { integerOnly = bool }

  /** Optionally set min and max **/
  private var minValue: BigDecimal = BigDecimal(Double.MinValue)
  def setMinValue(bigD: BigDecimal) {
    if (integerOnly) require(bigD.isValidLong, s"Invalid minValue for IntegerTextField ($bigD)")
    minValue = bigD
  }

  private var maxValue: BigDecimal = BigDecimal(Double.MaxValue)
  def setMaxValue(bigD: BigDecimal) {
    if (integerOnly) require(bigD.isValidLong, s"Invalid maxValue for IntegerTextField ($bigD)")
    maxValue = bigD
  }

  /** Allow only integers / decimals **/
  onKeyTyped = (ke: KeyEvent) => {

    val char = ke.character
    val charIsNumber = char matches ("""\d""")
    val charIsDot = char == "."
    val dotInCurrentText = text() matches (""".*\..*""")

    /* Allow decimals */
    if (charIsNumber == false) {

      /* If not integer only, allow one '.' or ',' */
      if (charIsDot && integerOnly == false) {
        if (dotInCurrentText) ke.consume()
      } else {
        ke.consume()
      }
    }
  }

  /** Parse String into Int / BigDecimal **/
  def getInt(): Int = {
    val res = text().toInt
    _testRequirements(res)
    res
  }

  def getLong(): Long = {
    val res = text().toLong
    _testRequirements(res)
    res
  }

  def getBigDecimal(): BigDecimal = {
    val res = BigDecimal(text())
    _testRequirements(res)
    res
  }

  /** Set value **/
  def setValue(int: Int): Unit = {
    _testRequirements(BigDecimal(int))
    text = int.toString()
  }

  def setValue(long: Long): Unit = {
    _testRequirements(BigDecimal(long))
    text = long.toString()
  }

  def setValue(bigDecimal: Option[BigDecimal]): Unit = {
    if (bigDecimal.isEmpty) {
      text = ""

    } else {
      val bigD = bigDecimal.get
      if (integerOnly) setValue(bigD.toLong)
      else {
        _testRequirements(bigD)
        text = bigD.toString()
      }
    }
  }

  def setValue(bigDecimal: BigDecimal): Unit = setValue(Option(bigDecimal))

  def setValue(string: String): Unit = {
    try {
      val bigD = if (string.isEmpty()) None else Some(BigDecimal(string))
      setValue(bigD)
    } catch {
      case e: Exception => logger.trace(e.getMessage())
    }
  }

  /** Test requirements on min and max values **/
  private def _testRequirements(bigD: BigDecimal): Unit = {
    require(bigD >= minValue, s"Given value ($bigD) is inferior to min value ($minValue).")
    require(bigD <= maxValue, s"Given value ($bigD) is superior to max value ($maxValue).")
  }
}

/**
 * ****************************************************************** *
 *  Extension of NumericTextField, accepting integers (Int/Long) only *
 * ****************************************************************** *
 */
class IntegerTextField extends NumericTextField {
  
  setIntergersOnly(true)
  setMinValue(BigDecimal(Long.MinValue))
  setMaxValue(BigDecimal(Long.MaxValue))

  /** Secondary constructors **/
  def this(int: Int) = {
    this
    this.setValue(int)
  }

  def this(long: Long) = {
    this
    this.setValue(long)
  }
}