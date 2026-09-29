package com.glass.launcher

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.core.graphics.drawable.toBitmap

enum class GlassThemeStyle(val displayName: String, val description: String) {
    LIQUID_FROSTED("iOS Liquid Glass", "زجاج سائل شفاف نقي وانعكاسات بلورية مثل iOS 18"),
    OBSIDIAN_DARK("Obsidian Dark", "زجاج بلوري ليلي داكن بحواف متوهجة فخمة"),
    CYBER_NEON("Cyber Neon", "زجاج سايبر شفاف بحواف نيون ساطعة"),
    CRYSTAL_MONO("Crystal White", "كريستال زجاجي ناصع بأيقونات بيضاء نقية")
}

object LiquidGlassIconEngine {

    // Fast memory cache for rendered glass icons
    private val iconCache = LruCache<String, Bitmap>(250)

    fun getOrRender(
        context: Context,
        packageName: String,
        drawable: Drawable?,
        appName: String,
        style: GlassThemeStyle = GlassThemeStyle.LIQUID_FROSTED,
        size: Int = 220
    ): Bitmap {
        val cacheKey = "${packageName}_${style.name}_$size"
        val cached = iconCache.get(cacheKey)
        if (cached != null && !cached.isRecycled) {
            return cached
        }

        val rendered = renderGlassIcon(drawable, appName, style, size)
        iconCache.put(cacheKey, rendered)
        return rendered
    }

    fun clearCache() {
        iconCache.evictAll()
    }

    /**
     * Renders authentic iOS 18 / VisionOS Liquid Glass Squircle Tile
     */
    private fun renderGlassIcon(
        drawable: Drawable?,
        appName: String,
        style: GlassThemeStyle,
        size: Int
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isDither = true }

        val pad = size * 0.08f
        val tileSize = size - (pad * 2f)
        val cornerR = tileSize * 0.23f
        val rect = RectF(pad, pad, pad + tileSize, pad + tileSize)

        // 1. Soft Ambient Drop Shadow underneath the floating glass capsule
        paint.style = Paint.Style.FILL
        paint.shader = null
        paint.color = Color.parseColor("#48000000")
        val shadowRect = RectF(rect.left + 2f, rect.top + 6f, rect.right + 2f, rect.bottom + 9f)
        canvas.drawRoundRect(shadowRect, cornerR, cornerR, paint)

