package fr.proline.admin.gui.process

import com.typesafe.scalalogging.LazyLogging

import scala.collection.JavaConverters._

import fr.proline.admin.service.db.SetupProline
import fr.proline.admin.service.db.setup.DatabaseSetupConfig
import fr.proline.context.DatabaseConnectionContext
import fr.proline.core.orm.uds.{ ExternalDb, Project, UserAccount }
import fr.proline.core.orm.uds.repository.ExternalDbRepository
import fr.proline.core.orm.uds.repository.ProjectRepository
import fr.proline.repository.DatabaseConnectorFactory
import fr.proline.repository.IDataStoreConnectorFactory
import fr.proline.repository.IDatabaseConnector
import fr.proline.repository.ProlineDatabaseType
import fr.proline.repository.ProlineDatabaseType.MSI
import fr.proline.repository.ProlineDatabaseType.LCMS
import fr.proline.core.dal.DoJDBCWork
import scala.util.Try

/**
 * Some utilities relative to UDS database connection
 */
object UdsRepository extends LazyLogging {

  private var _udsDbConfig: DatabaseSetupConfig = null
  private var _udsDbConnector: IDatabaseConnector = null
  private var _udsDbContext: DatabaseConnectionContext = null
  private var _dsConnectorFactory: IDataStoreConnectorFactory = null

  /**
   * Get UDS database CONFIG
   */
  def getUdsDbConfig(): DatabaseSetupConfig = {
    _udsDbConfig = SetupProline.getUpdatedConfig.udsDBConfig
    _udsDbConfig
  }

  def setUdsDbConfig(udsDbConfig: DatabaseSetupConfig) {
    require(udsDbConfig != null, "udsDbConfig is null")

    // Set new udsDbConfig
    this._udsDbConfig = udsDbConfig
    // Close DataStore connector factory
    if (_dsConnectorFactory != null) {
      _dsConnectorFactory.closeAll()
      _dsConnectorFactory = null
    }

    // Close udsDbContext
    if (_udsDbContext != null) {
      if (_udsDbContext.isClosed == false) {
        // An error message will be displayed by proline Databases (EntityManager is closed): thread problem?
        // And there's nothing we can do...
        _udsDbContext.close()
        _udsDbContext = null
      }
    }

    // Close udsDbConnector //TODO: this.closeUdsDbConnector()
    if (_udsDbConnector != null) {
      if (_udsDbConnector.isClosed == false) _udsDbConnector.close()
      _udsDbConnector = null
    }
  }

  /**
   * Get UDS database CONNECTOR
   */
  def getUdsDbConnector(): IDatabaseConnector = {
    if (_udsDbConnector == null) {
      _udsDbConnector = this.getUdsDbConfig.toNewConnector()
    }
    _udsDbConnector
  }

  /**
   * Get UDS database CONTEXT
   */
  def getUdsDbContext(): DatabaseConnectionContext = {
    if (_udsDbContext == null) {
      _udsDbContext = new DatabaseConnectionContext(getUdsDbConnector)
    }
    _udsDbContext
  }

  /**
   * Get datastore connector factory using UDS database
   */
  def getDataStoreConnFactory(): IDataStoreConnectorFactory = {
    if (_dsConnectorFactory == null) {
      // FIXME: fix the EntityManager is already closed bug (from DataStoreConnectorFactory) to reuse existing "udsDbConnector"
      _dsConnectorFactory = new DynamicDataStoreConnectorFactory(getUdsDbConnector())
    }
    _dsConnectorFactory
  }

  /**
   * Get userName and password from udsDbConfig
   *
   */
  def getConProperties(): (String, String) = {
    require(!getUdsDbConfig.userName.isEmpty(), "Username must not be empty.")
    (getUdsDbConfig.userName, getUdsDbConfig.password)
  }

