package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.udacity.project4.locationreminders.data.dto.ReminderDTO

import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.junit.After
import org.junit.Test
import org.junit.Assert.*

import kotlinx.coroutines.ExperimentalCoroutinesApi;
import kotlinx.coroutines.test.runBlockingTest
import org.apache.commons.lang3.RandomStringUtils

import kotlin.random.Random

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {
    private lateinit var database: RemindersDatabase

    // executes each task synchronously
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun initDB() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).build()
    }

    @After
    fun closeDB() = database.close()

    // Add testing implementation to the RemindersDao.kt
    @Test
    fun insertAndSelectAll() = runBlockingTest {
        val newReminder: ReminderDTO = generateReminderDTO()
        database.reminderDao().saveReminder(newReminder)

        val allReminders: List<ReminderDTO> = database.reminderDao().getReminders()

        // there should be only 1 reminder in DB
        assertTrue(allReminders.size == 1)

        assertEquals(newReminder, allReminders[0])
    }

    @Test
    fun insertAndSelectById() = runBlockingTest {
        val newReminder: ReminderDTO = generateReminderDTO()
        database.reminderDao().saveReminder(newReminder)

        val obtainedReminder: ReminderDTO? = database.reminderDao().getReminderById(newReminder.id)

        assertEquals(newReminder, obtainedReminder)
    }

    @Test
    fun insertTwoAndDeleteAll() = runBlockingTest {
        database.reminderDao().saveReminder(generateReminderDTO())
        database.reminderDao().saveReminder(generateReminderDTO())

        val allReminders: List<ReminderDTO> = database.reminderDao().getReminders()
        // there should be 2 reminders in DB
        assertTrue(allReminders.size == 2)

        database.reminderDao().deleteAllReminders()
        val noReminders: List<ReminderDTO> = database.reminderDao().getReminders()
        // there should be only no contacts in DB
        assertTrue(noReminders.isEmpty())
    }

    // utils
    private fun generateReminderDTO(): ReminderDTO {
        // return random new reminder
        return ReminderDTO(
            title = "title " + randText(),
            description = "description " + randText(),
            location = "location " + randText(),
            latitude = Random.nextDouble(),
            longitude = Random.nextDouble())
    }

    private fun randText(length: Int = 2): String {
        return RandomStringUtils.randomAlphanumeric(length);
    }

}