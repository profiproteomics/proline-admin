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
import java.text.SimpleDateFormat
import scala.sys.process.Process
import scala.sys.process.ProcessLogger
import scala.collection.JavaConversions._

import com.google.gson.JsonObject
import com.google.gson.JsonParser

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.apache.commons.csv.CSVRecord

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

              //pg_dump  databases for a project

              logger.info(s"Starting to pg_dump of MSI,LCMS and schema of UDS databases...")
              if (execPgDump(externalDbMsi.getHost(), externalDbMsi.getPort(), externalDbMsi.getDbUser(), externalDbMsi.getDbPassword(), externalDbMsi.getDbName(), externalDbLcms.getDbName(), pathDestination, pathSource, projectId)) {
                val fileToWrite = new File(pathDestination, "\\project_" + projectId + "\\uds_db_data.csv")

                var csvPrinter: CSVPrinter = null

                csvPrinter = new CSVPrinter(new FileWriter(fileToWrite.getAbsoluteFile()), CSVFormat.MYSQL.withDelimiter('#').withNullString("null"));

                //rows project 
                val serializedProperties = Option(project.getSerializedProperties())
                csvPrinter.printRecord("project", projectId.toString, project.getName(), project.getDescription(), project.getCreationTimestamp(), serializedProperties.getOrElse(""), project.getOwner().getId().toString,
                  project.getLockExpirationTimestamp(), project.getLockUserID());

                //rows of external_db(msi)

                csvPrinter.printRecord("externaldbmsi", externalDbMsi.getId().toString, externalDbMsi.getDbName(), externalDbMsi.getConnectionMode(), externalDbMsi.getDbUser(), externalDbMsi.getDbPassword(),
                  externalDbMsi.getHost(), externalDbMsi.getPort(), externalDbMsi.getType(), externalDbMsi.getDbVersion(), externalDbMsi.getIsBusy().toString, externalDbMsi.getSerializedProperties())

                // rows of external_db(lcms)

                csvPrinter.printRecord("externaldblcms", externalDbLcms.getId().toString, externalDbLcms.getDbName(), externalDbLcms.getConnectionMode(), externalDbLcms.getDbUser(), externalDbLcms.getDbPassword(),
                  externalDbLcms.getHost(), externalDbLcms.getPort(), externalDbLcms.getType(), externalDbLcms.getDbVersion(), externalDbLcms.getIsBusy().toString, externalDbLcms.getSerializedProperties())
                DoJDBCWork.withEzDBC(udsDbCtx) { ezDBC =>

                  // rows of project_db_map

                  ezDBC.selectAndProcess(" SELECT project_id,external_db_id FROM project_db_map WHERE project_id=" + projectId) { record =>
                    if (record.getLong("project_id") > 0 && record.getLong("external_db_id") > 0) {

                      csvPrinter.printRecord("projectdbmap", record.getLong("project_id").toString, record.getLong("external_db_id").toString)
                    }
                  }
                  //rows of data_set

                  ezDBC.selectAndProcess(" SELECT id,number,name,description ,type ,keywords ,creation_timestamp ,modification_log,children_count ,serialized_properties, result_set_id ,result_summary_id ,aggregation_id ,fractionation_id ,quant_method_id ,parent_dataset_id ,project_id FROM data_set WHERE project_id=" + projectId) { record =>
                    if (record.getLong("id") > 0 && record.getInt("number") >= 0 && record.getString("name") != null && record.getString("type") != null && record.getInt("children_count") >= 0 && record.getLong("project_id") >= 0) {

                      csvPrinter.printRecord("dataset", record.getLong("id").toString, record.getInt("number").toString, record.getString("name"), record.getStringOption("description").getOrElse(""), record.getString("type"), record.getStringOption("keywords").getOrElse(""),
                        record.getString("creation_timestamp"), record.getStringOption("modification_log").getOrElse(""), record.getInt("children_count").toString, record.getStringOption("serialized_properties").getOrElse(""), record.getLong("result_set_id").toString,
                        record.getLong("result_summary_id").toString, record.getLong("aggregation_id").toString, record.getLong("fractionation_id").toString, record.getLong("quant_method_id").toString, record.getLong("parent_dataset_id").toString,
                        record.getLong("project_id").toString)
                    }
                  }
                  // rows of run_identification

                  ezDBC.selectAndProcess(" SELECT id,serialized_properties,run_id,raw_file_identifier FROM run_identification WHERE id in (SELECT id FROM data_set WHERE project_id=" + projectId + ")") { record =>
                    if (record.getLong("id") > 0) {
                      csvPrinter.printRecord("runidentification", record.getLong("id").toString, record.getStringOption("serialized_properties").getOrElse(""), record.getLong("run_id").toString, record.getStringOption("raw_file_identifier").getOrElse(""))
                    }
                  }

                  // rows of project_user_account_map
                  ezDBC.selectAndProcess(" SELECT project_id,user_account_id,serialized_properties,write_permission FROM project_user_account_map WHERE project_id=" + projectId) { record =>
                    if ((record.getLong("project_id") > 0) && (record.getLong("user_account_id") > 0) && (record.getString("write_permission") != null)) {
                      csvPrinter.printRecord("projectuseraccount", record.getLong("project_id").toString, record.getLong("user_account_id").toString, record.getStringOption("serialized_properties").getOrElse(""), record.getBoolean("write_permission").toString)
                    }
                  }
                }

                csvPrinter.flush()
                csvPrinter.close()
                // set the file only readable 
                fileToWrite.setWritable(false)
                logger.info("Project with id= " + projectId + " has been archived .")
              }
            } catch {
              case t: Throwable => logger.error("Error while archiving project", t)
            }
            //update serialized properties 
            
              val sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
              array.addProperty("archived", sdf.format(new Date()).toString())
              array.addProperty("actif",false)
              project.setSerializedProperties(array.toString())
              udsEM.merge(project)
     
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

    val pathDestinationProject = new File(pathDestination, "\\project_" + projectId)
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
          logger.error("failed trying to create the directory project_" + projectId + "")
        }
      } else {
        logger.error("the directory project_" + projectId + " already exist !")
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
