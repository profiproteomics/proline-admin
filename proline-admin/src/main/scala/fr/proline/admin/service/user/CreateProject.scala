package fr.proline.admin.service.user

import javax.persistence.EntityTransaction
import com.typesafe.scalalogging.LazyLogging
import fr.profi.jdbc.easy.{ date2Formattable, int2Formattable, string2Formattable }
import fr.proline.admin.service.db.{ CreateProjectDBs, SetupProline }
import fr.proline.admin.service.ICommandWork
import fr.proline.core.orm.uds.{ Dataset => UdsDataset, Project => UdsProject, UserAccount => UdsUser }
import fr.proline.core.orm.uds.repository.ExternalDbRepository
import fr.proline.core.orm.util.DataStoreConnectorFactory
import fr.proline.repository.IDataStoreConnectorFactory
import fr.proline.repository.DriverType
import fr.proline.core.dal.ProlineEzDBC
import fr.proline.repository.ProlineDatabaseType
import fr.proline.context.DatabaseConnectionContext
import fr.proline.core.dal.context._
import fr.proline.repository._
/**
 * @author David Bouyssie
 *
 */
class CreateProject(
    connectorFactory: IDataStoreConnectorFactory,
    udsDbContext: DatabaseConnectionContext,
    projectName: String,
    projectDescription: String,
    ownerId: Long,
    createDbs: Boolean = true) extends ICommandWork with LazyLogging {

  var projectId: Long = -1L

  def doWork() {

    // Retrieve UDS entity manager
    val udsEM = udsDbContext.getEntityManager
    var localUdsTransaction: EntityTransaction = null
    var udsTransacOK: Boolean = false
    try {
      if (!udsDbContext.isInTransaction) {
        localUdsTransaction = udsEM.getTransaction
        localUdsTransaction.begin()
        udsTransacOK = false
      }
      // Retrieve the owner => we assume it has been already created
      val udsUser = udsEM.find(classOf[UdsUser], ownerId)
      require(udsUser != null, "undefined user with id=" + ownerId)
      // Create the project
      val udsProject = new UdsProject(udsUser)
      udsProject.setName(projectName)
      udsProject.setDescription(projectDescription)
      udsProject.setCreationTimestamp(fr.profi.util.sql.getTimeAsSQLTimestamp)

      udsEM.persist(udsProject)

      // Create an empty TRASH dataset for this project
      val udsDataset = new UdsDataset(udsProject)
      udsDataset.setNumber(1)
      udsDataset.setName(UdsDataset.DatasetType.TRASH.toString)
      udsDataset.setType(UdsDataset.DatasetType.TRASH)
      udsDataset.setCreationTimestamp(fr.profi.util.sql.getTimeAsSQLTimestamp)
      udsDataset.setChildrenCount(0)

      udsEM.persist(udsDataset)

      if (localUdsTransaction != null) {
        localUdsTransaction.commit()
        udsTransacOK = true
      }
      projectId = udsProject.getId
      logger.debug("Project #" + projectId + " has been created")
    } finally {
      if ((localUdsTransaction != null) && !udsTransacOK && udsDbContext.getDriverType() != DriverType.SQLITE) {
        logger.info("Rollbacking current UDS Db Transaction")
        try {
          localUdsTransaction.rollback()
        } catch {
          case ex: Exception => logger.error("Error rollbacking UDS Db Transaction", ex)
        }
      }
    }
    if ((projectId > 0L) && (createDbs)) {
      mergeExternalDbs()
    } else {
      logger.error("Failed to create project DBs!")
    }
  }

  /** Create project databases and update project with current external DBs version */
  private def mergeExternalDbs() {
    // Create project databases
    new CreateProjectDBs(udsDbContext, SetupProline.config, projectId).doWork()
    // Update project with the current external DBs version
    try {
      val msiDbConnector = connectorFactory.getMsiDbConnector(projectId)
      val msiDbVersionOpt = ProjectUtils.retrieveExtDbVersion(msiDbConnector)
      val lcmsDbConnector = connectorFactory.getLcMsDbConnector(projectId)
      val lcmsDbVersionOpt = ProjectUtils.retrieveExtDbVersion(lcmsDbConnector)
      ProjectUtils.updateExternalDbs(udsDbContext, projectId, msiDbVersionOpt, lcmsDbVersionOpt)
    } catch {
      case t: Throwable => logger.error("Error while trying to update project schema version", t.getMessage())
    }
  }

}

