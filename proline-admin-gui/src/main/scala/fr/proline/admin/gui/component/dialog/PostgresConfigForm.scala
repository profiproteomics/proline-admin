package fr.proline.admin.gui.component.dialog

import com.typesafe.scalalogging.slf4j.Logging

import java.io.File
import java.io.FileWriter

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.collection.mutable.ListBuffer
import scala.util.control.Breaks._

import scalafx.Includes._
import scalafx.beans.property.LongProperty
import scalafx.geometry.Insets
import scalafx.geometry.Pos
import scalafx.scene.Cursor
import scalafx.scene.Node
import scalafx.scene.Scene
import scalafx.scene.control.Button
import scalafx.scene.control.CheckBox
import scalafx.scene.control.ComboBox
import scalafx.scene.control.Label
import scalafx.scene.control.ScrollPane
import scalafx.scene.control.ScrollPane.ScrollBarPolicy
import scalafx.scene.control.Tooltip
import scalafx.scene.input.KeyEvent
import scalafx.scene.layout.GridPane
import scalafx.scene.layout.HBox
import scalafx.scene.layout.StackPane
import scalafx.scene.layout.VBox
import scalafx.stage.Modality
import scalafx.stage.Stage

import fr.profi.util.primitives._
import fr.profi.util.scala.ByteUtils._
import fr.profi.util.scalafx.{ScalaFxUtils, NumericTextField, IntegerTextField}
import fr.profi.util.scalafx.ScalaFxUtils.{closeIfEscapePressed, fireIfEnterPressed, seq2obsBuff }
import fr.proline.admin.gui.Main
import fr.proline.admin.gui.process.config.ConfigFileKVLine
import fr.proline.admin.gui.process.config.postgres._
import fr.proline.admin.gui.process.config.postgres.PgParamType._
import fr.proline.admin.gui.util.ConfigParamSlider


/**
 * *********************************** *
 * All components for one Config param *
 * *********************************** *
 */
case class PgFormLine(
  checkBox: CheckBox,
  nameLabel: Label,
  slider: ConfigParamSlider,
  valueNode: Node
)

/**
 * ***************************************************************** *
 * Form to edit and update PostgreSQL configuration (postresql.conf) *
 * ***************************************************************** *
 */
class PostgresConfigForm extends Stage with Logging {

  val formEditor = this

  /* Stage's properties */
  title = s"PostgreSQL configuration editor -- ${Main.postgresqlDataDir}"
  initModality(Modality.WINDOW_MODAL)
  initOwner(Main.stage)
  width = 800
  minWidth = 616

  /**
   * ***** *
   * STUFF *
   * ***** *
   */
  /* Read initial settings */
  val pgConfigFile = new PostgresConfigFile(Main.postgresqlDataDir + "/postgresql.conf")
  val pgConfigInitSettings = pgConfigFile.lineByKey

  /* Get min, max, default, and optimized values */
  val pgConfigDefaults = PostgresConfigV9_4.valueRangeByParam

  /* Get unit and formatter */
  val formatByParam = PostgresConfigV9_4.formatByParam

  /* Know graphical components for each param */
  val compoByParam: HashMap[PostgresOptimizableParamEnum.Param, PgFormLine] = HashMap()

  /**
   * ********** *
   * COMPONENTS *
   * ********** *
   */

  /* Note */
  val note = new Label {
    text = "Ticks marked with a star correspond to optimized values."
    style = "-fx-font-style:italic;"
  }
  
//  /* Physical memory viewer */
//  val physicalMemoryLabel = new Label("Physical memory")
  //TODO: Physical memory viewer

  /* Sliders & co */
  val labeledParams = PostgresOptimizableParamEnum.params()

  val checkBoxes = ListBuffer[CheckBox]()
  val nameLabels = ListBuffer[Label]()
  val sliders = ListBuffer[ConfigParamSlider]()
  val valueNodes = ListBuffer[Node]()
  val bytesValues = ListBuffer[LongProperty]()

