package prieto.fernando.template.ui.renderer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import com.huawei.hiar.*
import fernando.prieto.arengine_common.ArDemoRuntimeException
import fernando.prieto.arengine_common.DisplayRotationManager
import fernando.prieto.arengine_common.TextDisplay
import fernando.prieto.arengine_common.TextureDisplay
import fernando.prieto.rendering.world.GestureEvent
import fernando.prieto.rendering.world.GestureEvent.*
import fernando.prieto.rendering.world.LabelDisplay
import fernando.prieto.rendering.world.ObjectDisplay
import fernando.prieto.rendering.world.VirtualObject
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * This class provides rendering management related to the world scene, including
 * label rendering and virtual object rendering management.
 *
 * @author HW
 * @since 2020-03-21
 * @re-written by Fernando Prieto Moyano
 * @date 2020-09-15
 */
private const val TAG = "WorldRenderer"

class WorldRenderer(
    private val activity: FragmentActivity,
    private val context: Context
) : GLSurfaceView.Renderer {
    private val PROJ_MATRIX_OFFSET = 0

    private val PROJ_MATRIX_NEAR = 0.1f

    private val PROJ_MATRIX_FAR = 100.0f

    private val MATRIX_SCALE_SX = -1.0f

    private val MATRIX_SCALE_SY = -1.0f

    private val BLUE_COLORS = floatArrayOf(66.0f, 133.0f, 244.0f, 255.0f)

    private val GREEN_COLORS = floatArrayOf(66.0f, 133.0f, 244.0f, 255.0f)

    private var session: ARSession? = null

    private var textView: TextView? = null

    private var searchingTextView: TextView? = null

    private var planeOtherTextView: TextView? = null

    private var planeWallTextView: TextView? = null

    private var planeFloorTextView: TextView? = null

    private var planeSeatTextView: TextView? = null

    private var planeTableTextView: TextView? = null

    private var planeCeilingTextView: TextView? = null

    private var frames = 0

    private var lastInterval: Long = 0

    private var fps = 0f

    private val textureDisplay = TextureDisplay()

    private val textDisplay = TextDisplay()

    private val labelDisplay = LabelDisplay()

    private val objectDisplay = ObjectDisplay()

    private var displayRotationManager: DisplayRotationManager? = null

    private var queuedSingleTaps: ArrayBlockingQueue<GestureEvent>? = null

    private val virtualObjects = ArrayList<VirtualObject>()

    private var selectedObj: VirtualObject? = null

    /**
     * Set ARSession, which will update and obtain the latest data in OnDrawFrame.
     *
     * @param arSession ARSession.
     */
    fun setArSession(arSession: ARSession?) {
        if (arSession == null) {
            Log.e(TAG, "setSession error, arSession is null!")
            return
        }
        session = arSession
    }

    /**
     * Set a gesture type queue.
     *
     * @param queuedSingleTaps Gesture type queue.
     */
    fun setQueuedSingleTaps(queuedSingleTaps: ArrayBlockingQueue<GestureEvent>) {
        if (queuedSingleTaps == null) {
            Log.e(TAG, "setSession error, arSession is null!")
            return
        }
        this.queuedSingleTaps = queuedSingleTaps
    }

    /**
     * Set the DisplayRotationManage object, which will be used in onSurfaceChanged and onDrawFrame.
     *
     * @param displayRotationManager DisplayRotationManage is a customized object.
     */
    fun setDisplayRotationManage(displayRotationManager: DisplayRotationManager?) {
        if (displayRotationManager == null) {
            Log.e(
                TAG,
                "SetDisplayRotationManage error, displayRotationManage is null!"
            )
            return
        }
        this.displayRotationManager = displayRotationManager
    }

    /**
     * Create a thread for text display in the UI thread. This thread will be called back in TextureDisplay.
     *
     * @param text      Gesture information displayed on the screen
     * @param positionX The left padding in pixels.
     * @param positionY The right padding in pixels.
     */
    private fun showWorldTypeTextView(text: String?, positionX: Float, positionY: Float) {
        activity?.runOnUiThread {
            textView?.setTextColor(Color.WHITE)

            // Set the font size to be displayed on the screen.
            textView?.textSize = 10f

            text?.let {
                textView?.text = text
                textView?.setPadding(positionX.toInt(), positionY.toInt(), 0, 0)
            } ?: run {
                textView?.text = ""
            }
        }
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // Set the window color.
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)

        textureDisplay.init()
        textDisplay.setListener { text, positionX, positionY ->
            showWorldTypeTextView(
                text,
                positionX,
                positionY
            )
        }

        labelDisplay.init(getPlaneBitmaps())

        objectDisplay.init(this.context)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        textureDisplay.onSurfaceChanged(width, height)
        GLES20.glViewport(0, 0, width, height)
        displayRotationManager?.updateViewportRotation(width, height)
        objectDisplay.setSize(width.toFloat(), height.toFloat())
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        if (session == null) {
            return
        }
        if (displayRotationManager?.deviceRotation == true) {
            displayRotationManager?.updateArSessionDisplayGeometry(session)
        }

        try {
            session?.setCameraTextureName(textureDisplay.externalTextureId)
            val arFrame = session?.update()
            val arCamera = arFrame?.camera

            // The size of the projection matrix is 4 * 4.
            val projectionMatrix = FloatArray(16)
            arCamera?.getProjectionMatrix(
                projectionMatrix,
                PROJ_MATRIX_OFFSET,
                PROJ_MATRIX_NEAR,
                PROJ_MATRIX_FAR
            )
            textureDisplay.onDrawFrame(arFrame)
            val sb = StringBuilder()
            updateMessageData(sb)
            textDisplay.onDrawFrame(sb)

            // The size of ViewMatrix is 4 * 4.
            val viewMatrix = FloatArray(16)
            arCamera?.getViewMatrix(viewMatrix, 0)
            val arPlanesTracked =
                session?.getAllTrackables(ARPlane::class.java)?.filter { arPlane ->
                    arPlane.type != ARPlane.PlaneType.UNKNOWN_FACING &&
                            arPlane.trackingState == ARTrackable.TrackingState.TRACKING
                } ?: emptyList()
            if (arPlanesTracked.isNotEmpty()) {
                hideLoadingMessage()
            }
            labelDisplay.onDrawFrame(
                session?.getAllTrackables(ARPlane::class.java), arCamera?.displayOrientedPose,
                projectionMatrix
            )
            handleGestureEvent(arFrame, arCamera, projectionMatrix, viewMatrix)
            val lightEstimate = arFrame?.lightEstimate
            var lightPixelIntensity = 1f
            if (lightEstimate?.state != ARLightEstimate.State.NOT_VALID) {
                lightPixelIntensity = lightEstimate?.pixelIntensity ?: 0f
            }
            drawAllObjects(projectionMatrix, viewMatrix, lightPixelIntensity)
        } catch (e: ArDemoRuntimeException) {
            Log.e(TAG, "Exception on the ArDemoRuntimeException!")
        } catch (t: Throwable) {
            // This prevents the app from crashing due to unhandled exceptions.
            Log.e(TAG, "Exception on the OpenGL thread: ", t)
        }
    }

    private fun drawAllObjects(
        projectionMatrix: FloatArray,
        viewMatrix: FloatArray,
        lightPixelIntensity: Float
    ) {
        val ite = virtualObjects.iterator()
        while (ite.hasNext()) {
            val obj = ite.next()
            if (obj.anchor.trackingState == ARTrackable.TrackingState.STOPPED) {
                ite.remove()
            }
            if (obj.anchor.trackingState == ARTrackable.TrackingState.TRACKING) {
                objectDisplay.onDrawFrame(viewMatrix, projectionMatrix, lightPixelIntensity, obj)
            }
        }
    }

    private fun getPlaneBitmaps(): ArrayList<Bitmap?>? {
        val bitmaps = ArrayList<Bitmap?>()
        bitmaps.add(getPlaneBitmap(planeOtherTextView))
        bitmaps.add(getPlaneBitmap(planeWallTextView))
        bitmaps.add(getPlaneBitmap(planeFloorTextView))
        bitmaps.add(getPlaneBitmap(planeSeatTextView))
        bitmaps.add(getPlaneBitmap(planeTableTextView))
        bitmaps.add(getPlaneBitmap(planeCeilingTextView))
        return bitmaps
    }

    private fun getPlaneBitmap(textView: TextView?): Bitmap? {
        textView?.isDrawingCacheEnabled = true
        textView?.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        textView?.layout(0, 0, textView.measuredWidth, textView.measuredHeight)
        var bitmap = textView?.drawingCache
        val matrix = Matrix()
        matrix.setScale(MATRIX_SCALE_SX, MATRIX_SCALE_SY)
        bitmap?.let {
            bitmap = Bitmap.createBitmap(it, 0, 0, it.width, it.height, matrix, true)
        }
        return bitmap
    }

    /**
     * Update the information to be displayed on the screen.
     *
     * @param sb String buffer.
     */
    private fun updateMessageData(sb: StringBuilder) {
        sb.append("FPS=").append(doFpsCalculate()).append(System.lineSeparator())
    }

    private fun doFpsCalculate(): Float {
        ++frames
        val timeNow = System.currentTimeMillis()

        // Convert millisecond to second.
        if ((timeNow - lastInterval) / 1000.0f > 0.5f) {
            fps = frames / ((timeNow - lastInterval) / 1000.0f)
            frames = 0
            lastInterval = timeNow
        }
        return fps
    }

    private fun hideLoadingMessage() {
        activity?.runOnUiThread {
            searchingTextView?.let {
                it.isVisible = false
                searchingTextView = null
            }
        }
    }

    private fun handleGestureEvent(
        arFrame: ARFrame?,
        arCamera: ARCamera?,
        projectionMatrix: FloatArray,
        viewMatrix: FloatArray
    ) {
        val event = queuedSingleTaps?.poll() ?: return

        // Do not perform anything when the object is not tracked.
        if (arCamera?.trackingState != ARTrackable.TrackingState.TRACKING) {
            return
        }
        when (event.type) {
            GESTURE_EVENT_TYPE_DOWN -> {
                doWhenEventTypeDown(viewMatrix, projectionMatrix, event)
            }
            GESTURE_EVENT_TYPE_SCROLL -> {
                selectedObj?.let { virtualObject ->
                    hitTest4Result(arFrame, arCamera, event.eventSecond)?.let { arHitResult ->
                        virtualObject.anchor = arHitResult.createAnchor()
                    }
                }

            }
            GESTURE_EVENT_TYPE_SINGLETAPUP -> {
                // Do not perform anything when an object is selected.
                selectedObj?.let {
                    return
                }
                val tap = event.eventFirst
                hitTest4Result(arFrame, arCamera, tap)?.let { arHitResult: ARHitResult ->
                    doWhenEventTypeSingleTap(arHitResult)
                }
            }
            else -> {
                Log.e(TAG, "Unknown motion event type, and do nothing.")
            }
        }
    }

    private fun doWhenEventTypeDown(
        viewMatrix: FloatArray,
        projectionMatrix: FloatArray,
        event: GestureEvent
    ) {
        selectedObj?.let {
            it.setIsSelected(false)
            selectedObj = null
        }

        for (virtualObject in virtualObjects) {
            if (objectDisplay.hitTest(
                    viewMatrix,
                    projectionMatrix,
                    virtualObject,
                    event.eventFirst
                )
            ) {
                virtualObject.setIsSelected(true)
                selectedObj = virtualObject
                break
            }
        }
    }

    private fun doWhenEventTypeSingleTap(hitResult: ARHitResult) {
        // The hit results are sorted by distance. Only the nearest hit point is valid.
        // Set the number of stored objects to 10 to avoid the overload of rendering and AR Engine.
        if (virtualObjects.size >= 16) {
            virtualObjects[0].anchor.detach()
            virtualObjects.removeAt(0)
        }
        when (hitResult.trackable) {
            is ARPoint -> {
                virtualObjects.add(
                    VirtualObject(
                        hitResult.createAnchor(),
                        BLUE_COLORS
                    )
                )
            }
            is ARPlane -> {
                virtualObjects.add(
                    VirtualObject(
                        hitResult.createAnchor(),
                        GREEN_COLORS
                    )
                )
            }
            else -> {
                Log.i(TAG, "Hit result is not plane or point.")
            }
        }
    }

    private fun hitTest4Result(
        frame: ARFrame?,
        camera: ARCamera?,
        event: MotionEvent
    ): ARHitResult? {
        var hitResult: ARHitResult? = null
        val hitTestResults = frame?.hitTest(event)
        hitTestResults?.indices?.let { indices ->
            for (hitResultIndex in indices) {
                // Determine whether the hit point is within the plane polygon.
                val hitResultTemp = hitTestResults[hitResultIndex] ?: continue
                val trackable = hitResultTemp.trackable
                val isPlanHitJudge =
                    (trackable is ARPlane && trackable.isPoseInPolygon(hitResultTemp.hitPose)
                            && calculateDistanceToPlane(hitResultTemp.hitPose, camera?.pose) > 0)

                // Determine whether the point cloud is clicked and whether the point faces the camera.
                val isPointHitJudge = (trackable is ARPoint
                        && trackable.orientationMode == ARPoint.OrientationMode.ESTIMATED_SURFACE_NORMAL)

                // Select points on the plane preferentially.
                if (isPlanHitJudge || isPointHitJudge) {
                    hitResult = hitResultTemp
                    if (trackable is ARPlane) {
                        break
                    }
                }
            }
        }

        return hitResult
    }

    /**
     * Calculate the distance between a point in a space and a plane. This method is used
     * to calculate the distance between a camera in a space and a specified plane.
     *
     * @param planePose  ARPose of a plane.
     * @param cameraPose ARPose of a camera.
     * @return Calculation results.
     */
    private fun calculateDistanceToPlane(planePose: ARPose, cameraPose: ARPose?): Float {
        // The dimension of the direction vector is 3.
        val normals = FloatArray(3)

        // Obtain the unit coordinate vector of a normal vector of a plane.
        planePose.getTransformedAxis(1, 1.0f, normals, 0)

        // Calculate the distance based on projection.
        return getDistanceByProjection(planePose, cameraPose, normals)
    }

    private fun getDistanceByProjection(
        planePose: ARPose,
        cameraPose: ARPose?,
        normals: FloatArray
    ): Float = (cameraPose?.tx() ?: 0f - planePose.tx()) * normals[0] + // 0:x
            ((cameraPose?.ty() ?: 0f - planePose.ty()) * normals[1]) + // 1:y
            (cameraPose?.tz() ?: 0f - planePose.tz()) * normals[2] // 2:z

    fun setTextViews(
        textView: TextView?,
        searchingTextView: TextView?,
        planeOtherTextView: TextView?,
        planeWallTextView: TextView?,
        planeFloorTextView: TextView?,
        planeSeatTextView: TextView?,
        planeTableTextView: TextView?,
        planeCeilingTextView: TextView?
    ) {
        this.textView = textView
        this.searchingTextView = searchingTextView
        this.planeOtherTextView = planeOtherTextView
        this.planeWallTextView = planeWallTextView
        this.planeFloorTextView = planeFloorTextView
        this.planeSeatTextView = planeSeatTextView
        this.planeTableTextView = planeTableTextView
        this.planeCeilingTextView = planeCeilingTextView
    }
}