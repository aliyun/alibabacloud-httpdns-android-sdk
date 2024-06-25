package com.alibaba.ams.emas.demo

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

object PreResolveCacheHolder {
    var preResolveV4List: MutableList<String> = mutableListOf()
    var preResolveV6List: MutableList<String> = mutableListOf()
    var preResolveBothList: MutableList<String> = mutableListOf()
    var preResolveAutoList: MutableList<String> = mutableListOf()

    fun convertPreResolveCacheData(cacheData: String?) {
        if (cacheData == null) {
            return
        }
        try {
            val jsonObject = JSONObject(cacheData)
            val v4Array = jsonObject.optJSONArray("v4")
            val v6Array = jsonObject.optJSONArray("v6")
            val bothArray = jsonObject.optJSONArray("both")
            val autoArray = jsonObject.optJSONArray("auto")

            if (v4Array != null) {
                var length = v4Array.length()
                --length
                while (length >= 0) {
                    preResolveV4List.add(0, v4Array.getString(length))
                    --length
                }
            }

            if (v6Array != null) {
                var length = v6Array.length()
                --length
                while (length >= 0) {
                    preResolveV6List.add(0, v6Array.getString(length))
                    --length
                }
            }

            if (bothArray != null) {
                var length = bothArray.length()
                --length
                while (length >= 0) {
                    preResolveBothList.add(0, bothArray.getString(length))
                    --length
                }
            }

            if (autoArray != null) {
                var length = autoArray.length()
                --length
                while (length >= 0) {
                    preResolveAutoList.add(0, autoArray.getString(length))
                    --length
                }
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun convertPreResolveString(): String {
        val jsonObject = JSONObject()
        val v4Array = JSONArray()
        val v6Array = JSONArray()
        val bothArray = JSONArray()
        val autoArray = JSONArray()
        for (host in preResolveV4List) {
            v4Array.put(host)
        }
        jsonObject.put("v4", v4Array)

        for (host in preResolveV6List) {
            v6Array.put(host)
        }
        jsonObject.put("v6", v6Array)

        for (host in preResolveBothList) {
            bothArray.put(host)
        }
        jsonObject.put("both", bothArray)

        for (host in preResolveAutoList) {
            autoArray.put(host)
        }
        jsonObject.put("auto", autoArray)

        return jsonObject.toString()
    }
}