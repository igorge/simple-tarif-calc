package gie.android

import android.app.{Activity, Fragment}
import scala.collection.mutable.ArrayBuffer
import android.os.Bundle
import org.scaloid.common.{SLinearLayout, TraitViewGroup, TagUtil, Destroyable}
import android.view.{View, ViewGroup, LayoutInflater}
import android.content.{Intent, Context}
import java.io._
import android.database.DataSetObserver
import android.widget.ListAdapter
import scala.language.higherKinds

trait GCreatableAny {
  protected val impl_onCreateBodies = new ArrayBuffer[Bundle => Any]

  protected def impl_run_onCreateBodies(b: Bundle){
    impl_onCreateBodies.foreach(_(b))
  }

  def onCreateEx[T](fun: Bundle => T){
    impl_onCreateBodies += fun
  }

}

trait GCreatable extends GCreatableAny {

}

trait GCreatableExtendS extends GCreatableAny { this: org.scaloid.common.Creatable =>

  override def onCreate(body: => Any):()=>Any = {
    val el = (() => body)
    this.onCreateEx((_: Bundle) => body)
    el
  }

}


trait GBundled {
  def save(b: Bundle)
  def load(b: Bundle)
}

trait GByKeyStorable {
  def save(storeFun: (String, Array[Byte])=>Any)
  def load(loadFun: (String)=>Array[Byte])
}

trait GBundleSavable { this: GCreatableAny =>
  protected val impl_onSaveInstanceBodies = new ArrayBuffer[Bundle => Any]

  def bindBundled(bundled: GBundled){
    impl_onCreateBodies += (b=>{ bundled.load(b)})
    impl_onSaveInstanceBodies += (b=>{ bundled.save(b)})
  }

}

trait GActivityResult {
  protected val impl_onActivityResult = new ArrayBuffer[(Int,Int,Intent)=>Boolean]
  protected def impl_run_onActivityResult(requestCode: Int, resultCode: Int, data: Intent): Boolean = {
    impl_onActivityResult.find( _(requestCode, resultCode, data) ).isDefined
  }

  def onActivityResult(fun: (Int,Int,Intent)=>Boolean){
    impl_onActivityResult += fun
  }

  def onActivityResult(resultCode: Int)(fun: (Int,Intent)=>Boolean){
    impl_onActivityResult += { (requestCode: Int, rCode: Int, data: Intent) =>
      if(rCode==resultCode) {
        fun(requestCode, data)
      } else {
        false
      }
    }
  }

}


trait GActivity extends org.scaloid.common.SActivity with GCreatableExtendS with GBundleSavable with GActivityOptionsMenu with GActivityResult {

  override def onCreate(b: Bundle){

    assume(onCreateBodies.isEmpty)

    super.onCreate(b)
    impl_run_onCreateBodies(b)
  }

  override def onSaveInstanceState(b: Bundle){
    super.onSaveInstanceState(b)
    impl_onSaveInstanceBodies.foreach( _(b) )
  }

  override def onActivityResult(requestCode: Int, resultCode: Int, data: Intent){
    logger.debug(s"onActivityResult(${requestCode}, ${resultCode}, ${data})")

    if( !impl_run_onActivityResult(requestCode, resultCode, data ) ) {
      logger.debug("onActivityResult(): result not handled, calling super")
      super.onActivityResult(requestCode, resultCode, data )
    }

  }


}


trait GAdapterObservable { this: android.widget.Adapter =>

  protected val observables = new ArrayBuffer[DataSetObserver]()

  override def registerDataSetObserver(observer: DataSetObserver): Unit = {
    observables +=observer
//    logger.debug(s"Subscribed DataSetObserver: ${observer}")
  }

  override def unregisterDataSetObserver(observer: DataSetObserver): Unit = {

    import gie.android.GlobalLogger.logger

    val idx = observables.indexOf(observer)
    if(idx>=0) {
      observables.remove(idx)
      //logger.debug(s"Unsubscribed DataSetObserver: ${observer}")
    } else {
      logger.debug_if(idx<0)(s"Trying to unregister not registered DataSetObserver '${observer}'")
    }
  }

  def dataHaveChanged(){ observables.foreach( _.onChanged() )  }

  def dataHaveInvalidated(){ observables.foreach( _.onInvalidated() )  }

}

trait GListAdapterHelper { this: ListAdapter =>
  override def isEnabled(position: Int): Boolean = true
  override def areAllItemsEnabled(): Boolean = false
  override def hasStableIds: Boolean = true
  override def getItemViewType(position: Int): Int = 1
  override def getViewTypeCount: Int = 1
  override def getItemId(position: Int): Long =  position
  override def isEmpty: Boolean = getCount == 0

}

trait GListAdapter[T <: AnyRef, Container[Y] <: IndexedSeq[Y]] extends GListAdapterHelper with GAdapterObservable { this: android.widget.ListAdapter =>
  protected def impl_container: Container[T]

  override def getCount: Int = impl_container.size
  override def getItem(position: Int): AnyRef = impl_container.apply(position)
}


abstract class GBundleSerializable[T <: AnyRef](key : String = null)(ctor: String=>T = null) extends GBundled with GByKeyStorable with TagUtil {

  logger.debug("ctor")
  private val impl_key = if( (key eq null) || (key.isEmpty) ) {
    this.getClass.getName
  } else {
    key
  }

  private var impl_value:T = _

  def fold[B](ifEmpty: => B)(f: T => B): B =if (isEmpty) ifEmpty else f(this.get)

  def isDefined = impl_value ne null
  def isEmpty = isDefined

  def clear() = {
    val oldValue = impl_value
    impl_value = null.asInstanceOf[T]
    oldValue
  }

