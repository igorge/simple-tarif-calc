package gie.android

import android.app.Activity
import org.scaloid.common._

import scala.language.implicitConversions

class ViewGroupOps(val v: android.view.ViewGroup) extends AnyVal{

  def ensureUniqueIds()(implicit activity: Activity) = {
    children.foreach(_.uniqueId)
    v.uniqueId
    v
  }

  def children = new Iterator[android.view.View]{
    val count = v.getChildCount()
    var current = -1

    def hasNext = current+1 < count
    def next() = {
      current+=1
      v.getChildAt(current)
    }
  }
}


trait ViewGroupTrait {

  @inline implicit def toViewGroupOps(v: android.view.ViewGroup): ViewGroupOps = new ViewGroupOps(v)


}