package com.sleepysoong.logent.galaxy

/** 갤럭시 앱 하나와 설치 후보 패키지 목록이에요. */
data class GalaxyApp(
    val key: String,
    val label: String,
    val packages: List<String>,
)

/**
 * 설정 화면 후보 하나예요.
 *
 * [component]가 있으면 삼성 설정의 명시 컴포넌트를 열고, [action]이 있으면 표준
 * Settings Intent를 열어요. 둘 중 정확히 하나만 지정해요.
 */
data class GalaxySettingTarget(
    val component: String? = null,
    val action: String? = null,
) {
    init {
        require((component == null) != (action == null)) { "component와 action 중 하나만 지정해야 해요." }
        require(component == null || component.contains('/')) { "component는 패키지/클래스 형식이어야 해요." }
        require(action == null || action.startsWith("android.settings.")) {
            "설정 action은 android.settings.* 여야 해요."
        }
    }
}

/** 설정 화면 묶음이에요. 마지막 폴백은 [GalaxyCatalog.settingsTargets]가 붙여요. */
data class GalaxySettingSection(
    val key: String,
    val label: String,
    val targets: List<GalaxySettingTarget>,
)

/**
 * 삼성 앱·설정 후보 표예요. Android 프레임워크를 쓰지 않는 순수 데이터라
 * JVM 테스트에서 그대로 검증해요.
 *
 * 패키지 이름과 설정 컴포넌트는 One UI·앱 버전에 따라 달라질 수 있어요. 설치되지
 * 않았거나 해석되지 않는 후보는 호출부가 다음 후보로 넘어가고, 마지막에는
 * [GalaxyIntents.ACTION_SETTINGS]로 폴백해요.
 */
object GalaxyCatalog {
    const val SECURE_FOLDER_PACKAGE = "com.samsung.knox.securefolder"

    /** Knox 구성 요소가 설치되어 있는지 확인할 공개 패키지 이름이에요. */
    val KNOX_PACKAGES: List<String> = listOf(
        "com.samsung.android.knox.attestation",
        "com.knox.vpn.proxyhandler",
    )

    val apps: List<GalaxyApp> = listOf(
        GalaxyApp("notes", "삼성 노트", listOf("com.samsung.android.app.notes")),
        GalaxyApp("reminder", "삼성 리마인더", listOf("com.samsung.android.app.reminder")),
        GalaxyApp("messages", "삼성 메시지", listOf("com.samsung.android.messaging")),
        GalaxyApp("phone", "삼성 전화", listOf("com.samsung.android.dialer")),
        GalaxyApp("contacts", "삼성 연락처", listOf("com.samsung.android.contacts")),
        GalaxyApp("calendar", "삼성 캘린더", listOf("com.samsung.android.calendar")),
        GalaxyApp("gallery", "갤러리", listOf("com.sec.android.gallery3d")),
        GalaxyApp("myfiles", "내 파일", listOf("com.sec.android.app.myfiles")),
        GalaxyApp("health", "삼성 헬스", listOf("com.sec.android.app.shealth")),
        GalaxyApp("wallet", "삼성 월렛", listOf("com.samsung.android.spay")),
        GalaxyApp("smartthings", "스마트싱스", listOf("com.samsung.android.oneconnect")),
        GalaxyApp("modes", "모드 및 루틴", listOf("com.samsung.android.app.routines")),
        GalaxyApp("securefolder", "보안 폴더", listOf(SECURE_FOLDER_PACKAGE)),
        GalaxyApp("wearable", "갤럭시 웨어러블", listOf("com.samsung.android.app.watchmanager")),
        GalaxyApp("buds", "갤럭시 버즈", listOf("com.samsung.accessory.buds")),
        GalaxyApp("theme", "삼성 테마", listOf("com.samsung.android.themestore")),
        GalaxyApp("members", "삼성 멤버스", listOf("com.samsung.android.voc")),
        GalaxyApp("store", "갤럭시 스토어", listOf("com.sec.android.app.samsungapps")),
        GalaxyApp("browser", "삼성 인터넷", listOf("com.sec.android.app.sbrowser")),
        GalaxyApp("camera", "카메라", listOf("com.sec.android.app.camera")),
        GalaxyApp("clock", "시계", listOf("com.sec.android.app.clockpackage")),
        GalaxyApp("calculator", "계산기", listOf("com.sec.android.app.popupcalculator")),
        GalaxyApp("weather", "날씨", listOf("com.samsung.android.daemonapp")),
        GalaxyApp("bixby", "빅스비", listOf("com.samsung.android.bixby.agent")),
        GalaxyApp("dex", "Samsung DeX", listOf("com.samsung.android.dex")),
        GalaxyApp("bixbyroutines", "빅스비 루틴", listOf("com.samsung.android.app.routines")),
    )

    val appKeys: List<String> = apps.map { it.key }

