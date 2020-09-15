package prieto.fernando.template.ui.fragment

import android.content.Intent
import android.graphics.SurfaceTexture
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.Surface
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.huawei.hiar.ARConfigBase
import com.huawei.hiar.AREnginesApk
import com.huawei.hiar.ARFaceTrackingConfig
import com.huawei.hiar.ARSession
import com.huawei.hiar.exceptions.*
import fernando.prieto.arengine_common.ConnectAppMarketActivity
import fernando.prieto.arengine_common.DisplayRotationManager
import fernando.prieto.rendering.face.FaceRenderManager
import prieto.fernando.template.R
import prieto.fernando.template.ui.camera.CameraSessionProvider
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_face.face_surface_view as faceSurfaceView
import kotlinx.android.synthetic.main.fragment_face.face_text_view as faceTexView

private const val TAG = "FaceFragment"

class FaceFragment @Inject constructor() : Fragment(R.layout.fragment_face) {

    private lateinit var displayRotationManager: DisplayRotationManager
    private lateinit var faceRenderManager: FaceRenderManager
    private var cameraSessionProvider: CameraSessionProvider? = null
    private var preViewSurface: Surface? = null
    private var vgaSurface: Surface? = null
    private var metaDataSurface: Surface? = null
    private var depthSurface: Surface? = null
    private var arConfig: ARConfigBase? = null
    private val isOpenCameraOutside = false
    private var arSession: ARSession? = null
    private var message: String? = null
    private var isRemindInstall = false

    // The initial texture ID is -1.
    private var textureId = -1

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        displayRotationManager = DisplayRotationManager(requireContext())

        faceSurfaceView.preserveEGLContextOnPause = true

        // Set the OpenGLES version.
        faceSurfaceView.setEGLContextClientVersion(2)

