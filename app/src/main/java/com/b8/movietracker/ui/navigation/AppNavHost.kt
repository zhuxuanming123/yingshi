package com.b8.movietracker.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.b8.movietracker.ui.detail.DetailScreen
import com.b8.movietracker.ui.home.HomeScreen
import com.b8.movietracker.ui.mylist.MyListScreen
import com.b8.movietracker.ui.search.SearchScreen

/**
 * 路由常量集中定义（避免手写字符串散落各处导致拼写错误）。
 */
object Routes {
    const val HOME = "home"                 // 首页
    const val SEARCH = "search"             // 搜索页
    const val MY_LIST = "my_list"           // 我的片单
    const val DETAIL = "detail/{movieId}"   // 详情页（带路径参数）

    /** 生成详情页路由，如 detail/3 */
    fun detail(movieId: Long): String = "detail/$movieId"
}

/**
 * 应用根组件：底部导航（3 个 Tab）+ NavHost 页面容器。
 *
 * @param initialMovieId 非空时表示"从通知点击进入"，启动后自动导航到对应详情页
 */
@Composable
fun MovieTrackerApp(initialMovieId: Long? = null) {
    val navController = rememberNavController()

    // 通知点击跳转：启动时若有 movieId，导航到详情页
    LaunchedEffect(Unit) {
        if (initialMovieId != null) {
            navController.navigate(Routes.detail(initialMovieId))
        }
    }

    // 监听当前导航栈顶，用于判断底部栏显示与选中项
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // 详情页不显示底部导航栏（只在三个主页显示）
    val showBottomBar = currentRoute in setOf(Routes.HOME, Routes.SEARCH, Routes.MY_LIST)

    // 底部 Tab 切换的通用逻辑：避免重复入栈，并保留各 Tab 的状态
    val navigateToTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == Routes.HOME,
                        onClick = { navigateToTab(Routes.HOME) },
                        icon = { Icon(Icons.Filled.Home, contentDescription = "首页") },
                        label = { Text("首页") },
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.SEARCH,
                        onClick = { navigateToTab(Routes.SEARCH) },
                        icon = { Icon(Icons.Filled.Search, contentDescription = "搜索") },
                        label = { Text("搜索") },
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.MY_LIST,
                        onClick = { navigateToTab(Routes.MY_LIST) },
                        icon = { Icon(Icons.Filled.List, contentDescription = "我的片单") },
                        label = { Text("我的片单") },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding),
        ) {
            // 首页
            composable(Routes.HOME) {
                HomeScreen(onMovieClick = { movieId ->
                    navController.navigate(Routes.detail(movieId))
                })
            }
            // 搜索页
            composable(Routes.SEARCH) {
                SearchScreen(onMovieClick = { movieId ->
                    navController.navigate(Routes.detail(movieId))
                })
            }
            // 我的片单
            composable(Routes.MY_LIST) {
                MyListScreen(onMovieClick = { movieId ->
                    navController.navigate(Routes.detail(movieId))
                })
            }
            // 详情页（列表 → 详情 导航，任务1 的核心要求）
            composable(
                route = Routes.DETAIL,
                arguments = listOf(navArgument("movieId") { type = NavType.LongType }),
            ) {
                DetailScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
