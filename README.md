# SavingLy

App Android nativa para gestionar **gastos**, **ahorros** y **colecciones** (libros, Funkos, cartas MTG, videojuegos, etc.) con todo el almacenamiento local. Hecha con Jetpack Compose, Material 3, Room y Hilt.

## Stack

- **Kotlin 2.0** + **Jetpack Compose** + **Material 3** (con dynamic color)
- **Arquitectura:** MVVM + Clean Architecture ligera (`data` / `domain` / `ui`)
- **Persistencia:** Room (SQLite), 100% offline
- **DI:** Hilt
- **Navegación:** Navigation Compose
- **Gráficos:** Vico
- **Notificaciones:** WorkManager + NotificationCompat
- **Async:** Coroutines + Flow

## Funcionalidades v1

- **Gastos**: registro con categorías, método de pago, filtro por mes, total mensual
- **Ahorros**: metas con progreso visual, aportaciones individuales
- **Colecciones**: el corazón de la app — múltiples colecciones (Stephen King, Funkos, Master Set MTG, juegos…), cada ítem con estado `TENGO` / `QUIERO` / `RESERVADO` / `PRESTADO` / `VENDIDO`, condición, precio, rareza, favoritos
- **Estadísticas**: gasto anual, gráfico mensual con Vico, desglose por categoría
- **Recordatorios**: notificaciones programadas con WorkManager

## Cómo abrir el proyecto

### Opción A (recomendada): Android Studio

1. Abre **Android Studio Hedgehog (2023.1.1)** o más reciente
2. **File → Open** y selecciona la carpeta `MisFinanzas`
3. Cuando avise de que falta `gradle-wrapper.jar`, acepta que lo regenere, o ejecuta `gradle wrapper --gradle-version 8.9` desde un terminal con Gradle instalado
4. Sync Gradle, y a correr

### Opción B: Línea de comandos

Si tienes Gradle instalado globalmente:

```bash
cd MisFinanzas
gradle wrapper --gradle-version 8.9
./gradlew assembleDebug
```

## Requisitos

- **JDK 17**
- **Android SDK 34**
- **minSdk 26** (Android 8.0)

## Estructura

```
app/src/main/java/com/misfinanzas/app/
├── data/
│   ├── local/         # Room: DAOs, entidades, converters, BD
│   └── repository/    # Implementaciones de repositorios
├── domain/
│   ├── model/         # Modelos de dominio (Gasto, MetaAhorro, Coleccion…)
│   └── repository/    # Interfaces de repositorios
├── ui/
│   ├── theme/         # Material 3, colores, tipografía
│   ├── navigation/    # NavHost y rutas
│   ├── components/    # Composables reutilizables
│   └── screens/       # Pantallas (home, gastos, ahorros, colecciones…)
├── notifications/     # Helper + WorkManager Worker
├── di/                # Módulos Hilt
├── utils/             # Formateadores
├── MainActivity.kt
└── MisFinanzasApp.kt
```

## Datos por defecto

La BD arranca vacía. Puedes empezar a usar la app inmediatamente:

1. **Inicio** → resumen del mes
2. **Gastos** → pulsa **+** y crea tu primer gasto
3. **Ahorros** → crea una meta (ej. *"Master Set MTG Final Fantasy"*)
4. **Colecciones** → crea una colección, abre y añade ítems con estado *Tengo* o *Quiero*
5. **Estadísticas** → ver gráficos cuando haya datos
6. **⚙ Recordatorios** → configura notificaciones diarias

## Próximos pasos sugeridos

- Importación/exportación CSV de gastos
- Imágenes en ítems (usando Coil, ya incluido)
- Sincronización opcional con Drive/Firebase
- Widget en pantalla de inicio con resumen
- Modo presupuesto mensual con alertas
