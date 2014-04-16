package gie.rx

import org.scaloid.common._
import rx.lang.scala._
import rx.subscriptions.BooleanSubscription
import gie.android.GlobalLogger.logger

object AndroidUIThreadScheduler extends Scheduler {

  object asJavaScheduler extends rx.Scheduler {

    class InnerScheduler extends rx.Scheduler.Inner { inner =>

      val innerSubscription = new BooleanSubscription()
      assume(!innerSubscription.isUnsubscribed)

      def unsubscribe() = innerSubscription.unsubscribe()
      def isUnsubscribed() = innerSubscription.isUnsubscribed()

      def schedule(action: rx.functions.Action1[rx.Scheduler.Inner]) = {
        logger.debug("scheduling from  " + Thread.currentThread().getId)

        runOnUiThread {
          logger.debug("scheduled item invoked at " + Thread.currentThread().getId)
          if (!innerSubscription.isUnsubscribed()) action.call(inner)
        }
      }


      def schedule(action: rx.functions.Action1[rx.Scheduler.Inner], delayTime: Long, unit: java.util.concurrent.TimeUnit) = {

        logger.debug("delay-scheduling from  " + Thread.currentThread().getId)

        handler.postDelayed( new Runnable {
          override def run(){
            logger.debug("delay-scheduled item invoked at " + Thread.currentThread().getId)
            if( !innerSubscription.isUnsubscribed() ) action.call(inner)
          }

        }, unit.toMillis(delayTime))
      }

    }

    def schedule(action: rx.functions.Action1[rx.Scheduler.Inner], delayTime: Long, unit: java.util.concurrent.TimeUnit): rx.Subscription = {

      val inner = new InnerScheduler()
      inner.schedule(action, delayTime, unit)

      inner
    }

    def schedule(action: rx.functions.Action1[rx.Scheduler.Inner]): rx.Subscription = {

      val inner = new InnerScheduler()
      inner.schedule(action)

      inner
    }
  }


}

