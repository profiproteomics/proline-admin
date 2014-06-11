package fr.proline.admin.service.db.setup

/*import com.typesafe.scalalogging.slf4j.Logging
import fr.proline.context.DatabaseConnectionContext
import fr.proline.core.orm.ps.{ AdminInformation => PsAdminInfos }
import fr.proline.module.rm.unimod.UnimodImporter
import fr.proline.util.sql.getTimeAsSQLTimestamp
import fr.proline.repository.IDatabaseConnector
import javax.persistence.EntityManager
import javax.persistence.EntityTransaction

/**
 * @author David Bouyssie
 *
 */

/* Cannot use DatabaseConnectionContext here because Db Shema does not exist yet ! */
class SetupPsDB(val dbConnector: IDatabaseConnector,
                val dbConfig: DatabaseSetupConfig) extends ISetupDB with Logging {

  protected def importDefaults() {
    val psEMF = dbConnector.getEntityManagerFactory

    var psTransaction: EntityTransaction = null
    var psTransacOK: Boolean = false

    val psEM = psEMF.createEntityManager()

    try {
      // Begin transaction
      psTransaction = psEM.getTransaction
      psTransaction.begin()
      psTransacOK = false

      val psAdminInfos = new PsAdminInfos()
      psAdminInfos.setModelVersion(dbConfig.schemaVersion)
      psAdminInfos.setDbCreationDate(getTimeAsSQLTimestamp)
      //psAdminInfos.setModelUpdateDate()    
      psEM.persist(psAdminInfos)

      logger.info("Admin information imported !")

      // Create Unimod file input stream
      val unimodIS = getClass().getResourceAsStream("/mascot_config/unimod.xml")

      // Import the Unimod definitions
      new UnimodImporter().importUnimodStream(unimodIS, psEM, false)

      // Commit transaction
      psTransaction.commit()
      psTransacOK = true

      logger.info("Unimod definitions imported !")
    } finally {

      if ((psTransaction != null) && !psTransacOK) {
        logger.info("Rollbacking PS Db EntityTransaction")

        try {
          psTransaction.rollback()
        } catch {
          case ex: Exception => logger.error("Error rollbacking PS Db EntityTransaction")
        }

      }

      if (psEM != null) {
        try {
          psEM.close()
        } catch {
          case exClose: Exception => logger.error("Error closing PS Db EntityManager")
        }
      }

    }

  }

}*/