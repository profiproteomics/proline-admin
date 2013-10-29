package fr.proline.admin

import com.beust.jcommander.{JCommander, MissingCommandException, Parameter, ParameterException, Parameters}
import com.weiglewilczek.slf4s.Logging

import fr.proline.admin.service.db.SetupProline
import fr.proline.admin.service.db.maintenance.DumpDatabase
import fr.proline.admin.service.user.{CreateProject, CreateUser}
import fr.proline.core.orm.util.{DataStoreConnectorFactory, DataStoreUpgrader}
import fr.proline.util.ThreadLogger

object RunCommand extends App with Logging {

  trait JCommandReflection {
    lazy private val _parametersAnnotation = this.getClass().getAnnotation(classOf[Parameters])

    object Parameters {
      lazy val names = _parametersAnnotation.commandNames()
      lazy val firstName = names(0)
      lazy val description = _parametersAnnotation.commandDescription()
    }
  }

  @Parameters(commandNames = Array("setup"), commandDescription = "Set Up the Proline databases", separators = "=")
  private object SetupProlineCommand extends JCommandReflection

  @Parameters(commandNames = Array("create_project"), commandDescription = "Create new project", separators = "=")
  private object CreateProjectCommand extends JCommandReflection {

    @Parameter(names = Array("--owner_id", "-oid"), description = "The user account id of the project owner", required = true)
    var ownerId: Int = 0

    @Parameter(names = Array("--name", "-n"), description = "The project name", required = true)
    var projectName: String = ""

    @Parameter(names = Array("--description", "-desc"), description = "The project description", required = false)
    var projectDescription: String = ""
  }

  @Parameters(commandNames = Array("create_user"), commandDescription = "Create new user account", separators = "=")
  private object CreateUserCommand extends JCommandReflection {
    @Parameter(names = Array("--login", "-l"), description = "The user account login", required = true)
    var userLogin: String = ""
  }

  @Parameters(commandNames = Array("dump_msi_db"), commandDescription = "Dump MSIdb content into an XML file", separators = "=")
  private object DumpMsiDbCommand extends JCommandReflection {

    @Parameter(names = Array("--project_id", "-p"), description = "The id of the project corresponding to this MSIdb", required = true)
    var projectId: Int = 0

    @Parameter(names = Array("--file_path", "-f"), description = "The path of the XML file to be generated", required = true)
    var filePath: String = ""
  }

  @Parameters(commandNames = Array("dump_ps_db"), commandDescription = "Dump PSdb content into an XML file", separators = "=")
  private object DumpPsDbCommand extends JCommandReflection {
    @Parameter(names = Array("--file_path", "-f"), description = "The path of the XML file to be generated", required = true)
    var filePath: String = ""
  }

  @Parameters(commandNames = Array("dump_uds_db"), commandDescription = "Dump UDSdb content into an XML file", separators = "=")
  private object DumpUdsDbCommand extends JCommandReflection {
    @Parameter(names = Array("--file_path", "-f"), description = "The path of the XML file to be generated", required = true)
    var filePath: String = ""
  }
  
  @Parameters(commandNames = Array("upgrade_dbs"), commandDescription = "Upgrade all databases to the latest format", separators = "=")
  private object UpgradeDatabasesCommand extends JCommandReflection

  var hasDsConnectorFactory = false
  lazy val dsConnectorFactory: DataStoreConnectorFactory = {

    // Instantiate a database manager
    val dsConnectorFactory = DataStoreConnectorFactory.getInstance()
    if (dsConnectorFactory.isInitialized == false) {
      dsConnectorFactory.initialize(SetupProline.config.udsDBConfig.toNewConnector)
    }

    hasDsConnectorFactory = true

    dsConnectorFactory
  }

  override def main(args: Array[String]): Unit = {
    Thread.currentThread.setUncaughtExceptionHandler(new ThreadLogger(logger.name))

    // Instantiate a JCommander object and affect some commands
    val jCmd = new JCommander()
    jCmd.addCommand(SetupProlineCommand)
    jCmd.addCommand(CreateProjectCommand)
    jCmd.addCommand(CreateUserCommand)
    jCmd.addCommand(DumpMsiDbCommand)
    jCmd.addCommand(DumpPsDbCommand)
    jCmd.addCommand(DumpUdsDbCommand)
    jCmd.addCommand(UpgradeDatabasesCommand)

    // Try to parse the command line
    var parsedCommand = ""
    try {
      jCmd.parse(args: _*)

      parsedCommand = jCmd.getParsedCommand()
      println("Running '" + parsedCommand + "' command...")

      // Execute parsed command
      parsedCommand match {
        case SetupProlineCommand.Parameters.firstName => {
          // Run the setup proline service
          SetupProline()
        }
        case CreateProjectCommand.Parameters.firstName => {

          import fr.proline.admin.service.user.CreateProject

          val projectId = CreateProject(
            CreateProjectCommand.projectName,
            CreateProjectCommand.projectDescription,
            CreateProjectCommand.ownerId
          )

          this.logger.info("project with id='" + projectId + "' has been created !")

        }
        case CreateUserCommand.Parameters.firstName => {
          import fr.proline.admin.service.user.CreateUser
          CreateUser(CreateUserCommand.userLogin)
        }
        case DumpMsiDbCommand.Parameters.firstName => {
          import fr.proline.admin.service.db.maintenance.DumpDatabase
          val msiDbConnector = dsConnectorFactory.getMsiDbConnector(DumpMsiDbCommand.projectId)
          DumpDatabase(msiDbConnector, DumpMsiDbCommand.filePath)
        }
        case DumpPsDbCommand.Parameters.firstName => {
          import fr.proline.admin.service.db.maintenance.DumpDatabase
          DumpDatabase(dsConnectorFactory.getPsDbConnector, DumpPsDbCommand.filePath)
        }
        case DumpUdsDbCommand.Parameters.firstName => {
          import fr.proline.admin.service.db.maintenance.DumpDatabase
          DumpDatabase(dsConnectorFactory.getUdsDbConnector, DumpUdsDbCommand.filePath)
        }
        case UpgradeDatabasesCommand.Parameters.firstName => {
          if (dsConnectorFactory.isInitialized) {
            logger.debug("Upgrading all Proline Databases...")
            
            if( DataStoreUpgrader.upgradeAllDatabases(dsConnectorFactory) ) {
              logger.info("Databases successfully upgraded !")
            } else {
              logger.error("Databases upgrade failed !")
            }
            
          } else {
            logger.error("Unable to initialize DataStoreConnectorFactory instance")
          }
        }
        case _ => {
          throw new MissingCommandException("unknown command '" + jCmd.getParsedCommand() + "'")
        }
      }

    } catch {

      case pEx: ParameterException => {
        println()
        logger.warn("Invalid command or parameter", pEx)
        jCmd.usage()
      }

      case ex: Exception => {
        println()
        logger.error("Execution of command '" + parsedCommand + "' failed", ex)
      }

    } finally {
      if (hasDsConnectorFactory) dsConnectorFactory.closeAll()
    }

  }

}