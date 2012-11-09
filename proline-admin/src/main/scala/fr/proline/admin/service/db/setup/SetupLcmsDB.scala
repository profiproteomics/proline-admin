package fr.proline.admin.service.db.setup

import com.weiglewilczek.slf4s.Logging
import fr.proline.core.dal.DatabaseManagement

/**
 * @author David Bouyssie
 *
 */
class SetupLcmsDB( val dbManager: DatabaseManagement,
                   val dbConfig: DatabaseSetupConfig,
                   val projectId: Int ) extends ISetupDB with Logging {
  
  protected def importDefaults() {
    this.logger.info("no default values at the moment")
  }
  
}