        // Set the EGL configuration chooser, including for the
        // number of bits of the color buffer and the number of depth bits.
        faceSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0)

        faceRenderManager = FaceRenderManager(requireContext(), requireActivity())
        faceRenderManager.setDisplayRotationManage(displayRotationManager)
        faceRenderManager.setTextView(faceTexView)

        faceSurfaceView.setRenderer(faceRenderManager)
        faceSurfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        arEngineAbilityCheck()
    }

    /**
     * Check whether HUAWEI AR Engine server (com.huawei.arengine.service) is installed on the current device.
     * If not, redirect the user to HUAWEI AppGallery for installation.
     */
    private fun arEngineAbilityCheck(): Boolean {
        val isInstallArEngineApk =
            AREnginesApk.isAREngineApkReady(requireActivity().applicationContext)
        if (!isInstallArEngineApk && isRemindInstall) {
            Toast.makeText(requireContext(), "Please agree to install.", Toast.LENGTH_LONG).show()
            requireActivity().finish()
        }
        Log.d(TAG, "Is Install AR Engine Apk: $isInstallArEngineApk")
        if (!isInstallArEngineApk) {
            startActivity(Intent(requireContext(), ConnectAppMarketActivity::class.java))
            isRemindInstall = true
        }
        return AREnginesApk.isAREngineApkReady(requireActivity().applicationContext)
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
        super.onResume()
        displayRotationManager.registerDisplayListener()
        var exception: java.lang.Exception? = null
        message = null
        if (arSession == null) {
            try {
                if (!arEngineAbilityCheck()) {
                    requireActivity().finish()
                    return
                }
                arSession = ARSession(requireContext())
                arConfig = ARFaceTrackingConfig(arSession)
                arConfig?.setPowerMode(ARConfigBase.PowerMode.POWER_SAVING)
                if (isOpenCameraOutside) {
                    arConfig?.setImageInputMode(ARConfigBase.ImageInputMode.EXTERNAL_INPUT_ALL)
                }
                arSession?.configure(arConfig)
            } catch (capturedException: java.lang.Exception) {
                exception = capturedException
                setMessageWhenError(capturedException)
            }
            message?.let {
                stopArSession(exception!!)
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
        setCamera()
        faceRenderManager.setArSession(arSession)
        faceRenderManager.setOpenCameraOutsideFlag(isOpenCameraOutside)
        faceRenderManager.setTextureId(textureId)
        faceSurfaceView.onResume()
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

    private fun stopArSession(exception: Exception) {
        Log.i(TAG, "Stop session start.")
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        Log.e(TAG, "Creating session error ", exception)
        arSession?.let { session ->
            session.stop()
            arSession = null
        }
        Log.i(TAG, "Stop session end.")
    }

    private fun setCamera() {
        if (isOpenCameraOutside && cameraSessionProvider == null) {
            Log.i(TAG, "new Camera")
            val dm = DisplayMetrics()
            cameraSessionProvider = CameraSessionProvider(activity)
            cameraSessionProvider?.setupCamera(dm.widthPixels, dm.heightPixels)
        }

        // Check whether setCamera is called for the first time.
        if (isOpenCameraOutside) {
            if (textureId != -1) {
                arSession?.setCameraTextureName(textureId)
                initSurface()
            } else {
                val textureIds = IntArray(1)
                GLES20.glGenTextures(1, textureIds, 0)
                textureId = textureIds[0]
                arSession?.setCameraTextureName(textureId)
                initSurface()
            }
            val surfaceTexture = SurfaceTexture(textureId)
            cameraSessionProvider?.setPreviewTexture(surfaceTexture)
            cameraSessionProvider?.setPreViewSurface(preViewSurface)
            cameraSessionProvider?.setVgaSurface(vgaSurface)
            cameraSessionProvider?.setDepthSurface(depthSurface)
            if (cameraSessionProvider?.openCamera() == true) {
                val showMessage = "Open camera filed!"
                Log.e(TAG, showMessage)
                Toast.makeText(requireContext(), showMessage, Toast.LENGTH_LONG).show()
                requireActivity().finish()
            }
        }
    }

    private fun initSurface() {
        val surfaceTypeList = arConfig?.imageInputSurfaceTypes
        val surfaceList = arConfig?.imageInputSurfaces
        Log.i(TAG, "surfaceList size : " + surfaceList?.size)
        val size = surfaceTypeList?.size ?: 0
        for (i in 0 until size) {
            val type = surfaceTypeList?.get(i)
            val surface = surfaceList?.get(i)
            when {
                ARConfigBase.SurfaceType.PREVIEW == type -> {
                    preViewSurface = surface
                }
                ARConfigBase.SurfaceType.VGA == type -> {
                    vgaSurface = surface
                }
                ARConfigBase.SurfaceType.METADATA == type -> {
                    metaDataSurface = surface
                }
                ARConfigBase.SurfaceType.DEPTH == type -> {
                    depthSurface = surface
                }
                else -> {
                    Log.i(TAG, "Unknown type.")
                }
            }
            Log.i(TAG, "list[$i] get surface : $surface, type : $type")
        }
    }

    override fun onPause() {
        Log.i(TAG, "onPause start.")
        super.onPause()
        if (isOpenCameraOutside) {
            cameraSessionProvider.let {
                it?.closeCamera()
                it?.stopCameraThread()
                cameraSessionProvider = null
            }
        }

        if (arSession != null) {
            displayRotationManager.unregisterDisplayListener()
            faceSurfaceView.onPause()
            arSession?.pause()
            Log.i(TAG, "Session paused!")
        }
        Log.i(TAG, "onPause end.")
    }

    override fun onDestroy() {
        Log.i(TAG, "onDestroy start.")
        super.onDestroy()
        arSession?.let { session ->
            Log.i(TAG, "Session onDestroy!")
            session.stop()
            arSession = null
            Log.i(TAG, "Session stop!")
        }
        Log.i(TAG, "onDestroy end.")
    }
}