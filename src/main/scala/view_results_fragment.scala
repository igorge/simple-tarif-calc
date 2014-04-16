package gie.tarif_app

import gie.android.utils._
import gie.android._

import org.scaloid.common._
import rx.lang.scala._

import android.view.{View, Gravity}

import scala.util.{Failure, Success, Try}
import scala.language.postfixOps
import gie.android.{GMigLayout, GGridLayout}

class ViewResultsFragment extends GFragment with Observer[Try[CompletePayment]] {
  
  private def dPad = 5 dip

  private class ViewResultsFragmentView extends SVerticalLayout{
    //this.id = R.id.view_results_fragment_35e69cbc
  }

  private var view:ViewResultsFragmentView = _

  onCreateView {
    val v = new ViewResultsFragmentView{

    }

    view = v
    v
  }

  private def impl_clear() = if(view ne null){
    view.removeAllViews()
  }

  private def impl_buildPrevCurrView(cp:CompletePayment) = {
    def mkBlock(text: CharSequence, value: CharSequence) = new SVerticalLayout{
        STextView(text).<<(WRAP_CONTENT, WRAP_CONTENT).Gravity(Gravity.CENTER)
        STextView(value).<<(WRAP_CONTENT, WRAP_CONTENT).Gravity(Gravity.CENTER)
    }
    val cpView = new SVerticalLayout{
      this += new SHorizontalLayout {
        this += mkBlock(R.string.view_results_fragment_previous, cp.measurements.prev.toString).padding(dPad).<<(WRAP_CONTENT, WRAP_CONTENT).>>
        this += mkBlock(R.string.view_results_fragment_current, cp.measurements.curr.toString).padding(dPad).<<(WRAP_CONTENT, WRAP_CONTENT).>>
        this += mkBlock(R.string.view_results_fragment_total, cp.totalKWatt.toString).padding(dPad).<<(WRAP_CONTENT, WRAP_CONTENT).>>
      }.<<(WRAP_CONTENT, WRAP_CONTENT).Gravity(Gravity.CENTER).>>
    }

    cpView
  }


  private def impl_buildPaymentsView2(cp:CompletePayment) = {
    if (cp.payment.size == 0)
      None
    else Some(new GMigLayout{
      layoutConstraint( _.fillX )

      STextView(R.string.view_results_fragment_limit)
      STextView(R.string.measurements_kWatt)
      STextView(R.string.view_results_fragment_rate)
      STextView(R.string.view_results_fragment_price).cc(cc.wrap)

      cp.payment.foreach{v=>
        STextView(s"${r2Text(R.string.view_results_fragment_upto)} ${v.rate._1}")
        STextView(v.amount.toString)
        STextView(v.rate._2.toString)
        STextView(v.price.toString).cc(cc.wrap)
      }
    })
  }

  private def impl_buildPaymentsView(cp:CompletePayment): Option[View] = {
    if(cp.payment.size == 0)
      None
    else
      Some(new SVerticalLayout{
          this+=new GGridLayout(){

            style {
              case v: View => v.padding(dPad,0, dPad, 0)
            }

            cp.payment.foreach{v=>
              STextView(s"${r2Text(R.string.view_results_fragment_upto)} ${v.rate._1}").<<(WRAP_CONTENT, WRAP_CONTENT).>>
              STextView(v.amount.toString).<<(WRAP_CONTENT, WRAP_CONTENT).>>
              STextView(v.rate._2.toString).<<(WRAP_CONTENT, WRAP_CONTENT).>>
              STextView(v.price.toString).<<(WRAP_CONTENT, WRAP_CONTENT).>>
            }
          }.columnCount(4).<<(WRAP_CONTENT, WRAP_CONTENT).Gravity(Gravity.CENTER).>>
      })
  }

  override def onNext(v:Try[CompletePayment]) = v match {
    case Success(v)=>
      impl_clear()

      view += impl_buildPrevCurrView(v)
      impl_buildPaymentsView2(v) foreach ( v=> view += v.<<(view.FILL_PARENT, view.WRAP_CONTENT)(view.defaultLayoutParams).>>)
      view += new STextView(s"${r2Text(R.string.view_results_fragment_total)} ${v.payment.foldLeft(BigDecimal(0)){(l,r)=>l+r.price}}").padding(dPad)

    case Failure(e)=>
      impl_clear()
      view += new STextView(e.toString)
  }


}