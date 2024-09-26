package ai.devchat.common

object Constants {
    val ASSISTANT_NAME_ZH = DevChatBundle.message("assistant.name.zh")
    val ASSISTANT_NAME_EN = DevChatBundle.message("assistant.name.en")
    val FUNC_TYPE_NAMES: Set<String> = setOf(
        "FUN", // Kotlin
        "METHOD", // Java
        "FUNCTION_DEFINITION", // C, C++
        "Py:FUNCTION_DECLARATION", // Python
        "FUNCTION_DECLARATION", "METHOD_DECLARATION", // Golang
        "JS:FUNCTION_DECLARATION", "JS:FUNCTION_EXPRESSION", // JS
        "JS:TYPESCRIPT_FUNCTION", "JS:TYPESCRIPT_FUNCTION_EXPRESSION",  // TS
        "CLASS_METHOD", // PHP
        "FUNCTION", // PHP, Rust
        "Ruby:METHOD", // Ruby
    )
    val CALL_EXPRESSION_ELEMENT_TYPE_NAMES: Set<String> = setOf(
        "CALL_EXPRESSION", // Kotlin, C, C++, Python
        "METHOD_CALL_EXPRESSION", // Java
        "CALL_EXPR", // Go, Rust
        "JS_CALL_EXPRESSION", // JS
        "TS_CALL_EXPRESSION", // TS
        "PHP_METHOD_REFERENCE", // PHP
        "CALL", // Ruby
    )
}