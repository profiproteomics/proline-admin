package fr.proline.admin.service.db.migration

import com.typesafe.scalalogging.StrictLogging
import javax.persistence.{ EntityManager, EntityTransaction }
import fr.profi.util.security._
import fr.proline.admin.service.ICommandWork
import fr.proline.context.DatabaseConnectionContext
import fr.proline.core.dal.ProlineEzDBC
import fr.proline.core.orm.uds.repository.ExternalDbRepository
import fr.proline.core.orm.uds.repository.ProjectRepository
import fr.proline.core.orm.uds.{ UserAccount => UdsUser }
import fr.proline.repository._
import scala.collection.mutable.Set
import scala.collection.JavaConversions._
import java.io.{ ByteArrayOutputStream, PrintStream }
import java.sql.Connection
/**
 *
 * Upgrades all Proline Databases (UDS and all projects MSI and LCMS Dbs).
 *
 * @param dsConnectorFactory
 *            Must be a valid initialized DataStoreConnectorFactory instance.
 */
class UpgradeAllDatabases(
    val dsConnectorFactory: IDataStoreConnectorFactory) extends ICommandWork with StrictLogging {

  def doWork(): Unit = {

    if ((dsConnectorFactory == null) || !dsConnectorFactory.isInitialized)
      throw new IllegalArgumentException("Invalid connectorFactory")

    // Open a connection to the UDSdb
    val udsDbConnector = dsConnectorFactory.getUdsDbConnector
    val udsDbCtx = new DatabaseConnectionContext(udsDbConnector)
    val udsEM = udsDbCtx.getEntityManager

    try {

      val udsTx = udsEM.getTransaction()
      udsTx.begin()

      /* Upgrade all Projects (MSI and LCMS) Dbs */
      /* find all project (active/disabled) */
      val projectIds = ProjectRepository.findAllProjectIds(udsEM)

      if ((projectIds != null) && !projectIds.isEmpty) {

        for (projectId <- projectIds) {
          logger.debug(s"Upgrading databases of Project #$projectId")

          /* Upgrade MSI Db */
          val msiDbConnector = dsConnectorFactory.getMsiDbConnector(projectId)
          val msiVersion = UpgradeDatabase(
            msiDbConnector,
            s"MSIdb (project #$projectId)",
            upgradeCallback = { msiVersion =>

              /* Update MSI Db version */
              _updateExternalDbVersion(msiVersion, udsEM, ProlineDatabaseType.MSI, Some(projectId))

              /* Upgrade MSI Db definitions */
              val msiDbCtx = new DatabaseConnectionContext(msiDbConnector)

              try {
                // Executed inside a local transaction (see IUpgradeDb)
                new UpgradeMsiDbDefinitions(msiDbCtx).run()
              } finally {
                msiDbCtx.close()
              }
            })

          /* Upgrade LCMS Db */
          val lcMsDbConnector = dsConnectorFactory.getLcMsDbConnector(projectId)
          UpgradeDatabase(lcMsDbConnector, s"LCMSdb (project #$projectId)", upgradeCallback = { lcMsVersion =>
            /* Update LCMS Db version */
            _updateExternalDbVersion(lcMsVersion, udsEM, ProlineDatabaseType.LCMS, Some(projectId))
          })

        }

      }
      udsEM.flush()
      udsTx.commit()
      udsEM.getTransaction()
      udsTx.begin()

      /* Upgrade UDS Db */
      // Note: was moved after MSI/LCMS databases to handle migration of core V2
      UpgradeDatabase(udsDbConnector, "UDSdb", closeConnector = false, upgradeCallback = { udsDbVersion =>
        /* Upgrade UDS Db definitions */
        new UpgradeUdsDbDefinitions(udsDbCtx).run()
      })

      udsEM.flush()
      udsTx.commit()

      if (UpgradeDatabase.failedConDbNameSet.isEmpty)
        logger.info("Proline databases upgrade has finished successfully!")
      else
        logger.warn(s"Proline databases upgrade has finished, but some databases = ${UpgradeDatabase.failedConDbNameSet.mkString(",")} failed to migrate!")

      //Create default user admin
      createDefaultAdmin(udsEM, udsDbConnector.getDriverType)

    } finally {

      // Close UDSdb connection context
      if (udsDbCtx != null) {
        udsDbCtx.close()
      }

      /*
      // Close UDSdb connector at the end
      if (udsDbConnector != null)
        udsDbConnector.close()
					 */
    }
    ()
  }
  /** Create default Admin user */
  private def createDefaultAdmin(udsEM: EntityManager, driverType: DriverType) {
    var localUdsTransaction: EntityTransaction = null
    try {
      localUdsTransaction = udsEM.getTransaction
      localUdsTransaction.begin()
      val createDefaultAdminQuery = udsEM.createQuery("select user from UserAccount user where user.login='admin'")
      val defaultAdminList = createDefaultAdminQuery.getResultList()
      if (defaultAdminList.isEmpty) {
        logger.info("Creating default admin user")
        val udsUser = new UdsUser()
        udsUser.setLogin("admin")
        udsUser.setPasswordHash(sha256Hex("proline"))
        udsUser.setCreationMode("AUTO")
        val serializedPropertiesMap = new java.util.HashMap[String, Object]
        serializedPropertiesMap.put("user_group", UdsUser.UserGroupType.ADMIN.name())
        udsUser.setSerializedPropertiesAsMap(serializedPropertiesMap)
        udsEM.persist(udsUser)
        localUdsTransaction.commit()
        if (udsUser.getId > 0L) logger.info("Default admin user has been created successfully!")
      }
    } catch {
      case ex: Exception => {
        logger.error("Error while trying to create default admin user ", ex.getMessage)
        if (localUdsTransaction != null && driverType != DriverType.SQLITE)
          logger.info("Rollbacking current UDS Db Transaction")
        try {
          localUdsTransaction.rollback()
        } catch {
          case ex: Exception => logger.error("Error rollbacking UDS Db Transaction", ex.getMessage)
        }
      }
    }
  }

  private def _updateExternalDbVersion(newVersion: String, udsEM: EntityManager, dbType: ProlineDatabaseType, projectId: Option[Long] = None) {

    val extDb = projectId match {
      case None => ExternalDbRepository.findExternalByType(udsEM, dbType)
      case Some(id) => ExternalDbRepository.findExternalByTypeAndProject(
        udsEM,
        dbType,
        udsEM.find(classOf[fr.proline.core.orm.uds.Project], id))
    }

    extDb.setDbVersion(newVersion)
  }

}

