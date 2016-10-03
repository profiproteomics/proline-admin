package fr.proline.admin.postgres.install
import com.typesafe.scalalogging.LazyLogging
import scala.collection.mutable.ArrayBuffer
import fr.proline.admin.postgres.install._
import fr.proline.admin.gui.util.ShowPopupWindow
import fr.proline.admin.gui.QuickStart

/**
 * check version of os and check if postgresq is installed  
 *
 **/

object CheckInstalledPostgres {
  
  private  var  OS:String=System.getProperty("os.name").toLowerCase()
  private  var command:String=_
  
  /* check postgresql and alert if not installed  */
  
  val checkregistry=new CheckRegistry()
   def checkPostgres():Unit={
    if(isWindows()){
     
     if(!checkregistry.readRegistry("HKEY_LOCAL_MACHINE\\SOFTWARE\\","PostgreSQL","reg query")){
     ShowPopupWindow(
          wTitle = "Software PostgreSQL",
          wText = "Check if PostgreSQL is installed , you should  have Administrateur rights ! "
        )
     }
    }
    
    if(isUnix()){
      
      if(!checkregistry.readRegistry("psql","psql","which")){
      ShowPopupWindow(
          wTitle = "Software PostgreSQL",
          wText = "Check if PostgreSQL is installed ! "
       )
     } 
    }
  }
  
  /* get the operating system: Windows or Unix */
  
  def isWindows():Boolean={
    return (OS.indexOf("win") >= 0)
  }
  def isMac():Boolean={
    return (OS.indexOf("mac") >= 0)
  }
  def isUnix():Boolean={
    return (OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") > 0 )
  }
  def isSolaris():Boolean={
    return (OS.indexOf("sunos") >= 0);
  }
 /* warning message */

 def showPopUp():Unit={
   
 }

}
