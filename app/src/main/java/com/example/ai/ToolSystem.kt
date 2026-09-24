package com.example.ai

data class ToolParameter(
    val name: String,
    val type: String, // "STRING", "INTEGER", "BOOLEAN"
    val description: String,
    val required: Boolean = true
)

data class ToolDefinition(
    val name: String,
    val description: String,
    val parameters: List<ToolParameter>,
    val requiresConfirmation: Boolean = false,
    val confirmationPrompt: String? = null,
    val requiredPermissions: List<String> = emptyList()
)

data class PendingToolCall(
    val toolName: String,
    val arguments: Map<String, String>,
    val prompt: String
)

sealed class ToolResult {
    data class Success(val message: String, val details: String? = null) : ToolResult()
    data class Error(val errorMessage: String, val recoverySuggestion: String? = null) : ToolResult()
    data class RequiresConfirmation(
        val title: String,
        val details: String,
        val pendingCall: PendingToolCall
    ) : ToolResult()
    data class MissingPermission(
        val permissions: List<String>,
        val reason: String
    ) : ToolResult()
}
