package fr.proline.admin.service.user

import com.typesafe.scalalogging.LazyLogging
import fr.proline.core.orm.util.DataStoreConnectorFactory
import fr.proline.context.DatabaseConnectionContext
import fr.proline.admin.service.db.SetupProline
import fr.proline.core.dal.context._
import fr.proline.core.orm.uds.{ UserAccount => UdsUser }
import scala.collection.Set

/**
 * Change UserGroup, A Proline user can be in user group or in admin group.
 * @param udsDbContext The connection context to UDSDb to change  the user(s) group into.
 * @param userIdSet The Set of user(s) id to change their group.
 * @param isUser specify if the user(s) is in user group or in admin group.
 * <code>true</code> if in user group otherwise in admin group.
 *
 */
class ChangeUserGroup(
    udsDbContext: DatabaseConnectionContext,
    userIdSet: Set[Long],
    isUser: Boolean = true) extends LazyLogging {
  var isSuccess: Boolean = false
  def run() {
    import fr.proline.core.orm.uds.{ UserAccount => UdsUser }
    import fr.profi.util.security._
    val isTxOk = udsDbContext.tryInTransaction {
      val batchSize = 20
      // Creation UDS entity manager
      val udsEM = udsDbContext.getEntityManager
      userIdSet.zipWithIndex.foreach {
        case (userId, index) =>
          val udsUser = udsEM.find(classOf[UdsUser], userId)
          require(udsUser != null, s"The user with id= #${userId} does not exist.")
          // modify user group
          var serializedPropertiesMap = udsUser.getSerializedPropertiesAsMap()
          if (serializedPropertiesMap == null) {
            serializedPropertiesMap = new java.util.HashMap[String, Object]
          }
          if (isUser) {
            serializedPropertiesMap.put("user_group", UdsUser.UserGroupType.USER.name())
          } else {
            serializedPropertiesMap.put("user_group", UdsUser.UserGroupType.ADMIN.name())
          }
          udsUser.setSerializedPropertiesAsMap(serializedPropertiesMap);
          udsEM.merge(udsUser)
          if (index % batchSize == 0 && index > 0) {
            udsEM.flush()
            udsEM.clear()
          }
      }
    }
    isSuccess = isTxOk
    if (isTxOk) {
      logger.info(s"The group of user(s) with id(s)= #${userIdSet.mkString(",")} has been changed successfully.")
    } else {
      logger.error(" can't change user(s) group!")
    }
  }
}

object ChangeUserGroup extends LazyLogging {

  /**
   * Change UserGroup, A Proline user can be in user group or in admin group.
   * @param userIdSet The set of user(s) id to change their group.
   * @param isUser specify if the user(s) is in user group or in admin group.
   * <code>true</code> if in user group otherwise in admin group.
   * @return <code>true</code> if the state of user has been changed successfully.
   */
  def apply(userIdSet: Set[Long], isUser: Boolean = true): Boolean = {
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
        // modify User Group
        val changeUserGroup = new ChangeUserGroup(udsDbContext,
          userIdSet,
          isUser)
        changeUserGroup.run()
        isSuccess = changeUserGroup.isSuccess
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