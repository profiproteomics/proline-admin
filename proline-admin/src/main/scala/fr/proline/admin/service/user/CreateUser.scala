package fr.proline.admin.service.user

import com.typesafe.scalalogging.LazyLogging
import fr.proline.admin.service.db.SetupProline
import fr.proline.context.DatabaseConnectionContext
import fr.proline.core.dal.context._
import fr.proline.core.orm.uds.{ Project => UdsProject, UserAccount => UdsUser }
import fr.proline.core.orm.util.DataStoreConnectorFactory
import javax.persistence.EntityTransaction
import fr.proline.repository.DriverType
import fr.profi.util.security._

/**
 * @author David Bouyssie
 *
 * @param udsDbContext : connection context to UDSDb to create user into
 * @param isGroupUser : true if user belongs to user group false if it is an admin
 * @param login : login of new user
 * @param password Specified password should be encrypted using sha256Hex
 */
class CreateUser(
  udsDbContext: DatabaseConnectionContext,
  login: String,
  password: String,
  isGroupUser: Boolean
) extends LazyLogging {

  var userId: Long = -1L

  def run() {

    val udsUser = new UdsUser()
      
    val isTxOk = udsDbContext.tryInTransaction {
      
      // Creation UDS entity manager
      val udsEM = udsDbContext.getEntityManager
      
      logger.info(s"creating user with login '${login}'...")

      // Create the project
      udsUser.setLogin(login)
      udsUser.setPasswordHash(password)
      udsUser.setCreationMode("MANUAL")


      val serializedPropertiesMap = new java.util.HashMap[String, Object]
      if (isGroupUser) {
        serializedPropertiesMap.put("user_group",UdsUser.UserGroupType.USER.name())
      } else {
        serializedPropertiesMap.put("user_group",UdsUser.UserGroupType.ADMIN.name())
      }
      udsUser.setSerializedPropertiesAsMap(serializedPropertiesMap);
      
      
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

object CreateUser extends LazyLogging {

  /**
    *
    * @param login login of new user
    * @param pswd  Option , specify new user password. If none, default will be used
    * @param user : specify if user belongs to user group (otherwise it belong to admin)
    * @param passwdEncrypted : specify if specified password is already encrypted or not.
    * @return id of created user
    */
  def apply(login: String, pswd: Option[String] = None, user: Option[Boolean] = None, passwdEncrypted: Option[Boolean] = None ): Long = {

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
        val password = if (pswd.isDefined){
          if(passwdEncrypted.isDefined && (passwdEncrypted.get == true) )
            pswd.get
          else
            sha256Hex(pswd.get)
        } else sha256Hex("proline")

        val isGroupUser = if (user.isDefined) user.get else true
        val userCreator = new CreateUser(udsDbContext, login, password, isGroupUser)
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
