package fr.proline.admin.service.db.setup

import java.io.{ File, InputStream }
import scala.io.Source
import com.googlecode.flyway.core.Flyway
import com.typesafe.scalalogging.slf4j.Logging

import fr.proline.admin.helper.sql._
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

    if (initDbSchema(dbConnector, dbConfig)) {
      importDefaults()
      logger.info("database '" + dbConfig.dbName + "' successfully set up !")
    } else {
      throw new Exception(dbConfig.dbName + " schema initialization failed")
    }

    _executed = true
  }

}
