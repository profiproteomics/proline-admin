package fr.proline.admin.gui.process.config

import fr.profi.util.scala.ByteUtils._

/**
 * ********************************************** *
 * Model extreme and suggested values for a field *
 * ********************************************** *
 */
/*trait IConfigParamValueRange[@specialized(Float,Long) T] {
  val minValue: T
  val defaultValue: T
  val suggestedValue: T
  val maxValue: T
}*/

/*abstract class AbstractConfigParamValueRange {
  def this
}*/

case class ConfigParamValueRange(
  minValue: BigDecimal,
  defaultValue: BigDecimal,
  suggestedValue: BigDecimal,
  maxValue: BigDecimal
) {
  
  def this(minValue: String, defaultValue: String, suggestedValue: String, maxValue: String) = {
    this(
      BigDecimal(parseBytesAmount(minValue)._1),
      BigDecimal(parseBytesAmount(defaultValue)._1),
      BigDecimal(parseBytesAmount(suggestedValue)._1),
      BigDecimal(parseBytesAmount(maxValue)._1)
    )
  }
  
  def this(minValue: Double, defaultValue: Double, suggestedValue: Double, maxValue: Double) = {
    this( BigDecimal(minValue), BigDecimal(defaultValue), BigDecimal(suggestedValue), BigDecimal(maxValue) )
  }
  
}

/*
case class ConfigFloatParamValueRange(
  minValue: Float,
  defaultValue: Float,
  suggestedValue: Float,
  maxValue: Float
) extends IConfigParamValueRange[Float] {

  def this(minValue: Double, defaultValue: Double, suggestedValue: Double, maxValue: Double) = {
    this( minValue.toFloat, defaultValue.toFloat, suggestedValue.toFloat, maxValue.toFloat)
  }
  
}*/

/*case class ConfigStringParamValueRange(
  minValue: String,
  defaultValue: String,
  suggestedValue: String,
  maxValue: String
//) extends IConfigParamValueRange
) extends IConfigParamValueRange[String]*/
