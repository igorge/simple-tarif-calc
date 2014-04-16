package gie.android

import android.app.Activity
import scala.collection.mutable.ArrayBuffer
import android.view.{Menu, MenuItem=>AndroidMenuItem}
import android.view.MenuItem.OnMenuItemClickListener

case class MenuItemDef( title: ()=>CharSequence,
                        groupId: Int = Menu.NONE,
                        id: Int = Menu.NONE,
                        order: Int = Menu.NONE,
                        onOptionsItemSelected: AndroidMenuItem=>Boolean = null,
                        onMenuItemClicked: AndroidMenuItem=>Boolean = (_)=>false)


trait GActivityOptionsMenuImpl {

  protected val impl_menus = new ArrayBuffer[PartialFunction[AndroidMenuItem,Boolean]]()
  protected val impl_toBuildMenus = new ArrayBuffer[MenuItemDef]()

  protected def impl_onOptionsItemSelected(menu: AndroidMenuItem): Boolean = {
    impl_menus.find( _.isDefinedAt(menu) ).fold{false}{ _(menu) }
  }

  protected def impl_onCreateOptionsMenu(menu: Menu) = {

    impl_toBuildMenus.foreach{ menuDef=>

      val currMenu = menu.add(menuDef.groupId, menuDef.id, menuDef.order, menuDef.title())

      if( menuDef.onOptionsItemSelected ne null ){
        impl_menus += {
          case `currMenu` => menuDef.onOptionsItemSelected (currMenu)
        }
      }

      currMenu.setOnMenuItemClickListener( new OnMenuItemClickListener {
        override def onMenuItemClick(item: AndroidMenuItem): Boolean = menuDef.onMenuItemClicked(item)
      })

    }

    impl_toBuildMenus.clear()
  }

  protected def defOptionMenu(  title: =>CharSequence,
                                groupId: Int = Menu.NONE,
                                id: Int = Menu.NONE,
                                order: Int = Menu.NONE,
                                onOptionsItemSelected: AndroidMenuItem=>Boolean = null)(
                                onMenuItemClicked: AndroidMenuItem=>Boolean = (_)=>false){
    val memo = MenuItemDef(()=>title, groupId, id, order, onOptionsItemSelected, onMenuItemClicked)
    impl_toBuildMenus += memo
  }

}


trait GActivityOptionsMenu extends Activity with GActivityOptionsMenuImpl {

  override def onOptionsItemSelected(menu: AndroidMenuItem): Boolean = {
    if(impl_onOptionsItemSelected(menu)) true else super.onOptionsItemSelected(menu)
  }

  override def onCreateOptionsMenu(menu: Menu) = {
    super.onCreateOptionsMenu(menu)

    impl_onCreateOptionsMenu(menu)

    true
  }

}