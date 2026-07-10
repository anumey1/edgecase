package com.dicereligion.edgecase

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog

/**
 * The "Customize Sliver" popup shown from the Position screen. Edits a working copy of [SliverConfig]
 * with a live preview; on Apply it persists the config and hands it back via the callback so the caller
 * can refresh the preview and hot-reload the running overlay.
 */
class SliverCustomizeDialog private constructor(
    private val context: Context,
    initial: SliverConfig,
    private val onApplied: (SliverConfig) -> Unit
) {
    private val working: SliverConfig = initial.copy()
    private val view: View = LayoutInflater.from(context).inflate(R.layout.dialog_customize_sliver, null)
    private val dialog: AlertDialog = AlertDialog.Builder(context).setView(view).create()

    private val preview = view.findViewById<SliverPreviewView>(R.id.sliverPreview)
    private val rgColorMode = view.findViewById<RadioGroup>(R.id.rgColorMode)
    private val hueContainer = view.findViewById<View>(R.id.hueContainer)
    private val rowHue = view.findViewById<LabeledSeekBar>(R.id.rowHue)
    private val colorSwatch = view.findViewById<View>(R.id.colorSwatch)
    private val etWidth = view.findViewById<EditText>(R.id.etWidth)
    private val etHeight = view.findViewById<EditText>(R.id.etHeight)

    private var binding = false

    private fun build() {
        // Preview mirrors the saved edge.
        val prefs = context.getSharedPreferences(SliverConfig.PREFS, Context.MODE_PRIVATE)
        preview.side = if (prefs.getString("sliver_side", "right") == "left")
            ArcSliverView.Side.LEFT else ArcSliverView.Side.RIGHT

        paintHueTrack()
        bindControls()

        rgColorMode.setOnCheckedChangeListener { _, checkedId ->
            if (binding) return@setOnCheckedChangeListener
            working.colorMode = if (checkedId == R.id.rbColorCustom)
                SliverConfig.ColorMode.CUSTOM else SliverConfig.ColorMode.DEFAULT
            updateColorSection()
            refresh()
        }

        etWidth.addTextChangedListener(sizeWatcher(isWidth = true))
        etHeight.addTextChangedListener(sizeWatcher(isWidth = false))

        view.findViewById<Button>(R.id.btnResetSliver).setOnClickListener {
            val defaults = SliverConfig()
            copyInto(working, defaults)
            bindControls()
            refresh()
        }
        view.findViewById<Button>(R.id.btnCancelSliver).setOnClickListener { dialog.dismiss() }
        view.findViewById<Button>(R.id.btnApplySliver).setOnClickListener {
            working.widthDp = parseDp(etWidth, working.widthDp, 8f, 160f)
            working.heightDp = parseDp(etHeight, working.heightDp, 12f, 240f)
            working.save(context)
            onApplied(working)
            dialog.dismiss()
        }

        dialog.show()
        dialog.window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.92f).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    /** (Re)configure every control from [working]. */
    private fun bindControls() {
        binding = true

        configureRow(R.id.rowOpacity, "Opacity", 0f, 1f, working.opacity, { "${(it * 100).toInt()}%" }) {
            working.opacity = it; refresh()
        }
        rowHue.configure("Hue", 0f, 360f, working.customHue, { "${it.toInt()}°" }) {
            working.customHue = it; updateSwatch(); refresh()
        }
        configureRow(R.id.rowT1Thickness, "Top thick", 0.02f, 0.35f, working.tooth1Thickness, ::frac) {
            working.tooth1Thickness = it; refresh()
        }
        configureRow(R.id.rowT2Thickness, "Bot thick", 0.02f, 0.35f, working.tooth2Thickness, ::frac) {
            working.tooth2Thickness = it; refresh()
        }
        configureRow(R.id.rowT1Length, "Top length", 0.10f, 0.95f, working.tooth1Length, ::frac) {
            working.tooth1Length = it; refresh()
        }
        configureRow(R.id.rowT2Length, "Bot length", 0.10f, 0.95f, working.tooth2Length, ::frac) {
            working.tooth2Length = it; refresh()
        }
        configureRow(R.id.rowT1TipY, "Top angle", 0.02f, 0.48f, working.tooth1TipY, ::frac) {
            working.tooth1TipY = it; refresh()
        }
        configureRow(R.id.rowT2TipY, "Bot angle", 0.52f, 0.98f, working.tooth2TipY, ::frac) {
            working.tooth2TipY = it; refresh()
        }
        configureRow(R.id.rowGums, "Gums", 0.0f, 0.60f, working.gumsDepth, ::frac) {
            working.gumsDepth = it; refresh()
        }
        configureRow(R.id.rowGap, "Gap", 0.05f, 0.90f, working.gap, ::frac) {
            working.gap = it; refresh()
        }

        rgColorMode.check(
            if (working.colorMode == SliverConfig.ColorMode.CUSTOM) R.id.rbColorCustom else R.id.rbColorDefault
        )
        etWidth.setText(fmtDp(working.widthDp))
        etHeight.setText(fmtDp(working.heightDp))

        updateColorSection()
        binding = false
        refresh()
    }

    private fun configureRow(
        id: Int, label: String, min: Float, max: Float, value: Float,
        formatter: (Float) -> String, onChange: (Float) -> Unit
    ) {
        view.findViewById<LabeledSeekBar>(id).configure(label, min, max, value, formatter, onChange)
    }

    private fun updateColorSection() {
        hueContainer.visibility =
            if (working.colorMode == SliverConfig.ColorMode.CUSTOM) View.VISIBLE else View.GONE
        updateSwatch()
    }

    private fun updateSwatch() {
        colorSwatch.setBackgroundColor(working.baseColor())
    }

    private fun refresh() {
        if (binding) return
        preview.setConfig(working)
    }

    /** Paints the hue SeekBar's track as a rainbow spectrum. */
    private fun paintHueTrack() {
        val stops = IntArray(13) { i -> Color.HSVToColor(floatArrayOf(i * 30f, 1f, 1f)) }
        val g = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, stops)
        g.cornerRadius = 8f * context.resources.displayMetrics.density
        rowHue.seek().apply {
            progressDrawable = ColorDrawable(Color.TRANSPARENT)
            background = g
            minimumHeight = (14 * context.resources.displayMetrics.density).toInt()
        }
    }

    private fun sizeWatcher(isWidth: Boolean) = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            if (binding) return
            val v = s?.toString()?.toFloatOrNull() ?: return
            if (isWidth) working.widthDp = v.coerceIn(8f, 160f)
            else working.heightDp = v.coerceIn(12f, 240f)
            refresh()
        }

        override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
    }

    private fun parseDp(et: EditText, fallback: Float, min: Float, max: Float): Float {
        val v = et.text?.toString()?.toFloatOrNull() ?: return fallback
        return v.coerceIn(min, max)
    }

    private fun frac(v: Float): String = String.format("%.2f", v)
    private fun fmtDp(v: Float): String =
        if (v == v.toInt().toFloat()) v.toInt().toString() else String.format("%.1f", v)

    private fun copyInto(dst: SliverConfig, src: SliverConfig) {
        dst.opacity = src.opacity
        dst.colorMode = src.colorMode
        dst.customHue = src.customHue
        dst.tooth1Thickness = src.tooth1Thickness
        dst.tooth2Thickness = src.tooth2Thickness
        dst.tooth1Length = src.tooth1Length
        dst.tooth2Length = src.tooth2Length
        dst.tooth1TipY = src.tooth1TipY
        dst.tooth2TipY = src.tooth2TipY
        dst.gumsDepth = src.gumsDepth
        dst.gap = src.gap
        dst.widthDp = src.widthDp
        dst.heightDp = src.heightDp
    }

    companion object {
        /** Build and show the dialog. [onApplied] receives the saved config when the user taps Apply. */
        fun show(context: Context, initial: SliverConfig, onApplied: (SliverConfig) -> Unit) {
            SliverCustomizeDialog(context, initial, onApplied).build()
        }
    }
}
