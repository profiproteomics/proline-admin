package fr.proline.admin.service.db.setup

import com.weiglewilczek.slf4s.Logging
import fr.proline.context.DatabaseConnectionContext
import fr.proline.repository.IDatabaseConnector

/**
 * @author David Bouyssie
 *
 */
class SetupLcmsDB( val dbConnector: IDatabaseConnector,
                   val dbContext: DatabaseConnectionContext,
                   val dbConfig: DatabaseSetupConfig
                 ) extends ISetupDB with Logging {
  
  protected def importDefaults() {
    this.logger.info("no default values at the moment")
  }
  
}