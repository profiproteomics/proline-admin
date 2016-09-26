package fr.proline.admin.postgres.install
import java.io.IOException
import java.io._
import java.io.StringWriter
import scala.util.control.Breaks._
/** 
 * check if software isn installed 
 **/
class CheckRegistry {
  
  def readRegistry(location:String,key:String,command:String):Boolean={
  var registryExist:Boolean=false
    try{
      var process:Process=Runtime.getRuntime().exec(command+" "+ location)
      val reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
      var line : String ="";
       try{
         while ((line = reader.readLine()) != null) {
           if(line.indexOf(key)>=0){
             registryExist=true
            }
           }
        }
        catch{
             case e: IOException => e.printStackTrace()
         }        
      }catch{
        case e: Exception => 
      }
    return registryExist 
  }
}