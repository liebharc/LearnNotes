package com.github.liebharc

import org.scalatest.FunSpec

import abc.parser.TuneParser
import abc.ui.swing.JScoreComponent
import java.io.File

class AnalysisSpec extends FunSpec {

  describe("A lib exploration test") {
    it("should display a window") {
      //
      // K:D
      // CDEFGABcdefggfedcBAGFEDC

      val tuneAsString = "X:0\nK:D\nC"
      val tune = new TuneParser().parse(tuneAsString)
      val scoreUI = new JScoreComponent()
      scoreUI.setTune(tune)
      scoreUI.writeScoreTo(new File("C:\\Users\\christian\\Desktop\\score.png"))
    }
  }
}