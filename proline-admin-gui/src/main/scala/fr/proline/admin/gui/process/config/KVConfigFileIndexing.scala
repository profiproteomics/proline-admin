package fr.proline.admin.gui.process.config

import java.io.File

import scala.collection.mutable.HashMap
import scala.io.Source

import fr.proline.admin.gui.process.config.postgres._
import fr.profi.util.StringUtils.LINE_SEPARATOR

/**
 * *********************************************** *
 * Model an informative line in configuration file, *
 * e.g. containing a key/value pair.                *
 * ********************************************** *
 */

trait ConfigFileLine {

  val line: String
  val index: Int
  val commented: Boolean

  //def toNewLine
}

case class ConfigFileKVLine(
  line: String,
  index: Int,
  key: String,
  var valueString: String,
  valueStartIdx: Int,
  valueEndIdx: Int,
  commented: Boolean = false) extends ConfigFileLine {

  /*private var valueStartIdx: Int = -1
  private var valueEndIdx: Int = -1
  
  var value: String = {
    val localizedValue = valueExtractor(line)
    valueStartIdx = localizedValue._2
    valueEndIdx = localizedValue._3
    localizedValue._1
  }*/

  /** Create a new KVLine, corresponding to this one, commented **/
  def comment(): ConfigFileKVLine = {

    //require(this.commented == false, "Can't comment a line that is already commented")
    if (this.commented) this
    else ConfigFileKVLine(
      '#' + line,
      index,
      key,
      valueString,
      valueStartIdx + 1,
      valueEndIdx + 1,
      commented = true)
  }
  /** Create a new KVLine, corresponding to this one, UNcommented **/
  def uncomment(): ConfigFileKVLine = {

    //require(this.commented, "Can't uncomment a line that is already uncommented")
    if (this.commented == false) this
    else ConfigFileKVLine(
      line.drop(1),
      index,
      key,
      valueString,
      valueStartIdx - 1,
      valueEndIdx - 1,
      commented = true)
  }

  /** Update value string and related fields: line & end idx. Return a new object **/
  def toNewKVLine(newValueString: String, commented: Boolean = false): ConfigFileKVLine = {

    //Comment or uncomment line if needed
    val oldKVLine = {
      if (commented) this.comment()
      else this.uncomment()
    }
    val oldLine = oldKVLine.line
    val oldValueStartIdx = oldKVLine.valueStartIdx

    // Build line from start to value end
    val sb = new StringBuilder()
    sb ++= oldLine.substring(0, oldValueStartIdx)
    sb ++= newValueString

    // Optionaly add some comments or whitespaces after value
    val lineLen = oldLine.length
    if (lineLen > oldKVLine.valueEndIdx) {
      sb ++= oldLine.substring(oldKVLine.valueEndIdx, lineLen)
    }

    // New line features
    val newLine = sb.result()
    val newValueEndIdx = oldValueStartIdx + newValueString.length - 1

    // New line
    ConfigFileKVLine(
      newLine,
      index,
      key,
      newValueString,
      oldKVLine.valueStartIdx,
      newValueEndIdx,
      commented)
  }

  //  override def toString(): String = {
  //    if(valueStartIdx == -1 || valueEndIdx == -1) line
  //    else {
  //      _toString(line, valueString, valueEndIdx)
  //    } 
  //  }
  override def toString(): String = this.line

  /*
  private def _buildLine(line: String, valueString: String): String = {
    println("oldLineWithCommentOpt: "+line)
    val lineLen = line.length
    println("lineLen: " + lineLen)
    val sb = new StringBuilder()
    println("line.substring(0, valueStartIdx): " + line.substring(0, valueStartIdx))
    sb ++= line.substring(0, valueStartIdx)
    println("valueString: " + valueString)
    sb ++= valueString

    //val lastCharIdx = line.length -1

    if (lineLen > valueEndIdx) {
      println("line.substring(valueEndIdx, lineLen): " + line.substring(valueEndIdx, lineLen))
      sb ++= line.substring(valueEndIdx, lineLen)
    }
    println("result: "+sb.result())
    sb.result()
  }*/

}

/**
 * ***************************************************** *
 * Trait for configuration files reading/parsing/writing *
 * Based on a key-value model                            *
 * ***************************************************** *
 */

trait KVConfigFileIndexing {

  /* To be defined in each implementation */
  def filePath: String
  //def paramKeys: Array[String]
  protected def parseLine(line: String, lineIdx: Int): Option[ConfigFileKVLine]

  /** Get config file **/
  lazy private val _configFile = new File(filePath)

  /** Get file lines in an array **/
  lazy val lines: Array[String] = {

    require(_configFile.exists(), "No file exists at given path: " + filePath)

    val src = Source.fromFile(_configFile)

    try {
      src.getLines().toArray
    } catch {
      case t: Throwable => Array()
    } finally {
      src.close()
    }
  }

  // TODO: trait + implems

  var lineIdx = 0
  lazy val linesArray = lines
  lazy val linesCount = linesArray.length

  /** Parse lines and get information by key **/
  lazy val lineByKey: HashMap[String, ConfigFileKVLine] = {

    // TODO: require(KVLine)

    val lineByKey = HashMap[String, ConfigFileKVLine]()

    while (lineIdx < linesCount) {
      val line = linesArray(lineIdx)
      val lineOpt = parseLine(line, lineIdx)

      if (lineOpt.isDefined) {
        val kvLine = lineOpt.get
        lineByKey += kvLine.key -> kvLine
      }

      lineIdx += 1
    }

    //println("Found " + lineByKey.size + " KV lines in file")
    //println("-> " + lineByKey.values.filter(_.commented).size + " are commented")
    lineByKey
  }

  /** String updated to be written in file **/
  override def toString(): String = synchronized {

    // Update lines array
    for (kvLine <- lineByKey.values) {
      lines(kvLine.index) = kvLine.toString()
    }

    lines.mkString(LINE_SEPARATOR)
  }

}