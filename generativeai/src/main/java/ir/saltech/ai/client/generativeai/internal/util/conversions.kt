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

@file:OptIn(ExperimentalSerializationApi::class)

package ir.saltech.ai.client.generativeai.internal.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import ir.saltech.ai.client.generativeai.common.CountTokensResponse
import ir.saltech.ai.client.generativeai.common.GenerateContentResponse
import ir.saltech.ai.client.generativeai.common.RequestOptions
import ir.saltech.ai.client.generativeai.common.client.GenerationConfig
import ir.saltech.ai.client.generativeai.common.client.Schema
import ir.saltech.ai.client.generativeai.common.server.BlockReason
import ir.saltech.ai.client.generativeai.common.server.Candidate
import ir.saltech.ai.client.generativeai.common.server.CitationSources
import ir.saltech.ai.client.generativeai.common.server.FinishReason
import ir.saltech.ai.client.generativeai.common.server.HarmProbability
import ir.saltech.ai.client.generativeai.common.server.PromptFeedback
import ir.saltech.ai.client.generativeai.common.server.SafetyRating
import ir.saltech.ai.client.generativeai.common.shared.Blob
import ir.saltech.ai.client.generativeai.common.shared.BlobPart
import ir.saltech.ai.client.generativeai.common.shared.CodeExecutionResult
import ir.saltech.ai.client.generativeai.common.shared.CodeExecutionResultPart
import ir.saltech.ai.client.generativeai.common.shared.Content
import ir.saltech.ai.client.generativeai.common.shared.ExecutableCode
import ir.saltech.ai.client.generativeai.common.shared.ExecutableCodePart
import ir.saltech.ai.client.generativeai.common.shared.FileData
import ir.saltech.ai.client.generativeai.common.shared.FileDataPart
import ir.saltech.ai.client.generativeai.common.shared.FunctionCall
import ir.saltech.ai.client.generativeai.common.shared.FunctionCallPart
import ir.saltech.ai.client.generativeai.common.shared.FunctionResponse
import ir.saltech.ai.client.generativeai.common.shared.FunctionResponsePart
import ir.saltech.ai.client.generativeai.common.shared.HarmBlockThreshold
import ir.saltech.ai.client.generativeai.common.shared.HarmCategory
import ir.saltech.ai.client.generativeai.common.shared.Outcome
import ir.saltech.ai.client.generativeai.common.shared.Part
import ir.saltech.ai.client.generativeai.common.shared.SafetySetting
import ir.saltech.ai.client.generativeai.common.shared.TextPart
import ir.saltech.ai.client.generativeai.type.BlockThreshold
import ir.saltech.ai.client.generativeai.type.CitationMetadata
import ir.saltech.ai.client.generativeai.type.ExecutionOutcome
import ir.saltech.ai.client.generativeai.type.FunctionCallingConfig
import ir.saltech.ai.client.generativeai.type.FunctionDeclaration
import ir.saltech.ai.client.generativeai.type.ImagePart
import ir.saltech.ai.client.generativeai.type.SerializationException
import ir.saltech.ai.client.generativeai.type.Tool
import ir.saltech.ai.client.generativeai.type.ToolConfig
import ir.saltech.ai.client.generativeai.type.UsageMetadata
import ir.saltech.ai.client.generativeai.type.content
import kotlinx.serialization.ExperimentalSerializationApi
import java.io.ByteArrayOutputStream
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.json.JSONObject

private const val BASE_64_FLAGS = Base64.NO_WRAP

internal fun ir.saltech.ai.client.generativeai.type.RequestOptions.toInternal() =
  RequestOptions(timeout, apiVersion)

internal fun ir.saltech.ai.client.generativeai.type.Content.toInternal() =
  Content(this.role, this.parts.map { it.toInternal() })

