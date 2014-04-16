package gie.tarif_app

import scala.language.postfixOps

import android.os.{Debug, Bundle}
import org.scaloid.common._
import scala.util.{Failure, Success, Try}
import rx.lang.scala.Observable
import gie.android.{MenuItemDef, GActivityOptionsMenu, FragmentMemo}
import gie.rx.AndroidUIThreadScheduler
import java.util.concurrent.Executors
import scala.concurrent.duration._
import android.view.{MenuItem, Menu}

package object validation {

  def numberValidationFilter(min: Int, max: Int = Int.MaxValue):Int => Try[Int] =
    (v: Int)=>if(v>=min && v<=max) Success(v) else Failure(new IllegalArgumentException(s"value should be in range [$min..$max]"))

  def strToNumberValidationFilter(min: Int, max: Int = Int.MaxValue):String => Try[Int] = {
    val numRangeFilter = numberValidationFilter(min, max)
    (v: String)=>Try{ v.toInt } flatMap numRangeFilter
  }

}


case class Discount(pct: Int, kwatt: Int)
case class Measurements(prev: Int, curr: Int, discount: Option[Discount])

trait HaveObservable[T] {
  def observable: Observable[T]
}

trait HaveBoot {
  def boot():Unit
}

object scheduler {
  val javaService= Executors.newScheduledThreadPool(3)
  val rx = _root_.rx.lang.scala.schedulers.ExecutorScheduler(javaService)
}

class Main extends SActivity with GActivityOptionsMenu {

  object measurementsFragment extends FragmentMemo[MeasurementsFragment]
  object viewResultFragment extends FragmentMemo[ViewResultsFragment]

//  Debug.waitForDebugger()
defOptionMenu( R.string.rates_menu ){_=>
          val intent = SIntent[RatesActivity]
          startActivity(intent)
          true
  }

  onCreate {
    val l= new SVerticalLayout {  }.id(R.id.main_f19a9e7c).padding(20.dip)

    contentView = l
    measurementsFragment.create(l.id)
    viewResultFragment.create(l.id)
  }

  onCreate {
    measurementsFragment.get.observable.debounce(2 seconds, AndroidUIThreadScheduler) map (v=> v flatMap (CalculatePayment(_)) ) observeOn(AndroidUIThreadScheduler) subscribe viewResultFragment.get
  }

}
