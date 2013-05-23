package fr.proline.admin.service.db.setup

import java.sql.Connection
import com.weiglewilczek.slf4s.Logging
import fr.proline.context.DatabaseConnectionContext
import fr.proline.repository.DriverType
import fr.proline.repository.IDatabaseConnector

/**
 * @author David Bouyssie
 *
 */
class SetupPdiDB( val dbConnector: IDatabaseConnector,
                  val dbContext: DatabaseConnectionContext,
                  val dbConfig: DatabaseSetupConfig,
                  val prolineConfig: ProlineSetupConfig ) extends ISetupDB with Logging {
  
  lazy val ncbiConfig = prolineConfig.pdiDBDefaults.resources.getConfig("ncbi")
  lazy val taxoConfig = ncbiConfig.getConfig("taxonomy")
  lazy val nodesFilePath = taxoConfig.getString("nodes_file")
  lazy val namesFilePath = taxoConfig.getString("names_file")
    
  protected def importDefaults() {
    
     if (dbContext.isJPA) _importDefaultsUsingJPA()
     else if (dbContext.getDriverType() == DriverType.POSTGRESQL ) _importDefaultsUsingPgCopyManager()
     else throw new Exception("unsupported driver type for PDI defaults importation")    
    
  }
  
  protected def _importDefaultsUsingJPA() {
    
    import fr.proline.module.rm.taxonomy.JPATaxonomyImporter
    
    val pdiEM = dbContext.getEntityManager
    
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
    
    val pdiDbConn = dbContext.getConnection()
    PGTaxonomyImporter.importTaxonomy(nodesFilePath, namesFilePath, pdiDbConn.asInstanceOf[BaseConnection] )
  }
  
}