package fr.proline.admin.service.user

import com.typesafe.scalalogging.LazyLogging
import fr.proline.admin.service.db.SetupProline
import fr.proline.context.DatabaseConnectionContext
import fr.proline.core.dal.context._
import fr.proline.core.orm.uds.{ UserAccount => UdsUser }
import fr.proline.core.orm.util.DataStoreConnectorFactory
import javax.persistence.EntityTransaction

/**
 * Modify UserGroup, userGroup can be User or Admin
 *
 */
class ModifyUserGroup(
  udsDbContext: DatabaseConnectionContext,
  userId: Long,
  isUser: Boolean) extends LazyLogging {

  def run() {

    import fr.proline.core.orm.uds.{ UserAccount => UdsUser }
    import fr.profi.util.security._

    val isTxOk = udsDbContext.tryInTransaction {

      // Creation UDS entity manager
      val udsEM = udsDbContext.getEntityManager
      val udsUser = udsEM.find(classOf[UdsUser], userId)
      if (udsUser != null) {
        // modify user group

         var serializedPropertiesMap = udsUser.getSerializedPropertiesAsMap();
         if (serializedPropertiesMap == null) {
           serializedPropertiesMap = new java.util.HashMap[String, Object]
         }
         if (isUser) {
        	 serializedPropertiesMap.put("UserGroup","USER")
         } else {
        	 serializedPropertiesMap.put("UserGroup","ADMIN")
         }
         udsUser.setSerializedPropertiesAsMap(serializedPropertiesMap);
        
        udsEM.merge(udsUser)
      } else {
        logger.info(s" user with id= ${userId} does not exist ")
      }
    }
    if (isTxOk) {
      logger.info("Your user group has been succefully reset")
    } else {
      logger.error(" can't modify user group !")
    }

  }

}

object ModifyUserGroup extends LazyLogging {

  def apply(userId: Long, isUser: Boolean) {

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
        // modify User Group
        
        val modifyUserGroup = new ModifyUserGroup(udsDbContext, userId, isUser)
        modifyUserGroup.run()

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