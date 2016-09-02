package fr.proline.admin.gui.component.configuration.form

import com.typesafe.scalalogging.LazyLogging

import scala.collection.mutable.ArrayBuffer

import scalafx.Includes._
import scalafx.collections.ObservableBuffer
import scalafx.geometry.Insets
import scalafx.geometry.Pos
import scalafx.scene.Cursor
import scalafx.scene.Cursor.sfxCursor2jfx
import scalafx.scene.control.Button
import scalafx.scene.control.CheckBox
import scalafx.scene.control.ComboBox
import scalafx.scene.control.Hyperlink
import scalafx.scene.control.Label
import javafx.scene.control.Alert
import javafx.scene.control.Alert._
import javafx.scene.control.Alert.AlertType
import scalafx.scene.control.PasswordField
import scalafx.scene.control.TextField
import scalafx.scene.layout.HBox
import scalafx.scene.layout.Priority
import scalafx.scene.layout.StackPane
import scalafx.scene.layout.VBox
import scalafx.stage.Screen
import scalafx.stage.Stage

import fr.proline.admin.gui.IconResource
import fr.proline.admin.gui.Main
import fr.proline.admin.gui.process.DatabaseConnection
import fr.proline.admin.gui.process.ProlineAdminConnection
import fr.proline.admin.gui.process.config._
import fr.proline.admin.gui.util.FxUtils
import fr.proline.admin.gui.util.GetConfirmation
import fr.proline.repository.DriverType
import fr.proline.admin.gui.QuickStart

import fr.profi.util.scala.ScalaUtils.doubleBackSlashes
import fr.profi.util.scala.ScalaUtils.isEmpty
import fr.profi.util.scala.ScalaUtils.stringOpt2string
import fr.profi.util.scalafx.BoldLabel
import fr.profi.util.scalafx.FileBrowsing
import fr.profi.util.scalafx.NumericTextField
import fr.profi.util.scalafx.ScalaFxUtils
import fr.profi.util.scalafx.ScalaFxUtils._
import fr.profi.util.scalafx.TitledBorderPane
import fr.profi.util.scala.ScalaUtils

import java.net.InetAddress;
import java.net.UnknownHostException;

import fr.proline.repository.DriverType
import javafx.scene.control.Tooltip
/**
 * Step 2 : a window to edit database configuration's file .
 */
class DatabaseConfig extends VBox with LazyLogging {

	/*
	 * ********** *
	 * COMPONENTS *
	 * ********** *
	 */
  private var  hostname:String = ""
	private var  addr:InetAddress =null
	private var  username:String=""
	private val driver = DriverType.POSTGRESQL
	/* DB connection */
	
