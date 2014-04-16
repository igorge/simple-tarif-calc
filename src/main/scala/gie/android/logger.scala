package gie.android

import android.util.Log
import org.scaloid.common.{TagUtil, LoggerTag}

trait LoggerTrait {

  @inline protected def isLoggable(tag: String, level:Int) = true

  @inline protected def loggingText(str: String, t: Throwable) = str + (if (t == null) "" else "\n" + Log.getStackTraceString(t))


}

object logger extends LoggerTrait {
  @inline def verbose (str: => String, t: Throwable = null)(implicit tag: LoggerTag) { if (isLoggable(tag.tag, Log.VERBOSE)) Log.v  (tag.tag, loggingText(str, t))}
  @inline def debug   (str: => String, t: Throwable = null)(implicit tag: LoggerTag) { if (isLoggable(tag.tag, Log.DEBUG  )) Log.d  (tag.tag, loggingText(str, t))}
  @inline def info    (str: => String, t: Throwable = null)(implicit tag: LoggerTag) { if (isLoggable(tag.tag, Log.INFO   )) Log.i  (tag.tag, loggingText(str, t))}
  @inline def warn    (str: => String, t: Throwable = null)(implicit tag: LoggerTag) { if (isLoggable(tag.tag, Log.WARN   )) Log.w  (tag.tag, loggingText(str, t))}
  @inline def error   (str: => String, t: Throwable = null)(implicit tag: LoggerTag) { if (isLoggable(tag.tag, Log.ERROR  )) Log.e  (tag.tag, loggingText(str, t))}
  @inline def wtf     (str: => String, t: Throwable = null)(implicit tag: LoggerTag) { if (isLoggable(tag.tag, Log.ASSERT )) Log.wtf(tag.tag, loggingText(str, t))}

  @inline def debug_if(predicate: =>Boolean)(str: => String, t: Throwable = null)(implicit loggerTag: LoggerTag) {
    if (isLoggable(loggerTag.tag, Log.DEBUG)) {
      if(predicate) Log.d(loggerTag.tag, loggingText(str, t))
    }
  }

}


package GlobalLogger {

object logger extends LoggerTrait with TagUtil {
  @inline def verbose(str: => String, t: Throwable = null) {
    if (isLoggable(loggerTag.tag, Log.VERBOSE)) Log.v(loggerTag.tag, loggingText(str, t))
  }

  @inline def debug(str: => String, t: Throwable = null) {
    if (isLoggable(loggerTag.tag, Log.DEBUG)) Log.d(loggerTag.tag, loggingText(str, t))
  }

  @inline def info(str: => String, t: Throwable = null) {
    if (isLoggable(loggerTag.tag, Log.INFO)) Log.i(loggerTag.tag, loggingText(str, t))
  }

  @inline def warn(str: => String, t: Throwable = null) {
    if (isLoggable(loggerTag.tag, Log.WARN)) Log.w(loggerTag.tag, loggingText(str, t))
  }

  @inline def error(str: => String, t: Throwable = null) {
    if (isLoggable(loggerTag.tag, Log.ERROR)) Log.e(loggerTag.tag, loggingText(str, t))
  }

  @inline def wtf(str: => String, t: Throwable = null) {
    if (isLoggable(loggerTag.tag, Log.ASSERT)) Log.wtf(loggerTag.tag, loggingText(str, t))
  }

  @inline def debug_if(predicate: =>Boolean)(str: => String, t: Throwable = null) {
    if (isLoggable(loggerTag.tag, Log.DEBUG)) {
      if(predicate) Log.d(loggerTag.tag, loggingText(str, t))
    }
  }

}

}
  