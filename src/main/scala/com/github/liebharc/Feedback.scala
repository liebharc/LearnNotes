package com.github.liebharc

import java.util

import android.app.Activity
import android.media.{AudioFormat, AudioRecord, MediaRecorder}
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.{LayoutInflater, View, ViewGroup}
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data._
import com.meapsoft.FFT

import scala.concurrent.duration.Duration

trait FeedbackBehaviour extends Activity with TypedFindView {
  private def pitchChart: LineChart = findView(TR.pitch)

  private var audioRecord: Option[AnalysisLoop] = None

  def initializeFeedback(): Unit = {
    val entries  = new util.ArrayList[Entry]
    for (i <- 0 until 5) {
      entries .add(new Entry(i.toFloat, i))
    }

    val dataSet = new LineDataSet(entries, "Label")
    val xLabels  = new util.ArrayList[String]
    xLabels.add("a")
    xLabels.add("b")
    xLabels.add("c#")
    xLabels.add("d")
    xLabels.add("e")
    val lineData = new LineData(xLabels, dataSet)
    pitchChart.setData(lineData)
  }

  def startAnalysis(): Unit = {
    stopAnalysis()
    val analysis = new AnalysisLoop(pitchChart)
    audioRecord = Some(analysis)
    val thread = new java.lang.Thread(analysis)
    thread.start()
  }

  def stopAnalysis(): Unit = {
    audioRecord match {
      case Some(r) => r.abort()
      case _ => ()
    }

    audioRecord = None
  }
}

class FeedbackFragment extends Fragment {
  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    super.onCreateView(inflater, container, savedInstanceState)
    val view = inflater.inflate(R.layout.feedback, container, false)
    return view
  }

  override def onViewCreated(view: View, savedInstanceState: Bundle) = {
    super.onViewCreated(view, savedInstanceState)
    val typedActivity = getActivity.asInstanceOf[FeedbackBehaviour]
    typedActivity.initializeFeedback()
  }
}/*
case class AudioSettings(sampleRate: Double, channel: Int, encoding: Int) {
  def bytesPerSample = if (encoding == AudioFormat.ENCODING_PCM_16BIT) { 2 } else { 1 }
}*/

class AnalysisLoop(amplitudesChart: LineChart) extends java.lang.Runnable {
  private val SampleRates = List(8000, 11025, 22050, 44100) // Try to find a low as possible sample rate
  private val Channel = AudioFormat.CHANNEL_IN_MONO
  private val Encoding = AudioFormat.ENCODING_PCM_16BIT

  private val BufferElements = 1024 // want to play 2048 (2K) since 2 bytes we use only 102

  private var active = true

  def abort(): Unit = {
    active = false
  }

  private def attemptToCreateAnAudioRecord(): Option[AudioRecord] = {
    // Next line should be MediaRecorder.AudioSource.MIC but that doesn't work.
    // That is because AudioSource is a non-static inner class of MediaRecorder
    // therefore Scala doesn't allow to access AudioSource in a static context even
    // if we are looking for a const value.
    // I hardly can blame Scala for this, this API design looks really strange.
    // Not even the constructors are private (instead they would throw a RuntimeException) if someone
    // tries to instantiate them. Doing some more research a lot of the early Android decision were
    // driven by performance considerations. Not sure if this is one of them.
    val mic = 1

    for (audioFormat <- List(AudioFormat.ENCODING_PCM_16BIT, AudioFormat.ENCODING_PCM_8BIT);
         channelConfig <- List(AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_IN_STEREO);
         sampleRate <- SampleRates) {
      try {
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
        Log.d("Violin Feedback", "Attempting rate " + sampleRate + "Hz, bits: " + audioFormat + ", channel: " + channelConfig);
        if (bufferSize != AudioRecord.ERROR_BAD_VALUE && bufferSize <= BufferElements) {
          // check if we can instantiate and have a success
          val recorder = new AudioRecord(mic, sampleRate, channelConfig, audioFormat, BufferElements);

          if (recorder.getState() == AudioRecord.STATE_INITIALIZED) {
            return Some(recorder)
          }
        }
      }
      catch {
        case e: Exception => Log.e("Violin Feedback", sampleRate + "Exception, keep trying.", e);
      }
    }

    return None
  }

  override def run(): Unit = {
    val recorderOption = attemptToCreateAnAudioRecord()
    if (recorderOption.isEmpty) {
      return
    }

    val recorder = recorderOption.get
    recorder.startRecording()
    val data = new Array[Short](BufferElements)
    val real = new Array[Double](BufferElements)
    val imag = new Array[Double](BufferElements)
    val anlysis = new Analysis(BufferElements)
    val amplitudes  = new util.ArrayList[Entry]
    val noteNames  = new util.ArrayList[String]
    var frame = 0
    while (active) {
      recorder.read(data, 0, BufferElements);
      for (i <- 0 until BufferElements) {
        real(i) = data(i).toDouble
        imag(i) = 0.0f
      }

      val amplitude = anlysis.analyse(real, imag)
      amplitudes.add(new Entry(amplitude.toFloat, frame))
      noteNames.add("a")
      frame += 1
    }
    recorder.stop()
    recorder.release()

    val dataSet = new LineDataSet(amplitudes, "Amplitudes")
    val lineData = new LineData(noteNames, dataSet)
    amplitudesChart.setData(lineData)
  }
}

class Analysis(length: Int) {
  private val fft = new FFT(length)

  def analyse(real: Array[Double], imag: Array[Double]): Double = {
    fft.fft(real, imag)
    for (i <- 0 until length) {
      real(i) = Math.sqrt(real(i) * real(i) + imag(i) * imag(i))
      fft.fft(real, imag)
    }

    val (_, amplitude) = findPeak(real)
    amplitude
  }

  private def findPeak(magnitude: Array[Double]): (Int, Double) = {
    var maxIdx = 0
    var maxValue = magnitude(maxIdx)
    for (i <- maxIdx + 1 until length) {
      if (magnitude(i) > i) {
        maxIdx = i
        maxValue = magnitude(maxIdx)
      }
    }

    (maxIdx, maxValue)
  }
}