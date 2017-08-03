package fr.profi.util.scala

import java.io.File
import scala.runtime.ScalaRunTime
import java.nio.file.Files

/**
 * ********************************* *
 * Some utilities to play with Scala *
 * ********************************* *
 */

object ScalaUtils {

  /** Compute if a string is null-or-empty **/
  def isEmpty(stringOpt: Option[String]): Boolean = (
    stringOpt == null || stringOpt.isEmpty || (stringOpt.isDefined && stringOpt.get.isEmpty()))

  /** Compute if a string is null-or-empty **/
  def isEmpty(string: String): Boolean = (
    string == null || string.isEmpty)

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

  /** Create backup file **/
  def createBackupFile(file: File): java.nio.file.Path = synchronized {
    require(file.exists(), "File doesn't exist")

    val fileName = file.getName()
    val dir = file.getParentFile()

    /* Get all files in dir that that are the given file or a backup */
    val regex = s"""^$fileName(\\.\\d+\\.bak)?$$""".r
    val filteredFileNames = dir.listFiles().filter(f => regex.findFirstIn(f.getName).isDefined)
    val filteredNamesLen = filteredFileNames.length
    require(filteredNamesLen > 0, "At least provided file should match regex: " + regex)

    /* Copy current file with i.bak extension ( i = backup index ) */
    val backupFile = new File(file.getPath() + s".$filteredNamesLen.bak") // nextBackupIdx = filteredNamesLen

    import java.nio.file._
    Files.copy(
      file.toPath(),
      backupFile.toPath(),
      StandardCopyOption.REPLACE_EXISTING,
      StandardCopyOption.COPY_ATTRIBUTES,
      LinkOption.NOFOLLOW_LINKS)
  }

  /** Double-quoted string **/
  def doubleQuoted(string: String): String = if (isDoubleQuoted(string)) string else s""""$string""""

  def isDoubleQuoted(string: String): Boolean = {
    //hard to read but ultra-efficient
    string.charAt(0) == '"' && string.charAt(string.length - 1) == '"'
  }

  /** check is valid configuration file **/
  def isConfFile(filePath: String): Boolean = (new File(filePath).exists) && (new File(filePath).getName == "application.conf")

  /**
   * ********* *
   * IMPLICITS *
   * ********* *
   */

  /** Get string, potentially empty, rather than option **/
  implicit def stringOpt2string(strOpt: Option[String]): String = strOpt.getOrElse("")

}