package com.github.liebharc

import android.app.Activity
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.{LayoutInflater, View, ViewGroup}

import scala.concurrent.duration.Duration

trait ControlBehaviour
  extends Activity
    with TypedFindView
    with SoundPoolProvider
    with FeedbackBehaviour {
  private var isRunning: Boolean = false

  /*
    Metronome sound: http://soundbible.com/914-Metronome.html

    Main Page > All Sounds > Metronome Sound Effect > [share]
    Metronome Sound Recorded by Mike Koenig

    Download Wav Metronome Downlaod MP3 Metronome Downlaod Compressed Zip Metronome
    Title: Metronome
    About: Sound of a metronome clicking and keeping time.
    this is a wood block metranome sound and is pretty standard.
    if you want to change the speed or pitch of this please be sure to visit the sound
    editing link on the bottom right of the page after downloading.
    this sound was requested by sam.
   */
  private lazy val metronomeSound = soundPool.load(this, R.raw.metronome, 1)

  private def runStopButton = findView(TR.buttonStartStopAnalysis)

  private def metronomeSpeed = findView(TR.metrospeed)

  private var metronomeSaved = ""

  def initializeControl(): Unit = {
    updateControlGuiStatus()
    if (metronomeSaved != null
      && !metronomeSaved.isEmpty
      && metronomeSpeed != null) {
      metronomeSpeed.setText(metronomeSaved)
    }
  }

  override def onSaveInstanceState(bundle: Bundle): Unit = {
    super.onSaveInstanceState(bundle)
    if (metronomeSpeed != null) {
      bundle.putString("metronome", metronomeSpeed.getText.toString)
    }
  }

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    if (savedInstanceState != null) {
      metronomeSaved = savedInstanceState.getString("metronome")
    }
  }

  private def updateControlGuiStatus(): Unit = {
    val text = if (isRunning) "Stop" else "Run"
    runStopButton.setText(text)
    metronomeSpeed.setEnabled(!isRunning)
  }

  private var currentMetronome: Option[Metronome] = None

  private def stopMetronome(): Unit = {
    currentMetronome match {
      case Some(m) => m.abort()
      case _ => ()
    }
    currentMetronome = None
  }

  def runStopFeedback(view: View): Unit = {
    isRunning = !isRunning
    if (isRunning) {
      stopMetronome()
      val speedString = metronomeSpeed.getText.toString
      if (speedString != null && !speedString.isEmpty) {
        val bpm = metronomeSpeed.getText.toString.toInt
        if (bpm > 0) {
          val bps = bpm.toDouble / 60.0
          val soundDuration = Duration(176, "ms")
          val waitInterval = 1000.0 / bps.toDouble
          val duration = Duration(waitInterval.toLong, "ms") - soundDuration
          if (duration.toMillis > 0) {
            val metronome = new Metronome(duration, this, metronomeSound)
            currentMetronome = Some(metronome)
            val thread = new java.lang.Thread(metronome)
            thread.start()
          }
        }
      }

      startAnalysis()
    }
    else {
      stopMetronome()
      stopAnalysis()
    }

    updateControlGuiStatus()
  }
}

class Metronome(duration: Duration, pool: SoundPoolProvider, soundId: Int) extends java.lang.Runnable {
  private var active = true

  def abort(): Unit = {
    active = false
  }

  override def run(): Unit = {
    while(active) {
      pool.soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
      Thread.sleep(duration.toMillis)
    }
  }
}

class ControlFragment extends Fragment {
  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    super.onCreateView(inflater, container, savedInstanceState)
    inflater.inflate(R.layout.control, container, false)
  }

  override def onViewCreated(view: View, savedInstanceState: Bundle): Unit = {
    super.onViewCreated(view, savedInstanceState)
    val typedActivity = getActivity.asInstanceOf[ControlBehaviour]
    typedActivity.initializeControl()
  }
}