package prieto.fernando.template.ui.camera

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Range
import android.util.Size
import android.view.Surface
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import fernando.prieto.arengine_common.ArDemoRuntimeException
import java.io.Serializable
import java.lang.Long.signum
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

private const val TAG = "CameraSessionProvider"

class CameraSessionProvider @Inject constructor(activity: FragmentActivity?) {

    private var cameraDevice: CameraDevice? = null

    private var cameraCaptureSession: CameraCaptureSession? = null

    private var cameraId: String? = null

    private var vgaSurface: Surface? = null

    private lateinit var activity: Activity

    private var previewSize: Size? = null

    private var cameraThread: HandlerThread? = null

    private var cameraHandler: Handler? = null

    private var surfaceTexture: SurfaceTexture? = null

    private var depthSurface: Surface? = null

    private var captureRequestBuilder: CaptureRequest.Builder? = null

    private val cameraOpenCloseLock = Semaphore(1)

    private var preViewSurface: Surface? = null

    private val stateCallback: CameraDevice.StateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            Log.i(TAG, "CameraDevice onOpened!")
            startPreview()
        }

        override fun onDisconnected(camera: CameraDevice) {
            cameraOpenCloseLock.release()
            camera.close()
            Log.i(TAG, "CameraDevice onDisconnected!")
            cameraDevice = null
        }

        override fun onError(camera: CameraDevice, error: Int) {
            cameraOpenCloseLock.release()
            camera.close()
            Log.i(TAG, "CameraDevice onError!")
            cameraDevice = null
        }
    }

    init {
        startCameraThread()
    }

    /**
     * Obtain the preview size and camera ID.
     *
     * @param width Device screen width, in pixels.
     * @param height Device screen height, in pixels.
     */
    fun setupCamera(width: Int, height: Int) {
        val cameraManager = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            for (id in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val cameraLensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (cameraLensFacing == null || cameraLensFacing != CameraCharacteristics.LENS_FACING_FRONT) {
                    continue
                }
                val maps =
                    characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                if (maps?.getOutputSizes(SurfaceTexture::class.java) == null) {
                    continue
                }
                previewSize =
                    getOptimalSize(maps.getOutputSizes(SurfaceTexture::class.java), width, height)
                cameraId = id
                Log.i(
                    TAG, "Preview width = " + previewSize!!.width + ", height = "
                            + previewSize?.height + ", cameraId = " + cameraId
                )
                break
            }
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Set upCamera error")
        }
    }

    /**
     * Start the camera thread.
     */
    private fun startCameraThread() {
        cameraThread = HandlerThread("CameraThread")
        cameraThread?.start()
        cameraThread?.looper?.let {
            Log.e(TAG, "startCameraThread mCameraThread.getLooper() null!")
            return
        }
        cameraHandler = Handler(cameraThread?.looper)
    }

    /**
     * Close the camera thread.
     * This method will be called when [FaceActivity.onPause].
     */
    fun stopCameraThread() {
        cameraThread?.quitSafely()
        try {
            cameraThread?.join()
            cameraThread = null
            cameraHandler = null
        } catch (e: InterruptedException) {
            Log.e(TAG, "StopCameraThread error")
        }
    }

    /**
     * Launch the camera.
     *
     * @return Open success or failure.
     */
    fun openCamera(): Boolean {
        Log.i(TAG, "OpenCamera!")
        var cameraManager: CameraManager? = null
        cameraManager = if (activity.getSystemService(Context.CAMERA_SERVICE) is CameraManager) {
            activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        } else {
            return false
        }
        try {
            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }

            // 2500 is the maximum waiting time.
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw ArDemoRuntimeException("Time out waiting to lock camera opening.")
            }
            cameraManager.openCamera(cameraId ?: "", stateCallback, cameraHandler)
        } catch (e: CameraAccessException) {
            Log.e(TAG, "OpenCamera error.")
            return false
        } catch (e: InterruptedException) {
            Log.e(TAG, "OpenCamera error.")
            return false
        }
        return true
    }

    /**
     * Close the camera.
     */
    fun closeCamera() {
        try {
            cameraOpenCloseLock.acquire()
            Log.i(TAG, "Stop CameraCaptureSession begin!")
            stopPreview()
            Log.i(TAG, "Stop CameraCaptureSession stopped!")
            cameraDevice?.let {
                Log.i(TAG, "Stop Camera!")
                cameraDevice?.close()
                cameraDevice = null
                Log.i(TAG, "Stop Camera stopped!")
            }
        } catch (e: InterruptedException) {
            throw ArDemoRuntimeException("Interrupted while trying to lock camera closing.", e)
        } finally {
            cameraOpenCloseLock.release()
        }
    }

    private fun getOptimalSize(sizeMap: Array<Size>, width: Int, height: Int): Size? {
        val max = max(width, height)
        val min = min(width, height)
        val sizeList: List<Size> = sizeMap.filter { size ->
            size.width > max && size.height > min
        }
        return if (sizeList.isEmpty()) {
            sizeMap[0]
        } else Collections.min(sizeList, CalculatedAreaDifference())
    }

    /**
     * Calculate the area difference.
     * serialVersionUID = 7710120461881073428L
     * @author HW
     * @since 2020-03-15
     */
    class CalculatedAreaDifference : Comparator<Size?>, Serializable {
        override fun compare(lhs: Size?, rhs: Size?): Int {
            return if (lhs != null && rhs != null) {
                signum(lhs.width * lhs.height - rhs.width * rhs.height.toLong())
            } else -1
        }
    }

    /**
     * Set the texture of the surface.
     *
     * @param surfaceTexture Surface texture.
     */
    fun setPreviewTexture(surfaceTexture: SurfaceTexture) {
        this.surfaceTexture = surfaceTexture
    }

    /**
     * Set the preview surface.
     *
     * @param surface Surface.
     */
    fun setPreViewSurface(surface: Surface?) {
        preViewSurface = surface
    }

    /**
     * Set the VGA surface.
     *
     * @param surface Surface
     */
    fun setVgaSurface(surface: Surface?) {
        vgaSurface = surface
    }

    /**
     * Set the depth surface.
     *
     * @param surface Surface.
     */
    fun setDepthSurface(surface: Surface?) {
        depthSurface = surface
    }

    private fun startPreview() {
        if (surfaceTexture == null) {
            Log.i(TAG, "mSurfaceTexture is null!")
            return
        }
        Log.i(TAG, "StartPreview!")
        surfaceTexture?.setDefaultBufferSize(previewSize!!.width, previewSize!!.height)
        if (cameraDevice == null) {
            Log.i(TAG, "cameraDevice is null!")
            return
        }
        try {
            captureRequestBuilder =
                cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            val surfaces: MutableList<Surface> = ArrayList()
            preViewSurface?.let {
                surfaces.add(it)
            }
            vgaSurface?.let {
                surfaces.add(it)
            }
            depthSurface?.let {
                surfaces.add(it)
            }
            captureSession(surfaces)
        } catch (e: CameraAccessException) {
            Log.e(TAG, "StartPreview error")
        }
    }

    private fun captureSession(surfaces: List<Surface>) {
        try {
            cameraDevice!!.createCaptureSession(
                surfaces,
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        try {
                            cameraDevice?.let {
                                Log.w(TAG, "CameraDevice stop!")
                                return
                            }
                            preViewSurface?.let {
                                captureRequestBuilder?.addTarget(it)
                            }
                            vgaSurface?.let {
                                captureRequestBuilder?.addTarget(it)
                            }
                            depthSurface?.let {
                                captureRequestBuilder?.addTarget(it)
                            }

                            // Set the number of frames to 30.
                            val fpsRange = Range(30, 30)
                            captureRequestBuilder?.set(
                                CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                                fpsRange
                            )
                            val captureRequests: MutableList<CaptureRequest> = ArrayList()
                            captureRequests.add(captureRequestBuilder!!.build())
                            cameraCaptureSession = session
                            cameraCaptureSession?.setRepeatingBurst(
                                captureRequests,
                                null,
                                cameraHandler
                            )
                            cameraOpenCloseLock.release()
                        } catch (e: CameraAccessException) {
                            Log.e(TAG, "CaptureSession onConfigured error")
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {}
                    override fun onClosed(session: CameraCaptureSession) {
                        Log.i(TAG, "CameraCaptureSession stopped!")
                    }
                },
                cameraHandler
            )
        } catch (e: CameraAccessException) {
            Log.e(TAG, "CaptureSession error")
        }
    }

    private fun stopPreview() {
        if (cameraCaptureSession != null) {
            cameraCaptureSession?.close()
            cameraCaptureSession = null
        } else {
            Log.i(TAG, "cameraCaptureSession is null!")
        }
    }
}