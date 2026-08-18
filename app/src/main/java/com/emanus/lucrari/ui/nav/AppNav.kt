package com.emanus.lucrari.ui.nav

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.emanus.lucrari.R
import com.emanus.lucrari.ui.screen.ClientsScreen
import com.emanus.lucrari.ui.screen.JobDetailScreen
import com.emanus.lucrari.ui.screen.JobsScreen
import com.emanus.lucrari.ui.screen.MoneyScreen
import com.emanus.lucrari.ui.screen.MoreScreen
import com.emanus.lucrari.ui.screen.PunchScreen
import com.emanus.lucrari.ui.screen.TodayScreen

/** Cele 5 destinații din bara de jos (SPEC §6). */
enum class Tab(val route: String, @StringRes val labelRes: Int, val icon: ImageVector) {
	TODAY("today", R.string.tab_today, Icons.Outlined.Today),
	JOBS("jobs", R.string.tab_jobs, Icons.Outlined.Construction),
	PUNCH("punch", R.string.tab_punch, Icons.Outlined.Checklist),
	MONEY("money", R.string.tab_money, Icons.Outlined.Payments),
	MORE("more", R.string.tab_more, Icons.Outlined.MoreHoriz),
}

const val ROUTE_JOB_DETAIL = "job/{id}"
const val ROUTE_CLIENTS = "clients"

@Composable
fun AppRoot() {
	val navController = rememberNavController()
	val backStackEntry by navController.currentBackStackEntryAsState()
	val route = backStackEntry?.destination?.route

	Scaffold(
		bottomBar = {
			NavigationBar {
				Tab.entries.forEach { tab ->
					NavigationBarItem(
						selected = isSelected(tab, route),
						onClick = {
							navController.navigate(tab.route) {
								popUpTo(navController.graph.startDestinationId) { saveState = true }
								launchSingleTop = true
								restoreState = true
							}
						},
						icon = { Icon(tab.icon, contentDescription = null) },
						label = { Text(stringResource(tab.labelRes)) },
					)
				}
			}
		},
	) { padding ->
		NavHost(
			navController = navController,
			startDestination = Tab.TODAY.route,
			modifier = Modifier.padding(padding),
		) {
			composable(Tab.TODAY.route) {
				TodayScreen(onOpenJob = { id -> navController.navigate("job/" + id) })
			}
			composable(Tab.JOBS.route) {
				JobsScreen(onOpenJob = { id -> navController.navigate("job/" + id) })
			}
			composable(Tab.PUNCH.route) { PunchScreen() }
			composable(Tab.MONEY.route) { MoneyScreen() }
			composable(Tab.MORE.route) {
				MoreScreen(onOpenClients = { navController.navigate(ROUTE_CLIENTS) })
			}
			composable(
				route = ROUTE_JOB_DETAIL,
				arguments = listOf(navArgument("id") { type = NavType.StringType }),
			) { entry ->
				JobDetailScreen(
					jobId = entry.arguments?.getString("id").orEmpty(),
					onBack = { navController.popBackStack() },
				)
			}
			composable(ROUTE_CLIENTS) {
				ClientsScreen(onBack = { navController.popBackStack() })
			}
		}
	}
}

/** Tabul rămâne aprins și când ești pe un ecran copil (detaliu lucrare, clienți). */
private fun isSelected(tab: Tab, route: String?): Boolean = when {
	route == null -> false
	route == tab.route -> true
	tab == Tab.JOBS && route.startsWith("job/") -> true
	tab == Tab.MORE && route == ROUTE_CLIENTS -> true
	else -> false
}
