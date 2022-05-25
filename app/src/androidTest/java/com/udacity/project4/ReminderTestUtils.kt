package com.udacity.project4

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import org.apache.commons.lang3.RandomStringUtils
import kotlin.random.Random

class ReminderTestUtils {
    companion object {
        fun generateReminderDTO(): ReminderDTO {
            // return random new reminder
            return ReminderDTO(
                title = "title " + randText(),
                description = "description " + randText(),
                location = "location " + randText(),
                latitude = Random.nextDouble(),
                longitude = Random.nextDouble()
            )
        }

        private fun randText(length: Int = 2): String {
            return RandomStringUtils.randomAlphanumeric(length);
        }
    }


}