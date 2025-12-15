package com.guy2.simpleStockGraph

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt

/**
 * SimpleGraphView - A lightweight line graph view with gradient fill
 */
class SimpleGraphView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class LineMode {
        STRAIGHT,  // Sharp corners at each point
        CURVED     // Smooth bezier curves between points
    }

    // ==================== Paints (MUST be initialized first!) ====================
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val backgroundPaint = Paint().apply {
        style = Paint.Style.FILL
    }

    // Paths
    private val linePath = Path()
    private val fillPath = Path()

    // ==================== Data ====================
    private var dataPoints: FloatArray = floatArrayOf()
    private var minValue: Float? = null
    private var maxValue: Float? = null
    private var calculatedMin: Float = 0f
    private var calculatedMax: Float = 1f

    // ==================== Properties ====================
    @ColorInt
    var strokeColor: Int = COLOR_GREEN
        set(value) {
            field = value
            linePaint.color = value
            invalidate()
        }

    var strokeWidth: Float = 4f
        set(value) {
            field = value
            linePaint.strokeWidth = value
            invalidate()
        }

    var lineMode: LineMode = LineMode.CURVED
        set(value) {
            field = value
            invalidate()
        }

    /** Curve smoothness factor: 0.0 = sharp, 0.5 = very smooth. Default 0.2 */
    var curveSmoothness: Float = 0.2f
        set(value) {
            field = value.coerceIn(0f, 0.5f)
            invalidate()
        }

    var gradientEnabled: Boolean = true
        set(value) {
            field = value
            invalidate()
        }

    @ColorInt
    var gradientColor: Int = COLOR_GREEN
        set(value) {
            field = value
            updateGradient()
            invalidate()
        }

    var gradientAlpha: Int = 80
        set(value) {
            field = value.coerceIn(0, 255)
            updateGradient()
            invalidate()
        }

    @ColorInt
    var graphBackground: Int = COLOR_DARK_BG
        set(value) {
            field = value
            invalidate()
        }

    var graphPadding: Float = 8f
        set(value) {
            field = value
            invalidate()
        }

    // ==================== Init ====================
    init {
        // Set default paint values
        linePaint.color = strokeColor
        linePaint.strokeWidth = strokeWidth

        // Parse XML attributes if present
        attrs?.let {
            val ta = context.obtainStyledAttributes(it, R.styleable.SimpleGraphView)
            try {
                strokeColor = ta.getColor(R.styleable.SimpleGraphView_sgv_strokeColor, COLOR_GREEN)
                strokeWidth = ta.getDimension(R.styleable.SimpleGraphView_sgv_strokeWidth, 4f)
                gradientEnabled = ta.getBoolean(R.styleable.SimpleGraphView_sgv_gradientEnabled, true)
                gradientColor = ta.getColor(R.styleable.SimpleGraphView_sgv_gradientColor, strokeColor)
                gradientAlpha = ta.getInt(R.styleable.SimpleGraphView_sgv_gradientAlpha, 80)
                graphBackground = ta.getColor(R.styleable.SimpleGraphView_sgv_backgroundColor, COLOR_DARK_BG)
                graphPadding = ta.getDimension(R.styleable.SimpleGraphView_sgv_graphPadding, 8f)
                curveSmoothness = ta.getFloat(R.styleable.SimpleGraphView_sgv_curveSmoothness, 0.2f)

                val modeInt = ta.getInt(R.styleable.SimpleGraphView_sgv_lineMode, 1)
                lineMode = if (modeInt == 0) LineMode.STRAIGHT else LineMode.CURVED
            } finally {
                ta.recycle()
            }
        }

        // Preview data for XML editor
        if (isInEditMode) {
            dataPoints = generatePreviewData()
        }
    }

    private fun generatePreviewData(): FloatArray {
        // Generate sample data that looks like a stock chart
        return floatArrayOf(
            50f, 55f, 52f, 60f, 58f, 65f, 62f, 70f, 68f, 75f,
            72f, 78f, 74f, 80f, 76f, 82f, 79f, 85f, 82f, 88f
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

    fun setMinMax(min: Float?, max: Float?) {
        minValue = min
        maxValue = max
        calculateMinMax()
        invalidate()
    }

    private fun calculateMinMax() {
        if (dataPoints.isEmpty()) {
            calculatedMin = 0f
            calculatedMax = 1f
            return
        }

        calculatedMin = minValue ?: dataPoints.minOrNull() ?: 0f
        calculatedMax = maxValue ?: dataPoints.maxOrNull() ?: 1f

        if (calculatedMax == calculatedMin) {
            calculatedMax = calculatedMin + 1f
        }
    }

    // ==================== Drawing ====================

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateGradient()

        // Recalculate for preview
        if (isInEditMode && dataPoints.isNotEmpty()) {
            calculateMinMax()
        }
    }

    private fun updateGradient() {
        if (height > 0) {
            val topColor = Color.argb(
                gradientAlpha,
                Color.red(gradientColor),
                Color.green(gradientColor),
                Color.blue(gradientColor)
            )
            val bottomColor = Color.argb(
                0,
                Color.red(gradientColor),
                Color.green(gradientColor),
                Color.blue(gradientColor)
            )

            fillPaint.shader = LinearGradient(
                0f, graphPadding,
                0f, height - graphPadding,
                topColor,
                bottomColor,
                Shader.TileMode.CLAMP
            )
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw background
        backgroundPaint.color = graphBackground
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        if (dataPoints.size < 2) return

        // Calculate points
        val points = calculatePoints()

        // Build paths based on line mode
        when (lineMode) {
            LineMode.STRAIGHT -> buildStraightPaths(points)
            LineMode.CURVED -> buildCurvedPaths(points)
        }

        // Draw gradient fill
        if (gradientEnabled) {
            canvas.drawPath(fillPath, fillPaint)
        }

        // Draw line
        canvas.drawPath(linePath, linePaint)
    }

    private fun calculatePoints(): List<PointF> {
        val graphWidth = width - 2 * graphPadding
        val graphHeight = height - 2 * graphPadding
        val stepX = graphWidth / (dataPoints.size - 1)

        return dataPoints.mapIndexed { index, value ->
            val x = graphPadding + index * stepX
            val normalizedY = (value - calculatedMin) / (calculatedMax - calculatedMin)
            val y = graphPadding + graphHeight * (1 - normalizedY)
            PointF(x, y)
        }
    }

    private fun buildStraightPaths(points: List<PointF>) {
        linePath.reset()
        fillPath.reset()

        val bottomY = height - graphPadding

        points.forEachIndexed { index, point ->
            if (index == 0) {
                linePath.moveTo(point.x, point.y)
                fillPath.moveTo(point.x, bottomY)
                fillPath.lineTo(point.x, point.y)
            } else {
                linePath.lineTo(point.x, point.y)
                fillPath.lineTo(point.x, point.y)
            }
        }

        // Close fill path
        fillPath.lineTo(points.last().x, bottomY)
        fillPath.close()
    }

    private fun buildCurvedPaths(points: List<PointF>) {
        linePath.reset()
        fillPath.reset()

        if (points.isEmpty()) return

        val bottomY = height - graphPadding

        // Start fill path from bottom
        fillPath.moveTo(points.first().x, bottomY)
        fillPath.lineTo(points.first().x, points.first().y)

        // Start line path
        linePath.moveTo(points.first().x, points.first().y)

        // Draw cubic bezier curves between points
        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val curr = points[i]

            // Calculate control points
            val deltaX = (curr.x - prev.x) * curveSmoothness

            val cp1x = prev.x + deltaX
            val cp1y = prev.y
            val cp2x = curr.x - deltaX
            val cp2y = curr.y

            linePath.cubicTo(cp1x, cp1y, cp2x, cp2y, curr.x, curr.y)
            fillPath.cubicTo(cp1x, cp1y, cp2x, cp2y, curr.x, curr.y)
        }

        // Close fill path
        fillPath.lineTo(points.last().x, bottomY)
        fillPath.close()
    }

    // ==================== Builder-style setters ====================

    fun applyStroke(color: Int, width: Float): SimpleGraphView {
        strokeColor = color
        strokeWidth = width
        return this
    }

    fun applyGradient(enabled: Boolean, color: Int = strokeColor, alpha: Int = 80): SimpleGraphView {
        gradientEnabled = enabled
        gradientColor = color
        gradientAlpha = alpha
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