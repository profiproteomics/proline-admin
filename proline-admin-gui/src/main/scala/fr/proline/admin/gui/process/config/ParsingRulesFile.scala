package fr.proline.admin.gui.process.config

import java.io.{File, FileWriter}

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import fr.profi.util.StringUtils.LINE_SEPARATOR
import fr.profi.util.scala.ScalaUtils
import fr.profi.util.scala.TypesafeConfigWrapper._

import scala.collection.JavaConversions._
import scala.collection.mutable.{ListBuffer, StringBuilder}

/**
 * ParsingRulesFile Build a model to update, to add or to remove parsing rules from sequence repository parsing-rules.conf file.
 * @param path The path of the sequence repository parsing-rules.conf file.
 *
 * @author aromdhani
 *
 */

class ParsingRulesFile(val path: String) extends LazyLogging {

  require(path != null && path.isEmpty() == false, "Parsing Rules file must not be null nor empty")
  private val parsingRulesFile = new File(path)

  /** Read parsing-rules.conf  file **/

  def read() = {
    try {
      val parsing = ConfigFactory.parseFile(parsingRulesFile)

      /* Retrieve local Fasta directories  */

      def getLocalFastaDirectories(): ListBuffer[String] = {
        var listFastaDir: ListBuffer[String] = ListBuffer()
        val listLocalFastaDirs = parsing.getStringList("local-fasta-directories")
        if ((listLocalFastaDirs != null) && (!listLocalFastaDirs.isEmpty)) {
          listLocalFastaDirs.foreach(dir =>
            listFastaDir += dir)
        } else {
          logger.warn("Local Fasta Directories must not be empty")
        }
        listFastaDir
      }

      /* Retrieve parsing rules  */

      def getParsingRules(): ListBuffer[Rule] = {
        var parsingRulesList: ListBuffer[Rule] = ListBuffer()
        val parsingList = parsing.getObjectList("parsing-rules")
        if ((parsingList != null) && (!parsingList.isEmpty)) {
          parsing.getObjectList("parsing-rules").foreach(
            rule => {
              var listfastaNames: ListBuffer[String] = ListBuffer()
              val name = rule.toConfig.getString("name")
              val fastaVersion = rule.toConfig.getString("fasta-version")
              val fastaNames = rule.toConfig.getStringList("fasta-name")
              val proteinAccession = rule.toConfig.getString("protein-accession")
              if ((fastaNames != null) && (!fastaNames.isEmpty)) { fastaNames.foreach(fastName => listfastaNames += fastName) }
              else {
                logger.warn("Fasta Names must not be empty")
              }
              // Return parsing rule as Instance of Rule 
              parsingRulesList += Rule(name, listfastaNames, fastaVersion, proteinAccession)
            })
        } else {
          logger.warn("Parsing Rules msut not be empty")
        }
        parsingRulesList
      }

      /* Retrieve default protein accession */

      def getDefaultProteinAccession(): Option[String] = {
        parsing.getStringOpt("default-protein-accession")
      }

      /* Return Parsing Rule  */
      Some(
        ParsingRule(
          localFastaDirectories = getLocalFastaDirectories(),
          parsingRules = getParsingRules(),
          defaultProteinAccession = getDefaultProteinAccession()))

    } catch {
      case t: Throwable =>
        logger.error("Error occurated while reading parsing rule file", t)
        None
    }
  }

  /** Write parsing-rule.conf file **/

  def write(parsingRule: ParsingRule): Unit = synchronized {

    /* write in template in model of parsing rule */

    val parsingRuleTemplate = s"""
//Specify path to fasta files for SeqRepository daemon. Multiple path separated by ',' between []
//local-fasta-directories =["${ScalaUtils.doubleBackSlashes("S:\\sequence")}"] 
local-fasta-directories =[ ${parsingRule.toTypeSafeFastaDirString()} ]

// Rules used for parsing fasta entries. Multiple rules could be specified.
// name : identifying rule definition
// fasta-name : FASTA file name must match specified Java Regex CASE_INSENSITIVE. multiple Regex separated by ',' between []
// fasta-version : Java Regex with capturing group for fasta release version string (CASE_INSENSITIVE)
// protein-accession : Java Regex with capturing group for protein accession
parsing-rules = ${parsingRule.toTypeSafeParsingRulesString()}

//Default Java Regex with capturing group for protein accession if fasta file name doesn't match parsing_rules RegEx
// ${ScalaUtils.doubleBackSlashes(">(\\S+)")} :  String after '>' and before first space
default-protein-accession ="${ScalaUtils.doubleBackSlashes(parsingRule.defaultProteinAccession.getOrElse(">(\\S+)"))}"

"""
    /* Print in file (overwrite old parsing rule File) */
    synchronized {
      val out = new FileWriter(parsingRulesFile)
      try { out.write(parsingRuleTemplate) }
      finally { out.close }
    }
  }
}

