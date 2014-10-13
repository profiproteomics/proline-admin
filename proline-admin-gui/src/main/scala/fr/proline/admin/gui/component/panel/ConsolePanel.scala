package fr.proline.admin.gui.component.panel

import java.io.OutputStream
import java.io.PrintStream

import scalafx.application.Platform
import scalafx.geometry.Insets
import scalafx.scene.layout.Priority
import scalafx.scene.layout.StackPane
import scalafx.scene.web.WebView

/**
 * Create an area to display the console content.
 */
object ConsolePanel {

  /**
   * ********** *
   * COMPONENTS *
   * ********** *
   */

  /** Webview : console displayer */
  val consoleDisp = new WebView {
    //    margin = Insets(10)
    hgrow = Priority.ALWAYS
    vgrow = Priority.ALWAYS
    //TODO: wrap text, forbid horizontal scrollbar
    //val scrolls = consoleDisp.lookupAll(".scroll-bar")
  }

  /** Provide the webview a grey border */
  val consoleArea = new StackPane {
    content = consoleDisp
    margin = Insets(10)
    style = "-fx-border-color: #C0C0C0; -fx-border-width: 1; -fx-border-radius:2;"
    hgrow = Priority.ALWAYS
    vgrow = Priority.ALWAYS
  }

  /**
   * ******** *
   * FEATURES *
   * ******** *
   */

  /** Redirect application console content in this panel */
  val psOut = new PrintStream(new Console(this.consoleDisp))
  //  val psErr = new PrintStream(new Console(this.consoleDisp, true))
  System.setOut(psOut)
  //  System.setErr(psErr)
  System.setErr(psOut)

  /** Display this panel */
  def apply(): StackPane = consoleArea
}

/**
 * Create a custom console by redirecting Output stream to desired textFlow (here in ConsolePanel).
 */
class Console(
  consoleDisp: WebView,
  isStdErr: Boolean = false) extends OutputStream {

  /** Create HTML content */
  // Provide console style (<kbd>) and auto-scroll to bottom (javaScript)
  private val _htmlHeader = """<html>
<head>
	<script language="javascript" type="text/javascript">
  	function toBottom(){
			window.print("in toBottom()")
    	window.scroll(0, document.body.scrollHeight);
   }
	</script>
</head>
<body onload='toBottom()'>
	<kbd> 
"""

  private val _htmlFooter = """</body></html>"""

  private val _consoleBuffer = new StringBuilder(_htmlHeader)

  /** Add formatted text to webView content */
  private def _addTextToWebView(newLines: String, htmlWebView: WebView = this.consoleDisp) = synchronized {

    _consoleBuffer ++= _formatText(newLines)
    Platform.runLater(consoleDisp.engine.loadContent(_consoleBuffer.result()))
  }

  /** Format HTML text: set color and font */
  private def _formatText(strToFormat: String): String = {

    // Utility
    def _textMatches(test: String): Boolean = strToFormat.toUpperCase() matches s"""(?s).*${test.toUpperCase()}.*"""

    /** Define text color according to text content and/or origin (stdErr, stdOut) */
    val color = {
      if (isStdErr) "red"
      else {

        if (_textMatches("error") || _textMatches("exception") || _textMatches("fail")) "red"

        else if (_textMatches("warn")) "orange"

        else if (_textMatches("success")) "green" // || _textMatches("info")

        else "black"
      }
    }

    /** Make and return HTML string */
    s"""<kbd style='color:$color'>$strToFormat</kdb>"""
  }

  /**
   *  Override OutputStream write methods
   */
  override def write(byte: Int) = {
    _addTextToWebView(byte.toChar.toString())
  }

  override def write(byteArr: Array[Byte]) = {
    val asString = new String(byteArr, "UTF-8")
    val corrString = asString.replaceAll("\n", "<br>") //.replaceAll("\r", "<br>")

    _addTextToWebView(corrString)
  }

  override def write(byteArr: Array[Byte], off: Int, len: Int) = {
    val asString = new String(byteArr, off, len)
    val corrString = asString.replaceAll("\n", "<br>") //.replaceAll("\r", "<br>")

    _addTextToWebView(corrString)
  }
}
