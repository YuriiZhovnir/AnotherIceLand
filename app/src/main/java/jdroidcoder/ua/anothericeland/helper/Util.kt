package jdroidcoder.ua.anothericeland.helper

import android.content.Context
import android.graphics.*
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import jdroidcoder.ua.anothericeland.R

object Util {
    fun buildIcon(context: Context, bitmap: Bitmap, idRes: Int): Bitmap {
        val customMarkerView = LayoutInflater.from(context).inflate(R.layout.custom_marker_layout, null)
        val markerImageView = customMarkerView.findViewById<ImageView>(R.id.logo)
        val container = customMarkerView.findViewById<ImageView>(R.id.container)
        container.setImageResource(idRes)
        markerImageView.setImageBitmap(context?.resources?.getDimension(R.dimen.round_corner_5)?.toInt()?.let { getRoundedCornerBitmap(bitmap, it) })
        customMarkerView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        customMarkerView.layout(0, 0, customMarkerView.measuredWidth, customMarkerView.measuredHeight)
        customMarkerView.buildDrawingCache()
        val returnedBitmap = Bitmap.createBitmap(customMarkerView.measuredWidth, customMarkerView.measuredHeight,
                Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        canvas.drawColor(Color.WHITE, PorterDuff.Mode.SRC_IN)
        val drawable = customMarkerView.background
        drawable?.draw(canvas)
        customMarkerView.draw(canvas)
        return returnedBitmap
    }

    private fun getRoundedCornerBitmap(bitmap: Bitmap, roundPixelSize: Int): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint()
        val rect = Rect(0, 0, bitmap.width, bitmap.height)
        val rectF = RectF(rect)
        val roundPx = roundPixelSize.toFloat()
        paint.isAntiAlias = true
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)
        return output
    }
}