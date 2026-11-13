package com.b8.movietracker.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/**
 * 全局唯一的 Preferences DataStore 实例。
 *
 * 为什么用顶层属性委托：
 *   DataStore 官方要求「同一文件在进程内只能有一个实例」，否则会抛
 *   "There are multiple DataStores active for the same file"。
 *   用 `by preferencesDataStore(...)` 的顶层扩展属性即可保证单例，
 *   再由 Hilt 的 DataStoreModule 把它注入到仓库里。
 *
 * 为什么不用 SharedPreferences：
 *   1. DataStore 基于协程 + Flow，读写天然异步，不会阻塞主线程；
 *   2. 写入是事务性的（edit 内部原子提交），不会出现半写坏文件；
 *   3. 支持把配置以 Flow 形式暴露，界面上"改一处、全局实时生效"。
 */
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "b8_settings",
)