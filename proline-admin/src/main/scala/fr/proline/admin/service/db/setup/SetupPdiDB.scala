package fr.proline.admin.service.db.setup

import com.weiglewilczek.slf4s.Logging
import fr.proline.core.dal.DatabaseManagement
import fr.proline.module.rm.ncbi.JPATaxonomyImporter

/**
 * @author David Bouyssie
 *
 */
class SetupPdiDB( val dbManager: DatabaseManagement,
                  val dbConfig: DatabaseSetupConfig,
                  val prolineConfig: ProlineSetupConfig ) extends ISetupDB with Logging {
  
  protected def importDefaults() {
    
    val pdiEM = dbManager.pdiEMF.createEntityManager()
    
    // Begin transaction
    val pdiTransaction = pdiEM.getTransaction()    
    pdiTransaction.begin()
    
    val ncbiConfig = prolineConfig.pdiDBDefaults.resources.getConfig("ncbi")
    val taxoConfig = ncbiConfig.getConfig("taxonomy")
    
    JPATaxonomyImporter.importTaxonomy(taxoConfig.getString("nodes_file"), taxoConfig.getString("names_file"), pdiEM)
    
    // Commit transaction
    pdiTransaction.commit()
    
  }
  
}