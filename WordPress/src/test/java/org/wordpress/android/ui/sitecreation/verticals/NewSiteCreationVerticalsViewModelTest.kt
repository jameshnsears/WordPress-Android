package org.wordpress.android.ui.sitecreation.verticals

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.arch.lifecycle.Observer
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.inOrder
import com.nhaarman.mockito_kotlin.whenever
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import kotlinx.coroutines.experimental.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.R
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.vertical.VerticalModel
import org.wordpress.android.fluxc.store.VerticalStore.FetchVerticalsError
import org.wordpress.android.fluxc.store.VerticalStore.OnVerticalsFetched
import org.wordpress.android.fluxc.store.VerticalStore.VerticalErrorType
import org.wordpress.android.fluxc.store.VerticalStore.VerticalErrorType.GENERIC_ERROR
import org.wordpress.android.test
import org.wordpress.android.ui.sitecreation.usecases.DummyFetchVerticalHeaderInfoError
import org.wordpress.android.ui.sitecreation.usecases.DummyOnVerticalsHeaderInfoFetched
import org.wordpress.android.ui.sitecreation.usecases.DummyVerticalsHeaderInfoModel
import org.wordpress.android.ui.sitecreation.usecases.FetchVerticalsHeaderInfoUseCase
import org.wordpress.android.ui.sitecreation.usecases.FetchVerticalsUseCase
import org.wordpress.android.ui.sitecreation.verticals.NewSiteCreationVerticalsViewModel.VerticalsListItemUiState.VerticalsFetchSuggestionsErrorUiState
import org.wordpress.android.ui.sitecreation.verticals.NewSiteCreationVerticalsViewModel.VerticalsListItemUiState.VerticalsHeaderUiState
import org.wordpress.android.ui.sitecreation.verticals.NewSiteCreationVerticalsViewModel.VerticalsListItemUiState.VerticalsModelUiState
import org.wordpress.android.ui.sitecreation.verticals.NewSiteCreationVerticalsViewModel.VerticalsListItemUiState.VerticalsSearchInputUiState
import org.wordpress.android.ui.sitecreation.verticals.NewSiteCreationVerticalsViewModel.VerticalsUiState

