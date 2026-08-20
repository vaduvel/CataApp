package com.emanus.lucrari.ui.nav

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.emanus.lucrari.R
import com.emanus.lucrari.ui.screen.CalendarScreen
import com.emanus.lucrari.ui.screen.ClientsScreen
import com.emanus.lucrari.ui.screen.DescrizioneScreen
import com.emanus.lucrari.ui.screen.JobDetailScreen
import com.emanus.lucrari.ui.screen.JobMoneyScreen
import com.emanus.lucrari.ui.screen.JobsScreen
import com.emanus.lucrari.ui.screen.MoneyScreen
import com.emanus.lucrari.ui.screen.MoreScreen
import com.emanus.lucrari.ui.screen.PhotosScreen
import com.emanus.lucrari.ui.screen.PunchScreen
import com.emanus.lucrari.ui.screen.TodayScreen
import com.emanus.lucrari.ui.theme.Dimens

/** Cele 5 destinații din bara de jos (SPEC §6). */
enum class Tab(val route: String, @StringRes val labelRes: Int, val icon: ImageVector) {
	TODAY("today", R.string.tab_today, Icons.Outlined.Today),
	JOBS("jobs", R.string.tab_jobs, Icons.Outlined.Construction),
	PUNCH("punch", R.string.tab_punch, Icons.Outlined.Checklist),
	MONEY("money", R.string.tab_money, Icons.Outlined.Payments),
	MORE("more", R.string.tab_more, Icons.Outlined.MoreHoriz),
}

const val ROUTE_JOB_DETAIL = "job/{id}"
const val ROUTE_JOB_MONEY = "money/{id}"
const val ROUTE_DESCRIZIONE = "descrizione/{id}"
const val ROUTE_CLIENTS = "clients"
const val ROUTE_PHOTOS = "photos"
const val ROUTE_CALENDAR = "calendar"

@Composable
fun AppRoot() {
	val navController = rememberNavController()
	val backStackEntry by navController.currentBackStackEntryAsState()
	val route = backStackEntry?.destination?.route

	Scaffold(
		containerColor = MaterialTheme.colorScheme.background,
		bottomBar = {
			NavigationBar(
				containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
				contentColor = MaterialTheme.colorScheme.onSurface,
			) {
				Tab.entries.forEach { tab ->
					val selected = isSelected(tab, route)
					NavigationBarItem(
						selected = selected,
						onClick = {
							navController.navigate(tab.route) {
								popUpTo(navController.graph.startDestinationId) { saveState = true }
								launchSingleTop = true
								restoreState = true
							}
						},
						icon = {
							Column(horizontalAlignment = Alignment.CenterHorizontally) {
								Box(
									modifier = Modifier
										.width(Dimens.navigationBrandMarkWidth)
										.height(Dimens.navigationBrandMarkHeight)
										.clip(RoundedCornerShape(50))
										.background(
											if (selected) MaterialTheme.colorScheme.tertiary else Color.Transparent,
										),
								)
								Spacer(Modifier.height(Dimens.space4))
								Icon(tab.icon, contentDescription = null)
							}
						},
						label = { Text(stringResource(tab.labelRes)) },
						colors = NavigationBarItemDefaults.colors(
							selectedIconColor = MaterialTheme.colorScheme.secondary,
							selectedTextColor = MaterialTheme.colorScheme.onSurface,
							indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
							unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
							unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
						),
					)
				}
			}
		},
	) { padding ->
		// Fiecare ecran din NavHost are propriul Scaffold. Simpla adăugare de padding nu le
		// spune și că marginile de sistem au fost deja acoperite aici, așa că ele le adaugă a
		// doua oară: conținutul se scurta jos cu înălțimea barei de navigare (ultimul rând din
		// „Mai mult" ieșea din ecran). consumeWindowInsets marchează marginile drept consumate.
		NavHost(
			navController = navController,
			startDestination = Tab.TODAY.route,
			modifier = Modifier
				.padding(padding)
				.consumeWindowInsets(padding),
		) {
			composable(Tab.TODAY.route) {
				TodayScreen(onOpenJob = { id -> navController.navigate("job/" + id) })
			}
			composable(Tab.JOBS.route) {
				JobsScreen(
					onOpenJob = { id -> navController.navigate("job/" + id) },
					onOpenCalendar = { navController.navigate(ROUTE_CALENDAR) },
				)
			}
			composable(Tab.PUNCH.route) {
				PunchScreen(onOpenJob = { id -> navController.navigate("job/" + id) })
			}
			composable(Tab.MONEY.route) {
				MoneyScreen(onOpenJobMoney = { id -> navController.navigate("money/" + id) })
			}
			composable(Tab.MORE.route) {
				MoreScreen(
					onOpenCalendar = { navController.navigate(ROUTE_CALENDAR) },
					onOpenClients = { navController.navigate(ROUTE_CLIENTS) },
					onOpenPhotos = { navController.navigate(ROUTE_PHOTOS) },
				)
			}
			composable(
				route = ROUTE_JOB_DETAIL,
				arguments = listOf(navArgument("id") { type = NavType.StringType }),
			) { entry ->
				JobDetailScreen(
					jobId = entry.arguments?.getString("id").orEmpty(),
					onBack = { navController.popBackStack() },
					onOpenMoney = { id -> navController.navigate("money/" + id) },
					onOpenDescrizione = { id -> navController.navigate("descrizione/" + id) },
				)
			}
			composable(
				route = ROUTE_JOB_MONEY,
				arguments = listOf(navArgument("id") { type = NavType.StringType }),
			) { entry ->
				JobMoneyScreen(
					jobId = entry.arguments?.getString("id").orEmpty(),
					onBack = { navController.popBackStack() },
				)
			}
			composable(
				route = ROUTE_DESCRIZIONE,
				arguments = listOf(navArgument("id") { type = NavType.StringType }),
			) { entry ->
				DescrizioneScreen(
					jobId = entry.arguments?.getString("id").orEmpty(),
					onBack = { navController.popBackStack() },
				)
			}
			composable(ROUTE_CLIENTS) {
				ClientsScreen(onBack = { navController.popBackStack() })
			}
			composable(ROUTE_PHOTOS) {
				PhotosScreen(onBack = { navController.popBackStack() })
			}
			composable(ROUTE_CALENDAR) {
				CalendarScreen(
					onBack = { navController.popBackStack() },
					onOpenJob = { id -> navController.navigate("job/" + id) },
				)
			}
		}
	}
}

/** Tabul rămâne aprins și când ești pe un ecran copil. */
private fun isSelected(tab: Tab, route: String?): Boolean = when {
	route == null -> false
	route == tab.route -> true
	tab == Tab.JOBS && route.startsWith("job/") -> true
	tab == Tab.JOBS && route.startsWith("descrizione/") -> true
	tab == Tab.JOBS && route == ROUTE_CALENDAR -> true
	tab == Tab.MONEY && route.startsWith("money/") -> true
	tab == Tab.MORE && (route == ROUTE_CLIENTS || route == ROUTE_PHOTOS) -> true
	else -> false
}