  /* For each parameter, define components */
  labeledParams.foreach { labeledParam =>

    val paramFormat = formatByParam(labeledParam)
    val unit = paramFormat.unitString
    val formatter = paramFormat.formatter
    val initValueOpt = pgConfigInitSettings.get(labeledParam.name toLowerCase) //TODO: add field 'configKey' to Param rather than use name.toLowerCase

//    val bytesValue = new LongProperty()
    
    /* Check box: unselected if param is commented out */
    val checkBox = new CheckBox() { 
      selected = initValueOpt.isDefined
    }

    /* Name of the param */
    val nameLabel = new Label(labeledParam.label) {
      minWidth = 180
      maxWidth = 180

      tooltip = new Tooltip {
        text = paramFormat.toolTipText
      }
    }

    /* Slider for param value selection */
    //for byte amounts, unit is kB (?????????)
    val slider = new ConfigParamSlider(pgConfigDefaults(labeledParam)) {
      prefWidth <== formEditor.width
    }

    /* Numeric text field for param value viz. and selection */
    val VALUE_NODE_WIDTH = 180
    val valueNode: Node = {

      val sliderMinAsLong = slider.minValue.toLong
      val sliderMaxAsLong = slider.maxValue.toLong
      val paramType: PgParamType.Value = paramFormat.paramType

      /* FOR TIME VALUES */
      if (paramType == TIME) {

        /* Components */
        val minHours = sliderMinAsLong / 60

        val hoursField = new IntegerTextField(0) {
          setMinValue(if (minHours < 1) minHours else minHours - 1)
          setMaxValue((sliderMaxAsLong / 60) + 1)
          maxWidth = 30
          alignment = Pos.BaselineRight
        }
        val minutesField = new IntegerTextField(0) {
          setMinValue(0L)
          setMaxValue(60L)
          maxWidth = 30
          alignment = Pos.BaselineRight
        }

        /* Bindings */
        def _updateFieldsWithSliderValue() {
          hoursField.setValue(slider.value.intValue() / 60)
          minutesField.setValue(slider.value.intValue() % 60)
        }

        def _updateSliderWithFieldsValue() {
          val nonEmptyHours: Int = if (hoursField.text().isEmpty()) 0 else hoursField.getInt()
          slider.value = nonEmptyHours * 60 + minutesField.getInt()
        }

        slider.value.onChange { (_, oldValue, newValue) =>
          try { _updateFieldsWithSliderValue() }
          catch { case t: Throwable => { slider.value = toDouble(oldValue) } }
        }
        hoursField.text.onChange { (_, oldValue, newValue) =>
          try { _updateSliderWithFieldsValue() }
          catch { case t: Throwable => { hoursField.text = oldValue } }
        }
        minutesField.text.onChange { (_, oldValue, newValue) =>
          try { _updateSliderWithFieldsValue() }
          catch { case t: Throwable => { minutesField.text = oldValue } }
        }

        /* Node */
        new HBox {
          minWidth = VALUE_NODE_WIDTH
          maxWidth = VALUE_NODE_WIDTH
          padding = Insets(0, 0, 0, 20)
          spacing = 8
          content = Seq(
            hoursField,
            new Label("hours") {
              minWidth = 10
              padding = Insets(0, 5, 0, 0)
            },
            minutesField,
            new Label("min") {
              minWidth = 15
            }
          )
        }

      } 
      
      /* FOR BYTE AMOUNTS */
      else if (paramType == BYTES) {
        
        /* Components */
        val numericField = new NumericTextField(0) {
          setMinValue(0)
        }
        val unitBox = new ComboBox[ByteUnit.Value] {
          items = ByteUnit.values.toSeq
          minWidth = 60
        }

        /* Bindings */

        // Update fields with slider value
        def _updateFieldsWithSliderValue(oldValue: Number) {
          val bytesAmount = slider.value.longValue()          
          val asString = formatBytesAmount(
            bytesAmount = bytesAmount,
            numberFormat = "%.2f%s"
          )
          val (asDouble, unitValue) = parseBytesAmount(asString)

          if( bytesAmount > toLong(oldValue) ) {
            numericField.setValue(asDouble)
            unitBox.selectionModel().select(unitValue)
          } else {
            unitBox.selectionModel().select(unitValue)
            numericField.setValue(asDouble)
          }
        }

        // Update slider with fields value
        def _updateSliderWithFieldsValue() {
          val fieldValue = numericField.getBigDecimal().toDouble
          val bytesAsDouble = getBytesAmount(fieldValue, unitBox.selectionModel().selectedItem()).toDouble
          if (bytesAsDouble > slider.maxValue) throw new Exception(s"value $bytesAsDouble is superior to max value ${slider.maxValue}")
          slider.value = bytesAsDouble
        }

        // Slider event: try to update fields or update slider
        slider.value.onChange { (_, oldValue, newValue) =>
          try { _updateFieldsWithSliderValue(oldValue) }
          catch { case t: Throwable => { slider.value = toDouble(oldValue)} }
        }

        // Field event: try to update slider or update fields
        numericField.text.onChange { (_, oldValue, newValue) =>
          if (unitBox.selectionModel().selectedItem() != null) {
            try { _updateSliderWithFieldsValue() }
            catch { case t: Throwable => { numericField.text = oldValue } }
          }
        }

        // Combo box event: try to update slider or update fields
        unitBox.selectionModel().selectedItem.onChange { (_, oldValue, newValue) =>
          if (numericField.text() != "") {
            try { _updateSliderWithFieldsValue() }
            catch { case t: Throwable => { unitBox.selectionModel().select(oldValue) } }
          }
        }

        /* Node */
        new HBox {
          minWidth = VALUE_NODE_WIDTH
          maxWidth = VALUE_NODE_WIDTH
          padding = Insets(0, 0, 0, 20)
          spacing = 8
          content = Seq(numericField, unitBox)
        }
      } 
      
      /* FOR FLOATS AND INTEGERS */
      else {

        /* Component */
        val numericField = new NumericTextField(0) {

          /* FOR FLOAT VALUES */
          if (paramType == DECIMAL) {

            setMinValue(slider.minValue)
            setMaxValue(slider.maxValue)

            /* Bindings */
            def _updateFieldWithSliderValue() {
              this.setValue("%.1f".formatLocal(java.util.Locale.US, slider.value.doubleValue()))
            }
            
            def _updateSliderWithFieldsValue() {
              slider.value = this.getBigDecimal().toDouble
            }

            slider.value.onChange { (_, oldValue, newValue) =>
              try { _updateFieldWithSliderValue() }
              catch { case t: Throwable => { slider.value = toDouble(oldValue) } }
            }
            
            this.text.onChange {  (_, oldValue, newValue) =>
              try { _updateSliderWithFieldsValue() }
              catch { case t: Throwable => { this.text = oldValue } }
            }
          }
          
          /* For integer values */ else {

            setIntergersOnly(true)
            setMinValue(sliderMinAsLong)
            setMaxValue(sliderMaxAsLong)

            // Bindings
            def _updateFieldWithSliderValue() {
              this.setValue(BigDecimal(slider.value.longValue()))
            }
            def _updateSliderWithFieldsValue() {
              slider.value = this.getLong().toDouble
            }
            slider.value.onChange {
              try { _updateFieldWithSliderValue() }
              catch { case t: Throwable => { _updateSliderWithFieldsValue() } }
            }
            this.text.onChange {
              try { _updateSliderWithFieldsValue() }
              catch { case t: Throwable => { _updateFieldWithSliderValue() } }
            }
          }
        }

        /* Optionally add unit label */
        if (paramFormat.unitString.isEmpty()) {
          new StackPane {
            minWidth = VALUE_NODE_WIDTH
            maxWidth = VALUE_NODE_WIDTH
            padding = Insets(0, 0, 0, 20)
            content = numericField
          }
        } else {
          new HBox {
            minWidth = VALUE_NODE_WIDTH
            maxWidth = VALUE_NODE_WIDTH
            padding = Insets(0, 0, 0, 20)
            spacing = 5
            content = Seq(
              numericField,
              new Label(unit) {
                minWidth = 70
              }
            )
          }
        }
      }
    }

    /* Link component availability */
    Seq(nameLabel, slider, valueNode).foreach(_.disable <== !checkBox.selected)

    /* Initialize values */
    slider.value = {
      if (initValueOpt.isDefined) paramFormat.parser(initValueOpt.get.valueString).toDouble
      else pgConfigDefaults(labeledParam).suggestedValue.toDouble
    }

    /* Add to buffers and map */
    checkBoxes += checkBox
    nameLabels += nameLabel
    sliders += slider
    valueNodes += valueNode
    compoByParam += labeledParam -> PgFormLine(checkBox, nameLabel, slider, valueNode)
  }

