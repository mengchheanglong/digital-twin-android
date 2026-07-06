package com.transcendiverse.digitaltwin

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherIntegrationContractTest {
    @Test
    fun launcherShellUsesOasisSpatialTwinHomeAndAppsPages() {
        val activity = source("app/src/main/java/com/transcendiverse/digitaltwin/launcher/LauncherActivity.kt")
        val screen = source("app/src/main/java/com/transcendiverse/digitaltwin/launcher/LauncherScreen.kt")

        assertTrue(activity.contains("SharedPreferencesTodayCacheStore"))
        assertTrue(activity.contains("SharedPreferencesTodaySettingsStore"))
        assertTrue(activity.contains("LauncherAppsRepository"))
        assertTrue(activity.contains("LauncherFavoritesStore"))
        assertTrue(activity.contains("favoriteLauncherApps"))
        assertTrue(activity.contains("LauncherTodaySummary.from"))
        assertTrue(activity.contains("TodaySyncScheduler.enqueueOneTime"))

        assertTrue(screen.contains("HorizontalPager"))
        assertTrue(screen.contains("val initialPage = if (startOnTwinPage) TWIN_PAGE_INDEX else HOME_PAGE_INDEX"))
        assertTrue(screen.contains("rememberPagerState(initialPage = initialPage"))
        assertTrue(screen.contains("TWIN_PAGE_INDEX -> TwinLauncherPage"))
        assertTrue(screen.contains("HOME_PAGE_INDEX -> LauncherHomePage"))
        assertTrue(screen.contains("APPS_PAGE_INDEX -> LauncherAppsPage"))
        assertTrue(screen.contains("// Page mapping: 0 = Twin, 1 = Home, 2 = Apps."))
        assertTrue(screen.contains("LauncherBackgroundColor"))
        assertTrue(screen.contains("TwinActiveProtocolCard"))
        assertTrue(screen.contains("MAX_HOME_FAVORITE_APPS = 5"))
        assertTrue(screen.contains("ACTIVE PROTOCOL"))
        assertTrue(screen.contains("LauncherGoogleSearchWidget"))
        assertTrue(screen.contains("Search with Google"))
        assertTrue(screen.contains("CompanionLaunchRequest(destination = CompanionDestination.Today)"))
        assertTrue(screen.contains("homeIntentNonce"))
        assertTrue(screen.contains("LauncherPageAnimationSpec"))

        assertFalse(screen.contains("LauncherHomeFooter("))
        assertFalse(screen.contains("LauncherTextAction(text = \"Apps\""))
        assertFalse(screen.contains("LauncherTextAction(text = \"Companion\""))
        assertFalse(screen.contains("LauncherTextAction(text = \"Settings\""))
        assertFalse(screen.contains("TwinOasisPanel"))
        assertFalse(screen.contains("TwinOasisRow"))
        assertFalse(screen.contains("LauncherFavoritesCard"))
        assertFalse(screen.contains("FavoriteAppShortcut"))
        assertFalse(screen.contains("text = app.packageName"))
        assertFalse(screen.contains("package name"))
        assertFalse(screen.contains("\u2605"))
        assertFalse(screen.contains("\u2606"))
    }

    @Test
    fun appDrawerStaysSearchableDarkAndUsesAllowHideCopy() {
        val drawer = source("app/src/main/java/com/transcendiverse/digitaltwin/launcher/LauncherAppDrawer.kt")
        val repository = source("app/src/main/java/com/transcendiverse/digitaltwin/launcher/LauncherAppsRepository.kt")

        assertTrue(drawer.contains("OutlinedTextField"))
        assertTrue(drawer.contains("placeholder = { Text(\"Search apps\") }"))
        assertTrue(drawer.contains("searchLauncherApps(apps, query)"))
        assertTrue(drawer.contains("DrawerInput"))
        assertTrue(drawer.contains("DrawerAccent"))
        assertTrue(repository.contains("displayLabel.contains(cleaned, ignoreCase = true)"))
        assertTrue(repository.contains("app.packageName.contains(cleaned, ignoreCase = true)"))
        assertTrue(drawer.contains("text = if (isAllowed) \"Hide\" else \"Allow\""))
        assertTrue(drawer.contains(".weight(1f)"))
        assertFalse(drawer.contains("LauncherTextAction(text = \"refresh\""))
        assertFalse(drawer.contains("LauncherTextAction(text = \"close\""))
        assertFalse(drawer.contains("Recently installed"))
        assertFalse(drawer.contains("Social"))
        assertFalse(drawer.contains("Work"))
        assertFalse(drawer.contains("TextButton("))
        assertFalse(drawer.contains("\u2605"))
        assertFalse(drawer.contains("\u2606"))
        assertFalse(drawer.contains("text = app.packageName"))
        assertFalse(drawer.contains("package name"))
    }

    @Test
    fun leftTwinPageHasExactCompanionDestinationsAndRealInlineActions() {
        val screen = source("app/src/main/java/com/transcendiverse/digitaltwin/launcher/LauncherScreen.kt")
        val activity = source("app/src/main/java/com/transcendiverse/digitaltwin/launcher/LauncherActivity.kt")
        val request = source("app/src/main/java/com/transcendiverse/digitaltwin/CompanionLaunchRequest.kt")
        val journalStore = source("app/src/main/java/com/transcendiverse/digitaltwin/data/JournalDraftStore.kt")

        assertTrue(screen.contains("onOpenCompanionDestination"))
        assertTrue(screen.contains("CompanionDestination.CheckIn"))
        assertTrue(screen.contains("CompanionDestination.Quest"))
        assertTrue(screen.contains("CompanionDestination.Journal"))
        assertTrue(screen.contains("CompanionDestination.Ask"))
        assertTrue(screen.contains("save baseline"))
        assertTrue(screen.contains("+10%"))
        assertTrue(screen.contains("send ask"))
        assertTrue(screen.contains("Local draft"))
        assertTrue(screen.contains("Prompt"))

        assertTrue(activity.contains("NetworkCheckInRepository"))
        assertTrue(activity.contains("submitDaily(cleanRatings)"))
        assertTrue(activity.contains("NetworkQuestRepository"))
        assertTrue(activity.contains("resolveActiveQuest(currentQuest, repository.listQuests())"))
        assertTrue(activity.contains("repository.updateProgress(activeQuest.id, nextProgress)"))
        assertTrue(activity.contains("NetworkChatRepository"))
        assertTrue(activity.contains("sendMessage(message = message, chatId = activeChatId)"))
        assertTrue(activity.contains("SharedPreferencesJournalDraftStore"))
        assertTrue(activity.contains("journalDraftStore.save(cleanDraft)"))
        assertTrue(activity.contains("NetworkJournalRepository"))
        assertTrue(activity.contains("createEntry("))
        assertTrue(activity.contains("generatedJournalTitle(cleanDraft)"))
        assertTrue(activity.contains("Journal synced to web"))
        assertTrue(activity.contains("Local draft saved"))
        assertTrue(activity.contains("NetworkTodayRepository"))

        assertTrue(request.contains("putCompanionLaunchRequest"))
        assertTrue(journalStore.contains("launcher_journal_draft"))
    }

    @Test
    fun launcherKeepsSettingsVisibleWithoutDefaultHomeAutomation() {
        val screen = source("app/src/main/java/com/transcendiverse/digitaltwin/launcher/LauncherScreen.kt")
        val activity = source("app/src/main/java/com/transcendiverse/digitaltwin/launcher/LauncherActivity.kt")
        val drawer = source("app/src/main/java/com/transcendiverse/digitaltwin/launcher/LauncherAppDrawer.kt")

        assertTrue(screen.contains("TwinLauncherPage"))
        assertTrue(screen.contains("LauncherPanelTinyAction(text = \"open\""))
        assertTrue(screen.contains("LauncherPanelTinyAction(text = \"⚙\""))
        assertTrue(screen.contains("onClick = onOpenSettings"))
        assertTrue(screen.contains("onOpenGoogleSearch"))
        assertTrue(screen.contains("BackHandler"))
        assertTrue(activity.contains("override fun onNewIntent(intent: Intent)"))
        assertTrue(activity.contains("Intent.ACTION_WEB_SEARCH"))
        assertTrue(activity.contains("SearchManager.QUERY"))
        assertFalse(drawer.contains("verticalScroll"))
        assertFalse(drawer.contains("rememberScrollState"))
        assertFalse(activity.contains("ACTION_MANAGE_DEFAULT_APPS_SETTINGS"))
        assertFalse(activity.contains("RoleManager"))
        assertFalse(activity.contains("ROLE_HOME"))
    }

    @Test
    fun companionLaunchUsesSeparateTaskIntentAndDestinationExtrasFromHomeSurface() {
        val activity = source("app/src/main/java/com/transcendiverse/digitaltwin/launcher/LauncherActivity.kt")
        val main = source("app/src/main/java/com/transcendiverse/digitaltwin/MainActivity.kt")
        val request = source("app/src/main/java/com/transcendiverse/digitaltwin/CompanionLaunchRequest.kt")

        assertTrue(activity.contains("startActivity(companionTaskIntent(this, request))"))
        assertTrue(activity.contains("Intent(context, MainActivity::class.java)"))
        assertTrue(activity.contains("Intent.FLAG_ACTIVITY_NEW_TASK"))
        assertTrue(activity.contains("Intent.FLAG_ACTIVITY_CLEAR_TOP"))
        assertTrue(activity.contains("Intent.FLAG_ACTIVITY_SINGLE_TOP"))
        assertTrue(activity.contains(".putCompanionLaunchRequest(request)"))

        assertTrue(main.contains("override fun onNewIntent(intent: Intent)"))
        assertTrue(main.contains("setIntent(intent)"))
        assertTrue(main.contains("launchRequestState.value = intent.toCompanionLaunchRequest"))
        assertTrue(request.contains("EXTRA_COMPANION_DESTINATION"))
        assertTrue(request.contains("EXTRA_COMPANION_PAYLOAD"))
    }

    @Test
    fun manifestDeclaresLauncherAppVisibilityQuery() {
        val manifest = source("app/src/main/AndroidManifest.xml")

        assertTrue(manifest.contains("<queries>"))
        assertTrue(manifest.contains("android.intent.category.LAUNCHER"))
    }

    private fun source(path: String): String {
        val candidates = listOf(
            File(path),
            File(path.removePrefix("app/")),
        )
        return candidates.firstOrNull(File::isFile)?.readText()
            ?: error("Unable to read $path")
    }
}
