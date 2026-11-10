package com.sleepysoong.logent.galaxy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GalaxyCatalogTest {
    @Test
    fun `앱 카탈로그는 요구한 26개 키를 모두 가져요`() {
        val expected = setOf(
            "notes", "reminder", "messages", "phone", "contacts", "calendar", "gallery",
            "myfiles", "health", "wallet", "smartthings", "modes", "securefolder", "wearable",
            "buds", "theme", "members", "store", "browser", "camera", "clock", "calculator",
            "weather", "bixby", "dex", "bixbyroutines",
        )

        assertEquals(26, GalaxyCatalog.appKeys.size)
        assertEquals(expected, GalaxyCatalog.appKeys.toSet())
    }

    @Test
    fun `앱 키와 패키지 후보에는 중복이나 빈 값이 없어요`() {
        assertEquals(GalaxyCatalog.appKeys.size, GalaxyCatalog.appKeys.distinct().size)
        GalaxyCatalog.apps.forEach { app ->
            assertTrue("빈 앱 키", app.key.matches(Regex("[a-z][a-z0-9]*")))
            assertTrue("빈 앱 라벨: ${app.key}", app.label.isNotBlank())
            assertTrue("패키지 후보 없음: ${app.key}", app.packages.isNotEmpty())
            assertEquals("중복 패키지 후보: ${app.key}", app.packages.size, app.packages.distinct().size)
            app.packages.forEach { packageName ->
                assertTrue("빈 패키지 이름: ${app.key}", packageName.isNotBlank())
                assertTrue(
                    "잘못된 패키지 이름: $packageName",
                    packageName.matches(Regex("[a-z][a-z0-9_]*(\\.[a-z0-9_]+)+")),
                )
            }
        }
    }

    @Test
    fun `앱 사이 패키지 중복은 루틴 별칭 하나뿐이에요`() {
        val owners = mutableMapOf<String, MutableList<String>>()
        GalaxyCatalog.apps.forEach { app ->
            app.packages.forEach { packageName ->
                owners.getOrPut(packageName) { mutableListOf() }.add(app.key)
            }
        }
        val duplicated = owners.filterValues { it.size > 1 }

        assertEquals(setOf("com.samsung.android.app.routines"), duplicated.keys)
        assertEquals(setOf("modes", "bixbyroutines"), duplicated.values.single().toSet())
    }

    @Test
    fun `주요 앱은 요구한 패키지 이름을 그대로 써요`() {
        assertEquals(listOf("com.samsung.android.app.notes"), GalaxyCatalog.app("notes")!!.packages)
        assertEquals(listOf("com.samsung.android.app.reminder"), GalaxyCatalog.app("reminder")!!.packages)
        assertEquals(listOf("com.samsung.android.messaging"), GalaxyCatalog.app("messages")!!.packages)
        assertEquals(listOf("com.sec.android.gallery3d"), GalaxyCatalog.app("gallery")!!.packages)
        assertEquals(listOf("com.samsung.knox.securefolder"), GalaxyCatalog.app("securefolder")!!.packages)
        assertEquals(listOf("com.samsung.android.dex"), GalaxyCatalog.app("dex")!!.packages)
        assertEquals(listOf("com.samsung.android.app.routines"), GalaxyCatalog.app("modes")!!.packages)
        assertEquals(listOf("com.samsung.android.app.routines"), GalaxyCatalog.app("bixbyroutines")!!.packages)
        assertNull(GalaxyCatalog.app("unknown"))
    }

    @Test
    fun `설치된 첫 후보 패키지를 고르고 없으면 null이에요`() {
        val app = GalaxyApp("demo", "데모", listOf("a.one", "a.two"))

        assertEquals("a.one", GalaxyCatalog.firstInstalled(app) { it == "a.one" || it == "a.two" })
        assertEquals("a.two", GalaxyCatalog.firstInstalled(app) { it == "a.two" })
        assertNull(GalaxyCatalog.firstInstalled(app) { false })
    }

    @Test
    fun `설정 섹션 키는 요구한 13개예요`() {
        val expected = setOf(
            "oneui_home", "navigation", "edge_panels", "modes", "secure_folder", "battery",
            "display", "sound", "notifications", "privacy", "accounts", "system_update", "about",
        )

        assertEquals(13, GalaxyCatalog.settingSectionKeys.size)
        assertEquals(expected, GalaxyCatalog.settingSectionKeys.toSet())
        assertNotNull(GalaxyCatalog.settingSection("navigation"))
        assertNull(GalaxyCatalog.settingSection("unknown"))
    }

    @Test
    fun `모든 설정 섹션은 표준 설정 폴백을 마지막에 둬요`() {
        GalaxyCatalog.settingsSections.forEach { section ->
            val targets = GalaxyCatalog.settingsTargets(section)

            assertTrue("빈 후보: ${section.key}", targets.isNotEmpty())
            assertEquals("중복 후보: ${section.key}", targets.size, targets.distinct().size)
            val last = targets.last()
            assertEquals("마지막 폴백: ${section.key}", GalaxyIntents.ACTION_SETTINGS, last.action)
            assertNull("폴백은 action이어야 해요: ${section.key}", last.component)

            targets.forEach { target ->
                assertTrue(
                    "component와 action은 하나만: ${section.key}",
                    (target.component == null) != (target.action == null),
                )
                target.component?.let { component ->
                    assertTrue(component, component.startsWith("com.samsung.android.settings/"))
                    assertTrue(
                        component,
                        component.substringAfter('/').startsWith("com.samsung.android.settings.Settings$"),
                    )
                }
                target.action?.let { action ->
                    assertTrue(action, action.startsWith("android.settings."))
                }
            }
        }
    }

    @Test
    fun `DeX 설정 후보도 표준 설정 폴백을 마지막에 둬요`() {
        val targets = GalaxyCatalog.dexTargets()

        assertTrue(targets.size >= 2)
        assertNull(targets.last().component)
        assertEquals(GalaxyIntents.ACTION_SETTINGS, targets.last().action)
        assertTrue(targets.dropLast(1).all { it.component != null })
    }

    @Test
    fun `SEM_PLATFORM_INT는 One UI 버전 문자열로 바뀌어요`() {
        assertEquals("1.0", GalaxyCatalog.oneUiVersion(100_000))
        assertEquals("3.1", GalaxyCatalog.oneUiVersion(120_100))
        assertEquals("4.1.1", GalaxyCatalog.oneUiVersion(130_101))
        assertEquals("6.0", GalaxyCatalog.oneUiVersion(150_000))
        assertEquals("7.0", GalaxyCatalog.oneUiVersion(160_000))
        assertNull(GalaxyCatalog.oneUiVersion(null))
        assertNull(GalaxyCatalog.oneUiVersion(99_999))
        assertNull(GalaxyCatalog.oneUiVersion(0))
    }
}
