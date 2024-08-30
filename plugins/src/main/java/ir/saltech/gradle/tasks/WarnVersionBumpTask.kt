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

package ir.saltech.gradle.tasks

import ir.saltech.gradle.types.LinesChanged
import ir.saltech.gradle.types.VersionType.*
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskExecutionException

/**
 * A Gradle task to warn about API version bumps beyond what is expected
 *
 * The task uses the provided [changesFile] to infer if merging the changes currently present in the
 * repo will have an impact on the public api.
 *
 * @property changesFile a file containing a [LinesChanged]; representing the changes made in this
 *   repo
 * @throws TaskExecutionException if changes cause an minor or major API bump
 */
abstract class WarnVersionBumpTask : DefaultTask() {
  @get:[Optional InputFile] abstract val export: RegularFileProperty

  @get:InputFile abstract val changesFile: RegularFileProperty

  @TaskAction
  fun add() {
    val diff = LinesChanged.fromFile(changesFile.asFile.get())

    if (diff.bump == MAJOR || diff.bump == MINOR) {
      if (export.isPresent) {
        throw TaskExecutionException(this, Exception("Based on exported API, changes are ${diff.bump}, higher than PATCH. If this is intended, add a changelog entry. Otherwise, revert the changes."))
      } else {
        throw TaskExecutionException(this, Exception("Based on last release API, changes are ${diff.bump}, higher than PATCH. If this is intended, add a changelog entry. Otherwise, revert the changes. To compare local changes, run exportApi on a commit first."))
      }
    }
  }
}
