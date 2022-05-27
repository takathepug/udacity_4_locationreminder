package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource(var reminders: MutableList<ReminderDTO> = mutableListOf()) : ReminderDataSource {

    suspend fun getRemindersWithError(returnError: Boolean): Result<List<ReminderDTO>> {
        return if (returnError) {
            Result.Error("TEST-Error")
        } else {
            getReminders()
        }
    }

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        return Result.Success(ArrayList(reminders))
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminders.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        TODO("return the reminder with the id")
    }

    override suspend fun deleteAllReminders() {
        reminders.clear()
    }


}