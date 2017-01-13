package fr.proline.admin.service.user

import javax.persistence.EntityTransaction
import javax.persistence.FlushModeType
import com.typesafe.scalalogging.LazyLogging

import fr.proline.admin.service.ICommandWork
import fr.proline.core.orm.util.DataStoreConnectorFactory
import fr.proline.repository.DriverType
import fr.proline.core.orm.uds.ExternalDb
import fr.proline.core.orm.uds.Project
import fr.proline.core.orm.uds.{ UserAccount, ProjectUserAccountMapPK, ProjectUserAccountMap }
import fr.proline.core.orm.uds.repository.ExternalDbRepository
import fr.proline.context._
import fr.proline.core.dal.DoJDBCReturningWork
import fr.proline.repository._
import fr.proline.core.dal.DoJDBCWork

import java.util.Date
import java.text.SimpleDateFormat
import scala.sys.process.Process
import scala.sys.process.ProcessLogger
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.{ File, FileReader, FileNotFoundException, IOException }
import scala.collection.JavaConversions._
import scala.collection._

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord
/**
 *  pg_restore  :
 *  -msi_db ,lcms_db ,uds_db only schema databases
 *  -some selected rows from uds_db database for the current project
 *
 */
class RestoreProject(dsConnectorFactory: IDataStoreConnectorFactory, projectId: Long, userId: Long, pathSource: String, pathDestination: String) extends ICommandWork with LazyLogging {
  var process: Process = null
  def doWork() {
    val udsDbConnector = dsConnectorFactory.getUdsDbConnector
    val udsDbCtx = new UdsDbConnectionContext(udsDbConnector)
    val udsEM = udsDbCtx.getEntityManager
    var localUdsTransaction: EntityTransaction = null
    var udsTransacOK: Boolean = false
    var dataBasesExist: Boolean = false

    try {
      /* check if databases lcms_db and msi_db exist  */
      try {
        var nbMsi = 0
        var nbLcms = 0
        DoJDBCWork.withEzDBC(udsDbCtx) { ezDBC =>
          ezDBC.selectAndProcess("SELECT count(1) as nbrmsi FROM pg_database WHERE datname='msi_db_project_" + projectId + "'") { record =>
            nbMsi = record.getInt("nbrmsi")
          }
          ezDBC.selectAndProcess("SELECT count(1) as nbrlcms FROM pg_database WHERE datname='lcms_db_project_" + projectId + "'") { record =>
            nbLcms = record.getInt("nbrlcms")
          }
        }
        if ((nbMsi > 0) && (nbLcms > 0)) {
          dataBasesExist = true
        }
      } catch {
        case t: Throwable => logger.error("Error while checking MSI and LCMS databases", t)
      }
      udsEM.setFlushMode(FlushModeType.COMMIT)
      if (!udsDbCtx.isInTransaction) {
        localUdsTransaction = udsEM.getTransaction()
        localUdsTransaction.begin()
        udsTransacOK = false
      }
      if ((projectId > 0) && (userId > 0) && (pathSource != null) && (!pathSource.isEmpty) && (pathDestination != null) && (!pathDestination.isEmpty)) {

        var project = udsEM.find(classOf[Project], projectId)
        var user = udsEM.find(classOf[UserAccount], userId)
        val udsDbDataFile = new File(pathDestination + "\\project_" + projectId + "\\uds_db_data.csv")
        var parser = new JsonParser()
        var array: JsonObject = null

        /* get project msi and lcms versions */

        var versions = getVersions(udsDbDataFile)
        try {
          var msiDbVersionCsv = show(versions.get("msi")).toDouble
          var lcmsDbVersionCsv = show(versions.get("lcms")).toDouble
        } catch {
          case e: NumberFormatException => logger.error(" can't convert database version you should upgrade your databases !")
        }

        /* case 1: project exist in uds_db */

        if ((project != null) && (user != null)) {
          val externalDbMsi = ExternalDbRepository.findExternalByTypeAndProject(udsEM, fr.proline.repository.ProlineDatabaseType.MSI, project)
          val externalDbLcms = ExternalDbRepository.findExternalByTypeAndProject(udsEM, fr.proline.repository.ProlineDatabaseType.LCMS, project)
          val msiVersion = externalDbMsi.getDbVersion
          val lcmsVersion = externalDbLcms.getDbVersion
          /* parse project properties */
          val properties = project.getSerializedProperties()
          try {
            array = parser.parse(properties).getAsJsonObject()
          } catch {
            case e: Exception =>
              logger.error("error accessing project properties")
              array = parser.parse("{}").getAsJsonObject()
          }

          /* case 1.1 : lcms_db and msi_db exist and their references in uds_db */
          
          if (dataBasesExist == true) {

            /* add a property restored : date  */

            val sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            array.addProperty("restored", sdf.format(new Date()).toString())
            array.addProperty("actif", true)
            project.setSerializedProperties(array.toString())
            udsEM.merge(project)

            if (localUdsTransaction != null) {
              localUdsTransaction.commit()
            }
            logger.info(" project #" + projectId + "  has been restored .")
          } else {

            /* case 1.2 : lcms_db and msi_db does not exist  */

            if (localUdsTransaction != null) {
              localUdsTransaction.commit()
            }
            /* create lcms and msi databases */
            createDataBases(udsDbCtx)
            /* pg_restore databases */
            if ((externalDbMsi.getHost() != null) && (!externalDbMsi.getHost().isEmpty) && (externalDbMsi.getDbUser() != null) && (!externalDbMsi.getDbUser().isEmpty) &&
              (externalDbMsi.getDbName() != null) && (!externalDbMsi.getDbName().isEmpty) && (externalDbLcms.getDbName() != null) && (!externalDbLcms.getDbName().isEmpty) &&
              (externalDbMsi.getPort() > 0)) {

              logger.info(s"Starting to restore msi_db and lcms_db ...")
              execPgRestore(externalDbMsi.getHost(), externalDbMsi.getPort(), externalDbMsi.getDbUser(), externalDbMsi.getDbPassword(), externalDbMsi.getDbName(),
                externalDbLcms.getDbName(), pathDestination, pathSource, projectId, false)
            }
            if (localUdsTransaction != null) {
              localUdsTransaction.begin()
            }
            val sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            array.addProperty("restored", sdf.format(new Date()).toString())
            array.addProperty("actif", true)
            project.setSerializedProperties(array.toString())
            udsEM.merge(project)
            if (localUdsTransaction != null) {
              localUdsTransaction.commit()
            }
            logger.info(" project #" + projectId + "  has been restored .")
          }
        } else {
          /* case 2: project does not exist in uds_db */
          logger.info("import project #" + projectId + "  in uds_db database")

          /*insert data in uds_db */
          upsertData(udsDbCtx, udsDbDataFile)
          /* case 2.1 : lcms_db and msi_db exist and their references in uds_db */
          if (localUdsTransaction != null) {
            localUdsTransaction.commit()
          }
          if (dataBasesExist == true) {

            //to do : update project 

            logger.info(" project #" + projectId + "  has been restored .")
          } else {
            /* case 1.2 : lcms_db and msi_db does not exist  */

            /* create lcms and msi databases */
            createDataBases(udsDbCtx)

            /* pg_restore databases */
            if ((!show(versions.get("host")).isEmpty) && (!show(versions.get("user")).isEmpty) && (show(versions.get("port")).toInt > 0)) {

              execPgRestore(show(versions.get("host")).toLowerCase(), show(versions.get("port")).toInt, show(versions.get("user")), show(versions.get("pw")), show(versions.get("msiName")),
                show(versions.get("lcmsName")), pathDestination, pathSource, projectId, false)
            }
            /* case 1.2 : lcms_db and msi_db does not exist  */

            logger.info(" project #" + projectId + "  has been restored .")
          }

        }
      } else {
        logger.error("Some parameters are missing for the project #" + projectId + " check  project id ,source path or destination path values !")
      }

    } finally {
      udsEM.setFlushMode(FlushModeType.AUTO)
      udsDbCtx.close()
      udsDbConnector.close()
    }
  }

