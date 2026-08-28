package com.example.gesture

import android.content.Context
import android.content.SharedPreferences
import com.example.model.GestureAction
import com.example.model.GestureType
import com.example.model.MediaAction
import com.example.model.SystemActionType
import org.json.JSONObject

class GestureMapper(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Current mapping in memory
    private val mappingMap = mutableMapOf<GestureType, GestureAction>()

    init {
        loadMappings()
    }

    private fun loadMappings() {
        mappingMap.clear()
        // Load defaults
        mappingMap[GestureType.CURSOR_POINT] = GestureAction.None
        mappingMap[GestureType.PINCH_CLICK] = GestureAction.Click(isDouble = false)
        mappingMap[GestureType.FIST_HOLD] = GestureAction.MediaControl(MediaAction.PLAY_PAUSE)
        mappingMap[GestureType.SWIPE_UP] = GestureAction.Swipe(com.example.model.SwipeDirection.UP)
        mappingMap[GestureType.SWIPE_DOWN] = GestureAction.Swipe(com.example.model.SwipeDirection.DOWN)
        mappingMap[GestureType.SWIPE_LEFT] = GestureAction.MediaControl(MediaAction.NEXT_TRACK)
        mappingMap[GestureType.SWIPE_RIGHT] = GestureAction.MediaControl(MediaAction.PREV_TRACK)
        mappingMap[GestureType.PEACE_SIGN] = GestureAction.SystemAction(SystemActionType.ESCAPE_MODE)
        mappingMap[GestureType.ROCK_ON] = GestureAction.SystemAction(SystemActionType.FLASHLIGHT)
        mappingMap[GestureType.THUMBS_UP] = GestureAction.MediaControl(MediaAction.VOLUME_UP)
        mappingMap[GestureType.THUMBS_DOWN] = GestureAction.MediaControl(MediaAction.VOLUME_DOWN)
        mappingMap[GestureType.OK_SIGN] = GestureAction.Custom("Open YouTube", "https://youtube.com")
        mappingMap[GestureType.VOLUME_SLIDE] = GestureAction.VolumeControl(0.5f, 0f)

        // Read overrides from SharedPreferences
        for (gesture in GestureType.values()) {
            val key = "gesture_${gesture.name}"
            if (prefs.contains(key)) {
                val savedAction = prefs.getString(key, null)
                if (savedAction != null) {
                    val parsed = parseActionString(savedAction)
                    if (parsed != null) {
                        mappingMap[gesture] = parsed
                    }
                }
            }
        }
    }

    fun setMapping(gesture: GestureType, action: GestureAction) {
        mappingMap[gesture] = action
        val serialized = serializeAction(action)
        prefs.edit().putString("gesture_${gesture.name}", serialized).apply()
    }

    fun getAction(gesture: GestureType): GestureAction {
        return mappingMap[gesture] ?: GestureAction.None
    }

    fun getAllMappings(): Map<GestureType, GestureAction> {
        return mappingMap.toMap()
    }

    fun resetToDefaults() {
        prefs.edit().clear().apply()
        loadMappings()
    }

    fun exportToJson(): String {
        val root = JSONObject()
        for ((gesture, action) in mappingMap) {
            root.put(gesture.name, serializeAction(action))
        }
        return root.toString(2)
    }

    fun importFromJson(jsonString: String): Boolean {
        return try {
            val root = JSONObject(jsonString)
            val editor = prefs.edit()
            for (gesture in GestureType.values()) {
                if (root.has(gesture.name)) {
                    val actionStr = root.getString(gesture.name)
                    val parsed = parseActionString(actionStr)
                    if (parsed != null) {
                        mappingMap[gesture] = parsed
                        editor.putString("gesture_${gesture.name}", actionStr)
                    }
                }
            }
            editor.apply()
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun serializeAction(action: GestureAction): String {
        return when (action) {
            is GestureAction.Click -> "CLICK:${action.isDouble}"
            is GestureAction.Hold -> "HOLD:${action.isHolding}"
            is GestureAction.Swipe -> "SWIPE:${action.direction.name}"
            is GestureAction.MediaControl -> "MEDIA:${action.action.name}"
            is GestureAction.VolumeControl -> "VOLUME:${action.levelPercent}"
            is GestureAction.SystemAction -> "SYSTEM:${action.type.name}"
            is GestureAction.Custom -> "CUSTOM:${action.name}|${action.payload}"
            is GestureAction.MouseMove -> "MOUSE:${action.x},${action.y}"
            is GestureAction.BrightnessControl -> "BRIGHTNESS:${action.levelPercent}"
            GestureAction.None -> "NONE"
        }
    }

    private fun parseActionString(str: String): GestureAction? {
        return try {
            val parts = str.split(":", limit = 2)
            val type = parts[0]
            val value = if (parts.size > 1) parts[1] else ""

            when (type) {
                "CLICK" -> GestureAction.Click(value.toBoolean())
                "HOLD" -> GestureAction.Hold(value.toBoolean())
                "SWIPE" -> GestureAction.Swipe(com.example.model.SwipeDirection.valueOf(value))
                "MEDIA" -> GestureAction.MediaControl(MediaAction.valueOf(value))
                "VOLUME" -> GestureAction.VolumeControl(value.toFloatOrNull() ?: 0.5f, 0f)
                "SYSTEM" -> GestureAction.SystemAction(SystemActionType.valueOf(value))
                "CUSTOM" -> {
                    val sub = value.split("|", limit = 2)
                    GestureAction.Custom(sub[0], if (sub.size > 1) sub[1] else "")
                }
                "NONE" -> GestureAction.None
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        private const val PREFS_NAME = "gesture_phone_mappings"
    }
}
