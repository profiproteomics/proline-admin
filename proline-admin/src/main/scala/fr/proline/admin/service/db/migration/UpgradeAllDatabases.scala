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
import fr.proline.core.orm.uds.ExternalDb
import fr.proline.repository._
import scala.collection.mutable.Set
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import java.io.{ ByteArrayOutputStream, PrintStream }
import java.sql.Connection
import scala.util.Try
import com.google.gson.JsonObject
import com.google.gson.JsonParser
/**
 *
 * Upgrades all Proline Databases (UDS and all projects MSI and LCMS Dbs).
 *
 * @param dsConnectorFactory
 *            Must be a valid initialized DataStoreConnectorFactory instance.
 */
class UpgradeAllDatabases(
    val dsConnectorFactory: IDataStoreConnectorFactory) extends ICommandWork with StrictLogging {
  var failedDbSet: Set[String] = Set.empty
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

      // Create default user admin
      _createDefaultAdmin(udsEM, udsDbConnector.getDriverType)
      // Retrieve ps database migration state. This is for remove ps_db
      _isPsDbMigrationOk(udsEM)
      if (failedDbSet.isEmpty) { logger.info("Proline databases upgrade has finished successfully!") }
      else {
        logger.warn(s"--- Proline databases upgrade has finished, but some databases cannot migrate: ${failedDbSet.mkString(",")}")
      }
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
  private def _createDefaultAdmin(udsEM: EntityManager, driverType: DriverType) {
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

  /** Update external db with schema db version */
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

  /** Retrieve ps_db migration state */
  private def _isPsDbMigrationOk(udsEM: EntityManager): Unit = {
    var parser = new JsonParser()
    val udsExternalDbClass = classOf[ExternalDb]
    val jpqlSelectExternalDb = s"Select db FROM ${udsExternalDbClass.getName} db where db.type = :type"
    val udsExternalDbs = udsEM.createQuery(jpqlSelectExternalDb, udsExternalDbClass)
      .setParameter("type", fr.proline.repository.ProlineDatabaseType.MSI)
      .getResultList()
    val udsExternalDbsArray = udsExternalDbs.asScala.toArray
    udsExternalDbsArray.foreach {
      extDb =>
        val properties = extDb.getSerializedProperties()
        // We use a fallback to an empty JSON object to avoid error if processing an externalDb that has no serialized properties.
        val extDbJsonProps: JsonObject = Try(parser.parse(properties).getAsJsonObject()).getOrElse(parser.parse("{}").getAsJsonObject())
        if (!extDbJsonProps.has("is_psdb_migration_ok") || (!extDbJsonProps.get("is_psdb_migration_ok").getAsBoolean)) failedDbSet += extDb.getDbName()
    }
  }
}

object UpgradeDatabase extends StrictLogging {
  /** Check database connection before to upgrade */
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
  /** Upgrade database  */
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
