package fr.proline.admin.service.db.migration

import com.googlecode.flyway.core.Flyway
import com.typesafe.scalalogging.LazyLogging
import fr.proline.context.DatabaseConnectionContext
import fr.proline.core.dal.context._

/**
 * @author David Bouyssie
 *
 */
trait IUpgradeDB extends LazyLogging {

  val dbCtx: DatabaseConnectionContext
  private var _executed = false

  // Interface
  protected def upgradeDefinitions(): Unit

  /** Execution state. */
  def isExecuted = _executed

  /** Execute the setup of the database. */
  def run() {
    require(_executed == false, "upgrde already executed")

    // TODO: call the DatabaseUpgrader

    try {

      dbCtx.tryInTransaction {
        this.upgradeDefinitions()
      }

    } finally {

      if (dbCtx != null) {
        dbCtx.close()
      }

    }

    dbCtx.synchronized {
      _executed = true
    }
   
  }

}
