package com.guy2.simpleStockGraph

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt

/**
 * SimpleBarsView - A bar chart for stock-like open/close visualization
 *
 * Each bar represents a segment (day) with open and close values.
 * - Green: close >= open (price went up)
 * - Red: close < open (price went down)
 * - Bar spans from open to close value
 */
class SimpleBarsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    /** Represents a single bar with open and close values */
    data class BarData(val open: Float, val close: Float) {
        val isPositive: Boolean get() = close >= open
        val min: Float get() = minOf(open, close)
        val max: Float get() = maxOf(open, close)
    }

    // ==================== Paints ====================
    private val positivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val negativePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val backgroundPaint = Paint().apply {
        style = Paint.Style.FILL
    }

    // ==================== Data ====================
    private var bars: List<BarData> = emptyList()
    private var minValue: Float? = null
    private var maxValue: Float? = null
    private var calculatedMin: Float = 0f
    private var calculatedMax: Float = 1f

    // ==================== Properties ====================
    @ColorInt
    var positiveColor: Int = COLOR_GREEN
        set(value) {
            field = value
            positivePaint.color = value
            invalidate()
        }

    @ColorInt
    var negativeColor: Int = COLOR_RED
        set(value) {
            field = value
            negativePaint.color = value
            invalidate()
        }

    /** Bar width as fraction of available space per bar (0.0 - 1.0) */
    var barWidthRatio: Float = 0.4f
        set(value) {
            field = value.coerceIn(0.1f, 1f)
            invalidate()
        }

    /** Corner radius for bars (only top corners are rounded) */
    var barCornerRadius: Float = 4f
        set(value) {
            field = value
            invalidate()
        }

    @ColorInt
    var graphBackground: Int = COLOR_DARK_BG
        set(value) {
            field = value
            invalidate()
        }

    var graphPadding: Float = 16f
        set(value) {
            field = value
            invalidate()
        }

    // ==================== Init ====================
    init {
        positivePaint.color = positiveColor
        negativePaint.color = negativeColor

        attrs?.let {
            val ta = context.obtainStyledAttributes(it, R.styleable.SimpleBarsView)
            try {
                positiveColor = ta.getColor(R.styleable.SimpleBarsView_sbv_positiveColor, COLOR_GREEN)
                negativeColor = ta.getColor(R.styleable.SimpleBarsView_sbv_negativeColor, COLOR_RED)
                barWidthRatio = ta.getFloat(R.styleable.SimpleBarsView_sbv_barWidthRatio, 0.4f)
                barCornerRadius = ta.getDimension(R.styleable.SimpleBarsView_sbv_barCornerRadius, 4f)
                graphBackground = ta.getColor(R.styleable.SimpleBarsView_sbv_backgroundColor, COLOR_DARK_BG)
                graphPadding = ta.getDimension(R.styleable.SimpleBarsView_sbv_graphPadding, 16f)
                minValue = if (ta.hasValue(R.styleable.SimpleBarsView_sbv_minValue))
                    ta.getFloat(R.styleable.SimpleBarsView_sbv_minValue, 0f) else null
                maxValue = if (ta.hasValue(R.styleable.SimpleBarsView_sbv_maxValue))
                    ta.getFloat(R.styleable.SimpleBarsView_sbv_maxValue, 0f) else null
            } finally {
                ta.recycle()
            }
        }

        if (isInEditMode) {
            bars = generatePreviewData()
            calculateMinMax()
        }
    }

    private fun generatePreviewData(): List<BarData> {
        return listOf(
            BarData(50f, 65f),   // +
            BarData(64f, 55f),   // -
            BarData(56f, 75f),   // +
            BarData(74f, 60f),   // -
            BarData(61f, 80f),   // +
            BarData(79f, 70f),   // -
            BarData(71f, 90f),   // +
            BarData(89f, 78f),   // -
            BarData(79f, 95f),   // +
            BarData(94f, 85f),   // -
            BarData(86f, 72f),   // -
            BarData(73f, 88f),   // +
            BarData(87f, 100f),  // +
            BarData(99f, 90f),   // -
            BarData(91f, 82f),   // -
            BarData(83f, 95f),   // +
            BarData(94f, 105f),  // +
            BarData(104f, 92f),  // -
            BarData(93f, 110f),  // +
            BarData(109f, 98f),  // -
            BarData(99f, 88f),   // -
            BarData(89f, 102f),  // +
            BarData(101f, 115f), // +
            BarData(114f, 105f), // -
            BarData(106f, 95f),  // -
            BarData(96f, 108f),  // +
            BarData(107f, 120f), // +
            BarData(119f, 110f), // -
            BarData(111f, 125f), // +
            BarData(124f, 115f)  // -
        )
    }

    // ==================== Data Methods ====================

    /** Set data from list of BarData */
    fun setData(data: List<BarData>) {
        bars = data.toList()
        calculateMinMax()
        invalidate()
    }

    /** Set data from paired arrays: opens[i] and closes[i] form one bar */
    fun setData(opens: FloatArray, closes: FloatArray) {
        require(opens.size == closes.size) { "Opens and closes arrays must have same size" }
        bars = opens.zip(closes.toList()) { open, close -> BarData(open, close) }
        calculateMinMax()
        invalidate()
    }

    /** Set data from list of Pairs (open, close) */
    fun setDataPairs(data: List<Pair<Float, Float>>) {
        bars = data.map { BarData(it.first, it.second) }
        calculateMinMax()
        invalidate()
    }

    /** Set data from flat array: [open1, close1, open2, close2, ...] */
    fun setDataFlat(data: FloatArray) {
        require(data.size % 2 == 0) { "Data array must have even number of elements" }
        bars = data.toList().chunked(2) { BarData(it[0], it[1]) }
        calculateMinMax()
        invalidate()
    }

    /** Set manual min/max. Pass null for auto-calculation */
    fun setMinMax(min: Float?, max: Float?) {
        minValue = min
        maxValue = max
        calculateMinMax()
        invalidate()
    }

    private fun calculateMinMax() {
        if (bars.isEmpty()) {
            calculatedMin = 0f
            calculatedMax = 1f
            return
        }

        val allMin = bars.minOfOrNull { it.min } ?: 0f
        val allMax = bars.maxOfOrNull { it.max } ?: 1f

        calculatedMin = minValue ?: allMin
        calculatedMax = maxValue ?: allMax

        // Avoid zero range
        if (calculatedMax == calculatedMin) {
            calculatedMax = calculatedMin + 1f
        }
    }

    // ==================== Drawing ====================

    private val barRect = RectF()
    private val barPath = Path()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw background
        backgroundPaint.color = graphBackground
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        if (bars.isEmpty()) return

        val graphWidth = width - 2 * graphPadding
        val graphHeight = height.toFloat()
        val barSpacing = graphWidth / bars.size
        val barWidth = barSpacing * barWidthRatio

        bars.forEachIndexed { index, bar ->
            val centerX = graphPadding + barSpacing * index + barSpacing / 2
            val left = centerX - barWidth / 2
            val right = centerX + barWidth / 2

            // Calculate Y positions (inverted: 0 at top)
            val range = calculatedMax - calculatedMin
            val topY = graphHeight * (1 - (bar.max - calculatedMin) / range)
            val bottomY = graphHeight * (1 - (bar.min - calculatedMin) / range)

            val paint = if (bar.isPositive) positivePaint else negativePaint

            // Draw bar with rounded corners
            if (barCornerRadius > 0 && (bottomY - topY) > barCornerRadius * 2) {
                barPath.reset()
                barRect.set(left, topY, right, bottomY)
                barPath.addRoundRect(barRect, barCornerRadius, barCornerRadius, Path.Direction.CW)
                canvas.drawPath(barPath, paint)
            } else {
                canvas.drawRect(left, topY, right, bottomY, paint)
            }
        }
    }

    // ==================== Builder-style setters ====================

    fun applyColors(positive: Int, negative: Int): SimpleBarsView {
        positiveColor = positive
        negativeColor = negative
        return this
    }

    fun applyBarStyle(widthRatio: Float, cornerRadius: Float): SimpleBarsView {
        barWidthRatio = widthRatio
        barCornerRadius = cornerRadius
        return this
    }

    companion object {
        val COLOR_GREEN = Color.parseColor("#4ADE80")
        val COLOR_RED = Color.parseColor("#F87171")
        val COLOR_PINK = Color.parseColor("#EC4899")
        val COLOR_CYAN = Color.parseColor("#22D3EE")
        val COLOR_DARK_BG = Color.parseColor("#1A1A2E")

        /** Helper to create BarData */
        fun bar(open: Float, close: Float) = BarData(open, close)
    }
}