  /**
   *  Test connection with database
   */
  def isUdsDbReachable(verbose: Boolean = true): Boolean = {

    val udsDbConnector = getUdsDbConnector()

    var udsDbCtx: DatabaseConnectionContext = null //especially created for SQLite test, needs to be closed in this method

    try {

      /** Check to retrieve DB connection */
      logger.debug("Checking connection to UDSdb. Please wait ...")

      udsDbConnector.getDataSource().getConnection()

      /** Additionnal check for file-based databases (SQLite) */
      udsDbCtx = new DatabaseConnectionContext(udsDbConnector)
      udsDbCtx.getEntityManager().find(classOf[ExternalDb], 1L)
      logger.debug("INFO - Proline is already set up !")
      true
    } catch {
      case t: Throwable => {
        System.err.println("WARN - Proline is not set up !")
        false
      }
    } finally {
      if (udsDbCtx != null) udsDbCtx.close()
    }
  }
  /**
   *  Get the exhaustive list of UserAccount instances in database as a map of type "login ->  id"
   */
  def getAllUserAccounts(): Array[UserAccount] = {
    val udsDbContext = this.getUdsDbContext()
    try {
      val udsEM = udsDbContext.getEntityManager()
      udsEM.clear()
      val UdsUserAccountClass = classOf[UserAccount]
      val jpqlSelectUserAccount = s"FROM ${UdsUserAccountClass.getName}"
      val udsUsers = udsEM.createQuery(jpqlSelectUserAccount, UdsUserAccountClass).getResultList()
      val res = udsUsers.asScala.toArray
      res
    } catch {
      case t: Throwable => {
        synchronized {
          logger.warn("Can't load user accounts from UDSdb:")
          logger.warn(t.getLocalizedMessage())
          println("ERROR - Can't load user accounts from UDSdb")
          throw t
        }
      }
    }
  }

  /**
   *  Get the exhaustive list of Project instances in database
   */
  def getAllProjects(): Array[Project] = {
    val udsDbContext = this.getUdsDbContext()
    val projects = try {
      val udsEM = udsDbContext.getEntityManager()
      udsEM.clear()
      val UdsProjectClass = classOf[Project]
      val jpqlSelectProject = s"FROM ${UdsProjectClass.getName}"
      val udsProjects = udsEM.createQuery(jpqlSelectProject, UdsProjectClass).getResultList()
      val result = udsProjects.asScala.toArray
      result
    } catch {
      case t: Throwable => {
        synchronized {
          logger.warn("Can't load projects from UDSdb :", t)
          System.err.println("ERROR - Can't load projects from UDSdb")
          System.err.println(t.getMessage())
          throw t
        }
      }
    }
    projects
  }
  
  /**
   *  @return An array of external_db
   *
   */

  def getAllExtDbs(): Array[ExternalDb] = {
    val udsDbContext = this.getUdsDbContext()
    val externalDbs = try {
      val udsEM = udsDbContext.getEntityManager()
      udsEM.clear()
      val externalDbClass = classOf[ExternalDb]
      val jpqlSelectExternalDb = s"Select ed FROM ${externalDbClass.getName} ed where ed.type = :msi or ed.type = :lcms"
      val externalDbList = udsEM.createQuery(jpqlSelectExternalDb, externalDbClass)
        .setParameter("msi", fr.proline.repository.ProlineDatabaseType.MSI)
        .setParameter("lcms", fr.proline.repository.ProlineDatabaseType.LCMS)
        .getResultList()
      externalDbList.asScala.toArray
    } catch {
      case t: Throwable => {
        synchronized {
          logger.warn("Can't load ExternalDb from UDSdb :", t)
          System.err.println("ERROR - Can't load ExternalDb from UDSdb")
          System.err.println(t.getMessage())
        }
        throw t
      }
    }
    externalDbs
  }

  /**
   *  Get the exhaustive list of Project instances in database, grouped by owner
   */
  def getAllProjectsGroupedByOwner(): Map[UserAccount, Array[Project]] = {
    getAllProjects().groupBy(_.getOwner())
  }

  /**
   *  Get projects list for a given user (owner)
   */
  def findProjectsByOwnerId(userId: Long): Array[Project] = {
    ProjectRepository.findOwnedProjects(this.getUdsDbContext.getEntityManager(), userId).asScala.toArray
  }

  /**
   * Get External_db MSI by project
   */
  def getProjectMsiVerison(project: Project): ExternalDb = {
    ExternalDbRepository.findExternalByTypeAndProject(this.getUdsDbContext.getEntityManager(), fr.proline.repository.ProlineDatabaseType.MSI, project)
  }

  /**
   * Get External_db LCMS version by project
   *
   */