internal fun ir.saltech.ai.client.generativeai.type.Part.toInternal(): Part {
  return when (this) {
    is ir.saltech.ai.client.generativeai.type.TextPart -> TextPart(text)
    is ImagePart -> BlobPart(Blob("image/jpeg", encodeBitmapToBase64Png(image)))
    is ir.saltech.ai.client.generativeai.type.BlobPart ->
      BlobPart(Blob(mimeType, Base64.encodeToString(blob, BASE_64_FLAGS)))
    is ir.saltech.ai.client.generativeai.type.FunctionCallPart ->
      FunctionCallPart(FunctionCall(name, args))
    is ir.saltech.ai.client.generativeai.type.FunctionResponsePart ->
      FunctionResponsePart(FunctionResponse(name, response.toInternal()))
    is ir.saltech.ai.client.generativeai.type.FileDataPart ->
      FileDataPart(FileData(fileUri = uri, mimeType = mimeType))
    is ir.saltech.ai.client.generativeai.type.ExecutableCodePart ->
      ExecutableCodePart(ExecutableCode(language, code))
    is ir.saltech.ai.client.generativeai.type.CodeExecutionResultPart ->
      CodeExecutionResultPart(CodeExecutionResult(outcome.toInternal(), output))
    else ->
      throw SerializationException(
        "The given subclass of Part (${javaClass.simpleName}) is not supported in the serialization yet."
      )
  }
}

internal fun ir.saltech.ai.client.generativeai.type.SafetySetting.toInternal() =
  SafetySetting(harmCategory.toInternal(), threshold.toInternal())

internal fun ir.saltech.ai.client.generativeai.type.GenerationConfig.toInternal() =
  GenerationConfig(
    temperature = temperature,
    topP = topP,
    topK = topK,
    candidateCount = candidateCount,
    maxOutputTokens = maxOutputTokens,
    stopSequences = stopSequences,
    responseMimeType = responseMimeType,
    responseSchema = responseSchema?.toInternal(),
  )

internal fun ir.saltech.ai.client.generativeai.type.HarmCategory.toInternal() =
  when (this) {
    ir.saltech.ai.client.generativeai.type.HarmCategory.HARASSMENT -> HarmCategory.HARASSMENT
    ir.saltech.ai.client.generativeai.type.HarmCategory.HATE_SPEECH -> HarmCategory.HATE_SPEECH
    ir.saltech.ai.client.generativeai.type.HarmCategory.SEXUALLY_EXPLICIT ->
      HarmCategory.SEXUALLY_EXPLICIT
    ir.saltech.ai.client.generativeai.type.HarmCategory.DANGEROUS_CONTENT ->
      HarmCategory.DANGEROUS_CONTENT
    ir.saltech.ai.client.generativeai.type.HarmCategory.UNKNOWN -> HarmCategory.UNKNOWN
  }

internal fun BlockThreshold.toInternal() =
  when (this) {
    BlockThreshold.NONE -> HarmBlockThreshold.BLOCK_NONE
    BlockThreshold.ONLY_HIGH -> HarmBlockThreshold.BLOCK_ONLY_HIGH
    BlockThreshold.MEDIUM_AND_ABOVE -> HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE
    BlockThreshold.LOW_AND_ABOVE -> HarmBlockThreshold.BLOCK_LOW_AND_ABOVE
    BlockThreshold.UNSPECIFIED -> HarmBlockThreshold.UNSPECIFIED
  }

internal fun ExecutionOutcome.toInternal() =
  when (this) {
    ExecutionOutcome.UNSPECIFIED -> Outcome.UNSPECIFIED
    ExecutionOutcome.OK -> Outcome.OUTCOME_OK
    ExecutionOutcome.FAILED -> Outcome.OUTCOME_FAILED
    ExecutionOutcome.DEADLINE_EXCEEDED -> Outcome.OUTCOME_DEADLINE_EXCEEDED
  }

internal fun Tool.toInternal() =
  ir.saltech.ai.client.generativeai.common.client.Tool(
    functionDeclarations?.map { it.toInternal() },
    codeExecution = codeExecution?.toInternal(),
  )

internal fun ToolConfig.toInternal() =
  ir.saltech.ai.client.generativeai.common.client.ToolConfig(
    ir.saltech.ai.client.generativeai.common.client.FunctionCallingConfig(
      when (functionCallingConfig.mode) {
        FunctionCallingConfig.Mode.ANY ->
          ir.saltech.ai.client.generativeai.common.client.FunctionCallingConfig.Mode.ANY
        FunctionCallingConfig.Mode.AUTO ->
          ir.saltech.ai.client.generativeai.common.client.FunctionCallingConfig.Mode.AUTO
        FunctionCallingConfig.Mode.NONE ->
          ir.saltech.ai.client.generativeai.common.client.FunctionCallingConfig.Mode.NONE
      }
    )
  )

