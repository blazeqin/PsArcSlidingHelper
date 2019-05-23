package com.blazeqin.psarcslidinghelper

import android.content.Context
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.widget.Scroller

class PsSlidingHelper private constructor(
    private val context:Context,
    private var pivotX: Int,
    private var pivotY: Int,
    private val listener: OnSlidingListener
){

    private val mScroller = Scroller(context)
    //滑动速度跟踪器VelocityTracker, 这个类可以用来监听手指移动改变的速度;
    private val mVelocityTracker = VelocityTracker.obtain()
    private val mScrollAvailabilityRatio = .3f
    private val mHandler  by lazy { InertialSlidingHandler(this@PsSlidingHelper) }
    private var isRecycled = false//是否已经释放
    private var mSlidingFinishListener:OnSlidingFinishListener? = null
    private var isSelfSliding = false;//是否是自己在滑动
    private var isInertialSlidingEnable = false//是否开启惯性滑动
    private var mStartX = 0F
    private var mStartY = 0F
    private var isClockwiseScrolling: Boolean = false//是否是顺时针滑动
    private var isShouldBeGetY: Boolean = false//在x和y轴方向，是否y方向滑动距离更大


    companion object {
        private const val TAG = "PsSlidingHelper"
        fun create(targetView: View, listener: OnSlidingListener):PsSlidingHelper {
            val width = targetView.width
            val height = targetView.height
            if (width <= 0) {
                Log.e(TAG,"targetView width <= 0! please invoke the updatePivotX(int) method to update the PivotX!", RuntimeException())
            }
            if (height <= 0) {
                Log.e(TAG,"targetView height <= 0! please invoke the updatePivotY(int) method to update the PivotY!", RuntimeException())
            }
            val x = getAbsoluteX(targetView) + width/2 //view的中间点
            val y = getAbsoluteY(targetView) + height/2
            return PsSlidingHelper(targetView.context,x,y,listener)
        }

        /**
         * 获取view在屏幕中的绝对Y坐标
         */
        private fun getAbsoluteY(view: View): Int {
            var y = view.y
            val parent = view.parent
            if (parent is View) {
                y += getAbsoluteY(parent)
            }
            return y.toInt()
        }

        /**
         * 获取view在屏幕中的绝对X坐标
         */
        private fun getAbsoluteX(view: View): Int {
            var x = view.x
            val parent = view.parent
            if (parent is View) {
                x += getAbsoluteX(parent)
            }
            return x.toInt()
        }
    }

    /**
     * handle touch event
     */
    fun handleMovement(event: MotionEvent) {
        checkIsRecycled()
        var x:Float = 0F
        var y:Float = 0F
        if (isSelfSliding) {
            x = event.rawX
            y = event.rawY
        } else {
            x = event.x
            y = event.y
        }
        mVelocityTracker.addMovement(event)//监测移动速度，有人说按2000算，小于2000：慢，大于：快
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (!mScroller.isFinished) {
                    mScroller.abortAnimation()
                }
            }
            MotionEvent.ACTION_MOVE -> handleActionMove(x,y)
            MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL,MotionEvent.ACTION_OUTSIDE -> {
                if (isInertialSlidingEnable) {

                }
            }
            else -> {}
        }
        mStartX = x
        mStartY = y
    }

    /**
     * 计算滑动的角度
     *余弦定理 cosA=(b^2+c^2-a^2)/2bc
     */
    private fun handleActionMove(x: Float, y: Float) {
        //勾股定理求边长
        val lineA = Math.sqrt(
            Math.pow(Math.abs(mStartX - pivotX).toDouble(), 2.0) + Math.pow(Math.abs(mStartY - pivotY).toDouble(), 2.0)
        )
        val lineB = Math.sqrt(
            Math.pow(Math.abs(x - pivotX).toDouble(), 2.0) + Math.pow(Math.abs(y - pivotY).toDouble(), 2.0)
        )
        val lineC = Math.sqrt(
            Math.pow(Math.abs(mStartX - x).toDouble(), 2.0) + Math.pow(Math.abs(mStartY - y).toDouble(), 2.0)
        )

        if (lineA > 0 && lineB > 0 && lineC > 0) {
            val angle = adjustAngle(Math.toDegrees(Math.acos((Math.pow(lineA,2.0)+Math.pow(lineB,2.0)-Math.pow(lineC,2.0))/(2*lineA*lineB))))
            if (!angle.isNaN()) {
                isClockwiseScrolling=isClockwise(x,y)
                listener.onSliding(if(isClockwiseScrolling) angle else -angle)
            }
        }
    }

    /**
     * 是否顺时针滑动
     * 如果在y方向滑动距离更大。。。
     * 有bug，有概率会判断失误
     * 替换方案：A点触摸点，C点滑动点，D点相对点：（A.x-D.x）*(C.y-D.y)-(A.y-D.y)*(C.x-D.x)>0
     */
    private fun isClockwise(x: Float, y: Float): Boolean {
        isShouldBeGetY = Math.abs(y -mStartY) > Math.abs(x - mStartX)
//        return if (isShouldBeGetY) (x < pivotX != y > mStartY) else (y < pivotY == x > mStartX)
        return (mStartX-pivotX)*(y-pivotY)-(mStartY-pivotY)*(x-pivotX)>0
    }

    /**
     * 调整角度：在0~360
     */
    private fun adjustAngle(rotation: Double): Float {
        var result = rotation.toFloat()
        if (result < 0) {
            result += 360F
        }
        if (result > 360F) {
            result %= 360F
        }
        return result
    }

    /**
     * update the circle X pivot
     */
    fun updatePivotX(pivotX: Int) {
        checkIsRecycled()
        this.pivotX = pivotX
    }
    /**
     * update the circle Y pivot
     */
    fun updatePivotY(pivotY: Int) {
        checkIsRecycled()
        this.pivotY = pivotY
    }

    /**
     * check resource is released
     */
    private fun checkIsRecycled() {
        if (isRecycled) {
            throw IllegalStateException("PsSlidingHelper is recycled!")
        }
    }

    fun setOnSlideFinishListener(listener: OnSlidingFinishListener) {
        mSlidingFinishListener = listener
    }
}

/**
 * 主线程惯性回调
 */
class InertialSlidingHandler(val mHelper: PsSlidingHelper): Handler() {
    override fun handleMessage(msg: Message?) {
//        mHelper.computeInertialSliding()
    }
}

/**
 * 回调：开启弧形滑动
 */
interface OnSlidingListener{
    fun onSliding(angle: Float)
}

/**
 * 监听滚动结束
 */
interface OnSlidingFinishListener{
    fun onSlidingFinished()
}
