package gie.tarif_app

import org.scaloid.common._
import android.widget.ListAdapter
import android.view.{ViewGroup, View}
import scala.collection.mutable.ArrayBuffer
import gie.android.GlobalLogger.logger
import scala.collection.mutable
import gie.android._
import gie.android.utils._
import android.os.Bundle
import android.content.Intent
import android.text.{Editable, InputType, InputFilter}
import scala.util.{Try, Success, Failure}
import android.app.Activity


object EditRateActivity {

  type RateT = ElectricityRates.RateT
  type ParamT = (Int,RateT)
  type ResultT = ParamT

  val RATE_VALUE = "rate_value"
  val RATE_UPDATED_VALUE = "rate_updated_value"

  def setOptions(intent: Intent, idx: Int, rate: (Int, BigDecimal)){
    assume(intent ne null)
    intent.putExtra(RATE_VALUE, toBinary((idx,rate)))
  }

  def getResult(intent: Intent): Option[ResultT] = {
    val updated = fromBinary[ResultT](intent.getByteArrayExtra(RATE_UPDATED_VALUE))

    Option(updated)
  }

  def genResultData(idx: Int, rate: RateT): Intent = {
    val data = new Intent

    data.putExtra(RATE_UPDATED_VALUE, toBinary((idx, rate)))

    data
  }
}

class EditRateActivity extends GActivity {
  import EditRateActivity._

  object param extends GBundleSerializable[ParamT](RATE_VALUE)(
    key => fromBinary[ParamT]( getIntent.getByteArrayExtra(key) ) )

  object rateToEdit extends GBundleSerializable[RateT](RATE_UPDATED_VALUE)( _ => param()._2 )

  bindBundled(rateToEdit)

  onCreate{

    contentView = new GMigLayout{
      layoutConstraint( _.fillX )
      //setColumnConstraints("[][fill,grow]")

      import InputType._

      STextView(R.string.view_results_fragment_limit)
      this += new SEditText(rateToEdit()._1.toString){
        this.afterTextChanged { p1: Editable =>
          val validator = validation.strToNumberValidationFilter(0, ElectricityRates.wrapPoint)
          validator (p1.toString) match {
            case Failure(err) =>  this.setError(err.toString)
            case Success(v)   =>  rateToEdit{ rateToEdit().copy(_1 = v) };  this.setError(null)
          }
        }

      }.inputType(TYPE_CLASS_NUMBER).<<.cc( cc.wrap).>>

      STextView(R.string.view_results_fragment_price)
      this += new SEditText(rateToEdit()._2.toString){
        this.afterTextChanged { p1: Editable =>
          def validator(str: String) = Try{ BigDecimal(str) }
          validator (p1.toString) match {
            case Failure(err) =>  this.setError(err.toString)
            case Success(v)   =>  rateToEdit{ rateToEdit().copy(_2 = v) };  this.setError(null)
          }
        }

      }.inputType(TYPE_CLASS_NUMBER | TYPE_NUMBER_FLAG_DECIMAL).<<.cc(cc.wrap).>>

    }.asInstanceOf[View]
  }

  defOptionMenu(R.string.save){_ =>

    setResult(Activity.RESULT_OK, genResultData(param()._1, rateToEdit()) )
    finish()

    true
  }


}




class RatesActivity extends GActivity {

  private def impl_BuildRow(id: Int, rate: (Int, BigDecimal))(delFun: =>Any)(editFun: =>Any): View = {
    logger.debug("impl_BuildRow(...)")
    new GMigLayout{
      layoutConstraint( _.fillX )

      STextView( rate.toString )
      this += new SHorizontalLayout {
        SButton(R.string.edit, editFun)
        SButton(R.string.delete, delFun)
      }.<<.cc( cc.alignX(ALIGN_RIGHT)).>>
    }
  }

  object ratesData extends GBundleSerializable[ArrayBuffer[(Int, BigDecimal)]]()( _ => ElectricityRates().to[ArrayBuffer] )

  onActivityResult(Activity.RESULT_OK){ (requestCode: Int, data: Intent) =>

    for( data     <- Option(data);
         updated  <- EditRateActivity.getResult(data)){

      val idx = updated._1
      val newRate = updated._2

      if (idx == -1) {
        ratesData() += newRate
      } else {
        ratesData()(idx) = newRate
      }

      ElectricityRates.update(ratesData())
      ratesData.clear()

      ratesAdapter.dataHaveChanged()
    }

    true
  }

  private def impl_removeRateAtIndex(idx: Int){
    ratesData().remove(idx)
    ElectricityRates.update(ratesData())
    ratesData.clear()
  }

  defOptionMenu(R.string.add){ _ =>

    val intent = SIntent[EditRateActivity]
    EditRateActivity.setOptions(intent, -1, (0, BigDecimal(0)))
    startActivityForResult(intent, 42)

    true
  }

  onCreateEx {b: Bundle =>

    contentView = new SListView(){

      this.adapter = ratesAdapter
    }
  }

  lazy val ratesAdapter = new ListAdapter with GListAdapter[(Int, BigDecimal), mutable.ArrayBuffer] {
    def impl_container = ratesData()
    override def getView(position: Int, convertView: View, parent: ViewGroup): View =
      impl_BuildRow(position, impl_container.apply(position)){
        impl_removeRateAtIndex(position)
        dataHaveChanged()
      }{
        val intent = SIntent[EditRateActivity]
        EditRateActivity.setOptions(intent, position, ratesData()(position))
        startActivityForResult(intent, 42)
      }
  }
}