internal fun ir.saltech.ai.client.generativeai.common.UsageMetadata.toPublic(): UsageMetadata =
  UsageMetadata(promptTokenCount ?: 0, candidatesTokenCount ?: 0, totalTokenCount ?: 0)

internal fun FunctionDeclaration.toInternal() =
  ir.saltech.ai.client.generativeai.common.client.FunctionDeclaration(
    name,
    description,
    Schema(
      properties = parameters.associate { it.name to it.toInternal() },
      required = requiredParameters,
      type = "OBJECT",
      nullable = false,
    ),
  )

internal fun <T> ir.saltech.ai.client.generativeai.type.Schema<T>.toInternal(): Schema =
  Schema(
    type.name,
    description,
    format,
    nullable,
    enum,
    properties?.mapValues { it.value.toInternal() },
    required,
    items?.toInternal(),
  )

internal fun JSONObject.toInternal() = Json.decodeFromString<JsonObject>(toString())

internal fun Candidate.toPublic(): ir.saltech.ai.client.generativeai.type.Candidate {
  val safetyRatings = safetyRatings?.map { it.toPublic() }.orEmpty()
  val citations = citationMetadata?.citationSources?.map { it.toPublic() }.orEmpty()
  val finishReason = finishReason.toPublic()

  return ir.saltech.ai.client.generativeai.type.Candidate(
    this.content?.toPublic() ?: content("model") {},
    safetyRatings,
    citations,
    finishReason,
  )
}

internal fun Content.toPublic(): ir.saltech.ai.client.generativeai.type.Content =
  ir.saltech.ai.client.generativeai.type.Content(role, parts.map { it.toPublic() })

internal fun Part.toPublic(): ir.saltech.ai.client.generativeai.type.Part {
  return when (this) {
    is TextPart -> ir.saltech.ai.client.generativeai.type.TextPart(text)
    is BlobPart -> {
      val data = Base64.decode(inlineData.data, BASE_64_FLAGS)
      if (inlineData.mimeType.contains("image")) {
        ImagePart(decodeBitmapFromImage(data))
      } else {
        ir.saltech.ai.client.generativeai.type.BlobPart(inlineData.mimeType, data)
      }
    }
    is FunctionCallPart ->
      ir.saltech.ai.client.generativeai.type.FunctionCallPart(functionCall.name, functionCall.args)
    is FunctionResponsePart ->
      ir.saltech.ai.client.generativeai.type.FunctionResponsePart(
        functionResponse.name,
        functionResponse.response.toPublic(),
      )
    is FileDataPart ->
      ir.saltech.ai.client.generativeai.type.FileDataPart(fileData.fileUri, fileData.mimeType)
    is ExecutableCodePart ->
      ir.saltech.ai.client.generativeai.type.ExecutableCodePart(
        executableCode.language,
        executableCode.code,
      )
    is CodeExecutionResultPart ->
      ir.saltech.ai.client.generativeai.type.CodeExecutionResultPart(
        codeExecutionResult.outcome.toPublic(),
        codeExecutionResult.output,
      )
    else ->
      throw SerializationException(
        "Unsupported part type \"${javaClass.simpleName}\" provided. This model may not be supported by this SDK."
      )
  }
}

internal fun CitationSources.toPublic() =
  CitationMetadata(startIndex = startIndex, endIndex = endIndex, uri = uri ?: "", license = license)

internal fun SafetyRating.toPublic() =
  ir.saltech.ai.client.generativeai.type.SafetyRating(category.toPublic(), probability.toPublic())

internal fun PromptFeedback.toPublic(): ir.saltech.ai.client.generativeai.type.PromptFeedback {
  val safetyRatings = safetyRatings?.map { it.toPublic() }.orEmpty()
  return ir.saltech.ai.client.generativeai.type.PromptFeedback(
    blockReason?.toPublic(),
    safetyRatings,
  )
}

