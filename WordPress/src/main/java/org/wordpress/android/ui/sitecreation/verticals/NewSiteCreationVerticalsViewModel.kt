package org.wordpress.android.ui.sitecreation.verticals

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.support.annotation.StringRes
import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import org.apache.commons.lang3.StringUtils
import org.wordpress.android.R
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.vertical.VerticalModel
import org.wordpress.android.fluxc.store.VerticalStore.OnVerticalsFetched
import org.wordpress.android.models.networkresource.ListState
import org.wordpress.android.models.networkresource.ListState.Error
import org.wordpress.android.models.networkresource.ListState.Loading
import org.wordpress.android.modules.IO_DISPATCHER
import org.wordpress.android.modules.MAIN_DISPATCHER
import org.wordpress.android.ui.sitecreation.usecases.DummyOnVerticalsHeaderInfoFetched
import org.wordpress.android.ui.sitecreation.usecases.DummyVerticalsHeaderInfoModel
import org.wordpress.android.ui.sitecreation.usecases.FetchVerticalsHeaderInfoUseCase
import org.wordpress.android.ui.sitecreation.usecases.FetchVerticalsUseCase
import org.wordpress.android.ui.sitecreation.verticals.NewSiteCreationVerticalsViewModel.VerticalsListItemUiState.VerticalsFetchSuggestionsErrorUiState
import org.wordpress.android.ui.sitecreation.verticals.NewSiteCreationVerticalsViewModel.VerticalsListItemUiState.VerticalsHeaderUiState
import org.wordpress.android.ui.sitecreation.verticals.NewSiteCreationVerticalsViewModel.VerticalsListItemUiState.VerticalsModelUiState
import org.wordpress.android.ui.sitecreation.verticals.NewSiteCreationVerticalsViewModel.VerticalsListItemUiState.VerticalsSearchInputUiState
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.experimental.CoroutineContext

private const val throttleDelay: Int = 500

