package fr.proline.admin.service.user

import com.typesafe.scalalogging.LazyLogging
import fr.proline.admin.service.db.SetupProline
import fr.proline.context.DatabaseConnectionContext
import fr.proline.core.dal.context._
import fr.proline.core.orm.uds.{ UserAccount => UdsUser }
import fr.proline.core.orm.util.DataStoreConnectorFactory
import javax.persistence.EntityTransaction

/**
 * Reset password ,if password is indefined , 'proline' used as default password .
 *
 */
class ResetPassword(
  udsDbContext: DatabaseConnectionContext,
  userId: Long,
  password: String) extends LazyLogging {

  def run() {

    import fr.proline.core.orm.uds.{ UserAccount => UdsUser }
    import fr.profi.util.security._

    val isTxOk = udsDbContext.tryInTransaction {

      // Creation UDS entity manager
      val udsEM = udsDbContext.getEntityManager
      val udsUser = udsEM.find(classOf[UdsUser], userId)
      if (udsUser != null) {
        // reset user password 
        udsUser.setPasswordHash(sha256Hex(password))
        udsEM.merge(udsUser)
      } else {
        logger.info(s" user with id= ${userId} does not exist ")
      }
    }
    if (isTxOk) {
      logger.info("Your password has been succefully reset")
    } else {
      logger.error(" can't reset password !")
    }

  }

}

object ResetPassword extends LazyLogging {

  def apply(userId: Long, pswd: Option[String] = None) {

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
        // reset password
        val password = if (pswd.isDefined) pswd.get else "proline"
        val resetPassword = new ResetPassword(udsDbContext, userId, password)
        resetPassword.run()

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
