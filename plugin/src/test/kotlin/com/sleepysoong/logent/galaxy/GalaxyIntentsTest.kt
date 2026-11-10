package com.sleepysoong.logent.galaxy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class GalaxyIntentsTest {
    @Test
    fun `INSERT MIME은 공개 계약 문자열이에요`() {
        assertEquals("vnd.android.cursor.dir/note", GalaxyIntents.NOTE_MIME_TYPE)
        assertEquals("vnd.android.cursor.dir/reminder", GalaxyIntents.REMINDER_MIME_TYPE)
        assertEquals("android.intent.extra.SUBJECT", GalaxyIntents.EXTRA_SUBJECT)
        assertEquals("android.intent.extra.TEXT", GalaxyIntents.EXTRA_TEXT)
        assertEquals("sms_body", GalaxyIntents.EXTRA_SMS_BODY)
    }

    @Test
    fun `노트 INSERT는 공개 MIME과 제목·본문 extras를 써요`() {
        val spec = GalaxyIntents.noteInsert(
            title = "장보기",
            body = "우유, 달걀",
            packageName = "com.samsung.android.app.notes",
        )

        assertEquals(GalaxyIntents.ACTION_INSERT, spec.action)
        assertEquals(GalaxyIntents.NOTE_MIME_TYPE, spec.type)
        assertEquals("com.samsung.android.app.notes", spec.packageName)
        assertEquals("장보기", spec.extras[GalaxyIntents.EXTRA_SUBJECT])
        assertEquals("우유, 달걀", spec.extras[GalaxyIntents.EXTRA_TEXT])
        assertNull(spec.data)
    }

    @Test
    fun `빈 제목과 본문은 extras에 넣지 않아요`() {
        val spec = GalaxyIntents.noteInsert(title = " ", body = null)

        assertTrue(spec.extras.isEmpty())
        assertEquals(GalaxyIntents.NOTE_MIME_TYPE, spec.type)
    }

    @Test
    fun `리마인더 INSERT는 리마인더 MIME을 써요`() {
        val spec = GalaxyIntents.reminderInsert("회의", "14시", "com.samsung.android.app.reminder")

        assertEquals(GalaxyIntents.ACTION_INSERT, spec.action)
        assertEquals(GalaxyIntents.REMINDER_MIME_TYPE, spec.type)
        assertEquals("회의", spec.extras[GalaxyIntents.EXTRA_SUBJECT])
        assertEquals("14시", spec.extras[GalaxyIntents.EXTRA_TEXT])
    }

    @Test
    fun `문자 작성은 SENDTO sms 스펙이고 전송 action이 아니에요`() {
        val spec = GalaxyIntents.smsCompose(
            to = "010-1234-5678",
            body = "안녕하세요",
            packageName = "com.samsung.android.messaging",
        )

        assertEquals(GalaxyIntents.ACTION_SENDTO, spec.action)
        assertEquals("sms:010-1234-5678", spec.data)
        assertEquals("com.samsung.android.messaging", spec.packageName)
        assertEquals("안녕하세요", spec.extras[GalaxyIntents.EXTRA_SMS_BODY])
        assertNull(spec.type)
    }

    @Test
    fun `본문이 없으면 sms_body를 넣지 않아요`() {
        val spec = GalaxyIntents.smsCompose("+821012345678", body = "  ")

        assertTrue(spec.extras.isEmpty())
        assertEquals("sms:+821012345678", spec.data)
    }

    @Test
    fun `스토어 폴백은 삼성 스토어 다음 Play 스토어예요`() {
        val stores = GalaxyIntents.storeFallbacks("com.samsung.android.app.notes")

        assertEquals(2, stores.size)
        assertEquals(
            listOf(
                "samsungapps://ProductDetail/com.samsung.android.app.notes",
                "market://details?id=com.samsung.android.app.notes",
            ),
            stores.map { it.data },
        )
        assertTrue(stores.all { it.action == GalaxyIntents.ACTION_VIEW })
        assertTrue(stores.all { it.packageName == null && it.type == null })
    }

    @Test
    fun `빈 패키지나 번호는 거부해요`() {
        assertThrows(IllegalArgumentException::class.java) { GalaxyIntents.storeFallbacks(" ") }
        assertThrows(IllegalArgumentException::class.java) { GalaxyIntents.samsungStoreUri("") }
        assertThrows(IllegalArgumentException::class.java) { GalaxyIntents.smsCompose("  ") }
    }

    @Test
    fun `설정 컴포넌트는 action 없이 component로 만들어져요`() {
        val component = "com.samsung.android.settings/" +
            "com.samsung.android.settings.Settings\$DisplaySettingsActivity"
        val spec = GalaxyIntents.settings(GalaxySettingTarget(component = component))

        assertNull(spec.action)
        assertEquals(component, spec.component)
        assertNull(spec.data)
    }

    @Test
    fun `표준 설정 action은 action으로 만들어져요`() {
        val spec = GalaxyIntents.settings(GalaxySettingTarget(action = "android.settings.DISPLAY_SETTINGS"))

        assertEquals("android.settings.DISPLAY_SETTINGS", spec.action)
        assertNull(spec.component)
    }

    @Test
    fun `Intent 스펙은 action이나 component가 없으면 거부해요`() {
        assertThrows(IllegalArgumentException::class.java) { IntentSpec() }
        assertThrows(IllegalArgumentException::class.java) { IntentSpec(action = " ") }
        assertThrows(IllegalArgumentException::class.java) {
            IntentSpec(component = "com.example.SettingsActivity")
        }
    }

    @Test
    fun `설정 대상은 component와 action 중 하나만 가져야 해요`() {
        assertThrows(IllegalArgumentException::class.java) { GalaxySettingTarget() }
        assertThrows(IllegalArgumentException::class.java) {
            GalaxySettingTarget(
                component = "com.samsung.android.settings/com.samsung.android.settings.Settings\$BatterySettingsActivity",
                action = "android.settings.SETTINGS",
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            GalaxySettingTarget(action = "com.example.NOT_SETTINGS")
        }
    }
}
