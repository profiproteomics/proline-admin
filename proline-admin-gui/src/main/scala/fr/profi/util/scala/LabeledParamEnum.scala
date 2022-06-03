package fr.profi.util.scala

import scala.language.implicitConversions
/** 
 * **************************************** *
 * Enumeration with labeled values (params) *
 * **************************************** * 
 **/

object LabeledParamEnum extends LabeledParamEnum {
  
  /** Implicitly get Param 'label' field when a string is needed **/
  implicit def paramToString(param: LabeledParamEnum#Param): String = param.label
  
}


trait LabeledParamEnum extends Enumeration {
  
  thisenum =>

  /** Param class **/
  class Param(index: Int, val name: String, val label: String) extends Val(index: Int, name: String) {
    override def toString() = name
  }

  /** Create a new Param  **/
  protected final def Param(name: String, label: String): Param = new Param(this.nextId, name, label)


  /** Get all values in enumeration **/
  def params(): List[Param] = {
    thisenum.values.toList.map(p => p.asInstanceOf[Param])
  }

}