object CreateProject extends LazyLogging {

  def apply(name: String, description: String, ownerId: Long): Long = {

    var projectId: Long = -1L

    // Retrieve Proline configuration
    val prolineConf = SetupProline.config

    var localUdsDbConnector: Boolean = false

    val connectorFactory = DataStoreConnectorFactory.getInstance()
    val udsDbConnector = if (connectorFactory.isInitialized) {
      connectorFactory.getUdsDbConnector
    } else {
      // Instantiate a database manager
      val udsDBConfig = prolineConf.udsDBConfig
      val newUdsDbConnector = udsDBConfig.toNewConnector()
      localUdsDbConnector = true
      newUdsDbConnector
    }
    try {
      if ((connectorFactory == null) || !connectorFactory.isInitialized()) {
        connectorFactory.initialize(udsDbConnector)
      }
      val udsDbContext = new DatabaseConnectionContext(udsDbConnector)
      try {
        // Create project
        val projectCreator = new CreateProject(
          connectorFactory,
          udsDbContext,
          name,
          description,
          ownerId,
          createDbs = true)
        projectCreator.doWork()
        projectId = projectCreator.projectId

      } finally {
        logger.debug("Closing current UDS Db Context")
        try {
          udsDbContext.close()
        } catch {
          case exClose: Exception => logger.error("Error closing UDS Db Context", exClose)
        }
      }

    } finally {

      if (localUdsDbConnector && (udsDbConnector != null)) {
        udsDbConnector.close()
      }
    }
    projectId
  }
}

object ProjectUtils extends LazyLogging {

  /** Retrieve external database schema version */
  def retrieveExtDbVersion(dbConnector: IDatabaseConnector): Option[String] = {
    var schemaVersionOpt: Option[String] = None
    if (dbConnector == null) {
      logger.warn("DataStoreConnectorFactory has no valid connector")
    } else {
      try {
        val driverType = dbConnector.getDriverType
        if (driverType != DriverType.SQLITE) {
          // Try to retrieve the version reached after the applied migration
          val ezDBC = ProlineEzDBC(dbConnector.getDataSource.getConnection, dbConnector.getDriverType)
          schemaVersionOpt = Option {
            ezDBC.selectHead("""SELECT "version" FROM "schema_version" ORDER BY "version_rank" DESC LIMIT 1""") { r =>
              r.nextString
            }
          }
        } else {
          logger.error("Error unsupported driver type!")
        }
      } catch {
        case t: Throwable => logger.error("Error while trying to retrieve project version: ", t.printStackTrace())
      } finally {
        if (dbConnector != null && !dbConnector.isClosed())
          dbConnector.close()
      }
    }
    schemaVersionOpt
  }

  /** Update external databases with current project schema version */
  def updateExternalDbs(udsDbContext: DatabaseConnectionContext, projectId: Long, msiDbVersionOpt: Option[String], lcmsDbVersionOpt: Option[String]): Boolean = {
    val isTxOk = udsDbContext.tryInTransaction {
      // Creation UDS entity manager
      val udsEM = udsDbContext.getEntityManager
      msiDbVersionOpt.foreach { currentVersion =>
        val extDbMsi = ExternalDbRepository.findExternalByTypeAndProject(
          udsEM,
          ProlineDatabaseType.MSI,
          udsEM.find(classOf[fr.proline.core.orm.uds.Project], projectId))
        extDbMsi.setDbVersion(currentVersion)
      }
      lcmsDbVersionOpt.foreach { currentVersion =>
        val extDbLcms = ExternalDbRepository.findExternalByTypeAndProject(
          udsEM,
          ProlineDatabaseType.LCMS,
          udsEM.find(classOf[fr.proline.core.orm.uds.Project], projectId))
        extDbLcms.setDbVersion(currentVersion)
      }
    }
    isTxOk
  }
}
