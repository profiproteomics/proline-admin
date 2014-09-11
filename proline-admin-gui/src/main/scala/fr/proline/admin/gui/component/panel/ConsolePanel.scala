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
  val psErr = new PrintStream(new Console(this.consoleDisp, true))
  System.setOut(psOut)
  //  System.setErr(psErr) //TODO: redirect sys err (not for debug modeà

  /** Display this panel */
  //  def apply(): WebView = consoleDisp
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

  //    	window.scrollTo(0, document.body.scrollHeight);

  private val _consoleBuffer = new StringBuilder(_htmlHeader)
  //  private val _consoleBuffer = new StringBuilder(_htmlHeader + _htmlFooter)

  /** Add formatted text to webView content */
  private def _addTextToWebView(newLines: String, htmlWebView: WebView = this.consoleDisp) = {

    _consoleBuffer ++= _formatText(newLines)
    synchronized { consoleDisp.engine.loadContent(_consoleBuffer.result()) }
  }

  /** Format HTML text: set color and font */
  private def _formatText(strToFormat: String): String = {

    /** Utility */
    def _textMatches(test: String): Boolean = strToFormat.toUpperCase() matches s"""(?s).*${test.toUpperCase()}.*"""
    //    def _textMatches(str: String): Boolean = text.text().toUpperCase() matches s"""(?s).*${str.toUpperCase()}.*"""

    /** Define text color/weight according to text content and/or origin (stdErr, stdOut) */
    //    val (bold, endBold) = {
    //      if (_textMatches("> run_cmd") || (_textMatches("""\[.+success\]""")) || (_textMatches("""\[.+finished with error\]"""))) ("<b>", "</b>")
    //      else ("", "")
    //    }

    val color = {
      if (isStdErr) "orange" //"red"
      else {

        if (_textMatches("error") || _textMatches("exception") || _textMatches("fail")) "red" // || _textMatches("warn"))

        else if (_textMatches("info") || _textMatches("success")) "green"

        else "black"
      }
    }

    /** Make and return HTML string */
    //    s"""<kbd style='color:$color'>$bold$strToFormat$endBold</kdb><br>"""
    s"""<kbd style='color:$color'>$strToFormat</kdb>""" //<br>"""
  }

  /** Override OutputStream write method */
  override def write(byte: Int) = synchronized {
    _addTextToWebView(byte.toChar.toString())
    this.flush()

  }

  override def write(byteArr: Array[Byte]) = synchronized {
    val asString = new String(byteArr, "UTF-8")
    val corrString = asString.replaceAll("\n", "<br>") //.replaceAll("\r", "<br>")

    Platform.runLater {
      _addTextToWebView(corrString) //TODO: scroll
    }
    this.flush()
  }

  override def write(byteArr: Array[Byte], off: Int, len: Int) = synchronized {
    val asString = new String(byteArr, off, len)
    val corrString = asString.replaceAll("\n", "<br>") //.replaceAll("\r", "<br>")

    Platform.runLater {
      _addTextToWebView(corrString) //TODO: scroll
    }

    this.flush()
  }
}
