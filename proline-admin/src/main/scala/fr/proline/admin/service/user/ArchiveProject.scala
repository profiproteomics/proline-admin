package fr.proline.admin.service.user

import com.typesafe.scalalogging.LazyLogging
import fr.proline.core.orm.util.DataStoreConnectorFactory
import fr.proline.context.DatabaseConnectionContext
import fr.proline.admin.service.db.SetupProline
import fr.proline.core.dal.context._
import fr.proline.core.dal.DoJDBCWork
import fr.proline.core.dal.DoJDBCReturningWork
import fr.proline.core.orm.uds.{ Project, Dataset, UserAccount, ExternalDb }
import fr.proline.core.orm.uds.repository.ExternalDbRepository

import java.io.{ File, FileWriter, BufferedWriter }
import java.nio.file.{ Files, Paths }
import java.text.SimpleDateFormat
import java.util.Date

import scala.sys.process.Process
import scala.sys.process.ProcessLogger
import scala.collection.mutable.{ ListBuffer, Map }
import scala.util.{ Try, Success, Failure }
import com.google.gson.{ JsonObject, JsonParser }
import play.api.libs.json._
import org.apache.commons.io.FileUtils
import org.zeroturnaround.zip.ZipUtil

/**
 * Archive a Proline project. It makes pg_dump of msi_db and lcms_db databases.
 * It creates json file with the project properties.
 *
 * @author aromdhani
 *
 * @param udsDbContext The connection context to UDSDb to archive project into.
 * @param projectId The project id.
 * @param binDirPath The PostgreSQL bin directory path. It should contains pg_dump file.
 * @param archiveDirPath The archive directory path.
 *
 */
