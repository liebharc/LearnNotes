package com.github.liebharc

import java.util

import android.app.Activity
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.{LayoutInflater, View, ViewGroup}
import com.github.mikephil.charting.data._

trait FeedbackBehaviour extends Activity with TypedFindView {
  def pitchChart = findView(TR.pitch)

  def initializeFeedback(): Unit = {
    val entries  = new util.ArrayList[Entry]
    for (i <- 0 until 5) {
      entries .add(new Entry(i.toFloat, i))
    }

    val dataSet = new LineDataSet(entries, "Label")
    val xLabels  = new util.ArrayList[String]
    xLabels.add("a")
    xLabels.add("b")
    xLabels.add("c#")
    xLabels.add("d")
    xLabels.add("e")
    pitchChart.setData(new LineData(xLabels, dataSet))
  }
}

class FeedbackFragment extends Fragment {
  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    super.onCreateView(inflater, container, savedInstanceState)
    val view = inflater.inflate(R.layout.feedback, container, false)
    return view
  }

  override def onViewCreated(view: View, savedInstanceState: Bundle) = {
    super.onViewCreated(view, savedInstanceState)
    val typedActivity = getActivity.asInstanceOf[FeedbackBehaviour]
    typedActivity.initializeFeedback()
  }
}