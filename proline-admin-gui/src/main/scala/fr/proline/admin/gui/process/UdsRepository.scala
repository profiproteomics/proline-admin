package fr.proline.admin.gui.process

import javax.persistence.EntityManager

import scala.collection.JavaConverters._

import com.typesafe.scalalogging.LazyLogging
import com.typesafe.scalalogging.StrictLogging

import fr.proline.admin.service.db.SetupProline
import fr.proline.admin.service.db.setup.DatabaseSetupConfig
import fr.proline.context.DatabaseConnectionContext
import fr.proline.core.orm.uds.ExternalDb
import fr.proline.core.orm.uds.Project
import fr.proline.core.orm.uds.UserAccount
import fr.proline.core.orm.uds.repository.ProjectRepository
import fr.proline.repository.IDataStoreConnectorFactory
import fr.proline.repository.IDatabaseConnector
import fr.proline.repository.UncachedDataStoreConnectorFactory



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
    //    if (udsDbConfig != null) udsDbConfig //not re-initialized if invalid config (private) 
    //    else SetupProline.getUpdatedConfig.udsDBConfig
    SetupProline.getUpdatedConfig.udsDBConfig
  }

  def setUdsDbConfig(udsDbConfig: DatabaseSetupConfig) {
    require(udsDbConfig != null, "udsDbConfig is null")

    // Set new udsDbConfig
    this._udsDbConfig = udsDbConfig
    
    // Close DataStore connector factory
    if( _dsConnectorFactory != null ) {
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
    
    if( _dsConnectorFactory == null ) {
      
      // FIXME: fix the EntityManager is already closed bug (from DataStoreConnectorFactory) to reuse existing "udsDbConnector"
      //_dsConnectorFactory = new DynamicDataStoreConnectorFactory(this.getUdsDbConfig.toNewConnector())
      
      val dsConnectorFactory = UncachedDataStoreConnectorFactory.getInstance()
      if (!dsConnectorFactory.isInitialized) {
        dsConnectorFactory.initialize(this.getUdsDbConfig.toNewConnector())
      }
      
      _dsConnectorFactory = dsConnectorFactory
    }
    
    _dsConnectorFactory
  }

  /**
   *  Test connection with database
   */
  def isUdsDbReachable(verbose: Boolean = true): Boolean = {

    val udsDbConnector = getUdsDbConnector()

    var udsDbCtx: DatabaseConnectionContext = null //especially created for SQLite test, needs to be closed in this method
    
    try {

      /** Check to retrieve DB connection */
      udsDbConnector.getDataSource().getConnection()

      /** Additionnal check for file-based databases (SQLite) */
      udsDbCtx = new DatabaseConnectionContext(udsDbConnector)
      udsDbCtx.getEntityManager().find(classOf[ExternalDb], 1L)

      logger.debug("Proline is already set up !")
      
      true

    } catch {
      case t: Throwable => {

        System.err.println("WARN - Proline is not set up !")
        if (verbose) System.err.println(t.getMessage())
        
        logger.trace("Proline is not set up : ", t)

        false
      }
    } finally {
      if( udsDbCtx != null ) udsDbCtx.close()
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
          //          println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%")
          //          e.printStackTrace()
          //          println("***********************************")
          //          printProblematicLine(e) //TODO: delete me

          throw e
        }
      }
    }
  }

  /**
   *  Get the exhaustive list of Project instances in database, grouped by owner
   */
  def getAllProjectsGroupedByOwner(): Map[UserAccount, Array[Project]] = {

    val udsDbContext = this.getUdsDbContext()
    val udsEM = udsDbContext.getEntityManager()

    val projectMap = try {

      // from fr.profi.pwx.dal.ProlineDataStore (pwx-scala daemons)
      val UdsProjectClass = classOf[Project]
      val jpqlSelectProject = s"FROM ${UdsProjectClass.getName}"

      val udsProjects = udsEM.createQuery(jpqlSelectProject, UdsProjectClass).getResultList()

      udsProjects.asScala.toArray.groupBy(_.getOwner())

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

    projectMap
  }

  /**
   *  Get projects list for a given user (owner)
   */
  def findProjectsByOwnerId(userId: Long): Array[Project] = {
    ProjectRepository.findOwnedProjects(this.getUdsDbContext.getEntityManager(), userId).asScala.toArray
  }

}

/*
class DynamicDataStoreConnectorFactory(
  private val udsDbConnector: IDatabaseConnector = null
) extends IDataStoreConnectorFactory with LazyLogging {
  require( udsDbConnector != null, "udsDbConnector is null" )
  
	private lazy val udsEMF = udsDbConnector.getEntityManagerFactory()
	private lazy val udsEM = udsEMF.createEntityManager()
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
  
  override def closeProjectConnectors(projectId: Long) {
    throw new Exception("closeProjectConnectors is not effective here: please close the connectors directly")
  }

}*/