package com.arrowpuzzle.game.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.os.SystemClock
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.OvershootInterpolator
import com.arrowpuzzle.game.audio.SoundSynthesizer
import com.arrowpuzzle.game.engine.CollisionDetector
import com.arrowpuzzle.game.haptics.HapticController
import com.arrowpuzzle.game.model.ArrowModel
import com.arrowpuzzle.game.model.ArrowType
import com.arrowpuzzle.game.model.LevelData
import java.util.Random
import kotlin.math.cos
import kotlin.math.sin

/**
 * Interactive hardware-accelerated Canvas rendering the puzzle board,
 * arrow launch dynamics, blocked collision physics, particle FX, and combos.
 */
class ArrowBoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    interface GameListener {
        fun onArrowCleared(clearedArrow: ArrowModel, remainingCount: Int, comboStreak: Int)
        fun onArrowBlocked(blockedArrow: ArrowModel)
        fun onLevelCompleted()
    }

    var listener: GameListener? = null
    var hapticController: HapticController? = null

    private var levelData: LevelData? = null
    private val activeArrows = mutableListOf<ArrowModel>()

    private var cellSize: Float = 0f
    private var originX: Float = 0f
    private var originY: Float = 0f

    // Visual Paints
    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val headPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.WHITE
        strokeWidth = 3f
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#FEF08A")
        strokeWidth = 14f
        alpha = 180
    }
    private val gridBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#152033")
        style = Paint.Style.FILL
    }
    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 48f
        isFakeBoldText = true
        color = Color.parseColor("#FBBF24")
    }

    // Touch & Combo Timing
    private var lastTapTime = 0L
    private val touchCooldownMs = 80L
    private var comboStreak = 0
    private var lastClearTime = 0L
    private val comboWindowMs = 1500L

    // Visual Effects
    private val particles = mutableListOf<Particle>()
    private val floatingTexts = mutableListOf<FloatingText>()
    private val random = Random()

    fun loadLevel(data: LevelData) {
        this.levelData = data
        this.activeArrows.clear()
        // Deep copy arrow models for fresh level run
        this.activeArrows.addAll(data.arrows.map { it.copy() })
        this.comboStreak = 0
        this.particles.clear()
        this.floatingTexts.clear()
        recalculateDimensions()
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        recalculateDimensions()
    }

    private fun recalculateDimensions() {
        val data = levelData ?: return
        if (width <= 0 || height <= 0) return

        val padX = 64f
        val padY = 64f
        val availW = width - padX * 2f
        val availH = height - padY * 2f

        val cellW = availW / data.gridWidth.toFloat()
        val cellH = availH / data.gridHeight.toFloat()
        cellSize = Math.min(cellW, cellH)

        val totalBoardW = cellSize * data.gridWidth.toFloat()
        val totalBoardH = cellSize * data.gridHeight.toFloat()

        originX = (width - totalBoardW) / 2f
        originY = (height - totalBoardH) / 2f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val data = levelData ?: return

        // 1. Draw subtle board backdrop card
        val boardRect = RectF(
            originX - 16f,
            originY - 16f,
            originX + data.gridWidth * cellSize + 16f,
            originY + data.gridHeight * cellSize + 16f
        )
        canvas.drawRoundRect(boardRect, 32f, 32f, gridBgPaint)

        // 2. Draw all active arrows
        for (arrow in activeArrows) {
            if (arrow.isRemoved) continue
            drawArrow(canvas, arrow)
        }

        // 3. Draw active particle burst FX
        drawParticles(canvas)

        // 4. Draw floating combo badges
        drawFloatingTexts(canvas)

        // Keep 60/120fps display animation loop active if anything is animating
        if (activeArrows.any { it.isLaunched || it.isWiggling } || particles.isNotEmpty() || floatingTexts.isNotEmpty()) {
            postInvalidateOnAnimation()
        }
    }

    private fun drawArrow(canvas: Canvas, arrow: ArrowModel) {
        val halfLen = (arrow.length * cellSize) / 2f
        var cx = originX + arrow.gridX * cellSize + cellSize / 2f
        var cy = originY + arrow.gridY * cellSize + cellSize / 2f

        // Apply launch displacement if currently shooting off-screen
        if (arrow.isLaunched) {
            val flyDistance = Math.max(width, height).toFloat() * 1.5f * arrow.launchProgress
            cx += arrow.direction.dx * flyDistance
            cy += arrow.direction.dy * flyDistance
        }

        // Apply wiggle displacement if blocked
        if (arrow.isWiggling) {
            cx += arrow.wiggleOffsetX
            cy += arrow.wiggleOffsetY
        }

        val dir = arrow.direction
        val hx = cx + dir.dx * halfLen
        val hy = cy + dir.dy * halfLen
        val tx = cx - dir.dx * halfLen
        val ty = cy - dir.dy * halfLen

        val arrowThickness = cellSize * 0.28f
        bodyPaint.strokeWidth = arrowThickness
        val arrowColor = if (arrow.isWiggling) Color.parseColor("#EF4444") else arrow.color.primaryHex
        bodyPaint.color = arrowColor
        headPaint.color = arrowColor

        // Highlight halo for Hint booster
        if (arrow.isHighlighted) {
            canvas.drawLine(tx, ty, hx, hy, glowPaint)
        }

        // Shaft (Capsule line)
        canvas.drawLine(tx, ty, hx, hy, bodyPaint)

        // Chevron Head at front
        val headSize = arrowThickness * 2.1f
        drawChevronHead(canvas, hx, hy, dir.angleDegrees, headSize, headPaint)

        // Opposing chevron head if bi-directional arrow
        if (arrow.type == ArrowType.DUAL_HEAD) {
            drawChevronHead(canvas, tx, ty, dir.opposite.angleDegrees, headSize, headPaint)
        }
    }

    private fun drawChevronHead(canvas: Canvas, x: Float, y: Float, angle: Float, size: Float, paint: Paint) {
        val rad = Math.toRadians(angle.toDouble())
        val cosA = cos(rad).toFloat()
        val sinA = sin(rad).toFloat()

        val perpX = -sinA
        val perpY = cosA

        val tipX = x + cosA * (size * 0.6f)
        val tipY = y + sinA * (size * 0.6f)

        val leftX = x - cosA * (size * 0.4f) + perpX * (size * 0.65f)
        val leftY = y - sinA * (size * 0.4f) + perpY * (size * 0.65f)

        val rightX = x - cosA * (size * 0.4f) - perpX * (size * 0.65f)
        val rightY = y - sinA * (size * 0.4f) - perpY * (size * 0.65f)

        val path = Path().apply {
            moveTo(tipX, tipY)
            lineTo(leftX, leftY)
            lineTo(x, y)
            lineTo(rightX, rightY)
            close()
        }
        canvas.drawPath(path, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_DOWN) return super.onTouchEvent(event)
        val data = levelData ?: return false

        val now = SystemClock.uptimeMillis()
        if (now - lastTapTime < touchCooldownMs) return true
        lastTapTime = now

        val touchX = event.x
        val touchY = event.y

        // Find candidate arrow clicked
        val hitArrow = findArrowAt(touchX, touchY) ?: return true

        // Verify trajectory
        val isClear = CollisionDetector.isPathClear(hitArrow, activeArrows, data.gridWidth, data.gridHeight)

        if (isClear) {
            launchArrow(hitArrow)
        } else {
            triggerBlockedWiggle(hitArrow)
        }

        return true
    }

    private fun findArrowAt(px: Float, py: Float): ArrowModel? {
        val clickRadius = cellSize * 0.7f
        var bestArrow: ArrowModel? = null
        var bestDist = Float.MAX_VALUE

        for (arrow in activeArrows) {
            if (arrow.isRemoved || arrow.isLaunched) continue
            val cx = originX + arrow.gridX * cellSize + cellSize / 2f
            val cy = originY + arrow.gridY * cellSize + cellSize / 2f
            val dist = Math.hypot((px - cx).toDouble(), (py - cy).toDouble()).toFloat()
            if (dist < clickRadius && dist < bestDist) {
                bestDist = dist
                bestArrow = arrow
            }
        }
        return bestArrow
    }

    private fun launchArrow(arrow: ArrowModel) {
        arrow.isLaunched = true
        arrow.isHighlighted = false

        // Timing & Combo Calculation
        val now = SystemClock.uptimeMillis()
        if (now - lastClearTime < comboWindowMs) {
            comboStreak++
        } else {
            comboStreak = 1
        }
        lastClearTime = now

        // Audio & Haptics
        SoundSynthesizer.playLaunchWhoosh()
        if (comboStreak > 1) {
            SoundSynthesizer.playCombo(comboStreak - 1)
            spawnComboText(arrow, comboStreak)
        }
        hapticController?.vibrateTap()

        // Spawn particle sparkle at launch point
        val cx = originX + arrow.gridX * cellSize + cellSize / 2f
        val cy = originY + arrow.gridY * cellSize + cellSize / 2f
        spawnParticles(cx, cy, arrow.color.primaryHex)

        // Smooth launch animation
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 200
            interpolator = AccelerateInterpolator(1.8f)
            addUpdateListener { anim ->
                arrow.launchProgress = anim.animatedValue as Float
                invalidate()
            }
            doOnEnd {
                arrow.isRemoved = true
                arrow.isLaunched = false
                val remaining = activeArrows.count { !it.isRemoved }
                listener?.onArrowCleared(arrow, remaining, comboStreak)

                if (remaining == 0) {
                    SoundSynthesizer.playVictory()
                    hapticController?.vibrateSuccess()
                    listener?.onLevelCompleted()
                }
            }
            start()
        }
    }

    private fun triggerBlockedWiggle(arrow: ArrowModel) {
        if (arrow.isWiggling) return
        arrow.isWiggling = true
        comboStreak = 0

        SoundSynthesizer.playBlockedThud()
        hapticController?.vibrateError()
        listener?.onArrowBlocked(arrow)

        val perpX = -arrow.direction.dy
        val perpY = arrow.direction.dx
        val wiggleAmplitude = 8f

        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 140
            interpolator = OvershootInterpolator(2.5f)
            addUpdateListener { anim ->
                val p = anim.animatedValue as Float
                val offset = sin(p * Math.PI * 4.0).toFloat() * wiggleAmplitude * (1f - p)
                arrow.wiggleOffsetX = perpX * offset
                arrow.wiggleOffsetY = perpY * offset
                invalidate()
            }
            doOnEnd {
                arrow.isWiggling = false
                arrow.wiggleOffsetX = 0f
                arrow.wiggleOffsetY = 0f
                invalidate()
            }
            start()
        }
    }

    /**
     * Executes the Hint booster: highlights the first clearable arrow on board.
     */
    fun triggerHint(): Boolean {
        val data = levelData ?: return false
        val clearable = CollisionDetector.findClearableArrows(activeArrows, data.gridWidth, data.gridHeight)
        if (clearable.isNotEmpty()) {
            activeArrows.forEach { it.isHighlighted = false }
            clearable.first().isHighlighted = true
            SoundSynthesizer.playClick()
            hapticController?.vibrateTap()
            invalidate()
            return true
        }
        return false
    }

    /**
     * Executes the Auto-Clear booster: automatically fires up to [count] clearable arrows.
     */
    fun triggerAutoClear(count: Int = 3) {
        val data = levelData ?: return
        var fired = 0
        for (i in 0 until count) {
            postDelayed({
                val clearable = CollisionDetector.findClearableArrows(activeArrows, data.gridWidth, data.gridHeight)
                if (clearable.isNotEmpty()) {
                    launchArrow(clearable.first())
                }
            }, (i * 240L))
        }
    }

    private fun spawnParticles(cx: Float, cy: Float, color: Int) {
        for (i in 0 until 16) {
            val angle = random.nextFloat() * Math.PI.toFloat() * 2f
            val speed = 3f + random.nextFloat() * 7f
            particles.add(
                Particle(
                    x = cx,
                    y = cy,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed,
                    color = color,
                    size = 5f + random.nextFloat() * 6f,
                    life = 1f
                )
            )
        }
    }

    private fun drawParticles(canvas: Canvas) {
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.x += p.vx
            p.y += p.vy
            p.life -= 0.04f
            if (p.life <= 0f) {
                iterator.remove()
                continue
            }
            particlePaint.color = p.color
            particlePaint.alpha = (p.life * 255).toInt().coerceIn(0, 255)
            canvas.drawCircle(p.x, p.y, p.size * p.life, particlePaint)
        }
    }

    private fun spawnComboText(arrow: ArrowModel, streak: Int) {
        val cx = originX + arrow.gridX * cellSize + cellSize / 2f
        val cy = originY + arrow.gridY * cellSize + cellSize / 2f
        val text = when {
            streak >= 6 -> "EPIC!"
            streak >= 4 -> "WONDERFUL!"
            streak >= 3 -> "AWESOME!"
            else -> "GOOD!"
        }
        floatingTexts.add(FloatingText(cx, cy, text, 1f))
    }

    private fun drawFloatingTexts(canvas: Canvas) {
        val iterator = floatingTexts.iterator()
        while (iterator.hasNext()) {
            val ft = iterator.next()
            ft.y -= 2f
            ft.life -= 0.03f
            if (ft.life <= 0f) {
                iterator.remove()
                continue
            }
            textPaint.alpha = (ft.life * 255).toInt().coerceIn(0, 255)
            canvas.drawText(ft.text, ft.x, ft.y, textPaint)
        }
    }

    private fun ValueAnimator.doOnEnd(action: () -> Unit) {
        addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                action()
            }
        })
    }

    private data class Particle(
        var x: Float, var y: Float,
        var vx: Float, var vy: Float,
        val color: Int, val size: Float,
        var life: Float
    )

    private data class FloatingText(
        var x: Float, var y: Float,
        val text: String, var life: Float
    )
}
