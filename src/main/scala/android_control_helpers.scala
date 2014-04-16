package gie.tarif_app

import android.text.Editable
import scala.util.{Failure, Success, Try}
import android.graphics.drawable.ColorDrawable
import android.graphics.Color
import org.scaloid.common._
import rx.lang.scala._
import gie.android.GlobalLogger.logger

import gie.rx.utils._

object helper {

  def makeObservable(te: android.widget.CheckBox): (android.widget.CheckBox, rx.lang.scala.Observable[Boolean]) = {

    val s = Subject[Boolean]()
    val bstrapS = lazyObservable{ te.isChecked }

    te.setOnCheckedChangeListener( new android.widget.CompoundButton.OnCheckedChangeListener(){
      def onCheckedChanged(compoundButton: android.widget.CompoundButton, b:Boolean){
        s.onNext( te.isChecked )
      }
    })

    (te, bstrapS ++ s)

  }

  def makeObservable(te: android.widget.EditText): (android.widget.EditText, rx.lang.scala.Observable[String]) = {

    val s = Subject[String]()
    val bstrapS = lazyObservable{
      logger.debug("seeding")
      te.getText.toString
    }

    te.afterTextChanged{ p1: Editable =>
      logger.debug(s"onNext(${p1.toString})")
      s.onNext( p1.toString )
    }

    (te, bstrapS ++ s)

  }

  def decoratedOnError(te:android.widget.EditText, ob: rx.lang.scala.Observable[Try[Int]]){
    val origBackground = te.getBackground
    val onErr =  new ColorDrawable(Color.RED)

    ob.subscribe(v=> v match {
      case Success(_) =>
        logger.debug("decoratedOnError: " + v.toString)
        te.setError(null)
      case Failure(ex) =>
        logger.debug("decoratedOnError: " + v.toString)
        te.setError( ex.toString )
    })
  }

}