  /* read csv file and get versions of databases */

  def getVersions(csvFile: File): Map[String, String] = {
    var versions: Map[String, String] = Map()
    val reader = new FileReader(csvFile)
    val csvFileParser = new CSVParser(reader, CSVFormat.MYSQL.withNullString("null"))
    try {
      var csvRecords = csvFileParser.getRecords()
      if ((csvRecords != null) && (!csvRecords.isEmpty)) {

        csvRecords.foreach { rec =>
          val record = rec.get(0)

          if ((record != null) && (!record.isEmpty) && (record.split("#")(0) == "externaldbmsi")) {
            versions += ("msi" -> record.split("#")(9))
            versions += ("host" -> record.split("#")(6))
            versions += ("port" -> record.split("#")(7))
            versions += ("user" -> record.split("#")(4))
            versions += ("pw" -> record.split("#")(5))
            versions += ("msiName" -> record.split("#")(2))
          }
          if ((record != null) && (!record.isEmpty) && (record.split("#")(0) == "externaldblcms"))
            versions += ("lcms" -> record.split("#")(9))
          versions += ("lcmsName" -> record.split("#")(2))
        }
      }
    } catch {
      case e: NumberFormatException => logger.error(" can't convert database version !")
    } finally {
      csvFileParser.close()
    }
    return versions
  }

