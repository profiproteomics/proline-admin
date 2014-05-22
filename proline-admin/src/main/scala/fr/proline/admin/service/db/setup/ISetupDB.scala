package fr.proline.admin.service.db.setup

import java.io.{ File, InputStream }
import scala.io.Source
import com.googlecode.flyway.core.Flyway
import com.typesafe.scalalogging.slf4j.Logging

import fr.proline.admin.helper.sql._
//import fr.proline.context.DatabaseConnectionContext
import fr.proline.repository.{ IDatabaseConnector, DatabaseUpgrader, DriverType }
import fr.proline.util.io._
import fr.proline.util.resources._
import fr.proline.util.ThreadLogger

/**
 * @author David Bouyssie
 *
 */
trait ISetupDB extends Logging {

  val dbConfig: DatabaseSetupConfig
  val dbConnector: IDatabaseConnector
  private var _executed = false

  // Interface
  protected def importDefaults(): Unit

  /** Execution state. */
  def isExecuted = _executed

  /** Execute the setup of the database. */
  def run() {

    val currentThread = Thread.currentThread

    if (!currentThread.getUncaughtExceptionHandler.isInstanceOf[ThreadLogger]) {
      currentThread.setUncaughtExceptionHandler(new ThreadLogger(logger.underlying.getName))
    }

    if (_executed) {
      throw new IllegalStateException("The setup has been already executed")
    }

    //try {
    if (initSchema()) {
      importDefaults()
      logger.info("database '" + dbConfig.dbName + "' successfully set up !")
    } else {
      throw new Exception(dbConfig.dbName + " schema initialization failed")
    }
    /*} catch {

      case ex: Exception => {
        logger.error(dbConfig.dbName + " schema initialization failed", ex)
      }

    }*/

    _executed = true
  }

  protected def initSchema(): Boolean = {

    // Create database if driver type is PostgreSQL
    if (dbConfig.driverType == DriverType.POSTGRESQL) {
      createPgDatabase(dbConnector, dbConfig, Some(logger))
    }

    // Initialize database schema
    //    dbConfig.schemaVersion = DatabaseUpgrader.upgradeDatabase(dbConnector);    
    //    if ((dbConfig.schemaVersion == null) || (dbConfig.schemaVersion.isEmpty()) || dbConfig.schemaVersion.equals("no.version")) false else true
    dbConfig.schemaVersion = "0.1"

    val upgradeStatus = if (DatabaseUpgrader.upgradeDatabase(dbConnector) > 0) true else false

    upgradeStatus
  }

}
