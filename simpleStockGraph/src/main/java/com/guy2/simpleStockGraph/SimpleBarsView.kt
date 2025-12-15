package com.guy2.simpleStockGraph

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt

/**
 * SimpleBarsView - A lightweight bar chart for displaying gains/losses
 *
 * Features:
 * - Green bars for positive values, red for negative
 * - Customizable colors, bar width, spacing, corner radius
 * - Supports baseline at zero or custom position
 * - XML preview support
 */
class SimpleBarsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

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
    private var dataPoints: FloatArray = floatArrayOf()
    private var minValue: Float? = null
    private var maxValue: Float? = null
    private var calculatedMin: Float = -1f
    private var calculatedMax: Float = 1f
    private var baselineValue: Float = 0f

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

    /** Corner radius for bars */
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

    /** Vertical padding ratio to prevent bars touching edges (0.0 - 0.5) */
    var verticalPaddingRatio: Float = 0.1f
        set(value) {
            field = value.coerceIn(0f, 0.5f)
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
                verticalPaddingRatio = ta.getFloat(R.styleable.SimpleBarsView_sbv_verticalPaddingRatio, 0.1f)
            } finally {
                ta.recycle()
            }
        }

        if (isInEditMode) {
            dataPoints = generatePreviewData()
            calculateMinMax()
        }
    }

    private fun generatePreviewData(): FloatArray {
        return floatArrayOf(
            3f, -2f, 5f, -4f, 2f, -1f, 6f, -3f, 4f, -5f,
            2f, -2f, 7f, -4f, 3f, -6f, 5f, -2f, 4f, -3f,
            6f, -1f, 3f, -4f, 5f, -2f, 4f, -5f, 3f, -3f
        )
    }

    // ==================== Data Methods ====================

    fun setData(data: FloatArray) {
        dataPoints = data.copyOf()
        calculateMinMax()
        invalidate()
    }

    fun setData(data: IntArray) {
        dataPoints = data.map { it.toFloat() }.toFloatArray()
        calculateMinMax()
        invalidate()
    }

    fun setData(data: List<Number>) {
        dataPoints = data.map { it.toFloat() }.toFloatArray()
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

    /** Set custom baseline value (default is 0) */
    fun setBaseline(value: Float) {
        baselineValue = value
        invalidate()
    }

    private fun calculateMinMax() {
        if (dataPoints.isEmpty()) {
            calculatedMin = -1f
            calculatedMax = 1f
            return
        }

        val dataMin = dataPoints.minOrNull() ?: 0f
        val dataMax = dataPoints.maxOrNull() ?: 0f

        // Ensure baseline is included in range
        calculatedMin = minValue ?: minOf(dataMin, baselineValue)
        calculatedMax = maxValue ?: maxOf(dataMax, baselineValue)

        // Add padding
        val range = calculatedMax - calculatedMin
        if (range > 0) {
            val padding = range * verticalPaddingRatio
            calculatedMin -= padding
            calculatedMax += padding
        }

        // Avoid zero range
        if (calculatedMax == calculatedMin) {
            calculatedMax = calculatedMin + 1f
        }
    }

    // ==================== Drawing ====================

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw background
        backgroundPaint.color = graphBackground
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        if (dataPoints.isEmpty()) return

        val graphWidth = width - 2 * graphPadding
        val graphHeight = height - 2 * graphPadding
        val barSpacing = graphWidth / dataPoints.size
        val barWidth = barSpacing * barWidthRatio

        // Calculate baseline Y position
        val baselineY = graphPadding + graphHeight * (1 - (baselineValue - calculatedMin) / (calculatedMax - calculatedMin))

        dataPoints.forEachIndexed { index, value ->
            val centerX = graphPadding + barSpacing * index + barSpacing / 2
            val left = centerX - barWidth / 2
            val right = centerX + barWidth / 2

            // Calculate bar top/bottom
            val valueY = graphPadding + graphHeight * (1 - (value - calculatedMin) / (calculatedMax - calculatedMin))

            val paint = if (value >= baselineValue) positivePaint else negativePaint

            val top: Float
            val bottom: Float
            if (value >= baselineValue) {
                top = valueY
                bottom = baselineY
            } else {
                top = baselineY
                bottom = valueY
            }

            // Draw rounded rect bar
            if (barCornerRadius > 0) {
                canvas.drawRoundRect(left, top, right, bottom, barCornerRadius, barCornerRadius, paint)
            } else {
                canvas.drawRect(left, top, right, bottom, paint)
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
    }
}