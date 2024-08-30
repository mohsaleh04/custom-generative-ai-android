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

package ir.saltech.gradle.tasks

import ir.saltech.gradle.types.LinesChanged
import ir.saltech.gradle.types.VersionType.*
import ir.saltech.gradle.util.SkipTask
import ir.saltech.gradle.util.spoiler
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

/**
 * A Gradle task to warn about API changes made during development.
 *
 * By default, saved in the root project's build directory under `api_changes.md`. The task uses the
 * provided [changesFile] to infer if merging the changes currently present in the repo will have an
 * impact on the public api.
 *
 * This should help developers avoid accidentally leaking internal implementation details into the
 * public API, and causing breaking changes without noticing.
 *
 * The generated message is intended to be safe to display straight to the developer.
 *
 * If no public api changes are found, then no message will be generated and the task will skip
 * itself.
 *
 * @property changesFile a file containing a [LinesChanged]; representing the changes made in this
 *   repo
 * @property outputFile where to save the warning message to
 * @throws SkipTask if no public api changes are found
 */
abstract class WarnAboutApiChangesTask : DefaultTask() {
  @get:InputFile abstract val changesFile: RegularFileProperty

  @get:OutputFile abstract val outputFile: RegularFileProperty

  @TaskAction
  fun add() {
    val diff = LinesChanged.fromFile(changesFile.asFile.get())

    val added = spoiler("APIs Added", diff.added.joinToString("\n\n") { it.trim() })
    val removed = spoiler("APIs Removed", diff.removed.joinToString("\n\n") { it.trim() })

    val message =
      when (diff.bump) {
        MAJOR ->
          """
                |You've removed things from the public API. This means your change will cause a
                |**major** version bump during release time. If you're okay with this, you can ignore
                |this message.
                | 
                |${removed.takeUnless { diff.removed.isEmpty() }.orEmpty()}
                |${added.takeUnless { diff.added.isEmpty() }.orEmpty()}
            """
            .trimMargin()
        MINOR ->
          """
                |You've added things to the public API. This means your change will cause a
                |**minor** version bump during release time. If you're okay with this, you can ignore
                |this message.
                | 
                |${added.takeUnless { diff.added.isEmpty() }.orEmpty()}
            """
            .trimMargin()
        else -> throw ir.saltech.gradle.util.SkipTask("No public api changes found")
      }

    outputFile.asFile.get().writeText(message)
  }
}