    val settingsSections: List<GalaxySettingSection> = listOf(
        GalaxySettingSection(
            key = "oneui_home",
            label = "One UI 홈 화면",
            targets = listOf(
                component("Settings\$HomeScreenSettingsActivity"),
                action("android.settings.HOME_SETTINGS"),
            ),
        ),
        GalaxySettingSection(
            key = "navigation",
            label = "내비게이션 바",
            targets = listOf(
                component("Settings\$NavigationBarSettingsActivity"),
                action("android.settings.DISPLAY_SETTINGS"),
            ),
        ),
        GalaxySettingSection(
            key = "edge_panels",
            label = "엣지 패널",
            targets = listOf(
                component("Settings\$EdgePanelsSettingsActivity"),
                component("Settings\$EdgeScreenSettingsActivity"),
                action("android.settings.DISPLAY_SETTINGS"),
            ),
        ),
        GalaxySettingSection(
            key = "modes",
            label = "모드 및 루틴",
            targets = listOf(
                component("Settings\$ModesAndRoutinesSettingsActivity"),
                component("Settings\$ModesSettingsActivity"),
            ),
        ),
        GalaxySettingSection(
            key = "secure_folder",
            label = "보안 폴더",
            targets = listOf(
                component("Settings\$SecureFolderSettingsActivity"),
                action("android.settings.SECURITY_SETTINGS"),
            ),
        ),
        GalaxySettingSection(
            key = "battery",
            label = "배터리",
            targets = listOf(
                component("Settings\$BatterySettingsActivity"),
                action("android.settings.BATTERY_SAVER_SETTINGS"),
            ),
        ),
        GalaxySettingSection(
            key = "display",
            label = "디스플레이",
            targets = listOf(
                component("Settings\$DisplaySettingsActivity"),
                action("android.settings.DISPLAY_SETTINGS"),
            ),
        ),
        GalaxySettingSection(
            key = "sound",
            label = "소리 및 진동",
            targets = listOf(
                component("Settings\$SoundSettingsActivity"),
                action("android.settings.SOUND_SETTINGS"),
            ),
        ),
        GalaxySettingSection(
            key = "notifications",
            label = "알림",
            targets = listOf(
                component("Settings\$NotificationSettingsActivity"),
                action("android.settings.NOTIFICATION_SETTINGS"),
            ),
        ),
        GalaxySettingSection(
            key = "privacy",
            label = "개인정보 보호",
            targets = listOf(
                component("Settings\$PrivacySettingsActivity"),
                action("android.settings.PRIVACY_SETTINGS"),
            ),
        ),
        GalaxySettingSection(
            key = "accounts",
            label = "계정 및 백업",
            targets = listOf(
                component("Settings\$AccountsSettingsActivity"),
                action("android.settings.SYNC_SETTINGS"),
            ),
        ),
        GalaxySettingSection(
            key = "system_update",
            label = "소프트웨어 업데이트",
            targets = listOf(
                component("Settings\$SoftwareUpdateSettingsActivity"),
                component("Settings\$SystemUpdateSettingsActivity"),
            ),
        ),
        GalaxySettingSection(
            key = "about",
            label = "휴대전화 정보",
            targets = listOf(
                component("Settings\$DeviceInfoSettingsActivity"),
                action("android.settings.DEVICE_INFO_SETTINGS"),
            ),
        ),
    )

    val settingSectionKeys: List<String> = settingsSections.map { it.key }

    private val dexSettings: List<GalaxySettingTarget> = listOf(
        component("Settings\$SamsungDexSettingsActivity"),
        component("Settings\$DexSettingsActivity"),
    )

    fun app(key: String): GalaxyApp? = apps.firstOrNull { it.key == key }

    fun settingSection(key: String): GalaxySettingSection? = settingsSections.firstOrNull { it.key == key }

    /** 후보 중 설치된 첫 패키지를 골라요. 설치된 후보가 없으면 null이에요. */
    fun firstInstalled(app: GalaxyApp, isInstalled: (String) -> Boolean): String? =
        app.packages.firstOrNull(isInstalled)

    /** 섹션 후보 뒤에 표준 설정 폴백을 붙여요. 폴백은 항상 마지막이에요. */
    fun settingsTargets(section: GalaxySettingSection): List<GalaxySettingTarget> =
        section.targets + GalaxySettingTarget(action = GalaxyIntents.ACTION_SETTINGS)

    /** DeX 설정 후보와 표준 설정 폴백이에요. */
    fun dexTargets(): List<GalaxySettingTarget> =
        dexSettings + GalaxySettingTarget(action = GalaxyIntents.ACTION_SETTINGS)

    /**
     * `Build.VERSION.SEM_PLATFORM_INT` 값을 One UI 버전 문자열로 바꿔요.
     *
     * 삼성 설정 앱이 쓰는 방식과 같이 90000을 기준으로 계산해요
     * (예: 130000 → 4.0, 130101 → 4.1.1, 160000 → 7.0). 값이 없거나 One UI
     * 이전 형식이면 null이에요. 제조사 확장 필드라 기기에 따라 없을 수 있어요.
     */
    fun oneUiVersion(semPlatform: Int?): String? {
        if (semPlatform == null || semPlatform < 100_000) return null
        val relative = semPlatform - 90_000
        val major = relative / 10_000
        val minor = relative % 10_000 / 100
        val patch = relative % 100
        return buildString {
            append(major)
            append('.')
            append(minor)
            if (patch > 0) {
                append('.')
                append(patch)
            }
        }
    }

    private fun component(className: String): GalaxySettingTarget =
        GalaxySettingTarget(component = "com.samsung.android.settings/com.samsung.android.settings.$className")

    private fun action(name: String): GalaxySettingTarget = GalaxySettingTarget(action = name)
}
