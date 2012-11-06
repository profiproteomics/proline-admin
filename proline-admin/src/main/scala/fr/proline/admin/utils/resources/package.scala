package fr.proline.admin.utils

import java.io.File

/**
 * @author David Bouyssie
 *
 */

package object resources {
  
  def pathToFileOrResourceToFile( path: String, resClass: Class[_] ): File = {
    var file = new File(path)
    if( file.exists() == false) {
      val resource = resClass.getResource(path)
      if( resource != null ) {
        file = new File( resource.toURI() )
      }
    }
    
    file
  }
  
}