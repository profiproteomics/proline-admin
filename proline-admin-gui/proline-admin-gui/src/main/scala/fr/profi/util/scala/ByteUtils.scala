package fr.profi.util.scala

import fr.profi.util.primitives.toDouble

/**
 * ************************************************** *
 * Some methods to handle byte amounts                *
 * (read, write, get unit, get most readable form...) *
 * ************************************************** *
 **/

object ByteUtils {
  
  
  /**
   * Human readable string to display large amounts of bytes
   * i.e. 4288840704 (Long) -> "4.3MB" (String)
   */
  // Inspired by 
  // http://stackoverflow.com/questions/3758606/how-to-convert-byte-size-into-human-readable-format-in-java
  // and 
  // http://stackoverflow.com/questions/3263892/format-file-size-as-mb-gb-etc
  def formatBytesAmount(
    bytesAmount: Long,
    numberFormat: String = "%.1f%s",
    locale: java.util.Locale = java.util.Locale.US,
    siBase: Boolean = false
  ): String = {
    
    require( bytesAmount >= 0, "Invalid bytes amount: " + bytesAmount)

    val base = if (siBase) 1000 else 1024
    if (bytesAmount < base) return numberFormat.formatLocal(locale, bytesAmount, 'B')
    
    //val exponent = (Math.log(bytesAmount) / Math.log(base)).toInt
    //val fractionalCoeff = bytesAmount / Math.pow(base, exponent)
    val exponent = (63 - java.lang.Long.numberOfLeadingZeros(bytesAmount))  / 10
    val fractionalCoeff = bytesAmount.toDouble / (1L << (exponent * 10))
    val unit = new String( Array( (if (siBase) "kMGTPE" else "KMGTPE").charAt(exponent - 1), 'B' ) )

    numberFormat.formatLocal(locale, fractionalCoeff, unit)
  }
//
//  def formatBytesAmountInt(
//    bytesAmount: Long,
//    numberFormat: String = "%.0f%s",
//    siBase: Boolean = false
//  ): String = {
//
//    val (asDouble, unit) = _geFittedtValueAndUnit(bytesAmount, siBase)
//    val intDigitsCount = asDouble.toInt.toString().length()
//    
//    if (intDigitsCount > 2)
//    ""
//  }
//
//  private def _geFittedtValueAndUnit(
//    bytesAmount: Long,
//    siBase: Boolean = false
//  ): (Double, ByteUnit.Value) = {
//    
//    require(bytesAmount >= 0, "Invalid bytes amount: " + bytesAmount)
//
//    val base = if (siBase) 1000 else 1024
//    if (bytesAmount < base) return (bytesAmount, ByteUnit.B)
//    
//    //val exponent = (Math.log(bytesAmount) / Math.log(base)).toInt
//    //val fractionalCoeff = bytesAmount / Math.pow(base, exponent)
//    val exponent = (63 - java.lang.Long.numberOfLeadingZeros(bytesAmount))  / 10
//    val fractionalCoeff = bytesAmount.toDouble / (1L << (exponent * 10))
//    val unit = new String( Array( (if (siBase) "kMGTPE" else "KMGTPE").charAt(exponent - 1), 'B' ) )
//
//    (fractionalCoeff, ByteUnit.withName(unit.toUpperCase()))
//  }

  /** Get the number of bytes from a number of kB, MB, Gb, ... **/
  /*def getBytesAmount(value: Double, unit: ByteUnit.Value): Long = {
    //val valueAsLong = math.round(value) //Better than toLong: round to nearest integer, not to next.
    val valueAsLong = toLong(value)
//    println("valueAsLong: " + valueAsLong)
//    println("unit: " + unit)
    getBytesAmount(valueAsLong, unit)
  }*/
  
  def getBytesAmount(value: Double, unit: ByteUnit.Value): Long = {
//    println("in getBytesAmount")

    val bytesAmountAsDouble = unit match {
      case ByteUnit.B  => value
      case ByteUnit.KB => value kB
      case ByteUnit.MB => value MB
      case ByteUnit.GB => value GB
      case ByteUnit.TB => value TB
      case _           => throw new Exception("Unknown unit: " + unit)
    }
    
    math.round(bytesAmountAsDouble)
  }
  

  /** Get value and unit from string representing a byte amount **/
  def parseBytesAmount(
    bytesAmountAsStr: String, 
    defaultUnit: ByteUnit.Value = ByteUnit.KB
  ): (BigDecimal, ByteUnit.Value) = {
    
    val pattern = """^([\d.]+)\s?(\w+)""".r

    bytesAmountAsStr match {
      case pattern(valueString, unitString) => {
        //        println("valueString: "+valueString)
        //        println("unitString: "+unitString)

        //        val valueAsDouble = toDouble(valueString)
        val unitValue: ByteUnit.Value = {

          if (unitString.isEmpty()) defaultUnit
          else {

            //TODO. Simplify using withName?
            require(unitString.last.toUpper == 'B', s"Unit string ('$unitString') must finish by the character 'B', no matter the case ")
            unitString.head.toUpper match {
              case 'B' => ByteUnit.B
              case 'K' => ByteUnit.KB
              case 'M' => ByteUnit.MB
              case 'G' => ByteUnit.GB
              case 'T' => ByteUnit.TB
              case _   => throw new Exception (s"Unit string ('$unitString') must begin by one of the following characters, no matter the case: B, K, M, G, T.")
            }

          }
        }
        
        //(toLong(value), unitValue)
        //(math.round(valueAsDouble), unitValue)
        (BigDecimal(valueString), unitValue)
      }
      case _ => throw new Exception(s"Unable to read bytes amount in value '$bytesAmountAsStr'")
    }
  }

  /**
   * ******** *
   * WRAPPERS *
   * ******** *
   */
  
  /** Work with byte amounts **/
  object ByteUnit extends Enumeration {
    val B = Value("B")
    val KB = Value("kB")
    val MB = Value("MB")
    val GB = Value("GB")
    val TB = Value("TB")
  }
  
  object BytesAmountWrapper {
    val kbCoeff = 1024
    val mbCoeff = kbCoeff * 1024
    val gbCoeff = mbCoeff * 1024
    val tbCoeff = gbCoeff * 1024
  }
  
  // Extending AnyVal allows to create a "Value Class" (only primitives) which does not instantiate object at runtime
  implicit class BytesAmountWrapper( val bytesAmount: Double ) extends AnyVal {
    def kB(): Double = bytesAmount * BytesAmountWrapper.kbCoeff
    def MB(): Double = bytesAmount * BytesAmountWrapper.mbCoeff
    def GB(): Double = bytesAmount * BytesAmountWrapper.gbCoeff
    def TB(): Double = bytesAmount * BytesAmountWrapper.tbCoeff
  }

}