package fr.proline.admin.gui.process.config

import java.io.File

import scala.collection.mutable.ArrayBuffer
import scala.io.Source

import fr.proline.admin.gui.process.config.postgres._



/** *********************************************** *
 * Model an informative line in configuration file, *
 * e.g. containing a key/value pair.                *
 * ********************************************** **/
case class PgHbaConnectionLine(
  line: String,
  index: Int,
  addressType: AddressType.Value,
  connectionType: ConnectionType.Value,
  database: String,
  user: String,
  addressWithCIDR: String,
  method: Method.Value,
  commented: Boolean = false
) extends ConfigFileLine {
  //TODO: REQUIREMENTS
  
  override def toString(): String = this.line
}

/**
 * ***************************************************** *
 * Trait for configuration files reading/parsing/writing *
 * Based on a tab-separated model                        *
 * ***************************************************** *
 **/

trait TabbedConfigFileIndexing {

  /* To be defined in each implementation */
  def filePath: String  
  protected def parseLine(line: String, lineIdx: Int): Option[PgHbaConnectionLine]
//  protected def parseLine(line: String, lineIdx: Int, addressType: AddressType.Value): Option[PgHbaConnectionLine]

  /** Get config file **/
  lazy private val _configFile = new File(filePath)
  
  /** Get file lines in an array **/
  var lines = getLines()
  def getLines(): Array[String] = {

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

  /** Parse lines and get connections **/
  var lineIdx = 0
  lazy val linesArray = getLines()
  lazy val linesCount = linesArray.length

  //TODO: move to correct place: pg_hba.conf -specific
  lazy val connectionLines: Array[PgHbaConnectionLine] = {

    val _connectionLines = ArrayBuffer[PgHbaConnectionLine]()

    while (lineIdx < linesCount) {
      val line = linesArray(lineIdx)
      val lineOpt = parseLine(line, lineIdx)
      
      if (lineOpt.isDefined) {
        val connectionLine = lineOpt.get
        _connectionLines += connectionLine
      }

      lineIdx += 1
    }
    _connectionLines.result().toArray
  }

  /** String updated to be written in file **/
  override def toString(): String = synchronized {    
    connectionLines.mkString("\n")
  }

}