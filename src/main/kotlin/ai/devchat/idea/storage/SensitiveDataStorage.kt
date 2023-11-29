package ai.devchat.idea.storage

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe

object SensitiveDataStorage {
    private const val KEY_NAME = "USER_KEY"
    private const val SUBSYSTEM = "ai.devchat.idea"
    private fun createCredentialAttributes(): CredentialAttributes {
        return CredentialAttributes(
            generateServiceName(SUBSYSTEM, KEY_NAME)
        )
    }

    var key: String?
        get() {
            val attributes = createCredentialAttributes()
            return PasswordSafe.instance.getPassword(attributes)
        }
        set(key) {
            val attributes = createCredentialAttributes()
            val credentials = Credentials("default", key)
            PasswordSafe.instance.set(attributes, credentials)
        }
}
