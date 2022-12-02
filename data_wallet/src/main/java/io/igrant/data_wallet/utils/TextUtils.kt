package io.igrant.data_wallet.utils

import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.MaskFilter
import android.graphics.Paint
import android.graphics.Rect
import android.text.Html
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.MaskFilterSpan
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.TextView
import io.igrant.data_wallet.R
import java.util.*


object TextUtils {

//    fun makeTextViewResizable(
//        tv: TextView,
//        maxLine: Int,
//        expandText: String,
//        viewMore: Boolean
//    ) {
//        if (tv.tag == null) {
//            tv.tag = tv.text
//        }
//        val vto = tv.viewTreeObserver
//        vto.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
//            override fun onGlobalLayout() {
//                val obs = tv.viewTreeObserver
//                obs.removeGlobalOnLayoutListener(this)
//                if (maxLine == 0) {
//                    val lineEndIndex = tv.layout.getLineEnd(0)
//                    val text =
//                        tv.text.subSequence(0, lineEndIndex - expandText.length + 1)
//                            .toString() + " " + expandText
//                    tv.text = text
//                    tv.movementMethod = LinkMovementMethod.getInstance()
//                    tv.setText(
//                        addClickablePartTextViewResizable(
//                            Html.fromHtml(tv.text.toString()), tv, maxLine, expandText,
//                            viewMore
//                        ), TextView.BufferType.SPANNABLE
//                    )
//                } else if (maxLine > 0 && tv.lineCount >= maxLine) {
//                    val lineEndIndex = tv.layout.getLineEnd(maxLine - 1)
//                    val text =
//                        tv.text.subSequence(0, lineEndIndex - expandText.length + 1)
//                            .toString() + " " + expandText
//                    tv.text = text
//                    tv.movementMethod = LinkMovementMethod.getInstance()
//                    tv.setText(
//                        addClickablePartTextViewResizable(
//                            Html.fromHtml(tv.text.toString()), tv, maxLine, expandText,
//                            viewMore
//                        ), TextView.BufferType.SPANNABLE
//                    )
//                } else {
//                    val lineEndIndex =
//                        tv.layout.getLineEnd(tv.layout.lineCount - 1)
//                    val text =
//                        tv.text.subSequence(0, lineEndIndex).toString() + " " + expandText
//                    tv.text = text
//                    tv.movementMethod = LinkMovementMethod.getInstance()
//                    tv.setText(
//                        addClickablePartTextViewResizable(
//                            Html.fromHtml(tv.text.toString()), tv, lineEndIndex, expandText,
//                            viewMore
//                        ), TextView.BufferType.SPANNABLE
//                    )
//                }
//            }
//        })
//    }

//    private fun addClickablePartTextViewResizable(
//        strSpanned: Spanned, tv: TextView,
//        maxLine: Int, spanableText: String, viewMore: Boolean
//    ): SpannableStringBuilder? {
//        val str = strSpanned.toString()
//        val ssb = SpannableStringBuilder(strSpanned)
//        if (str.contains(spanableText)) {
//            ssb.setSpan(object : ReadMoreSpannable(false) {
//                override fun onClick(p0: View) {
//                    if (viewMore) {
//                        tv.layoutParams = tv.layoutParams
//                        tv.setText(tv.tag.toString(), TextView.BufferType.SPANNABLE)
//                        tv.invalidate()
//                        makeTextViewResizable(
//                            tv,
//                            -1,
//                            tv.context.resources.getString(R.string.txt_read_less),
//                            false
//                        )
//                    } else {
//                        tv.layoutParams = tv.layoutParams
//                        tv.setText(tv.tag.toString(), TextView.BufferType.SPANNABLE)
//                        tv.invalidate()
//                        makeTextViewResizable(
//                            tv,
//                            3,
//                            tv.context.resources.getString(R.string.txt_read_more),
//                            true
//                        )
//                    }
//                }
//            }, str.indexOf(spanableText), str.indexOf(spanableText) + spanableText.length, 0)
//        }
//        return ssb
//    }

    fun dpToPxFun(dp: Float, context: Context): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            context.resources.displayMetrics
        )
            .toInt()
    }

    fun getCapsSentences(tagName: String): String {
        if (tagName != "") {
            val splits = tagName.split(" ").toTypedArray()
            val sb = StringBuilder()
            for (i in splits.indices) {
                val eachWord = splits[i]
                if (i > 0 && eachWord.isNotEmpty()) {
                    sb.append(" ")
                }
                if (eachWord.isNotEmpty()) {
                    val cap = (eachWord.substring(0, 1).toUpperCase(Locale.getDefault())
                            + eachWord.substring(1))
                    sb.append(cap)
                }
            }
            return sb.toString()
        } else {
            return tagName
        }
    }

    fun setBlurryText(text: String, textView: TextView, isBlur: Boolean) {
        val string = SpannableString(text)
        if (isBlur) {
            val blurMask: MaskFilter = BlurMaskFilter(14f, BlurMaskFilter.Blur.NORMAL)
            string.setSpan(
                MaskFilterSpan(blurMask),
                0,
                text.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        textView.text = string
        textView.setTextIsSelectable(!isBlur)
    }


    fun findTextWidth(
        topTextView: TextView,
        bottomTextView: TextView,
        string: String,
        isBlur: Boolean,
        width: Int
    ) {
        val paint = Paint()
        paint.textSize = topTextView.textSize
        val result = Rect()
        paint.getTextBounds(string, 0, string.length, result)
        if (result.width() + 20 > width) {
            topTextView.visibility = View.GONE
            bottomTextView.visibility = View.VISIBLE
            setBlurryText(string, bottomTextView, isBlur)
        } else {
            topTextView.visibility = View.VISIBLE
            bottomTextView.visibility = View.GONE
            setBlurryText(string, topTextView, isBlur)
        }
    }

    const val MINIMUM_BALANCE_WIDTH = 40
    fun findTextWidth(
        topTextView: TextView,
        bottomTextView: TextView,
        titleTextView: TextView,
        titleString: String,
        valueString: String,
        isBlur: Boolean,
        width: Int
    ) {
        val paint = Paint()
        paint.textSize = topTextView.textSize
        val result = Rect()

        //calculating the title sting width
        paint.getTextBounds(titleString, 0, titleString.length, result)
        val titleWidth = result.width()

        //setting title to title text view
        titleTextView.text = getCapsSentences(titleString)

        //calculating the width of value
        paint.getTextBounds(valueString, 0, valueString.length, result)
        val valueWidth = result.width()

        if ((titleWidth + valueWidth + 20) <= width) {
            topTextView.visibility = View.VISIBLE
            bottomTextView.visibility = View.GONE
            setBlurryText(valueString, topTextView, isBlur)
        } else {
            topTextView.visibility = View.GONE
            bottomTextView.visibility = View.VISIBLE
            setBlurryText(valueString, bottomTextView, isBlur)
        }
    }

    /**
     * This method converts dp unit to equivalent pixels, depending on device density.
     *
     * @param dp A value in dp (density independent pixels) unit. Which we need to convert into pixels
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent px equivalent to dp depending on device density
     */
    fun convertDpToPixel(dp: Float, context: Context): Float {
        return dp * (context.resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
    }
}