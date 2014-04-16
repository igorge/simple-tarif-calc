package gie.tarif_app

import android.content.Context
import gie.android.utils
import gie.android.logger
import scala.collection.mutable.ArrayBuffer
import java.io.{BufferedOutputStream, BufferedInputStream}
import org.scaloid.common.TagUtil
import scala.util.{Success, Failure, Try}

object ElectricityRates extends TagUtil {
  val wrapPoint = 99999
  type RateT = (Int, BigDecimal)
  type RatesT = Array[(Int,BigDecimal)]
  type RatesBuffer = ArrayBuffer[RateT]
  def apply()(implicit ctx: Context): Array[RateT] = {
    this.get
  }

  private val CONFIG_FILE_NAME = "rates.dat"

  private val impl_fallBackRates = Array((150, BigDecimal("0.2802")), (800, BigDecimal("0.3648")), (wrapPoint, BigDecimal("0.9576")))
  private var impl_rates: ArrayBuffer[(Int,BigDecimal)] = _

  private def impl_loadRates()(implicit ctx: Context): Try[RatesBuffer] = Try{
    resource.managed{ new BufferedInputStream(ctx.openFileInput(CONFIG_FILE_NAME)) } acquireAndGet { inFile=>
      utils.fromBinary[RatesBuffer](inFile)
    }
  }

  private def impl_storeRates(rates: RatesBuffer)(implicit ctx: Context) = Try {
    resource.managed{ new BufferedOutputStream(ctx.openFileOutput(CONFIG_FILE_NAME, Context.MODE_PRIVATE)) } acquireAndGet { outFile=>
      logger.debug("Storing rates: "+rates)
      utils.toBinary(rates, outFile)
    }
  }


  def update(rates: Seq[RateT])(implicit ctx: Context){
    assume(rates ne null)

    impl_rates = rates.sortWith( _._1 < _._1 ).to[ArrayBuffer]
    impl_storeRates(impl_rates).get
  }


  def get(implicit ctx: Context):RatesT = {
    assume(ctx ne null)

    if (impl_rates eq null) {

      //    impl_rates = impl_fallBackRates.to[ArrayBuffer]
      //    impl_storeRates(impl_rates).get

      impl_loadRates() match {
        case Failure(v)=>
          logger.debug(s"impl_loadRates() have failed with: ${v} ")
          impl_rates = impl_fallBackRates.to[ArrayBuffer]
          impl_storeRates(impl_rates).get
          logger.debug(s"impl_loadRates() stored fallback rates: ${impl_rates} ")
        case Success(v) =>
          logger.debug(s"impl_loadRates() have loaded rates: ${v} ")
          impl_rates = v
      }
    }

    logger.debug("returning rates: "+impl_rates)
    impl_rates.toArray
  }
}