  def get: T = {
    logger.debug("get")

    if(impl_value eq null){
      logger.debug("get: invoking default value constructor")
      if(ctor eq null) throw new NoSuchElementException("Value is null and no 'ctor' is defined")
      impl_value = ctor(impl_key)
      if(impl_value eq null) throw new NoSuchElementException("'ctor' returned null")
    }

    impl_value
  }

  def value_=(newValue: T) = this.apply(newValue)
  def value: T = this.get

  def apply() = get

  def apply(newValue:T): T = {
    logger.debug_if(impl_value ne null)(s"Resetting value from '${impl_value}' to '${String.valueOf(newValue)}'")

    impl_value = newValue

    impl_value
  }

  def save(b: Bundle){
    logger.debug(s"[${impl_key}] save")

    if(b eq null){
      logger.debug(s"[${impl_key}] 'Bundle' is empty, ignoring save")
    } else {
      if( impl_value eq null ) {
        logger.debug(s"[${impl_key}] value is null, ignoring save")
      } else {
        b.putByteArray(impl_key, utils.toBinary(impl_value))
      }
    }
  }

  def load(binData: Array[Byte]){

    if(binData eq null){
      logger.debug(s"[${impl_key}] no data, nothing to load")
    } else {
      impl_value = utils.fromBinary(binData).asInstanceOf[T]
      logger.debug(s"[${impl_key}] found, data: ${impl_value}]")
    }
  }

  def load(b: Bundle){

    logger.debug(s"[${impl_key}] load")

    if(b eq null){
      logger.debug(s"[${impl_key}] 'Bundle' is empty, nothing to load")
    } else {
      load(b.getByteArray(impl_key))
    }

  }

  def load(loadFun: String => Array[Byte]): Unit = {
    load( loadFun(impl_key) )
  }

  def save(storeFun: (String, Array[Byte]) => Any): Unit = {
    if( impl_value eq null ) {
      logger.debug(s"[${impl_key}] value is null, ignoring save")
    } else {
      storeFun(impl_key, utils.toBinary(impl_value))
    }
  }
}


class SHorizontalLayout(implicit context: Context, parentVGroup: TraitViewGroup[_] = null) extends SLinearLayout {
  orientation = HORIZONTAL
}


package object utils extends FragmentUtils with ViewGroupTrait {

  def toBinary[T <: AnyRef](o: T): Array[Byte] = {
    val bos = new ByteArrayOutputStream
    val out = new ObjectOutputStream(bos)
    out.writeObject(o)
    out.close()
    bos.toByteArray
  }

  def toBinary[T <: AnyRef](o: T, os: OutputStream) {
    val out = new ObjectOutputStream(os)
    out.writeObject(o)
    out.flush()
  }

  def fromBinary[T <: AnyRef](bytes: Array[Byte]): T = {
    val in = new ObjectInputStream(new ByteArrayInputStream(bytes))
    val obj = in.readObject
    in.close()
    obj.asInstanceOf[T]
  }

  def fromBinary[T <: AnyRef](is: InputStream): T = {
    val in = new ObjectInputStream(is)
    val obj = in.readObject
    in.close()
    obj.asInstanceOf[T]
  }


  trait FragmentTrait[T <: Fragment] {
    def basis: Fragment

    def activity = basis.getActivity()
  }

  trait StartStoppable {
    protected val onStartBodies = new ArrayBuffer[() => Any]
    protected val onStopBodies = new ArrayBuffer[() => Any]

    def onStart(body: => Any): () => Any = {
      val el = () => {
        body
      }
      onStartBodies += el
      el
    }

    def onSop(body: => Any): () => Any = {
      val el = () => {
        body
      }
      onStopBodies += el
      el
    }
  }


  trait Attachable {
    protected val onAttachBodies = new ArrayBuffer[(Activity) => Any]

    def onAttach(el: Activity => Any): Activity => Any = {
      onAttachBodies += el
      el
    }

    def onAttach(body: => Any): Activity => Any = {
      onAttach((a: Activity) => body)
    }
  }

  trait Detachable {
    protected val onDetachBodies = new ArrayBuffer[() => Any]

    def onDetach(body: => Any) = {
      val el = (() => body)
      onDetachBodies += el
      el
    }
  }



  trait GFragment extends Fragment with FragmentTrait[GFragment] with GCreatable with Destroyable with Attachable with Detachable with StartStoppable with TagUtil {

    override def basis = this

    protected implicit def ctx: android.content.Context = basis.getActivity()

    protected implicit def implicitActivity = activity

    protected var viewFun: (LayoutInflater, ViewGroup, Bundle) => View = (_, _, _) => null

    override def onAttach(a: Activity) {
      super.onAttach(a)
      onAttachBodies foreach (_(a))
    }

    override def onDetach() {
      onDetachBodies foreach (_())
      super.onDetach()
    }

    override def onCreate(b: Bundle) {
      super.onCreate(b)
      impl_run_onCreateBodies(b)
    }

    override def onDestroy() {
      onDestroyBodies.foreach(_())
      super.onDestroy()
    }

    override def onCreateView(li: LayoutInflater, vg: ViewGroup, b: Bundle): View = {
      val v = viewFun(li, vg, b)
      v
    }

    override def onStart() {
      super.onStart()
      onStartBodies foreach (_())
    }

    override def onStop() {
      super.onStop()
      onStopBodies foreach (_())
    }

    protected def onCreateView(fun: Bundle => View) = {
      val el = (_: LayoutInflater, _: ViewGroup, b: Bundle) => {
        fun(b)
      }
      viewFun = el
      el
    }

    protected def onCreateView(body: => View) = {
      val el = (_: LayoutInflater, _: ViewGroup, _: Bundle) => {
        body
      }
      viewFun = el
      el
    }

  }

}