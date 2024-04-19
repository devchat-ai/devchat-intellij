package ai.devchat.common

import com.intellij.AbstractBundle
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

const val BUNDLE = "messages.DevChatBundle"

object DevChatBundle : AbstractBundle(BUNDLE) {
    @JvmStatic fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String {
        return DevChatBundle.getMessage(key, *params)
    }
}