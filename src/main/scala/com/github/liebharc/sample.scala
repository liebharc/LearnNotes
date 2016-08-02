package com.github.liebharc

import android._
import android.app._
import android.os._
import android.widget._
import android.view._

class MainActivity extends Activity with TypedFindView {
    lazy val textview = findView(TR.text)
    lazy val buttonLow = findView(TR.buttonLow).asInstanceOf[Button]
    lazy val buttonHigh = findView(TR.buttonHigh).asInstanceOf[Button]
    lazy val buttonReplay = findView(TR.buttonReplay).asInstanceOf[Button]

    /** Called when the activity is first created. */
    override def onCreate(savedInstanceState: Bundle) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)
        textview.setText("Hello world, from learnnotes")
    }

  def tooLowSelected(view: View) {
    textview.setText("Too low")
  }

  def perfectSelected(view: View) {
    textview.setText("Perfect")
  }

  def tooHighSelected(view: View) {
    textview.setText("Too High")
  }

  def replaySound(view: View) {
    textview.setText("Replay")
  }
}