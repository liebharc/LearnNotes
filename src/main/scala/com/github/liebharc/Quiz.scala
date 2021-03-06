package com.github.liebharc

import android.app.Activity
import android.graphics.{Bitmap, BitmapFactory}
import android.os._
import android.support.v4.app.Fragment
import android.view.{LayoutInflater, View, ViewGroup}

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
  val bitmap: Bitmap
  def isTooLow: Boolean
  def isTooHigh: Boolean
  def isPerfect: Boolean
  def storageId = name + bit(isTooLow) + bit(isPerfect) + bit(isTooHigh)

  private def bit(value: Boolean) = if (value) "1" else "0"
}

case class TooLowSample(name: String, bitmap: Bitmap, id: Int) extends SoundSample {
  override def isTooHigh: Boolean = false
  override def isTooLow: Boolean = true
  override def isPerfect: Boolean = false
}

case class TooHighSample(name: String, bitmap: Bitmap, id: Int) extends SoundSample {
  override def isTooHigh: Boolean = true
  override def isTooLow: Boolean = false
  override def isPerfect: Boolean = false
}

case class PerfectSample(name: String, bitmap: Bitmap, id: Int) extends SoundSample {
  override def isTooHigh: Boolean = false
  override def isTooLow: Boolean = false
  override def isPerfect: Boolean = true
}

object SoundTriplet {
  def apply(name: String, bitmap: Bitmap, tooLow: Int, perfect: Int, tooHigh: Int): List[SoundSample]
  = List(TooLowSample(name, bitmap, tooLow), PerfectSample(name, bitmap, perfect), TooHighSample(name, bitmap, tooHigh))
}

trait QuizBehaviour
  extends Activity
    with TypedFindView
    with SoundPoolProvider {

  private val volume = 1.0f

  private def noteImageView = findView(TR.noteimage)

  private def statsView = findView(TR.stats)

  private lazy val sounds = {
    var samples: List[List[SoundSample]] = Nil
    def loadImage(id: Int): Bitmap = BitmapFactory.decodeStream(getResources().openRawResource(id))
    def loadSound(id: Int): Int = soundPool.load(this, id, 1)
    val dNoteImage = loadImage(R.raw.imgnoted3)
    samples ::= List(PerfectSample("D", dNoteImage, loadSound(R.raw.dnote)),
      TooHighSample("D", dNoteImage, loadSound(R.raw.dhighnote)))
    samples ::= SoundTriplet("E",
      loadImage(R.raw.imgnotee3),
      loadSound(R.raw.elownote),
      loadSound(R.raw.enote),
      loadSound(R.raw.ehighnote))
    samples ::= SoundTriplet("F#",
      loadImage(R.raw.imgnotef3),
      loadSound( R.raw.fsharplownote),
      loadSound(R.raw.fsharpnote),
      loadSound(R.raw.fsharphighnote))
    samples ::= SoundTriplet("G",
      loadImage(R.raw.imgnoteg3),
      loadSound(R.raw.glownote),
      loadSound(R.raw.gnote),
      loadSound(R.raw.ghighnote))
    samples ::= SoundTriplet("A",
      loadImage(R.raw.imgnotea3),
      loadSound(R.raw.alownote),
      loadSound(R.raw.anote),
      loadSound(R.raw.ahighnote))
    samples ::= SoundTriplet("B",
      loadImage(R.raw.imgnoteb3),
      loadSound(R.raw.blownote),
      loadSound(R.raw.bnote),
      loadSound(R.raw.bhighnote))
    samples ::= SoundTriplet("C#",
      loadImage(R.raw.imgnotec4),
      loadSound(R.raw.csharplownote),
      loadSound(R.raw.csharpnote),
      loadSound(R.raw.csharphighnote))
    samples ::= SoundTriplet("D",
      loadImage(R.raw.imgnoted4),
      loadSound(R.raw.d2lownote),
      loadSound(R.raw.d2note),
      loadSound(R.raw.d2highnote))
    samples ::= SoundTriplet("E",
      loadImage(R.raw.imgnotee4),
      loadSound(R.raw.e2lownote),
      loadSound(R.raw.e2note),
      loadSound(R.raw.e2highnote))
    samples.flatten
  }

  private var currentSound: Option[SoundSample] = None

  private var stats = Statistics(0, 0)

  override def onSaveInstanceState(bundle: Bundle): Unit = {
    super.onSaveInstanceState(bundle)
    currentSound match {
      case Some(s) => bundle.putString("currentSound", s.storageId)
      case _ => ()
    }

    bundle.putInt("wrongGuess", stats.wrong)
    bundle.putInt("rightGuess", stats.right)
  }

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    if (savedInstanceState != null) {
      val wrong = savedInstanceState.getInt("wrongGuess")
      val right = savedInstanceState.getInt("rightGuess")
      stats = Statistics(right, wrong)
      val soundId = savedInstanceState.getString("currentSound")
      val selectedSound = sounds.find(r => r.storageId == soundId)
      currentSound = selectedSound
    }
  }

  private def pickNewSound(): Unit =  {
    val pick = Utils.randomPick(sounds)
    currentSound = Some(pick)
    noteImageView.setImageBitmap(pick.bitmap)
  }

  private def rightInput(): Unit =  {
    stats = stats.rightInput()
    statsView.setText(stats.humanFriendly)
    pickNewSound()
    playCurrentSound()
  }

  private def wrongInput(): Unit =  {
    stats = stats.wrongInput()
    statsView.setText(stats.humanFriendly)
  }

  def tooLowSelected(view: View): Unit = {
    currentSound match {
      case None => ()
      case Some(sound) if sound.isTooLow =>
        rightInput()
      case _ =>
        wrongInput()
    }
  }

  def perfectSelected(view: View): Unit =  {
    currentSound match {
      case None => ()
      case Some(sound) if sound.isPerfect =>
        rightInput()
      case _ =>
        wrongInput()
    }
  }

  def tooHighSelected(view: View): Unit =  {
    currentSound match {
      case None => ()
      case Some(sound) if sound.isTooHigh =>
        rightInput()
      case _ =>
        wrongInput()
    }
  }

  def replaySound(view: View): Unit = {
    playCurrentSound()
  }

  def statsReset(view: View): Unit = {
    stats = Statistics(0, 0)
    statsView.setText(stats.humanFriendly)
  }

  def playCurrentSound(): Unit = {
    currentSound match {
      case None => ()
      case Some(sound) =>
        soundPool.play(sound.id, volume, volume, 1, 0, 1f);
    }
  }

  def initializeBehaviour(): Unit = {
    statsView.setText(stats.humanFriendly)
    if (currentSound.isEmpty) {
      pickNewSound()
    } else {
      val sound = currentSound.get
      noteImageView.setImageBitmap(sound.bitmap)
    }
  }
}

class QuizFragment extends Fragment {
  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    super.onCreateView(inflater, container, savedInstanceState)
    inflater.inflate(R.layout.quiz, container, false)
  }

  override def onViewCreated(view: View, savedInstanceState: Bundle): Unit =  {
    super.onViewCreated(view, savedInstanceState)
    val typedActivity = getActivity.asInstanceOf[QuizBehaviour]
    typedActivity.initializeBehaviour()
  }
}