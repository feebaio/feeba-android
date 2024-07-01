import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Region
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.widget.FrameLayout

class ResizableFrameLayout(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    FrameLayout(context, attrs, defStyleAttr) {

    private var lastTouchX: Float = 0f
    private var lastTouchY: Float = 0f
    private var notchSize: Float = 50f
    private var notchColor: Int = android.graphics.Color.BLACK
    private var notchHoverColor: Int = android.graphics.Color.BLUE
    private var paint: Paint = Paint()
    private var isResizing: Boolean = false

    private val notchPath: Path
        get() {
            val path = Path()
            path.moveTo(width.toFloat(), height.toFloat())
            path.lineTo(width.toFloat(), height - notchSize)
            path.lineTo(width - notchSize, height.toFloat())
            path.close()
            return path
        }

    private val notchRegion: Region
        get() {
            val region = Region()
            region.setPath(notchPath, Region(0, 0, width, height))
            return region
        }


    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        paint.color = notchColor
        canvas.drawPath(notchPath, paint)
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
                if (notchRegion.contains(lastTouchX.toInt(), lastTouchY.toInt())) {
                    isResizing = true
                    notchSize *= 1.5f
                    paint.color = notchHoverColor
                    invalidate()
                    return true
                }
            }
        }
        return super.onInterceptTouchEvent(event)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isResizing) {
            return super.onTouchEvent(event)
        }

        when (event.action) {
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - lastTouchX
                val dy = event.y - lastTouchY

                val newWidth = (width + dx).toInt()
                val newHeight = (height + dy).toInt()

                layoutParams = layoutParams.apply {
                    width = newWidth
                    height = newHeight
                }
                lastTouchX = event.x
                lastTouchY = event.y
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isResizing = false
                notchSize /= 1.5f
                paint.color = notchColor
                invalidate()
                // Iterate child views and their class names
                for (i in 0 until childCount) {
                    Log.d("ResizableFrameLayout::onTouchEvent", "Child $i: ${getChildAt(i).javaClass.simpleName}")
                    getChildAt(i).requestLayout()
                }
//                Log.d("ResizableFrameLayout::requestLayout", "Width: $width, Height: $height")
//                forceLayout() // Trigger a re-measure of the children®
            }
        }
        return true
    }
}