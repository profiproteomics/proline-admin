package fr.proline.admin.service.user

import com.typesafe.scalalogging.StrictLogging

import fr.proline.admin.service.ICommandWork
import fr.proline.context.DatabaseConnectionContext
import fr.proline.core.dal.ProlineEzDBC
import fr.proline.core.orm.uds.repository.ExternalDbRepository
import fr.proline.core.orm.uds.repository.ProjectRepository
import fr.proline.core.orm.uds.{ UserAccount => UdsUser }
import fr.proline.repository._
import fr.proline.core.dal.context._
import javax.persistence.EntityManager
import scala.collection.JavaConversions._
import collection.mutable.{ Map, Set }
import java.io.{ ByteArrayOutputStream, PrintStream }
import java.sql.Connection
/**
 *
 * Check Proline Databases versions (UDS ,MSI and LCMS Dbs) and get the available updates.
 *
 * @param dsConnectorFactory
 *            Must be a valid initialized DataStoreConnectorFactory instance.
 */
class CheckForUpdates(
    val dsConnectorFactory: IDataStoreConnectorFactory) extends ICommandWork with StrictLogging {
  var undoneMigrationsByDb = Map[String, Map[String, String]]()
  def doWork(): Unit = {

    if ((dsConnectorFactory == null) || !dsConnectorFactory.isInitialized)
      throw new IllegalArgumentException("Invalid connectorFactory")

    // Open a connection to the UDSdb
    val udsDbConnector = dsConnectorFactory.getUdsDbConnector
    val udsDbCtx = new DatabaseConnectionContext(udsDbConnector)
    try {
      udsDbCtx.tryInTransaction {
        val udsEM = udsDbCtx.getEntityManager
        /* Check undone migrations for Proline projects (MSI and LCMS) */
        val projectIds = ProjectRepository.findAllProjectIds(udsEM)
        if ((projectIds != null) && !projectIds.isEmpty) {

          for (projectId <- projectIds) {
            logger.debug(s"Checking for available updates for the project with id= #$projectId")
            /* Check for available updates for MSI Db */
            val msiDbConnector = dsConnectorFactory.getMsiDbConnector(projectId)
            val undoneMigrationsMsiDb = CheckDbUpdates(
              msiDbConnector,
              s"MSIdb (project #$projectId)")
            if (!undoneMigrationsMsiDb.isEmpty) undoneMigrationsByDb += (s"MSIdb_project_$projectId" -> undoneMigrationsMsiDb)
            /* Check for available updates for LCMS Db */
            val lcMsDbConnector = dsConnectorFactory.getLcMsDbConnector(projectId)
            val undoneMigrationsLcmsDb = CheckDbUpdates(
              lcMsDbConnector,
              s"LCMSdb (project #$projectId)")
            if (!undoneMigrationsLcmsDb.isEmpty) undoneMigrationsByDb += (s"LCMSdb_project_$projectId" -> undoneMigrationsLcmsDb)
          }
        }
        /* Check for updates for UDS Db  */
        val undoneMigrationsUdsDb = CheckDbUpdates(
          udsDbConnector,
          "UDSdb",
          closeConnector = false)
        if (!undoneMigrationsUdsDb.isEmpty) undoneMigrationsByDb += ("UDSdb" -> undoneMigrationsUdsDb)
        if (! undoneMigrationsByDb.isEmpty) logger.info("Need Update Migration:YES")
        else
          logger.info("Need Update Migration:NO")
        if (CheckDbUpdates.failedDbNameSet.isEmpty) logger.info("Check for updates has finished successfully!")
        else
          logger.warn(s"--- Check for updates has finished, but some databases cannot migrate: ${CheckDbUpdates.failedDbNameSet.mkString(",")}")
      }
    } catch {
      case t: Throwable => logger.error("Error while trying to check for available updates! ", t.printStackTrace())
    } finally {
      // Close UDSdb connection context
      if (udsDbCtx != null) {
        udsDbCtx.close()
      }
    }
  }
}

object CheckDbUpdates extends StrictLogging {

  var failedDbNameSet: Set[String] = Set.empty

  /** Check that the connection to database is established before to check script's state from Flyway */
  private def isConnectionEstablished(dbConnector: IDatabaseConnector): Boolean = {
    var connection: Option[Connection] = None
    var isConnectionEstablished: Boolean = false
    var stream = new ByteArrayOutputStream()
    try {
      logger.debug("Testing the database connection. Please wait...")
      var ps = new PrintStream(stream)
      System.setErr(ps)
      connection = Option { dbConnector.createUnmanagedConnection() }
      isConnectionEstablished = connection.isDefined
    } catch {
      case ex: Exception =>
        logger.error(s"Cannot get connection : ${ex.getMessage} ")
    } finally {
      stream.close()
      System.setErr(System.out)
      connection.collect {
        case (dbConn) if (!dbConn.isClosed()) => {
          try { dbConn.close() }
          catch { case ex: Exception => logger.error(s"Cannot close connection ${ex.getMessage}") }
        }
      }
    }
    isConnectionEstablished
  }

  def apply(
    dbConnector: IDatabaseConnector,
    dbShortName: String,
    closeConnector: Boolean = true): Map[String, String] = {
    var undoneMigrations: Map[String, String] = Map()
    if (dbConnector == null) {
      logger.warn(s"DataStoreConnectorFactory has no valid connector to $dbShortName")
    } else {
      try {
        if (isConnectionEstablished(dbConnector)) {
          val driverType = dbConnector.getDriverType
          if (driverType != DriverType.SQLITE) {

            /* Retrieve the available updates */
            val undoneMigrationsByDb = DatabaseUpgrader.getUndoneMigrations(dbConnector)
            val undoneMigrationsAsMap = undoneMigrationsByDb.get(dbConnector.getProlineDatabaseType())
            if (undoneMigrationsAsMap.isEmpty) {
              logger.info(s"There are no available scripts to apply for: $dbShortName")
            } else {
              undoneMigrationsAsMap.foreach {
                case (script, state) => {
                  undoneMigrations += (script -> state.toString)
                  logger.warn(s"The script $script is $state. To apply undone migration(s), please upgrade Proline databases.")
                }
              }
            }
            /* Check uds_db version */
            if (dbConnector.getProlineDatabaseType() == ProlineDatabaseType.UDS) {
              val ezDBC = ProlineEzDBC(dbConnector.getDataSource.getConnection, dbConnector.getDriverType)
              // Try to retrieve the current version from uds_db version
              val udsDbVersion =
                ezDBC.selectHead("""SELECT "version_rank" FROM "schema_version" ORDER BY "version_rank" DESC LIMIT 1""") { r =>
                  r.nextString
                }
              Option(udsDbVersion.toInt).collect {
                case (version) if (version < 8) => logger.warn("Proline databases are not upgraded! Important  updates are needed.")
              }
            }
          }
        } else {
          failedDbNameSet += dbShortName
        }
      } finally {
        if (closeConnector)
          dbConnector.close()
      }
    }
    undoneMigrations
  }
}

object CheckForUpdates extends StrictLogging {

  def apply(dsConnectorFactory: IDataStoreConnectorFactory): Unit = {

    try {
      new CheckForUpdates(dsConnectorFactory).doWork()
    } catch {
      case t: Throwable =>
        logger.error("Error while trying to check for Proline updates ", t.printStackTrace())
    }
  }

}
