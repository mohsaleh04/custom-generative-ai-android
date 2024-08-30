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

@file:OptIn(ExperimentalSerializationApi::class)

package ir.saltech.ai.client.generativeai.common

import ir.saltech.ai.client.generativeai.common.client.GenerationConfig
import ir.saltech.ai.client.generativeai.common.client.Tool
import ir.saltech.ai.client.generativeai.common.client.ToolConfig
import ir.saltech.ai.client.generativeai.common.shared.Content
import ir.saltech.ai.client.generativeai.common.shared.SafetySetting
import ir.saltech.ai.client.generativeai.common.util.fullModelName
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

sealed interface Request

@Serializable
data class GenerateContentRequest(
  val model: String? = null,
  val contents: List<Content>,
  @SerialName("safety_settings") val safetySettings: List<SafetySetting>? = null,
  @SerialName("generation_config") val generationConfig: GenerationConfig? = null,
  val tools: List<Tool>? = null,
  @SerialName("tool_config") var toolConfig: ToolConfig? = null,
  @SerialName("system_instruction") val systemInstruction: Content? = null,
) : Request

@Serializable
data class CountTokensRequest(
  val generateContentRequest: GenerateContentRequest? = null,
  val model: String? = null,
  val contents: List<Content>? = null,
  val tools: List<Tool>? = null,
  @SerialName("system_instruction") val systemInstruction: Content? = null,
) : Request {
  companion object {
    fun forGenAI(generateContentRequest: GenerateContentRequest) =
      CountTokensRequest(
        generateContentRequest =
          generateContentRequest.model?.let {
            generateContentRequest.copy(model = fullModelName(it))
          } ?: generateContentRequest
      )

    fun forVertexAI(generateContentRequest: GenerateContentRequest) =
      CountTokensRequest(
        model = generateContentRequest.model?.let { fullModelName(it) },
        contents = generateContentRequest.contents,
        tools = generateContentRequest.tools,
        systemInstruction = generateContentRequest.systemInstruction,
      )
  }
}
