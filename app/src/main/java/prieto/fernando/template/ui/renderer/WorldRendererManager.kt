package prieto.fernando.template.ui.renderer

import android.app.Activity
import android.content.Context
import android.opengl.GLSurfaceView
import android.widget.TextView
import com.huawei.hiar.ARSession
import fernando.prieto.arengine_common.DisplayRotationManager
import fernando.prieto.arengine_common.TextDisplay
import fernando.prieto.arengine_common.TextureDisplay
import fernando.prieto.rendering.world.GestureEvent
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
 * @re-write Fernando Prieto Moyano
 * @date 2020-09-15
 */
class WorldRendererManager : GLSurfaceView.Renderer{
    private val PROJ_MATRIX_OFFSET = 0

    private val PROJ_MATRIX_NEAR = 0.1f

    private val PROJ_MATRIX_FAR = 100.0f

    private val MATRIX_SCALE_SX = -1.0f

    private val MATRIX_SCALE_SY = -1.0f

    private val BLUE_COLORS = floatArrayOf(66.0f, 133.0f, 244.0f, 255.0f)

    private val GREEN_COLORS = floatArrayOf(66.0f, 133.0f, 244.0f, 255.0f)

    private val mSession: ARSession? = null

    private val mActivity: Activity? = null

    private val mContext: Context? = null

    private val mTextView: TextView? = null

    private val mSearchingTextView: TextView? = null

    private val mPlaneOtherTextView: TextView? = null

    private val mPlaneWallTextView: TextView? = null

    private val mPlaneFloorTextView: TextView? = null

    private val mPlaneSeatTextView: TextView? = null

    private val mPlaneTableTextView: TextView? = null

    private val mPlaneCeilingTextView: TextView? = null

    private val frames = 0

    private val lastInterval: Long = 0

    private val fps = 0f

    private val mTextureDisplay = TextureDisplay()

    private val mTextDisplay = TextDisplay()

    private val mLabelDisplay = LabelDisplay()

    private val mObjectDisplay = ObjectDisplay()

    private val mDisplayRotationManager: DisplayRotationManager? = null

    private val mQueuedSingleTaps: ArrayBlockingQueue<GestureEvent>? = null

    private val mVirtualObjects = ArrayList<VirtualObject>()

    private val mSelectedObj: VirtualObject? = null

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        TODO("Not yet implemented")
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        TODO("Not yet implemented")
    }

    override fun onDrawFrame(gl: GL10?) {
        TODO("Not yet implemented")
    }
}