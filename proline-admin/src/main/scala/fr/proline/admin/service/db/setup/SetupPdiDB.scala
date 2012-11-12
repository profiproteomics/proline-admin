package fr.proline.admin.service.db.setup

import com.weiglewilczek.slf4s.Logging
import fr.proline.core.dal.DatabaseManagement

/**
 * @author David Bouyssie
 *
 */
class SetupPdiDB( val dbManager: DatabaseManagement,
                  val dbConfig: DatabaseSetupConfig,
                  val prolineConfig: ProlineSetupConfig ) extends ISetupDB with Logging {
  
  lazy val ncbiConfig = prolineConfig.pdiDBDefaults.resources.getConfig("ncbi")
  lazy val taxoConfig = ncbiConfig.getConfig("taxonomy")
  lazy val nodesFilePath = taxoConfig.getString("nodes_file")
  lazy val namesFilePath = taxoConfig.getString("names_file")
    
  protected def importDefaults() {
    
    if( dbConfig.driverType == "postgresql" ) _importDefaultsUsingPgCopyManager()
    else _importDefaultsUsingJPA()
    
  }
  
  protected def _importDefaultsUsingJPA() {
    
    import fr.proline.module.rm.ncbi.JPATaxonomyImporter
    
    val pdiEM = dbManager.pdiEMF.createEntityManager()
    
    // Begin transaction
    val pdiTransaction = pdiEM.getTransaction()    
    pdiTransaction.begin()
    
    JPATaxonomyImporter.importTaxonomy(nodesFilePath, namesFilePath, pdiEM)
    
    // Commit transaction
    pdiTransaction.commit()
  }
  
  protected def _importDefaultsUsingPgCopyManager() {
    
    import org.postgresql.core.BaseConnection
    import fr.proline.module.rm.ncbi.PGTaxonomyImporter
    
    val pdiDbConn = dbManager.pdiDBConnector.getConnection()
    PGTaxonomyImporter.importTaxonomy(nodesFilePath, namesFilePath, pdiDbConn.asInstanceOf[BaseConnection] )
  }
  
}