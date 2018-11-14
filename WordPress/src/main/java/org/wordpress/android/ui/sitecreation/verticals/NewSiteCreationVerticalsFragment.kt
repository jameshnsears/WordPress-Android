package org.wordpress.android.ui.sitecreation.verticals

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.os.Parcelable
import android.support.annotation.LayoutRes
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.sitecreation.NewSiteCreationBaseFormFragment
import org.wordpress.android.ui.sitecreation.NewSiteCreationListener
import org.wordpress.android.ui.sitecreation.verticals.NewSiteCreationVerticalsViewModel.VerticalsListItemUiState
import javax.inject.Inject

private const val keyListState = "list_state"

class NewSiteCreationVerticalsFragment : NewSiteCreationBaseFormFragment<NewSiteCreationListener>() {
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewModel: NewSiteCreationVerticalsViewModel

    private lateinit var fullscreenErrorLayout: ViewGroup
    private lateinit var fullscreenProgressLayout: ViewGroup
    private lateinit var contentLayout: ViewGroup
    private lateinit var skipButton: Button

    @Inject protected lateinit var viewModelFactory: ViewModelProvider.Factory

    @LayoutRes
    override fun getContentLayout(): Int {
        return R.layout.new_site_creation_verticals_screen
    }

    override fun setupContent(rootView: ViewGroup) {
        // TODO receive title from the MainVM
        // important for accessibility - talkback
        activity!!.setTitle(R.string.new_site_creation_verticals_title)
        fullscreenErrorLayout = rootView.findViewById(R.id.error_layout)
        fullscreenProgressLayout = rootView.findViewById(R.id.progress_layout)
        contentLayout = rootView.findViewById(R.id.content_layout)
        initRecyclerView(rootView)
        initViewModel()
        initRetryButton(rootView)
        initSkipButton(rootView)
    }

    private fun initRecyclerView(rootView: ViewGroup) {
        recyclerView = rootView.findViewById(R.id.recycler_view)
        val layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        linearLayoutManager = layoutManager
        recyclerView.layoutManager = linearLayoutManager
        initAdapter()
    }

    private fun initAdapter() {
        val adapter = NewSiteCreationVerticalsAdapter()
        recyclerView.adapter = adapter
    }

    private fun initViewModel() {
        viewModel = ViewModelProviders.of(activity!!, viewModelFactory)
                .get(NewSiteCreationVerticalsViewModel::class.java)

        viewModel.uiState.observe(this, Observer { state ->
            state?.let {
                contentLayout.visibility = if (state.showContent) View.VISIBLE else View.GONE
                fullscreenErrorLayout.visibility = if (state.showFullscreenError) View.VISIBLE else View.GONE
                fullscreenProgressLayout.visibility = if (state.showFullscreenProgress) View.VISIBLE else View.GONE
                skipButton.visibility = if (state.showSkipButton) View.VISIBLE else View.GONE
                updateSuggestions(state.items)
            }
        })

        viewModel.start()
    }

    private fun initRetryButton(rootView: ViewGroup) {
        val retryBtn = rootView.findViewById<Button>(R.id.error_retry)
        retryBtn.setOnClickListener { _ -> viewModel.onFetchHeaderInfoRetry() }
    }

    private fun initSkipButton(rootView: ViewGroup) {
        skipButton = rootView.findViewById<Button>(R.id.btn_skip)
        skipButton.setOnClickListener { _ ->
            //TODO add skip action
        }
    }

    override fun onHelp() {
        if (mSiteCreationListener != null) {
            mSiteCreationListener.helpCategoryScreen()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity!!.application as WordPress).component().inject(this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(keyListState, linearLayoutManager.onSaveInstanceState())
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        savedInstanceState?.getParcelable<Parcelable>(keyListState)?.let {
            linearLayoutManager.onRestoreInstanceState(it)
        }
    }

    private fun updateSuggestions(suggestions: List<VerticalsListItemUiState>) {
        (recyclerView.adapter as NewSiteCreationVerticalsAdapter).update(suggestions)
    }

    companion object {
        val TAG = "site_creation_verticals_fragment_tag"
    }
}