	val userNameLabel = new Label("User name :")
	val userNameField = new TextField{  
	  text.onChange{(_,oldText,newText)=>
	  updateUSername(newText)    
	  }
  }
	username="postgres"
	userNameField.setTooltip(new Tooltip("enter the username of your database."))
  if((username==null)||(username.equals(""))){
	  username=System.getProperty("user.name")
  }
  userNameField.setPromptText(username)
  val pwdLabel = new Label("Password :")
  val showPwdBox = new CheckBox("Show password") {
	selected = false
			vgrow = Priority.Always
			minWidth = 112
			maxWidth = 112
  }
  val passwordPWDField = new PasswordField() {
	visible <== !showPwdBox.selected
  }
  val passwordTextField = new TextField() {
	  text <==> passwordPWDField.text
			promptText <==> passwordPWDField.promptText
			visible <== !passwordPWDField.visible
			text.onChange{(_,oldText,newText)=>
			updatePassword(newText)
			}
  }
  passwordTextField.setTooltip(new Tooltip("enter the password of your database."))
  passwordTextField.setPromptText("postgres")
  val hostNameLabel = new Label("Host name :")
  val hostNameField = new TextField {
	  text.onChange{(_,oldText,newText)=>
	  updateHost(newText)
	  }
  }
  addr = InetAddress.getLocalHost();
  hostname = addr.getHostName();
  if((hostname==null)||(hostname.equals(""))){
	  hostname="Example : localhost"
  }
  hostNameField.setPromptText(hostname)
  val hostNameWarning = new Label{
	graphic = ScalaFxUtils.newImageView(IconResource.WARNING)
			text = "Don't use the term 'localhost', but the real IP address or fully qualified name of the server."
			wrapText = true
  }
  hostNameField.setTooltip(new Tooltip("enter your hostname."));
  val portLabel = new Label("Port :")
  val portField = new NumericTextField{   
	  text.onChange{
		  (_,oldText,newText)=>
		  if(!newText.equals("")&&(newText!=null)){
			  updatePort(newText.toInt)
		  }
	  }
  }
  portField.setPromptText("5432")
  portField.setTooltip(new Tooltip("enter the port of your database(default:5432)."));
  val testConnectionHyperlink = new Hyperlink("Test connection") {
	  onAction = handle {
	    
		/*test connection database*/ 
		//  DatabaseConnection.testConnectionToPostgres(userNameField,passwordTextField,hostNameField,portField) 
	    DatabaseConnection.testDbConnection(driver,userNameField,passwordTextField,hostNameField,portField,true,true) 
		// dialgoConnectionSuccess()
		//      if(DatabaseConnection.testConnectionToPostgres1(userNameField,passwordTextField,hostNameField,portField)==false){
		//        //connection to database failed
		//        dialgoConnectionFail()
		//      }

	    }
  }

/*
 * ****** *
 * LAYOUT *
 * ****** *
 */

/* Size & Resize properties */
  Seq(
	  	userNameLabel, pwdLabel, hostNameLabel, portLabel
		  ).foreach(_.minWidth = 60)

  Seq(
		userNameField, passwordPWDField, passwordTextField, hostNameField, hostNameWarning, portField
		).foreach { node =>
		node.minWidth = 120
		//node.prefWidth <== parentStage.width
    }

//VBox & HBox spacing
  private val V_SPACING = 10
  private val H_SPACING = 5

  /* DB connection */

  val dbPwdPane = new StackPane {
	  alignmentInParent = Pos.BottomLeft
		content = List(passwordPWDField, passwordTextField)
  }
    val dbConnectionSettings = new TitledBorderPane(
      
    title = "Step 2 : edit database connection", 
   contentNode = new VBox {
	  padding = Insets(5)
	  spacing = V_SPACING
	  alignment = Pos.BaselineRight
	  content = List(
		  new HBox {
			  spacing = H_SPACING
			  content = List(userNameLabel, userNameField)
		  },
	    new HBox {
		    spacing = H_SPACING
		    content = List(pwdLabel,dbPwdPane, showPwdBox)
		  },
		  new HBox {
				spacing = H_SPACING
				content = List(
					hostNameLabel,
					new VBox {
						content = List(hostNameField, hostNameWarning)
					}
				)
	  	},
	   new HBox {
	      spacing = H_SPACING
		    content = List(portLabel, portField)
	    },
	      testConnectionHyperlink
      )
   })

  /* VBox layout and content */
  alignment = Pos.Center
  alignmentInParent = Pos.Center
  spacing = 4 * V_SPACING
  content = List(
    ScalaFxUtils.newVSpacer(minH =1),
		dbConnectionSettings
	)
  /* update global variables */
  private def updateUSername(name:String){
	  QuickStart.userName=name
  }
  private def updatePassword(passUser:String){
	  QuickStart.passwordUser=passUser
  }
  private def updateHost(hostname:String){
	  QuickStart.hostNameUser=hostname
  }
  private def updatePort(portnumber:Int){
	  QuickStart.port=portnumber
  }
  /* testConnectionToPostgres */
  
  private def dialgoConnectionSuccess(){
    new Alert(AlertType.INFORMATION, "Connection test successed !!!").showAndWait()
  }
  private def dialgoConnectionFail(){
    new Alert(AlertType.INFORMATION, "Connection test failed !!!").showAndWait()
  }
 }