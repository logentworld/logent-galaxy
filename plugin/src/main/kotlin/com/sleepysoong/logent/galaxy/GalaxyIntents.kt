package com.sleepysoong.logent.galaxy

/**
 * Android `Intent`를 프레임워크 없이 검증할 수 있도록 값으로 표현한 스펙이에요.
 *
 * 플러그인은 이 스펙을 실제 `Intent`로 바꿔 실행하고, JVM 테스트는 action·data·type·
 * extras만 검사해요.
 */
data class IntentSpec(
    val action: String? = null,
    val data: String? = null,
    val type: String? = null,
    val packageName: String? = null,
    val component: String? = null,
    val extras: Map<String, String> = emptyMap(),
) {
    init {
        require(action != null || component != null) { "action이나 component 중 하나는 필요해요." }
        require(action == null || action.isNotBlank()) { "action이 비어 있어요." }
        require(component == null || component.contains('/')) { "component는 패키지/클래스 형식이어야 해요." }
        require(packageName == null || packageName.isNotBlank()) { "packageName이 비어 있어요." }
    }
}

/**
 * 삼성 앱·설정 화면을 여는 공개 Intent 빌더예요.
 *
 * 비공개 API·Knox SDK 없이 `ACTION_INSERT`, `ACTION_SENDTO`, `ACTION_VIEW` 딥링크,
 * 표준 Settings action만 만들어요.
 */
object GalaxyIntents {
    const val ACTION_VIEW = "android.intent.action.VIEW"
    const val ACTION_INSERT = "android.intent.action.INSERT"
    const val ACTION_SENDTO = "android.intent.action.SENDTO"
    const val ACTION_SETTINGS = "android.settings.SETTINGS"

    const val EXTRA_SUBJECT = "android.intent.extra.SUBJECT"
    const val EXTRA_TEXT = "android.intent.extra.TEXT"
    const val EXTRA_SMS_BODY = "sms_body"

    const val NOTE_MIME_TYPE = "vnd.android.cursor.dir/note"
    const val REMINDER_MIME_TYPE = "vnd.android.cursor.dir/reminder"

    const val SAMSUNG_STORE_SCHEME = "samsungapps"
    const val PLAY_STORE_SCHEME = "market"

    /** `samsungapps://ProductDetail/<package>` 딥링크예요. */
    fun samsungStoreUri(packageName: String): String {
        require(packageName.isNotBlank()) { "패키지 이름이 필요해요." }
        return "$SAMSUNG_STORE_SCHEME://ProductDetail/$packageName"
    }

    /** `market://details?id=<package>` 딥링크예요. */
    fun playStoreUri(packageName: String): String {
        require(packageName.isNotBlank()) { "패키지 이름이 필요해요." }
        return "$PLAY_STORE_SCHEME://details?id=$packageName"
    }

    /** 삼성 스토어 → Play 스토어 순서의 설치 안내 후보예요. */
    fun storeFallbacks(packageName: String): List<IntentSpec> = listOf(
        IntentSpec(action = ACTION_VIEW, data = samsungStoreUri(packageName)),
        IntentSpec(action = ACTION_VIEW, data = playStoreUri(packageName)),
    )

    /** 삼성 노트 새 노트 편집 화면을 여는 공개 INSERT 스펙이에요. */
    fun noteInsert(title: String?, body: String?, packageName: String? = null): IntentSpec =
        insert(NOTE_MIME_TYPE, title, body, packageName)

    /** 삼성 리마인더 새 항목 편집 화면을 여는 공개 INSERT 스펙이에요. */
    fun reminderInsert(title: String?, body: String?, packageName: String? = null): IntentSpec =
        insert(REMINDER_MIME_TYPE, title, body, packageName)

    /** 문자 작성 화면을 여는 `ACTION_SENDTO sms:` 스펙이에요. 문자를 보내지 않아요. */
    fun smsCompose(to: String, body: String? = null, packageName: String? = null): IntentSpec {
        require(to.isNotBlank()) { "받는 번호가 필요해요." }
        val extras = body?.takeIf { it.isNotBlank() }
            ?.let { mapOf(EXTRA_SMS_BODY to it) }
            ?: emptyMap()
        return IntentSpec(
            action = ACTION_SENDTO,
            data = "sms:$to",
            packageName = packageName,
            extras = extras,
        )
    }

    /** [GalaxySettingTarget]을 실제로 시도할 Intent 스펙으로 바꿔요. */
    fun settings(target: GalaxySettingTarget): IntentSpec =
        if (target.component != null) {
            IntentSpec(component = target.component)
        } else {
            IntentSpec(action = target.action)
        }

    private fun insert(
        mimeType: String,
        title: String?,
        body: String?,
        packageName: String?,
    ): IntentSpec {
        val extras = buildMap {
            title?.takeIf { it.isNotBlank() }?.let { put(EXTRA_SUBJECT, it) }
            body?.takeIf { it.isNotBlank() }?.let { put(EXTRA_TEXT, it) }
        }
        return IntentSpec(
            action = ACTION_INSERT,
            type = mimeType,
            packageName = packageName,
            extras = extras,
        )
    }
}