        // 2. Base Frosted Glass Fill
        when (style) {
            GlassThemeStyle.LIQUID_FROSTED -> {
                paint.shader = LinearGradient(
                    rect.left, rect.top, rect.right, rect.bottom,
                    intArrayOf(Color.parseColor("#44FFFFFF"), Color.parseColor("#18FFFFFF"), Color.parseColor("#22FFFFFF")),
                    floatArrayOf(0f, 0.5f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            GlassThemeStyle.OBSIDIAN_DARK -> {
                paint.shader = LinearGradient(
                    rect.left, rect.top, rect.right, rect.bottom,
                    intArrayOf(Color.parseColor("#801E222D"), Color.parseColor("#5010141D"), Color.parseColor("#60151822")),
                    floatArrayOf(0f, 0.5f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            GlassThemeStyle.CYBER_NEON -> {
                paint.shader = LinearGradient(
                    rect.left, rect.top, rect.right, rect.bottom,
                    intArrayOf(Color.parseColor("#4000F5D4"), Color.parseColor("#1800B4D8"), Color.parseColor("#250077B6")),
                    floatArrayOf(0f, 0.5f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            GlassThemeStyle.CRYSTAL_MONO -> {
                paint.shader = LinearGradient(
                    rect.left, rect.top, rect.right, rect.bottom,
                    intArrayOf(Color.parseColor("#50FFFFFF"), Color.parseColor("#20E0E0E0"), Color.parseColor("#35FFFFFF")),
                    floatArrayOf(0f, 0.5f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
        }
        canvas.drawRoundRect(rect, cornerR, cornerR, paint)

        // 3. Subtle Inner Gradient Depth (luminous at top, darker at bottom)
        paint.shader = LinearGradient(
            rect.left, rect.top, rect.left, rect.bottom,
            intArrayOf(Color.parseColor("#30FFFFFF"), Color.TRANSPARENT, Color.parseColor("#22000000")),
            floatArrayOf(0f, 0.45f, 1.0f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(rect, cornerR, cornerR, paint)

        // 4. Draw Inner App Icon Content
        if (drawable != null) {
            val iconPad = tileSize * 0.20f
            val iconRect = RectF(rect.left + iconPad, rect.top + iconPad, rect.right - iconPad, rect.bottom - iconPad)

            try {
                val origBitmap = try {
                    drawable.toBitmap(iconRect.width().toInt().coerceAtLeast(48), iconRect.height().toInt().coerceAtLeast(48))
                } catch (_: Exception) {
                    val b = Bitmap.createBitmap(iconRect.width().toInt().coerceAtLeast(48), iconRect.height().toInt().coerceAtLeast(48), Bitmap.Config.ARGB_8888)
                    val c = Canvas(b)
                    drawable.setBounds(0, 0, c.width, c.height)
                    drawable.draw(c)
                    b
                }

                // Mask app icon inside squircle capsule
                val innerR = cornerR * 0.65f
                val maskedBitmap = Bitmap.createBitmap(origBitmap.width, origBitmap.height, Bitmap.Config.ARGB_8888)
                val maskCanvas = Canvas(maskedBitmap)
                val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                val mRect = RectF(0f, 0f, origBitmap.width.toFloat(), origBitmap.height.toFloat())
                maskCanvas.drawRoundRect(mRect, innerR, innerR, maskPaint)
                maskPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
                maskCanvas.drawBitmap(origBitmap, 0f, 0f, maskPaint)

                // Soft inner shadow for depth
                paint.shader = null
                paint.style = Paint.Style.FILL
                paint.color = Color.parseColor("#35000000")
                canvas.drawRoundRect(RectF(iconRect.left, iconRect.top + 3f, iconRect.right, iconRect.bottom + 3f), innerR, innerR, paint)

                // Draw masked app icon
                canvas.drawBitmap(maskedBitmap, null, iconRect, null)
            } catch (_: Exception) {}
        } else {
            // Draw Fallback Monogram
            paint.shader = null
            paint.style = Paint.Style.FILL
            paint.color = Color.WHITE
            paint.textSize = tileSize * 0.45f
            paint.typeface = Typeface.DEFAULT_BOLD
            paint.textAlign = Paint.Align.CENTER
            val letter = if (appName.isNotBlank()) appName.take(1).uppercase() else "★"
            canvas.drawText(letter, rect.centerX(), rect.centerY() + (tileSize * 0.16f), paint)
        }

        // 5. Specular 3D Glass Reflection Arc across upper half
        paint.shader = LinearGradient(
            rect.left, rect.top, rect.right, rect.top + tileSize * 0.55f,
            intArrayOf(Color.parseColor("#60FFFFFF"), Color.parseColor("#18FFFFFF"), Color.TRANSPARENT),
            floatArrayOf(0.0f, 0.45f, 1.0f),
            Shader.TileMode.CLAMP
        )
        val glossPath = Path().apply {
            moveTo(rect.left, rect.top + cornerR)
            quadTo(rect.left, rect.top, rect.left + cornerR, rect.top)
            lineTo(rect.right - cornerR, rect.top)
            quadTo(rect.right, rect.top, rect.right, rect.top + cornerR)
            lineTo(rect.right, rect.top + tileSize * 0.35f)
            cubicTo(
                rect.right - tileSize * 0.20f, rect.top + tileSize * 0.50f,
                rect.left + tileSize * 0.20f, rect.top + tileSize * 0.55f,
                rect.left, rect.top + tileSize * 0.40f
            )
            close()
        }
        canvas.drawPath(glossPath, paint)

        // 6. Beveled Refractive Glass Border (Multi-tone metallic/crystal edge)
        paint.shader = LinearGradient(
            rect.left, rect.top, rect.right, rect.bottom,
            intArrayOf(
                Color.parseColor("#E0FFFFFF"),
                Color.parseColor("#50FFFFFF"),
                Color.parseColor("#18FFFFFF"),
                Color.parseColor("#90FFFFFF")
            ),
            floatArrayOf(0.0f, 0.35f, 0.70f, 1.0f),
            Shader.TileMode.CLAMP
        )
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.2f
        canvas.drawRoundRect(rect, cornerR, cornerR, paint)

        return bitmap
    }
}
