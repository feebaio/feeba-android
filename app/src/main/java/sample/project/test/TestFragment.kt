package sample.project.test

import ResizableFrameLayout
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import io.feeba.Utils
import io.feeba.data.SurveyPresentation
import io.feeba.data.state.Defaults
import io.feeba.ui.IntegrationMode
import io.feeba.ui.PageFrame
import io.feeba.ui.PageResized
import io.feeba.ui.createWebViewInstance
import io.feeba.ui.createWebViewInstanceForManualLoad

class TestFragment : Fragment() {
    private lateinit var parent: LinearLayout
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        parent = LinearLayout(requireContext()).apply {
            id = View.generateViewId()
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            setBackgroundColor(Color.BLUE)
        }
        val survey = arguments?.getSerializable("survey") as SurveyPresentation
        parent.addView(ResizableFrameLayout(requireContext()).apply {
            setBackgroundColor(Color.GREEN)
            fun resizeTheContainer(w: Int, h: Int) {
                this.layoutParams = this.layoutParams.apply {
                    width = w
                    height = h
                }
            }
            addView(
                createWebViewInstance(requireContext(), survey, Defaults.appHistoryState, IntegrationMode.Modal,
                    onPageLoaded = { webView, loadType ->
                        if (loadType is PageResized) {
                            resizeTheContainer(loadType.w, loadType.h)
                        }

                    }, onError = {}, onOutsideTouch = {}).apply {
                    layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT)
                })
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT)
        })
        return this.parent
    }
}