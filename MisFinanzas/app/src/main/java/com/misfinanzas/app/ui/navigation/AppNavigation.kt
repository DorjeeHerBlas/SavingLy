package com.misfinanzas.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.misfinanzas.app.ui.screens.ahorros.AhorrosScreen
import com.misfinanzas.app.ui.screens.ahorros.DetalleMetaScreen
import com.misfinanzas.app.ui.screens.colecciones.ColeccionesScreen
import com.misfinanzas.app.ui.screens.colecciones.DetalleColeccionScreen
import com.misfinanzas.app.ui.screens.colecciones.DetalleItemScreen
import com.misfinanzas.app.ui.screens.compra.ListaCompraScreen
import com.misfinanzas.app.ui.screens.estadisticas.EstadisticasScreen
import com.misfinanzas.app.ui.screens.gastos.GastosScreen
import com.misfinanzas.app.ui.screens.home.HomeScreen
import com.misfinanzas.app.ui.screens.configuracion.ConfiguracionScreen

sealed class Ruta(val ruta: String) {
    data object Home : Ruta("home")
    data object Gastos : Ruta("gastos")
    data object Ahorros : Ruta("ahorros")
    data object Colecciones : Ruta("colecciones")
    data object Compra : Ruta("compra")
    data object Estadisticas : Ruta("estadisticas")
    data object Configuracion : Ruta("configuracion")

    data object DetalleMeta : Ruta("meta/{metaId}") {
        fun crearRuta(metaId: Long) = "meta/$metaId"
    }
    data object DetalleColeccion : Ruta("coleccion/{coleccionId}") {
        fun crearRuta(coleccionId: Long) = "coleccion/$coleccionId"
    }
    data object DetalleItem : Ruta("item/{coleccionId}/{itemId}") {
        fun crearRuta(coleccionId: Long, itemId: Long) = "item/$coleccionId/$itemId"
    }
}

private data class TabItem(
    val ruta: String,
    val labelResId: Int,
    val icono: ImageVector
)

private val tabs = listOf(
    TabItem(Ruta.Home.ruta, com.misfinanzas.app.R.string.tab_inicio, Icons.Outlined.Home),
    TabItem(Ruta.Gastos.ruta, com.misfinanzas.app.R.string.tab_gastos, Icons.Outlined.Receipt),
    TabItem(Ruta.Ahorros.ruta, com.misfinanzas.app.R.string.tab_ahorros, Icons.Outlined.Savings),
    TabItem(Ruta.Compra.ruta, com.misfinanzas.app.R.string.tab_compra, Icons.Outlined.ShoppingCart),
    TabItem(Ruta.Colecciones.ruta, com.misfinanzas.app.R.string.tab_colecciones, Icons.Outlined.Inventory2),
    TabItem(Ruta.Estadisticas.ruta, com.misfinanzas.app.R.string.tab_estadisticas, Icons.Outlined.QueryStats)
)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val rutaActual = backStack?.destination?.route

    val mostrarBottomBar = rutaActual in tabs.map { it.ruta } || rutaActual == Ruta.Configuracion.ruta

    Scaffold(
        bottomBar = {
            if (mostrarBottomBar) {
                NavigationBar {
                    tabs.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icono, contentDescription = stringResource(item.labelResId)) },
                            selected = backStack?.destination?.hierarchy?.any { it.route == item.ruta } == true,
                            onClick = {
                                if (rutaActual != item.ruta) {
                                    navController.navigate(item.ruta) {
                                        popUpTo(Ruta.Home.ruta) { saveState = false }
                                        launchSingleTop = true
                                        restoreState = false
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Ruta.Home.ruta,
            modifier = Modifier.padding(padding)
        ) {
            composable(Ruta.Home.ruta) {
                HomeScreen(
                    onIrAGastos = { navController.navigate(Ruta.Gastos.ruta) },
                    onIrAAhorros = { navController.navigate(Ruta.Ahorros.ruta) },
                    onIrAColecciones = { navController.navigate(Ruta.Colecciones.ruta) },
                    onIrAConfiguracion = { navController.navigate(Ruta.Configuracion.ruta) },
                    onAbrirMeta = { id -> navController.navigate(Ruta.DetalleMeta.crearRuta(id)) },
                    onAbrirColeccion = { id -> navController.navigate(Ruta.DetalleColeccion.crearRuta(id)) },
                    onAbrirItem = { coleccionId, itemId ->
                        navController.navigate(Ruta.DetalleItem.crearRuta(coleccionId, itemId))
                    }
                )
            }
            composable(Ruta.Gastos.ruta) { GastosScreen() }
            composable(Ruta.Ahorros.ruta) {
                AhorrosScreen(
                    onAbrirMeta = { id -> navController.navigate(Ruta.DetalleMeta.crearRuta(id)) }
                )
            }
            composable(Ruta.Colecciones.ruta) {
                ColeccionesScreen(
                    onAbrirColeccion = { id -> navController.navigate(Ruta.DetalleColeccion.crearRuta(id)) }
                )
            }
            composable(Ruta.Compra.ruta) { ListaCompraScreen() }
            composable(Ruta.Estadisticas.ruta) { EstadisticasScreen() }
            composable(Ruta.Configuracion.ruta) {
                ConfiguracionScreen(onVolver = { navController.popBackStack() })
            }
            composable(Ruta.DetalleMeta.ruta) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("metaId")?.toLongOrNull() ?: 0L
                DetalleMetaScreen(metaId = id, onVolver = { navController.popBackStack() })
            }
            composable(Ruta.DetalleColeccion.ruta) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("coleccionId")?.toLongOrNull() ?: 0L
                DetalleColeccionScreen(
                    coleccionId = id,
                    onVolver = { navController.popBackStack() },
                    onAbrirItem = { itemId ->
                        navController.navigate(Ruta.DetalleItem.crearRuta(id, itemId))
                    }
                )
            }
            composable(Ruta.DetalleItem.ruta) { backStackEntry ->
                val coleccionId = backStackEntry.arguments?.getString("coleccionId")?.toLongOrNull() ?: 0L
                val itemId = backStackEntry.arguments?.getString("itemId")?.toLongOrNull() ?: 0L
                DetalleItemScreen(
                    coleccionId = coleccionId,
                    itemId = itemId,
                    onVolver = { navController.popBackStack() }
                )
            }
        }
    }
}
