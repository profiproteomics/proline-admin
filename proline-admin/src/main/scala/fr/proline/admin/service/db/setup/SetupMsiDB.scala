package fr.proline.admin.service.db.setup

import javax.persistence.Persistence
import scala.collection.JavaConversions.{ collectionAsScalaIterable }
import com.typesafe.config.Config
import com.typesafe.scalalogging.slf4j.Logging
import fr.proline.context.DatabaseConnectionContext
import fr.proline.core.dal.tables.msi.{ MsiDbScoringTable, MsiDbObjectTreeSchemaColumns }
import fr.proline.core.orm.msi.{ AdminInformation => MsiAdminInfos, Scoring => MsiScoring, ObjectTreeSchema => MsiSchema }
import fr.proline.util.sql.getTimeAsSQLTimestamp
import fr.proline.repository.IDatabaseConnector
import javax.persistence.EntityTransaction
import javax.persistence.EntityManager

/**
 * @author David Bouyssie
 *
 */

/* Cannot use DatabaseConnectionContext here because Db Shema does not exist yet ! */
class SetupMsiDB(val dbConnector: IDatabaseConnector,
                 val dbConfig: DatabaseSetupConfig,
                 val defaults: MsiDBDefaults) extends ISetupDB with Logging {

  protected def importDefaults() {

    val msiEMF = dbConnector.getEntityManagerFactory

    var msiTransaction: EntityTransaction = null
    var msiTransacOK: Boolean = false

    val msiEM = msiEMF.createEntityManager()

    try {
      // Begin transaction
      msiTransaction = msiEM.getTransaction
      msiTransaction.begin()
      msiTransacOK = false

      // Import Admin information
      _importAdminInformation(msiEM)
      logger.info("Admin information imported !")

      // Import scoring definitions
      _importScorings(msiEM, defaults.scorings)
      logger.info("Scoring definitions imported !")

      // Import schemata
      _importSchemata(msiEM, defaults.schemata)
      logger.info("Schemata imported !")

      // Commit transaction
      msiTransaction.commit()
      msiTransacOK = true
    } finally {

      if ((msiTransaction != null) && !msiTransacOK) {
        logger.info("Rollbacking MSI Db EntityTransaction")

        try {
          msiTransaction.rollback()
        } catch {
          case ex: Exception => logger.error("Error rollbacking MSI Db EntityTransaction")
        }

      }

      if (msiEM != null) {
        try {
          msiEM.close()
        } catch {
          case exClose: Exception => logger.error("Error closing MSI Db EntityManager")
        }
      }

    }

  }

  private def _importAdminInformation(msiEM: EntityManager) {

    val msiAdminInfos = new MsiAdminInfos()
    msiAdminInfos.setModelVersion(dbConfig.schemaVersion)
    msiAdminInfos.setDbCreationDate(getTimeAsSQLTimestamp)
    //udsAdminInfos.setModelUpdateDate()
    msiEM.persist(msiAdminInfos)

  }

  private def _importScorings(msiEM: EntityManager, scorings: java.util.List[Config]) {

    val scoringCols = MsiDbScoringTable.columns

    // Store scorings
    for (scoring <- scorings) {

      // Create new scoring
      val msiScoring = new MsiScoring()
      msiScoring.setSearchEngine(scoring.getString(scoringCols.SEARCH_ENGINE))
      msiScoring.setName(scoring.getString(scoringCols.NAME))
      msiScoring.setDescription(scoring.getString(scoringCols.DESCRIPTION))

      msiEM.persist(msiScoring)

    }

  }

  private def _importSchemata(msiEM: EntityManager, schemata: java.util.List[Config]) {

    val otsCols = MsiDbObjectTreeSchemaColumns

    // Store schemata
    for (schema <- schemata) {

      // Create new scoring
      val msiSchema = new MsiSchema()
      msiSchema.setName(schema.getString(otsCols.NAME))
      msiSchema.setType(schema.getString(otsCols.TYPE))
      msiSchema.setIsBinaryMode(schema.getBoolean(otsCols.IS_BINARY_MODE))
      msiSchema.setVersion(schema.getString(otsCols.VERSION))
      msiSchema.setSchema("")
      msiSchema.setSerializedProperties(schema.getString(otsCols.SERIALIZED_PROPERTIES))

      msiEM.persist(msiSchema)

    }

  }

}