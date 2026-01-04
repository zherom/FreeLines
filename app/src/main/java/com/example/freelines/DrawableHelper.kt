package com.example.freelines

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat

object DrawableHelper {

    private val cache = mutableMapOf<Int, Drawable.ConstantState>()

    private val colorMapping = mapOf(
        0 to "#F44336", // Red
        1 to "#4CAF50", // Green
        2 to "#2196F3", // Blue
        3 to "#FFEB3B", // Yellow
        4 to "#9C27B0", // Purple
        5 to "#FF9800"  // Orange
    )

    fun getColoredDrawable(context: Context, colorType: Int): Drawable? {
        if (cache.containsKey(colorType)) {
            return cache[colorType]?.newDrawable(context.resources)?.mutate()
        }

        val templateDrawable = AppCompatResources.getDrawable(context, R.drawable.tile_occupied_template)?.mutate()
            ?: return AppCompatResources.getDrawable(context, R.drawable.tile_empty)

        val colorHex = colorMapping.getOrDefault(colorType, "#888888")
        val color = Color.parseColor(colorHex)

        // Wrap the drawable and apply the tint.
        // This will only affect the parts of the layer-list that are pure white.
        val wrappedDrawable = DrawableCompat.wrap(templateDrawable)
        DrawableCompat.setTint(wrappedDrawable, color)

        cache[colorType] = wrappedDrawable.constantState!!

        return wrappedDrawable
    }
}