  val paramsLen = labeledParams.length
  require(
    checkBoxes.length == paramsLen &&
      nameLabels.length == paramsLen &&
      sliders.length == paramsLen &&
      valueNodes.length == paramsLen,
    "Incorrect number of components in PostgresSettingsForm"
  )

  /* Buttons */
  val setAllToOptimizedButton = new Button("Set all to optimized value") {
    wrapText = true
    onAction = handle { _setAllToOptimized() }
  }
  val setQllToDefaultsButton = new Button("Set all to default value") {
    wrapText = true
    onAction = handle { _setAllToDefault() }
  }
  val applyButton = new Button("Apply") {
    onAction = handle { _onApplyPressed() }
  }
  val cancelButton = new Button("Cancel") {
    onAction = handle { formEditor.close() }
  }

  /**
   * ****** *
   * LAYOUT *
   * ****** *
   */

  /* Params: GridPane */
  val nodeBuffer = ListBuffer[Tuple3[Node, Int, Int]]()

  for (i <- 0 until paramsLen) {

    nodeBuffer += (
      (checkBoxes(i), 0, i),
      (nameLabels(i), 1, i),
      (sliders(i), 2, i),
      (valueNodes(i), 3, i)
    )
  }

  val paramsGridPane = new GridPane {

    gridLinesVisible = true
    alignment = Pos.BottomCenter
    vgap = 40
    content = ScalaFxUtils.getFormattedGridContent3(nodeBuffer.result())
  }

