package com.github.liebharc

import org.scalatest.FunSpec

class BasicSpec extends FunSpec {

  describe("A sound triplet") {

    it("should convert to a list with three elements") {
      val triplet = SoundTriplet(1, 3, 5)
      assert(triplet.toList === List(1, 3, 5))
    }
  }

  describe("A sounds collection") {

    it("should convert to a list with three elements per triplet") {
      val sounds = Sounds(SoundTriplet(1, 3, 5))
      assert(sounds.toList === List(1, 3, 5))
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
      val sounds = Sounds(SoundTriplet(1, 3, 5))
      assert(Utils.randomPick(sounds.toList) <= 5)
    }
  }
}