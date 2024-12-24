package com.alibaba.ams.emas.demo
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
object BatchResolveCacheHolder {
    var batchResolveV4List: MutableList<String> = mutableListOf()
    var batchResolveV6List: MutableList<String> = mutableListOf()
    var batchResolveBothList: MutableList<String> = mutableListOf()
    var batchResolveAutoList: MutableList<String> = mutableListOf()
    fun convertBatchResolveCacheData(cacheData: String?) {
        if (cacheData == null) {
            batchResolveBothList.add("www.baidu.com")
            batchResolveBothList.add("m.baidu.com")
            batchResolveBothList.add("www.aliyun.com")
            batchResolveBothList.add("www.taobao.com")
            batchResolveBothList.add("www.163.com")
            batchResolveBothList.add("www.sohu.com")
            batchResolveBothList.add("www.sina.com.cn")
            batchResolveBothList.add("www.douyin.com")
            batchResolveBothList.add("www.qq.com")
            batchResolveBothList.add("www.chinaamc.com")
            batchResolveBothList.add("m.chinaamc.com")
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
                    batchResolveV4List.add(0, v4Array.getString(length))
                    --length
                }
            }
            if (v6Array != null) {
                var length = v6Array.length()
                --length
                while (length >= 0) {
                    batchResolveV6List.add(0, v6Array.getString(length))
                    --length
                }
            }
            if (bothArray != null) {
                var length = bothArray.length()
                --length
                while (length >= 0) {
                    batchResolveBothList.add(0, bothArray.getString(length))
                    --length
                }
            }
            if (autoArray != null) {
                var length = autoArray.length()
                --length
                while (length >= 0) {
                    batchResolveAutoList.add(0, autoArray.getString(length))
                    --length
                }
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }
    fun convertBatchResolveString(): String {
        val jsonObject = JSONObject()
        val v4Array = JSONArray()
        val v6Array = JSONArray()
        val bothArray = JSONArray()
        val autoArray = JSONArray()
        for (host in batchResolveV4List) {
            v4Array.put(host)
        }
        jsonObject.put("v4", v4Array)
        for (host in batchResolveV6List) {
            v6Array.put(host)
        }
        jsonObject.put("v6", v6Array)
        for (host in batchResolveBothList) {
            bothArray.put(host)
        }
        jsonObject.put("both", bothArray)
        for (host in batchResolveAutoList) {
            autoArray.put(host)
        }
        jsonObject.put("auto", autoArray)
        return jsonObject.toString()
    }
}