package jdroidcoder.ua.anothericeland.fragment

import android.os.Build
import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import butterknife.ButterKnife
import butterknife.OnClick
import butterknife.Unbinder
import com.squareup.picasso.Picasso
import jdroidcoder.ua.anothericeland.R
import jdroidcoder.ua.anothericeland.activity.MapActivity
import jdroidcoder.ua.anothericeland.helper.GlobalData
import kotlinx.android.synthetic.main.app_bar.*
import kotlinx.android.synthetic.main.fragment_location_details.*
import java.io.File

class DetailsFragment : Fragment() {
    companion object {
        const val TAG = "DetailsFragment"

        fun newInstance() = DetailsFragment()
    }

    private var unbinder: Unbinder = Unbinder.EMPTY

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater?.inflate(R.layout.fragment_location_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        unbinder = ButterKnife.bind(this, view)
        val pointDetails = MapActivity.markers?.get(GlobalData?.selectedMarker)
        GlobalData?.trip?.days?.let {
            for (d in it) {
                for (p in d.points) {
                    if (p == pointDetails) {
                        title?.text = d.name
                    }
                }
            }
        }
        try {
            Picasso.get().load(File(pointDetails?.image)).into(locationImage)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        locationName?.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(pointDetails?.name, Html.FROM_HTML_MODE_COMPACT)
        } else {
            Html.fromHtml(pointDetails?.name)
        }
        pointDetails?.description?.let{
        description?.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Html.fromHtml(it, Html.FROM_HTML_MODE_COMPACT)
            } else {
                Html.fromHtml(it)
            }
        }
        description.movementMethod = LinkMovementMethod.getInstance()
        locationName.movementMethod = LinkMovementMethod.getInstance()
//        locationName?.text = pointDetails?.name
//        description?.text = pointDetails?.description
        if (pointDetails?.isHotel == true) {
            phonesContainer?.visibility = View.VISIBLE
            phones?.text = pointDetails?.phone
        } else {
            phonesContainer?.visibility = View.GONE
        }
        closeButton?.visibility = View.VISIBLE
        shadow?.visibility = View.GONE
    }

    @OnClick(R.id.closeButton)
    fun closeButton() {
//        GlobalData?.selectedMarker = null
        activity?.supportFragmentManager?.popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        unbinder.unbind()
    }
}