  /* Buttons */
  val buttonList = Seq(setAllToOptimizedButton, setQllToDefaultsButton, applyButton, cancelButton)
  buttonList.foreach { b =>
    b.prefWidth <== formEditor.width
    b.minHeight = 30
    b.prefHeight = 35
    b.style = "-fx-font-weight:bold;-fx-inner-border : black; "
  }

  val topButtons = new HBox {
    alignmentInParent = Pos.Center
    spacing = 20
    content = Seq(setAllToOptimizedButton, setQllToDefaultsButton)
  }

  val bottomButtons = new HBox {
    alignmentInParent = Pos.Center
    spacing = 20
    content = Seq(applyButton, cancelButton)
  }

  /* Scene */
  scene = new Scene {
    onKeyPressed = (ke: KeyEvent) => {
      closeIfEscapePressed(formEditor, ke)
      fireIfEnterPressed(applyButton, ke)
    }
    root = new ScrollPane {

      hbarPolicy = ScrollBarPolicy.AS_NEEDED
      vbarPolicy = ScrollBarPolicy.AS_NEEDED
      val SCROLL_BAR_PUTATIVE_WIDTH = 40

      content = new VBox {

        alignment = Pos.Center
        padding = Insets(20)
        spacing = 30

        minWidth <== formEditor.width - SCROLL_BAR_PUTATIVE_WIDTH
        maxWidth <== formEditor.width - SCROLL_BAR_PUTATIVE_WIDTH
        minHeight <== formEditor.height - SCROLL_BAR_PUTATIVE_WIDTH

        content = Seq(topButtons, note, paramsGridPane, bottomButtons)
      }
    }
  }

  /**
   * ******** *
   * FEATURES *
   * ******** *
   */

  private def _setAllToOptimized() {
    compoByParam.foreach {
      case (labeledParam, formLine) =>
        formLine.checkBox.selected = true
        formLine.slider.value = pgConfigDefaults(labeledParam).suggestedValue.toDouble
    }
  }

  private def _setAllToDefault() {
    compoByParam.foreach {
      case (labeledParam, formLine) =>
        formLine.checkBox.selected = true
        formLine.slider.value = pgConfigDefaults(labeledParam).defaultValue.toDouble
    }
  }

