package com.github.liebharc

import org.scalatest.FunSpec

class BasicSpec extends FunSpec {

  describe("A sound triplet") {
    it("should consist of a low, perfect and high variant") {
      val sounds = SoundTriplet("G", 0, 1, 2)
      assert(sounds(0).isTooLow == true)
      assert(sounds(0).isPerfect == false)
      assert(sounds(0).isTooHigh == false)

      assert(sounds(1).isTooLow == false)
      assert(sounds(1).isPerfect == true)
      assert(sounds(1).isTooHigh == false)

      assert(sounds(2).isTooLow == false)
      assert(sounds(2).isPerfect == false)
      assert(sounds(2).isTooHigh == true)
    }
  }

  describe("Statistics") {
    it("should remember errors") {
      assert(Statistics(0, 0).wrongInput() === Statistics(0, 1))
    }

    it("should remember successes") {
      assert(Statistics(0, 0).rightInput() === Statistics(1, 0))
    }

    it("should count a total") {
      assert(Statistics(3, 5).total === 8)
    }

    it("should print to a more human friendly string") {
      assert(Statistics(3, 5).humanFriendly === "3/5")
    }
  }

  describe("Utils") {
    it("should always pick 0 for a list with one element") {
      assert(Utils.randomPick(List(1)) === 1)
    }

    it("should always pick a valid number from a sounds list") {
      val sounds = SoundTriplet("F", 1, 3, 5)
      assert(Utils.randomPick(sounds).id <= 5)
    }
  }
}