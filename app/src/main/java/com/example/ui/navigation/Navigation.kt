package com.example.ui.navigation

sealed class Screen(val route: String, val title: String) {
    object Home : Screen("home", "Home")
    object Chat : Screen("chat", "Chat")
    object Voice : Screen("voice", "Voice")
    object AiTools : Screen("ai_tools", "AI Tools")
    object Settings : Screen("settings", "Settings")

    // Specialized Feature Hubs
    object Communication : Screen("communication", "Communication")
    object Calls : Screen("calls", "Phone & Calls")
    object Media : Screen("media", "Media Control")
    object DeviceTools : Screen("device_tools", "Device Tools")
    object FileManager : Screen("file_manager", "Files")
    object Photos : Screen("photos", "Photos & Vision")
    object DeepResearch : Screen("deep_research", "Deep Research")
    object Automations : Screen("automations", "Automations")
    object Maps : Screen("maps", "My World / Maps")
    object PcConnect : Screen("pc_connect", "PC Connect")
    object Connectors : Screen("connectors", "Connectors")
    object Notifications : Screen("notifications", "Notifications")
    object ScreenAutomation : Screen("screen_automation", "Screen Automation")
    object Security : Screen("security", "Security & Permissions")
    object Memory : Screen("memory", "Memory")
    object Developer : Screen("developer", "Developer Hub")
    object About : Screen("about", "About MYRA")
}
