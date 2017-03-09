package src.main.scala.fr.proline.admin.gui.component.wizard

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigRenderOptions
import com.typesafe.scalalogging.LazyLogging
import scala.util.matching.Regex
import scala.collection.JavaConversions._
import scala.collection.mutable.StringBuilder
import scala.io.Source
import scala.collection.mutable.Map
import scala.collection.mutable.ListBuffer

import java.io.File
import java.io.FileWriter
import fr.proline.admin.gui.util.ShowPopupWindow
import fr.profi.util.StringUtils.LINE_SEPARATOR
import fr.profi.util.scala.ScalaUtils
import fr.profi.util.scala.TypesafeConfigWrapper._

/** parse and edit Sequence Repository parsing rules file **/

class ParsingRulesFile(val path: String) extends LazyLogging {

  require(path != null && path.isEmpty() == false, "Parsing Rules file must not be null nor empty")
  private val parsingRulesFile = new File(path)

  /** read parsingRules  file **/
  def read() = {
    try {
      /* create Parsing rule  */
      val parsing = ConfigFactory.parseFile(parsingRulesFile)

      /* retrieve local fasta directories  */
      def getLocalFastaDirectories(): ListBuffer[String] = {
        var listFastaDir: ListBuffer[String] = ListBuffer()
        parsing.getStringList("local-fasta-directories").toList.foreach { dir =>
          listFastaDir += dir
        }
        listFastaDir
      }

      /* retrieve parsing rule file */

      def getParsingRules(): ListBuffer[Any] = {
        var parsingRulesList: ListBuffer[Any] = ListBuffer()
        parsing.getObjectList("parsing-rules").toList.foreach {
          rule => parsingRulesList += rule
        }
        parsingRulesList
      }

      /* retrieve default protein accession */

      def getDefaultProteinAccession(): Option[String] = {
        parsing.getStringOpt("default-protein-accession")
      }

      val listFastaDir = getLocalFastaDirectories()
      val listParsingRules = getParsingRules()
      val defaultProAccession = getDefaultProteinAccession()
      /* Return Parsing Rule  */
      Some(
        ParsingRule(
          //serverConfFilePath = path,
          localFastaDirectories = listFastaDir,
          parsingRules = listParsingRules,
          defaultProteinAccession = defaultProAccession))

    } catch {
      case t: Throwable =>
        logger.error("Error occurated while reading parsing rule file ", t)
        None
    }
  }

  /** write parsing rule file **/

  def write(parsingRule: ParsingRule): Unit = synchronized {
    /*write in template in model of parsing rule */
    val parsingRuleTemplate = s"""
//Specify path to fasta files for SeqRepository daemon. Multiple path separated by ',' between []
//local-fasta-directories =["S:\\sequence"] 
local-fasta-directories =[ ${parsingRule.toTypeSafeFastaDirString()} ]

// Rules used for parsing fasta entries. Multiple rules could be specified.
// name : identifying rule definition
// fasta-name : FASTA file name must match specified Java Regex CASE_INSENSITIVE. multiple Regex separated by ',' between []
// fasta-version : Java Regex with capturing group for fasta release version string (CASE_INSENSITIVE)
// protein-accession : Java Regex with capturing group for protein accession
parsing-rules = ${parsingRule.toTypeSafeParsingRulesString()}

//Default Java Regex with capturing group for protein accession if fasta file name doesn't match parsing_rules RegEx
// >(\\S+) :  String after '>' and before first space
default-protein-accession ="${ScalaUtils.doubleBackSlashes(parsingRule.defaultProteinAccession.getOrElse(">(\\S+)"))}"

"""
    /* Print in file (overwrite old parsing rule File) */
    synchronized {
      //val out = new FileWriter(adminConfigFile, false)
      val out = new FileWriter(parsingRulesFile)
      try { out.write(parsingRuleTemplate) }
      finally { out.close }
    }
  }
}

/** Model what in the sequence repository parsing rules file */