@RunWith(MockitoJUnitRunner::class)
class NewSiteCreationVerticalsViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock lateinit var dispatcher: Dispatcher
    @Mock lateinit var fetchVerticalsUseCase: FetchVerticalsUseCase
    @Mock lateinit var fetchVerticalsHeaderInfoUseCase: FetchVerticalsHeaderInfoUseCase

    private lateinit var viewModel: NewSiteCreationVerticalsViewModel

    @Mock private lateinit var uiStateObserver: Observer<VerticalsUiState>

    private val dummySearchInputHint = "dummyHint"
    private val dummySearchInputTitle = "dummyTitle"
    private val dummySearchInputSubtitle = "dummySubtitle"
    private val successHeaderInfoEvent = DummyOnVerticalsHeaderInfoFetched(
            DummyVerticalsHeaderInfoModel(
                    dummySearchInputTitle,
                    dummySearchInputSubtitle,
                    dummySearchInputHint
            ), null
    )
    private val errorHeaderInfoEvent = DummyOnVerticalsHeaderInfoFetched(
            null,
            DummyFetchVerticalHeaderInfoError(GENERIC_ERROR, null)
    )
    val firstModel = VerticalModel("firstModel", "1")
    private val secondModel = VerticalModel("secondModel", "2")
    private val headerAndEmptyInputState = VerticalsUiState(
            showFullscreenError = false,
            showFullscreenProgress = false,
            showContent = true,
            showSkipButton = true,
            items = listOf(
                    VerticalsHeaderUiState(dummySearchInputTitle, dummySearchInputSubtitle),
                    VerticalsSearchInputUiState(dummySearchInputHint, false, false)
            )
    )
    private val fetchingSuggestionsState = VerticalsUiState(
            showFullscreenError = false,
            showFullscreenProgress = false,
            showContent = true,
            showSkipButton = false,
            items = listOf(VerticalsSearchInputUiState(dummySearchInputHint, true, true))
    )

    private val fetchingSuggestionsFailedState = VerticalsUiState(
            showFullscreenError = false,
            showFullscreenProgress = false,
            showContent = true,
            showSkipButton = false,
            items = listOf(
                    VerticalsSearchInputUiState(dummySearchInputHint, false, true),
                    VerticalsFetchSuggestionsErrorUiState(
                            R.string.site_creation_fetch_suggestions_failed,
                            R.string.button_retry
                    )
            )
    )

    private val firstModelDisplayedState = VerticalsUiState(
            showFullscreenError = false,
            showFullscreenProgress = false,
            showContent = true,
            showSkipButton = false,
            items = listOf(
                    VerticalsSearchInputUiState(dummySearchInputHint, false, true),
                    VerticalsModelUiState(firstModel.verticalId, firstModel.name)
            )
    )

    private val secondModelDisplayedState = VerticalsUiState(
            showFullscreenError = false,
            showFullscreenProgress = false,
            showContent = true,
            showSkipButton = false,
            items = listOf(
                    VerticalsSearchInputUiState(dummySearchInputHint, false, true),
                    VerticalsModelUiState(secondModel.verticalId, secondModel.name)
            )
    )

    private val firstModelEvent = OnVerticalsFetched("a", listOf(firstModel), null)
    private val secondModelEvent = OnVerticalsFetched("b", listOf(secondModel), null)
    private val fetchSuggestionsFailedEvent = OnVerticalsFetched(
            "c",
            emptyList(),
            FetchVerticalsError(VerticalErrorType.GENERIC_ERROR, null)
    )

    @Before
    fun setUp() {
        viewModel = NewSiteCreationVerticalsViewModel(
                dispatcher,
                fetchVerticalsHeaderInfoUseCase,
                fetchVerticalsUseCase,
                Dispatchers.Unconfined,
                Dispatchers.Unconfined
        )
        val uiStateObservable = viewModel.uiState
        uiStateObservable.observeForever(uiStateObserver)
    }

    @Test
    fun verifyHeaderInfoFetchedOnStart() = test {
        whenever(fetchVerticalsHeaderInfoUseCase.fetchVerticalHeaderInfo()).thenReturn(successHeaderInfoEvent)
        viewModel.start()
        assertTrue(viewModel.uiState.value!!.showContent)
        assertFalse(viewModel.uiState.value!!.showFullscreenError)
    }

    @Test
    fun verifyFullscreenErrorShownOnFailedHeaderInfoRequest() = test {
        whenever(fetchVerticalsHeaderInfoUseCase.fetchVerticalHeaderInfo()).thenReturn(errorHeaderInfoEvent)
        viewModel.start()
        assertFalse(viewModel.uiState.value!!.showContent)
        assertTrue(viewModel.uiState.value!!.showFullscreenError)
    }

    @Test
    fun verifyRetryWorksOnFullScreenError() = test {
        whenever(fetchVerticalsHeaderInfoUseCase.fetchVerticalHeaderInfo()).thenReturn(errorHeaderInfoEvent)
        viewModel.start()
        assertTrue(viewModel.uiState.value!!.showFullscreenError)

        viewModel.onFetchHeaderInfoRetry()
        assertTrue(viewModel.uiState.value!!.showFullscreenError)

        whenever(fetchVerticalsHeaderInfoUseCase.fetchVerticalHeaderInfo()).thenReturn(successHeaderInfoEvent)
        viewModel.onFetchHeaderInfoRetry()
        assertTrue(viewModel.uiState.value!!.showContent)
        assertFalse(viewModel.uiState.value!!.showFullscreenError)
    }

    @Test
    fun verifyHeaderShownOnEmptyQuery() = test {
        whenever(fetchVerticalsHeaderInfoUseCase.fetchVerticalHeaderInfo()).thenReturn(successHeaderInfoEvent)
        viewModel.start()
        assertTrue(viewModel.uiState.value!!.items[0] is VerticalsHeaderUiState)
    }

    @Test
    fun verifyHeaderNotShownOnNonEmptyQuery() = test {
        whenever(fetchVerticalsHeaderInfoUseCase.fetchVerticalHeaderInfo()).thenReturn(successHeaderInfoEvent)
        whenever(fetchVerticalsUseCase.fetchVerticals(any())).thenReturn(firstModelEvent)
        viewModel.start()
        viewModel.updateQuery("a", 0)
        assertFalse(viewModel.uiState.value!!.items[0] is VerticalsHeaderUiState)
    }

    @Test
    fun verifyInputShownOnHeaderInfoFetched() = test {
        whenever(fetchVerticalsHeaderInfoUseCase.fetchVerticalHeaderInfo()).thenReturn(successHeaderInfoEvent)
        viewModel.start()
        val inputState = viewModel.uiState.value!!.items[1] as VerticalsSearchInputUiState
        assertFalse(inputState.showProgress)
        assertFalse(inputState.showClearButton)
        assertEquals(dummySearchInputHint, inputState.hint)
    }

    @Test
    fun verifyClearSearchNotShownOnEmptyQuery() = test {
        whenever(fetchVerticalsHeaderInfoUseCase.fetchVerticalHeaderInfo()).thenReturn(successHeaderInfoEvent)
        viewModel.start()
        val searchState = viewModel.uiState.value!!.items[1] as VerticalsSearchInputUiState
        assertFalse(searchState.showClearButton)
    }

    @Test
    fun verifyClearSearchShownOnNoneEmptyQuery() = test {
        whenever(fetchVerticalsHeaderInfoUseCase.fetchVerticalHeaderInfo()).thenReturn(successHeaderInfoEvent)
        whenever(fetchVerticalsUseCase.fetchVerticals(any())).thenReturn(firstModelEvent)
        viewModel.start()
        viewModel.updateQuery("abc", 0)
        val searchState = viewModel.uiState.value!!.items[0] as VerticalsSearchInputUiState
        assertTrue(searchState.showClearButton)
    }

    @Test
    fun verifySearchProgressNotShownOnHeaderInfoFetched() = test {
        whenever(fetchVerticalsHeaderInfoUseCase.fetchVerticalHeaderInfo()).thenReturn(successHeaderInfoEvent)
        viewModel.start()
        val searchState = viewModel.uiState.value!!.items[1] as VerticalsSearchInputUiState
        assertFalse(searchState.showProgress)
    }

    @Test
    fun verifyStatesAfterUpdatingQuery() = test {
        whenever(fetchVerticalsHeaderInfoUseCase.fetchVerticalHeaderInfo()).thenReturn(successHeaderInfoEvent)
        whenever(fetchVerticalsUseCase.fetchVerticals(any())).thenReturn(firstModelEvent)
        viewModel.start()
        viewModel.updateQuery("a", delay = 0)
        whenever(fetchVerticalsUseCase.fetchVerticals(any())).thenReturn(secondModelEvent)
        viewModel.updateQuery("ab", delay = 0)

        inOrder(uiStateObserver).apply {
            verify(uiStateObserver).onChanged(headerAndEmptyInputState)
            verify(uiStateObserver).onChanged(fetchingSuggestionsState)
            verify(uiStateObserver).onChanged(firstModelDisplayedState)
            verify(uiStateObserver).onChanged(fetchingSuggestionsState)
            verify(uiStateObserver).onChanged(secondModelDisplayedState)
            verifyNoMoreInteractions()
        }
    }

    @Test
    fun verifyRetryItemShownOnFailedSugguestionsRequest() = test {
        whenever(fetchVerticalsHeaderInfoUseCase.fetchVerticalHeaderInfo()).thenReturn(successHeaderInfoEvent)
        whenever(fetchVerticalsUseCase.fetchVerticals(any())).thenReturn(fetchSuggestionsFailedEvent)
        viewModel.start()
        viewModel.updateQuery("a", delay = 0)
        inOrder(uiStateObserver).apply {
            verify(uiStateObserver).onChanged(headerAndEmptyInputState)
            verify(uiStateObserver).onChanged(fetchingSuggestionsState)
            verify(uiStateObserver).onChanged(fetchingSuggestionsFailedState)
            verifyNoMoreInteractions()
        }
    }

}
