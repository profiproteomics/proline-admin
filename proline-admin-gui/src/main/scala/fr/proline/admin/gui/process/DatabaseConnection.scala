package fr.proline.admin.gui.process

import com.typesafe.scalalogging.LazyLogging

import scala.util.Failure
import scala.util.Success
import scala.util.Try

import java.sql.Connection

import fr.proline.admin.gui.process.config.AdminConfig
import fr.proline.admin.gui.util.ShowPopupWindow
import fr.proline.repository.DriverType
import fr.proline.admin.gui.Wizard
/**
 * Custom exceptions
 */
case class UnhandledDriverTypeException(dirverType: DriverType) extends Exception(
  s"Connection to DB of type $dirverType is not handled by Proline-Admin-GUI yet.")

case class UnknownDriverTypeException(dirverType: DriverType) extends Exception(
  "Unknown driver type: " + dirverType.getJdbcURLProtocol())

/**
 * Database connection utilities
 * Handles exceptions with popups
 */
object DatabaseConnection extends LazyLogging {

  def testDbConnection(
    driverType: DriverType,
    userName: String,
    password: String,
    host: String,
    port: Int,
    showSuccessPopup: Boolean,
    showFailurePopup: Boolean): Boolean = { //return connectionEstablished

    require(driverType != null, "driverType is null")

    val tryConnection: Try[Connection] = driverType match {

      case DriverType.POSTGRESQL => testConnectionToPostgres(userName, password, host, port)

      case DriverType.H2 => testConnectionToH2(userName, password, host, port)

      case DriverType.SQLITE => testConnectionToSQLite(userName, password, host, port)

      case _ => Failure(UnknownDriverTypeException(driverType))
      //case _                     => (false, Some(UnknownDriverTypeException(driverType)))
    }

    /* Log +/- popup */
    tryConnection match {

      case Success(connection) => {
        logger.debug("Successfully connected to database !")

        if (showSuccessPopup) ShowPopupWindow(
          wTitle = "Test connection to database",
          wText = "The connection to the database has been successfully established !")
        // return isSuccess
        true
      }

      case Failure(t) => {

        //logger.debug("Unable to connect to database", errorOpt.get)
        val errorMsg = t.getMessage()
        logger.warn("Unable to connect to database:\n" + errorMsg)

        if (showFailurePopup) ShowPopupWindow(
          wTitle = "Test connection to database",
          wText = "The connection to the database could not be established with this configuration.\n\n" +
            "Check that your Proline configuration (Database connection parameters) is correct.\n" +
            s"Note: you may also check that your $driverType server is running.\n\n" +
            "Got the following error:\n" + errorMsg)

        // return isSuccess
        false
      }
    }
  }

  def testDbConnection(
    adminConfig: AdminConfig,
    showSuccessPopup: Boolean = true,
    showFailurePopup: Boolean = true): Boolean = {
    testDbConnection(
      adminConfig.driverType.getOrElse(null),
      adminConfig.dbUserName.getOrElse(""),
      adminConfig.dbPassword.getOrElse(""),
      adminConfig.dbHost.getOrElse(""),
      adminConfig.dbPort.getOrElse(5432),
      showSuccessPopup,
      showFailurePopup)
  }

  /** Test connection to PostgreSQL database **/
  def testConnectionToPostgres(
    userName: String,
    password: String,
    host: String,
    port: Int): Try[Connection] = {

    fr.proline.admin.helper.sql.checkPgConnection(host, port, userName, password)
  }

  /** Test connection to h2 database **/
  def testConnectionToH2(
    userName: String,
    password: String,
    host: String,
    port: Int): Try[Connection] = {
    //TODO: implement me?
    Failure(UnhandledDriverTypeException(DriverType.H2))
  }

  /** Test connection to SQLite database **/
  def testConnectionToSQLite(
    userName: String,
    password: String,
    host: String,
    port: Int): Try[Connection] = {

    //TODO: implement me?
    Failure(UnhandledDriverTypeException(DriverType.SQLITE))
  }

  def testDbConnectionToWizard(
    driverType: DriverType,
    userName: String,
    password: String,
    host: String,
    port: Int,
    showSuccessPopup: Boolean,
    showFailurePopup: Boolean): Boolean = { //return connectionEstablished

    require(driverType != null, "driverType is null")

    val tryConnection: Try[Connection] = driverType match {

      case DriverType.POSTGRESQL => testConnectionToPostgres(userName, password, host, port)

      case DriverType.H2 => testConnectionToH2(userName, password, host, port)

      case DriverType.SQLITE => testConnectionToSQLite(userName, password, host, port)

      case _ => Failure(UnknownDriverTypeException(driverType))
      //case _                     => (false, Some(UnknownDriverTypeException(driverType)))
    }

    /* Log +/- popup */
    tryConnection match {

      case Success(connection) => {
        logger.debug("Successfully connected to database !")

        if (showSuccessPopup)
          ShowPopupWindow(
            wTitle = "Test connection to database",
            wText = "The connection to the database has been successfully established !",
            wParent = Option(Wizard.stage))
        // return isSuccess
        true
      }

      case Failure(t) => {

        //logger.debug("Unable to connect to database", errorOpt.get)
        val errorMsg = t.getMessage()
        logger.warn("Unable to connect to database:\n" + errorMsg)

        if (showFailurePopup) ShowPopupWindow(
          wTitle = "Test connection to database",
          wText = "The connection to the database could not be established with this configuration.\n\n" +
            "Check that your Proline configuration (Database connection parameters) is correct.\n" +
            s"Note: you may also check that your $driverType server is running.\n\n" +
            "Got the following error:\n" + errorMsg,
          wParent = Option(Wizard.stage))

        // return isSuccess
        false
      }
    }
  }

}