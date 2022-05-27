package com.udacity.project4.locationreminders.savereminder

import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.R
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

@Config(sdk = [Build.VERSION_CODES.P])
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private lateinit var datasource: FakeDataSource
    private lateinit var saveReminderViewModel: SaveReminderViewModel

    @Before
    fun setUp() {
        datasource = FakeDataSource()
        saveReminderViewModel =
            SaveReminderViewModel(ApplicationProvider.getApplicationContext(), datasource)
    }

    @After
    fun stoppingKoin() {
        stopKoin()
    }

    // provide testing to the SaveReminderView and its live data objects
    @Test
    fun saveReminderNoTitleShowsErrorSnackBar() = mainCoroutineRule.runBlockingTest {
        val newReminder = getFakeReminderItem()
        newReminder.location = null

        val validation = saveReminderViewModel.validateEnteredData(newReminder)
        assertThat(validation, `is`(false))

        assertThat(
            saveReminderViewModel.showSnackBarInt.getOrAwaitValue(),
            `is`(R.string.err_select_location)
        )
    }

    @Test
    fun saveReminderSuccessShowsToast() = mainCoroutineRule.runBlockingTest {
        val newReminder = getFakeReminderItem()

        saveReminderViewModel.saveReminder(newReminder)
        assertThat(saveReminderViewModel.showToast.getOrAwaitValue(), `is`("Reminder Saved !"))
    }

    @Test
    fun saveReminderShowsLoading() = mainCoroutineRule.runBlockingTest {
        // don't execute coroutines automatically
        mainCoroutineRule.pauseDispatcher()

        val newReminder = getFakeReminderItem()

        saveReminderViewModel.validateAndSaveReminder(newReminder)
        assertThat(saveReminderViewModel.showLoading.getOrAwaitValue(), CoreMatchers.`is`(true))

        // resuming hides loading
        mainCoroutineRule.resumeDispatcher()
        assertThat(saveReminderViewModel.showLoading.getOrAwaitValue(), CoreMatchers.`is`(false))
    }

    // utils
    // returns a new valid ReminderDataItem
    private fun getFakeReminderItem(): ReminderDataItem {
        return ReminderDataItem(
            title = "Title 4543",
            description = "Description 3454",
            location = "Location 345643",
            latitude = 10.123456,
            longitude = 10.123456)
    }

}