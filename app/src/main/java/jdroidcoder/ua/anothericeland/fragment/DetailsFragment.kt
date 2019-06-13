package jdroidcoder.ua.anothericeland.fragment

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import butterknife.ButterKnife
import butterknife.OnClick
import butterknife.Unbinder
import jdroidcoder.ua.anothericeland.R
import kotlinx.android.synthetic.main.app_bar.*

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
        closeButton?.visibility = View.VISIBLE
        shadow?.visibility = View.GONE
    }

    @OnClick(R.id.closeButton)
    fun closeButton() {
        activity?.supportFragmentManager?.popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        unbinder.unbind()
    }
}
