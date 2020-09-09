package prieto.fernando.template.ui.fragment

import android.content.Intent
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.huawei.hiar.ARConfigBase
import com.huawei.hiar.AREnginesApk
import com.huawei.hiar.ARHandTrackingConfig
import com.huawei.hiar.ARSession
import com.huawei.hiar.exceptions.*
import fernando.prieto.arengine_common.ConnectAppMarketActivity
import fernando.prieto.arengine_common.DisplayRotationManager
import fernando.prieto.rendering.hand.HandRenderManager
import prieto.fernando.template.R
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_hand.hand_surface_view as handSurfaceView
import kotlinx.android.synthetic.main.fragment_hand.hand_text_view as handTexView

private const val TAG = "HandFragment"

class HandFragment @Inject constructor() : Fragment(R.layout.fragment_hand) {

    private lateinit var handRenderManager: HandRenderManager
    private lateinit var displayRotationManager: DisplayRotationManager

    private var arSession: ARSession? = null
    private var message: String? = null
    private var isRemindInstall = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        displayRotationManager = DisplayRotationManager(requireContext())

        // Keep the OpenGL ES running context.
        handSurfaceView.preserveEGLContextOnPause = true

        // Set the OpenGLES version.
        handSurfaceView.setEGLContextClientVersion(2)

        // Set the EGL configuration chooser, including for the
        // number of bits of the color buffer and the number of depth bits.
        handSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0)

        handRenderManager = HandRenderManager(requireActivity())
        handRenderManager.setDisplayRotationManage(displayRotationManager)
        handRenderManager.setTextView(handTexView)

        handSurfaceView.setRenderer(handRenderManager)
        handSurfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        arEngineAbilityCheck()
    }

    override fun onResume() {
        Log.d(TAG, "onResume")
        super.onResume()
        var exception: java.lang.Exception? = null
        message = null
        if (arSession == null) {
            try {
                if (!arEngineAbilityCheck()) {
                    activity?.finish()
                    return
                }
                arSession = ARSession(requireContext())
                val config = ARHandTrackingConfig(arSession)
                config.cameraLensFacing = ARConfigBase.CameraLensFacing.FRONT
                config.powerMode = ARConfigBase.PowerMode.ULTRA_POWER_SAVING
                val item = ARConfigBase.ENABLE_DEPTH.toLong()
                config.enableItem = item
                arSession?.configure(config)
                handRenderManager.setArSession(arSession)
                Log.d(
                    TAG,
                    "Item = " + config.enableItem
                )
            } catch (capturedException: java.lang.Exception) {
                exception = capturedException
                setMessageWhenError(capturedException)
            }
            message?.let {
                stopArSession(exception ?: java.lang.Exception())
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
        handSurfaceView.onResume()
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

    private fun setMessageWhenError(catchException: java.lang.Exception) {
        when (catchException) {
            is ARUnavailableServiceNotInstalledException -> {
                startActivity(Intent(requireContext(), ConnectAppMarketActivity::class.java))
            }
            is ARUnavailableServiceApkTooOldException -> {
                message = "Please update HuaweiARService.apk"
            }
            is ARUnavailableClientSdkTooOldException -> {
                message = "Please update this app"
            }
            is ARUnSupportedConfigurationException -> {
                message = "The configuration is not supported by the device!"
            }
            else -> {
                message = "exception throw"
            }
        }
    }

    /**
     * Stop the ARSession and display exception information when an unrecoverable exception occurs.
     *
     * @param exception Exception occurred
     */
    private fun stopArSession(exception: java.lang.Exception) {
        Log.i(TAG, "stopArSession start.")
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        Log.e(TAG, "Creating session error", exception)
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
            displayRotationManager.unregisterDisplayListener()
            handSurfaceView.onPause()
            it.pause()
        }
        Log.i(TAG, "onPause end.")
    }

    override fun onDestroy() {
        Log.i(TAG, "onDestroy start.")
        super.onDestroy()
        arSession?.let {
            it.stop()
            arSession = null
        }
        Log.i(TAG, "onDestroy end.")
    }
}