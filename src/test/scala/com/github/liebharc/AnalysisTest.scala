package com.github.liebharc

import org.scalatest.FunSpec
import java.io.File

import co.uk.labbookpages.WavFile

class AnalysisSpec extends FunSpec {

  def getWaveFile(name: String) = {
    val cwd = new java.io.File(".").getCanonicalPath
    new File(cwd + "/src/main/res/raw/" + name)
  }

  describe("A lib exploration test") {
    it("should read a wav file") {
      val wavFile = WavFile.openWavFile(getWaveFile("ahighnote.wav"))
      val buffer = new Array[Double](100)
      var framesRead = 0
      do {
        framesRead = wavFile.readFrames(buffer, buffer.length)
        // do something with buffer

      } while (framesRead != 0)

      wavFile.close()
    }
  }
}