package fr.proline.admin.service.user

import com.typesafe.scalalogging.LazyLogging
import fr.proline.core.orm.util.DataStoreConnectorFactory
import fr.proline.context.DatabaseConnectionContext
import fr.proline.admin.service.db.SetupProline
import fr.proline.core.dal.context._
import fr.proline.core.orm.uds.{ UserAccount => UdsUser }
import scala.collection.Set
import scala.util.Try
import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * Change a Proline user(s) state.
 * @author aromdhani
 *
 * @param udsDbContext The connection context to UDSDb to activate or to disable the user(s) into.
 * @param userIdSet The set of user(s) id(s) to activate or to disable.
 * @param isActive specify if the user is active or disabled.
 *
 */
class ChangeUserState(
    udsDbContext: DatabaseConnectionContext,
    userIdSet: Set[Long],
    isActive: Boolean = true) extends LazyLogging {
  var isSuccess: Boolean = false
  def run() {
    val isTxOk = udsDbContext.tryInTransaction {
      val batchSize = 20
      // Creation UDS entity manager
      val udsEM = udsDbContext.getEntityManager
      userIdSet.zipWithIndex.foreach {
        case (userId, index) =>
          val udsUser = udsEM.find(classOf[UdsUser], userId)
          require(udsUser != null, s"The user with id= ${userId} does not exist.")
          val properties = udsUser.getSerializedProperties()
          val array: JsonObject = Try(JsonParser.parseString(properties).getAsJsonObject()).getOrElse(JsonParser.parseString("{}").getAsJsonObject())
          array.addProperty("is_active", isActive)
          udsUser.setSerializedProperties(array.toString())
          udsEM.merge(udsUser)
          if (index % batchSize == 0 && index > 0) {
            udsEM.flush()
            udsEM.clear()
          }
      }
    }
    isSuccess = isTxOk
    if (isTxOk) {
      logger.info(s"The state of user(s) with id(s)= #${userIdSet.mkString(",")} has been changed successfully.")
    } else {
      logger.error(" can't change user(s) state!")
    }
  }
}

object ChangeUserState extends LazyLogging {

  /**
   * Change a Proline user(s) state .
   * @param userId The set of user(s) id to activate or to disable.
   * @param isActive specify if the user is active or disabled.
   * @return <code>true</code> if the user(s) state has been changed successfully.
   */

  def apply(userId: Set[Long], isActive: Boolean = true): Boolean = {
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
        // change user state 
        val chnageUserState = new ChangeUserState(udsDbContext,
          userId,
          isActive)
        chnageUserState.run()
        isSuccess = chnageUserState.isSuccess
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