class ArchiveProject(
    udsDbCtx: DatabaseConnectionContext,
    pgUserName: String,
    projectId: Long,
    binDirPath: String,
    archiveDirPath: String) extends LazyLogging {
  var isSuccess: Boolean = false

  def run() {

    var archiveParamsAsMap: Map[String, Object] = Map[String, Object]()
    /*1-check the input parameters*/
    udsDbCtx.tryInTransaction {
      val udsEM = udsDbCtx.getEntityManager
      require(isValidatedBinDir(binDirPath).isDefined && isValidatedArchiveDir(archiveDirPath), "Invalid parameters. Make sure that the bin directory contains the pg_dump.exe file"
        + " and the archive directory exists.")
      val udsProjectOpt = Option(udsEM.find(classOf[Project], projectId))
      require((udsProjectOpt.isDefined), s"The project with id= #$projectId is undefined.")
      val project = udsProjectOpt.get
      archiveParamsAsMap += ("project" -> project)
      val externalDbMsiOpt = Option(ExternalDbRepository.findExternalByTypeAndProject(udsEM, fr.proline.repository.ProlineDatabaseType.MSI, project))
      val externalDbLcmsOpt = Option(ExternalDbRepository.findExternalByTypeAndProject(udsEM, fr.proline.repository.ProlineDatabaseType.LCMS, project))
      require((externalDbMsiOpt.isDefined) && (externalDbLcmsOpt.isDefined), s"The external databases MSI and LCMS of project with id= #$projectId are undefined.")
      val externalDbMsi = externalDbMsiOpt.get
      archiveParamsAsMap += ("msi" -> externalDbMsi)
      val externalDbLcms = externalDbLcmsOpt.get
      archiveParamsAsMap += ("lcms" -> externalDbLcms)
    }
    if (archiveParamsAsMap.values.count(_ != null) == 3) {
      logger.info(s"Start archiving the project with id = #$projectId. Please wait until to archive project complete ...")
      /* 2-chain operations : create files, pg_dump project databases(msi and lcms), create project properties(Json file) and compress the project folder */
      val (externalDbMsi, externalDbLcms, project) = (archiveParamsAsMap("msi").asInstanceOf[ExternalDb],
        archiveParamsAsMap("lcms").asInstanceOf[ExternalDb],
        archiveParamsAsMap("project").asInstanceOf[Project])

      val archiveProject = for {
        backupDbs <- backupDatabases(externalDbMsi.getHost(), externalDbMsi.getPort(), pgUserName, externalDbMsi.getDbName(), externalDbLcms.getDbName(), archiveDirPath, binDirPath, projectId)
        createProjProps <- createProjProperties(project, externalDbMsi, externalDbLcms)
        zipFile <- zipFile(archiveDirPath + File.separator + "project_" + projectId)
      } yield ()

      /* 3-update the project serialized properties or remove the project folder */
      archiveProject match {
        case Success(_) => {
          val isTxOk = udsDbCtx.tryInTransaction {
            val udsEM = udsDbCtx.getEntityManager
            val properties = project.getSerializedProperties()
            var parser = new JsonParser()
            var array: JsonObject = Try { parser.parse(properties).getAsJsonObject() } getOrElse { parser.parse("{}").getAsJsonObject() }
            val sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
            array.addProperty("archive_date", sdf.format(new Date()).toString())
            array.addProperty("is_active", false)
            project.setSerializedProperties(array.toString())
            udsEM.merge(project)
          }
          if (isTxOk) {
            isSuccess = true
            logger.info(s"The project with id= #$projectId has been archived successfully.")
          } else {
            isSuccess = false
            logger.error(s"Error while trying to update serialized properties!")
          }
        }
        case Failure(t) => {
          //delete the project directory
          deleteDirectory(new File(archiveDirPath + File.separator + "project_" + projectId))
          isSuccess = false
          logger.error(s"Error while trying to archive the project with id=#$projectId ", t.printStackTrace)
        }
      }
    }
  }

  /**
   * Check if it's a validated bin directory.
   * @param binDirPath the path of the bin directory
   * @return <code>Some(File)</code> if it's validated bin directory otherwise None.
   */
  def isValidatedBinDir(binDirPath: String): Option[File] = {
    if (new File(binDirPath).exists()) new File(binDirPath).listFiles().find(file => file.getName.matches("^pg_dump.exe$") && file.canExecute()) else None
  }

  /**
   * Check if user can write in the archive directory.
   * @param filePath the path of the archive directory
   * @return <code>true</code> if it's validated archive directory otherwise false.
   */
  def isValidatedArchiveDir(filePath: String): Boolean = new File(filePath).canWrite()

  /**
   * Compresses the given directory and all its sub-directories into a ZIP file.
   * @param the file to ZIP path
   *
   */
  def zipFile(zipFilePath: String): Try[Unit] = Try {
    ZipUtil.pack(new File(zipFilePath), new File(zipFilePath + ".zip"))
  }

  /**
   * Write project properties in json file.
   * @param the project properties file.
   * @param data the JsObject to write
   * @return <code>true</code> if writing json data is finished withh success.
   */
  def writeJsonData(file: File, data: JsObject): Boolean = {
    var bw: Option[BufferedWriter] = None
    try {
      synchronized {
        bw = Some(new BufferedWriter(new FileWriter(file)))
        bw.get.write(Json.stringify(data))
      }
      true
    } catch {
      case t: Throwable =>
        logger.error("Error while trying to write in json file!", t.printStackTrace);
        false
    } finally {
      if (bw.isDefined) bw.get.close()
    }
  }

  /**
   * Execute command as sequence of string.
   * @param command The command as sequence of string ,watch out from the white spaces.
   * @return <code>0</code> if the sequence executed successfully.
   */
  def execute(command: => Seq[String]): Int = {
    var process: Option[Process] = None
    var exitCode: Int = 1
    try {
      logger.debug("Executing " + command.mkString(" "))
      process = Some(Process(command).run(ProcessLogger(out => stdout(out), err => stderr(err))))
      exitCode = process.get.exitValue
    } finally {
      if (process.isDefined) {
        process.get.destroy
      }
    }
    logger.debug("Exit code is '" + exitCode + "'")
    if (exitCode != 0) {
      throw new Exception("Command has failed: " + command)
    }
    exitCode
  }

  /**
   * Create and write the project properties in json file.
   * @param project The project object
   * @param externalDbMsi The external msi_db
   * @param externalDbLcms The external lcms_db
   *
   */
  def createProjProperties(project: Project, externalDbMsi: ExternalDb, externalDbLcms: ExternalDb): Try[Unit] = Try {
    val projectPropFile = new File(archiveDirPath + File.separator + "project_" + projectId + File.separator + "project_properties.json")
    val owner = project.getOwner()
    var schemaVersion: Option[JsObject] = None
    var dataSetList = new ListBuffer[JsObject]()
    var runIdentificationList = new ListBuffer[JsObject]()
    var projectUserMapList = new ListBuffer[JsObject]()
    DoJDBCWork.withEzDBC(udsDbCtx) { ezDBC =>
      //data_set
      ezDBC.selectAndProcess(s" SELECT id, number, name, description, type, keywords, creation_timestamp, modification_log, child_count, serialized_properties, result_set_id ,result_summary_id, aggregation_id, fractionation_id, quant_method_id, parent_dataset_id, project_id FROM data_set WHERE project_id=$projectId  order by id asc") {
        record =>
          if (record.getLong("id") > 0 && record.getInt("number") >= 0 && record.getString("name") != null && record.getString("type") != null && record.getLong("project_id") > 0) {
            dataSetList += Json.obj("id" -> record.getLong("id"),
              "number" -> record.getInt("number"),
              "name" -> record.getString("name"),
              "description" -> record.getString("description"),
              "type" -> record.getString("type"),
              "keywords" -> record.getString("keywords"),
              "creation_timestamp" -> record.getString("creation_timestamp"),
              "modification_log" -> record.getStringOption("modification_log"),
              "child_count" -> record.getInt("child_count"),
              "serialized_properties" -> record.getString("serialized_properties"),
              "result_set_id" -> record.getLong("result_set_id"),
              "result_summary_id" -> record.getLong("result_summary_id"),
              "aggregation_id" -> record.getLong("aggregation_id"),
              "fractionation_id" -> record.getLong("fractionation_id"),
              "quant_method_id" -> record.getLong("quant_method_id"),
              "parent_dataset_id" -> record.getLong("parent_dataset_id"))
          }
      }
      //run_identification
      ezDBC.selectAndProcess(s" SELECT id,serialized_properties,run_id,raw_file_identifier FROM run_identification WHERE id in (SELECT id FROM data_set WHERE project_id=$projectId)") { record =>
        if (record.getLong("id") > 0) {
          runIdentificationList += Json.obj("id" -> record.getLong("id"),
            "serialized_properties" -> record.getString("serialized_properties"),
            "id" -> record.getLong("id"),
            "run_id" -> record.getLong("run_id"),
            "raw_file_identifier" -> record.getString("raw_file_identifier"))
        }
      }
      //project_user_account_map
      ezDBC.selectAndProcess(s" SELECT project_id,user_account_id,serialized_properties,write_permission FROM project_user_account_map WHERE project_id=$projectId") { record =>
        if ((record.getLong("project_id") > 0) && (record.getLong("user_account_id") > 0) && (record.getString("write_permission") != null)) {
          projectUserMapList += Json.obj("project_id" -> record.getLong("project_id"),
            "user_account_id" -> record.getLong("user_account_id"),
            "serialized_properties" -> record.getString("serialized_properties"),
            "write_permission" -> record.getBoolean("write_permission"))
        }
      }
      ezDBC.selectAndProcess("SELECT MAX(version_rank) AS version, MAX(installed_rank) AS rank  FROM schema_version") { record =>
        schemaVersion = Some(Json.obj("version" -> record.getInt("version"),
          "installed_rank" -> record.getInt("rank")))
      }
    }
    //project properties as Json object 
    val projectPropertiesAsJson = Json.obj(
      "schema_version" -> schemaVersion.get,
      "project" -> Json.obj("id" -> project.getId(),
        "name" -> project.getName(),
        "description" -> project.getDescription(),
        "creation_timestamp" -> project.getCreationTimestamp().toString,
        "serialized_properties" -> project.getSerializedProperties()),
      "user_account" -> Json.obj(
        "id" -> owner.getId(),
        "creation_mode" -> owner.getCreationMode(),
        "login" -> owner.getLogin(),
        "password_hash" -> owner.getPasswordHash(),
        "serialized_properties" -> owner.getSerializedProperties()),
      "external_db" -> Json.arr(
        Json.obj(
          "name" -> externalDbMsi.getDbName(),
          "version" -> externalDbMsi.getDbVersion(),
          "host" -> externalDbMsi.getHost(),
          "is_busy" -> externalDbMsi.getIsBusy(),
          "port" -> externalDbMsi.getPort().toInt,
          "serialized_properties" -> externalDbMsi.getSerializedProperties(),
          "type" -> externalDbMsi.getType().toString,
          "connection_mode" -> externalDbMsi.getConnectionMode().toString),
        Json.obj(
          "name" -> externalDbLcms.getDbName(),
          "version" -> externalDbLcms.getDbVersion(),
          "host" -> externalDbLcms.getHost(),
          "is_busy" -> externalDbLcms.getIsBusy(),
          "port" -> externalDbLcms.getPort().toInt,
          "serialized_properties" -> externalDbLcms.getSerializedProperties(),
          "type" -> externalDbLcms.getType().toString,
          "connection_mode" -> externalDbLcms.getConnectionMode().toString)),
      "data_set" -> Json.toJson(dataSetList),
      "run_identification" -> Json.toJson(runIdentificationList),
      "project_user_account_map" -> Json.toJson(projectUserMapList))
    writeJsonData(projectPropFile, projectPropertiesAsJson)
  }

  /**
   * backup Proline project databases.
   * @param host The host name.
   * @param port The port number its default value 5432.
   * @param user The user name.
   * @param msiDb The Msi database name.
   * @param lcmsDb The Lcms database name.
   * @param archivepath the archive path where the project will archived.
   * @param binPath The bin directory path.
   * @param projectId The project id.
   * @return <code>Success(True)</code> if the pg_dump finished with success.
   *
   */
  def backupDatabases(host: String, port: Integer, user: String, msiDb: String, lcmsDb: String, archiveDirPath: String, binDirPath: String, projectId: Long): Try[Boolean] =
    createFiles(archiveDirPath, binDirPath, msiDb, lcmsDb).flatMap {
      case (pgDumpPath, msiBackUpFile, lcmsBackUpFile) => Try {
        logger.info("Start to pg_dump database # " + msiDb)
        var cmd = Seq(pgDumpPath, "-h", host, "-p", port.toString, "-U", user, "-w", "-F", "c", "-b", "-v", "-f", msiBackUpFile.getPath(), msiDb)
        val dumpMsiExitCode = execute(cmd)
        logger.info("Start to pg_dump database # " + lcmsDb)
        cmd = Seq(pgDumpPath,"-h", host, "-p", port.toString, "-U", user, "-w", "-F", "c", "-b", "-v", "-f", lcmsBackUpFile.getPath(), lcmsDb)
        val dumpLcmsExitCode = execute(cmd)
        Seq(dumpMsiExitCode,
          dumpLcmsExitCode).forall(_ == 0)
      }
    }

  /**
   * Create a folder with the list of files used to archive a Proline project.
   * @param archivePath The path of archive path where the project will be archived.
   * @param binDirPath The bin directory of PostgreSQL should contains pg_dump.exe.
   * @param msiDb The name of msiDb.
   * @param lcsmDb The name of lcmsDb.
   * @return <code>Success(String, File, File)</code> tuple of the eventual created files.
   */
  def createFiles(archivePath: String, binDirPath: String, msiDb: String, lcmsDb: String): Try[(String, File, File)] = Try {
    val archiveProjectPath = archivePath + File.separator + "project_" + projectId
    val path = Paths.get(archiveProjectPath)
    Files.createDirectories(path)
    val pgDumpPath = FileUtils.getFile(new File(binDirPath), "pg_dump").getPath()
    val msiDbBackupFile = FileUtils.getFile(archiveProjectPath, msiDb + ".bak")
    val lcmsDbBackupFile = FileUtils.getFile(archiveProjectPath, lcmsDb + ".bak")
    (pgDumpPath, msiDbBackupFile, lcmsDbBackupFile)
  }

  /**
   * logger debug
   * @param err
   */
  def stdout(out: String) {
    logger.debug(out)
  }

  /**
   * logger debug or error
   * @param err
   */
  def stderr(err: String) {
    if (err.startsWith("Info: ")) {
      logger.debug(err)
    } else {
      logger.error(err)
    }
  }

  /**
   * Delete a directory.
   * @param  directory The Directory to delete.
   */
  def deleteDirectory(directory: File): Unit = {
    try {
      directory.listFiles().filter(_.isFile()).filter(_.exists()).foreach { f => if (!f.delete()) logger.error(s"Unable to delete file $f") }
      if (directory.exists() && directory.listFiles().isEmpty) directory.delete()
    } catch {
      case t: Throwable => logger.error("Unable to delete the project folder! ", t.printStackTrace)
    }
  }
}

object ArchiveProject {
  /**
   * @param projectId The project id.
   * @param binDirPath The bin directory path. It should contains pg_dump.exe
   * @param archiveDirPath The archive directory path.
   * @return <code>true</code> if the project is archived successfully otherwise false.
   *
   */
  def apply(pgUserName: String, projectId: Long, binDirPath: String, archiveDirPath: String): Boolean = {
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
        val archiveProject = new ArchiveProject(udsDbContext, pgUserName, projectId, binDirPath, archiveDirPath)
        archiveProject.run()
        isSuccess = archiveProject.isSuccess
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

