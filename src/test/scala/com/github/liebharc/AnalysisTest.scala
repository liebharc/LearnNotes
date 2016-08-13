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

  describe("Sound analysis") {
    it("should recognize a amplitude and frequency of a single tone (in tune)") {
      val wavFile = WavFile.openWavFile(getWaveFile("ahighnote.wav"))
      val size = 8192
      val buffer = new Array[Array[Double]](2)
      for (i <- 0 until buffer.length) {
        buffer(i) = new Array[Double](size)
      }

      val analysis = new Analysis(44100, buffer.length)

      var frameCount = 0
      var frameWithSound = 0
      var framesRead = 0
      do {
        framesRead = wavFile.readFrames(buffer, size)
        val amplitude = analysis.analyse(buffer(0))
        if (amplitude > -60 /* [dB] */) {
          frameWithSound += 1
        }
        // do something with buffer
        frameCount += 1
      } while (framesRead != 0)

      wavFile.close()

      val ratio = frameWithSound.toDouble / frameCount.toDouble
      assert(ratio > 0.05)
    }

    it("should be able to deal with pure noise") {
      // Noise data comes from an actual device in a quite environment
      val fs = 8000
      val noise: Array[Double] = List(-2,-15,-30,42,106,50,-117,-47,6,-10,45,17,-11,114,66,-122,-119,-17,98,49,-95,-110,-2,153,28,-102,-142
        ,18,113,102,-79,-231,1,165,94,-78,0,16,30,-19,-49,10,-26,112,20,-61,-73,-29,69,28,49,-61,42,-98,-91,50,9,100,-42,5,-34,45,-61,-61,53,21,16,46,93,-67,-59,-82,
        32,20,22,33,-11,-45,-22,-5,-14,68,-21,-73,68,-46,-17,-10,-6,77,-46,-41,57,-6,-27,-49,-43,76,41,16,-26,-35,13,10,-51,-57,18,48,25,32,-81,70,65,4,-97,-135,-9,97,
        14,14,2,-58,176,-6,-33,-103,-21,14,65,22,-38,-45,-50,8,-17,54,65,-26,-93,-38,-17,156,88,-30,-53,-101,9,-6,81,-34,21,38,-73,-50,-10,5,-33,46,16,-10,-35,36,2,13
        ,8,8,-50,-17,-50,-30,108,20,38,-9,-6,-46,-78,-30,93,30,14,-38,-69,-17,-18,-5,64,36,6,18,-59,-21,89,6,44,-61,-27,20,-41,25,-38,18,-35,-49,24,13,-3,-51,81,-5,33
        ,2,25,-77,-67,34,-29,156,-63,-41,-129,21,18,20,64,12,9,-55,33,-95,61,57,57,-69,-11,-77,9,-35,29,6,5,85,-14,41,-31,-19,64,-10,-121,10,4,-87,108,49,-29,-14,-27,
        -15,0,121,-38,-39,-149,-98,85,104,88,-99,-39,-119,-30,45,0,84,6,68,98,-74,6,21,10,-46,16,-23,24,41,86,-126,-95,-55,-61,122,1,49,-22,-17,-2,-98,45,-62,52,50,
        -25,-10,0,-18,65,-5,-62,-5,-59,14,21,21,-51,50,48,-9,42,-29,-3,57,-2,-5,-54,-97,17,0,86,-55,49,-10,-9,-61,-19,42,-19,34,0,21,-26,-74,-38,4,26,13,-9,-23,5,-47,
        -26,56,25,10,77,26,-95,62,4,-26,9,-46,-5,74,0,0,6,-78,64,69,24,-34,-70,-57,-129,-30,-15,30,16,-19,0,62,-14,14,32,141,-18,-71,22,-58,-15,0,20,21,-9,-81,74,40,38,
        8,-7,-26,-59,-10,12,26,-14,-62,14,-26,-66,41,12,-15,-25,-14,-3,69,70,-37,-86,10,6,46,-2,-35,-54,-95,17,81,-11,18,0,-34,60,-25,105,2,13,17,-77,-19,-31,54,104,-31
        ,-61,8,-42,-143,-39,72,45,48,5,-17,-74,-41,68,97,18,-77,12,36,-22,40,2,-31,-47,-67,-21,14,94,49,-33,-57,-73,2,-26,61,49,-18,-45,-62,-30,60,38,-31,6,-17,10,-63
        ,-6,-21,-43,-21,20,57,-13,62,41,18,26,14,26,25,6,0,-13,-51,-67,-41,-18,-31,-19,-15,-9,-6,5,-10,6,5,-5,20,24,54,29,6,12,-11,-18,-25,-23,-50,-45,-34,-18,10,
        -19,8,8,38,37,21,13,26,21,10,-3,-33,-21,-11,20,13,-19,-18,8,26,22,-2,22,14,-7,16,-35,-50,-37,-23,-6,-34,-23,-11,6,22,32,20,-23,-10,34,21,12,20,-9,-21,-18,-33,
        -42,-27,-27,-15,-3,17,6,-5,6,18,26,44,28,13,34,-5,-27,4,-17,-11,-33,-29,1,-18,14,17,-7,17,37,10,17,14,-6,-21,-11,-10,-34,-13,-45,-25,10,-7,-11,-13,5,44,58,8,8,
        -26,-14,-5,12,-22,-42,-13,-19,-26,-9,-18,-17,2,-11,13,40,54,20,36,-2,-37,-34,-49,-26,-5,-19,-7,41,33,32,18,6,21,22,9,-18,-54,-57,-26,1,-35,-49,-34,-37,9,54,56,
        38,41,16,21,21,0,-5,-25,-34,-43,-39,-35,-14,5,-11,-11,16,22,13,13,10,13,4,20,50,45,25,10,16,17,-30,-53,-55,-45,-13,-18,-26,-30,8,-13,13,12,-13,13,36,30,24,25,
        20,13,4,-19,-45,-47,-59,-21,1,-26,-14,12,25,41,10,-7,22,46,17,0,13,-7,-14,-3,-22,-11,-6,-23,1,-6,-5,-11,1,0,9,8,-3,8,8,17,6,2,-11,8,4,-51,-34,-29,-25,16,-6,
        -19,-6,20,6,-13,0,-10,5,-2,1,22,18,-5,-14,-18,-23,8,-5,13,29,1,-9,-7,1,21,-10,-49,-14,-33,-29,-25,-15,14,33,28,72,52,36,22,-7,-21,-23,-35,-42,-25,-26,-17,-22,
        -5,24,12,17,8,-34,-23,-22,-42,-23,-25,6,21,25,32,6,26,18,-3,-9,-49,-43,-34,-49,-34,-21,-30,-7,29,20,36,46,37,36,21,22,13,-15,-7,-25,-25,-14,9,8,12,-2,10,34,30,9
        ,-9,-19,-37,-2,-2,9,24,0,-5,21,42,24,18,1,1,17,-5,-18,-7,5,6,8,18,-2,-19,-23,-23,-7,-10,-27,-23,-23,-26,-27,-17,-9,10,28,18,8,16,-11,-10,-15,-5,5,-19,-10,-22,
        -13,-19,-30,-18,-13,-10,-25,-18,0,8,18,20,13,1,0,-6,4,-14,2,-9,-22,-15,-6,-9,14,20,4,9,-2,-3,16,17,-18,-15,14,-2,-23,-37,-53,-42,-21,10,9,4,1,24,13,-10,4,-3,
        -26,-29,-27,-7,2,9,16,25,1,21,37,40,42,5,-5,-15,-18,-10,-27,-25,-27,-15,-22,-14,2,2,-13,-6,18,38,32,10,32,30,21,25,1,-17,-22,-15,-30,-14,-3,-31,-23,0)
        .map(x => x.toDouble).toArray

      val analysis = new Analysis(fs, noise.length)
      val amplitude = analysis.analyse(noise)
      assert(amplitude > 0.1)
    }
  }
}