package fr.proline.admin.service.user

import com.typesafe.scalalogging.LazyLogging
import fr.proline.core.orm.util.DataStoreConnectorFactory
import fr.proline.context.DatabaseConnectionContext
import fr.proline.admin.service.db.SetupProline
import fr.proline.core.dal.context._
import fr.proline.core.orm.uds.{ UserAccount => UdsUser }

/**
 * Change password ,if password is indefined , 'proline' used as default password .
 * @param udsDbContext The connection context to UDSDb to restore project into.
 * @param userId The user id.
 * @param password The new password.
 */
class ChangePassword(
    udsDbContext: DatabaseConnectionContext,
    userId: Long,
    password: Option[String] = None) extends LazyLogging {
  var isSuccess: Boolean = false
  def run() {
    import fr.proline.core.orm.uds.{ UserAccount => UdsUser }
    import fr.profi.util.security._
    val isTxOk = udsDbContext.tryInTransaction {
      // Creation UDS entity manager
      val udsEM = udsDbContext.getEntityManager
      val udsUser = udsEM.find(classOf[UdsUser], userId)
      require(udsUser != null, s"The user with id=#${userId} does not exist")
      // reset user password 
      val pswd = password.getOrElse("proline")
      udsUser.setPasswordHash(sha256Hex(pswd))
      udsEM.merge(udsUser)
    }
    isSuccess = isTxOk
    if (isTxOk) {
      logger.info(s"The password of user with id= #$userId has been changed successfully.")
    } else {
      logger.error(" can't chnage password !")
    }
  }
}

object ChangePassword extends LazyLogging {

  /**
   * Change password ,if password is indefined , 'proline' used as default password .
   * @param userId The user id.
   * @param password The new password.
   *
   */
  def apply(userId: Long, pswd: Option[String] = None): Boolean = {

    // Retrieve Proline configuration
    val prolineConf = SetupProline.config
    var localUdsDbConnector: Boolean = false
    var isSuccess: Boolean = false
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
        // change password
        val changePassword = new ChangePassword(udsDbContext,
          userId,
          pswd)
        changePassword.run()
        isSuccess = changePassword.isSuccess
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
    isSuccess
  }
}