  def getProjectLcmsVersion(project: Project): ExternalDb = {
    ExternalDbRepository.findExternalByTypeAndProject(this.getUdsDbContext.getEntityManager(), fr.proline.repository.ProlineDatabaseType.LCMS, project)
  }
  /**
   * Compute the size of  lcms database
   * @param projectId The project id to compute its database size.
   *
   */
  def computeLcmsSize(projectId: Long): String = {
    Try {
      var size: String = "no.size"
      DoJDBCWork.withEzDBC(this.getUdsDbContext) { ezDBC =>
        ezDBC.selectAndProcess("SELECT pg_size_pretty(pg_database_size(datname)) as lcmsSize FROM pg_database WHERE datname='lcms_db_project_" + projectId + "'") { record =>
          size = record.getString("lcmsSize")
        }
      }
      size
    }.getOrElse("no.size")
  }
  /**
   * Compute the size of msi database
   * @param projectId The project id to compute its database size.
   *
   */

  def computeMsiSize(projectId: Long): String = {
    Try {
      var size: String = "no.size"
      DoJDBCWork.withEzDBC(this.getUdsDbContext) { ezDBC =>
        ezDBC.selectAndProcess("SELECT pg_size_pretty(pg_database_size(datname)) as msiSize FROM pg_database WHERE datname='msi_db_project_" + projectId + "'") { record =>
          size = record.getString("msiSize")
        }
      }
      size
    }.getOrElse("no.size")
  }
}

class DynamicDataStoreConnectorFactory(
    private val udsDbConnector: IDatabaseConnector = null) extends IDataStoreConnectorFactory with LazyLogging {

  require(udsDbConnector != null, "udsDbConnector is null")

  private lazy val udsEM = udsDbConnector.createEntityManager()

  override def isInitialized() = synchronized { if (udsDbConnector != null) true else false }

  override def getUdsDbConnector() = {
    udsDbConnector
  }

  /**
   * Return the same MSI Db for all projectId.
   */
  override def getMsiDbConnector(projectId: Long) = {
    createProjectDatabaseConnector(ProlineDatabaseType.MSI, projectId)
  }

  /**
   * Return the same LCMS Db for all projectId.
   */
  override def getLcMsDbConnector(projectId: Long) = {
    createProjectDatabaseConnector(ProlineDatabaseType.LCMS, projectId)
  }

  protected def createProjectDatabaseConnector(prolineDbType: ProlineDatabaseType, projectId: Long): IDatabaseConnector = {

    val project = udsEM.find(classOf[Project], projectId)

    require(project != null, s"Project #$projectId NOT found in UDS Db")

    val externalDb = ExternalDbRepository.findExternalByTypeAndProject(udsEM, prolineDbType, project)

    if (externalDb == null) {
      logger.warn(s"No ExternalDb for $prolineDbType Db of project #$projectId")
      null
    } else {
      _extDbToConnector(externalDb, prolineDbType)
    }
  }

  private def _dbTypeToConnector(prolineDbType: ProlineDatabaseType): IDatabaseConnector = {
    _extDbToConnector(ExternalDbRepository.findExternalByType(udsEM, prolineDbType), prolineDbType)
  }

  private def _extDbToConnector(externalDb: ExternalDb, prolineDbType: ProlineDatabaseType): IDatabaseConnector = {
    val (userName, password) = UdsRepository.getConProperties()
    externalDb.setDriverType(udsDbConnector.getDriverType())
    val dbConnProps = externalDb.toPropertiesMap(userName, password).asInstanceOf[java.util.Map[Object, Object]]
    DatabaseConnectorFactory.createDatabaseConnectorInstance(prolineDbType, dbConnProps)
  }

  override def closeAll() {
    this.synchronized {
      try {
        udsEM.close()
      } catch {
        case e: Exception => logger.error("Error closing UDS Db EntityManager", e.printStackTrace())
      }
      udsDbConnector.close()
    } // End of synchronized block on m_closeLock

  }

  override def closeLcMsDbConnector(projectId: Long) {
    throw new Exception("closeLcMsDbConnector isn't implemented. It is not expected to use it in Proline-Admin GUI")
  }

  override def closeMsiDbConnector(projectId: Long) {
    throw new Exception("closeMsiDbConnector isn't implemented. It is not expected to use it in Proline-Admin GUI")
  }

  override def closeProjectConnectors(projectId: Long) {
    throw new Exception("closeProjectConnectors is not effective here: please close the connectors directly")
  }

}
