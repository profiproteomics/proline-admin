package fr.proline.admin.service.db.setup

import java.sql.Connection
import com.typesafe.scalalogging.slf4j.Logging
import fr.proline.context.DatabaseConnectionContext
import fr.proline.repository.DriverType
import fr.proline.repository.IDatabaseConnector
import javax.persistence.EntityTransaction
import fr.proline.repository.util.PostgresUtils

/**
 * @author David Bouyssie
 *
 */

/* Cannot use DatabaseConnectionContext here because Db Shema does not exist yet ! */
class SetupPdiDB(val dbConnector: IDatabaseConnector,
                 val dbConfig: DatabaseSetupConfig,
                 val prolineConfig: ProlineSetupConfig) extends ISetupDB with Logging {

  lazy val ncbiConfig = prolineConfig.pdiDBDefaults.resources.getConfig("ncbi")
  lazy val taxoConfig = ncbiConfig.getConfig("taxonomy")
  lazy val nodesFilePath = taxoConfig.getString("nodes_file")
  lazy val namesFilePath = taxoConfig.getString("names_file")

  protected def importDefaults() {

    if (dbConnector.getDriverType() == DriverType.POSTGRESQL) {
      _importDefaultsUsingPgCopyManager()
    } else {
      _importDefaultsUsingJPA()
    }

  }

  protected def _importDefaultsUsingJPA() {

    import fr.proline.module.rm.taxonomy.JPATaxonomyImporter

    val pdiEMF = dbConnector.getEntityManagerFactory

    var pdiTransaction: EntityTransaction = null
    var pdiTransacOK: Boolean = false

    val pdiEM = pdiEMF.createEntityManager()

    try {
      // Begin transaction
      pdiTransaction = pdiEM.getTransaction
      pdiTransaction.begin()
      pdiTransacOK = false

      JPATaxonomyImporter.importTaxonomy(nodesFilePath, namesFilePath, null, pdiEM)

      // Commit transaction
      pdiTransaction.commit()
      pdiTransacOK = true
    } finally {

      if ((pdiTransaction != null) && !pdiTransacOK) {
        logger.info("Rollbacking PDI Db EntityTransaction")

        try {
          pdiTransaction.rollback()
        } catch {
          case ex: Exception => logger.error("Error rollbacking PDI Db EntityTransaction")
        }

      }

      if (pdiEM != null) {
        try {
          pdiEM.close()
        } catch {
          case exClose: Exception => logger.error("Error closing PDI Db EntityManager")
        }
      }

    }

  }

  protected def _importDefaultsUsingPgCopyManager() {

    import fr.proline.module.rm.taxonomy.PGTaxonomyImporter

    val pdiDataSource = dbConnector.getDataSource

    val pdiDbConn = pdiDataSource.getConnection

    try {
      PGTaxonomyImporter.importTaxonomy(nodesFilePath, namesFilePath, pdiDbConn)
    } finally {

      if (pdiDbConn != null) {
        try {
          pdiDbConn.close()
        } catch {
          case exClose: Exception => logger.error("Error closing PDI Db SQL Connection")
        }
      }

    }

  }

}