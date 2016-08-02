package com.github.liebharc

import java.util

import android._
import android.app._
import android.os._
import android.widget._
import android.view._
import android.media._

case class SoundTriplet(tooLow: Int, perfect: Int, tooHigh: Int)

case class Sounds(dString: Int)

class MainActivity extends Activity with TypedFindView {
  lazy val textView = findView(TR.text)

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

  lazy val sounds = {
    val dString = soundPool.load(this, R.raw.dstring, 1)
    Sounds(dString)
  }
  /** Called when the activity is first created. */
  override def onCreate(savedInstanceState: Bundle) {
      super.onCreate(savedInstanceState)
      setContentView(R.layout.main)
      textView.setText("Hello world, from learnnotes")
  }

  def tooLowSelected(view: View) {
    textView.setText("Too low")
  }

  def perfectSelected(view: View) {
    textView.setText("Perfect")
  }

  def tooHighSelected(view: View) {
    textView.setText("Too High")
  }

  def replaySound(view: View) {
    val volume = 1.0f
    soundPool.play(sounds.dString, volume, volume, 1, 0, 1f);
    textView.setText("Replay")
  }
}