/** Model what in the sequence repository parsing rules file */

case class ParsingRule(
    localFastaDirectories: ListBuffer[String],
    parsingRules: ListBuffer[Rule],
    defaultProteinAccession: Option[String]) {

  /* Return local Fasta directories strings */
  def toTypeSafeFastaDirString(): String = {
    val mpStrBuilder = new StringBuilder()
    if (!localFastaDirectories.isEmpty) {
      localFastaDirectories.foreach { dir =>
        if (!dir.equals(localFastaDirectories.last)) mpStrBuilder ++= s""" "${ScalaUtils.doubleBackSlashes(dir)}","""
        else mpStrBuilder ++= s""" "${ScalaUtils.doubleBackSlashes(dir)}" """
      }
    } else {
      /* Reset default value for Fasta directories */
      mpStrBuilder ++= s""" "${ScalaUtils.doubleBackSlashes("D:\\temp\\fasta")}" """
    }
    mpStrBuilder.result
  }

  /*  return parsing rules strings */

  def toTypeSafeParsingRulesString(): String = {

    val mpStrBuilder = new StringBuilder()
    if (!parsingRules.isEmpty) {
      mpStrBuilder ++= " [ "
      toStringRules
      mpStrBuilder ++= " ] "
    } else {
      /* reset  default parsing Rules */
      parsingRules += Rule("label1", ListBuffer("ISA_"), "_(?:D|(?:Decoy))_(.*)\\.fasta", ">\\w{2}\\|([^\\|]+)\\|")
      parsingRules += Rule("label2", ListBuffer("UP_", "S_cerevisiae_", "MyDB"), "_(?:D|(?:Decoy))_(.*)\\.fasta", ">\\w{2}\\|[^\\|]*\\|(\\S+)")
      parsingRules += Rule("UPS", ListBuffer("UPS1UPS2_"), "_(?:D|(?:Decoy))_(.*)\\.fasta", ">[^\\|]*\\|(\\S+)")
      mpStrBuilder ++= " [ "
      toStringRules
      mpStrBuilder ++= " ] "
    }

    def toStringRules() {
      parsingRules.foreach { rule =>
        if (!rule.equals(parsingRules.last)) {
          mpStrBuilder ++= "{" + LINE_SEPARATOR
          mpStrBuilder ++= rule.toTypeSafeRuleString() + LINE_SEPARATOR
          mpStrBuilder ++= "} ," + LINE_SEPARATOR
        } else {
          mpStrBuilder ++= "{" + LINE_SEPARATOR
          mpStrBuilder ++= rule.toTypeSafeRuleString()
          mpStrBuilder ++= "}"
        }
      }
    }
    mpStrBuilder.result
  }
}

/* Model for  Rule */

case class Rule(name: String,
    fastaNames: ListBuffer[String],
    fastaVersion: String,
    proteinAccession: String) {
  require(name != null && name.isEmpty() == false, "Id must not be null nor empty in Parsing rule")
  require(fastaVersion != null && fastaVersion.isEmpty() == false, "Fasta version must not be null nor empty in Parsing rule")
  require(fastaNames != null && fastaNames.size() > 0, "Fasta Names must not be null nor empty in Parsing rule")
  require(proteinAccession != null && proteinAccession.isEmpty() == false, "Protein Accession must not be null nor empty in Parsing rule")

  def toTypeSafeRuleString(): String = {
    val strRule = new StringBuilder()
    strRule ++= " name="
    strRule ++= s""" "$name" , """ + LINE_SEPARATOR
    strRule ++= " fasta-name=["
    fastaNames.foreach { fastaName =>
      if (fastaName.equals(fastaNames.last)) strRule ++= s""" "$fastaName"  """ else strRule ++= s""" "$fastaName" , """
    }
    strRule ++= "]," + LINE_SEPARATOR
    strRule ++= " fasta-version="
    strRule ++= s""" "${ScalaUtils.doubleBackSlashes(fastaVersion)}" ,""" + LINE_SEPARATOR
    strRule ++= " protein-accession="
    strRule ++= s""" "${ScalaUtils.doubleBackSlashes(proteinAccession)}" """ + LINE_SEPARATOR
    return strRule.result
  }
}
