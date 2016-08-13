package com.github.liebharc

import java.text.DecimalFormat
import java.util

import android.app.Activity
import android.media.{AudioFormat, AudioRecord}
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.{LayoutInflater, View, ViewGroup}
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data._
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.utils.{ColorTemplate, ViewPortHandler}
import com.meapsoft.FFT

import scala.concurrent.duration.Duration

trait FeedbackBehaviour extends Activity with TypedFindView {
  private def pitchChart: LineChart = findView(TR.pitch)

  private var audioRecord: Option[AnalysisLoop] = None

  private var currentThread: Option[Thread] = None

  private var lastData: Option[ChartData] = None

  def initializeFeedback(): Unit = {
    lastData match {
      case Some(d) => display(d)
      case _ => ()
    }
  }

  def startAnalysis(): Unit = {
    stopAnalysis()
    val analysis = new AnalysisLoop()
    audioRecord = Some(analysis)
    val thread = new java.lang.Thread(analysis)
    thread.start()
    currentThread = Some(thread)
  }

  def stopAnalysis(): Unit = {
    audioRecord match {
      case Some(r) =>
        r.abort()
        joinThread()
        lastData = Some(r.chartData)
        display(r.chartData)
      case _ => ()
    }

    audioRecord = None
  }

  private def display(data: ChartData): Unit = {
    val white = 0xFFFFFFFF
    val red = ColorTemplate.COLORFUL_COLORS(0)
    val orange  = ColorTemplate.COLORFUL_COLORS(1)
    val formatter = new AddNoteNamesValueFormatter(data.noteNames)
    val ampDataSet = new LineDataSet(data.amplitudes, "Amplitudes")
    ampDataSet.setColor(orange)
    ampDataSet.setCircleColor(orange)
    val freqDiffDataSet = new LineDataSet(data.freqDiffs, "Pitch Error")
    freqDiffDataSet.setColor(red)
    freqDiffDataSet.setCircleColor(red)
    val lineData = new LineData(combine(ampDataSet, freqDiffDataSet))
    lineData.setValueFormatter(formatter)
    lineData.setValueTextColor(white)
    pitchChart.setData(lineData)
    pitchChart.setDescription("")
    pitchChart.setDescriptionColor(white)
    pitchChart.getXAxis.setTextColor(white)
    pitchChart.getAxisLeft.setTextColor(white)
    pitchChart.getAxisRight.setTextColor(white)
    pitchChart.invalidate()
  }

  private def combine(dataSets: LineDataSet*): util.List[ILineDataSet] = {
    val result = new util.ArrayList[ILineDataSet](dataSets.length)
    for (set <- dataSets) {
      result.add(set)
    }

    result
  }

  private def joinThread(): Unit = {
    currentThread match {
      case Some(t) => t.join(Duration(10, "s").toMillis)
      case _ => ()
    }
  }
}

class FeedbackFragment extends Fragment {
  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    super.onCreateView(inflater, container, savedInstanceState)
    inflater.inflate(R.layout.feedback, container, false)
  }

  override def onViewCreated(view: View, savedInstanceState: Bundle) = {
    super.onViewCreated(view, savedInstanceState)
    val typedActivity = getActivity.asInstanceOf[FeedbackBehaviour]
    typedActivity.initializeFeedback()
  }
}

class ChartData {
  val amplitudes  = new util.ArrayList[Entry]
  val freqDiffs  = new util.ArrayList[Entry]
  val noteNames  = new util.ArrayList[String]
}

class AnalysisLoop() extends java.lang.Runnable {
  private val SampleRates = List(11025, 8000, 22050, 44100) // Try to find a low sample rate first
  private val BufferElements = 1024 // Number of double elements
  private val LogTag = "Violin Feedback"
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

    for (audioFormat <- List(AudioFormat.ENCODING_PCM_16BIT); // Don't try AudioFormat.ENCODING_PCM_8BIT since that would change our types later
         channelConfig <- List(AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_IN_STEREO);
         sampleRate <- SampleRates) {
      try {
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        Log.d(LogTag, "Attempting rate " + sampleRate + "Hz, bit format: " + audioFormat + ", channel: " + channelConfig)
        if (bufferSize != AudioRecord.ERROR_BAD_VALUE && bufferSize <= BufferElements) {
          // check if we can instantiate and have a success
          val recorder = new AudioRecord(mic, sampleRate, channelConfig, audioFormat, BufferElements)

          if (recorder.getState == AudioRecord.STATE_INITIALIZED) {
            Log.d(LogTag, "Success with rate " + sampleRate + "Hz, bit format: " + audioFormat + ", channel: " + channelConfig)
            return Some(recorder)
          }
        }
      }
      catch {
        case e: Exception => Log.e(LogTag, sampleRate + "Exception, keep trying.", e);
      }
    }

