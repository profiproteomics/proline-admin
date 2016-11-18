package fr.proline.admin.service.user

import javax.persistence.EntityTransaction
import javax.persistence.FlushModeType
import com.typesafe.scalalogging.LazyLogging

import fr.proline.admin.service.ICommandWork
import fr.proline.core.orm.util.DataStoreConnectorFactory
import fr.proline.repository.DriverType
import fr.proline.core.orm.uds.ExternalDb
import fr.proline.core.orm.uds.Project
import fr.proline.core.orm.uds.repository.ExternalDbRepository
import fr.proline.context._
import fr.proline.core.dal.DoJDBCReturningWork
import fr.proline.repository._
import fr.proline.core.dal.DoJDBCWork

import java.io._
import java.util.Date
import scala.sys.process.Process
import scala.sys.process.ProcessLogger

import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 *  pg_dump  :
 *  msi_db ,lcms_db ,uds_db only schema databases
 *  some selected rows from uds_db database for the current project
 *
 */
class ArchiveProject(dsConnectorFactory: IDataStoreConnectorFactory, projectId: Long, pathSource: String, pathDestination: String) extends ICommandWork with LazyLogging {
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
      if ((projectId > 0) && (pathSource != null) && (!pathSource.isEmpty) && (pathDestination != null) && (!pathDestination.isEmpty)) {
        val project = udsEM.find(classOf[Project], projectId)
        if (project != null) {
          val externalDbMsi = ExternalDbRepository.findExternalByTypeAndProject(udsEM, fr.proline.repository.ProlineDatabaseType.MSI, project)
          val externalDbLcms = ExternalDbRepository.findExternalByTypeAndProject(udsEM, fr.proline.repository.ProlineDatabaseType.LCMS, project)
          val createPgDumpSelectedRowsTables = new StringBuilder
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
          if ((externalDbMsi.getHost() != null) && (!externalDbMsi.getHost().isEmpty) && (externalDbMsi.getDbUser() != null) && (!externalDbMsi.getDbUser().isEmpty) &&
            (externalDbMsi.getDbName() != null) && (!externalDbMsi.getDbName().isEmpty) && (externalDbLcms.getDbName() != null) && (!externalDbLcms.getDbName().isEmpty) && (externalDbMsi.getPort() > 0)) {
            try {
              logger.info(s"Starting to backup the project #$projectId....")
              //pgdump  databases for a project 
              logger.info(s"Starting to pg_dump of MSI,LCMS and schema of UDS databases...")
              if (execPgDump(externalDbMsi.getHost(), externalDbMsi.getPort(), externalDbMsi.getDbUser(), externalDbMsi.getDbPassword(), externalDbMsi.getDbName(), externalDbLcms.getDbName(), pathDestination, pathSource, projectId)) {
                //Select some rows from uds_db only for the current project :
                //rows project 
                createPgDumpSelectedRowsTables.append("project").append("").append("\n")
                createPgDumpSelectedRowsTables.append(projectId).append("|").append(project.getName()).append("|").append(project.getDescription()).append("|").append(project.getCreationTimestamp()).append("|").
                  append(project.getSerializedProperties()).append("|").append(project.getOwner().getId()).append("|").append(project.getLockExpirationTimestamp()).append("|").append(project.getLockUserID()).append("").append("\n")
                //rows of externaldb(msi)
                createPgDumpSelectedRowsTables.append("External_db").append("").append("\n")
                createPgDumpSelectedRowsTables.append(externalDbMsi.getId()).append("|").append(externalDbMsi.getDbName()).append("|").append(externalDbMsi.getConnectionMode()).append("|").append(externalDbMsi.getDbUser())
                  .append("|").append(externalDbMsi.getDbPassword()).append("|").append(externalDbMsi.getHost()).append("|").append(externalDbMsi.getPort()).append("|").append(externalDbMsi.getType()).append("|")
                  .append(externalDbMsi.getDbVersion()).append("|").append(externalDbMsi.getIsBusy()).append("|").append(externalDbMsi.getSerializedProperties()).append("").append("\n")
                // rows of externaldb(lcms)
                createPgDumpSelectedRowsTables.append(externalDbLcms.getId()).append("|").append(externalDbLcms.getDbName()).append("|").append(externalDbLcms.getConnectionMode()).append("|").append(externalDbLcms.getDbUser())
                  .append("|").append(externalDbLcms.getDbPassword()).append("|").append(externalDbLcms.getHost()).append("|").append(externalDbLcms.getPort()).append("|").append(externalDbLcms.getType()).append("|")
                  .append(externalDbLcms.getDbVersion()).append("|").append(externalDbLcms.getIsBusy()).append("|").append(externalDbLcms.getSerializedProperties()).append("").append("\n")

                DoJDBCWork.withEzDBC(udsDbCtx) { ezDBC =>
                  // rows of project_db_map
                  createPgDumpSelectedRowsTables.append("project_db_map").append("").append("\n")
                  ezDBC.selectAndProcess(" SELECT project_id,external_db_id FROM project_db_map WHERE project_id=" + projectId) { record =>
                    if (record.getLong("project_id") > 0 && record.getLong("external_db_id") > 0) {
                      createPgDumpSelectedRowsTables.append(record.getLong("project_id")).append("|").append(record.getLong("external_db_id")).append("|")
                        .append("").append("\n")
                    }
                  }
                  //rows of dataset
                  createPgDumpSelectedRowsTables.append("data_set").append("").append("\n")
                  ezDBC.selectAndProcess(" SELECT id,number,name,description ,type ,keywords ,creation_timestamp ,modification_log,children_count ,serialized_properties, result_set_id ,result_summary_id ,aggregation_id ,fractionation_id ,quant_method_id ,parent_dataset_id ,project_id FROM data_set WHERE project_id=" + projectId) { record =>
                    if (record.getLong("id") > 0 && record.getInt("number") >= 0 && record.getString("name") != null && record.getString("type") != null && record.getInt("children_count") >= 0 && record.getLong("project_id") > 0) {
                      createPgDumpSelectedRowsTables.append(record.getLong("id")).append("|").append(record.getInt("number")).append("|").append(record.getString("name")).append("|").append(record.getString("description"))
                        .append("|").append(record.getString("type")).append("|").append(record.getString("keywords")).append("|").append(record.getString("creation_timestamp"))
                        .append("|").append(record.getString("modification_log")).append("|").append(record.getInt("children_count")).append("|").append(record.getStringOption("serialized_properties")).append("|").append(record.getLong("result_set_id"))
                        .append("|").append(record.getLong("result_summary_id")).append("|").append(record.getLong("aggregation_id")).append("|").append(record.getLong("fractionation_id")).append("|").append(record.getLong("quant_method_id")).append("|")
                        .append(record.getLong("parent_dataset_id")).append("|").append(record.getLong("project_id"))
                        .append("").append("\n")
                    }
                  }
                  //rows of run_identification
                  createPgDumpSelectedRowsTables.append("run_identification").append("").append("\n")
                  ezDBC.selectAndProcess(" SELECT id,serialized_properties,run_id,raw_file_identifier FROM run_identification WHERE id in (SELECT id FROM data_set WHERE project_id=" + projectId + ")") { record =>
                    if (record.getLong("id") > 0) {
                      createPgDumpSelectedRowsTables.append(record.getLong("id")).append("|").append(record.getString("serialized_properties")).append("|").append(record.getLong("run_id")).append("|").append(record.getString("raw_file_identifier"))
                        .append("").append("\n")
                    }
                  }
                }
                try {
                  val fileToWrite = new File(pathDestination, "\\\\pg_dump_project_" + projectId + "\\uds_db_project_" + projectId + ".csv")
                  val fw = new FileWriter(fileToWrite.getAbsoluteFile())
                  val bw = new BufferedWriter(fw)
                  bw.write(createPgDumpSelectedRowsTables.toString())
                  bw.flush()
                  bw.close()
                } catch {
                  case ex: IOException => logger.error("IOException", ex)
                }
              }
            } catch {
              case t: Throwable => logger.error("Error while archiving project", t)
            } finally {
              //empty stringbuilder 
              if ((createPgDumpSelectedRowsTables != null) && (createPgDumpSelectedRowsTables.length > 0))
                createPgDumpSelectedRowsTables.setLength(0)
            }
            //update serialized properties 
            if (!array.has("archived")) {
              array.addProperty("archived", new Date().toString)
              project.setSerializedProperties(array.toString())
              udsEM.merge(project)
            }
          } else {
            logger.error("Some parameters are missing in table uds_db.external_db")
          }

        } else {
          logger.error("project #" + projectId + " does not exist in uds_db database")
        }
      } else {
        logger.error("Some parameters are missing for the project #" + projectId + " check if project id ,source path or destination path are empty or null")
      }
      if (localUdsTransaction != null) {
        localUdsTransaction.commit()
        udsTransacOK = true
      }

    } finally {

      udsEM.setFlushMode(FlushModeType.AUTO)
      udsDbCtx.close()
      udsDbConnector.close()
    }
  }
  //execute command as sequence of string
  def execute(command: Seq[String]) {
    logger.debug("Executing " + command.mkString(" "))
    process = Process(command).run(ProcessLogger(out => stdout(out), err => stderr(err)))
    val exitCode = process.exitValue
    process.destroy
    process = null
    logger.debug("Exit code is '" + exitCode + "'")
    // check exit code
    if (exitCode != 0) {
      logger.error("Command has failed !")
    }
  }

  def execPgDump(host: String, port: Integer, user: String, passWord: String, msiDb: String, lcmsDb: String, pathDestination: String, pathSource: String, projectId: Long): Boolean = {

    val pathDestinationProject = new File(pathDestination, "\\pg_dump_project_" + projectId)
    val pathSrcDump = new File(pathSource, "\\pg_dump").getCanonicalPath()
    var pgDumpIsOk: Boolean = false
    try {
      if (!pathDestinationProject.exists()) {
        if (pathDestinationProject.mkdir()) {

          /* options of pg_dump 
							  -p, –port=PORT database server port number
								-i, –ignore-version proceed even when server version mismatches
								-h, –host=HOSTNAME database server host or socket directory
								-U, –username=NAME connect as specified database user
								-W, –password force password prompt (should happen automatically)
								-d, –dbname=NAME connect to database name
								-v, –verbose verbose mode
								-F, –format=c|t|p output file format (custom, tar, plain text)
								-c, –clean clean (drop) schema prior to create
								-b, –blobs include large objects in dump
								-v, –verbose verbose mode
								-f, –file=FILENAME output file name
						 */
          //pg_dump msi_db
          logger.info("Starting to backup database # " + msiDb)
          var cmd = Seq(pathSrcDump, "-i", "-h", host, "-p", port.toString, "-U", user, "-F", "c", "-b", "-v", "-f", new File(pathDestinationProject, "\\" + msiDb + ".sql").getCanonicalPath(), msiDb)
          execute(cmd)
          //pg_dump lcms_Db
          logger.info("Starting to backup database # " + lcmsDb)
          cmd = Seq(pathSrcDump, "-i", "-h", host, "-p", port.toString, "-U", user, "-F", "c", "-b", "-v", "-f", new File(pathDestinationProject, "\\" + lcmsDb + ".sql").getCanonicalPath(), lcmsDb)
          execute(cmd)
          //pg_dump uds_db
          logger.info("Starting to save schema only # uds_db")
          cmd = Seq(pathSrcDump, "-i", "-h", host, "-p", port.toString, "-U", user, "--schema-only", "-F", "c", "-b", "-v", "-f", new File(pathDestinationProject, "\\uds_db_schema.sql").getCanonicalPath(), "uds_db")
          execute(cmd)
          pgDumpIsOk = true
        } else {
          logger.error("failed trying to create the directory pg_dump_project_" + projectId + "")
        }
      } else {
        logger.error("the directory pg_dump_project_" + projectId + " already exist !")
      }
    } catch {
      case e: Exception => logger.error("error to execute cmd", e)
    }
    return pgDumpIsOk
  }

  def stdout(out: String) {
    logger.debug(out)
  }

  def stderr(err: String) {
    if (err.startsWith("Info: ")) {
      logger.debug(err)
    } else {
      logger.error(err)
    }
  }
}

object ArchiveProject {
  def apply(dsConnectorFactory: IDataStoreConnectorFactory, projectId: Long, pathSource: String, pathDestination: String): Unit = {
    new ArchiveProject(dsConnectorFactory, projectId, pathSource, pathDestination).doWork()
  }

}
