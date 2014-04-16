package gie.rx

import rx.lang.scala._
import scala.collection.mutable.ArrayBuffer
import scala.util.{Failure, Success, Try}
import gie.android.GlobalLogger.logger

package object utils {

  def lazyObservable[T](fun: => T): Observable[T] = Observable{ observer:Observer[T]=>
    observer.onNext( fun )
    observer.onCompleted()
    Subscription()
  }

  trait OptionalObservableTrait[T] {
    def relayToObservable_=(o: Observable[T])
    def relayToObservable:Observable[T]
    def observable:Observable[T]
  }

  def optionalObservableWrapper[T]():OptionalObservableTrait[T] = {

    val wrapper =  new OptionalObservableTrait[T]{
      private var relayObservable: Observable[T] = _

      private val observers = new ArrayBuffer[(Observer[T], Subscription)]()

      private def doSubscribe(o: Observer[T]) = {
        assume(relayObservable ne null)

        relayObservable.subscribe(
          onNext =  o.onNext(_),
          onError = o.onError(_)
          //onCompleted = ()=>o.onCompleted()
        )
      }

      private def addObserver(o: Observer[T]){
        if(relayObservable ne null){
          observers += ((o, doSubscribe(o)))
        } else {
          observers += ((o, null))
        }
      }

      private def removeObserver(o: Observer[T]){
        val obsIdx = observers.indexWhere{
          case (obs, _) if obs eq o => true
          case _ => false
        }

        assume(obsIdx != -1)
        val subs = observers.remove(obsIdx)._2
        if(subs ne null) subs.unsubscribe()
      }

      private val ob = Observable{ observer:Observer[T]=>

        addObserver(observer)

        Subscription{ removeObserver(observer) }
      }

      def relayToObservable_=(o: Observable[T]){

        logger.debug(s"relayToObservable = ${o}")

        if( relayObservable eq null ){
          relayObservable = o
          if(relayObservable ne null) {
            for(i <- (0 until observers.size)){
              assume( observers(i)._2 eq null )
              observers(i) = (observers(i)._1, doSubscribe(observers(i)._1))
            }
          }
        } else {
          relayObservable = o
          for(i <- (0 until observers.size)){
            assume( observers(i)._2 ne null )
            observers(i)._2.unsubscribe()
            observers(i) = (observers(i)._1, if(relayObservable eq null) null else doSubscribe(observers(i)._1))
          }
        }

      }

      def relayToObservable:Observable[T] = relayObservable

      def observable = ob

    }

    wrapper
  }


  def combineLatest[A,B,C](a: Observable[A], b: Observable[B], c: Observable[C]): Observable[(A,B,C)] = {
    val aAndB = a combineLatest b
    aAndB combineLatest c map (v=>(v._1._1, v._1._2, v._2))
  }

  def mergeTry[A,B](p: (Try[A],Try[B])): Try[(A,B)] = p match {
    case (Success(v1), Success(v2)) => Success(v1, v2)
    case (Failure(err@_), _) => Failure(err)
    case (_, Failure(err@_)) => Failure(err)
  }

  def mergeTry[A,B,C](p: (Try[A],Try[B],Try[C])): Try[(A,B,C)] = p match {
    case (Success(v1), Success(v2), Success(v3)) => Success(v1, v2, v3)
    case (Failure(err@_), _, _ ) => Failure(err)
    case (_, Failure(err@_), _ ) => Failure(err)
    case (_, _ ,Failure(err@_) ) => Failure(err)
  }

}