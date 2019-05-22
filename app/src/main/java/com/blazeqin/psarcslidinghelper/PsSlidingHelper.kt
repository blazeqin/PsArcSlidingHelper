package com.blazeqin.psarcslidinghelper

import android.content.Context
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.widget.Scroller
import java.lang.IllegalStateException

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
    private var isRecycled = false
    private var mSlidingFinishListener:OnSlidingFinishListener? = null
    private var isSelfSliding = false;
    private var isInertialSlidingEnable = false
    private var mStartX = 0F
    private var mStartY = 0F


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

    private fun handleActionMove(x: Float, y: Float) {
        var l = 0f;var t = 0f;var r = 0f;var b = 0f
        if (mStartX > x) {//x轴，小的在左边，大的在右边
            l = x
            r = mStartX
        }else{
            l = mStartX
            r = x
        }
        if (mStartY > y) {//y轴，小的在上面，大的在下面
            t = y
            b = mStartY
        } else {
            t = mStartY
            b = y
        }
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
