package fr.proline.admin.service.user

import javax.persistence.EntityTransaction
import javax.persistence.FlushModeType
import com.typesafe.scalalogging.LazyLogging

import fr.proline.admin.service.ICommandWork
import fr.proline.core.orm.util.DataStoreConnectorFactory
import fr.proline.repository.DriverType
import fr.proline.core.orm.uds.Project
import fr.proline.context._
import fr.proline.repository._
import java.io._
import java.util.Date
import java.text.SimpleDateFormat
import scala.sys.process.Process
import scala.sys.process.ProcessLogger
import scala.collection.JavaConversions._

import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 *  pg_dump  :
 *  msi_db ,lcms_db ,uds_db only schema databases
 *  some selected rows from uds_db database for the current project
 *
 */
class UnarchiveProject(dsConnectorFactory: IDataStoreConnectorFactory, projectId: Long) extends ICommandWork with LazyLogging {
  var process: Process = null

  def doWork() {
    val udsDbConnector = dsConnectorFactory.getUdsDbConnector
    val udsDbCtx = new UdsDbConnectionContext(udsDbConnector)
    val udsEM = udsDbCtx.getEntityManager
    var localUdsTransaction: EntityTransaction = null
    udsEM.setFlushMode(FlushModeType.COMMIT)
    var udsTransacOK: Boolean = false
    try {
      if (!udsDbCtx.isInTransaction) {
        localUdsTransaction = udsEM.getTransaction()
        localUdsTransaction.begin()
        udsTransacOK = false
      }
      if (projectId > 0) {
        val project = udsEM.find(classOf[Project], projectId)
        if (project != null) {

          // get the properties of the project to update

          val properties = project.getSerializedProperties()
          var parser = new JsonParser()
          var array: JsonObject = null
          try {
            array = parser.parse(properties).getAsJsonObject()
          } catch {
            case e: Exception =>
              logger.error("error accessing project properties")
              array = parser.parse("{}").getAsJsonObject()
          }
          val sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
          array.addProperty("active", true)
          project.setSerializedProperties(array.toString())
          udsEM.merge(project)
          if (localUdsTransaction != null) {
            localUdsTransaction.commit()
            udsTransacOK = true
          }
          logger.info("Project with id= " + projectId + " has been activated .")

        } else {
          logger.error("project #" + projectId + " does not exist in uds_db database")
        }
      } else {
        logger.error("the parameter for the project with id #" + projectId + "is incorrect ")
      }

    } finally {
      udsEM.setFlushMode(FlushModeType.AUTO)
      udsDbCtx.close()
      udsDbConnector.close()
    }
  }

}

object UnarchiveProject {
  def apply(dsConnectorFactory: IDataStoreConnectorFactory, projectId: Long): Unit = {
    new UnarchiveProject(dsConnectorFactory, projectId).doWork()
  }

}
