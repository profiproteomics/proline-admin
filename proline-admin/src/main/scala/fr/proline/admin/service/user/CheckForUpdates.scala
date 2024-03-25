package fr.proline.admin.service.user

import com.typesafe.scalalogging.StrictLogging
import fr.proline.admin.service.ICommandWork
import fr.proline.admin.service.db.migration.{UpgradeMsiDbDefinitions, UpgradeUdsDbDefinitions}
import fr.proline.context.{DatabaseConnectionContext, UdsDbConnectionContext}
import fr.proline.core.dal.ProlineEzDBC
import fr.proline.core.orm.uds.repository.ProjectRepository
import fr.proline.repository._
import fr.proline.core.dal.context._

import scala.collection.JavaConverters._
import collection.mutable.{Map, Set}
import java.io.{ByteArrayOutputStream, PrintStream}
import java.sql.Connection
import scala.collection.mutable
/**
 *
 * Check Proline Databases versions (UDS ,MSI and LCMS Dbs) and get the available updates.
 *
 * @param dsConnectorFactory
 *            Must be a valid initialized DataStoreConnectorFactory instance.
 */
class CheckForUpdates(
    val dsConnectorFactory: IDataStoreConnectorFactory) extends ICommandWork with StrictLogging {
  var undoneMigrationsByDb: mutable.Map[String, mutable.Map[String, String]] = Map[String, Map[String, String]]()
  var dbObjectNeedUpgrade : Boolean = false
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

          for (projectId <- projectIds.asScala) {
            logger.info(s"  --- Checking for available updates for the project with id= #$projectId")

            /* Check for available updates for MSI Db */
            val msiDbConnector = dsConnectorFactory.getMsiDbConnector(projectId)
            val (currentMsiNeedUpdate, undoneMigrationsMsiDb ) = CheckDbUpdates(
              msiDbConnector,
              s"MSIdb (project #$projectId)")

            if(currentMsiNeedUpdate)
              dbObjectNeedUpgrade = true //At least one db need update
            if (undoneMigrationsMsiDb.nonEmpty){
              undoneMigrationsByDb += (s"MSIdb_project_$projectId" -> undoneMigrationsMsiDb)
            }

            /* Check for available updates for LCMS Db */
            val lcMsDbConnector = dsConnectorFactory.getLcMsDbConnector(projectId)
            val (currentLcmsNeedUpdate,  undoneMigrationsLcmsDb) = CheckDbUpdates(
              lcMsDbConnector,
              s"LCMSdb (project #$projectId)")

            if (currentLcmsNeedUpdate)
              dbObjectNeedUpgrade = true //At least one db need update

            if (undoneMigrationsLcmsDb.nonEmpty)
              undoneMigrationsByDb += (s"LCMSdb_project_$projectId" -> undoneMigrationsLcmsDb)

            logger.info(s"  --- Project with id= #$projectId"+" need update ? "+ (if(currentMsiNeedUpdate || currentLcmsNeedUpdate) " YES " else " NO "))
          }
        }

        /* Check for updates for UDS Db  */
        logger.info(s"  --- Checking for available updates for the global database UDS db ")
        val (udsNeedUpdate, undoneMigrationsUdsDb) = CheckDbUpdates(
          udsDbConnector,
          "UDSdb",
          closeConnector = false)

        if (udsNeedUpdate)
          dbObjectNeedUpgrade = true //At least one db need update
        logger.info(s"  --- UDS db need update ? "+ (if(udsNeedUpdate) " YES " else " NO "))

        if (undoneMigrationsUdsDb.nonEmpty)  { //At least a migration to do
          undoneMigrationsByDb += ("UDSdb" -> undoneMigrationsUdsDb)
        }

        if (undoneMigrationsByDb.nonEmpty || dbObjectNeedUpgrade)
          logger.info("Need Update Migration:YES")
        else
          logger.info("Need Update Migration:NO")

        if (CheckDbUpdates.failedDbNameSet.isEmpty)
          logger.info("Check for updates has finished successfully!")
        else
          logger.info(s"--- Warning:  Check for updates has finished, but some databases cannot migrate: ${CheckDbUpdates.failedDbNameSet.mkString(",")}")
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

  var failedDbNameSet: mutable.Set[String] = mutable.Set.empty

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
    closeConnector: Boolean = true): (Boolean, mutable.Map[String, String]) = {
    val undoneMigrations: mutable.Map[String, String] = mutable.Map()
    var dbUpdateNeeded = false

    if (dbConnector == null) {
      logger.warn(s"DataStoreConnectorFactory has no valid connector to $dbShortName")
    } else {
      try {
        if (isConnectionEstablished(dbConnector)) {
          val driverType = dbConnector.getDriverType
          if (driverType != DriverType.SQLITE) {

            /* Retrieve the available updates */
            val undoneMigrationsByDb = DatabaseUpgrader.getUndoneMigrations(dbConnector)
            val undoneMigrationsAsMap = undoneMigrationsByDb.get(dbConnector.getProlineDatabaseType)
            if (undoneMigrationsAsMap.isEmpty) {
              logger.info(s"There are no available scripts to apply for: $dbShortName")
              if (dbConnector.getProlineDatabaseType == ProlineDatabaseType.MSI) {
                val msiDbCtx = new fr.proline.context.MsiDbConnectionContext(dbConnector)
                val dbUpgrade = new UpgradeMsiDbDefinitions(msiDbCtx)
                dbUpdateNeeded = dbUpgrade.needUpgrade()
              } else if (dbConnector.getProlineDatabaseType == ProlineDatabaseType.UDS) {
                val udsDbCtx =  new UdsDbConnectionContext(dbConnector)
                val dbUpgrade = new UpgradeUdsDbDefinitions(udsDbCtx)
                dbUpdateNeeded = dbUpgrade.needUpgrade()
              }
            } else {
              dbUpdateNeeded =true
              undoneMigrationsAsMap.asScala.foreach {
                case (script, state) => {
                  undoneMigrations += (script -> state.toString)
                  logger.warn(s"The script $script is $state. To apply undone migration(s), please upgrade Proline databases.")
                }
              }
            }

            /* Check uds_db version */
            if (dbConnector.getProlineDatabaseType == ProlineDatabaseType.UDS) {
              val ezDBC = ProlineEzDBC(dbConnector.getDataSource.getConnection, dbConnector.getDriverType)
              // Try to retrieve the current version from uds_db version
              val udsDbVersion = ezDBC.selectHead("""SELECT "version_rank" FROM "schema_version" ORDER BY "version_rank" DESC LIMIT 1""") { r =>
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
    (dbUpdateNeeded, undoneMigrations)
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
