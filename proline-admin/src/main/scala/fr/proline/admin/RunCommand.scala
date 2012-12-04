package fr.proline.admin

import java.io.File
import com.weiglewilczek.slf4s.Logging
import com.beust.jcommander.{JCommander,MissingCommandException,Parameter,ParameterException,Parameters}
import collection.JavaConversions._

import fr.proline.admin.service.db.SetupProline
import fr.proline.core.dal.DatabaseManagement


object RunCommand extends App with Logging {
  
  trait JCommandReflection {
    lazy private val _parametersAnnotation = this.getClass().getAnnotation( classOf[Parameters] )
    
    object Parameters {
      lazy val names = _parametersAnnotation.commandNames()
      lazy val firstName = names(0)
      lazy val description = _parametersAnnotation.commandDescription()
    }
  }
  
  @Parameters(commandNames = Array("setup"), commandDescription = "Set Up the Proline databases", separators = "=" )
  private object SetupProlineCommand extends JCommandReflection
  
  @Parameters(commandNames = Array("create_project"), commandDescription = "Create new project", separators = "=" )
  private object CreateProjectCommand extends JCommandReflection {
    
    @Parameter(names = Array("--owner_id","-oid"), description = "The user account id of the project owner", required = true )
    var ownerId: Int = 0
    
    @Parameter(names = Array("--name","-n"), description = "The project name", required = true )
    var projectName: String = ""
    
    @Parameter(names = Array("--description","-desc"), description = "The project description", required = false )
    var projectDescription: String = ""
  }
  
  @Parameters(commandNames = Array("create_user"), commandDescription = "Create new user account", separators = "=" )
  private object CreateUserCommand extends JCommandReflection {    
    @Parameter(names = Array("--login","-l"), description = "The user account login", required = true )
    var userLogin: String = ""
  }
  
  override def main(args: Array[String]): Unit = {
    
    // Instantiate a JCommander object and affect some commands
    val jCmd = new JCommander()  
    jCmd.addCommand( SetupProlineCommand )
    jCmd.addCommand( CreateProjectCommand )
    jCmd.addCommand( CreateUserCommand )
    
    // Try to parse the command line
    var parsedCommand = ""
    try {
      jCmd.parse(args: _*)
      
      parsedCommand = jCmd.getParsedCommand()
      println("Running '"+ parsedCommand +"' command...")
      
      // Execute parsed command
      parsedCommand match {
        case SetupProlineCommand.Parameters.firstName => {
          // Run the setup proline service
          SetupProline()
        }
        case CreateProjectCommand.Parameters.firstName => {
          
          import fr.proline.admin.service.db.CreateProjectDBs
          import fr.proline.admin.service.user.CreateProject
          
          // Retrieve Proline configuration
          val prolineConf = SetupProline.parseProlineSetupConfig( SetupProline.appConf )
          
          // Instantiate a database manager
          val udsDBConfig = prolineConf.udsDBConfig
          val udsDbConnector = udsDBConfig.toNewConnector
          
          // Create project
          val projectCreator = new CreateProject(
            udsDbConnector,
            CreateProjectCommand.projectName,
            CreateProjectCommand.projectDescription,
            CreateProjectCommand.ownerId
          )
          projectCreator.run()
          
          // Close the database manager
          udsDbConnector.closeAll()
          
          // Create a new database manager to avoid any conflict
          val udsDbConnector2 = udsDBConfig.toNewConnector
          val dbManager2 = new DatabaseManagement( udsDbConnector2 )
          
          // Create project databases
          new CreateProjectDBs( dbManager2, prolineConf, projectCreator.projectId ).run()          
          
          // Close the database manager
          udsDbConnector2.closeAll()
          dbManager2.closeAll()
          
          this.logger.info("project with id='"+ projectCreator.projectId +"' has been created !")
        }
        case CreateUserCommand.Parameters.firstName => {          
          import fr.proline.admin.service.user.CreateUser
          CreateUser( CreateUserCommand.userLogin )
        }
        case _ => {
          throw new MissingCommandException("unknown command '"+ jCmd.getParsedCommand()+"'" )
        }
      }
      
    } catch {
      case pe: ParameterException => {
        println("Invalid command or parameter:")
        println(pe.getMessage)
        jCmd.usage()
      }
      case e: Throwable => {
        println("Execution of command '"+parsedCommand+"' failed because:")
        println(e.getMessage+"\n")
        println("Error stack trace:")
        println(e.getStackTraceString)
      }
    }
    
  }
}