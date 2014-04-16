package gie.android

import org.scaloid.common._

import android.view.View
import android.widget.GridLayout
import com.saynomoo.mig4android._
import net.miginfocom.layout._

import scala.language.implicitConversions

trait TraitGMigLayoutUtil {
  val ALIGN_RIGHT = "right"
  val ALIGN_CENTER = "center"

  def lc =  new LC
  def ac =  new AC
  def cc =  new CC

  def parseColumnConstraints(c: String) = ConstraintParser.parseColumnConstraints(c)
  def parseRowConstraints(c: String) = ConstraintParser.parseRowConstraints(c)
}

trait TraitGMigLayout[V <: MigLayout] extends TraitGMigLayoutUtil with TraitViewGroup[V] {
  def layoutConstraint: LC = basis.getLayoutConstraints()
  def layoutConstraint_=(lc: LC) = {
    basis.setLayoutConstraints(lc)
    basis
  }

  def layoutConstraint[T](fun: LC=>T):T = {
    val constraint = lc
    val res = fun(constraint)
    layoutConstraint = constraint
    res
  }
}

object GMigLayout extends TraitGMigLayoutUtil{
}

class GMigLayout()(implicit context: android.content.Context, parentVGroup: TraitViewGroup[_] = null)
  extends MigLayout(context) with TraitGMigLayout[GMigLayout] {

  def basis = this
  override val parentViewGroup = parentVGroup

  implicit def defaultLayoutParams[V <: View](v: V): LayoutParams[V] = new LayoutParams(v, null)

  class LayoutParams[V <: View](v: V, cc: CC) extends MigLayout.LayoutParams(cc) with ViewGroupLayoutParams[LayoutParams[V], V] {
    def basis = this

    v.setLayoutParams(this)

    def cc:CC = this.getConstraints()

    def cc_=(cc: CC) = {
      this.setConstraints(cc)
      this
    }

    def cc(cc: CC): this.type = {
      this.setConstraints( cc )
      this
    }

    def parent = GMigLayout.this

    def >> : V = v

  }


}