class NewSiteCreationVerticalsViewModel @Inject constructor(
    private val dispatcher: Dispatcher,
    private val fetchVerticalsHeaderInfoUseCase: FetchVerticalsHeaderInfoUseCase,
    private val fetchVerticalsUseCase: FetchVerticalsUseCase,
    @Named(IO_DISPATCHER) private val IO: CoroutineContext,
    @Named(MAIN_DISPATCHER) private val MAIN: CoroutineContext
) : ViewModel(),
        CoroutineScope {
    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = IO + job
    private var isStarted = false

    private val _uiState: MutableLiveData<VerticalsUiState> = MutableLiveData()
    val uiState: LiveData<VerticalsUiState> = _uiState

    private var listState: ListState<VerticalModel> = ListState.Init()
    private lateinit var headerInfo: DummyVerticalsHeaderInfoModel

    init {
        dispatcher.register(fetchVerticalsUseCase)
    }

    override fun onCleared() {
        super.onCleared()
        dispatcher.unregister(fetchVerticalsUseCase)
    }

    fun start() {
        if (isStarted) {
            return
        }
        isStarted = true
        fetchHeaderInfo()
    }

    private fun fetchHeaderInfo() {
        launch {
            withContext(MAIN) {
                updateUiStateToFullScreenProgress()

            }
            val headerInfoEvent = fetchVerticalsHeaderInfoUseCase.fetchVerticalHeaderInfo()
            withContext(MAIN) {
                onHeaderInfoFetched(headerInfoEvent)
            }
        }
    }

    private fun onHeaderInfoFetched(event: DummyOnVerticalsHeaderInfoFetched) {
        if (event.isError) {
            updateUiStateToFullScreenError()
        } else {
            headerInfo = event.headerInfo!!
            updateUiStateToContent("", ListState.Ready(emptyList()))
        }
    }

    fun onFetchHeaderInfoRetry() {
        fetchHeaderInfo()
    }

    fun updateQuery(query: String, delay: Int = throttleDelay) {
        job.cancel() // cancel any previous requests
        updateUiStateToContent(query, ListState.Ready(emptyList()))
        fetchVerticals(query, delay)
    }

    private fun fetchVerticals(query: String, throttleDelay: Int) {
        launch {
            withContext(MAIN) {
                updateUiStateToContent(query, ListState.Loading(listState, false))
            }
            delay(throttleDelay)
            val fetchedVerticals = fetchVerticalsUseCase.fetchVerticals(query)
            withContext(MAIN) {
                onVerticalsFetched(query, fetchedVerticals)
            }
        }
    }

    private fun onVerticalsFetched(query: String, event: OnVerticalsFetched) {
        if (event.isError) {
            updateUiStateToContent(query, ListState.Error(listState, event.error.message))
        } else {
            updateUiStateToContent(query, ListState.Success(event.verticalList))
        }
    }

    private fun updateUiStateToFullScreenProgress() {
        _uiState.value = VerticalsUiState(
                showFullscreenError = false,
                showFullscreenProgress = true,
                showContent = false,
                showSkipButton = false,
                items = emptyList()
        )
    }

    private fun updateUiStateToFullScreenError() {
        _uiState.value = VerticalsUiState(
                showFullscreenError = true,
                showFullscreenProgress = false,
                showContent = false,
                showSkipButton = false,
                items = emptyList()
        )
    }

    private fun updateUiStateToContent(query: String, state: ListState<VerticalModel>) {
        listState = state
        _uiState.value = VerticalsUiState(
                showFullscreenError = false,
                showFullscreenProgress = false,
                showContent = true,
                showSkipButton = StringUtils.isEmpty(query),
                items = createContentUiStates(
                        query,
                        showProgress = state is Loading,
                        data = state.data,
                        errorFetchingSuggestions = state is Error
                )
        )
    }

    private fun createContentUiStates(
        query: String,
        showProgress: Boolean,
        data: List<VerticalModel>,
        errorFetchingSuggestions: Boolean
    ): List<VerticalsListItemUiState> {
        val items: ArrayList<VerticalsListItemUiState> = ArrayList()
        if (shouldShowHeader(query)) {
            addHeaderUiState(items)
        }
        addSearchInputUiState(query, showProgress, items)
        addSuggestionsUiState(errorFetchingSuggestions, data, items)
        return items
    }

    private fun shouldShowHeader(query: String): Boolean {
        return StringUtils.isEmpty(query)
    }

    private fun addHeaderUiState(items: ArrayList<VerticalsListItemUiState>) {
        items.add(VerticalsHeaderUiState(headerInfo.title, headerInfo.subtitle))
    }

    private fun addSearchInputUiState(
        query: String,
        showProgress: Boolean,
        items: ArrayList<VerticalsListItemUiState>
    ) {
        val inputState = VerticalsSearchInputUiState(
                query,
                headerInfo.inputHint,
                showProgress,
                showClearButton = !StringUtils.isEmpty(query)
        )
        inputState.onTextChanged = { query -> updateQuery(query) }
        items.add(inputState)
    }

    private fun addSuggestionsUiState(
        showErrorItem: Boolean,
        data: List<VerticalModel>,
        items: ArrayList<VerticalsListItemUiState>
    ) {
        if (showErrorItem) {
            items.add(
                    VerticalsFetchSuggestionsErrorUiState(
                            R.string.site_creation_fetch_suggestions_failed,
                            R.string.button_retry
                    )
            )
        } else {
            val lastItemIndex = data.size - 1
            data.forEachIndexed { index, model ->
                items.add(VerticalsModelUiState(model.verticalId, model.name, showDivider = index != lastItemIndex))
            }
        }
    }

    data class VerticalsUiState(
        val showFullscreenError: Boolean,
        val showFullscreenProgress: Boolean,
        val showContent: Boolean,
        val showSkipButton: Boolean,
        val items: List<VerticalsListItemUiState>
    )

    sealed class VerticalsListItemUiState {
        data class VerticalsHeaderUiState(val title: String, val subtitle: String) : VerticalsListItemUiState()
        data class VerticalsSearchInputUiState(
            val query: String,
            val hint: String,
            val showProgress: Boolean,
            val showClearButton: Boolean
        ) : VerticalsListItemUiState() {
            lateinit var onTextChanged: (String) -> Unit
        }

        data class VerticalsModelUiState(val id: String, val title: String, val showDivider: Boolean) :
                VerticalsListItemUiState()

        data class VerticalsFetchSuggestionsErrorUiState(
            @StringRes val messageResId: Int,
            @StringRes val retryButonResId: Int
        ) : VerticalsListItemUiState()
    }
}