case class ParsingRule(
  var localFastaDirectories: ListBuffer[String] = ListBuffer(),
  var parsingRules: ListBuffer[Any] = ListBuffer(),
  var defaultProteinAccession: Option[String]) {

  //update local fasta directory  

  def toTypeSafeFastaDirString(): String = {

    val mpStrBuilder = new StringBuilder()
    val lastElement = localFastaDirectories.toList.last
    localFastaDirectories.foreach { dir =>
      if (dir.equals(lastElement)) mpStrBuilder ++= s""" "${ScalaUtils.doubleBackSlashes(dir)}" """ else mpStrBuilder ++= s""" "${ScalaUtils.doubleBackSlashes(dir)}" """ + ","
    }
    mpStrBuilder.result
  }

  // update parsing rules file 

  def toTypeSafeParsingRulesString(): String = {

    val mpStrBuilder = new StringBuilder()
    val newConvertedList = parsingRules.toList.map(_.asInstanceOf[Rule])
    val lastElement = newConvertedList.last
    mpStrBuilder ++= " [ "
    newConvertedList.foreach { ruleObject =>
      if (ruleObject != lastElement) {
        mpStrBuilder ++= "{" + LINE_SEPARATOR

        mpStrBuilder ++= ruleObject.newRule() + LINE_SEPARATOR

        mpStrBuilder ++= "} ," + LINE_SEPARATOR
      } else {
        mpStrBuilder ++= "{" + LINE_SEPARATOR

        mpStrBuilder ++= ruleObject.newRule()

        mpStrBuilder ++= "}"
      }
    }
    mpStrBuilder ++= " ] "
    mpStrBuilder.result
  }

  //set default protein accession

  def setDefaultProteinAccesion(newDefProAcc: Option[String]) = {
    defaultProteinAccession = newDefProAcc
  }

}

// Model to create one rule 

case class Rule(var name: String, var fastaNames: ListBuffer[String], var fastaVersion: String, var proteinAccession: String) {

  require(name != null && name.isEmpty() == false, "missing argument name in rule")
  require(fastaVersion != null && fastaVersion.isEmpty() == false, "missing argument Fasta version in rule")
  require(fastaNames != null && fastaNames.size() > 0, "missing argument fasta names in rule")
  require(proteinAccession != null && proteinAccession.isEmpty() == false, "missing argument protein accession in rule")

  def newRule(): String = {
    var strRule = new StringBuilder()
    strRule ++= " name="
    strRule ++= s""" "$name" , """ + LINE_SEPARATOR
    strRule ++= " fasta-name=["
    val lastElement = fastaNames.toList.last
    fastaNames.foreach { fastaName =>
      if (fastaName.equals(lastElement)) strRule ++= s""" "$fastaName"  """ else strRule ++= s""" "$fastaName" , """
    }
    strRule ++= "]," + LINE_SEPARATOR
    strRule ++= " fasta-version="
    strRule ++= s""" "${ScalaUtils.doubleBackSlashes(fastaVersion)}" ,""" + LINE_SEPARATOR
    strRule ++= " protein-accession="
    strRule ++= s""" "${ScalaUtils.doubleBackSlashes(proteinAccession)}" """ + LINE_SEPARATOR
    return strRule.result
  }

  //update rule name

  def updateName(newName: String): String = {
    require(newName != null && newName.isEmpty() == false, "missing argument name in rule")
    name = newName
    return newRule()
  }

  //update Fasta name
  def updateFastaName(fastaNameList: ListBuffer[String]): String = {
    require(fastaNameList != null && fastaNameList.size() > 0, "missing argument fasta names in rule")
    fastaNames = fastaNameList
    return newRule()
  }

  //update fasta version
  def updateFastaVersion(newFastaVersion: String): String = {
    require(newFastaVersion != null && newFastaVersion.isEmpty() == false, "missing argument Fasta version in rule")
    fastaVersion = newFastaVersion
    return newRule()
  }

  // update protein accession 
  def updateProteinAccession(newProtAccession: String): String = {
    require(newProtAccession != null && newProtAccession.isEmpty() == false, "missing argument protein accession in rule")
    proteinAccession = newProtAccession
    return newRule()
  }

}