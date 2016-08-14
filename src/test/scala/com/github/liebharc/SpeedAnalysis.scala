package com.github.liebharc

import java.io.File

import co.uk.labbookpages.WavFile
import org.scalatest.FunSpec

import scala.collection.mutable.ListBuffer

class SpeedAnalysis extends FunSpec {
  def getWaveFile(name: String) = {
    val cwd = new java.io.File(".").getCanonicalPath
    new File(cwd + "/src/test/res/raw/" + name)
  }

  describe("speed analysis") {
    it("should estimate the speed of a piece") {
      // SwallowtailJig.wav is part of a fiddle tutorial created by Katy Adelson
      // katyadelson.com
      val wavFile = WavFile.openWavFile(getWaveFile("SwallowtailJig.wav"))
      val size = 8192
      val buffer = new Array[Array[Double]](2)
      for (i <- 0 until buffer.length) {
        buffer(i) = new Array[Double](size)
      }

      val analysis = new FrameAnalysis(44100, size)

      var framesRead = 0
      var frames: ListBuffer[FrameResult] = ListBuffer()
      do {
        framesRead = wavFile.readFrames(buffer, size)
        if (framesRead == size) {
          frames = frames :+ analysis.analyse(buffer(0))
        }

      } while (framesRead != 0)

      val speed = new MultiFrameAnalysis(44100, size)
      val result: List[MultiFrameResult] = speed.analyse(frames.toList)
      val averageSpeed = result.map(r => r.speed).sum / result.length
      assert(averageSpeed > 90.0 && averageSpeed < 120)
    }
  }
}
