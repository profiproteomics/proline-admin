package fr.proline.admin.gui.wizard.util

import scalafx.Includes._
import scalafx.scene.control.SelectionMode
import scalafx.scene.control.TableView
import scalafx.collections.ObservableBuffer
import javafx.scene.{ control => jfxsc }
import javafx.scene.input.MouseEvent
import javafx.event.EventHandler
import scala.collection.mutable.Set
import javafx.scene.{ control => jfxsc }
import scalafx.scene.control.SelectionMode.sfxEnum2jfx
import scalafx.scene.control.TableView.sfxTableView2jfx

/**
 * TableView with multiple selected items.
 * @author aromdhani
 * @param data an observable Buffer of items.
 *
 */
class MultiSelectTableView[S](data: ObservableBuffer[S]) extends TableView[S] {
  var selectedItems: Set[S] = Set[S]()
  val multiSelectTableView = this
  multiSelectTableView.items_=(data)
  //selection multiple
  multiSelectTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE)
  //resize columns
  multiSelectTableView.setColumnResizePolicy(jfxsc.TableView.CONSTRAINED_RESIZE_POLICY)
  //get the selected items  
  multiSelectTableView.getSelectionModel.selectedIndexProperty().addListener((o: javafx.beans.value.ObservableValue[_ <: Number], oldVal: Number, newVal: Number) => {
    multiSelectTableView.setOnMouseClicked(new EventHandler[MouseEvent] {
      override def handle(t: MouseEvent) {
        if (t.isControlDown()) {
          if (newVal.intValue() >= 0) {
            selectedItems += multiSelectTableView.getSelectionModel.getSelectedItem
          }
        } else {
          if (!selectedItems.isEmpty) {
            selectedItems.clear()
          }
          if (newVal.intValue() >= 0) {
            selectedItems += multiSelectTableView.getSelectionModel.getSelectedItem
          }
        }
      }
    })

  })
  //TODO Ctrl + A select all 

  //   multiSelectTableView.addEventHandler(KeyEvent.KEY_PRESSED, new EventHandler[KeyEvent] {
  //    override def handle(kv: KeyEvent) {
  //      if (kv.code == KeyCode.a && kv.controlDown) {
  //    }
  //  })
}