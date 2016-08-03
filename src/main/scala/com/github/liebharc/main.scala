package com.github.liebharc

import android.app._
import android.os._
import android.view._
import android.media._

object Utils {
  def randomPick[T](all: List[T]): T = {
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

trait SoundSample {
  val name: String
  val id: Int
  def isTooLow: Boolean
  def isTooHigh: Boolean
  def isPerfect: Boolean
}

case class TooLowSample(name: String, id: Int) extends SoundSample {
  override def isTooHigh: Boolean = false
  override def isTooLow: Boolean = true
  override def isPerfect: Boolean = false
}

case class TooHighSample(name: String, id: Int) extends SoundSample {
  override def isTooHigh: Boolean = true
  override def isTooLow: Boolean = false
  override def isPerfect: Boolean = false
}

case class PerfectSample(name: String, id: Int) extends SoundSample {
  override def isTooHigh: Boolean = false
  override def isTooLow: Boolean = false
  override def isPerfect: Boolean = true
}

object SoundTriplet {
  def apply(name: String, tooLow: Int, perfect: Int, tooHigh: Int): List[SoundSample]
    = List(TooLowSample(name, tooLow), PerfectSample(name, perfect), TooHighSample(name, tooHigh))
}

class MainActivity extends Activity with TypedFindView {
  private val volume = 1.0f

  private lazy val noteView = findView(TR.note)

  private lazy val statsView = findView(TR.stats)

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
    var samples: List[List[SoundSample]] = Nil
    samples ::= List(PerfectSample("D", soundPool.load(this, R.raw.dnote, 1)),
                     TooHighSample("D", soundPool.load(this, R.raw.dhighnote, 1)))
    samples ::= SoundTriplet("E",
      soundPool.load(this, R.raw.elownote, 1),
      soundPool.load(this, R.raw.enote, 1),
      soundPool.load(this, R.raw.ehighnote, 1))
    samples ::= SoundTriplet("F#",
      soundPool.load(this, R.raw.fsharplownote, 1),
      soundPool.load(this, R.raw.fsharpnote, 1),
      soundPool.load(this, R.raw.fsharphighnote, 1))
    samples ::= SoundTriplet("G",
      soundPool.load(this, R.raw.glownote, 1),
      soundPool.load(this, R.raw.gnote, 1),
      soundPool.load(this, R.raw.ghighnote, 1))
    samples ::= SoundTriplet("A",
      soundPool.load(this, R.raw.alownote, 1),
      soundPool.load(this, R.raw.anote, 1),
      soundPool.load(this, R.raw.ahighnote, 1))
    samples ::= SoundTriplet("B",
      soundPool.load(this, R.raw.blownote, 1),
      soundPool.load(this, R.raw.bnote, 1),
      soundPool.load(this, R.raw.bhighnote, 1))
    samples ::= SoundTriplet("C#",
      soundPool.load(this, R.raw.csharplownote, 1),
      soundPool.load(this, R.raw.csharpnote, 1),
      soundPool.load(this, R.raw.csharphighnote, 1))
    samples ::= SoundTriplet("D",
      soundPool.load(this, R.raw.d2lownote, 1),
      soundPool.load(this, R.raw.d2note, 1),
      soundPool.load(this, R.raw.d2highnote, 1))
    samples ::= SoundTriplet("E",
      soundPool.load(this, R.raw.e2lownote, 1),
      soundPool.load(this, R.raw.e2note, 1),
      soundPool.load(this, R.raw.e2highnote, 1))
    samples.flatten
  }

  private var currentSound: Option[SoundSample] = None

  private var stats = Statistics(0, 0)

  /** Called when the activity is first created. */
  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.main)
    statsView.setText(stats.humanFriendly)
    pickNewSound()
  }

  private def pickNewSound() {
    val pick = Utils.randomPick(sounds)
    currentSound = Some(pick)
    noteView.setText(pick.name)
  }

  private def rightInput() {
    stats = stats.rightInput()
    statsView.setText(stats.humanFriendly)
    pickNewSound()
    playCurrentSound()
  }

  private def wrongInput() {
    stats = stats.wrongInput()
    statsView.setText(stats.humanFriendly)
  }

  def tooLowSelected(view: View) {
    currentSound match {
      case None => ()
      case Some(sound) if sound.isTooLow =>
        rightInput()
      case _ =>
        wrongInput()
    }
  }

  def perfectSelected(view: View) {
    currentSound match {
      case None => ()
      case Some(sound) if sound.isPerfect =>
        rightInput()
      case _ =>
        wrongInput()
    }
  }

  def tooHighSelected(view: View) {
    currentSound match {
      case None => ()
      case Some(sound) if sound.isTooHigh =>
        rightInput()
      case _ =>
        wrongInput()
    }
  }

  def replaySound(view: View) {
    playCurrentSound()
  }

  def statsReset(view: View) {
    stats = Statistics(0, 0)
    statsView.setText(stats.humanFriendly)
  }

  def playCurrentSound() {
    currentSound match {
      case None => ()
      case Some(sound) =>
        soundPool.play(sound.id, volume, volume, 1, 0, 1f);
    }
  }
}