package ai.devchat.idea;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.CredentialAttributesKt;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;

public class SensitiveDataStorage {

    private static final String KEY_NAME = "USER_KEY";
    private static final String SUBSYSTEM = "ai.devchat.idea";

    private static CredentialAttributes createCredentialAttributes() {
        return new CredentialAttributes(
                CredentialAttributesKt.generateServiceName(SUBSYSTEM, KEY_NAME));
    }

    public static void setKey(String key) {
        CredentialAttributes attributes = createCredentialAttributes();
        Credentials credentials = new Credentials("default", key);
        PasswordSafe.getInstance().set(attributes, credentials);
    }

    public static String getKey() {
        CredentialAttributes attributes = createCredentialAttributes();
        return PasswordSafe.getInstance().getPassword(attributes);
    }
}
