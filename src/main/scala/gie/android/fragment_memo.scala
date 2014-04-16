package gie.android

import android.app.{Fragment, Activity}
import android.view.View
import gie.android.utils._
import scala.reflect.ClassTag

abstract class FragmentMemo[ T <: Fragment : ClassTag ](tag:String = null, ctorId: Int = View.NO_ID) {

  private var fragment: T = _
  private val impl_tag = if( (tag eq null) || (tag.isEmpty) ) {
    this.getClass.getName
  } else {
    tag
  }

  def id:Int = this.get.getId

  def create(createId: Int = View.NO_ID)(implicit ctx: Activity): T = {
    val effectiveId = if(createId == View.NO_ID) ctorId else createId

    if(effectiveId == View.NO_ID) throw new IllegalArgumentException(s"Provide valid id for FragmentMemo[${implicitly[ClassTag[T]].toString}]")

    if(fragment eq null){
      withFragmentTransaction { (fm,tr)=>
        val possibleFragment = fm.findFragmentByTag(impl_tag)
        if( possibleFragment eq null ) {
          fragment = implicitly[ClassTag[T]].runtimeClass.newInstance().asInstanceOf[T]
          tr.add(effectiveId, fragment, impl_tag)
          assume(id == effectiveId)
        } else {
          fragment = possibleFragment.asInstanceOf[T]
        }
      }
    }

    fragment
  }


  def get:T = {
    if( fragment eq null) throw new NoSuchElementException(s"create() FragmentMemo in onCreate() for '${implicitly[ClassTag[T]].toString}'")

    fragment
  }

  def apply() = get

}
