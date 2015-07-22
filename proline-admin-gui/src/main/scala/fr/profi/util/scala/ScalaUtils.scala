package fr.profi.util.scala

import java.io.File

/**
 * ********************************* *
 * Some utilities to play with Scala *
 * ********************************* *
 **/

object ScalaUtils {

  /** Compute if a string is null-or-empty **/
  def isEmpty(stringOpt: Option[String]): Boolean = (
    stringOpt == null || stringOpt.isEmpty || (stringOpt.isDefined && stringOpt.get.isEmpty())
  )

  /** Compute if a string is null-or-empty **/
  def isEmpty(string: String): Boolean = (
    string == null || string.isEmpty
  )
  
  /** Double back slashes in file path **/
  def doubleBackSlashes(str: String): String = {
    str.replaceAll("""\\""", """\\\\""")
  }

  /** Find best match in a collection **/
  //TODO
  //use minBy( math.abs(diff) ) 
  
  /** Get file extensions */
  //from PWX-Common FileUtils
  //TODO : remove me
  def getFileExtension(fileName: String): String = {
    val lastIndexOfDot = fileName.lastIndexOf(".")
    if (lastIndexOfDot == -1) {
      return "" // empty extension
    }

    fileName.substring(lastIndexOfDot + 1)
  }

  def getFileExtension(file: File): String = {
    this.getFileExtension(file.getName)
  }

  /** Double-quoted string **/
  def doubleQuoted(string: String): String = if (isDoubleQuoted(string)) string else s""""$string""""

  def isDoubleQuoted(string: String): Boolean = {
    //hard to read but ultra-efficient
    string.charAt(0) == '"' && string.charAt(string.length - 1) == '"'
  }


  /**
   * ********* *
   * IMPLICITS *
   * ********* *
   */

  /** Get string, potentially empty, rather than option **/
  implicit def stringOpt2string(strOpt: Option[String]): String = strOpt.getOrElse("")

  
}