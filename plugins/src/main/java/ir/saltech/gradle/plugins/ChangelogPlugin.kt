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

package ir.saltech.gradle.plugins

import ir.saltech.gradle.tasks.FindChangesTask
import ir.saltech.gradle.tasks.MakeChangeTask
import ir.saltech.gradle.tasks.MakeReleaseNotesTask
import ir.saltech.gradle.tasks.WarnAboutApiChangesTask
import ir.saltech.gradle.tasks.WarnVersionBumpTask
import ir.saltech.gradle.types.Changelog
import ir.saltech.gradle.types.RandomWordsGenerator
import ir.saltech.gradle.util.buildDir
import ir.saltech.gradle.util.childFile
import ir.saltech.gradle.util.file
import ir.saltech.gradle.util.moduleVersion
import ir.saltech.gradle.util.orElseIfNotExists
import ir.saltech.gradle.util.outputFile
import ir.saltech.gradle.util.provideProperty
import ir.saltech.gradle.util.regularOutputFile
import ir.saltech.gradle.util.tempFile
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.Optional
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.support.listFilesOrdered

/**
 * A Gradle plugin for managing and creating [Changelog] files.
 *
 * By default, a subdirectory under the root `.changes` directory that corresponds to the
 * [project name][Project.getName] will be used to save change files.
 *
 * Will also register the [ApiPlugin] if it's not already present, as it will handle the actual
 * generation of the API files.
 *
 * Registers the following tasks:
 * - `findChanges`
 * - `makeChange`
 * - `warnAboutApiChanges`
 * - `deleteChangeFiles`
 * - `makeReleaseNotes`
 *
 * You can learn more about these tasks by visiting their respective docs below.
 *
 * @see ChangelogPluginExtension
 * @see FindChangesTask
 * @see MakeChangeTask
 * @see WarnAboutApiChangesTask
 * @see MakeReleaseNotesTask
 */
abstract class ChangelogPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    with(project) {
      val extension =
        extensions.create<ChangelogPluginExtension>("changelog").apply { commonConfiguration() }

      val releasedApiFile = apiPlugin.exportFile.orElseIfNotExists(apiPlugin.apiFile)
      val newApiFile = tasks.named("buildApi").regularOutputFile

      val findChanges =
        tasks.register<FindChangesTask>("findChanges") {
          old.set(releasedApiFile)
          new.set(newApiFile)
          outputFile.set(tempFile("changes"))
        }

      val fileChanges = findChanges.regularOutputFile

      tasks.register<MakeChangeTask>("makeChange") {
        val changeMessage = provideProperty<String>("changeMessage")
        val changeName = provideProperty<String>("changeName")
          .orElse(RandomWordsGenerator.generateString())
        val changeOutput = extension.outputDirectory.childFile("${changeName.get()}.json")

        changesFile.set(fileChanges)
        message.set(changeMessage)
        outputFile.set(changeOutput)
      }

      tasks.register<WarnVersionBumpTask>("warnVersionBump") {
        if (export.isPresent) {
          export.set(apiPlugin.exportFile)
        }
        changesFile.set(fileChanges)
      }

      tasks.register<WarnAboutApiChangesTask>("warnAboutApiChanges") {
        changesFile.set(fileChanges)
        outputFile.set(extension.apiChangesFile)
      }

      val changelogFiles =
        extension.outputDirectory.map { it.asFile.listFilesOrdered { it2 -> it2.extension == "json" } }

      tasks.register<Delete>("deleteChangeFiles") {
        group = "cleanup"

        delete(changelogFiles)
      }

      tasks.register<MakeReleaseNotesTask>("makeReleaseNotes") {
        onlyIf("No changelog files found") { changelogFiles.get().isNotEmpty() }

        changeFiles.set(changelogFiles)
        version.set(project.moduleVersion)
        outputFile.set(extension.releaseNotesFile)
      }
    }
  }

  context(Project)
  private fun ChangelogPluginExtension.commonConfiguration() {
    outputDirectory.convention(rootProject.layout.file(".changes/${project.name}"))
    releaseNotesFile.convention(rootProject.buildDir("release_notes/${project.name}.md"))
    apiChangesFile.convention(rootProject.buildDir("api_changes/${project.name}.md"))
  }
}

/**
 * Extension properties for the [ChangelogPlugin].
 *
 * @property outputDirectory The directory into which to store the [Changelog] files
 * @property releaseNotesFile The file into which to save the release notes to
 * @property apiChangesFile The file into which to save the api changes warning to
 */
abstract class ChangelogPluginExtension {
  @get:Optional abstract val outputDirectory: RegularFileProperty
  @get:Optional abstract val releaseNotesFile: RegularFileProperty
  @get:Optional abstract val apiChangesFile: RegularFileProperty
}