  private def _onApplyPressed() {

    Main.stage.scene().setCursor(Cursor.WAIT)

    val configFileLines = pgConfigFile.lines
    val configFileLinesLen = configFileLines.length
    val newLinesBuffer = new ArrayBuffer[String](PostgresOptimizableParamEnum.params().length)

    /* Update valueString in ConfigKVLine, and lines in ConfigFileIndexing.lines */
    compoByParam.foreach { case (labeledParam, formLine) =>

      val valueAsString = formatByParam(labeledParam).formatter(formLine.slider.value.doubleValue())
      val commentLine = !formLine.checkBox.selected()

      val configLineKey = labeledParam.name toLowerCase //TODO: add field 'configKey' to Param rather than use name.toLowerCase
      val oldConfigKVLineOpt = pgConfigFile.lineByKey.get(configLineKey)

      val newConfigKVLineOpt: Option[ConfigFileKVLine] = {

        /* If KVLine is already known, update it */
        if (oldConfigKVLineOpt.isDefined) {
          val oldConfigKVLine = oldConfigKVLineOpt.get

          if (commentLine) Some(oldConfigKVLine.comment())
          else Some(oldConfigKVLine.toNewKVLine(valueAsString, commented = false)) //commentLine
        }

        /* Otherwise line is commented or doesn't exist */
        else {

          /* Don't change or create commented parameters */
          if (commentLine) None

          /* Try to find line (commented) in lines array, or append it */
          else {
            val newLine = _createNewKVLine(
              configLineKey,
              valueAsString,
              configFileLines,
              configFileLinesLen,
              newLinesBuffer
            )

            Some(newLine)
          }
        }
      }

      /* Change in map and lines array if needed */
      if (newConfigKVLineOpt.isDefined) {
        val newConfigKVLine = newConfigKVLineOpt.get
        pgConfigFile.lineByKey(configLineKey) = newConfigKVLine
        configFileLines(newConfigKVLine.index) = newConfigKVLine.line //newConfigKVLine.toString()
      }
    }

    /* Write updated lines in config file */
    synchronized {
      val out = new FileWriter(new File(pgConfigFile.filePath))
      try {
        val linesToBeWritten = (configFileLines ++ newLinesBuffer.result()).mkString("\n")
        out.write(linesToBeWritten)
      } finally {
        out.close
      }
    }

    /* Restart PostgreSQL if needed */
    //TODO : restart PostgreSQL when needed

    /* Close dialog and make some logback */
    formEditor.close()

    println("<br>INFO - postgresql.conf successfully updated !<br>")
    logger.info("postgresql.conf successfully updated !")

    Main.stage.scene().setCursor(Cursor.DEFAULT)
  }

  private def _createNewKVLine(
    configLineKey: String,
    valueAsString: String,
    configFileLines: Array[String],
    configFileLinesLen: Int,
    newLinesBuffer: ArrayBuffer[String]
  ): ConfigFileKVLine = {
    
    val pattern = ("""^#\s*(""" + configLineKey + """)\s*=?\s*([\w\.]+)\s*(#.+)*""").r

    /* First, try to find it as a commented line in lines array */
    for (
      lineIndex <- 0 until configFileLinesLen;
      val line = configFileLines(lineIndex);
      val kvMatchOpt = pattern.findFirstMatchIn(line);
      //val kvMatchOpt = line =# (pattern, "key", "value", "comment");
      if (kvMatchOpt.isDefined)
    ) {
      val kvMatch = kvMatchOpt.get

      /* Create and return updated KVLine */
      require(line.startsWith("#"), s"The KVLine to be created must begin by '#' ($line)")
      
      //TODO: improve by doing it all at the same time
      val oldKVLine = ConfigFileKVLine(
        line = line,
        index = lineIndex,
        key = kvMatch.group(1),
        valueString = kvMatch.group(2),
        valueStartIdx = kvMatch.start(2), 
        valueEndIdx = kvMatch.end(2),
        commented = true
      )
      
      return oldKVLine.toNewKVLine(valueAsString, commented = false)
      
//      return ConfigFileKVLine(
//        line = line.drop(1),
//        index = lineIndex,
//        key = kvMatch.group(1),
//        valueString = kvMatch.group(2),
//        valueStartIdx = kvMatch.start(2) - 1, //-1 because initial '#' has been removed
//        valueEndIdx = kvMatch.end(2) - 1,
//        commented = false
//      )
    }

    /* If key wasn't found, append line to file */
    val line = configLineKey + " = " + valueAsString
    val lineIdx = newLinesBuffer.length
    val configLineKeyLen = configLineKey.length()
    val valueStartIdx = configLineKeyLen + 3 //for ' = ' symbol
    val valueEndIdx = valueStartIdx + valueAsString.length() - 1

    newLinesBuffer += line
    ConfigFileKVLine(line, lineIdx, configLineKey, valueAsString, valueStartIdx, valueEndIdx, commented = false)
  }

}