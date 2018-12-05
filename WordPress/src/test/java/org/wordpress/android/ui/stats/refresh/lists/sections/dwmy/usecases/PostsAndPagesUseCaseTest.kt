package org.wordpress.android.ui.stats.refresh.lists.sections.dwmy.usecases

import com.nhaarman.mockito_kotlin.whenever
import kotlinx.coroutines.experimental.Dispatchers
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.time.PostAndPageViewsModel
import org.wordpress.android.fluxc.model.stats.time.PostAndPageViewsModel.ViewsModel
import org.wordpress.android.fluxc.model.stats.time.PostAndPageViewsModel.ViewsType.HOMEPAGE
import org.wordpress.android.fluxc.model.stats.time.PostAndPageViewsModel.ViewsType.PAGE
import org.wordpress.android.fluxc.model.stats.time.PostAndPageViewsModel.ViewsType.POST
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.store.StatsStore.OnStatsFetched
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.stats.time.PostAndPageViewsStore
import org.wordpress.android.test
import org.wordpress.android.ui.stats.refresh.lists.BlockList
import org.wordpress.android.ui.stats.refresh.lists.Error
import org.wordpress.android.ui.stats.refresh.lists.NavigationTarget
import org.wordpress.android.ui.stats.refresh.lists.NavigationTarget.ViewPostsAndPages
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Type.BLOCK_LIST
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Type.ERROR
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Link
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
import org.wordpress.android.ui.stats.refresh.utils.toFormattedString

private const val pageSize = 6
private val statsGranularity = DAYS

class PostsAndPagesUseCaseTest : BaseUnitTest() {
    @Mock lateinit var store: PostAndPageViewsStore
    @Mock lateinit var site: SiteModel
    @Mock lateinit var statsDateFormatter: StatsDateFormatter
    private lateinit var useCase: PostsAndPagesUseCase
    @Before
    fun setUp() {
        useCase = PostsAndPagesUseCase(statsGranularity, Dispatchers.Unconfined, store, statsDateFormatter)
    }

    @Test
    fun `returns failed item when store fails`() = test {
        val forced = false
        val refresh = true
        val message = "error"
        whenever(
                store.fetchPostAndPageViews(
                        site,
                        pageSize,
                        statsGranularity,
                        forced
                )
        ).thenReturn(OnStatsFetched(StatsError(GENERIC_ERROR, message)))

        val result = loadData(refresh, forced)

        assertThat(result is Error).isTrue()
        assertThat(result.type).isEqualTo(ERROR)
        assertThat((result as Error).errorMessage).isEqualTo(message)
    }

    @Test
    fun `result contains only empty item when response is empty`() = test {
        val forced = false
        val refresh = true
        val emptyModel = PostAndPageViewsModel(listOf(), false)
        whenever(
                store.fetchPostAndPageViews(
                        site,
                        pageSize,
                        statsGranularity,
                        forced
                )
        ).thenReturn(OnStatsFetched(emptyModel))

        val result = loadData(refresh, forced)

        assertThat(result is BlockList).isTrue()
        assertThat(result.type).isEqualTo(BLOCK_LIST)
        val items = (result as BlockList).items
        assertThat(items.size).isEqualTo(2)
        assertThat(items[0] is Title).isTrue()
        assertThat((items[0] as Title).text).isEqualTo(R.string.stats_posts_and_pages)
        assertThat(items[1] is Empty).isTrue()
    }

    @Test
    fun `result converts post`() = test {
        val forced = false
        val refresh = true
        val post = ViewsModel(1L, "Post 1", 10, POST, "post.com")
        val model = PostAndPageViewsModel(listOf(post), false)
        whenever(
                store.fetchPostAndPageViews(
                        site,
                        pageSize,
                        statsGranularity,
                        forced
                )
        ).thenReturn(OnStatsFetched(model))

        val result = loadData(refresh, forced)

        assertThat(result is BlockList).isTrue()
        assertThat(result.type).isEqualTo(BLOCK_LIST)
        val items = (result as BlockList).items
        assertThat(items.size).isEqualTo(2)
        assertThat(items[0] is Title).isTrue()
        assertThat((items[0] as Title).text).isEqualTo(R.string.stats_posts_and_pages)
        assertThat(items[1] is ListItemWithIcon).isTrue()
        val item = items[1] as ListItemWithIcon
        assertThat(item.icon).isEqualTo(R.drawable.ic_posts_grey_dark_24dp)
        assertThat(item.text).isEqualTo(post.title)
        assertThat(item.value).isEqualTo("10")
    }

