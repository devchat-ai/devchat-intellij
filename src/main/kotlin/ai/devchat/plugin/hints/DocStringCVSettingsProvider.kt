package ai.devchat.plugin.hints

import ai.devchat.common.DevChatBundle
import com.intellij.codeInsight.codeVision.settings.CodeVisionGroupSettingProvider

class DocStringCVSettingsProvider : CodeVisionGroupSettingProvider {
  override val groupId: String
    get() = "docstring"

  override val groupName: String
    get() = DevChatBundle.message("settings.code.vision.docstring.name")

  override val description: String
    get() = DevChatBundle.message("settings.code.vision.docstring.description")
}