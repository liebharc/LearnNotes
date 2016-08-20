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

import scala.collection.immutable.LinearSeq
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.Duration
import scala.reflect.ClassTag

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

    onNewDataAvailable(lastData)
  }

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    lastData match {
      case Some(d) => display(d)
      case _ =>
        if (savedInstanceState != null) {
          val data = new ChartData
          data.load(savedInstanceState)
          lastData = Some(data)
          display(data)
        }
    }

    onNewDataAvailable(lastData)
  }

  override def onSaveInstanceState(bundle: Bundle): Unit = {
    super.onSaveInstanceState(bundle)
    lastData match {
      case Some(d) =>
        if (bundle != null) {
          d.store(bundle)
        }
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
    onNewDataAvailable(lastData)
  }

  protected def onNewDataAvailable(data: Option[ChartData]): Unit = {

  }

  private def display(data: ChartData): Unit = {
    val chart = pitchChart
    if (chart == null) {
      return
    }

    val white = 0xFFFFFFFF
    val red = ColorTemplate.COLORFUL_COLORS(0)
    val orange = ColorTemplate.COLORFUL_COLORS(1)
    val darkGreen = ColorTemplate.COLORFUL_COLORS(2)
    val formatter = new AddNoteNamesValueFormatter(data.noteNames)
    val ampDataSet = new LineDataSet(data.amplitudes, "Amplitudes")
    ampDataSet.setColor(orange)
    ampDataSet.setCircleColor(orange)
    val freqDiffDataSet = new LineDataSet(data.freqDiffs, "Pitch Error")
    freqDiffDataSet.setColor(red)
    freqDiffDataSet.setCircleColor(red)
    val speedDataSet = new LineDataSet(data.speed, "Speed")
    speedDataSet.setColor(darkGreen)
    speedDataSet.setCircleColor(darkGreen)
    val lineData = new LineData(combine(ampDataSet, freqDiffDataSet, speedDataSet))
    lineData.setValueFormatter(formatter)
    lineData.setValueTextColor(white)
    chart.setData(lineData)
    chart.setDescription("")
    chart.setDescriptionColor(white)
    chart.getXAxis.setTextColor(white)
    chart.getAxisLeft.setTextColor(white)
    chart.getAxisRight.setTextColor(white)
    chart.invalidate()
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
  val speed  = new util.ArrayList[Entry]
  val noteNames  = new util.ArrayList[String]

  def store(bundle: Bundle): Unit = {
    bundle.putFloatArray("amplitudes", conv(amplitudes).map(r => r.getY))
    bundle.putFloatArray("freqDiffs", conv(freqDiffs).map(r => r.getY))
    bundle.putFloatArray("speed", conv(speed).map(r => r.getY))
    bundle.putStringArray("noteNames", conv(noteNames))
  }

  def load(bundle: Bundle): Unit = {
    loadArray(bundle.getFloatArray("amplitudes"), amplitudes)
    loadArray(bundle.getFloatArray("freqDiffs"), freqDiffs)
    loadArray(bundle.getFloatArray("speed"), speed)
    val names = bundle.getStringArray("noteNames")
    if (names != null)
    {
      for (n <- names) {
        noteNames.add(n)
      }
    }
  }

  private def conv[T](arrayList: util.ArrayList[T])(implicit m: ClassTag[T]): Array[T] = {
    val result = new Array[T](arrayList.size())
    for (i <- 0 until arrayList.size()) {
      result(i) = arrayList.get(i)
    }

    result
  }

  private def loadArray(array: Array[Float], target: util.ArrayList[Entry]): Unit = {
    if (array == null) {
      return
    }

    var i = 0
    for (f <- array) {
      target.add(new Entry(i.toFloat, f))
      i += 1
    }
  }

  def averageSpeed(): Float = {
    if (this.speed.size() == 0) {
      return Float.NaN
    }

    val speed = conv(this.speed)
    val average = speed.map(e => e.getY).sum / speed.length
    val lessOutliners = speed.map(e => e.getY).filter(f => f > 0.5 * average && f < 2 * average)
    if (lessOutliners.length == 0) {
      return average
    }

    return  lessOutliners.sum / lessOutliners.length
  }

  def noteStats(): Map[String, (Int, Float, Float, Float)] = {
    if (freqDiffs.size() == 0) {
      return Map()
    }

    val notes = conv(noteNames).zip(conv(freqDiffs).map(e => e.getY))
    val noteGroups = notes.groupBy(n => n._1)
    val noteAverage = noteGroups.map{
      case (n, p) =>
        val count = p.length
        val avg = p.map(p => p._2).sum / p.length
        val rms = Math.sqrt(p.map(p => p._2 * p._2).sum  / p.length).toFloat
        val stddev = Math.sqrt(p.map(p => (p._2 - avg) * (p._2 - avg)).sum  / p.length).toFloat
        (n, (count, avg, rms, stddev))
    }
    noteAverage
  }

  def summaryString(): String = {
    def format(value: Float): String = {
      val integer = math.round(value)
      "%5d".format(integer)
    }

    val averageSpeed = this.averageSpeed()
    val speedString = "Average speed [bpm]: " + format(averageSpeed)

    val noteAverage =  noteStats()
    val noteString = noteAverage
        .toList
        .sortWith{ case (a, b) => a._2._1 > b._2._1 }
        .map{
          case (n, p) =>
            val count = format(p._1.toFloat)
            val avg = format(p._2)
            val rms = format(p._3)
            val stddev = format(p._4)
            n + ":" + count + ", " + avg + ", " + rms + ", " + stddev}
      .mkString("\n")
    speedString + "\n" + "Note: COUNT, AVG, RMS, STDDEV\n" + noteString
  }
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
    val targetIntervalInS = 1.0 / 8.0 /* [s] */
    Log.i(LogTag, "Running analysis with " + sampleRate + " Sa/s")
    recorder.startRecording()
    val size = Math.max(BufferElements, closestPowerOfTwo(targetIntervalInS * sampleRate))
    val data = new Array[Short](size)
    val real = new Array[Double](size)
    val analysis = new FrameAnalysis(sampleRate, size)
    val frames:ListBuffer[FrameResult] = ListBuffer()
    var frame = 0
    while (active) {
      recorder.read(data, 0, size)
      for (i <- 0 until size) {
        real(i) = data(i).toDouble
      }

      val result = analysis.analyse(real)
      frames.append(result)
      frame += 1
    }
    recorder.stop()
    recorder.release()

    val multiFrameAnalysis = new MultiFrameAnalysis(sampleRate, size)
    val result = multiFrameAnalysis.analyse(frames.toList)
    copyToArrayList(result.map(r => r.amplitude), chartData.amplitudes)
    copyToArrayList(result.map(r => r.deltaFrequency), chartData.freqDiffs)
    copyToArrayList(result.map(r => r.speed), chartData.speed)
    copyNameToArrayList(result.map(r => r.note), chartData.noteNames)
  }

  private def copyToArrayList(list: List[Double], target: util.ArrayList[Entry]): Unit = {
    var i = 0
    for (l <- list) {
      target.add(new Entry(i.toFloat, l.toFloat))
      i += 1
    }
  }

  private def copyNameToArrayList(list: List[String], target: util.ArrayList[String]): Unit = {
    for (l <- list) {
      target.add(l)
    }
  }

  def closestPowerOfTwo(number: Double): Int = {
    Math.round(Math.pow(2, Math.ceil(Math.log(number)/Math.log(2)))).toInt
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

class FrameAnalysis(sampleRate: Double, length: Int) {
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
      1000 * diff / frequency,
      note,
      10 * Math.log10(2 * amplitude)) // Factor 2, since we only consider half the spectrum and thus only half the power
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

object MultiFrameResult {
  def apply(result: FrameResult, speed: Double): MultiFrameResult = {
    MultiFrameResult(result.frequency, result.deltaFrequency, result.note, result.amplitude, speed)
  }
}

case class MultiFrameResult(frequency: Double, deltaFrequency: Double, note: String, amplitude: Double, speed: Double)

class MultiFrameAnalysis(sampleRate: Double, frameLength: Int) {
  val timePerFrame = frameLength / sampleRate

  def analyse(frames: LinearSeq[FrameResult]): List[MultiFrameResult] = {
    if (frames.length < 5) {
      return frames.map(r => MultiFrameResult(r, Double.NaN)).toList
    }

    val peaks = amplitudePeakDetect(frames)
    val speed = diffDiv(peaks, 60 / timePerFrame)
    val atNotePos =
      peaks
        .zip(speed)
        .map{case (i, s) => MultiFrameResult(frames(i), s)}
    atNotePos
  }

  private def amplitudePeakDetect(frames: LinearSeq[FrameResult]): List[Int] = {
    var maxValue = frames(1).amplitude
    val peaks: ListBuffer[Int] = ListBuffer()
    for (i <- 1 until frames.length - 1) {
      if (frames(i).amplitude > frames(i - 1).amplitude && frames(i).amplitude > frames(i + 1).amplitude) {
        if (frames(i).amplitude > maxValue) {
          maxValue = frames(i).amplitude
        }

        peaks.append(i)
      }
    }

    // Filter out quite parts, likely noise
    peaks
      .filter(i => frames(i).amplitude > maxValue * 0.6)
      .toList
  }

  private def diffDiv(data: List[Int], factor: Double): List[Double] = {
    val result: ListBuffer[Double] = ListBuffer()
    result.append(factor / data(0))
    for (i <- 0 until data.length - 1) {
      result.append(factor / (data(i + 1) - data(i)))
    }

    result.toList
  }
}