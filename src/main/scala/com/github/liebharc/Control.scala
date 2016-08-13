package com.github.liebharc

import android.app.Activity
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.{LayoutInflater, View, ViewGroup}

trait ControlBehaviour extends Activity with TypedFindView {
  private var isRunning: Boolean = false

  def runStopButton = findView(TR.buttonStartStopAnalysis)

  def metronomSpeed = findView(TR.metrospeed)

  def initializeControl(): Unit = {

  }
}

class ControlFragment extends Fragment {
  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    super.onCreateView(inflater, container, savedInstanceState)
    val view = inflater.inflate(R.layout.control, container, false)
    return view
  }

  override def onViewCreated(view: View, savedInstanceState: Bundle): Unit = {
    super.onViewCreated(view, savedInstanceState)
    val typedActivity = getActivity.asInstanceOf[ControlBehaviour]
    typedActivity.initializeControl()
  }
}