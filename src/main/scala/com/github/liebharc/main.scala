package com.github.liebharc

import android.app.{ActionBar, FragmentTransaction}
import android.app.ActionBar.{Tab, TabListener}
import android.content.Context
import android.media.{AudioAttributes, SoundPool}
import android.os._
import android.support.v4.app._
import android.support.v4.view._
import android.util.AttributeSet
import android.view.MotionEvent

trait SoundPoolProvider {
  def soundPool: SoundPool
}

class MainActivity
    extends FragmentActivity
    with TypedFindView
    with QuizBehaviour
    with FeedbackBehaviour
    with SoundPoolProvider
    with ControlBehaviour {

  private def pager = findView(TR.pager)

  /*
    Useful links for soundpool:
    https://dzone.com/articles/playing-sounds-android
    http://stackoverflow.com/questions/17069955/play-sound-using-soundpool-example#17070454
   */
  lazy val soundPool = {
    val attributes = new AudioAttributes.Builder()
      .setUsage(AudioAttributes.USAGE_GAME)
      .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
      .build()

    new SoundPool.Builder()
      .setAudioAttributes(attributes)
      .build()
  }

  override def onSaveInstanceState(bundle: Bundle): Unit = {
    super.onSaveInstanceState(bundle)
    bundle.putInt("LastTab", findView(TR.pager).getCurrentItem)
  }

  override def onCreate(savedInstanceState: Bundle): Unit =  {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.main)
    pager.setAdapter(new PagerAdapter(getSupportFragmentManager))
    val actionBar = getActionBar
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

    if (savedInstanceState != null) {
      val lastTab = savedInstanceState.getInt("LastTab")
      if (lastTab >= 0 && lastTab < actionBar.getTabCount) {
        actionBar.getTabAt(lastTab).select()
      }
    }
  }
}

class PagerTabListener(pager: ViewPager) extends TabListener {
  override def onTabReselected(tab: Tab, fragmentTransaction: FragmentTransaction): Unit = ()

  override def onTabUnselected(tab: Tab, fragmentTransaction: FragmentTransaction): Unit = ()

  override def onTabSelected(tab: Tab, fragmentTransaction: FragmentTransaction): Unit =  {
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

    false
  }

  override def onInterceptTouchEvent(ev: MotionEvent): Boolean = {
    if (this.enabled) {
      return super.onInterceptTouchEvent(ev)
    }

    false
  }
}