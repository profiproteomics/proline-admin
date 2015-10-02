package fr.proline.admin.gui.process.config.postgres

import com.typesafe.scalalogging.LazyLogging

import scala.collection.mutable.ArrayBuffer

import fr.proline.admin.gui.component.dialog.PgHbaLine
import fr.proline.admin.gui.process.config._

/**
 * ****************************************************** *
 * PostgreSQL implementation : postgres.conf, pg_hba.conf *
 * ****************************************************** *
 */

class PgHbaConfigFile(val filePath: String) extends TabbedConfigFileIndexing with LazyLogging {

  /* Get indices of comment lines defining IPv4 and IPv6 addresses sections */
  private var ipv4CommentLineIndex = -1
  private var ipv6CommentLineIndex = -1
  private var firstEndingCommentsLineIndex = -1
  
  private val IPv4_LINE = "# IPv4 local connections:"
  private val IPv6_LINE = "# IPv6 local connections:"
  
  /* Define parsing pattern */
  val columnsPattern = """^(\w+)\s+([\w,]+)\s+(\w+)\s+(\S+)\s+(\w+)(.*)$"""
  //TODO: handle commented lines :: val columnsPattern = """^(#?\s*\w+)\s+([\w,]+)\s+(\w+)\s+(\S+)\s+(\w+)\s*$"""

  /** Model line with ConfigFileKVLine **/
  protected def parseLine(
    line: String,
    lineIdx: Int
  ): Option[PgHbaConnectionLine] = {

    /* First, find IPv4 comment line */
    if (ipv4CommentLineIndex < 0) {
      if (line matches s""".*$IPv4_LINE.*""") ipv4CommentLineIndex = lineIdx
      return None
    }
    
    /* Then look for 3 patterns: PgHbaConfigLine, IPv6 comment line, final comment lines */
    else {
      
      // IPv6 comment line
      if (line matches s""".*$IPv6_LINE.*""") {
        ipv6CommentLineIndex = lineIdx
        return None
      }
      
      // Final comment lines
      else if (line matches """#(?! IPv).*""") {
        if (firstEndingCommentsLineIndex < 0) firstEndingCommentsLineIndex = lineIdx
        return None
      }
      
      // PgHbaConfigLine
      else {
        val _addressType = if (ipv6CommentLineIndex < 0) AddressType.IPv4 else AddressType.IPv6

        import fr.profi.util.regex.RegexUtils._
        val matches =    line   =# (columnsPattern, groupNames = "connectionType", "databases", "user", "adress", "method", "coms" )
        matches.map  { tabMatch =>
          PgHbaConnectionLine(
            line,
            lineIdx,
            _addressType,
            ConnectionType.withName(tabMatch.group("connectionType")),
            tabMatch.group("databases"),
            tabMatch.group("user"),
            tabMatch.group("adress"),
            Method.withName(tabMatch.group("method")),
            commented = false
          )
        }
      }
    }
  }

  //    /** Get ending comment lines **/
  //  def getEndingCommentLine() : Array[String] = {
  //    this.lines.drop(firstEndingCommentsLineIndex)
  //  }

  /** Update file content and indices **/
  def updateLines(
    newIPv4Lines: Array[PgHbaLine], //"graphical line" model
    newIPv6Lines: Array[PgHbaLine]
  ): Array[String] = {

    /* Get comments before and after critical lines */
    val oldLines = this.getLines()
    val beginningCommentLines = oldLines.take(ipv4CommentLineIndex + 1)
    val endingCommentLines = oldLines.drop(firstEndingCommentsLineIndex)

    val newLines = ArrayBuffer[String]()
    newLines ++= beginningCommentLines

    /* Add IPv4  lines */
    var i = ipv4CommentLineIndex + 1
    
    newIPv4Lines.foreach{ line =>
      newLines += line.toTabbedLine()
      i += 1
    }
    
    /* Add IPv6 lines */
    ipv6CommentLineIndex = i
    newLines += IPv6_LINE
    
    newIPv6Lines.foreach{ line =>
      newLines += line.toTabbedLine()
      i += 1
    }
    
    /* Add ending comments */
    firstEndingCommentsLineIndex = i
    newLines ++= endingCommentLines
    
    /* Update and return lines */
    this.lines = newLines.result().toArray
    this.lines
  }

  def updateLines(newLines: Array[PgHbaLine]): Array[String] = {
    val ipv4Buffer = new ArrayBuffer[PgHbaLine]()
    val ipv6Buffer = new ArrayBuffer[PgHbaLine]()

    newLines.foreach { line =>
      if (line.addressType == AddressType.IPv4) ipv4Buffer += line
      else ipv6Buffer += line
    }
    
    updateLines(ipv4Buffer.result().toArray, ipv6Buffer.result().toArray)
  }

}

/**
 * ************************ *
 * Some useful enumerations *
 * ************************ *
 */
//TODO: Params

object AddressType extends Enumeration {
  val IPv4 = Value("IPv4")
  val IPv6 = Value("IPv6")
}

object ConnectionType extends Enumeration {
  val LOCAL = Value("local")
  val SOCKET = Value("socket")
  val HOST = Value("host")
  val HOST_SSL = Value("hostssl")
  val HOST_NO_SSL = Value("hostnossl")
}

object Method extends Enumeration {
  val TRUST = Value("trust")
  val REJECT = Value("reject")
  val MD5 = Value("md5")
  val PASSWORD = Value("password")
  val GSS = Value("gss")
  val SSPI = Value("sspi")
  val IDENT = Value("ident")
  val PEER = Value("peer")
  val PAM = Value("pam")
  val LDAP = Value("ldap")
  val RADIUS = Value("radius")
  val CERT = Value("cert")
}