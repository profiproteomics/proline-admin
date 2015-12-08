package fr.proline.admin.service.db.migration

import javax.persistence.EntityManager
import scala.collection.JavaConversions._
import com.typesafe.scalalogging.StrictLogging
import fr.proline.admin.service.ICommandWork
import fr.proline.context.DatabaseConnectionContext
import fr.proline.core.dal.ProlineEzDBC
import fr.proline.core.orm.uds.repository.ExternalDbRepository
import fr.proline.core.orm.uds.repository.ProjectRepository
import fr.proline.core.orm.util.DataStoreConnectorFactory
import fr.proline.repository._

/**
 *
 * Upgrades all Proline Databases (UDS, PDI, PS and all projects MSI and LCMS Dbs).
 * 
 * @param connectorFactory
 *            Must be a valid initialized DataStoreConnectorFactory instance.
 */
class UpgradeAllDatabases(
  val dsConnectorFactory: DataStoreConnectorFactory
) extends ICommandWork with StrictLogging {

  def doWork(): Unit = {

    if ((dsConnectorFactory == null) || !dsConnectorFactory.isInitialized())
      throw new IllegalArgumentException("Invalid connectorFactory")

    /* Upgrade UDS Db */
    val udsDbConnector = dsConnectorFactory.getUdsDbConnector
    _updradeDatabase(udsDbConnector, "UDSdb", closeConnector = false)
    var udsEM: EntityManager = null

    def _updateExternalDbVersion(dbType: ProlineDatabaseType, newVersion: String) {
      val extDb = ExternalDbRepository.findExternalByType(udsEM, dbType)
      extDb.setDbVersion(newVersion)
    }

    try {

      udsEM = udsDbConnector.getEntityManagerFactory.createEntityManager()

      /* Upgrade PDI Db */
      _updradeDatabase(dsConnectorFactory.getPdiDbConnector, "PDIdb", onUpgradeSuccess = { pdiDbVersion =>
        /* Update PDI Db version */
        _updateExternalDbVersion(ProlineDatabaseType.PDI, pdiDbVersion)
      })

      /* Upgrade PS Db */
      _updradeDatabase(dsConnectorFactory.getPsDbConnector, "PSdb", onUpgradeSuccess = { psDbVersion =>
        /* Update PS Db version */
        _updateExternalDbVersion(ProlineDatabaseType.PS, psDbVersion)
      })

      /* Upgrade all Projects (MSI and LCMS) Dbs */
      val projectIds = ProjectRepository.findAllProjectIds(udsEM)
      
      if ((projectIds != null) && projectIds.isEmpty() == false) {
        
        val udsTx = udsEM.getTransaction()
        udsTx.begin()

        for (projectId <- projectIds) {
          logger.debug(s"Upgrading databases of Project #$projectId")

          /* Upgrade MSI Db */
          val msiDbConnector = dsConnectorFactory.getMsiDbConnector(projectId)
          val msiVersion = _updradeDatabase(
            msiDbConnector,
            s"MSIdb (project #$projectId)",
            onUpgradeSuccess = { msiVersion =>

              /* Update MSI Db version */
              _updateExternalDbVersion(ProlineDatabaseType.MSI, msiVersion)

              /* Upgrade MSI Db definitions */
              new UpgradeMsiDbDefinitions(new DatabaseConnectionContext(msiDbConnector)).run()
            }
          )

          /* Upgrade LCMS Db */
          val lcMsDbConnector = dsConnectorFactory.getLcMsDbConnector(projectId)
          _updradeDatabase(lcMsDbConnector, s"LCMSdb (project #$projectId)", onUpgradeSuccess = { lcMsVersion =>
            /* Update LCMS Db version */
            _updateExternalDbVersion(ProlineDatabaseType.LCMS, lcMsVersion)
          })

        }
        
        udsEM.flush()
        udsTx.commit()
      }

    } finally {

      // Close UDS entity manager
      if (udsEM != null) {
        udsEM.close()
      }

      // Close UDSdb connector at the end
      if (udsDbConnector != null)
        udsDbConnector.close()
    }

    ()
  }

  private def _updradeDatabase(
    dbConnector: IDatabaseConnector,
    dbShortName: String,
    closeConnector: Boolean = true,
    onUpgradeSuccess: String => Unit = null
  ): IDatabaseConnector = {

    if (dbConnector == null) {
      logger.warn(s"DataStoreConnectorFactory has no valid connector to $dbShortName")
    } else {

      try {
        val dbMigrationCount = DatabaseUpgrader.upgradeDatabase(dbConnector)

        if (dbMigrationCount < 0) {
          throw new Exception(s"Unable to upgrade $dbShortName")
        } else {
          logger.info(s"$dbShortName: $dbMigrationCount migration(s) done.")
        }
        
        val driverType = dbConnector.getDriverType

        // TODO: find a way to migrate SQLite databases with flyway
        val version = if( driverType == DriverType.SQLITE ) "no.version"
        else { 
          // Try to retrieve the version reached after the applied migration
          val ezDBC = ProlineEzDBC(dbConnector.getDataSource.getConnection, dbConnector.getDriverType)
          ezDBC.selectHead("""SELECT "version" FROM "schema_version" ORDER BY "version_rank" DESC LIMIT 1""") { r =>
            r.nextString
          }
        }

        if (onUpgradeSuccess != null)
          onUpgradeSuccess(version)

      } finally {

        if (closeConnector)
          dbConnector.close()
      }
    }

    dbConnector
  }

}

object UpgradeAllDatabases extends StrictLogging {
  
  def apply(dsConnectorFactory: DataStoreConnectorFactory) = {
    
    try {
      new UpgradeAllDatabases(dsConnectorFactory).doWork()
      
      logger.info("Databases successfully upgraded !")
    } catch {
      case t: Throwable => {
        logger.error("Databases upgrade failed !", t)
      }
    }
    
  }
  
}