object UpgradeDatabase extends StrictLogging {

  var failedConDbNameSet: Set[String] = Set.empty

  /** Check that the connection to database is established before to upgrade it */
  private def isConnectionEstablished(dbConnector: IDatabaseConnector): Boolean = {
    var connection: Option[Connection] = None
    var isConnectionEstablished: Boolean = false
    var stream = new ByteArrayOutputStream()
    try {
      logger.info("Checking database connection. Please wait ...")
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
    closeConnector: Boolean = true,
    repairChecksum: Boolean = true, // TODO: add this param to GUI
    upgradeCallback: String => Unit = null): IDatabaseConnector = {

    if (dbConnector == null) {
      logger.warn(s"DataStoreConnectorFactory has no valid connector to $dbShortName")
    } else {
      try {
        if (isConnectionEstablished(dbConnector)) {
          val dbMigrationCount = DatabaseUpgrader.upgradeDatabase(dbConnector, repairChecksum)
          if (dbMigrationCount < 0) {
            throw new Exception(s"Unable to upgrade $dbShortName")
          } else {
            logger.info(s"$dbShortName: $dbMigrationCount migration(s) done.")
          }
          val driverType = dbConnector.getDriverType

          // TODO: find a way to migrate SQLite databases using flyway
          val version = if (driverType == DriverType.SQLITE) "no.version"
          else {
            // Try to retrieve the version reached after the applied migration
            val ezDBC = ProlineEzDBC(dbConnector.getDataSource.getConnection, dbConnector.getDriverType)
            ezDBC.selectHead("""SELECT "version" FROM "schema_version" ORDER BY "version_rank" DESC LIMIT 1""") { r =>
              r.nextString
            }
          }
          if (upgradeCallback != null)
            upgradeCallback(version)
        } else {
          //Set of database names that failed to connect   
          failedConDbNameSet += dbShortName
        }

      } finally {
        if (closeConnector)
          dbConnector.close()
      }
    }
    dbConnector
  }
}

object UpgradeAllDatabases extends StrictLogging {

  def apply(dsConnectorFactory: IDataStoreConnectorFactory): Unit = {

    try {
      new UpgradeAllDatabases(dsConnectorFactory).doWork()
    } catch {
      case t: Throwable =>
        logger.error("Databases upgrade failed !", t)
    }
  }

}