    None
  }

  val chartData = new ChartData()

  override def run(): Unit = {
    val recorderOption = attemptToCreateAnAudioRecord()
    if (recorderOption.isEmpty) {
      return
    }

    val recorder = recorderOption.get
    val sampleRate = recorder.getSampleRate
    val targetIntervalInS = 0.125 /* [s] */
    Log.i(LogTag, "Running analysis with " + sampleRate + " Sa/s")
    recorder.startRecording()
    val size = Math.max(BufferElements, closestPowerOfTwo(targetIntervalInS * sampleRate))
    val data = new Array[Short](size)
    val real = new Array[Double](size)
    val analysis = new Analysis(sampleRate, size)

    var frame = 0
    while (active) {
      recorder.read(data, 0, size)
      for (i <- 0 until size) {
        real(i) = data(i).toDouble
      }

      val result = analysis.analyse(real)
      chartData.amplitudes.add(new Entry(frame, result.amplitude.toFloat))
      chartData.freqDiffs.add(new Entry(frame, result.deltaFrequency.toFloat))
      chartData.noteNames.add(result.note)
      frame += 1
    }
    recorder.stop()
    recorder.release()
  }

  def closestPowerOfTwo(number: Double): Int = {
    Math.round(Math.pow(2, Math.ceil((Math.log(number)/Math.log(2))))).toInt
  }
}

class AddNoteNamesValueFormatter(val notes: util.ArrayList[String]) extends ValueFormatter {
 private val decimalFormat: DecimalFormat = new DecimalFormat("########0") // Integer part only

  override def getFormattedValue(value: Float, entry: Entry, dataSetIndex: Int, viewPortHandler: ViewPortHandler): String = {
    val index = Math.round(entry.getX)
    var note = "?"
    if (index >= 0 && index < notes.size()){
      val possibleNote = notes.get(index)
      if (!possibleNote.isEmpty) {
        note = possibleNote
      }
    }

    note + ": " + decimalFormat.format(value)
  }
}

object NoteFrequencies {
  // Source: http://www.phy.mtu.edu/~suits/notefreqs.html
  private val nameAndFreq: List[(String, Double)] = List(
    "G3" -> 196.0,
    "A3" -> 220.0,
    "B3" -> 246.94,
    "C4" -> 261.63,
    "C#4" -> 277.18,
    "D4" -> 293.66,
    "E4" -> 329.63,
    "F4" -> 349.23,
    "F#4" -> 369.99,
    "G4" -> 392.0,
    "A4" -> 440.0,
    "B4" -> 493.88,
    "C5" -> 523.25,
    "C#5" -> 554.37,
    "D5" -> 587.33,
    "E5" -> 659.25,
    "F5" -> 698.46,
    "F#5" -> 739.99,
    "G5" -> 783.99,
    "A5" -> 880.00,
    "B5" -> 987.77
  )

  private val frequencies: List[Double] = nameAndFreq.map(x => x._2)

  private val names: List[String] = nameAndFreq.map(x => x._1)

  private val tolerance = 0.8

  val lowerBound: Double = frequencies.head * tolerance

  val upperBound: Double = frequencies.last / tolerance

  private val noResult = ("", 0.0)

  def findClosestNote(freq: Double): (String, Double) = {
    // The first two checks filter out completely unreasonable frequencies
    if (freq < lowerBound) {
      return noResult
    }

    if (freq > upperBound) {
      return noResult
    }

    if (freq <= frequencies.head)
    {
      return calcDiff(freq, 0)
    }

    if (freq >= frequencies.last)
    {
      return calcDiff(freq, frequencies.length - 1)
    }

    for (i <- 1 until frequencies.length) {
      if (freq >= frequencies(i - 1) && freq <= frequencies(i)) {
        val diffLower = freq - frequencies(i - 1)
        val diffHigher = frequencies(i) - freq
        if (diffLower < diffHigher) {
          return calcDiff(freq, i - 1)
        }
        else {
          return calcDiff(freq, i)
        }

      }
    }

    noResult
  }

  private def calcDiff(freq: Double, index: Int): (String, Double) = {
    (names(index), freq - frequencies(index))
  }
}

case class FrameResult(frequency: Double, deltaFrequency: Double, note: String, amplitude: Double)

class Analysis(sampleRate: Double, length: Int) {
  private val fft = new FFT(length)
  private val imag = new Array[Double](length)
  private val rbw = sampleRate / length

  def analyse(real: Array[Double]): FrameResult = {
    for (i <- 0 until length) {
      imag(i) = 0.0
    }

    fft.fft(real, imag)
    // The FFT doesn't do a FFT shift. Since the data is time domain data is real
    // the spectrum will be symmetric. We can therefore ignore the 2nd half
    // completely
    val spectrumLength = length / 2
    for (i <- 0 until spectrumLength) {
      real(i) = Math.sqrt(real(i) * real(i) + imag(i) * imag(i))
    }

    val lowerBound = Math.round(NoteFrequencies.lowerBound / rbw).toInt
    val upperBound = Math.round(NoteFrequencies.upperBound / rbw).toInt
    val (peakidx, amplitude) =
      findPeak(
        real,
        Math.max(0, lowerBound),
         Math.min(spectrumLength - 1, upperBound))
    val frequency = peakidx * rbw
    val (note, diff) = NoteFrequencies.findClosestNote(frequency)

    FrameResult(
      frequency,
      100.0 * diff / frequency,
      note,
      10 * Math.log10(2 * amplitude))
  }

  private def findPeak(magnitude: Array[Double], lowerBound: Int, uppperBound: Int): (Int, Double) = {
    var maxIdx = lowerBound
    var maxValue = magnitude(maxIdx)
    for (i <- maxIdx + 1 to uppperBound) {
      if (magnitude(i) > maxValue) {
        maxIdx = i
        maxValue = magnitude(maxIdx)
      }
    }

    (maxIdx, maxValue)
  }
}