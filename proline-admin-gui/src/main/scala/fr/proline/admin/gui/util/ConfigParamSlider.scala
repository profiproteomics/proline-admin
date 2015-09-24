package fr.proline.admin.gui.util

import com.typesafe.scalalogging.LazyLogging

import scalafx.scene.control.Slider
import scalafx.util.StringConverter

import fr.proline.admin.gui.process.config.ConfigParamValueRange

import SliderTickName._


/** 
 *  ***************************** *
 *  Enumerate Slider ticks' names *
 *  ***************************** *
 **/
object SliderTickName {
  val MIN = "min"
  val DEFAULT = "default"
  val SUGGESTED = "suggested"
  val MAX = "max"
}


/**
 * ***************************************************************** *
 * Create and parametrize a slider for a given ConfigParamValueRange *
 * ***************************************************************** *
 **/
/*class ConfigParamSlider[@specialized(Float,Long) T <: Numeric[T]](configParamValueRange: IConfigParamValueRange[T])(implicit num: Numeric[T]) extends Slider {
  min = configParamValueRange.minValue
  max = configParamValueRange.maxValue
  
  implicit def num2Double(d: Numeric[T]): Double = d
}*/

class ConfigParamSlider(val configParamValueRange: ConfigParamValueRange) extends Slider with LazyLogging {

  /* Get remarkable values as Double, since slider value is a Double */ 
  val (minValue, suggestedValue, defaultValue, maxValue) = (
    configParamValueRange.minValue.toDouble,
    configParamValueRange.suggestedValue.toDouble,
    configParamValueRange.defaultValue.toDouble,
    configParamValueRange.maxValue.toDouble
  )

  /* Set min and max */
  min = minValue
  max = maxValue

  /* Define ticks: make sure suggested value corresponds to a tick */
  showTickLabels = true
  showTickMarks = true
  minorTickCount = 0
  majorTickUnit = (suggestedValue - minValue).abs
  // WARNING: no tick for default if default < suggested

  labelFormatter = new StringConverter[Double] {

    /** Set tick name given its value **/
    def toString(d: Double): String = {
      if (d == suggestedValue) "*" //SUGGESTED. Use '*' so that it's displayed even if both blockIncrement and Stage are thin
      else ""
    }

    /** Set tick value given its name **/
    def fromString(string: String): Double = string match {
      case MIN       => minValue
      case DEFAULT   => defaultValue
      case SUGGESTED => suggestedValue
      case MAX       => maxValue
    }
  }

}