internal fun FinishReason?.toPublic() =
  when (this) {
    null -> null
    FinishReason.MAX_TOKENS -> ir.saltech.ai.client.generativeai.type.FinishReason.MAX_TOKENS
    FinishReason.RECITATION -> ir.saltech.ai.client.generativeai.type.FinishReason.RECITATION
    FinishReason.SAFETY -> ir.saltech.ai.client.generativeai.type.FinishReason.SAFETY
    FinishReason.STOP -> ir.saltech.ai.client.generativeai.type.FinishReason.STOP
    FinishReason.OTHER -> ir.saltech.ai.client.generativeai.type.FinishReason.OTHER
    FinishReason.UNSPECIFIED -> ir.saltech.ai.client.generativeai.type.FinishReason.UNSPECIFIED
    FinishReason.UNKNOWN -> ir.saltech.ai.client.generativeai.type.FinishReason.UNKNOWN
  }

internal fun HarmCategory.toPublic() =
  when (this) {
    HarmCategory.HARASSMENT -> ir.saltech.ai.client.generativeai.type.HarmCategory.HARASSMENT
    HarmCategory.HATE_SPEECH -> ir.saltech.ai.client.generativeai.type.HarmCategory.HATE_SPEECH
    HarmCategory.SEXUALLY_EXPLICIT ->
      ir.saltech.ai.client.generativeai.type.HarmCategory.SEXUALLY_EXPLICIT
    HarmCategory.DANGEROUS_CONTENT ->
      ir.saltech.ai.client.generativeai.type.HarmCategory.DANGEROUS_CONTENT
    HarmCategory.UNKNOWN -> ir.saltech.ai.client.generativeai.type.HarmCategory.UNKNOWN
  }

internal fun HarmProbability.toPublic() =
  when (this) {
    HarmProbability.HIGH -> ir.saltech.ai.client.generativeai.type.HarmProbability.HIGH
    HarmProbability.MEDIUM -> ir.saltech.ai.client.generativeai.type.HarmProbability.MEDIUM
    HarmProbability.LOW -> ir.saltech.ai.client.generativeai.type.HarmProbability.LOW
    HarmProbability.NEGLIGIBLE -> ir.saltech.ai.client.generativeai.type.HarmProbability.NEGLIGIBLE
    HarmProbability.UNSPECIFIED ->
      ir.saltech.ai.client.generativeai.type.HarmProbability.UNSPECIFIED
    HarmProbability.UNKNOWN -> ir.saltech.ai.client.generativeai.type.HarmProbability.UNKNOWN
  }

internal fun BlockReason.toPublic() =
  when (this) {
    BlockReason.UNSPECIFIED -> ir.saltech.ai.client.generativeai.type.BlockReason.UNSPECIFIED
    BlockReason.SAFETY -> ir.saltech.ai.client.generativeai.type.BlockReason.SAFETY
    BlockReason.OTHER -> ir.saltech.ai.client.generativeai.type.BlockReason.OTHER
    BlockReason.UNKNOWN -> ir.saltech.ai.client.generativeai.type.BlockReason.UNKNOWN
  }

internal fun Outcome.toPublic() =
  when (this) {
    Outcome.UNSPECIFIED -> ExecutionOutcome.UNSPECIFIED
    Outcome.OUTCOME_OK -> ExecutionOutcome.OK
    Outcome.OUTCOME_FAILED -> ExecutionOutcome.FAILED
    Outcome.OUTCOME_DEADLINE_EXCEEDED -> ExecutionOutcome.DEADLINE_EXCEEDED
  }

internal fun GenerateContentResponse.toPublic() =
  ir.saltech.ai.client.generativeai.type.GenerateContentResponse(
    candidates?.map { it.toPublic() }.orEmpty(),
    promptFeedback?.toPublic(),
    usageMetadata?.toPublic(),
  )

internal fun CountTokensResponse.toPublic() =
  ir.saltech.ai.client.generativeai.type.CountTokensResponse(totalTokens)

internal fun JsonObject.toPublic() = JSONObject(toString())

private fun encodeBitmapToBase64Png(input: Bitmap): String {
  ByteArrayOutputStream().let {
    input.compress(Bitmap.CompressFormat.JPEG, 80, it)
    return Base64.encodeToString(it.toByteArray(), BASE_64_FLAGS)
  }
}

private fun decodeBitmapFromImage(input: ByteArray) =
  BitmapFactory.decodeByteArray(input, 0, input.size)
