package ai.devchat.plugin.hints

import ai.devchat.common.DevChatBundle
import com.intellij.codeInsight.codeVision.settings.CodeVisionGroupSettingProvider

class UnitTestsCVSettingsProvider : CodeVisionGroupSettingProvider {
  override val groupId: String
    get() = "unitTests"

  override val groupName: String
    get() = DevChatBundle.message("settings.code.vision.unitTests.name")

  override val description: String
    get() = DevChatBundle.message("settings.code.vision.unitTests.description")
}