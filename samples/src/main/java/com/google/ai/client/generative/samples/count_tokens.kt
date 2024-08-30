/*
 * Copyright 2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ir.saltech.ai.client.generative.samples

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import ir.saltech.ai.client.generativeai.GenerativeModel
import ir.saltech.ai.client.generativeai.type.Schema
import ir.saltech.ai.client.generativeai.type.Tool
import ir.saltech.ai.client.generativeai.type.content
import ir.saltech.ai.client.generativeai.type.defineFunction
import ir.saltech.ai.sample.R

// Set up your API Key
// ====================
//
// To use the Gemini API, you'll need an API key. To learn more, see
// the "Set up your API Key section" in the [Gemini API
// quickstart](https://ai.google.dev/gemini-api/docs/quickstart?lang=android#set-up-api-key).

suspend fun tokensTextOnly() {
  // [START tokens_text_only]
  val generativeModel =
      GenerativeModel(
          // Specify a Gemini model appropriate for your use case
          modelName = "gemini-1.5-flash",
          // Access your API key as a Build Configuration variable (see "Set up your API key" above)
          apiKey = BuildConfig.apiKey)

  // For text-only input
  val (totalTokens) = generativeModel.countTokens("Write a story about a magic backpack.")
  print(totalTokens)
  // [END tokens_text_only]
}

suspend fun tokensChat() {
  // [START tokens_chat]
  val generativeModel =
      GenerativeModel(
          // Specify a Gemini model appropriate for your use case
          modelName = "gemini-1.5-flash",
          // Access your API key as a Build Configuration variable (see "Set up your API key" above)
          apiKey = BuildConfig.apiKey)

  val chat =
      generativeModel.startChat(
          history =
              listOf(
                  content(role = "user") { text("Hello, I have 2 dogs in my house.") },
                  content(role = "model") {
                    text("Great to meet you. What would you like to know?")
                  }))

  val history = chat.history
  val messageContent = content { text("This is the message I intend to send") }
  val (totalTokens) = generativeModel.countTokens(*history.toTypedArray(), messageContent)
  print(totalTokens)
  // [END tokens_chat]
}

suspend fun tokensMultimodalImageInline(context: Context) {
  // [START tokens_multimodal_image_inline]
  val generativeModel =
      GenerativeModel(
          // Specify a Gemini model appropriate for your use case
          modelName = "gemini-1.5-flash",
          // Access your API key as a Build Configuration variable (see "Set up your API key" above)
          apiKey = BuildConfig.apiKey)

  val image1: Bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.image1)
  val image2: Bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.image2)

  val multiModalContent = content {
    image(image1)
    image(image2)
    text("What's the difference between these pictures?")
  }

  val (totalTokens) = generativeModel.countTokens(multiModalContent)
  print(totalTokens)
  // [END tokens_multimodal_image_inline]
}

suspend fun tokensSystemInstruction() {
    // [START tokens_system_instruction]
    val generativeModel =
        GenerativeModel(
            // Specify a Gemini model appropriate for your use case
            modelName = "gemini-1.5-flash",
            // Access your API key as a Build Configuration variable (see "Set up your API key" above)
            apiKey = BuildConfig.apiKey,
            systemInstruction = content(role = "system") { text("You are a cat. Your name is Neko.")}
        )

    // For text-only input
    val (totalTokens) = generativeModel.countTokens("What is your name?")
    print(totalTokens)
    // [END tokens_system_instruction]
}

suspend fun tokenTools() {
    // [START tokens_tools]
    val multiplyDefinition = defineFunction(
        name = "multiply",
        description = "returns the product of the provided numbers.",
        parameters = listOf(
            Schema.double("a", "First number"),
            Schema.double("b", "Second number")
        )
    )
    val usableFunctions = listOf(multiplyDefinition)

    val generativeModel =
        GenerativeModel(
            // Specify a Gemini model appropriate for your use case
            modelName = "gemini-1.5-flash",
            // Access your API key as a Build Configuration variable (see "Set up your API key" above)
            apiKey = BuildConfig.apiKey,
            tools = listOf(Tool(usableFunctions))
        )

    // For text-only input
    val (totalTokens) = generativeModel.countTokens("What's the product of 9 and 358?")
    print(totalTokens)
    // [END tokens_tools]
}