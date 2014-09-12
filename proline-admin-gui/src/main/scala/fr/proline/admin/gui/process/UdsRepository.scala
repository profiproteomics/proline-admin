package fr.proline.admin.gui.process

import java.io.File
import com.typesafe.scalalogging.slf4j.Logging
import scala.collection.JavaConverters.asScalaBufferConverter
import fr.proline.admin.service.db.SetupProline
import fr.proline.context.DatabaseConnectionContext
import fr.proline.core.orm.uds.Project
import fr.proline.core.orm.uds.UserAccount
import fr.proline.core.orm.util.DataStoreConnectorFactory
import fr.proline.repository.IDatabaseConnector
import fr.proline.core.orm.uds.repository.ProjectRepository

/**
 * Some utilities relative to UDS database connection
 */
object UdsRepository extends Logging {

  /**
   *  Test connexion with database
   */
  def isUdsDbReachable(): Boolean = {

    /** Look for uds.sqlite file in database directory */
    val (dir, file) = (SetupProline.getUpdatedConfig.udsDBConfig.dbDirectory, SetupProline.getUpdatedConfig.udsDBConfig.dbName)
    val udsFile = new File(dir + "/" + file)
    if (udsFile.exists()) true else false
  }

  /**
   *  Get the exhaustive list of UserAccount instances in database as a map of type "login ->  id"
   */
  def getAllUserAccounts(): Array[UserAccount] = { //login, id

    val udsDbConnector = _getUdsDbConnector()
    val udsDbContext = new DatabaseConnectionContext(udsDbConnector)
    
    /*case class UdsDbException( message: String, cause: Throwable ) extends Exception
    implicit def error2udsError( e: Exception ): UdsDbException = {
      new UdsDbException( e.getMessage(), e.getCause() )
    }*/

    try {

      // from fr.profi.pwx.dal.ProlineDataStore (pwx-scala daemons)
      val udsEM = udsDbContext.getEntityManager()
      val UdsUserAccountClass = classOf[UserAccount]
      val jpqlSelectUserAccount = s"FROM ${UdsUserAccountClass.getName}"

      val udsUsers = udsEM.createQuery(jpqlSelectUserAccount, UdsUserAccountClass).getResultList()
      //udsUsers.asScala.map { user => (user.getLogin(), user.getId()) }.toMap[String, Long] //FIXME: irrelevant!
      udsUsers.asScala.toArray

    } catch {
      // Log Exception message and re-throw Exception
      case e: Exception => { logger.error("can't load user accounts from UDSdb"); throw e }
    } finally {
      udsDbContext.close()
    }

  }

  /**
   *  Get the exhaustive list of Project instances in database as a map of type "name ->  owner"
   */
  def getAllProjectsGroupedByOwner(): Map[UserAccount, Array[Project]] = {

    val udsDbConnector = _getUdsDbConnector()
    val udsDbContext = new DatabaseConnectionContext(udsDbConnector)

    val projectMap = try {

      // from fr.profi.pwx.dal.ProlineDataStore (pwx-scala daemons)
      val udsEM = udsDbContext.getEntityManager()
      val UdsProjectClass = classOf[Project]
      val jpqlSelectProject = s"FROM ${UdsProjectClass.getName}"

      //TODO : heeeeeeeeeere make it much more smart !  [UserAccount, UdsProject]
      val udsProjects = udsEM.createQuery(jpqlSelectProject, UdsProjectClass).getResultList()
      //udsProjects.asScala.map { p => (p.getOwner(), (p.getName(), p.getDescription())) }.toMap[UserAccount, (String, String)]
      
      udsProjects.asScala.toArray.groupBy(_.getOwner())

    } finally {
      udsDbContext.close()
      udsDbConnector.close()
    }

    projectMap
  }

  /**
   *  Get projects name' list for a given user (owner)
   */
  def findProjectsByOwnerId(userId: Long): Array[Project] = {
    
    val udsDbContext = new DatabaseConnectionContext(_getUdsDbConnector)
    
    try {
      ProjectRepository.findOwnedProjects(udsDbContext.getEntityManager(), userId).asScala.toArray
    } finally {
      udsDbContext.close()
    }

    /*
     //TODO: refine database request rather than filter on project map
     lazy val map = getProjectMap()

    val userProjects = map.filter {
      case ( ua, (name,desc)) => ua.getLogin() == userName //WARNING: assume logins are unique
    }.map { _._2 }

    userProjects.toArray*/
  }

  /**
   * Get UDS database connector
   */
  private def _getUdsDbConnector(): IDatabaseConnector = {

    // from fr.proline.admin.service.user.CreateUser
    val connectorFactory = DataStoreConnectorFactory.getInstance()

    if (connectorFactory.isInitialized) {
      connectorFactory.getUdsDbConnector
    } else {
      
      val udsDBConfig = SetupProline.getUpdatedConfig.udsDBConfig
      
      val connector = udsDBConfig.toNewConnector()
      connectorFactory.initialize( connector )
      
      connector
    }
  }
  
}