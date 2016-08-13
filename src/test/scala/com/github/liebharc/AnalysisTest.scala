package com.github.liebharc

import java.io.File

import co.uk.labbookpages.WavFile
import com.meapsoft.FFT
import org.scalatest.FunSpec

class AnalysisSpec extends FunSpec {

  def getWaveFile(name: String) = {
    val cwd = new java.io.File(".").getCanonicalPath
    new File(cwd + "/src/main/res/raw/" + name)
  }

  describe("A lib exploration test") {
    it("should read a wav file") {
      val wavFile = WavFile.openWavFile(getWaveFile("ahighnote.wav"))
      val size = 128
      val buffer = new Array[Array[Double]](2)
      for (i <- 0 until buffer.length) {
        buffer(i) = new Array[Double](size)
      }

      var framesRead = 0
      var frameCount = 0
      var frameWithSound = 0
      val fft = new FFT(buffer.length)
      do {
        framesRead = wavFile.readFrames(buffer, size)
        val imag = new Array[Double](size)
        fft.fft(buffer(0), imag)
        val sum = buffer(0).zip(imag).map(a => Math.sqrt(a._1 * a._1 + a._2 * a._2)).sum
        if (sum > 0.1) {
          frameWithSound += 1
        }
        // do something with buffer
        frameCount += 1
      } while (framesRead != 0)

      wavFile.close()

      val ratio = frameWithSound.toDouble / frameCount.toDouble
      assert(ratio > 0.05)
    }
  }
}