package gie.android

import org.scaloid.common._

import android.view.View
import android.widget.GridLayout

import scala.language.implicitConversions

trait TraitGGridLayout[V <: android.widget.GridLayout] extends TraitViewGroup[V] {
  def columnCount: Int = basis.getColumnCount()
  def columnCount_=(count: Int) = { basis.setColumnCount(count); basis }
  def columnCount(count: Int) = { basis.setColumnCount(count); basis }

}

class GGridLayout(implicit context: android.content.Context, parentVGroup: TraitViewGroup[_] = null)
  extends android.widget.GridLayout(context) with TraitGGridLayout[GGridLayout] {

  def basis = this
  override val parentViewGroup = parentVGroup

  implicit def defaultLayoutParams[V <: View](v: V): LayoutParams[V] = new LayoutParams(v)

  class LayoutParams[V <: View](v: V) extends GridLayout.LayoutParams() with ViewGroupMarginLayoutParams[LayoutParams[V], V] {
    def basis = this

    v.setLayoutParams(this)

    def parent = GGridLayout.this

    def >> : V = v

  }


}