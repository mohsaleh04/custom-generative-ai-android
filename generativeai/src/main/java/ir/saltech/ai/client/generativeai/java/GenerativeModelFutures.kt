/*
 * Copyright 2023 Google LLC
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

package ir.saltech.ai.client.generativeai.java

import androidx.concurrent.futures.SuspendToFutureAdapter
import ir.saltech.ai.client.generativeai.GenerativeModel
import ir.saltech.ai.client.generativeai.type.Content
import ir.saltech.ai.client.generativeai.type.CountTokensResponse
import ir.saltech.ai.client.generativeai.type.GenerateContentResponse
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.reactive.asPublisher
import org.reactivestreams.Publisher

/**
 * Helper method for interacting with a [GenerativeModel] from Java.
 *
 * @see from
 */
abstract class GenerativeModelFutures internal constructor() {

    /**
     * Generates a response from the backend with the provided [Content]s.
     *
     * @param prompt A group of [Content]s to send to the model.
     */
    abstract fun generateContent(vararg prompt: Content): ListenableFuture<GenerateContentResponse>

    /**
     * Generates a streaming response from the backend with the provided [Content]s.
     *
     * @param prompt A group of [Content]s to send to the model.
     */
    abstract fun generateContentStream(vararg prompt: Content): Publisher<GenerateContentResponse>

    /**
     * Counts the number of tokens used in a prompt.
     *
     * @param prompt A group of [Content]s to count tokens of.
     */
    abstract fun countTokens(vararg prompt: Content): ListenableFuture<CountTokensResponse>

    /** Creates a chat instance which internally tracks the ongoing conversation with the model */
    abstract fun startChat(): ChatFutures

    /**
     * Creates a chat instance which internally tracks the ongoing conversation with the model
     *
     * @param history an existing history of context to use as a starting point
     */
    abstract fun startChat(history: List<Content>): ChatFutures

    /** Returns the [GenerativeModel] instance that was used to create this object */
    abstract fun getGenerativeModel(): GenerativeModel

    private class FuturesImpl(private val model: GenerativeModel) : GenerativeModelFutures() {
        override fun generateContent(
            vararg prompt: Content
        ): ListenableFuture<GenerateContentResponse> =
            SuspendToFutureAdapter.launchFuture { model.generateContent(*prompt) }

        override fun generateContentStream(vararg prompt: Content): Publisher<GenerateContentResponse> =
            model.generateContentStream(*prompt).asPublisher()

        override fun countTokens(vararg prompt: Content): ListenableFuture<CountTokensResponse> =
            SuspendToFutureAdapter.launchFuture { model.countTokens(*prompt) }

        override fun startChat(): ChatFutures = startChat(emptyList())

        override fun startChat(history: List<Content>): ChatFutures =
            ChatFutures.from(model.startChat(history))

        override fun getGenerativeModel(): GenerativeModel = model
    }

    companion object {

        /** @return a [GenerativeModelFutures] created around the provided [GenerativeModel] */
        @JvmStatic
        fun from(model: GenerativeModel): GenerativeModelFutures = FuturesImpl(model)
    }
}
