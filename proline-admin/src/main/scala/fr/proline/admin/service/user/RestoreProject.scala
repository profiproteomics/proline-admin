package fr.proline.admin.service.user

import javax.persistence.EntityTransaction
import javax.persistence.FlushModeType
import javax.persistence.EntityManager
import javax.persistence.Query;

import com.typesafe.scalalogging.LazyLogging

import fr.proline.admin.service.ICommandWork
import fr.proline.admin.service.db.migration.UpgradeAllDatabases
import fr.proline.core.orm.util.DataStoreConnectorFactory
import fr.proline.repository.DriverType
import fr.proline.core.orm.uds.ExternalDb
import fr.proline.core.orm.uds.Project
import fr.proline.core.orm.uds.{ UserAccount, ProjectUserAccountMapPK, ProjectUserAccountMap, Dataset, Aggregation, Fractionation, QuantitationMethod }
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
    var dbVersions: Boolean = false
    try {
      // check  databases lcms_db and msi_db exist 
      dataBasesExist = dataBaseExists(udsDbCtx, projectId)
      udsEM.setFlushMode(FlushModeType.COMMIT)
      if (!udsDbCtx.isInTransaction) {
        localUdsTransaction = udsEM.getTransaction()
        localUdsTransaction.begin()
        udsTransacOK = false
      }
      if ((projectId > 0) && (userId > 0) && (pathSource != null) && (!pathSource.isEmpty) && (pathDestination != null) && (!pathDestination.isEmpty)) {

        var project = udsEM.find(classOf[Project], projectId)
        var user = udsEM.find(classOf[UserAccount], userId)
        val udsDbDataFile = new File(pathDestination + "\\project_" + projectId + "\\uds_db_data.tsv")
        var parser = new JsonParser()
        var array: JsonObject = null
        var versions = getVersions(udsDbDataFile)

        // case 1: project exist in uds_db 

        if ((project != null) && (user != null)) {
          val externalDbMsi = ExternalDbRepository.findExternalByTypeAndProject(udsEM, fr.proline.repository.ProlineDatabaseType.MSI, project)
          val externalDbLcms = ExternalDbRepository.findExternalByTypeAndProject(udsEM, fr.proline.repository.ProlineDatabaseType.LCMS, project)
          // parse project properties 
          val properties = project.getSerializedProperties()
          try {
            array = parser.parse(properties).getAsJsonObject()
          } catch {
            case e: Exception =>
              logger.error("error accessing project properties")
              array = parser.parse("{}").getAsJsonObject()
          }

          // case 1.1 : project exist in uds_db ; lcms_db and msi_db exists    

          if (dataBasesExist == true) {
            //compare versions 
            //add two properties: restore_date : date  ; is_active :true 
            setProperties(array)
            project.setSerializedProperties(array.toString())
            udsEM.merge(project)
            if (localUdsTransaction != null) {
              localUdsTransaction.commit()
            }
            logger.info(" project #" + projectId + "  has been restored .")
          } else {

            //case 1.2 : lcms_db and msi_db does not exist 

            if (localUdsTransaction != null) {
              localUdsTransaction.commit()
            }
            //create lcms and msi databases 
            createDataBases(udsDbCtx, projectId)
            //pg_restore databases 
            if ((externalDbMsi.getHost() != null) && (!externalDbMsi.getHost().isEmpty) && (externalDbMsi.getDbUser() != null) && (!externalDbMsi.getDbUser().isEmpty) &&
              (externalDbMsi.getDbName() != null) && (!externalDbMsi.getDbName().isEmpty) && (externalDbLcms.getDbName() != null) && (!externalDbLcms.getDbName().isEmpty) &&
              (externalDbMsi.getPort() > 0)) {
              logger.info(s"Starting to restore msi_db and lcms_db ...")
              execPgRestore(externalDbMsi.getHost(), externalDbMsi.getPort(), externalDbMsi.getDbUser(), externalDbMsi.getDbPassword(), externalDbMsi.getDbName(),
                externalDbLcms.getDbName(), pathDestination, pathSource, projectId, projectId, false)
            }
            if (localUdsTransaction != null) {
              localUdsTransaction.begin()
            }
            setProperties(array)
            project.setSerializedProperties(array.toString())
            udsEM.merge(project)
            if (localUdsTransaction != null) {
              localUdsTransaction.commit()
            }
            logger.info(" project #" + projectId + "  has been restored .")
          }
        } else {

          // case 2: project does not exist in uds_db 

          logger.info("import project #" + projectId + "  in uds_db database")
          //insert data in uds_db 
          val newProjectId = upsertDataUdsEm(udsEM, udsDbDataFile, userId)
          val newProject = udsEM.find(classOf[Project], newProjectId)
          dataBasesExist = dataBaseExists(udsDbCtx, newProjectId)
          val properties = newProject.getSerializedProperties()
          try {
            array = parser.parse(properties).getAsJsonObject()
          } catch {
            case e: Exception =>
              array = parser.parse("{}").getAsJsonObject()
          }
          if (localUdsTransaction != null) {
            localUdsTransaction.commit()
          }

          // create lcms_db and msi_db databases 

          createDataBases(udsDbCtx, newProjectId)

          //pg_restore databases 

          if ((!show(versions.get("host")).isEmpty) && (!show(versions.get("user")).isEmpty) && (show(versions.get("port")).toInt > 0)) {
            execPgRestore(show(versions.get("host")).toLowerCase(), show(versions.get("port")).toInt, show(versions.get("user")), show(versions.get("pw")), show(versions.get("msiName")),
              show(versions.get("lcmsName")), pathDestination, pathSource, projectId, newProjectId, false)
          }

          //case 1.2 : lcms_db and msi_db does not exist 

          //update properties for the new project
          if (localUdsTransaction != null) {
            localUdsTransaction.begin()
          }
          setProperties(array)
          newProject.setSerializedProperties(array.toString())
          udsEM.merge(newProject)
          if (localUdsTransaction != null) {
            localUdsTransaction.commit()
          }
          logger.info(" project with id #" + projectId + "  has been restored with a new project id #" + newProjectId + " .")

        }
      } else {
        logger.error("Some parameters are missing for the project #" + projectId + " check  project id ,source path, destination path or owner id values !")
      }

    } finally {
      udsEM.setFlushMode(FlushModeType.AUTO)
      udsDbCtx.close()
      udsDbConnector.close()
    }
  }

  // read tsv file and get versions of dataBases 

  def getVersions(csvFile: File): Map[String, String] = {
    var versions: Map[String, String] = Map()
    val reader = new FileReader(csvFile)
    val csvFileParser = new CSVParser(reader, CSVFormat.MYSQL.withNullString("null"))
    try {
      var csvRecords = csvFileParser.getRecords()
      if ((csvRecords != null) && (!csvRecords.isEmpty)) {

        csvRecords.foreach { rec =>
          val record = rec.get(0)

          if ((record != null) && (!record.isEmpty) && (record.split("#")(0) == "external_db")) {

            versions += ("host" -> record.split("#")(6))
            versions += ("port" -> record.split("#")(7))
            versions += ("user" -> record.split("#")(4))
            versions += ("pw" -> record.split("#")(5))
            if ((record != null) && (!record.isEmpty) && (record.split("#")(8) == "MSI")) {
              versions += ("msi" -> record.split("#")(9))
              versions += ("msiName" -> record.split("#")(2))
            }
            if ((record != null) && (!record.isEmpty) && (record.split("#")(8) == "LCMS")) {
              versions += ("lcms" -> record.split("#")(9))
              versions += ("lcmsName" -> record.split("#")(2))
            }
          }

        }
      }
    } catch {
      case e: NumberFormatException => logger.error(" can't convert database version !")
    } finally {
      csvFileParser.close()
    }
    return versions
  }

  // import data from csv file and insert database  

  def upsertDataUdsEm(udsEM: EntityManager, csvFile: File, userId: Long): Long = {
    var dataSetParentIds: Map[Dataset, Long] = Map()
    var dataSetOldNewIds: Map[Long, Dataset] = Map()
    var newProjectId: Long = 0L
    val reader = new FileReader(csvFile)
    val csvFileParser = new CSVParser(reader, CSVFormat.MYSQL.withNullString("null"))
    try {
      val user = udsEM.find(classOf[UserAccount], userId)
      require(user != null, "undefined user with id=" + userId)
      val udsProject = new Project(user)
      var csvRecords = csvFileParser.getRecords()
      if ((csvRecords != null) && (!csvRecords.isEmpty)) {

        csvRecords.foreach { rec =>
          val record = rec.get(0)

          // insert data in table project 

          if ((record != null) && (!record.isEmpty) && (record.split("#")(0) == "project")) {
            val projectId = record.split("#")(1).toLong
            val name = record.split("#")(2)
            val description = record.split("#")(3)
            val creationTimestamp = java.sql.Timestamp.valueOf(record.split("#")(4))
            var srProperties = record.split("#")(5)
            val ownerId = record.split("#")(6).toLong
            val lockExpirationTimestamp = record.split("#")(7)
            val lockUserId = record.split("#")(8)
            val query = udsEM.createQuery("Select p from Project p where p.owner.id=:id and p.name=:name")
            query.setParameter("id", ownerId)
            query.setParameter("name", name)
            val projectList = query.getResultList().toList
            if (!projectList.isEmpty) {
              logger.error("the owner has a project with the same name ! ")
            }
            if ((projectId > 0) && (name != null) && (!name.isEmpty) && (creationTimestamp != null) && (ownerId > 0)) {
              try {
                udsProject.setName(name)
                udsProject.setDescription(description)
                udsProject.setCreationTimestamp(creationTimestamp)
                udsProject.setSerializedProperties(srProperties)
                udsEM.persist(udsProject)
                newProjectId = udsProject.getId()
              } catch {
                case t: Throwable => logger.error("Error while insering data into table uds_db.project", t)
              }
            } else {
              logger.error(" trying to insert null in table uds_db.project ")
            }
          }

          //insert data in table external_db

          if ((record != null) && (!record.isEmpty) && (record.split("#")(0) == "external_db")) {
            val externalDbId = record.split("#")(1).toLong
            var name = ""
            if (record.split("#")(8) == "MSI") {
              name = "msi_db_project_" + newProjectId
            }
            if (record.split("#")(8) == "LCMS") {
              name = "lcms_db_project_" + newProjectId
            }
            val connectionMode = record.split("#")(3)
            val userName = record.split("#")(4)
            val ps = record.split("#")(5)
            val host = record.split("#")(6)
            val port = Integer.valueOf((record.split("#")(7)))
            val dbType = record.split("#")(8)
            val version = record.split("#")(9)
            val isBusy = (record.split("#")(10)).toBoolean
            val srProperties = record.split("#")(11)
            if ((externalDbId > 0) && (name != null) && (!name.isEmpty) && (connectionMode != null) && (!connectionMode.isEmpty)
              && (dbType != null) && (!dbType.isEmpty) && (version != null) && (!version.isEmpty)) {
              try {
                val udsExternalDb = new ExternalDb()
                udsExternalDb.setDbName(name)
                udsExternalDb.setDbPassword(ps)
                udsExternalDb.setDbUser(userName)
                udsExternalDb.setDbVersion(version)
                udsExternalDb.setHost(host)
                udsExternalDb.setPort(port)
                udsExternalDb.setIsBusy(isBusy)
                udsExternalDb.setSerializedProperties(srProperties)
                udsExternalDb.setDriverType(DriverType.POSTGRESQL)
                udsExternalDb.setType(ProlineDatabaseType.valueOf(dbType))
                udsExternalDb.setConnectionMode(ConnectionMode.valueOf(connectionMode))
                udsExternalDb.addProject(udsProject)
                udsEM.persist(udsExternalDb)
                udsProject.addExternalDatabase(udsExternalDb)

              } catch {
                case t: Throwable => logger.error("Error while insering data into table uds_db.external_db", t)
              }
            } else {
              logger.error(" trying to insert null in table uds_db.external_db")
            }
          }

          // insert data in DataSet

          if ((record != null) && (!record.isEmpty) && (record.split("#")(0) == "data_set")) {
            val id = record.split("#")(1).toLong
            val number = record.split("#")(2).toInt
            val name = record.split("#")(3)
            val description = record.split("#")(4)
            val dbType = record.split("#")(5)
            val keyWords = record.split("#")(6)
            val creationTimestamp = java.sql.Timestamp.valueOf(record.split("#")(7))
            val modificationLog = record.split("#")(8)
            val childrenCount = record.split("#")(9).toInt
            val serProperties = record.split("#")(10)
            var resultSetId = record.split("#")(11).toLong
            var resultSummaryId = record.split("#")(12).toLong
            var aggregationId = record.split("#")(13).toLong
            var quantMethodId = record.split("#")(15).toLong
            var parentDatasetId = record.split("#")(16).toLong
            var fractionationId = record.split("#")(14).toLong
            val projectId = record.split("#")(17)

            if ((id > 0) && (name != null) && (!name.isEmpty) && (dbType != null) && (!dbType.isEmpty) && (projectId != null)) {

              try {
                val aggregation = udsEM.find(classOf[Aggregation], aggregationId)
                val fraction = udsEM.find(classOf[Fractionation], fractionationId)
                val quantitationMethod = udsEM.find(classOf[QuantitationMethod], quantMethodId)
                val udsDataset = new Dataset(udsProject)
                udsDataset.setChildrenCount(childrenCount)
                udsDataset.setCreationTimestamp(creationTimestamp)
                udsDataset.setDescription(description)
                udsDataset.setType(Dataset.DatasetType.valueOf(dbType))
                if (resultSetId > 0)
                  udsDataset.setResultSetId(resultSetId) else udsDataset.setResultSetId(null)
                if (resultSummaryId > 0)
                  udsDataset.setResultSummaryId(resultSummaryId) else udsDataset.setResultSummaryId(null)
                udsDataset.setKeywords(keyWords)
                udsDataset.setModificationLog(modificationLog)
                udsDataset.setName(name)
                udsDataset.setNumber(number)
                udsDataset.setProject(udsProject)
                udsDataset.setSerializedProperties(serProperties)
                udsDataset.setFractionation(fraction)
                udsDataset.setAggregation(aggregation)
                udsDataset.setMethod(quantitationMethod)
                if (parentDatasetId > 0) {
                  dataSetParentIds += (udsDataset -> parentDatasetId)
                } else {
                  //dataSet root or trash 
                  udsDataset.setParentDataset(null)
                }
                udsEM.persist(udsDataset)
                dataSetOldNewIds += (id -> udsDataset)

              } catch {
                case t: Throwable => logger.error("Error while insering data into table uds_db.data_set", t)
              }
            } else {
              logger.error(" trying to insert null in table uds_db.data_set ")
            }
          }
        }
      }
    } catch {
      case ex: IOException => logger.error("Had an IOException trying to read csv file")
    } finally {
      csvFileParser.close()
    }

    //update relations between datasets and their parents 

    if (!dataSetParentIds.isEmpty) {
      for ((k, v) <- dataSetParentIds) {
        var dataset = k
        dataset.setParentDataset(getDataset(dataSetOldNewIds.get(v)))
      }
    }
    return newProjectId
  }
  //check if databases exists
  def dataBaseExists(udsContext: UdsDbConnectionContext, projectId: Long): Boolean = {
    var dataBasesExist = false
    try {

      var nbMsi = 0
      var nbLcms = 0
      DoJDBCWork.withEzDBC(udsContext) { ezDBC =>
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
    return dataBasesExist
  }

  // create lcms and msi databases 

  def createDataBases(udsContext: UdsDbConnectionContext, projectId: Long) {
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

  // rename databases if its exists 

  def renameDataBases(udsContext: UdsDbConnectionContext, projectId: Long, newProjectId: Long) {
    logger.debug("renaming MSI and LCMS databases ... ")
    try {
      DoJDBCWork.withEzDBC(udsContext) { ezDBC =>
        ezDBC.execute("ALTER DATABASE msi_db_project_" + projectId + " RENAME TO msi_db_project_" + newProjectId)
        ezDBC.execute("ALTER DATABASE lcms_db_project_" + projectId + " RENAME TO lcms_db_project_" + newProjectId)
      }
    } catch {
      case t: Throwable => logger.error("Error while renaming  MSI and LCMS databases", t)
    }
  }

  // execute command as sequence of string 

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

  //pg_restore databases 
  def execPgRestore(host: String, port: Integer, user: String, passWord: String, msiDb: String, lcmsDb: String, pathDestination: String, pathSource: String, projectId: Long, newProjectId: Long, udsDb: Boolean) {

    val pathDestinationProject = new File(pathDestination + "\\project_" + projectId)
    val pathSrcPgRestore = new File(pathSource, "\\pg_restore").getCanonicalPath()
    try {
      if (pathDestinationProject.exists()) {

        if (new File(pathDestinationProject.getCanonicalPath(), "\\msi_db_project_" + projectId + ".bak").exists()) {
          //pg_restore msi_db
          logger.info("Starting to restore database # " + msiDb)

          var cmd = Seq(pathSrcPgRestore, "-i", "-h", host, "-p", port.toString, "-U", user, "-d", "msi_db_project_" + newProjectId, "-v", pathDestinationProject + "\\msi_db_project_" + projectId + ".bak")
          logger.info("cmd # " + cmd)
          execute(cmd)
        } else {
          logger.error("the file msi_db_project_" + projectId + ".bak does not exist !")
        }
        if (new File(pathDestinationProject.getCanonicalPath(), "\\lcms_db_project_" + projectId + ".bak").exists()) {
          //pg_restore lcms_Db
          logger.info("Starting to restore database # " + lcmsDb)
          var cmd = Seq(pathSrcPgRestore, "-i", "-h", host, "-p", port.toString, "-U", user, "-d", "lcms_db_project_" + newProjectId, "-v", pathDestinationProject + "\\lcms_db_project_" + projectId + ".bak")
          execute(cmd)
        } else {
          logger.error("the file lcms_db_project_" + projectId + ".bak does not exist !")
        }
        if (udsDb == true) {
          if (new File(pathDestinationProject.getCanonicalPath(), "\\uds_db_schema.bak").exists()) {
            //pg_restore uds_db schema
            logger.info("Starting to restore schema only # uds_db")
            var cmd = Seq(pathSrcPgRestore, "-i", "-h", host, "-p", port.toString, "-U", user, "-d", "uds_db", "-v", pathDestinationProject + "\\uds_db_schema.bak")
            execute(cmd)
          } else {
            logger.error("the file uds_db_schema.sql does not exist !")
          }
        }
      } else {
        logger.error("the directory project_" + projectId + " does not exist !")
      }
    } catch {
      case e: Exception => logger.error("error to execute cmd", e)
    }
  }
  // update properties
  def setProperties(array: JsonObject) {
    val sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    array.addProperty("restore_date", sdf.format(new Date()).toString())
    array.addProperty("is_active", true)
  }

  def show(x: Option[String]) = x match {
    case Some(s) => s
    case None => ""
  }

  def getDataset(x: Option[Dataset]) = x match {
    case Some(s) => s
    case None => null
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
