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

plugins {
    id("com.android.library") version "8.1.3" apply false
    id("org.jetbrains.dokka") version "1.8.20" apply false
    kotlin("android") version "1.8.22" apply false
    kotlin("plugin.serialization") version "1.8.22" apply false
    id("com.ncorti.ktfmt.gradle") version "0.18.0" apply false
    id("com.vanniktech.maven.publish") version "0.28.0" apply false
    id("com.gradleup.nmcp") version "0.0.7" apply false
    id("license-plugin")
    id("multi-project-plugin")
}

license {
    template.set(file("licenses/APACHE-2.0"))
    include.set(listOf("**/*.kt", "**/*.kts"))
}
