package gie.tarif_app

import scala.util.Try
import scala.math.min
import scala.collection.mutable.ArrayBuffer
import scala.annotation.tailrec
import gie.android.logger
import org.scaloid.common.TagUtil
import android.content.Context


class InvalidDiscountSize(value: Int, limit: Int) extends Exception{
  override def getMessage = s"Invalid discount size, value: $value, limit: $limit"
}

class InvalidMeasurements(old: Int, current: Int) extends Exception{
  override def getMessage = s"Invalid measurements: ($old, $current)"
}

case class Payment(rate: ElectricityRates.RateT, amount:Int, price:BigDecimal)
case class CompletePayment(measurements:Measurements, totalKWatt: Int, payment: Seq[Payment])

object CalculatePayment extends TagUtil {

  private def impl_calcDiscount(kwats: Int, discount: Discount, rate:ElectricityRates.RateT) = {
    assume(discount.pct>0)
    assume(discount.pct<=100)

    if (discount.kwatt>rate._1) throw new InvalidDiscountSize(discount.kwatt, rate._1)

    val discountKwatt =  min( min(kwats, discount.kwatt), rate._1)
    logger.debug("discountKwatt: "+discountKwatt)

    val discountRate = BigDecimal(100-discount.pct) / 100 * rate._2

    val price = discountRate * discountKwatt

    Payment( (discount.kwatt, discountRate),  discountKwatt, price)
  }

  def calcTotalWatts(measurementOld: Int, measurementCurrent: Int) =
    if (measurementOld>measurementCurrent)
      (ElectricityRates.wrapPoint-measurementOld+measurementCurrent)
    else
      (measurementCurrent-measurementOld)

  @tailrec
  private def impl_calcPayment(kwattAccounted: Int, kwattLeft: Int, rates: List[ElectricityRates.RateT], accum: ArrayBuffer[Payment]): ArrayBuffer[Payment] =
    if(kwattLeft == 0 || rates.isEmpty) accum else {
      assume(kwattLeft>0)
      val thisRate = rates.head
      assume(kwattAccounted<=thisRate._1)
      val ratedWatt = math.min(thisRate._1 - kwattAccounted, kwattLeft)

      if(ratedWatt>0) {
        val payment = Payment(thisRate, ratedWatt, thisRate._2 * ratedWatt)
        accum += payment
      }

      impl_calcPayment(kwattAccounted+ratedWatt, kwattLeft-ratedWatt, rates.tail,accum)
    }


  def apply(measurements: Measurements)(implicit ctx: Context) = Try{
    if (  (measurements.curr>ElectricityRates.wrapPoint) ||
          (measurements.curr<0) ||
          (measurements.prev>ElectricityRates.wrapPoint) ||
          (measurements.prev<0) ) throw new InvalidMeasurements(measurements.prev, measurements.curr)

    val rates = ElectricityRates().toList
    val totalKwatsUsage = calcTotalWatts(measurements.prev, measurements.curr)

    logger.debug(" totalKwatsUsage ----------> "+totalKwatsUsage)

    val discountMaybe = measurements.discount map (impl_calcDiscount(totalKwatsUsage,_, rates.head))
    val kwattLeft = discountMaybe map (totalKwatsUsage - _.amount) getOrElse totalKwatsUsage

    val payments = new ArrayBuffer[Payment]()
    for( discount<-discountMaybe) payments+=discount

    impl_calcPayment((discountMaybe map (_.amount)) getOrElse 0, kwattLeft, rates, payments)

    logger.debug("payment: "+ payments.toSeq)

    CompletePayment(measurements, totalKwatsUsage, payments)
  }

}
