package com.github.liebharc

import android.app.{ActionBar, FragmentTransaction}
import android.app.ActionBar.{Tab, TabListener}
import android.content.Context
import android.os._
import android.support.v4.app._
import android.support.v4.view._
import android.util.AttributeSet
import android.view.MotionEvent

class MainActivity
    extends FragmentActivity
    with TypedFindView
    with QuizBehaviour
    with FeedbackBehaviour
    with ControlBehaviour {

  /** Called when the activity is first created. */
  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.main)
    val pager = findView(TR.pager)
    pager.setAdapter(new PagerAdapter(getSupportFragmentManager))
    val actionBar = getActionBar()
    actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS)
    val names = "Control" :: "Feedback" :: "Quiz" :: Nil
    for (name <- names) {
      val page =
        actionBar
          .newTab()
          .setText(name)
          .setTabListener(new PagerTabListener(pager))
      actionBar.addTab(page)
    }
  }
}

class PagerTabListener(pager: ViewPager) extends TabListener {
  override def onTabReselected(tab: Tab, fragmentTransaction: FragmentTransaction): Unit = ()

  override def onTabUnselected(tab: Tab, fragmentTransaction: FragmentTransaction): Unit = ()

  override def onTabSelected(tab: Tab, fragmentTransaction: FragmentTransaction) {
    pager.setCurrentItem(tab.getPosition)
  }
}

class PagerAdapter(manager: FragmentManager) extends FragmentPagerAdapter(manager) {

  override def getCount = 3

  override def getPageTitle(position: Int): CharSequence = "Page #" + position

  override def getItem(position: Int): Fragment = {
    position match {
      case 0 => new ControlFragment()
      case 1 => new FeedbackFragment()
      case 2 => new QuizFragment()
      case _ => null
    }
  }
}

class CustomViewPager(context: Context, attr: AttributeSet) extends ViewPager(context, attr) {

  var enabled: Boolean = false

  override def onTouchEvent(ev: MotionEvent): Boolean = {
    if (this.enabled) {
      return super.onTouchEvent(ev)
    }

    return false
  }

  override def onInterceptTouchEvent(ev: MotionEvent): Boolean = {
    if (this.enabled) {
      return super.onInterceptTouchEvent(ev)
    }

    return false
  }
}