package fr.proline.admin.service.user

import com.typesafe.scalalogging.slf4j.Logging
import fr.proline.admin.service.db.SetupProline
import fr.proline.context.DatabaseConnectionContext
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

    // Creation UDS entity manager
    val udsEM = udsDbContext.getEntityManager

    var localUdsTransaction: EntityTransaction = null
    var udsTransacOK: Boolean = false

    try {

      if (!udsDbContext.isInTransaction) {
        localUdsTransaction = udsEM.getTransaction
        localUdsTransaction.begin()
        udsTransacOK = false
      }

      logger.info("creating user with login '" + login + "'...")

      // Create the project
      val udsUser = new UdsUser()
      udsUser.setLogin(login)
      udsUser.setPasswordHash(sha256Hex(password))
      udsUser.setCreationMode("MANUAL")

      udsEM.persist(udsUser)

      if (localUdsTransaction != null) {
        localUdsTransaction.commit()
        udsTransacOK = true
      }

      userId = udsUser.getId
      logger.debug("User #" + userId + " has been created")
    } finally {

      if ((localUdsTransaction != null) && !udsTransacOK && udsDbContext.getDriverType != DriverType.SQLITE) {
        logger.info("Rollbacking current UDS Db Transaction")

        try {
          localUdsTransaction.rollback()
        } catch {
          case ex: Exception => logger.error("Error rollbacking UDS Db Transaction", ex)
        }

      }

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
