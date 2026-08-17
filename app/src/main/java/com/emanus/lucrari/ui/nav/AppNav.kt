package com.emanus.lucrari.ui.nav

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
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.emanus.lucrari.R
import com.emanus.lucrari.ui.screen.JobsScreen
import com.emanus.lucrari.ui.screen.MoneyScreen
import com.emanus.lucrari.ui.screen.MoreScreen
import com.emanus.lucrari.ui.screen.PunchScreen
import com.emanus.lucrari.ui.screen.TodayScreen

/** Cele cinci destinații din bara de jos. Ordinea e cea din SPEC §6. */
enum class Tab(val route: String, val labelRes: Int, val icon: ImageVector) {
	TODAY("today", R.string.tab_today, Icons.Outlined.Today),
	JOBS("jobs", R.string.tab_jobs, Icons.Outlined.Construction),
	PUNCH("punch", R.string.tab_punch, Icons.Outlined.Checklist),
	MONEY("money", R.string.tab_money, Icons.Outlined.Payments),
	MORE("more", R.string.tab_more, Icons.Outlined.MoreHoriz),
}

@Composable
fun AppRoot() {
	val navController = rememberNavController()
	val backStackEntry by navController.currentBackStackEntryAsState()
	val currentRoute = backStackEntry?.destination?.route

	Scaffold(
		bottomBar = {
			NavigationBar {
				Tab.entries.forEach { tab ->
					NavigationBarItem(
						selected = currentRoute == tab.route,
						onClick = {
							if (currentRoute != tab.route) {
								navController.navigate(tab.route) {
									popUpTo(navController.graph.findStartDestination().id) {
										saveState = true
									}
									launchSingleTop = true
									restoreState = true
								}
							}
						},
						icon = { Icon(tab.icon, contentDescription = null) },
						label = { Text(stringResource(tab.labelRes)) },
					)
				}
			}
		},
	) { innerPadding ->
		NavHost(
			navController = navController,
			startDestination = Tab.TODAY.route,
			modifier = Modifier.padding(innerPadding),
		) {
			composable(Tab.TODAY.route) { TodayScreen() }
			composable(Tab.JOBS.route) { JobsScreen() }
			composable(Tab.PUNCH.route) { PunchScreen() }
			composable(Tab.MONEY.route) { MoneyScreen() }
			composable(Tab.MORE.route) { MoreScreen() }
		}
	}
}
