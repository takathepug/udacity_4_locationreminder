package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource(var reminders: MutableList<ReminderDTO> = mutableListOf()) :
    ReminderDataSource {
    companion object {
        val ERROR_MSG = "Error"
    }

    var shouldReturnError: Boolean = false

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        return if (shouldReturnError) {
            Result.Error(ERROR_MSG)
        } else {
            Result.Success(ArrayList(reminders))
        }
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminders.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        return if (shouldReturnError) {
            Result.Error(ERROR_MSG)
        } else {
            val reminder = reminders.find { it.id == id }

            if (reminder != null) Result.Success(reminder) else Result.Error(
                "Reminder not found",
                404
            )
        }

    }

    override suspend fun deleteAllReminders() {
        reminders.clear()
    }


}