  /* import data from csv file and insert database  */

  def upsertData(udsContext: UdsDbConnectionContext, csvFile: File) {
    val reader = new FileReader(csvFile)
    val csvFileParser = new CSVParser(reader, CSVFormat.MYSQL.withNullString("null"))
    try {
      var csvRecords = csvFileParser.getRecords()
      if ((csvRecords != null) && (!csvRecords.isEmpty)) {

        csvRecords.foreach { rec =>
          val record = rec.get(0)

          /* insert data in table project */

          if ((record != null) && (!record.isEmpty) && (record.split("#")(0) == "project")) {
            val projectId = record.split("#")(1)
            val name = record.split("#")(2)
            val description = record.split("#")(3)
            val creationTimestamp = record.split("#")(4)
            var srProperties = record.split("#")(5)
            val ownerId = record.split("#")(6)
            val lockExpirationTimestamp = record.split("#")(7)
            val lockUserId = record.split("#")(8)
            if ((projectId != null) && (name != null) && (!name.isEmpty) && (creationTimestamp != null) && (!creationTimestamp.isEmpty) && (ownerId != null)) {
              try {
                DoJDBCWork.withEzDBC(udsContext) { ezDBC =>
                  ezDBC.execute("insert into project values (" + projectId + ",'" + name + "','" + description + "','" + creationTimestamp + "','" +
                    srProperties + "'," + ownerId + "," + lockExpirationTimestamp + "," + lockUserId + ")")
                }
              } catch {
                case t: Throwable => logger.error("Error while insering data into table uds_db.project", t)
              }
            } else {
              logger.error(" trying to insert null in table uds_db.project ")
            }
          }

          /* insert data in table external_db */

          if ((record != null) && (!record.isEmpty) && ((record.split("#")(0) == "externaldbmsi") || (record.split("#")(0) == "externaldblcms"))) {
            val externalDbId = record.split("#")(1)
            val name = record.split("#")(2)
            val connectionMode = record.split("#")(3)
            val userName = record.split("#")(4)
            val ps = record.split("#")(5)
            val host = record.split("#")(6)
            val port = record.split("#")(7)
            val dbType = record.split("#")(8)
            val version = record.split("#")(9)
            val isBusy = record.split("#")(10)
            val srProperties = record.split("#")(11)
            if ((externalDbId != null) && (name != null) && (!name.isEmpty) && (connectionMode != null) && (!connectionMode.isEmpty)
              && (dbType != null) && (!dbType.isEmpty) && (version != null) && (!version.isEmpty) && (isBusy != null) && (!isBusy.isEmpty)) {
              try {
                DoJDBCWork.withEzDBC(udsContext) { ezDBC =>
                  ezDBC.execute("insert into external_db values (" + externalDbId + ",'" + name + "','" + connectionMode + "','" + userName + "','" +
                    ps + "','" + host + "'," + port + ",'" + dbType + "','" + version + "'," + isBusy + ",'" + srProperties + "')")
                }
              } catch {
                case t: Throwable => logger.error("Error while insering data into table uds_db.external_db", t)
              }
            } else {
              logger.error(" trying to insert null in table uds_db.external_db")
            }
          }

          /* insert data into project_db_map */

          if ((record != null) && (!record.isEmpty) && (record.split("#")(0) == "projectdbmap")) {
            val projectId = record.split("#")(1)
            val externalId = record.split("#")(2)
            if ((projectId != null) && (externalId != null)) {
              try {
                DoJDBCWork.withEzDBC(udsContext) { ezDBC =>
                  ezDBC.execute("insert into project_db_map values (" + projectId + "," + externalId + ")")
                }
              } catch {
                case t: Throwable => logger.error("Error while insering data into table uds_db.project_db_map", t)
              }
            } else {
              logger.error(" trying to insert null in table uds_db.project_db_map ")
            }
          }

          /* insert data into data_set */

          if ((record != null) && (!record.isEmpty) && (record.split("#")(0) == "dataset")) {
            val id = record.split("#")(1)
            val number = record.split("#")(2)
            val name = record.split("#")(3)
            val description = record.split("#")(4)
            val dbType = record.split("#")(5)
            val keyWords = record.split("#")(6)
            val creationTimestamp = record.split("#")(7)
            val modificationLog = record.split("#")(8)
            val childrenCount = record.split("#")(9)
            val serProperties = record.split("#")(10)
            var resultSetId: Integer = null
            if (record.split("#")(11).toInt > 0) resultSetId = record.split("#")(11).toInt
            var resultSummaryId: Integer = null
            if (record.split("#")(12).toInt > 0) resultSummaryId = record.split("#")(12).toInt
            var aggregationId: Integer = null
            if (record.split("#")(13).toInt > 0) aggregationId = record.split("#")(13).toInt
            var quantMethodId: Integer = null
            if (record.split("#")(15).toInt > 0) quantMethodId = record.split("#")(15).toInt
            var parentDatasetId: Integer = null
            if (record.split("#")(16).toInt > 0) parentDatasetId = record.split("#")(16).toInt
            var fractionationId: Integer = null
            if (record.split("#")(14).toInt > 0) fractionationId = record.split("#")(14).toInt
            val projectId = record.split("#")(17)

            if ((id != null) && (number != null) && (name != null) && (!name.isEmpty) && (dbType != null) && (!dbType.isEmpty)
              && (childrenCount != null) && (projectId != null)) {

              try {

                DoJDBCWork.withEzDBC(udsContext) { ezDBC =>
                  ezDBC.execute("insert into data_set values (" + id + "," + number + ",'" + name + "','" + description + "','" + dbType + "','" +
                    keyWords + "','" + creationTimestamp + "','" + modificationLog + "'," + childrenCount + ",'" + serProperties + "'," + resultSetId + "," +
                    resultSummaryId + "," + aggregationId + "," + fractionationId + "," + quantMethodId + "," + parentDatasetId + "," + projectId + ")")
                }

              } catch {
                case t: Throwable => logger.error("Error while insering data into table uds_db.data_set", t)
              }
            } else {
              logger.error(" trying to insert null in table uds_db.data_set ")
            }
          }

          /* insert data into runidentification :those rows can be null */
          if ((record != null) && (!record.isEmpty) && (record.split("#")(0) == "runidentification")) {
            val id = record.split("#")(1)
            val serProperties = record.split("#")(2)
            val runId = record.split("#")(3)
            val rawFileIdentifier = record.split("#")(4)
            if ((id != null)) {
              try {
                DoJDBCWork.withEzDBC(udsContext) { ezDBC =>
                  ezDBC.execute("insert into run_identification values (" + id + ",'" + serProperties + "'," + runId + ",'" + rawFileIdentifier + "')")
                }
              } catch {
                case t: Throwable => logger.error("Error while insering data into table uds_db.data_set", t)
              }

            } else {
              logger.error(" trying to insert null in table uds_db.run_identification ")
            }
          }

          /* insert data into project_user_account_map */

          if ((record != null) && (!record.isEmpty) && (record.split("#")(0) == "projectuseraccount")) {
            val projectId = record.split("#")(1)
            val userAccountId = record.split("#")(2)
            val serProperties = record.split("#")(3)
            val writePermission = record.split("#")(4)
            if ((projectId != null) && (userAccountId != null) && (writePermission != null)) {
              try {
                DoJDBCWork.withEzDBC(udsContext) { ezDBC =>
                  ezDBC.execute("insert into project_user_account_map values (" + projectId + "," + userAccountId + ",'" + serProperties + "'," + writePermission + ")")
                }
              } catch {
                case t: Throwable => logger.error("Error while insering data into table uds_db.project_user_account_map", t)
              }

            } else {
              logger.error(" trying to insert null in table uds_db.project_user_account_map ")
            }
          }
        }

      }
    } catch {
      case ex: IOException => logger.error("Had an IOException trying to read csv file")
    } finally {
      csvFileParser.close()
    }
  }

