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
import java.sql.Connection
import scala.collection.JavaConversions._
import java.io.{ ByteArrayOutputStream, PrintStream }
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
      //create default user admin  when it does not exist
      createDefaultAdmin(udsEM, udsTx)

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
  private def createDefaultAdmin(udsEM: EntityManager, udsTx: EntityTransaction) {
    try {
      val defaultAdminQuery = udsEM.createQuery("select user from UserAccount user where user.login='admin'")
      val defaultAdmin = defaultAdminQuery.getResultList()
      if (defaultAdmin.isEmpty) {
        udsEM.getTransaction()
        udsTx.begin()
        logger.info("Creating default admin user")
        val udsUser = new UdsUser()
        udsUser.setLogin("admin")
        udsUser.setPasswordHash(sha256Hex("proline"))
        udsUser.setCreationMode("AUTO")
        val serializedPropertiesMap = new java.util.HashMap[String, Object]
        serializedPropertiesMap.put("user_group", UdsUser.UserGroupType.ADMIN.name())
        udsUser.setSerializedPropertiesAsMap(serializedPropertiesMap)
        udsEM.persist(udsUser)
        udsTx.commit()
        if (udsUser.getId > 0L) logger.info("Default admin user has been created successfully!")
      }
    } catch {
      case t: Throwable => logger.error("Error while trying to create default admin user", t.getMessage)
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

  /** Check that the connection to database is established before to upgarde it */
  private def isDbConnected(dbConnector: IDatabaseConnector): Boolean = {
    var connection: Option[Connection] = None
    var isDbConnected: Boolean = false
    var stream = new ByteArrayOutputStream()
    try {
      logger.info("Checking database connection. Please wait ...")
      var ps = new PrintStream(stream)
      System.setErr(ps)
      connection = Option { dbConnector.getDataSource().getConnection() }
      isDbConnected = connection.isDefined
    } catch {
      case ex: Exception =>
        logger.error(s"Unable to connect to the database: ${ex.getMessage} ")
    } finally {
      stream.close()
      System.setErr(System.out)
      connection.collect {
        case (dbConn) if (!dbConn.isClosed()) => dbConn.close()
      }
    }
    isDbConnected
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
        if (isDbConnected(dbConnector)) {
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
      logger.info("Databases successfully upgraded !")
    } catch {
      case t: Throwable =>
        logger.error("Databases upgrade failed !", t)
    }

  }

}
