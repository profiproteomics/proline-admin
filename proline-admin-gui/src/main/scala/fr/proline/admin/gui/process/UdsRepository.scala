package fr.proline.admin.gui.process

import com.typesafe.scalalogging.LazyLogging

import scala.collection.JavaConverters._

import fr.proline.admin.service.db.SetupProline
import fr.proline.admin.service.db.setup.DatabaseSetupConfig
import fr.proline.context.DatabaseConnectionContext
import fr.proline.core.orm.uds.ExternalDb
import fr.proline.core.orm.uds.Project
import fr.proline.core.orm.uds.UserAccount
import fr.proline.core.orm.uds.repository.ExternalDbRepository
import fr.proline.core.orm.uds.repository.ProjectRepository
import fr.proline.repository.DatabaseConnectorFactory
import fr.proline.repository.IDataStoreConnectorFactory
import fr.proline.repository.IDatabaseConnector
import fr.proline.repository.ProlineDatabaseType
import fr.proline.repository.ProlineDatabaseType.MSI
import fr.proline.repository.ProlineDatabaseType.LCMS
import fr.proline.core.dal.DoJDBCWork

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
    SetupProline.getUpdatedConfig.udsDBConfig
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

  //  /** Close UDS db connector */
  //  def closeUdsDbConnector() {
  //    if (udsDbConnector != null) {
  //      if (udsDbConnector.isClosed == false) udsDbConnector.close()
  //      udsDbConnector = null
  //    }
  //  }

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
   * Get UDS entity manager
   */
  /*def getUdsDbEm(): EntityManager = {
    this.getUdsDbContext().getEntityManager()
  }*/

  /**
   * Get datastore connector factory using UDS databse
   */
  def getDataStoreConnFactory(): IDataStoreConnectorFactory = {
    if (_dsConnectorFactory == null) {
      // FIXME: fix the EntityManager is already closed bug (from DataStoreConnectorFactory) to reuse existing "udsDbConnector"
      _dsConnectorFactory = new DynamicDataStoreConnectorFactory(this.getUdsDbConfig.toNewConnector())
    }
    _dsConnectorFactory

    /*val connectorFactory = DataStoreConnectorFactory.getInstance()
    if (!connectorFactory.isInitialized) {
      connectorFactory.initialize(this.getUdsDbConnector())
    }

    connectorFactory*/
  }

  /**
   *  Test connection with database
   */
  def isUdsDbReachable(verbose: Boolean = true): Boolean = {

    val udsDbConnector = getUdsDbConnector()

    var udsDbCtx: DatabaseConnectionContext = null //especially created for SQLite test, needs to be closed in this method

    try {

      /** Check to retrieve DB connection */
      logger.debug("Checking connection to UDSdb")

      udsDbConnector.getDataSource().getConnection()

      /** Additionnal check for file-based databases (SQLite) */
      udsDbCtx = new DatabaseConnectionContext(udsDbConnector)
      udsDbCtx.getEntityManager().find(classOf[ExternalDb], 1L)
      logger.debug("Proline is already set up !")
      true

    } catch {
      case t: Throwable => {

        System.err.println("WARN - Proline is not set up !")
        //if (verbose) System.err.println(t.getMessage())
        logger.trace("Proline is not set up : ", t)

        false
      }
    } finally {
      if (udsDbCtx != null) udsDbCtx.close()
      //if (udsDbConnector != null && udsDbConnector.isClosed() == false) udsDbConnector.close()
    }
  }
  /**
   *  Get the exhaustive list of UserAccount instances in database as a map of type "login ->  id"
   */
  def getAllUserAccounts(): Array[UserAccount] = { //login, id

    val udsDbContext = this.getUdsDbContext()

    try {

      // from fr.profi.pwx.dal.ProlineDataStore (pwx-scala daemons)
      val udsEM = udsDbContext.getEntityManager()

      val UdsUserAccountClass = classOf[UserAccount]
      val jpqlSelectUserAccount = s"FROM ${UdsUserAccountClass.getName}"

      val udsUsers = udsEM.createQuery(jpqlSelectUserAccount, UdsUserAccountClass).getResultList()

      val res = udsUsers.asScala.toArray
      //      println(s"INFO - Loaded ${res.length} user(s) from UDSdb.")
      res

    } catch {
      // Log Exception message, print error message in console, re-throw Exception
      case e: Throwable => {
        synchronized {

          //          def printProblematicLine(t: Throwable) {
          //
          //            val filterFrPro = t.getStackTrace().filter(ste => (ste.getClassName().startsWith("fr.profi.")) || (ste.getClassName().startsWith("fr.proline.")))
          //            val pbLine = filterFrPro.head.toString()
          //            val paGuiLine = filterFrPro.filter(ste => ste.getClassName().startsWith("fr.proline.admin.gui")).head.toString()
          //            println(pbLine)
          //            println("at " + paGuiLine)
          //          }

          logger.warn("Can't load user accounts from UDSdb:")
          logger.warn(e.getLocalizedMessage())
          println("ERROR - Can't load user accounts from UDSdb")

          throw e
        }
      }
    }
  }

  /**
   *  Get the exhaustive list of Project instances in database
   */
  def getAllProjects(): Array[Project] = {

    val udsDbContext = this.getUdsDbContext()
    val udsEM = udsDbContext.getEntityManager()

    val projects = try {

      // from fr.profi.pwx.dal.ProlineDataStore (pwx-scala daemons)
      val UdsProjectClass = classOf[Project]
      val jpqlSelectProject = s"FROM ${UdsProjectClass.getName}"

      val udsProjects = udsEM.createQuery(jpqlSelectProject, UdsProjectClass).getResultList()

      udsProjects.asScala.toArray

    } catch {
      // Log Exception message, print error message in console, re-throw Exception
      case t: Throwable => {
        synchronized {
          logger.warn("Can't load projects from UDSdb :", t)

          System.err.println("ERROR - Can't load projects from UDSdb")
          System.err.println(t.getMessage())
        }
        throw t
      }

    } /*finally {
      //udsEM.close()
      if (udsDbContext != null) udsDbContext.close()
    }*/

    projects
  }
  /* get all databases */

  def getAllDataBases(): Array[ExternalDb] = {
    val udsDbContext = this.getUdsDbContext()
    val udsEM = udsDbContext.getEntityManager()
    val externalDbs = try {
      val externalDbClass = classOf[ExternalDb]
      val jpqlSelectExternalDb = s"FROM ${externalDbClass.getName}"
      val externalDbList=udsEM.createQuery(jpqlSelectExternalDb,externalDbClass).getResultList()
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
   * Get External_db MSI
   */
  def getProjectMsiVerison(project: Project): ExternalDb = {
    return ExternalDbRepository.findExternalByTypeAndProject(this.getUdsDbContext.getEntityManager(), fr.proline.repository.ProlineDatabaseType.MSI, project)
  }

  /**
   * Get External_db LCMS
   */

  def getProjectLcmsVersion(project: Project): ExternalDb = {
    return ExternalDbRepository.findExternalByTypeAndProject(this.getUdsDbContext.getEntityManager(), fr.proline.repository.ProlineDatabaseType.LCMS, project)
  }

  /**
   * Calculate size of a database
   */
  def calculateSize(projectId: Long): String = {
    val udsDbContext = this.getUdsDbContext()
    val size = new StringBuilder()
    try {
      DoJDBCWork.withEzDBC(udsDbContext) { ezDBC =>
        ezDBC.selectAndProcess("SELECT pg_size_pretty(pg_database_size(datname)) as msiSize FROM pg_database WHERE datname='msi_db_project_" + projectId + "'") { record =>
          size.append(record.getString("msiSize"))
        }
        ezDBC.selectAndProcess("SELECT pg_size_pretty(pg_database_size(datname)) as lcmsSize FROM pg_database WHERE datname='lcms_db_project_" + projectId + "'") { record =>
          size.append(" - ").append(record.getString("lcmsSize"))
        }
      }
    } catch {
      case t: Throwable => logger.error("Error while calculating size of MSI and LCMS databases", t)
    }
    size.toString()
  }

}

class DynamicDataStoreConnectorFactory(
  private val udsDbConnector: IDatabaseConnector = null) extends IDataStoreConnectorFactory with LazyLogging {

  require(udsDbConnector != null, "udsDbConnector is null")

  //private lazy val udsEMF = udsDbConnector.getEntityManagerFactory()
  //private lazy val udsEM = udsEMF.createEntityManager()
  private lazy val udsEM = udsDbConnector.createEntityManager()
  private var pdiInitialized = false
  private lazy val pdiDbConnector = _dbTypeToConnector(ProlineDatabaseType.PDI)
  private var psInitialized = false
  private lazy val psDbConnector = _dbTypeToConnector(ProlineDatabaseType.PS)

  override def isInitialized() = {
    true
  }

  override def getUdsDbConnector() = {
    udsDbConnector
  }

  override def getPdiDbConnector() = {
    pdiInitialized = true
    pdiDbConnector
  }

 override def getPsDbConnector() = {
    psInitialized = true
    psDbConnector
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
    val propertiesMap = externalDb.toPropertiesMap(udsDbConnector.getDriverType())
    //propertiesMap.put("ApplicationName", m_applicationName);
    DatabaseConnectorFactory.createDatabaseConnectorInstance(prolineDbType, propertiesMap)
  }

  override def closeAll() {
    this.synchronized {

      if (pdiInitialized) {
        pdiDbConnector.close()
      }

      if (psInitialized) {
        psDbConnector.close()
     }

      try {
        udsEM.close()
      } catch {
        case e: Exception => logger.error("Error closing UDS Db EntityManager", e)
      }

      udsDbConnector.close()

    } // End of synchronized block on m_closeLock

  }

  override def closeLcMsDbConnector(projectId: Long) {
    throw new Exception("closeLcMsDbConnector isn't implemented. I didn't expect to use it in PAdmin GUI")
  }

  override def closeMsiDbConnector(projectId: Long) {
    throw new Exception("closeMsiDbConnector isn't implemented. I didn't expect to use it in PAdmin GUI")
  }

  override def closeProjectConnectors(projectId: Long) {
    throw new Exception("closeProjectConnectors is not effective here: please close the connectors directly")
  }

}
