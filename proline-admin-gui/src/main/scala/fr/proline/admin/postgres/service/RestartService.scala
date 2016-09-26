package fr.proline.admin.postgres.service
import com.sun.security.auth._
import java.io.IOException
import java.io._
import java.io.StringWriter
import fr.proline.admin.postgres.install._
import java.util.regex.Matcher
import java.util.regex.Pattern
import fr.proline.admin.gui.util.ShowPopupWindow

/**
 *  search a service postgreSQL and try restart it 
 *  
 */ 

object AllDone extends Exception {
}
object RestartService {
  
  /*check if user is admin in windows */
  
  def isAdminInWindows():Boolean={
    var isAdmin:Boolean=false
    if(CheckInstalledPostgres.isWindows()){
     val groups:Array[String]=(new com.sun.security.auth.module.NTSystem()).getGroupIDs()
     for(group<- groups){
       if(group.equals("S-1-5-32-544")){
       isAdmin= true 
       }
     }
    }
     return isAdmin
  }
  /*check id user is admin in unix */
  
  def isAdminInUnix():Boolean={
    return true 
  }
  /* get service name with different PostgreSQL versions */
  def getServiceNameInWindows(serviceName:String):String={
     var name:String=""
     var process:Process=Runtime.getRuntime().exec("cmd.exe /c sc query state= all")
     val reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
     try{ 
       var line : String ="";
       while (((line = reader.readLine()) != null)&&(!line.equals(""))) {
         if(line.indexOf(serviceName)>=0){
           val pattern="(postgres.*(\\S+))"
           val r= Pattern.compile(pattern);
           val m:Matcher = r.matcher(line);
           if(m.find()) name=m.group()
             throw AllDone
           }
         }
        reader.close()
       }
       catch{
              case AllDone => 
       }      
   return name
  }
  
  /* Restart service:stop and start   */
  
  def restartServiceInWindows(serviceName:String):Unit={
    
   val serviceStop="cmd.exe /c sc stop "+serviceName
   val serviceStart="cmd.exe /c sc start "+serviceName
  
   
    try{ 
       var process:Process=Runtime.getRuntime().exec(serviceStop)
       process.waitFor()
       process==Runtime.getRuntime().exec(serviceStart)
     
//        ShowPopupWindow(
//          wTitle = "Service PostgreSQL ",
//          wText = ""+message+" ! "
//       )
    }
    catch{
       case e: Exception => e.printStackTrace
    }
  }
}