  /* create lcms and msi databases */

  def createDataBases(udsContext: UdsDbConnectionContext) {
    logger.info("creating MSI and LCMS databases ... ")
    try {
      DoJDBCWork.withEzDBC(udsContext) { ezDBC =>
        ezDBC.execute("CREATE DATABASE msi_db_project_" + projectId)
        ezDBC.execute("CREATE DATABASE lcms_db_project_" + projectId)
      }
    } catch {
      case t: Throwable => logger.error("Error while creating MSI and LCMS databases", t)
    }
  }

  /* execute command as sequence of string */
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
  /* pg_restore databases */
  def execPgRestore(host: String, port: Integer, user: String, passWord: String, msiDb: String, lcmsDb: String, pathDestination: String, pathSource: String, projectId: Long, udsDb: Boolean) {

    val pathDestinationProject = new File(pathDestination + "\\project_" + projectId)
    val pathSrcPgRestore = new File(pathSource, "\\pg_restore").getCanonicalPath()
    try {
      if (pathDestinationProject.exists()) {

        if (new File(pathDestinationProject.getCanonicalPath(), "\\msi_db_project_" + projectId + ".sql").exists()) {
          //pg_restore msi_db
          logger.info("Starting to restore database # " + msiDb)

          var cmd = Seq(pathSrcPgRestore, "-i", "-h", host, "-p", port.toString, "-U", user, "-d", "msi_db_project_" + projectId, "-v", pathDestinationProject + "\\msi_db_project_" + projectId + ".sql")
          logger.info("cmd # " + cmd)
          execute(cmd)
        } else {
          logger.error("the file msi_db_project_" + projectId + ".sql does not exist !")
        }
        if (new File(pathDestinationProject.getCanonicalPath(), "\\lcms_db_project_" + projectId + ".sql").exists()) {
          //pg_restore lcms_Db
          logger.info("Starting to restore database # " + lcmsDb)
          var cmd = Seq(pathSrcPgRestore, "-i", "-h", host, "-p", port.toString, "-U", user, "-d", "lcms_db_project_" + projectId, "-v", pathDestinationProject + "\\lcms_db_project_" + projectId + ".sql")
          execute(cmd)
        } else {
          logger.error("the file lcms_db_project_" + projectId + ".sql does not exist !")
        }
        if (udsDb == true) {
          if (new File(pathDestinationProject.getCanonicalPath(), "\\uds_db.sql").exists()) {
            //pg_restore uds_db schema
            logger.info("Starting to restore schema only # uds_db")
            var cmd = Seq(pathSrcPgRestore, "-i", "-h", host, "-p", port.toString, "-U", user, "-d", "uds_db", "-v", pathDestinationProject + "\\uds_db_data")
            execute(cmd)
          } else {
            logger.error("the file uds_db.sql does not exist !")
          }
        }
      } else {
        logger.error("the directory project_" + projectId + " does not exist !")
      }
    } catch {
      case e: Exception => logger.error("error to execute cmd", e)
    }
  }
  def show(x: Option[String]) = x match {
    case Some(s) => s
    case None => ""
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

object RestoreProject {
  def apply(dsConnectorFactory: IDataStoreConnectorFactory, projectId: Long, userId: Long, pathSource: String, pathDestination: String): Unit = {
    new RestoreProject(dsConnectorFactory, projectId, userId, pathSource, pathDestination).doWork()
  }
}
