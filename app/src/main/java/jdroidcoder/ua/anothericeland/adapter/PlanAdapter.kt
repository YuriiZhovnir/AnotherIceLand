package jdroidcoder.ua.anothericeland.adapter

import android.content.Context
import android.graphics.Color
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.widget.RecyclerView
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import jdroidcoder.ua.anothericeland.R
import jdroidcoder.ua.anothericeland.network.response.Point
import kotlinx.android.synthetic.main.plain_item_style.view.*

class PlanAdapter(
        var items: ArrayList<Point> = ArrayList(), var listener: SheetListener? = null,
        var changePointListener: ChangePointListener? = null
) : RecyclerView.Adapter<PlanAdapter.ViewHolder>() {
    private var context: Context? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent?.context
        return ViewHolder(LayoutInflater.from(parent?.context).inflate(R.layout.plain_item_style, parent, false))
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items?.get(position)
        holder.title.text = item.name
        val typeface = context?.let { ResourcesCompat.getFont(it, R.font.montserrat_bold) }
        holder.title.typeface = typeface
        holder.title.setTextColor(Color.parseColor("#0097DA"))
        holder.title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        holder.title.setPadding(0, 0, 0, 0)

        holder.item.title?.setOnClickListener {
            listener?.dayChosen(item)
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var item = itemView
        var title = itemView.title
    }
}

interface SheetListener {
    fun dayChosen(point: Point)
}

interface ChangePointListener {
    fun changePoint(point: Point, isShow: Boolean)
}