package org.wordpress.android.ui.sitecreation.verticals

import android.support.annotation.LayoutRes
import android.support.v4.content.ContextCompat
import android.support.v7.content.res.AppCompatResources
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.R.color
import org.wordpress.android.R.drawable
import org.wordpress.android.ui.sitecreation.verticals.NewSiteCreationVerticalsViewModel.VerticalsListItemUiState
import org.wordpress.android.ui.sitecreation.verticals.NewSiteCreationVerticalsViewModel.VerticalsListItemUiState.VerticalsFetchSuggestionsErrorUiState
import org.wordpress.android.ui.sitecreation.verticals.NewSiteCreationVerticalsViewModel.VerticalsListItemUiState.VerticalsHeaderUiState
import org.wordpress.android.ui.sitecreation.verticals.NewSiteCreationVerticalsViewModel.VerticalsListItemUiState.VerticalsModelUiState
import org.wordpress.android.ui.sitecreation.verticals.NewSiteCreationVerticalsViewModel.VerticalsListItemUiState.VerticalsSearchInputUiState

sealed class NewSiteCreationVerticalsViewHolder(internal val parent: ViewGroup, @LayoutRes layout: Int) :
        RecyclerView.ViewHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false)) {
    abstract fun onBind(uiState: VerticalsListItemUiState)

    class VerticalsHeaderViewHolder(
        parentView: ViewGroup
    ) : NewSiteCreationVerticalsViewHolder(parentView, R.layout.new_site_creation_verticals_header_item) {

        private val title = itemView.findViewById<TextView>(R.id.title)
        private val subtitle = itemView.findViewById<TextView>(R.id.subtitle)

        override fun onBind(uiState: VerticalsListItemUiState) {
            uiState as VerticalsHeaderUiState
            title.text = uiState.title
            subtitle.text = uiState.subtitle
        }
    }

    class VerticalsSearchInputViewHolder(
        parentView: ViewGroup
    ) : NewSiteCreationVerticalsViewHolder(parentView, R.layout.new_site_creation_verticals_search_input_item) {
        private val input = itemView.findViewById<EditText>(R.id.input)
        private val progressBar = itemView.findViewById<ViewGroup>(R.id.progress_bar)

        override fun onBind(uiState: VerticalsListItemUiState) {
            uiState as VerticalsSearchInputUiState
            input.setText(uiState.query)
            input.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    AppCompatResources.getDrawable(
                            itemView.context, R.drawable.ic_search_grey_24dp
                    ), null, null, null
            )

            input.hint = uiState.hint
            input.addTextChangedListener(object: TextWatcher{
                override fun afterTextChanged(s: Editable?) {}
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    uiState.onTextChanged.invoke(s?.toString() ?: "")
                }

            })
            progressBar.visibility = if (uiState.showProgress) View.VISIBLE else View.GONE
            // TODO add clear all buttons - custom view? if (uiState.showClearButton) View.VISIBLE else View.GONE
        }
    }

    class VerticalsSuggestionItemViewHolder(
        parentView: ViewGroup
    ) : NewSiteCreationVerticalsViewHolder(parentView, R.layout.new_site_creation_verticals_suggestion_item) {
        private val suggestion = itemView.findViewById<TextView>(R.id.suggestion)
        private val divider = itemView.findViewById<View>(R.id.divider)

        override fun onBind(uiState: VerticalsListItemUiState) {
            uiState as VerticalsModelUiState
            suggestion.text = uiState.title
            divider.visibility = if (uiState.showDivider) View.VISIBLE else View.GONE
            // TODO add onClick listener
        }
    }

    class VerticalsErrorViewHolder(
        parentView: ViewGroup
    ) : NewSiteCreationVerticalsViewHolder(parentView, R.layout.new_site_creation_verticals_error_item) {
        private val text = itemView.findViewById<TextView>(R.id.error_text)
        private val retry = itemView.findViewById<TextView>(R.id.retry)

        init {
            addRetryCompoundDrawable()
        }

        private fun addRetryCompoundDrawable() {
            val drawable = itemView.context.getDrawable(drawable.retry_icon)
            drawable.setTint(ContextCompat.getColor(itemView.context, color.wp_blue))
            retry.setCompoundDrawables(drawable, null, null, null)
        }

        override fun onBind(uiState: VerticalsListItemUiState) {
            uiState as VerticalsFetchSuggestionsErrorUiState
            text.text = itemView.context.getText(uiState.messageResId)
            retry.text = itemView.context.getText(uiState.retryButonResId)
        }
    }
}
