package fr.proline.admin.utils

import java.io.{InputStream,File,FileInputStream}

/**
 * @author David Bouyssie
 *
 */

package object resources {
  
  /*def pathToFileOrResourceToFile( path: String, resClass: Class[_] ): File = {
    var file = new File(path)
    if( file.exists() == false) {
      val resource = resClass.getResource(path)
      if( resource != null ) {
        file = new File( resource.toURI() )
      }
    }
    
    file
  }*/
  
  def pathToStreamOrResourceToStream( path: String, resClass: Class[_] ): InputStream = {
    var file = new File(path)
    
    if( file.exists == true) new FileInputStream(file.getAbsolutePath())
    else resClass.getResourceAsStream(path)
  }
  
}