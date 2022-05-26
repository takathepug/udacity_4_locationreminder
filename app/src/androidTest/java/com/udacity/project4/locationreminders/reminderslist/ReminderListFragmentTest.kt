package com.udacity.project4.locationreminders.reminderslist

import android.os.Bundle
import com.udacity.project4.R
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.data.local.LocalDB
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import com.udacity.project4.ReminderTestUtils.Companion.generateReminderDTO
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.GlobalContext.get
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import androidx.test.espresso.matcher.ViewMatchers.*
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.not
import org.mockito.Mockito.mock
import org.koin.core.context.GlobalContext
import org.koin.test.get
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest {
    private lateinit var repository: ReminderDataSource

    // executes each task synchronously
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun initRepository() {
        stopKoin()

        // koin config
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    getApplicationContext(),
                    get() as ReminderDataSource
                )
            }

            single { RemindersLocalRepository(get()) as ReminderDataSource }
            single { LocalDB.createRemindersDao(getApplicationContext()) }
        }

        startKoin {
            androidContext(getApplicationContext())
            modules(listOf(myModule))
        }

        repository = get().koin.get()

        runBlocking {
            repository.deleteAllReminders()
        }

    }

    @Test
    fun noRemindersNoDataMsg() = runBlockingTest {
        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

        onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))
    }

    @Test
    fun remindersShown() = runBlockingTest {

        val newReminder1: ReminderDTO = generateReminderDTO()
        val newReminder2: ReminderDTO = generateReminderDTO()

        runBlocking {
            repository.saveReminder(newReminder1)
            repository.saveReminder(newReminder2)
        }

        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

        // the "no data" text shouldn't be displayed
        onView(withId(R.id.noDataTextView)).check(matches(not(isDisplayed())))

        // check first reminder
        onView(withText(newReminder1.title)).check(matches(isDisplayed()))
        onView(withText(newReminder1.description)).check(matches(isDisplayed()))
        onView(withText(newReminder1.location)).check(matches(isDisplayed()))

        // check second reminder
        onView(withText(newReminder2.title)).check(matches(isDisplayed()))
        onView(withText(newReminder2.description)).check(matches(isDisplayed()))
        onView(withText(newReminder2.location)).check(matches(isDisplayed()))
    }

    @Test
    fun clickOnFabIcon_navigatesTo_saveReminderFragment() {
        val scenario =
            launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        val navController = mock(NavController::class.java)

        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }

        onView(withId(R.id.addReminderFAB)).perform(click())

        verify(navController).navigate(ReminderListFragmentDirections.toSaveReminder())
    }

}