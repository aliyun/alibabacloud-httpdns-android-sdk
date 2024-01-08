package com.alibaba.ams.emas.demo.widget

import android.content.Context
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Scroller
import com.aliyun.ams.httpdns.demo.R
import java.lang.ref.WeakReference
import kotlin.math.abs

/**
 * @author allen.wy
 * @date 2023/6/5
 */
class SwipeLayout(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
    ViewGroup(context, attrs, defStyleAttr) {
    private val mMatchParentChildren = mutableListOf<View>()
    private var menuViewResId = 0
    private var contentViewResId = 0
    private var menuView: View? = null
    private var contentView: View? = null
    private var contentViewLayoutParam: MarginLayoutParams? = null
    private var isSwiping = false
    private var lastP: PointF? = null
    private var firstP: PointF? = null
    private var fraction = 0.2f
    private var scaledTouchSlop = 0
    private var scroller: Scroller? = null
    private var finalDistanceX = 0f

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    /**
     * 初始化方法
     *
     * @param context
     * @param attrs
     * @param defStyleAttr
     */
    private fun init(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        //创建辅助对象
        val viewConfiguration = ViewConfiguration.get(context)
        scaledTouchSlop = viewConfiguration.scaledTouchSlop
        scroller = Scroller(context)
        //1、获取配置的属性值
        val typedArray = context.theme
            .obtainStyledAttributes(attrs, R.styleable.SwipeLayout, defStyleAttr, 0)
        try {
            val indexCount: Int = typedArray.indexCount
            for (i in 0 until indexCount) {
                when (typedArray.getIndex(i)) {
                    R.styleable.SwipeLayout_menuView -> {
                        menuViewResId =
                            typedArray.getResourceId(R.styleable.SwipeLayout_menuView, -1)
                    }
                    R.styleable.SwipeLayout_contentView -> {
                        contentViewResId =
                            typedArray.getResourceId(R.styleable.SwipeLayout_contentView, -1)
                    }
                    R.styleable.SwipeLayout_fraction -> {
                        fraction = typedArray.getFloat(R.styleable.SwipeLayout_fraction, 0.5f)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            typedArray.recycle()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        //获取childView的个数
        isClickable = true
        var count = childCount
        val measureMatchParentChildren =
            MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.EXACTLY ||
                    MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.EXACTLY
        mMatchParentChildren.clear()
        var maxHeight = 0
        var maxWidth = 0
        var childState = 0
        for (i in 0 until count) {
            val child: View = getChildAt(i)
            if (child.visibility != View.GONE) {
                measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0)
                val lp = child.layoutParams as MarginLayoutParams
                maxWidth =
                    maxWidth.coerceAtLeast(child.measuredWidth + lp.leftMargin + lp.rightMargin)
                maxHeight =
                    maxHeight.coerceAtLeast(child.measuredHeight + lp.topMargin + lp.bottomMargin)
                childState = combineMeasuredStates(childState, child.measuredState)
                if (measureMatchParentChildren) {
                    if (lp.width == LayoutParams.MATCH_PARENT ||
                        lp.height == LayoutParams.MATCH_PARENT
                    ) {
                        mMatchParentChildren.add(child)
                    }
                }
            }
        }
        // Check against our minimum height and width
        maxHeight = maxHeight.coerceAtLeast(suggestedMinimumHeight)
        maxWidth = maxWidth.coerceAtLeast(suggestedMinimumWidth)
        setMeasuredDimension(
            resolveSizeAndState(maxWidth, widthMeasureSpec, childState),
            resolveSizeAndState(
                maxHeight, heightMeasureSpec,
                childState shl MEASURED_HEIGHT_STATE_SHIFT
            )
        )
        count = mMatchParentChildren.size
        if (count < 1) {
            return
        }
        for (i in 0 until count) {
            val child: View = mMatchParentChildren[i]
            val lp = child.layoutParams as MarginLayoutParams
            val childWidthMeasureSpec = if (lp.width == LayoutParams.MATCH_PARENT) {
                val width = 0.coerceAtLeast(
                    measuredWidth - lp.leftMargin - lp.rightMargin
                )
                MeasureSpec.makeMeasureSpec(
                    width, MeasureSpec.EXACTLY
                )
            } else {
                getChildMeasureSpec(
                    widthMeasureSpec,
                    lp.leftMargin + lp.rightMargin,
                    lp.width
                )
            }
            val childHeightMeasureSpec = if (lp.height == FrameLayout.LayoutParams.MATCH_PARENT) {
                val height = 0.coerceAtLeast(
                    measuredHeight - lp.topMargin - lp.bottomMargin
                )
                MeasureSpec.makeMeasureSpec(
                    height, MeasureSpec.EXACTLY
                )
            } else {
                getChildMeasureSpec(
                    heightMeasureSpec,
                    lp.topMargin + lp.bottomMargin,
                    lp.height
                )
            }
            child.measure(childWidthMeasureSpec, childHeightMeasureSpec)
        }
    }

    override fun generateLayoutParams(attrs: AttributeSet?): LayoutParams {
        return MarginLayoutParams(context, attrs)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val count = childCount
        val left = 0 + paddingLeft
        val top = 0 + paddingTop
        for (i in 0 until count) {
            val child: View = getChildAt(i)
            if (menuView == null && child.id == menuViewResId) {
                menuView = child
                menuView!!.isClickable = true
            } else if (contentView == null && child.id == contentViewResId) {
                contentView = child
                contentView!!.isClickable = true
            }
        }
        //布局contentView
        val cRight: Int
        if (contentView != null) {
            contentViewLayoutParam = contentView!!.layoutParams as MarginLayoutParams?
            val cTop = top + contentViewLayoutParam!!.topMargin
            val cLeft = left + contentViewLayoutParam!!.leftMargin
            cRight = left + contentViewLayoutParam!!.leftMargin + contentView!!.measuredWidth
            val cBottom: Int = cTop + contentView!!.measuredHeight
            contentView!!.layout(cLeft, cTop, cRight, cBottom)
        }

        if (menuView != null) {
            val rightViewLp = menuView!!.layoutParams as MarginLayoutParams
            val lTop = top + rightViewLp.topMargin
            val lLeft =
                contentView!!.right + contentViewLayoutParam!!.rightMargin + rightViewLp.leftMargin
            val lRight: Int = lLeft + menuView!!.measuredWidth
            val lBottom: Int = lTop + menuView!!.measuredHeight
            menuView!!.layout(lLeft, lTop, lRight, lBottom)
        }
    }

    private var result: State? = null

    init {
        init(context, attrs, defStyleAttr)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                isSwiping = false
                if (lastP == null) {
                    lastP = PointF()
                }
                lastP!!.set(ev.rawX, ev.rawY)
                if (firstP == null) {
                    firstP = PointF()
                }
                firstP!!.set(ev.rawX, ev.rawY)
                if (viewCache != null) {
                    if (viewCache!!.get() != this) {
                        viewCache!!.get()!!.handlerSwipeMenu(State.CLOSE)
                    }
                    parent.requestDisallowInterceptTouchEvent(true)
                }
            }
            MotionEvent.ACTION_MOVE -> run {
                val distanceX: Float = lastP!!.x - ev.rawX
                val distanceY: Float = lastP!!.y - ev.rawY
                if (abs(distanceY) > scaledTouchSlop && abs(distanceY) > abs(distanceX)) {
                    return@run
                }
                scrollBy(distanceX.toInt(), 0)
                //越界修正
                if (scrollX < 0) {
                    scrollTo(0, 0)
                } else if (scrollX > 0) {
                    if (scrollX > menuView!!.right - contentView!!.right - contentViewLayoutParam!!.rightMargin) {
                        scrollTo(
                            menuView!!.right - contentView!!.right - contentViewLayoutParam!!.rightMargin,
                            0
                        )
                    }
                }
                //当处于水平滑动时，禁止父类拦截
                if (abs(distanceX) > scaledTouchSlop) {
                    parent.requestDisallowInterceptTouchEvent(true)
                }
                lastP!!.set(ev.rawX, ev.rawY)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                finalDistanceX = firstP!!.x - ev.rawX
                if (abs(finalDistanceX) > scaledTouchSlop) {
                    isSwiping = true
                }
                result = isShouldOpen()
                handlerSwipeMenu(result)
            }
            else -> {}
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {}
            MotionEvent.ACTION_MOVE -> {
                //滑动时拦截点击时间
                if (abs(finalDistanceX) > scaledTouchSlop) {
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                //滑动后不触发contentView的点击事件
                if (isSwiping) {
                    isSwiping = false
                    finalDistanceX = 0f
                    return true
                }
            }
        }
        return super.onInterceptTouchEvent(event)
    }

    /**
     * 自动设置状态
     *
     * @param result
     */
    private fun handlerSwipeMenu(result: State?) {
        if (result === State.RIGHT_OPEN) {
            viewCache = WeakReference(this)
            scroller!!.startScroll(
                scrollX,
                0,
                menuView!!.right - contentView!!.right - contentViewLayoutParam!!.rightMargin - scrollX,
                0
            )
            mStateCache = result
        } else {
            scroller!!.startScroll(scrollX, 0, -scrollX, 0)
            viewCache = null
            mStateCache = null
        }
        invalidate()
    }

    override fun computeScroll() {
        //判断Scroller是否执行完毕：
        if (scroller!!.computeScrollOffset()) {
            scrollTo(scroller!!.currX, scroller!!.currY)
            invalidate()
        }
    }

    /**
     * 根据当前的scrollX的值判断松开手后应处于何种状态
     *
     * @param
     * @param scrollX
     * @return
     */
    private fun isShouldOpen(): State? {
        if (scaledTouchSlop >= abs(finalDistanceX)) {
            return mStateCache
        }
        if (finalDistanceX < 0) {
            //关闭右边
            if (scrollX > 0 && menuView != null) {
                return State.CLOSE
            }
        } else if (finalDistanceX > 0) {
            //开启右边
            if (scrollX > 0 && menuView != null) {
                if (abs(menuView!!.width * fraction) < abs(scrollX)) {
                    return State.RIGHT_OPEN
                }
            }
        }
        return State.CLOSE
    }

    override fun onDetachedFromWindow() {
        if (this == viewCache?.get()) {
            viewCache!!.get()!!.handlerSwipeMenu(State.CLOSE)
        }
        super.onDetachedFromWindow()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (this == viewCache?.get()) {
            viewCache!!.get()!!.handlerSwipeMenu(mStateCache)
        }
    }


    companion object {
        var viewCache: WeakReference<SwipeLayout>? = null
            private set
        private var mStateCache: State? = null
    }

    enum class State {
        RIGHT_OPEN, CLOSE
    }
}