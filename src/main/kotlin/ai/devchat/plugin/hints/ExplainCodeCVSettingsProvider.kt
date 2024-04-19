package ai.devchat.plugin.hints

import ai.devchat.common.DevChatBundle
import com.intellij.codeInsight.codeVision.settings.CodeVisionGroupSettingProvider

class ExplainCodeCVSettingsProvider : CodeVisionGroupSettingProvider {
  override val groupId: String
    get() = "explainCode"

  override val groupName: String
    get() = DevChatBundle.message("settings.code.vision.explainCode.name")

  override val description: String
    get() = DevChatBundle.message("settings.code.vision.explainCode.description")
}