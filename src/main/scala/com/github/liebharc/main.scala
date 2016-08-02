package com.github.liebharc

import android.app._
import android.os._
import android.view._
import android.media._

object Utils {
  def randomPick(all: List[Int]): Int = {
    // Note that using scala random fails on the device, perhaps Proguard bug?
    import java.util.Random
    val random = new Random()
    val roll = random.nextInt(all.length)
    all(roll)
  }
}

case class Statistics(right: Int, wrong: Int) {
  def wrongInput() = Statistics(right, wrong + 1)
  def rightInput() = Statistics(right + 1, wrong)
  lazy val total = right + wrong
  lazy val humanFriendly = right.toString + "/" + wrong.toString
}

case class SoundTriplet(tooLow: Int, perfect: Int, tooHigh: Int) {
  val toList: List[Int] = List(tooLow, perfect, tooHigh)
}

case class Sounds(dNote: SoundTriplet) {
  val toList: List[Int] = List(dNote).flatMap(t => t.toList)
}

class MainActivity extends Activity with TypedFindView {
  private val volume = 1.0f

  private lazy val textView = findView(TR.text)

  /*
    Useful links for soundpool:
    https://dzone.com/articles/playing-sounds-android
    http://stackoverflow.com/questions/17069955/play-sound-using-soundpool-example#17070454
   */
  private lazy val soundPool = {
    val attributes = new AudioAttributes.Builder()
      .setUsage(AudioAttributes.USAGE_GAME)
      .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
      .build()

    new SoundPool.Builder()
      .setAudioAttributes(attributes)
      .build()
  }

  private lazy val sounds = {
    val dNote = soundPool.load(this, R.raw.dstring, 1)
    Sounds(SoundTriplet(dNote, dNote, dNote))
  }

  private var currentSound = -1

  private var stats = Statistics(0, 0)

  /** Called when the activity is first created. */
  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.main)
    textView.setText(stats.humanFriendly)
    currentSound = Utils.randomPick(sounds.toList)
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
    playCurrentSound()
  }

  def playCurrentSound() {
    soundPool.play(currentSound, volume, volume, 1, 0, 1f);
  }
}