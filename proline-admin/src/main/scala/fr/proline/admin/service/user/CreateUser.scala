package fr.proline.admin.service.user

import com.typesafe.scalalogging.slf4j.Logging
import fr.proline.admin.service.db.SetupProline
import fr.proline.context.DatabaseConnectionContext
import fr.proline.core.dal.context._
import fr.proline.core.orm.uds.{ Project => UdsProject, UserAccount => UdsUser }
import fr.proline.core.orm.util.DataStoreConnectorFactory
import javax.persistence.EntityTransaction
import fr.proline.repository.DriverType

/**
 * @author David Bouyssie
 *
 */
class CreateUser(
  udsDbContext: DatabaseConnectionContext,
  login: String,
  password: String
) extends Logging {

  var userId: Long = -1L

  def run() {

    import fr.proline.core.orm.uds.{ UserAccount => UdsUser }
    import fr.profi.util.security._
    
    val udsUser = new UdsUser()
      
    val isTxOk = udsDbContext.tryInTransaction {
      
      // Creation UDS entity manager
      val udsEM = udsDbContext.getEntityManager
      
      logger.info(s"creating user with login '${login}'...")

      // Create the project
      udsUser.setLogin(login)
      udsUser.setPasswordHash(sha256Hex(password))
      udsUser.setCreationMode("MANUAL")

      udsEM.persist(udsUser)
    }
    
    if( isTxOk ) {
      userId = udsUser.getId
      logger.debug(s"User #${userId} has been created")
    } else {
      logger.error(s"User '${login}' can't be created !")
    }

  }

}

object CreateUser extends Logging {

  def apply(login: String, pswd: Option[String] = None): Long = {

    // Retrieve Proline configuration
    val prolineConf = SetupProline.config

    var userId: Long = -1L

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
        // Create user
        val password = if (pswd.isDefined) pswd.get else "proline"
        val userCreator = new CreateUser(udsDbContext, login, password)
        userCreator.run()

        userId = userCreator.userId
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

    userId
  }

}
