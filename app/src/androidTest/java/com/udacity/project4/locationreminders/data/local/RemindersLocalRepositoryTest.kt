package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.ReminderTestUtils.Companion.generateReminderDTO
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

    // Add testing implementation to the RemindersLocalRepository.kt
    private lateinit var database: RemindersDatabase
    private lateinit var repository: RemindersLocalRepository

    // executes each task synchronously
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun initRepository() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).allowMainThreadQueries().build()

        repository = RemindersLocalRepository(database.reminderDao(), Dispatchers.Main)
    }

    @After
    fun cleanUp() {
        database.close()
    }

    @Test
    fun insertAndSelectAll() = runBlocking  {
        val newReminder: ReminderDTO = generateReminderDTO()
        repository.saveReminder(newReminder)

        val allReminders: List<ReminderDTO> =
            (repository.getReminders() as Result.Success<List<ReminderDTO>>).data

        // there should be only 1 reminder in DB
        Assert.assertTrue(allReminders.size == 1)

        Assert.assertEquals(newReminder, allReminders[0])
    }

    @Test
    fun insertAndSelectById() = runBlocking  {
        val newReminder: ReminderDTO = generateReminderDTO()
        repository.saveReminder(newReminder)

        val obtainedReminder: ReminderDTO? =
            (repository.getReminder(newReminder.id) as Result.Success<ReminderDTO>).data

        Assert.assertEquals(newReminder, obtainedReminder)
    }

    @Test
    fun insertTwoAndDeleteAll() = runBlocking  {
        repository.saveReminder(generateReminderDTO())
        repository.saveReminder(generateReminderDTO())

        val allReminders: List<ReminderDTO> = (repository.getReminders() as Result.Success<List<ReminderDTO>>).data
        // there should be 2 reminders in DB
        Assert.assertTrue(allReminders.size == 2)

        database.reminderDao().deleteAllReminders()
        val noReminders: List<ReminderDTO> = (repository.getReminders() as Result.Success<List<ReminderDTO>>).data
        // there should be only no contacts in DB
        Assert.assertTrue(noReminders.isEmpty())
    }

}