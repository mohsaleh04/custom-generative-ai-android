// Copyright 2024 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package ir.saltech.ai.client.generative.samples.java;

import ir.saltech.ai.client.generativeai.GenerativeModel;
import ir.saltech.ai.client.generativeai.java.GenerativeModelFutures;
import ir.saltech.ai.client.generativeai.type.BlockThreshold;
import ir.saltech.ai.client.generativeai.type.HarmCategory;
import ir.saltech.ai.client.generativeai.type.SafetySetting;
import java.util.Arrays;
import java.util.Collections;

// Set up your API Key
// ====================
//
// To use the Gemini API, you'll need an API key. To learn more, see
// the "Set up your API Key section" in the [Gemini API
// quickstart](https://ai.google.dev/gemini-api/docs/quickstart?lang=android#set-up-api-key).

class SafetySettings {
  void safetySettings() {
    // [START safety_settings]
    SafetySetting harassmentSafety =
        new SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.ONLY_HIGH);

    // Specify a Gemini model appropriate for your use case
    GenerativeModel gm =
        new GenerativeModel(
            "gemini-1.5-flash",
            BuildConfig.apiKey,
            null, // generation config is optional
            Collections.singletonList(harassmentSafety));

    GenerativeModelFutures model = GenerativeModelFutures.from(gm);
    // [END safety_settings]
  }

  void SafetySettingsMulti() {
    // [START safety_settings_multi]
    SafetySetting harassmentSafety =
        new SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.ONLY_HIGH);

    SafetySetting hateSpeechSafety =
        new SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.MEDIUM_AND_ABOVE);

    // Specify a Gemini model appropriate for your use case
    GenerativeModel gm =
        new GenerativeModel(
            "gemini-1.5-flash",
            BuildConfig.apiKey,
            null, // generation config is optional
            Arrays.asList(harassmentSafety, hateSpeechSafety));

    GenerativeModelFutures model = GenerativeModelFutures.from(gm);
    // [END safety_settings_multi]
  }
}
