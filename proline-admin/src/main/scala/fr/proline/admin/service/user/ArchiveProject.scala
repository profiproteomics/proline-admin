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

import java.io.{ File, IOException, FileWriter }
import java.nio.file.{ Files, Path, Paths }
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
import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.WildcardFileFilter
import org.zeroturnaround.zip.ZipUtil

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
              logger.info(s"Starting to backup the project with id = #$projectId...")

              //pg_dump  databases for a project

              logger.info(s"Starting to pg_dump of MSI,LCMS and schema of UDS databases...")

              if (execPgDump(externalDbMsi.getHost(), externalDbMsi.getPort(), externalDbMsi.getDbUser(), externalDbMsi.getDbName(), externalDbLcms.getDbName(), pathDestination, pathSource, projectId)) {
                val fileToWrite = new File(pathDestination + File.separator + "project_" + projectId + File.separator + "uds_db_data.tsv")

                var csvPrinter: CSVPrinter = null

                csvPrinter = new CSVPrinter(new FileWriter(fileToWrite.getAbsoluteFile()), CSVFormat.MYSQL.withDelimiter('#').withNullString("null"));

                //rows of project (uds)

                val serializedProperties = Option(project.getSerializedProperties())
                csvPrinter.printRecord("project", projectId.toString, project.getName(), project.getDescription(), project.getCreationTimestamp(), serializedProperties.getOrElse(""), project.getOwner().getId().toString,
                  project.getLockExpirationTimestamp(), project.getLockUserID())

                //rows of external_db(msi)

                csvPrinter.printRecord("external_db", externalDbMsi.getId().toString, externalDbMsi.getDbName(), externalDbMsi.getConnectionMode(), externalDbMsi.getDbUser(), externalDbMsi.getDbPassword(),
                  externalDbMsi.getHost(), externalDbMsi.getPort(), externalDbMsi.getType(), externalDbMsi.getDbVersion(), externalDbMsi.getIsBusy().toString, externalDbMsi.getSerializedProperties())

                // rows of external_db(lcms)

                csvPrinter.printRecord("external_db", externalDbLcms.getId().toString, externalDbLcms.getDbName(), externalDbLcms.getConnectionMode(), externalDbLcms.getDbUser(), externalDbLcms.getDbPassword(),
                  externalDbLcms.getHost(), externalDbLcms.getPort(), externalDbLcms.getType(), externalDbLcms.getDbVersion(), externalDbLcms.getIsBusy().toString, externalDbLcms.getSerializedProperties())
                DoJDBCWork.withEzDBC(udsDbCtx) { ezDBC =>

                  // rows of project_db_map

                  ezDBC.selectAndProcess(s" SELECT project_id,external_db_id FROM project_db_map WHERE project_id=$projectId ") { record =>
                    if (record.getLong("project_id") > 0 && record.getLong("external_db_id") > 0) {

                      csvPrinter.printRecord("project_db_map", record.getLong("project_id").toString, record.getLong("external_db_id").toString)
                    }
                  }

                  //rows of data_set

                  ezDBC.selectAndProcess(s" SELECT id,number,name,description ,type ,keywords ,creation_timestamp ,modification_log,children_count ,serialized_properties, result_set_id ,result_summary_id ,aggregation_id ,fractionation_id ,quant_method_id ,parent_dataset_id ,project_id FROM data_set WHERE project_id=$projectId  order by id asc") { record =>
                    if (record.getLong("id") > 0 && record.getInt("number") >= 0 && record.getString("name") != null && record.getString("type") != null && record.getInt("children_count") >= 0 && record.getLong("project_id") >= 0) {

                      csvPrinter.printRecord("data_set", record.getLong("id").toString, record.getInt("number").toString, record.getString("name"), record.getStringOption("description").getOrElse(""), record.getString("type"), record.getStringOption("keywords").getOrElse(""),
                        record.getString("creation_timestamp"), record.getStringOption("modification_log").getOrElse(""), record.getInt("children_count").toString, record.getStringOption("serialized_properties").getOrElse(""), record.getLong("result_set_id").toString,
                        record.getLong("result_summary_id").toString, record.getLong("aggregation_id").toString, record.getLong("fractionation_id").toString, record.getLong("quant_method_id").toString, record.getLong("parent_dataset_id").toString,
                        record.getLong("project_id").toString)
                    }
                  }
                  // rows of run_identification

                  ezDBC.selectAndProcess(s" SELECT id,serialized_properties,run_id,raw_file_identifier FROM run_identification WHERE id in (SELECT id FROM data_set WHERE project_id=$projectId)") { record =>
                    if (record.getLong("id") > 0) {
                      csvPrinter.printRecord("run_identification", record.getLong("id").toString, record.getStringOption("serialized_properties").getOrElse(""), record.getLong("run_id").toString, record.getStringOption("raw_file_identifier").getOrElse(""))
                    }
                  }

                  // rows of project_user_account_map
                  ezDBC.selectAndProcess(s" SELECT project_id,user_account_id,serialized_properties,write_permission FROM project_user_account_map WHERE project_id=$projectId") { record =>
                    if ((record.getLong("project_id") > 0) && (record.getLong("user_account_id") > 0) && (record.getString("write_permission") != null)) {
                      csvPrinter.printRecord("project_user_account", record.getLong("project_id").toString, record.getLong("user_account_id").toString, record.getStringOption("serialized_properties").getOrElse(""), record.getBoolean("write_permission").toString)
                    }
                  }
                }
                csvPrinter.flush()
                csvPrinter.close()
                // set the file readable only  
                fileToWrite.setWritable(false)

                //update serialized properties
                val sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
                array.addProperty("archive_date", sdf.format(new Date()).toString())
                array.addProperty("is_active", false)
                project.setSerializedProperties(array.toString())
                udsEM.merge(project)
                if (localUdsTransaction != null) {
                  localUdsTransaction.commit()
                  udsTransacOK = true
                }
                //zip directory 
                ZipUtil.pack(new File(pathDestination + File.separator + "project_" + projectId), new File(pathDestination + File.separator + "project_" + projectId + ".zip"))

                //get only zip directory 
                deleteDirectory(new File(pathDestination + File.separator + "project_" + projectId))

                logger.info(s"Project with id= $projectId has been archived .")
              }
            } catch {
              case t: Throwable => logger.error("Error while archiving project", t)
            }
          } else {
            logger.error("Some parameters are missing in table uds_db.external_db")
          }

        } else {
          logger.error(s"project # $projectId does not exist in uds_db database")
        }
      } else {
        logger.error(s"Invalid parameters for the project # $projectId")
      }

    } finally {
      udsEM.setFlushMode(FlushModeType.AUTO)
      udsDbCtx.close()
      udsDbConnector.close()
    }
  }

  //execute command as sequence of string

  def execute(command: Seq[String]): Boolean = {
    var exeCmd: Boolean = true
    logger.debug("Executing " + command.mkString(" "))
    process = Process(command).run(ProcessLogger(out => stdout(out), err => stderr(err)))
    val exitCode = process.exitValue
    process.destroy
    process = null
    logger.debug("Exit code is '" + exitCode + "'")
    // check exit code
    if (exitCode != 0) {
      exeCmd = false
      logger.error("Command has failed !")
    }
    return exeCmd
  }

  // dump all databases 

  def execPgDump(host: String, port: Integer, user: String, msiDb: String, lcmsDb: String, pathDestination: String, pathSource: String, projectId: Long): Boolean = {

    val pathDestinationProject = new File(pathDestination + File.separator + "project_" + projectId)

    // create the files 
    var pgDumpAll: Boolean = false
    try {
      val path = Paths.get(pathDestinationProject.getPath)
      if (!Files.exists(path)) {
        try {
          Files.createDirectories(path)
          println("path " + path)
          val pathSrcDump = FileUtils.getFile(new File(pathSource), "pg_dump").getPath()
          val msiBackUpFile = FileUtils.getFile(pathDestinationProject, msiDb + ".bak")
          val lcmsBackUpFile = FileUtils.getFile(pathDestinationProject, lcmsDb + ".bak")
          val udsBackUpFile = FileUtils.getFile(pathDestinationProject, "uds_db_schema.bak")
          /**
           * options of pg_dump
           * -p, –port=PORT database server port number
           * -i, –ignore-version proceed even when server version mismatches
           * -h, –host=HOSTNAME database server host or socket directory
           * -U, –username=NAME connect as specified database user
           * -W, –password force password prompt (should happen automatically)
           * -d, –dbname=NAME connect to database name
           * -v, –verbose verbose mode
           * -F, –format=c|t|p output file format (custom, tar, plain text)
           * -c, –clean clean (drop) schema prior to create
           * -b, –blobs include large objects in dump
           * -v, –verbose verbose mode
           * -f, –file=FILENAME output file name
           */
          println("msiBackUpFile : " + msiBackUpFile.getPath())
          logger.info("Starting to backup database # " + msiDb)
          var cmd = Seq(pathSrcDump, "-i", "-h", host, "-p", port.toString, "-U", user, "-w", "-F", "c", "-b", "-v", "-f", msiBackUpFile.getPath(), msiDb)
          val pgDumpMsi = execute(cmd)
          println("lcmsBackUpFile : " + lcmsBackUpFile.getPath())
          logger.info("Starting to backup database # " + lcmsDb)
          cmd = Seq(pathSrcDump, "-i", "-h", host, "-p", port.toString, "-U", user, "-w", "-F", "c", "-b", "-v", "-f", lcmsBackUpFile.getPath(), lcmsDb)
          val pgDumpLcms = execute(cmd)
          println("uds : " + udsBackUpFile.getPath())
          logger.info("Starting to backup schema  # uds_db")
          cmd = Seq(pathSrcDump, "-i", "-h", host, "-p", port.toString, "-U", user, "-w", "--schema-only", "-F", "c", "-b", "-v", "-f", udsBackUpFile.getPath(), "uds_db")
          val pgDumpUds = execute(cmd)

          if (pgDumpMsi && pgDumpLcms && pgDumpUds) {
            pgDumpAll = true
          } else {
            deleteDirectory(pathDestinationProject)
          }
        } catch {
          case ioe: IOException => ioe.printStackTrace()
        }
      } else {
        logger.error(s"the directory project_$projectId already exist !")
      }
    } catch {
      case e: Exception => logger.error("error to execute cmd", e)
    }
    return pgDumpAll
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

  //delete directory

  def deleteDirectory(directoryName: File) {
    try {
      FileUtils.deleteDirectory(directoryName)
    } catch {
      case ioe: IOException => ioe.printStackTrace()
    }
  }
}

object ArchiveProject {
  def apply(dsConnectorFactory: IDataStoreConnectorFactory, projectId: Long, pathSource: String, pathDestination: String): Unit = {
    new ArchiveProject(dsConnectorFactory, projectId, pathSource, pathDestination).doWork()
  }

}
