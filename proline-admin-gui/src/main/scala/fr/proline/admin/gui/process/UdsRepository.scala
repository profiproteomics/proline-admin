package fr.proline.admin.gui.process

import com.typesafe.scalalogging.slf4j.Logging

import scala.collection.JavaConverters.asScalaBufferConverter

import fr.proline.admin.service.db.SetupProline
import fr.proline.admin.service.db.setup.DatabaseSetupConfig
import fr.proline.context.DatabaseConnectionContext
import fr.proline.core.orm.uds.ExternalDb
import fr.proline.core.orm.uds.Project
import fr.proline.core.orm.uds.UserAccount
import fr.proline.core.orm.uds.repository.ProjectRepository
import fr.proline.core.orm.util.DataStoreConnectorFactory
import fr.proline.repository.IDatabaseConnector

import javax.persistence.EntityManager

/**
 * Some utilities relative to UDS database connection
 */
object UdsRepository extends Logging {

  private var udsDbConfig: DatabaseSetupConfig = null
  private var udsDbConnector: IDatabaseConnector = null
  private var udsDbContext: DatabaseConnectionContext = null

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
    this.udsDbConfig = udsDbConfig

    // Close udsDbContext
    if (udsDbContext != null) {
      if (udsDbContext.isClosed == false) udsDbContext.close()
      udsDbContext = null
    }

    // Close udsDbConnector //TODO: this.closeUdsDbConnector()
    if (udsDbConnector != null) {
      if (udsDbConnector.isClosed == false) udsDbConnector.close()
      udsDbConnector = null
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
  private def _getUdsDbConnector(): IDatabaseConnector = {

    if (udsDbConnector != null) udsDbConnector
    else {
      udsDbConnector = this.getUdsDbConfig.toNewConnector()
      udsDbConnector
    }
  }
  
  /**
   * Get UDS database CONTEXT
   */
  def getUdsDbContext(): DatabaseConnectionContext = {
    if (udsDbContext != null) {
      udsDbContext
    } else {
      udsDbContext = new DatabaseConnectionContext(_getUdsDbConnector)
      udsDbContext
    }
  }
  
  /**
   * Get UDS entity manager
   */
  def getUdsDbEm(): EntityManager = {
    this.getUdsDbContext().getEntityManager()
  }
  
  /**
   * Get datastore connector factory using UDS databse 
   */
  def getDataStoreConnFactory(): DataStoreConnectorFactory = {
    val connectorFactory = DataStoreConnectorFactory.getInstance()
    if (!connectorFactory.isInitialized) {
      connectorFactory.initialize(this._getUdsDbConnector())
    }

    connectorFactory
  }

  /**
   *  Test connection with database
   */
  def isUdsDbReachable(): Boolean = {

    val udsDbConnector = _getUdsDbConnector()

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
        logger.warn("Proline is not set up : ", t)

        System.err.println("WARN - Proline is not set up !")
        System.err.println(t.getMessage())

        false
      }
    } /*finally {
      if( udsDbCtx != null ) udsDbCtx.close()
      //if (udsDbConnector != null && udsDbConnector.isClosed() == false) udsDbConnector.close()
    } */

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