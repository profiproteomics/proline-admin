package src.main.scala.fr.proline.admin.gui.component.wizard

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigRenderOptions
import com.typesafe.scalalogging.LazyLogging

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

/**parse and write Sequence Repository parsing rules file*/

class ParsingRulesFile(val path: String) extends LazyLogging {

  require(path != null && path.isEmpty() == false, "Parsing Rules file must not be null nor empty")
  private val parsingRulesFile = new File(path)
  //  def test() = {
  //    val parsing = ConfigFactory.parseFile(parsingRulesFile)
  //    System.out.println("list " + parsing.getStringList("local-fasta-directories").toList)
  //    System.out.println("2" + parsing.getObjectList("parsing-rules").toList)
  //    //
  //    parsing.getObjectList("parsing-rules").toList.foreach {
  //      rule =>
  //        rule.map {
  //          case (k, v) =>
  //            k -> v.unwrapped().toString()
  //            System.out.println("k : " + k + "v : " + v.unwrapped().toString())
  //        }
  //    }
  //
  //  }
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

      def getParsingRules(): ListBuffer[Map[Object, String]] = {
        var parsingRulesList: ListBuffer[Map[Object, String]] = ListBuffer(Map())
        var mp: Map[Object, String] = Map()
        parsing.getObjectList("parsing-rules").toList.foreach {
          rule =>
            {
              rule.map {
                case (k, v) => { mp += (k -> v.unwrapped().toString()) }
              }
              parsingRulesList += mp
            }
        }
        parsingRulesList
      }

      /* retrieve deafult accession */

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
default-protein-accession ="${parsingRule.defaultProteinAccession.getOrElse(">(\\S+)")}"

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
  localFastaDirectories: ListBuffer[String] = ListBuffer(),
  parsingRules: ListBuffer[Map[Object, String]] = ListBuffer(Map()),
  defaultProteinAccession: Option[String]) {

  //update local fasta directory  

  def toTypeSafeFastaDirString(): String = {
    val mpStrBuilder = new StringBuilder()
    val lastElement = localFastaDirectories.last
    localFastaDirectories.foreach { dir =>
      if (dir != lastElement) { mpStrBuilder ++= s""" "${ScalaUtils.doubleBackSlashes(dir)}" """ + "," }
      else {
        mpStrBuilder ++= s""" "${ScalaUtils.doubleBackSlashes(dir)}" """
      }

    }
    mpStrBuilder.result
  }

  // update parsing rules file 

  def toTypeSafeParsingRulesString(): String = {
    val mpStrBuilder = new StringBuilder()
    val lastElement = parsingRules.last
    mpStrBuilder ++= " [ "
    parsingRules.foreach { ruleObject =>
      if (ruleObject != lastElement) {
        mpStrBuilder ++= "{" + LINE_SEPARATOR
        ruleObject.map {
          case (k, v) => {
            mpStrBuilder ++= s"""  $k.ParsingRulesEnum.NAME = "$v " """ + "," + LINE_SEPARATOR
            mpStrBuilder ++= s"""  $k.ParsingRulesEnum.FASTANAME = "$v " """ + "," + LINE_SEPARATOR
            mpStrBuilder ++= s"""  $k.ParsingRulesEnum.FASTAVERSION = "$v " """ + "," + LINE_SEPARATOR
            mpStrBuilder ++= s"""  $k.ParsingRulesEnum.PROTEINACCESSION = "$v " """ + LINE_SEPARATOR
          }
        }
        mpStrBuilder ++= "},"
      } else {
        mpStrBuilder ++= "{" + LINE_SEPARATOR
        ruleObject.map {
          case (k, v) => { mpStrBuilder ++= s"""  $k = "$v " """ + "," + LINE_SEPARATOR }
        }
        mpStrBuilder ++= "}"
      }
    }
    mpStrBuilder ++= " ] "
    mpStrBuilder.result
  }
  //update default protein accession 
}
//
object ParsingRulesEnum extends Enumeration {

  val NAME = Value("name")
  val FASTANAME = Value("fasta-name")
  val FASTAVERSION = Value("fasta-version")
  val PROTEINACCESSION = Value("protein-accession") //etc.
}