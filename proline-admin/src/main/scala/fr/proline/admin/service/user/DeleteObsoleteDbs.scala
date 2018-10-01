package fr.proline.admin.service.user

import com.typesafe.scalalogging.LazyLogging
import fr.proline.core.orm.util.DataStoreConnectorFactory
import fr.proline.context.DatabaseConnectionContext
import fr.proline.admin.service.db.SetupProline
import fr.proline.core.dal.ProlineEzDBC
import fr.proline.core.dal.context._
import fr.proline.core.dal.DoJDBCWork
import fr.proline.core.orm.uds.{ Project, ExternalDb }
import fr.proline.core.orm.uds.repository.ExternalDbRepository
import fr.proline.repository.ProlineDatabaseType
import scala.util.Try
import scala.collection.JavaConverters._
import fr.proline.repository._

/**
 *  drop obsolete proline databases (ps_db and pdi_db).
 *  @param dsConnectorFactory the connector factory to get databases connectors.
 *
 *  @author aromdhani
 *
 */
class DeleteObsoleteDbs(
    val dsConnectorFactory: IDataStoreConnectorFactory) extends LazyLogging {
  var isSuccess: Boolean = false
  def run() {
    if ((dsConnectorFactory == null) || !dsConnectorFactory.isInitialized())
      throw new IllegalArgumentException("Invalid connectorFactory!")
    // Open a connection to UDSdb
    val udsDbConnector = dsConnectorFactory.getUdsDbConnector
    val udsDbContext = new DatabaseConnectionContext(udsDbConnector)
    try {
      val isDbsUpgraded = DbsVersionChecker(udsDbConnector, udsDbContext)
      if (isDbsUpgraded) {
        try {
          logger.debug(s"Dropping ps_db database, please wait...")
          DoJDBCWork.withEzDBC(udsDbContext) { ezDBC =>
            ezDBC.execute("DROP DATABASE IF EXISTS ps_db")
          }
          logger.debug(s"Dropping pdi_db database, please wait...")
          DoJDBCWork.withEzDBC(udsDbContext) { ezDBC =>
            ezDBC.execute("DROP DATABASE IF EXISTS pdi_db")
          }
          isSuccess = true
        } catch {
          case t: Throwable => {
            logger.error("Error while trying to obsolete databases: ", t.printStackTrace())
          }
        }
      }
    } finally {
      // close UDSdb Context 
      if (udsDbContext != null)
        udsDbContext.close()
      // Close UDSdb connector at the end
      if (udsDbConnector != null && !udsDbConnector.isClosed())
        udsDbConnector.close()
    }
  }
}

object DeleteObsoleteDbs extends LazyLogging {

  /**
   *  delete obsolete Proline databases.
   *  @param dsConnectorFactory  The connector factory to get databases connectors.
   */
  def apply(dsConnectorFactory: IDataStoreConnectorFactory): Boolean = {
    var isSuccess: Boolean = false
    try {
      val deleteObsoleteDbs = new DeleteObsoleteDbs(dsConnectorFactory)
      deleteObsoleteDbs.run()
      isSuccess = deleteObsoleteDbs.isSuccess
      logger.info("Deleting obsolete proline databases have been finished successfully!")
    } catch {
      case t: Throwable => {
        logger.error("Deleting obsolete proline databases have been failed: ", t.printStackTrace())
      }
    }
    isSuccess
  }
}

object DbsVersionChecker extends LazyLogging {
  def apply(dbConnector: IDatabaseConnector,
    udsDbCtx: DatabaseConnectionContext): Boolean = {
    var isDbsUpgraded = false
    if (dbConnector == null) {
      logger.warn(s"DataStoreConnectorFactory has no valid connector.")
    } else {
      try {
        val driverType = dbConnector.getDriverType
        if (driverType == DriverType.POSTGRESQL || driverType == DriverType.H2) {

          val ezDBC = ProlineEzDBC(dbConnector.getDataSource.getConnection, dbConnector.getDriverType)

          // Try to retrieve the current version from uds_db version
          val udsDbVersion = ezDBC.selectHead("""SELECT "version" FROM "schema_version" ORDER BY "version_rank" DESC LIMIT 1""") { r =>
            r.nextString
          }
          udsDbCtx.tryInTransaction {
            val udsEM = udsDbCtx.getEntityManager
            val udsDbVersionOpt = Try(Some(udsDbVersion.toDouble)).getOrElse(None)
            val isUdsDbUpdated = udsDbVersionOpt.isDefined && udsDbVersionOpt.get >= 0.8
            require(isUdsDbUpdated, "uds database is not upgraded! Please upgrade your databases before to delete obsolete databases!")

            //Check that all MSI Dbs are upgraded and their versions are above 1.0
            val udsExternalDbClass = classOf[ExternalDb]
            val externalDbsMsiQuery = udsEM.createNamedQuery("findExternalDbByType", udsExternalDbClass).setParameter("type", ProlineDatabaseType.MSI).getResultList()
            val externalDbMsiVersionOpt = externalDbsMsiQuery.asScala.toList.map { extDb => Try(Some(extDb.getDbVersion.toDouble)).getOrElse(None) }
            val isAllMsiDbsUpdated = externalDbMsiVersionOpt.forall { extDbVersion => extDbVersion.isDefined && extDbVersion.get >= 0.9 }

            //Check that all LCMS Dbs are upgraded and their versions are above 0.7
            val externalDbsLcmsQuery = udsEM.createNamedQuery("findExternalDbByType", udsExternalDbClass).setParameter("type", ProlineDatabaseType.LCMS).getResultList()
            val externalDbsLcmsVersionOpt = externalDbsMsiQuery.asScala.toList.map { extDb => Try(Some(extDb.getDbVersion.toDouble)).getOrElse(None) }
            val isAllLcmsDbsUpdated = externalDbsLcmsVersionOpt.forall { extDbVersion => extDbVersion.isDefined && extDbVersion.get >= 0.7 }

            require(isAllMsiDbsUpdated && isAllLcmsDbsUpdated, "msi and lcms databases are not upgraded! Please upgrade your databases before to delete obsolete databases!")
            isDbsUpgraded = Seq(isUdsDbUpdated, isAllMsiDbsUpdated, isAllLcmsDbsUpdated).forall(_.==(true))
          }
        } else {
          logger.error("Error unsupported driver type!")
        }
      } catch {
        case t: Throwable => logger.error("Error while trying to check databases version: ", t.printStackTrace())
      }
    }
    isDbsUpgraded
  }
}
