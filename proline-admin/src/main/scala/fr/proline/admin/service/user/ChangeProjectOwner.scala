package fr.proline.admin.service.user

import com.typesafe.scalalogging.LazyLogging
import fr.proline.core.orm.util.DataStoreConnectorFactory
import fr.proline.context.DatabaseConnectionContext
import fr.proline.admin.service.db.SetupProline
import fr.proline.core.dal.context._
import fr.proline.core.orm.uds.{ UserAccount => UdsUser }
import fr.proline.core.orm.uds.Project

/**
 * Change a Proline project owner.
 * @author aromdhani
 *
 * @param udsDbContext The connection context to UDSDb to change the project owner into.
 * @param projectId The project id
 * @param userId The project owner id
 *
 */
class ChangeProjectOwner(
    udsDbContext: DatabaseConnectionContext,
    projectId: Long,
    userId: Long) extends LazyLogging {
  def run() {
   
    val isTxOk = udsDbContext.tryInTransaction {
      // Creation UDS entity manager
     val udsEM = udsDbContext.getEntityManager
      val udsProject = udsEM.find(classOf[Project], projectId) 
      val oldOwner = udsProject.getOwner()
      require(udsProject != null, s"The project with id= ${projectId} does not exist!")
      val udsUser = udsEM.find(classOf[UdsUser], userId)
      require(udsUser != null, s"The user with id= ${userId} does not exist!")
      udsProject.setOwner(udsUser)
      udsProject.removeMember(oldOwner)
      udsEM.merge(udsProject)
          }
    if (isTxOk) {
      logger.info(s"The project with id= #${projectId} has been assigned to the owner with id= #${userId} successfully.")
    } else {
      logger.error("Cannot change project owner!")
    }
  }
}

object ChangeProjectOwner extends LazyLogging {
 /**
 * Change a Proline project owner.
 * @param projectId the project id
 * @param userId the project owner id
 *
 */

  def apply(projectId: Long, userId:Long) = {
    // Retrieve Proline configuration
    val prolineConf = SetupProline.config
    var localUdsDbConnector: Boolean = false
    val connectorFactory = DataStoreConnectorFactory.getInstance()
    val udsDbConnector = if (connectorFactory.isInitialized) {
      connectorFactory.getUdsDbConnector
    } else {
      // Create a new connector
      val udsDBConfig = prolineConf.udsDBConfig
      val newUdsDbConnector = udsDBConfig.toNewConnector()
      localUdsDbConnector = true
      newUdsDbConnector
    }
    try {
      val udsDbContext = new DatabaseConnectionContext(udsDbConnector)
      try {
        // change user state 
        val chnageUserState = new ChangeProjectOwner(udsDbContext,
          projectId,
            userId
          )
        chnageUserState.run()
             } finally {
        logger.debug("Closing current UDS Db Context")
        try {
          udsDbContext.close()
        } catch {
          case exClose: Exception => logger.error("Error closing UDS Db Context", exClose)
        }
      }
    } finally {
      //close connector
      if (localUdsDbConnector && (udsDbConnector != null)) {
        udsDbConnector.close()
      }
    }
  }

}
