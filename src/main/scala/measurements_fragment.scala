package gie.tarif_app

import org.scaloid.common._

import scala.util.{Failure, Success, Try}
import scala.language.postfixOps
import gie.android.utils._
import gie.rx.utils._
import gie.android.logger

class MeasurementsFragment extends GFragment with  HaveObservable[Try[Measurements]] with TagUtil {

  import helper._
  import android.text.InputType._
  import rx.lang.scala._

  private val hint = "00000"
  private val gateOb = Subject[Unit]()

  private var impl_observable = optionalObservableWrapper[Try[Measurements]]()

  override def observable = {
    impl_observable.observable
  }

  onStart {
    logger.debug("onStart()")
    gateOb.onNext()
  }

  onCreateView { bundle=>

    val layout = new SVerticalLayout {
      this.id = R.id.measurements_fragment_8a2b5bbe

      var oldLayout = new SLinearLayout with HaveObservable[Try[Int]] {
        this.id = R.id.measurements_fragment_85f1af0a

        STextView(R.string.measurements_old).padding(5 dip).<<(0 dip, WRAP_CONTENT).Weight(1)
        val (oldEdit, ob) = {
          val p = makeObservable {
            SEditText("").id(R.id.measurements_fragment_da98f798).hint(hint).inputType(TYPE_CLASS_NUMBER).<<(0 dip, WRAP_CONTENT).Weight(2)>>
          }
          (p._1, p._2 map {
            validation.strToNumberValidationFilter(0)
          })
        }
        decoratedOnError(oldEdit, ob)

        def observable = ob
      }


      val currentLayout = new SLinearLayout with HaveObservable[Try[Int]] {
        STextView(R.string.measurements_current).padding(5 dip).<<(0 dip, WRAP_CONTENT).Weight(1)
        val (oldEdit, ob) = {
          val p = makeObservable {
            SEditText("").id(R.id.measurements_fragment_70128410).hint(hint).inputType(TYPE_CLASS_NUMBER).<<(0 dip, WRAP_CONTENT).Weight(2).>>
          }
          (p._1, p._2 map {
            validation.strToNumberValidationFilter(0)
          })
        }
        decoratedOnError(oldEdit, ob)

        def observable = ob
      }

      val discount = new SLinearLayout with HaveObservable[Try[Discount]] {
        STextView("%").padding(5 dip).<<(0 dip, WRAP_CONTENT).Weight(1)
        val (oldEdit, pctOb) = {
          val p = makeObservable {
            SEditText("").id(R.id.measurements_fragment_b40f2bbe).hint(hint).inputType(TYPE_CLASS_NUMBER).<<(0 dip, WRAP_CONTENT).Weight(2).>>
          }
          (p._1, p._2 map {
            validation.strToNumberValidationFilter(0, 100)
          })
        }
        decoratedOnError(oldEdit, pctOb)


        STextView(R.string.measurements_kWatt).padding(5 dip).<<(0 dip, WRAP_CONTENT).Weight(1)
        val (oldEdit2, kWattOb) = {
          val p = makeObservable {
            SEditText("").id(R.id.measurements_fragment_2531c0e0).hint(hint).inputType(TYPE_CLASS_NUMBER).<<(0 dip, WRAP_CONTENT).Weight(2).>>
          }
          (p._1, p._2 map {
            validation.strToNumberValidationFilter(0)
          })
        }
        decoratedOnError(oldEdit2, kWattOb)

        val ob = pctOb combineLatest kWattOb map (mergeTry(_)) map {
          case Success((p, k))=>Success(Discount(p,k))
          case Failure(e)=>Failure(e)
        }

        def observable = ob
      }


      this += oldLayout
      this += currentLayout
      val (discountBox, discountS) = makeObservable(SCheckBox(R.string.measurements_discount).id(R.id.measurements_fragment_bafd0310))
      this += discount

      discountS.subscribe {
        isChecked => discount.children foreach (_.setEnabled(isChecked))
      }

      val discountOb = discountS combineLatest discount.observable map {
        case (true, Success(v)) => Success(Some(v))
        case (false, _) => Success(None)
        case (true, Failure(v)) => Failure(v)
        case _ => throw new UnsupportedOperationException()
      }

      val measurementsOb = combineLatest(oldLayout.observable, currentLayout.observable, discountOb) map (mergeTry(_)) map {
        case Success((prev, curr, discount)) => Success(Measurements(prev, curr, discount))
        case Failure(err) => Failure(err)
      } combineLatest (gateOb) map (_._1)

      impl_observable.relayToObservable = measurementsOb

    } padding 1.dip


    logger.debug(s"created ${this}")

    layout

  }

}
