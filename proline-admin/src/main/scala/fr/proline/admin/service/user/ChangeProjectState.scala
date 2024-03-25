package fr.proline.admin.service.user

import com.typesafe.scalalogging.LazyLogging
import fr.proline.core.orm.util.DataStoreConnectorFactory
import fr.proline.context.DatabaseConnectionContext
import fr.proline.admin.service.db.SetupProline
import fr.proline.core.dal.context._
import fr.proline.core.orm.uds.Project
import scala.collection.Set
import scala.util.Try
import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * Change Proline project state to activate/disabled
 * @author aromdhani
 *
 * @param udsDbContext The connection context to UDSDb to activate or to disable the project(s) into.
 * @param projectIdSet The set of project(s) id to activate or to disable.
 * @param isActive specify if the project(s) is active or disabled.
 */
class ChangeProjectState(udsDbContext: DatabaseConnectionContext,
    projectIdSet: Set[Long],
    isActive: Boolean = true) extends LazyLogging {
  var isSuccess: Boolean = false
  def run() {
    val isTxOk = udsDbContext.tryInTransaction {
      val batchSize = 20
      // Creation UDS entity manager
      val udsEM = udsDbContext.getEntityManager
      projectIdSet.zipWithIndex.foreach {
        case (projectId, index) =>
          val project = udsEM.find(classOf[Project], projectId)
          require(project != null, s"Undefined project with id= #$projectId")
          val properties = project.getSerializedProperties()
          val array: JsonObject = Try(JsonParser.parseString(properties).getAsJsonObject()).getOrElse(JsonParser.parseString("{}").getAsJsonObject())
          array.addProperty("is_active", isActive)
          project.setSerializedProperties(array.toString())
          udsEM.merge(project)
          if (index % batchSize == 0 && index > 0) {
            udsEM.flush()
            udsEM.clear()
          }
      }
    }
    isSuccess = isTxOk
    if (isTxOk) {
      logger.info(s"The state of project(s) with id(s)= #${projectIdSet.mkString(",")} has been changed successfully.")
    } else {
      logger.error(" can't change project(s) state!")
    }
  }
}

object ChangeProjectState {

  /**
   * Change Proline project state to activated or disabled.
   * @param projectIdSet The set of project(s) id to activate or to disable.
   * @param isActive specify if the project(s) is active or disabled.
   * @return <code>true</code> if the project(s) state has changed successfully otherwise <code>false</code>.
   */
  def apply(projectIdSet: Set[Long], isActive: Boolean = true): Boolean = {
    val prolineConf = SetupProline.config
    var localUdsDbConnector: Boolean = false
    var isSuccess: Boolean = false
    val connectorFactory = DataStoreConnectorFactory.getInstance()
    val udsDbConnector = if (connectorFactory.isInitialized) {
      connectorFactory.getUdsDbConnector
    } else {
      // Instantiate a database manager
      val udsDBConfig = prolineConf.udsDBConfig
      val newUdsDbConnector = udsDBConfig.toNewConnector()
      localUdsDbConnector = true
      newUdsDbConnector
    }
    try {
      val udsDbContext = new DatabaseConnectionContext(udsDbConnector)
      try {
        val changeProjState = new ChangeProjectState(udsDbContext,
          projectIdSet,
          isActive)
        changeProjState.run()
        isSuccess = changeProjState.isSuccess
      } finally {
        try {
          udsDbContext.close()
        } catch {
          case exClose: Exception => print("Error while trying to close UDS Db Context", exClose)
        }
      }
    } finally {
      if (localUdsDbConnector && (udsDbConnector != null)) {
        udsDbConnector.close()
      }
    }
    isSuccess
  }
}
