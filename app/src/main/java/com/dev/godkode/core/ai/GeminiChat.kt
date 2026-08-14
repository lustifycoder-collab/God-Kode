/*
 * This file is part of GodKode.
 *
 * GodKode is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * GodKode is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with GodKode.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package com.dev.godkode.core.ai

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.Chat
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.generationConfig
import com.dev.godkode.core.Secrets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GeminiChat {
    private val model = GenerativeModel(
        modelName = "gemini-3.7-flash",
        apiKey = Secrets.getGenerativeAiApiKey(),
        generationConfig = generationConfig {
            temperature = 0.7f
            topK = 64
            topP = 0.95f
            maxOutputTokens = 8192
        },
        safetySettings = listOf(
            SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.MEDIUM_AND_ABOVE),
            SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.MEDIUM_AND_ABOVE),
            SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.MEDIUM_AND_ABOVE),
            SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.MEDIUM_AND_ABOVE),
        )
    )

    private var chat: Chat? = null

    @Synchronized
    private fun getOrCreateChat(): Chat {
        return chat ?: model.startChat().also { chat = it }
    }

    suspend fun sendMessage(message: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val response = getOrCreateChat().sendMessage(message)
            response.text ?: throw IllegalStateException("Empty response from Gemini")
        }
    }

    @Synchronized
    fun resetChat() {
        chat = null
    }
}
