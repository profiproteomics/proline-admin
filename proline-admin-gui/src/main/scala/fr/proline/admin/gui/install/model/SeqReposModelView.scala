package fr.proline.admin.gui.install.model

import com.typesafe.scalalogging.LazyLogging

import fr.proline.admin.gui.Install
import fr.proline.repository.DriverType
import fr.proline.admin.gui.process.config._
import fr.profi.util.scala.TypesafeConfigWrapper._
import fr.proline.admin.gui.process.DatabaseConnection
import java.io.File

/**
 * The sequence repository model view.
 * Defines UI actions read and write parameters from application.conf and parsing-rules.conf files.
 *
 * @author aromdhani
 *
 */

class SeqReposModelView(seqReposConfFilePath: String, parsingRulesFilePath: String) extends LazyLogging {

  /** Return sequence repository postgreSQL config from application.conf */
  def seqReposConfigOpt(): Option[SeqConfig] = {
    try {
      val seqReposConfigFile = new SeqConfigFile(seqReposConfFilePath)
      seqReposConfigFile.read()
    } catch {
      case ex: Exception =>
        logger.error("Error while trying to retrieve sequence repository properties from configuartion file!", ex.getMessage)
        None
    }
  }

  /** Return parsing rules config from parsing-rules.conf */
  def parsigRulesConfig(): Option[ParsingRule] = {
    new ParsingRulesFile(parsingRulesFilePath).read()
  }

  /** Test database connection with the entered values from GUI fileds */
  def onTestDbConn(driverType: DriverType, user: String, password: String, host: String, port: Int) = {
    DatabaseConnection.testDbConnection(
      driverType,
      user,
      password,
      host,
      port,
      showSuccessPopup = true,
      showFailurePopup = true,
      Option(Install.stage))
  }

  /** Save sequence repository configurations */
  def onSaveConfig(seqConfig: SeqConfig): Unit = {
    try {
      logger.debug("Update sequence repository configurations...")
      val seqReposConfigFile = new SeqConfigFile(seqReposConfFilePath)
      seqReposConfigFile.write(seqConfig)
    } catch {
      case ex: Exception => logger.error("Error while trying to update sequence repository configurations", ex.getMessage)
    }
  }

  /** Save sequence repository parsing rules */
  def onSaveParsingRules(parsingRule: ParsingRule): Unit = {
    try {
      logger.debug("Update sequence repository parsing rules...")
      val parsingRulesFile = new ParsingRulesFile(parsingRulesFilePath)
      parsingRulesFile.write(parsingRule)
    } catch {
      case ex: Exception => logger.error("Error while trying to update sequence repository parsing rules", ex.getMessage)
    }
  }
}

