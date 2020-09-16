package prieto.fernando.template.ui.fragment

import android.content.Intent
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.huawei.hiar.ARConfigBase
import com.huawei.hiar.AREnginesApk
import com.huawei.hiar.ARSession
import com.huawei.hiar.ARWorldTrackingConfig
import com.huawei.hiar.exceptions.*
import fernando.prieto.arengine_common.ConnectAppMarketActivity
import fernando.prieto.arengine_common.DisplayRotationManager
import fernando.prieto.rendering.world.GestureEvent
import prieto.fernando.template.R
import prieto.fernando.template.ui.renderer.WorldRenderer
import java.util.concurrent.ArrayBlockingQueue
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_world.plane_ceiling as planeCeilingTextView
import kotlinx.android.synthetic.main.fragment_world.plane_floor as planeFloorTextView
import kotlinx.android.synthetic.main.fragment_world.plane_other as planeOtherTextView
import kotlinx.android.synthetic.main.fragment_world.plane_seat as planeSeatTextView
import kotlinx.android.synthetic.main.fragment_world.plane_table as planeTableTextView
import kotlinx.android.synthetic.main.fragment_world.plane_wall as planeWallTextView
import kotlinx.android.synthetic.main.fragment_world.searching_text_view as searchingTextView
import kotlinx.android.synthetic.main.fragment_world.surface_view as worldSurfaceView
import kotlinx.android.synthetic.main.fragment_world.word_text_view as worldTextView

private const val TAG = "WorldFragment"
private const val MOTIONEVENT_QUEUE_CAPACITY = 2
private const val OPENGLES_VERSION = 2

class WorldFragment @Inject constructor() : Fragment(R.layout.fragment_world) {

    private val queuedSingleTaps: ArrayBlockingQueue<GestureEvent> =
        ArrayBlockingQueue(MOTIONEVENT_QUEUE_CAPACITY)

    private lateinit var displayRotationManager: DisplayRotationManager
    private lateinit var gestureDetector: GestureDetector

    private var worldRenderer: WorldRenderer? = null
    private var arSession: ARSession? = null
    private var message: String? = null
    private var isRemindInstall = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        displayRotationManager = DisplayRotationManager(requireContext())
        initGestureDetector()

        worldSurfaceView.preserveEGLContextOnPause = true
        worldSurfaceView.setEGLContextClientVersion(OPENGLES_VERSION)

        // Set the EGL configuration chooser, including for the number of
        // bits of the color buffer and the number of depth bits.
        worldSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0)

        worldRenderer = WorldRenderer(
            requireActivity(),
            requireContext()
        )
        worldRenderer?.setDisplayRotationManage(displayRotationManager)
        worldRenderer?.setQueuedSingleTaps(queuedSingleTaps)

        worldSurfaceView.setRenderer(worldRenderer)
        worldSurfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        worldRenderer?.setTextViews(
            worldTextView,
            searchingTextView,
            planeOtherTextView,
            planeWallTextView,
            planeFloorTextView,
            planeSeatTextView,
            planeTableTextView,
            planeCeilingTextView
        )
    }

    private fun initGestureDetector() {
        gestureDetector = GestureDetector(requireContext(), object : SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                onGestureEvent(GestureEvent.createSingleTapUpEvent(e))
                return true
            }

            override fun onDown(e: MotionEvent): Boolean {
                onGestureEvent(GestureEvent.createDownEvent(e))
                return true
            }

            override fun onScroll(
                e1: MotionEvent,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                onGestureEvent(GestureEvent.createScrollEvent(e1, e2, distanceX, distanceY))
                return true
            }
        })
        worldSurfaceView.setOnTouchListener { _, event ->
            gestureDetector?.onTouchEvent(event)
        }
    }

    private fun onGestureEvent(e: GestureEvent) {
        val offerResult = queuedSingleTaps.offer(e)
        if (offerResult) {
            Log.d(TAG, "Successfully joined the queue.")
        } else {
            Log.d(TAG, "Failed to join queue.")
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
        super.onResume()
        var exception: Exception? = null
        message = null
        if (arSession == null) {
            try {
                if (!arEngineAbilityCheck()) {
                    requireActivity().finish()
                    return
                }
                arSession = ARSession(requireContext())
                val config = ARWorldTrackingConfig(arSession)
                config.focusMode = ARConfigBase.FocusMode.AUTO_FOCUS
                config.semanticMode = ARWorldTrackingConfig.SEMANTIC_PLANE
                arSession?.configure(config)
                worldRenderer!!.setArSession(arSession)
            } catch (capturedException: Exception) {
                exception = capturedException
                setMessageWhenError(capturedException)
            }
            message?.let {
                stopArSession(exception)
                return
            }
        }
        try {
            arSession?.resume()
        } catch (e: ARCameraNotAvailableException) {
            Toast.makeText(
                requireContext(),
                "Camera open failed, please restart the app",
                Toast.LENGTH_LONG
            )
                .show()
            arSession = null
            return
        }
        displayRotationManager.registerDisplayListener()
        worldSurfaceView.onResume()
    }

    /**
     * Check whether HUAWEI AR Engine server (com.huawei.arengine.service) is installed on
     * the current device. If not, redirect the user to HUAWEI AppGallery for installation.
     */
    private fun arEngineAbilityCheck(): Boolean {
        val isInstallArEngineApk =
            AREnginesApk.isAREngineApkReady(requireActivity().applicationContext)
        if (!isInstallArEngineApk && isRemindInstall) {
            Toast.makeText(requireContext(), "Please agree to install.", Toast.LENGTH_LONG).show()
            requireActivity().finish()
        }
        Log.d(
            TAG,
            "Is Install AR Engine Apk: $isInstallArEngineApk"
        )
        if (!isInstallArEngineApk) {
            startActivity(Intent(requireContext(), ConnectAppMarketActivity::class.java))
            isRemindInstall = true
        }
        return AREnginesApk.isAREngineApkReady(requireActivity().applicationContext)
    }

    private fun setMessageWhenError(catchException: Exception) {
        if (catchException is ARUnavailableServiceNotInstalledException) {
            startActivity(Intent(requireContext(), ConnectAppMarketActivity::class.java))
        } else if (catchException is ARUnavailableServiceApkTooOldException) {
            message = "Please update HuaweiARService.apk"
        } else if (catchException is ARUnavailableClientSdkTooOldException) {
            message = "Please update this app"
        } else if (catchException is ARUnSupportedConfigurationException) {
            message = "The configuration is not supported by the device!"
        } else {
            message = "exception throw"
        }
    }

    private fun stopArSession(exception: Exception?) {
        Log.i(TAG, "stopArSession start.")
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        Log.e(
            TAG,
            "Creating session error",
            exception
        )
        arSession?.let {
            it.stop()
            arSession = null
        }
        Log.i(TAG, "stopArSession end.")
    }

    override fun onPause() {
        Log.i(TAG, "onPause start.")
        super.onPause()
        arSession?.let {
            displayRotationManager?.unregisterDisplayListener()
            worldSurfaceView.onPause()
            it.pause()
        }
        Log.i(TAG, "onPause end.")
    }

    override fun onDestroy() {
        Log.i(TAG, "onDestroy start.")
        arSession?.let {
            it.stop()
            arSession = null
        }
        super.onDestroy()
        Log.i(TAG, "onDestroy end.")
    }


}