    @Test
    fun `result converts page`() = test {
        val forced = false
        val refresh = true
        val title = "Page 1"
        val views = 15
        val page = ViewsModel(2L, title, views, PAGE, "page.com")
        val model = PostAndPageViewsModel(listOf(page), false)
        whenever(
                store.fetchPostAndPageViews(
                        site,
                        pageSize,
                        statsGranularity,
                        forced
                )
        ).thenReturn(OnStatsFetched(model))

        val result = loadData(refresh, forced)

        assertThat(result is BlockList).isTrue()
        assertThat(result.type).isEqualTo(BLOCK_LIST)
        val items = (result as BlockList).items
        assertThat(items.size).isEqualTo(2)
        assertThat(items[0] is Title).isTrue()
        assertThat((items[0] as Title).text).isEqualTo(R.string.stats_posts_and_pages)
        assertThat(items[1] is ListItemWithIcon).isTrue()
        val item = items[1] as ListItemWithIcon
        assertThat(item.icon).isEqualTo(R.drawable.ic_pages_grey_dark_24dp)
        assertThat(item.text).isEqualTo(title)
        assertThat(item.value).isEqualTo(views.toString())
    }

    @Test
    fun `result converts home page`() = test {
        val forced = false
        val refresh = true
        val title = "Homepage 1"
        val views = 20
        val homePage = ViewsModel(3L, title, views, HOMEPAGE, "homepage.com")
        val model = PostAndPageViewsModel(listOf(homePage), false)
        whenever(
                store.fetchPostAndPageViews(
                        site,
                        pageSize,
                        statsGranularity,
                        forced
                )
        ).thenReturn(OnStatsFetched(model))

        val result = loadData(refresh, forced)

        assertThat(result is BlockList).isTrue()
        assertThat(result.type).isEqualTo(BLOCK_LIST)
        val items = (result as BlockList).items
        assertThat(items.size).isEqualTo(2)
        assertThat(items[0] is Title).isTrue()
        assertThat((items[0] as Title).text).isEqualTo(R.string.stats_posts_and_pages)
        assertThat(items[1] is ListItemWithIcon).isTrue()
        val item = items[1] as ListItemWithIcon
        assertThat(item.icon).isEqualTo(R.drawable.ic_pages_grey_dark_24dp)
        assertThat(item.text).isEqualTo(title)
        assertThat(item.value).isEqualTo(views.toFormattedString())
    }

    @Test
    fun `shows divider between items`() = test {
        val forced = false
        val refresh = true
        val page = ViewsModel(2L, "Page 1", 10, PAGE, "page.com")
        val homePage = ViewsModel(3L, "Homepage 1", 20, HOMEPAGE, "homepage.com")
        val model = PostAndPageViewsModel(listOf(page, homePage), false)
        whenever(
                store.fetchPostAndPageViews(
                        site,
                        pageSize,
                        statsGranularity,
                        forced
                )
        ).thenReturn(OnStatsFetched(model))

        val result = loadData(refresh, forced)

        assertThat(result is BlockList).isTrue()
        assertThat(result.type).isEqualTo(BLOCK_LIST)
        val items = (result as BlockList).items
        assertThat(items.size).isEqualTo(3)
        assertThat(items[1] is ListItemWithIcon).isTrue()
        assertThat(items[2] is ListItemWithIcon).isTrue()
        assertThat((items[1] as ListItemWithIcon).showDivider).isEqualTo(true)
        assertThat((items[2] as ListItemWithIcon).showDivider).isEqualTo(false)
    }

    @Test
    fun `shows view more button when hasMore is true`() = test {
        val forced = false
        val refresh = true
        val id = 2L
        val url = "page.com"
        val page = ViewsModel(id, "Page 1", 10, PAGE, url)
        val hasMore = true
        val model = PostAndPageViewsModel(listOf(page), hasMore)
        whenever(statsDateFormatter.todaysDateInStatsFormat()).thenReturn("2018-10-10")
        whenever(
                store.fetchPostAndPageViews(
                        site,
                        pageSize,
                        statsGranularity,
                        forced
                )
        ).thenReturn(OnStatsFetched(model))

        val result = loadData(refresh, forced)

        assertThat(result is BlockList).isTrue()
        assertThat(result.type).isEqualTo(BLOCK_LIST)
        val items = (result as BlockList).items
        assertThat(items.size).isEqualTo(3)
        assertThat(items[1] is ListItemWithIcon).isTrue()
        assertThat(items[2] is Link).isTrue()

        var navigationTarget: NavigationTarget? = null
        useCase.navigationTarget.observeForever { navigationTarget = it }

        (items[2] as Link).navigateAction.click()

        assertThat(navigationTarget).isNotNull
        val viewPost = navigationTarget as ViewPostsAndPages
        assertThat(viewPost.statsGranularity).isEqualTo(statsGranularity)
    }

    private suspend fun loadData(refresh: Boolean, forced: Boolean): StatsBlock {
        var result: StatsBlock? = null
        useCase.liveData.observeForever { result = it }
        useCase.fetch(site, refresh, forced)
        return checkNotNull(result)
    }
}