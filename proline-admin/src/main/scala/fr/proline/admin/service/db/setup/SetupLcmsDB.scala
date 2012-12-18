package fr.proline.admin.service.db.setup

import com.weiglewilczek.slf4s.Logging
import fr.proline.admin.service.db.DatabaseConnectionContext

/**
 * @author David Bouyssie
 *
 */
class SetupLcmsDB( val lcmsDbContext: DatabaseConnectionContext,
                   val dbConfig: DatabaseSetupConfig
                 ) extends ISetupDB with Logging {
  
  protected def importDefaults() {
    this.logger.info("no default values at the moment")
  }
  
}