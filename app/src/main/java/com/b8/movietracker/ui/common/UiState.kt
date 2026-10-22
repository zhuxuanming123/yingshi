package com.b8.movietracker.ui.common

/**
 * 统一的 UI 状态封装（任务3：加载态 / 空态 / 错误态）。
 *
 * 为什么用 sealed interface 而不是 enum？
 *   1. 穷举性：when 分支必须覆盖所有子类，编译器会强制校验，不易漏写状态
 *   2. 类型安全：Success 能携带泛型数据 T（如 List<Movie>），其余状态不携带
 *   3. 可扩展：新增状态只需加一个子类
 *
 * 泛型 <out T>：协变，Success<List<Movie>> 可安全赋值给 UiState<Any> 等父类型。
 */
sealed interface UiState<out T> {

    /** 加载中 */
    data object Loading : UiState<Nothing>

    /** 加载成功，携带数据 */
    data class Success<T>(val data: T) : UiState<T>

    /** 加载成功但数据为空 */
    data object Empty : UiState<Nothing>

    /** 加载失败，携带错误信息 */
    data class Error(val message: String) : UiState<Nothing>
}
