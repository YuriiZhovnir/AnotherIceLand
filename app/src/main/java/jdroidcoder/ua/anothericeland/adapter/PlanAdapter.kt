package jdroidcoder.ua.anothericeland.adapter

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import jdroidcoder.ua.anothericeland.R
import kotlinx.android.synthetic.main.plain_item_style.view.*

class PlanAdapter(var items: ArrayList<Object> = ArrayList()) : RecyclerView.Adapter<PlanAdapter.ViewHolder>() {
    private var context: Context? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent?.context
        return ViewHolder(LayoutInflater.from(parent?.context).inflate(R.layout.plain_item_style, parent, false))
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.title.text = "Item $position"
        if (position < items.count()-1) {
            holder.bottomLine?.visibility = View.VISIBLE
        } else {
            holder.bottomLine?.visibility = View.INVISIBLE
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var title = itemView.title
        var bottomLine = itemView.bottomLine
    }
}