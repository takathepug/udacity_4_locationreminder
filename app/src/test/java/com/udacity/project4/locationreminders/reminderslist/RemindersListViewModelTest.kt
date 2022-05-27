package com.udacity.project4.locationreminders.reminderslist

import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

@Config(sdk = [Build.VERSION_CODES.P])
@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private lateinit var datasource: FakeDataSource
    private lateinit var remindersListViewModel: RemindersListViewModel

    @Before
    fun setUp() {
        datasource = FakeDataSource()
        remindersListViewModel  =
            RemindersListViewModel(ApplicationProvider.getApplicationContext(), datasource)
    }

    @After
    fun stoppingKoin() {
        stopKoin()
    }

    // provide testing to the RemindersListViewModel and its live data objects
    @Test
    fun loadRemindersOnErrorShowsSnackBar() = runBlockingTest {

        datasource.shouldReturnError = true
        remindersListViewModel.loadReminders()
        assertThat(remindersListViewModel.showSnackBar.getOrAwaitValue(), `is`(FakeDataSource.ERROR_MSG))

    }

    @Test
    fun loadEmptyRemindersShowsNoData() = runBlockingTest {
        remindersListViewModel.loadReminders()

        assertThat(remindersListViewModel.showNoData.getOrAwaitValue(), `is`(true))
    }

    @Test
    fun loadRemindersShowsLoading() = mainCoroutineRule.runBlockingTest {
        mainCoroutineRule.pauseDispatcher()

        // load paused
        remindersListViewModel.loadReminders()

        // loading shows
        assertThat(
            remindersListViewModel.showLoading.getOrAwaitValue(),
            `is`(true)
        )

        // resume and loading is hidden
        mainCoroutineRule.resumeDispatcher()

        assertThat(remindersListViewModel.showLoading.getOrAwaitValue(), `is`(false))
    }

}