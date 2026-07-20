package com.dicereligion.edgecase

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView

/**
 * A reusable control row — `[label] |———slider———| [value]` — used for every slider in the Customize
 * dialog. Maps the SeekBar's `0..STEPS` integer progress onto a float range `[min, max]` and reports
 * user-driven changes via the [configure] callback.
 *
 * Declared directly in XML; children are built here in [init].
 */
class LabeledSeekBar(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {

    private val labelView = TextView(context)
    private val seekBar = SeekBar(context)
    private val valueView = TextView(context)

    private var minValue = 0f
    private var maxValue = 1f
    private var formatter: (Float) -> String = { it.toString() }
    private var onChange: ((Float) -> Unit)? = null

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val d = resources.displayMetrics.density
        val padV = (6 * d).toInt()
        setPadding(0, padV, 0, padV)

        labelView.apply {
            setTextColor(Color.parseColor("#F5EFE6")) // aged marble
            textSize = 13f
            width = (104 * d).toInt()
        }
        seekBar.max = STEPS
        seekBar.thumb = context.getDrawable(R.drawable.ic_gem_thumb)   // square-gem thumb (§9, B11)
        valueView.apply {
            setTextColor(Color.parseColor("#9AA0A6")) // tarnished silver
            textSize = 12f
            gravity = Gravity.END
            width = (56 * d).toInt()
        }

        addView(labelView)
        addView(seekBar, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        addView(valueView)

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = minValue + (maxValue - minValue) * (progress.toFloat() / STEPS)
                valueView.text = formatter(value)
                if (fromUser) onChange?.invoke(value)
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
    }

    fun configure(
        label: String,
        min: Float,
        max: Float,
        value: Float,
        formatter: (Float) -> String,
        onChange: (Float) -> Unit
    ) {
        this.minValue = min
        this.maxValue = max
        this.formatter = formatter
        this.onChange = onChange
        labelView.text = label
        setValue(value)
    }

    /** Update the displayed value without firing [onChange] (e.g. on Reset). */
    fun setValue(value: Float) {
        val clamped = value.coerceIn(minValue, maxValue)
        seekBar.progress = (((clamped - minValue) / (maxValue - minValue)) * STEPS).toInt()
        valueView.text = formatter(clamped)
    }

    /** The underlying SeekBar, e.g. to paint a custom hue-spectrum track. */
    fun seek(): SeekBar = seekBar

    companion object {
        private const val STEPS = 1000
    }
}
