package gie.android

import android.app.{Activity, FragmentTransaction, FragmentManager}

trait FragmentUtils {
  def withFragmentTransaction[T]( op:(FragmentManager, FragmentTransaction)=>T )(implicit activity:Activity) = {
    val fragmentManager = activity.getFragmentManager()
    val fragmentTransaction = fragmentManager.beginTransaction()
    var r = op(fragmentManager, fragmentTransaction)
    fragmentTransaction.commit()

    r
  }

}