/* Copyright Simon Skilevic
 * Master Thesis for Chair of Databases and Information Systems
 * Uni Freiburg
 */

package org.aksw.s2rdf.dataset_creator

import java.io.File

import scala.sys.process.stringToProcess
/**
 * The set of different help-functions
 * TODO: move to the places, where they are used due to small number of
 * functions
 */
object Helper {

  /**
   * transform table name for storage table in HDFS
   */
  def getPartName(v: String): String = {
    v.replaceAll(":", "__").replaceAll("<", "_L_").replaceAll(">", "_B_").replace("/", "%2F")//.replace(".", "DOT")
  }

  /**
   * Float to String formatted
   */
  def fmt(v: Any): String = v match {
    case d : Double => "%1.2f" format d
    case f : Float => "%1.2f" format f
    case i : Int => i.toString
    case _ => throw new IllegalArgumentException
  }

  /**
   * get ratio a/b as formated string
   */
  def ratio(a: Long, b: Long): String = {
    fmt((a).toFloat/(b).toFloat)
  }

  /**
   * remove directory in HDFS (if not exists -> it's ok :))
   */
  def removeDirInHDFS(path: String) = {
    val cmd = "hdfs dfs -rm -f -r " + path
    val output = cmd.!!
  }

  /**
   * create directory in HDFS
   */
  def createDirInHDFS(path: String) = {
    try{
//      val cmd = "hdfs dfs -mkdir " + path
//      val output = cmd.!!
      new File(path).mkdirs()
    } catch {
      case e: Exception => println("Cannot create directory->"
                                   + path + "\n" + e)
    }
  }
}
