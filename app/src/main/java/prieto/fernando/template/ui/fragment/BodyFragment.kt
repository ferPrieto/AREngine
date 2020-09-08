package prieto.fernando.template.ui.fragment

import android.content.Intent
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.huawei.hiar.ARBodyTrackingConfig
import com.huawei.hiar.ARConfigBase
import com.huawei.hiar.AREnginesApk
import com.huawei.hiar.ARSession
import com.huawei.hiar.exceptions.*
import fernando.prieto.arengine_common.ConnectAppMarketActivity
import fernando.prieto.arengine_common.DisplayRotationManager
import fernando.prieto.rendering.body3d.BodyRenderManager
import prieto.fernando.template.R
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_body.body_surface_view as bodySurfaceView
import kotlinx.android.synthetic.main.fragment_body.body_text_view as bodyTexView

private const val TAG = "BodyFragment"

class BodyFragment @Inject constructor() : Fragment(R.layout.fragment_body) {

    private lateinit var bodyRenderManager: BodyRenderManager
    private lateinit var displayRotationManager: DisplayRotationManager
    private var arSession: ARSession? = null
    private var message: String? = null
    private var isRemindInstall = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        displayRotationManager = DisplayRotationManager(requireContext())

        // Keep the OpenGL ES running context.
        bodySurfaceView.preserveEGLContextOnPause = true

        // Set the OpenGLES version.
        bodySurfaceView.setEGLContextClientVersion(2)

        // Set the EGL configuration chooser, including for the
        // number of bits of the color buffer and the number of depth bits.
        bodySurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0)

        bodyRenderManager = BodyRenderManager(activity)
        bodyRenderManager.setDisplayRotationManage(displayRotationManager)
        bodyRenderManager.setTextView(bodyTexView)

        bodySurfaceView.setRenderer(bodyRenderManager)
        bodySurfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        arEngineAbilityCheck()
    }

    /**
     * Check whether HUAWEI AR Engine server (com.huawei.arengine.service) is installed on the current device.
     * If not, redirect the user to HUAWEI AppGallery for installation.
     */
    private fun arEngineAbilityCheck(): Boolean {
        val isInstallArEngineApk = AREnginesApk.isAREngineApkReady(activity?.applicationContext)
        if (!isInstallArEngineApk && isRemindInstall) {
            Toast.makeText(context, "Please agree to install.", Toast.LENGTH_LONG).show()
            activity?.finish()
        }
        Log.d(TAG, "Is Install AR Engine Apk: $isInstallArEngineApk")
        if (!isInstallArEngineApk) {
            startActivity(Intent(context, ConnectAppMarketActivity::class.java))
            isRemindInstall = true
        }
        return AREnginesApk.isAREngineApkReady(activity?.applicationContext)
    }

    private fun setMessageWhenError(catchException: Exception) {
        when (catchException) {
            is ARUnavailableServiceNotInstalledException -> {
                startActivity(Intent(context, ConnectAppMarketActivity::class.java))
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
                arSession = ARSession(context)
                val config = ARBodyTrackingConfig(arSession)
                config.enableItem = (ARConfigBase.ENABLE_DEPTH or ARConfigBase.ENABLE_MASK.toLong()
                    .toInt()).toLong()
                arSession?.configure(config)
                bodyRenderManager.setArSession(arSession)
            } catch (capturedException: java.lang.Exception) {
                exception = capturedException
                setMessageWhenError(capturedException)
            }
            message?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                Log.e(TAG, "Creating session", exception)
                if (arSession != null) {
                    arSession?.stop()
                    arSession = null
                }
                return
            }
        }
        try {
            arSession?.resume()
        } catch (e: ARCameraNotAvailableException) {
            Toast.makeText(context, "Camera open failed, please restart the app", Toast.LENGTH_LONG)
                .show()
            arSession = null
            return
        }
        bodySurfaceView.onResume()
        displayRotationManager.registerDisplayListener()
    }

    override fun onPause() {
        Log.i(TAG, "onPause start.")
        super.onPause()
        if (arSession != null) {
            displayRotationManager.unregisterDisplayListener()
            bodySurfaceView.onPause()
            arSession?.pause()
        }
        Log.i(TAG, "onPause end.")
    }

    override fun onDestroy() {
        Log.i(TAG, "onDestroy start.")
        super.onDestroy()
        arSession?.let { session ->
            session.stop()
            arSession = null
        }
        Log.i(TAG, "onDestroy end.")
    }

}