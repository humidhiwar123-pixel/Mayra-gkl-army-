package com.example.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MyraAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isServiceActive.value = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val currentApp = event.packageName?.toString() ?: ""
        _activePackage.value = currentApp

        // Keep last visible text elements updated for assistive context
        val root = rootInActiveWindow ?: return
        val texts = mutableListOf<String>()
        extractTextsFromNode(root, texts)
        _visibleScreenTexts.value = texts.take(30)
    }

    override fun onInterrupt() {
        _isServiceActive.value = false
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        _isServiceActive.value = false
    }

    private fun extractTextsFromNode(node: AccessibilityNodeInfo, list: MutableList<String>) {
        val text = node.text?.toString()
        val desc = node.contentDescription?.toString()
        if (!text.isNullOrBlank()) list.add(text)
        else if (!desc.isNullOrBlank()) list.add(desc)

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            extractTextsFromNode(child, list)
        }
    }

    fun clickNodeByText(targetText: String): Boolean {
        // Enforce safety: do not auto-click sensitive banking / password confirmation buttons
        val lower = targetText.lowercase()
        if (lower.contains("pay") || lower.contains("transfer") || lower.contains("pin") || lower.contains("otp")) {
            return false
        }

        val root = rootInActiveWindow ?: return false
        val nodes = root.findAccessibilityNodeInfosByText(targetText)
        for (node in nodes) {
            if (node.isClickable) {
                return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            var parent = node.parent
            while (parent != null) {
                if (parent.isClickable) {
                    return parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
                parent = parent.parent
            }
        }
        return false
    }

    fun inputTextToField(text: String): Boolean {
        // Enforce safety: check for password / financial inputs
        val root = rootInActiveWindow ?: return false
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return false

        if (focused.isPassword) {
            return false // Strict safety: NEVER type into password fields
        }

        val arguments = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        return focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
    }

    fun performScrollDown(): Boolean {
        val root = rootInActiveWindow ?: return false
        return root.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
    }

    fun performScrollUp(): Boolean {
        val root = rootInActiveWindow ?: return false
        return root.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
    }

    companion object {
        var instance: MyraAccessibilityService? = null
            private set

        private val _isServiceActive = MutableStateFlow(false)
        val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

        private val _activePackage = MutableStateFlow("")
        val activePackage: StateFlow<String> = _activePackage.asStateFlow()

        private val _visibleScreenTexts = MutableStateFlow<List<String>>(emptyList())
        val visibleScreenTexts: StateFlow<List<String>> = _visibleScreenTexts.asStateFlow()
    }
}
