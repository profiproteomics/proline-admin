package fr.proline.admin.service.db.migration

import org.flywaydb.core.Flyway
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
    require(_executed == false, "upgrade already executed")

    // TODO: call the DatabaseUpgrader ?
    dbCtx.tryInTransaction {
      this.upgradeDefinitions()
    }
    
    dbCtx.synchronized {
      _executed = true
    }
   
  }

}
