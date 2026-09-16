# Codebase Map

This document outlines the directory structure and the responsibilities of each file within the project.

```
/
├── .github/
│   └── workflows/
│       └── android.yml                              # GitHub Actions CI workflow to test, build APK, and automatically publish GitHub Releases
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml                  # App manifest declaring activity, theme, and FileProvider
│   │   │   ├── java/com/example/
│   │   │   │   ├── MainActivity.kt                  # Root activity hosting single-activity Jetpack Compose flow
│   │   │   │   ├── data/
│   │   │   │   │   ├── local/
│   │   │   │   │   │   ├── AppDatabase.kt           # Room Database definition with destructive migration fallback
│   │   │   │   │   │   └── Daos.kt                  # Room DAOs for tasks, logs, notes, and preferences
│   │   │   │   │   ├── model/
│   │   │   │   │   │   └── Entities.kt              # Room entities & enums (TaskScope, StartOfWeek, ThemeMode)
│   │   │   │   │   └── repository/
│   │   │   │   │       └── TaskRepository.kt        # Repository abstraction, implementation, and backup import logic
│   │   │   │   ├── domain/
│   │   │   │   │   ├── date/
│   │   │   │   │   │   └── PeriodCalculator.kt      # Temporal arithmetic (Daily, Weekly, Monthly, Yearly)
│   │   │   │   │   └── model/
│   │   │   │   │       └── DomainModels.kt          # UI domain models (TaskWithProgress, PeriodSummary)
│   │   │   │   ├── export/
│   │   │   │   │   ├── JsonBackupFormatter.kt       # JSON backup export and import parser
│   │   │   │   │   └── ReportExporter.kt            # Android Intent sharing with FileProvider
│   │   │   │   └── ui/
│   │   │   │       ├── components/
│   │   │   │       │   ├── ImportTasksDialog.kt     # Modal dialog for importing non-duplicate tasks from previous period
│   │   │   │       │   ├── OnboardingDialog.kt      # First-launch starter setup dialog
│   │   │   │       │   ├── PeriodNavigator.kt       # Scope tab switcher and period date navigator
│   │   │   │       │   ├── PeriodPerformanceBanner.kt# Completion ring and period star stats banner
│   │   │   │       │   ├── ScopedNotesSection.kt    # Scoped scratchpad notes component with updated placeholder
│   │   │   │       │   ├── StarRatingBar.kt         # Silver to Gold interactive star rating bar
│   │   │   │       │   ├── TaskEditDialog.kt        # Modal dialog for creating and editing tasks
│   │   │   │       │   ├── TaskItemCard.kt          # Task row card with reorder controls and star steppers
│   │   │   │       │   └── TrendLineGraph.kt        # Bézier completion trend line graph with interactive nodes
│   │   │   │       ├── screens/
│   │   │   │       │   ├── main/
│   │   │   │       │   │   ├── AppBottomNav.kt      # Three-tab bottom navigation (Tasks, Reports, Settings)
│   │   │   │       │   │   ├── MainScreen.kt        # Primary Tasks screen with FAB and no header
│   │   │   │       │   │   └── MainViewModel.kt     # ViewModel managing tasks, star logging, and periods
│   │   │   │       │   ├── reports/
│   │   │   │       │   │   ├── ReportsScreen.kt     # Performance analytics screen featuring 4-view line graph
│   │   │   │       │   │   └── ReportsViewModel.kt  # ViewModel for trend series (Days, Weeks, Months, Years)
│   │   │   │       │   └── settings/
│   │   │   │       │       └── SettingsScreen.kt    # Backup & Restore, appearance, week start, and defaults reset
│   │   │   │       └── theme/
│   │   │   │           ├── Color.kt                 # Theme palette including brand crimson (#750000)
│   │   │   │           ├── Theme.kt                 # Material 3 dark/light dynamic theme setup
│   │   │   │           └── Type.kt                  # Typography scale
│   │   │   └── res/
│   │   │       ├── drawable/                        # Vector assets and adaptive icon layers
│   │   │       ├── mipmap-anydpi-v26/               # Adaptive launcher icon XMLs
│   │   │       ├── values/                          # Strings, colours, and styles XML
│   │   │       └── xml/                             # FileProvider path definitions and backup rules
│   │   └── test/java/com/example/                   # Unit, Robolectric, and Roborazzi tests
│   └── build.gradle.kts                             # App module build configuration
├── build.gradle.kts                                 # Root project build configuration
├── LICENSE                                          # GNU General Public License v3.0 (GPLv3)
├── README.md                                        # Public repository overview and author documentation
├── usage.md                                         # Detailed user workflows and UI documentation
├── map.md                                           # Architectural repository structure and file map
└── settings.gradle.kts                              # Settings build script
```
