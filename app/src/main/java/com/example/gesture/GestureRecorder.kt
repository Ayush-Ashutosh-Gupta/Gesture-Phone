package com.example.gesture

import android.content.Context
import android.content.SharedPreferences
import com.example.Utils
import com.example.model.LandmarkPoint
import org.json.JSONArray
import org.json.JSONObject

data class RecordedGesture(
    val id: String,
    val name: String,
    val actionName: String,
    val landmarks: List<LandmarkPoint>,
    val timestamp: Long = System.currentTimeMillis()
)

class GestureRecorder(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val customGestures = mutableListOf<RecordedGesture>()

    init {
        loadRecordedGestures()
    }

    private fun loadRecordedGestures() {
        customGestures.clear()
        val jsonString = prefs.getString(KEY_GESTURES, null) ?: return
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.getString("id")
                val name = obj.getString("name")
                val actionName = obj.optString("actionName", "Custom Action")
                val timestamp = obj.optLong("timestamp", 0L)
                val landmarksArr = obj.getJSONArray("landmarks")
                val landmarks = mutableListOf<LandmarkPoint>()
                for (j in 0 until landmarksArr.length()) {
                    val lmObj = landmarksArr.getJSONObject(j)
                    landmarks.add(
                        LandmarkPoint(
                            lmObj.getDouble("x").toFloat(),
                            lmObj.getDouble("y").toFloat(),
                            lmObj.optDouble("z", 0.0).toFloat()
                        )
                    )
                }
                customGestures.add(RecordedGesture(id, name, actionName, landmarks, timestamp))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun saveGesture(name: String, actionName: String, landmarks: List<LandmarkPoint>): RecordedGesture {
        val newGesture = RecordedGesture(
            id = "gesture_${System.currentTimeMillis()}",
            name = name,
            actionName = actionName,
            landmarks = landmarks
        )
        customGestures.add(newGesture)
        persist()
        return newGesture
    }

    fun deleteGesture(id: String) {
        customGestures.removeAll { it.id == id }
        persist()
    }

    fun getAllCustomGestures(): List<RecordedGesture> {
        return customGestures.toList()
    }

    /**
     * Matches live landmarks against recorded templates using Procrustes/Cosine distance comparison
     */
    fun matchGesture(liveLandmarks: List<LandmarkPoint>, threshold: Float = 0.12f): RecordedGesture? {
        if (liveLandmarks.size < 21 || customGestures.isEmpty()) return null

        val liveWrist = liveLandmarks[0]
        var bestMatch: RecordedGesture? = null
        var minAvgDist = Float.MAX_VALUE

        for (template in customGestures) {
            if (template.landmarks.size < 21) continue
            val templateWrist = template.landmarks[0]

            var totalDist = 0f
            for (i in liveLandmarks.indices) {
                // Relative to wrist for translation invariance
                val lx = liveLandmarks[i].x - liveWrist.x
                val ly = liveLandmarks[i].y - liveWrist.y
                val tx = template.landmarks[i].x - templateWrist.x
                val ty = template.landmarks[i].y - templateWrist.y
                totalDist += Utils.distance2D(LandmarkPoint(lx, ly), LandmarkPoint(tx, ty))
            }
            val avgDist = totalDist / liveLandmarks.size

            if (avgDist < threshold && avgDist < minAvgDist) {
                minAvgDist = avgDist
                bestMatch = template
            }
        }

        return bestMatch
    }

    private fun persist() {
        val jsonArray = JSONArray()
        for (g in customGestures) {
            val obj = JSONObject()
            obj.put("id", g.id)
            obj.put("name", g.name)
            obj.put("actionName", g.actionName)
            obj.put("timestamp", g.timestamp)
            val lmArr = JSONArray()
            for (lm in g.landmarks) {
                val lmObj = JSONObject()
                lmObj.put("x", lm.x.toDouble())
                lmObj.put("y", lm.y.toDouble())
                lmObj.put("z", lm.z.toDouble())
                lmArr.put(lmObj)
            }
            obj.put("landmarks", lmArr)
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_GESTURES, jsonArray.toString()).apply()
    }

    companion object {
        private const val PREFS_NAME = "gesture_phone_recorder"
        private const val KEY_GESTURES = "recorded_gestures"
    }
}
