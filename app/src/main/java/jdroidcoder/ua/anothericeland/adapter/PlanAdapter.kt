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
import jdroidcoder.ua.anothericeland.helper.GlobalData
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
        if (item?.typeId == 3) {
            holder?.dayIsDone?.visibility = View.GONE
            val typeface = context?.let { ResourcesCompat.getFont(it, R.font.montserrat_bold) }
            holder.title.typeface = typeface
            holder.title.setTextColor(Color.parseColor("#0097DA"))
            holder.title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            holder.title.setPadding(0, 0, 0, 0)
            if (!item.isDone) {
                holder.indicator?.setImageResource(R.drawable.ic_circle_day_empty)
            } else {
                holder.indicator?.setImageResource(R.drawable.ic_circle_day_full)
            }
        } else {
            val typeface = context?.let { ResourcesCompat.getFont(it, R.font.montserrat_medium) }
            holder.title.typeface = typeface
            holder?.dayIsDone?.visibility = View.VISIBLE
            holder?.dayIsDone?.setOnCheckedChangeListener(null)
            holder?.dayIsDone?.isChecked = item.isDone == false
            holder?.dayIsDone?.setOnCheckedChangeListener { buttonView, isChecked ->
                GlobalData?.trip?.days?.let {
                    for (day in it) {
                        for (point in day.points) {
                            if (point == item) {
                                point.isDone = isChecked == false
                            }
                        }
                        val temp = day.points?.firstOrNull { p -> !p.isDone }
                        day.isDone = temp == null
                    }
                }
                if (isChecked) {
                    holder.title.setTextColor(Color.parseColor("#8D8D8D"))
                    holder.indicator?.setImageResource(R.drawable.ic_circle_point_empty)
                } else {
                    holder.title.setTextColor(Color.parseColor("#333333"))
                    holder.indicator?.setImageResource(R.drawable.ic_circle_point_full)
                }
                items?.get(position).isDone = isChecked == false
                changePointListener?.changePoint(item, isChecked)
            }
            context?.resources?.getDimension(R.dimen.padding_2)?.toInt()?.let { holder.title.setPadding(it, 0, 0, 0) }
            holder.title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            if (!item.isDone) {
                holder.title.setTextColor(Color.parseColor("#8D8D8D"))
                holder.indicator?.setImageResource(R.drawable.ic_circle_point_empty)
            } else {
                holder.title.setTextColor(Color.parseColor("#333333"))
                holder.indicator?.setImageResource(R.drawable.ic_circle_point_full)
            }
        }
        if (position < items.count() - 1) {
            holder.bottomLine?.visibility = View.VISIBLE
        } else {
            holder.bottomLine?.visibility = View.INVISIBLE
        }
        holder.item?.setOnClickListener {
            if (item.name?.contains("Day") == true) {
                listener?.dayChosen(items?.get(position + 1))
            } else {
                listener?.dayChosen(item)
            }
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var item = itemView
        var title = itemView.title
        var bottomLine = itemView.bottomLine
        var dayIsDone = itemView.dayIsDone
        var indicator = itemView.indicator
    }
}

interface SheetListener {
    fun dayChosen(point: Point)
}

interface ChangePointListener {
    fun changePoint(point: Point, isShow: Boolean)
}