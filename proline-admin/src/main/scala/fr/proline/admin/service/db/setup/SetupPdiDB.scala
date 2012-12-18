package fr.proline.admin.service.db.setup

import com.weiglewilczek.slf4s.Logging
import fr.proline.admin.service.db.DatabaseConnectionContext
import fr.proline.repository.DriverType

/**
 * @author David Bouyssie
 *
 */
class SetupPdiDB( val pdiDbContext: DatabaseConnectionContext,
                  val dbConfig: DatabaseSetupConfig,
                  val prolineConfig: ProlineSetupConfig ) extends ISetupDB with Logging {
  
  lazy val ncbiConfig = prolineConfig.pdiDBDefaults.resources.getConfig("ncbi")
  lazy val taxoConfig = ncbiConfig.getConfig("taxonomy")
  lazy val nodesFilePath = taxoConfig.getString("nodes_file")
  lazy val namesFilePath = taxoConfig.getString("names_file")
    
  protected def importDefaults() {
    
    if( dbConfig.driverType == DriverType.POSTGRESQL ) _importDefaultsUsingPgCopyManager()
    else _importDefaultsUsingJPA()
    
  }
  
  protected def _importDefaultsUsingJPA() {
    
    import fr.proline.module.rm.taxonomy.JPATaxonomyImporter
    
    val pdiEM = pdiDbContext.entityManager
    
    // Begin transaction
    val pdiTransaction = pdiEM.getTransaction()    
    pdiTransaction.begin()
    
    JPATaxonomyImporter.importTaxonomy(nodesFilePath, namesFilePath, null, pdiEM)
    
    // Commit transaction
    pdiTransaction.commit()
  }
  
  protected def _importDefaultsUsingPgCopyManager() {
    
    import org.postgresql.core.BaseConnection
    import fr.proline.module.rm.taxonomy.PGTaxonomyImporter
    
    val pdiDbConn = pdiDbContext.connection
    PGTaxonomyImporter.importTaxonomy(nodesFilePath, namesFilePath, pdiDbConn.asInstanceOf[BaseConnection] )
  }
  
}