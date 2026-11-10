package com.sleepysoong.logent.galaxy

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.net.Uri
import android.os.Build
import com.sleepysoong.logent.plugin.LogentPlugin
import com.sleepysoong.logent.plugin.PluginHost
import com.sleepysoong.logent.plugin.PluginTool
import com.sleepysoong.logent.plugin.ToolAccess
import com.sleepysoong.logent.plugin.ToolResult
import com.sleepysoong.logent.plugin.stringProperty
import com.sleepysoong.logent.plugin.text
import com.sleepysoong.logent.plugin.toolSpec
import kotlinx.serialization.json.JsonObject

/**
 * 삼성 갤럭시 앱·One UI 설정 화면을 여는 플러그인이에요.
 *
 * 설치 여부는 [android.content.pm.PackageManager]로 확인하고, 앱 실행은 공개 launcher
 * intent, 새 항목 작성은 `ACTION_INSERT`, 문자 작성은 `ACTION_SENDTO sms:`만 써요.
 * 읽기·자동화가 공개 API로 불가능한 기능은 빈 성공 대신 명확한 오류로 알려 줘요.
 */
class GalaxyPlugin : LogentPlugin {
    override val id: String = "com.sleepysoong.logent.galaxy"
    override val name: String = "Galaxy"
    override val version: String = BuildConfig.PLUGIN_VERSION
    override val apiVersion: Int = 4

    @Volatile
    private var host: PluginHost? = null

    override fun tools(): List<PluginTool> = listOf(
        appsTool(),
        notesTool(),
        reminderTool(),
        messagesTool(),
        settingsTool(),
        quickActionsTool(),
    )

    override suspend fun ensureReady(host: PluginHost): Result<Unit> {
        this.host = host
        return Result.success(Unit)
    }

    private fun appsTool(): PluginTool = PluginTool(
        spec = toolSpec(
            name = "galaxy_apps",
            description = "삼성 갤럭시 앱을 열거나, 설치된 삼성 앱 목록과 기기·One UI·보안 폴더 상태를 확인한다. " +
                "open은 공개 launcher intent를 쓰고 실행 activity가 없으면 스토어 상세 화면을 연다. " +
                "설치되지 않은 앱을 열었다고 보고하지 않는다.",
            required = listOf("action"),
        ) {
            stringProperty("action", "실행할 동작", listOf("open", "list", "status"))
            stringProperty("app", "open에 사용할 앱 키. list·status에서는 무시한다.", GalaxyCatalog.appKeys)
        },
        access = setOf(ToolAccess.WRITE),
        childVisible = false,
        label = "갤럭시 앱",
    ) { arguments, _ ->
        when (val action = arguments.text("action")?.trim()?.lowercase()) {
            "open" -> android { context ->
                val key = arguments.text("app")?.trim()?.lowercase()
                    ?: return@android ToolResult(
                        "open에는 app이 필요해요. 사용할 수 있는 값: ${GalaxyCatalog.appKeys.joinToString()}",
                        isError = true,
                    )
                openApp(context, key)
            }
            "list" -> android { listInstalledApps(it) }
            "status" -> android { deviceStatus(it) }
            null -> ToolResult("action이 필요해요. open, list, status 중 하나를 지정해요.", isError = true)
            else -> ToolResult("지원하지 않는 action이에요: $action. open, list, status 중 하나를 써요.", isError = true)
        }
    }

    private fun notesTool(): PluginTool = PluginTool(
        spec = toolSpec(
            name = "galaxy_notes",
            description = "삼성 노트를 열거나 새 노트 편집 화면을 연다. create는 공개 ACTION_INSERT와 " +
                "vnd.android.cursor.dir/note만 쓰며 저장은 사용자가 확인해야 한다. 노트 목록 읽기는 공개 API가 없어 list를 지원하지 않는다.",
            required = listOf("action"),
        ) {
            stringProperty("action", "실행할 동작", listOf("open", "create", "list"))
            stringProperty("title", "새 노트 제목")
            stringProperty("body", "새 노트 본문")
        },
        access = setOf(ToolAccess.WRITE),
        childVisible = false,
        label = "삼성 노트",
    ) { arguments, _ ->
        when (val action = arguments.text("action")?.trim()?.lowercase()) {
            "open" -> android { openApp(it, "notes") }
            "create" -> android { context ->
                createEntry(
                    context = context,
                    appKey = "notes",
                    mimeType = GalaxyIntents.NOTE_MIME_TYPE,
                    title = arguments.text("title"),
                    body = arguments.text("body"),
                )
            }
            "list" -> unsupportedRead(
                appLabel = "삼성 노트",
                detail = "노트 목록을 읽는 공개 API가 없어요. 노트 앱을 열어 직접 확인해 주세요.",
            )
            null -> ToolResult("action이 필요해요. open, create 중 하나를 지정해요.", isError = true)
            else -> ToolResult("지원하지 않는 action이에요: $action. open, create 중 하나를 써요.", isError = true)
        }
    }

    private fun reminderTool(): PluginTool = PluginTool(
        spec = toolSpec(
            name = "galaxy_reminder",
            description = "삼성 리마인더를 열거나 새 리마인더 편집 화면을 연다. create는 공개 ACTION_INSERT와 " +
                "vnd.android.cursor.dir/reminder만 쓰며 저장은 사용자가 확인해야 한다. 리마인더 목록 읽기는 공개 API가 없어 list를 지원하지 않는다.",
            required = listOf("action"),
        ) {
            stringProperty("action", "실행할 동작", listOf("open", "create", "list"))
            stringProperty("title", "리마인더 제목")
            stringProperty("body", "리마인더 메모")
        },
        access = setOf(ToolAccess.WRITE),
        childVisible = false,
        label = "삼성 리마인더",
    ) { arguments, _ ->
        when (val action = arguments.text("action")?.trim()?.lowercase()) {
            "open" -> android { openApp(it, "reminder") }
            "create" -> android { context ->
                val title = arguments.text("title")
                if (title.isNullOrBlank()) {
                    return@android ToolResult("리마인더 제목(title)이 필요해요.", isError = true)
                }
                createEntry(
                    context = context,
                    appKey = "reminder",
                    mimeType = GalaxyIntents.REMINDER_MIME_TYPE,
                    title = title,
                    body = arguments.text("body"),
                )
            }
            "list" -> unsupportedRead(
                appLabel = "삼성 리마인더",
                detail = "리마인더 목록을 읽는 공개 API가 없어요. 리마인더 앱을 열어 직접 확인해 주세요.",
            )
            null -> ToolResult("action이 필요해요. open, create 중 하나를 지정해요.", isError = true)
            else -> ToolResult("지원하지 않는 action이에요: $action. open, create 중 하나를 써요.", isError = true)
        }
    }

    private fun messagesTool(): PluginTool = PluginTool(
        spec = toolSpec(
            name = "galaxy_messages",
            description = "삼성 메시지를 열거나 문자 작성 화면을 연다. compose는 공개 ACTION_SENDTO sms:만 쓰고 " +
                "문자를 전송하지 않는다. 다른 앱의 문자 DB를 읽지 않으며 검색 딥링크는 공개되어 있지 않아 지원하지 않는다.",
            required = listOf("action"),
        ) {
            stringProperty("action", "실행할 동작", listOf("open", "compose", "search"))
            stringProperty("to", "compose의 받는 번호")
            stringProperty("body", "compose의 문자 본문")
        },
        access = setOf(ToolAccess.WRITE),
        childVisible = false,
        label = "삼성 메시지",
    ) { arguments, _ ->
        when (val action = arguments.text("action")?.trim()?.lowercase()) {
            "open" -> android { openApp(it, "messages") }
            "compose" -> android { context -> composeMessage(context, arguments) }
            "search" -> ToolResult(
                "삼성 메시지 앱의 검색 화면 딥링크는 공개되어 있지 않아 지원하지 않아요. " +
                    "open으로 메시지 앱을 연 뒤 검색을 직접 사용해 주세요. 다른 앱의 문자 DB는 읽지 않아요.",
                isError = true,
            )
            null -> ToolResult("action이 필요해요. open, compose 중 하나를 지정해요.", isError = true)
            else -> ToolResult("지원하지 않는 action이에요: $action. open, compose 중 하나를 써요.", isError = true)
        }
    }

    private fun settingsTool(): PluginTool = PluginTool(
        spec = toolSpec(
            name = "galaxy_settings",
            description = "갤럭시·One UI 설정 화면을 연다. 표준 Settings Intent와 삼성 설정 컴포넌트를 순서대로 시도하고 " +
                "실패하면 전체 설정으로 폴백한다. 이 도구는 화면만 열고 값을 바꾸지 않는다.",
            required = listOf("action", "section"),
        ) {
            stringProperty("action", "실행할 동작. open만 지원한다.", listOf("open"))
            stringProperty("section", "열 설정 화면", GalaxyCatalog.settingSectionKeys)
        },
        access = setOf(ToolAccess.WRITE),
        childVisible = false,
        label = "갤럭시 설정",
    ) { arguments, _ ->
        when (val action = arguments.text("action")?.trim()?.lowercase()) {
            "open" -> android { context ->
                val key = arguments.text("section")?.trim()?.lowercase()
                    ?: return@android ToolResult(
                        "section이 필요해요. 사용할 수 있는 값: ${GalaxyCatalog.settingSectionKeys.joinToString()}",
                        isError = true,
                    )
                val section = GalaxyCatalog.settingSection(key)
                    ?: return@android ToolResult(
                        "지원하지 않는 section이에요: $key. 사용할 수 있는 값: ${GalaxyCatalog.settingSectionKeys.joinToString()}",
                        isError = true,
                    )
                openSettingTargets(context, section.label, GalaxyCatalog.settingsTargets(section))
            }
            null -> ToolResult("action이 필요해요. open을 지정해요.", isError = true)
            else -> ToolResult("지원하지 않는 action이에요: $action. open만 지원해요.", isError = true)
        }
    }

    private fun quickActionsTool(): PluginTool = PluginTool(
        spec = toolSpec(
            name = "galaxy_quick_actions",
            description = "빅스비·스마트싱스·모드 및 루틴 앱이나 DeX 설정 화면을 연다. 모드 전환·DeX 켜기처럼 " +
                "공개 API가 없는 동작은 화면만 열고 실제 전환은 사용자가 해야 한다고 알린다.",
            required = listOf("action"),
        ) {
            stringProperty(
                "action",
                "실행할 동작",
                listOf("bixby", "smartthings", "modes", "dex_mode"),
            )
        },
        access = setOf(ToolAccess.WRITE),
        childVisible = false,
        label = "갤럭시 빠른 동작",
    ) { arguments, _ ->
        when (val action = arguments.text("action")?.trim()?.lowercase()) {
            "bixby" -> android { openApp(it, "bixby") }
            "smartthings" -> android { openApp(it, "smartthings") }
            "modes" -> android { openApp(it, "modes") }
            "dex_mode" -> android { context ->
                openSettingTargets(
                    context = context,
                    label = "Samsung DeX",
                    targets = GalaxyCatalog.dexTargets(),
                    detail = "DeX 모드 전환·해제는 공개 API가 없어 화면에서 직접 선택해야 해요.",
                )
            }
            null -> ToolResult(
                "action이 필요해요. bixby, smartthings, modes, dex_mode 중 하나를 지정해요.",
                isError = true,
            )
            else -> ToolResult("지원하지 않는 action이에요: $action.", isError = true)
        }
    }

    private fun openApp(context: Context, key: String): ToolResult {
        val app = GalaxyCatalog.app(key)
            ?: return ToolResult(
                "알 수 없는 app 값이에요: $key. 사용할 수 있는 값: ${GalaxyCatalog.appKeys.joinToString()}",
                isError = true,
            )
        val installed = app.packages.filter { isInstalled(context, it) }
        if (installed.isEmpty()) {
            return ToolResult(
                "${app.label}가 설치되어 있지 않아 열지 못했어요. ${installHint(app.packages.first())}",
                isError = true,
            )
        }
        val packageName = installed.first()
        val launchIntent = runCatching { context.packageManager.getLaunchIntentForPackage(packageName) }.getOrNull()
        if (launchIntent != null) {
            val started = runCatching {
                if (context !is Activity) launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
            }
            return if (started.isSuccess) {
                ToolResult("${app.label} 앱을 열었어요 ($packageName).")
            } else {
                ToolResult(
                    "${app.label} 앱 실행 화면을 열지 못했어요 ($packageName): ${started.failureMessage()}",
                    isError = true,
                )
            }
        }
        val store = GalaxyIntents.storeFallbacks(packageName).firstOrNull { start(context, it).isSuccess }
        return if (store != null) {
            ToolResult(
                "${app.label}($packageName)에는 launcher activity가 없어 스토어 상세 화면만 열었어요. " +
                    "앱 화면은 열리지 않았어요. 기기 설정 > 앱에서 직접 실행해 주세요.",
                isError = true,
            )
        } else {
            ToolResult(
                "${app.label}($packageName)을 열지 못했어요. launcher activity와 스토어 앱을 모두 찾지 못했어요. " +
                    "기기 설정 > 앱에서 직접 실행해 주세요.",
                isError = true,
            )
        }
    }

    private fun createEntry(
        context: Context,
        appKey: String,
        mimeType: String,
        title: String?,
        body: String?,
    ): ToolResult {
        val app = GalaxyCatalog.app(appKey)
            ?: return ToolResult("알 수 없는 앱 키예요: $appKey", isError = true)
        if (title.isNullOrBlank() && body.isNullOrBlank()) {
            return ToolResult("제목이나 본문 중 하나는 필요해요.", isError = true)
        }
        val packageName = GalaxyCatalog.firstInstalled(app) { isInstalled(context, it) }
            ?: return ToolResult(
                "${app.label}가 설치되어 있지 않아 새 항목 화면을 열지 못했어요. ${installHint(app.packages.first())}",
                isError = true,
            )
        val spec = when (mimeType) {
            GalaxyIntents.NOTE_MIME_TYPE -> GalaxyIntents.noteInsert(title, body, packageName)
            GalaxyIntents.REMINDER_MIME_TYPE -> GalaxyIntents.reminderInsert(title, body, packageName)
            else -> return ToolResult("지원하지 않는 항목 형식이에요: $mimeType", isError = true)
        }
        val started = start(context, spec)
        if (started.isFailure) {
            return ToolResult(
                "${app.label}가 ACTION_INSERT 요청을 처리할 수 없어 새 항목 화면을 열지 못했어요 " +
                    "($packageName): ${started.failureMessage()}. 앱을 최신 버전으로 업데이트한 뒤 다시 시도해요.",
                isError = true,
            )
        }
        return ToolResult(
            "${app.label} 새 항목 편집 화면을 열었어요 ($packageName). " +
                "이 도구는 화면만 열며 저장 여부는 앱에서 사용자가 직접 확인해야 해요.",
        )
    }

    private fun composeMessage(context: Context, arguments: JsonObject): ToolResult {
        val to = arguments.text("to")?.trim()?.takeIf { it.isNotEmpty() }
            ?: return ToolResult("compose에는 to(받는 번호)가 필요해요.", isError = true)
        if (!PHONE_PATTERN.matches(to)) {
            return ToolResult(
                "받는 번호 형식이 올바르지 않아요: $to. 숫자와 +, -, 괄호, 쉼표만 쓸 수 있어요.",
                isError = true,
            )
        }
        val app = GalaxyCatalog.app("messages")
            ?: return ToolResult("삼성 메시지 카탈로그 항목을 찾지 못했어요.", isError = true)
        val packageName = GalaxyCatalog.firstInstalled(app) { isInstalled(context, it) }
            ?: return ToolResult(
                "삼성 메시지가 설치되어 있지 않아 작성 화면을 열지 못했어요. ${installHint(app.packages.first())}",
                isError = true,
            )
        val started = start(context, GalaxyIntents.smsCompose(to, arguments.text("body"), packageName))
        if (started.isFailure) {
            return ToolResult(
                "삼성 메시지 작성 화면을 열지 못했어요 ($packageName): ${started.failureMessage()}",
                isError = true,
            )
        }
        return ToolResult(
            "삼성 메시지 작성 화면을 열었어요 (sms:$to). " +
                "이 도구는 문자를 보내지 않으며 전송은 사용자가 직접 눌러야 해요.",
        )
    }

    private fun openSettingTargets(
        context: Context,
        label: String,
        targets: List<GalaxySettingTarget>,
        detail: String = "",
    ): ToolResult {
        var lastFailure: String? = null
        for (target in targets) {
            val spec = GalaxyIntents.settings(target)
            val started = start(context, spec)
            if (started.isSuccess) {
                val opened = spec.component ?: spec.action.orEmpty()
                val suffix = detail.takeIf { it.isNotBlank() }?.let { "\n$it" } ?: ""
                return ToolResult(
                    "${label} 설정 화면을 열었어요 ($opened). " +
                        "이 도구는 화면만 열고 값을 바꾸지 않아요. 실제 변경은 화면에서 직접 해 주세요.$suffix",
                )
            }
            lastFailure = started.failureMessage()
        }
        val reason = lastFailure?.let { " 마지막 시도 오류: $it" } ?: ""
        return ToolResult(
            "${label} 설정 화면을 열지 못했어요.$reason One UI 버전에 따라 설정 컴포넌트가 다를 수 있어요. " +
                "설정 앱에서 '$label' 항목을 직접 찾아 주세요.",
            isError = true,
        )
    }

    private fun unsupportedRead(appLabel: String, detail: String): ToolResult =
        ToolResult(
            "$appLabel 읽기는 지원하지 않아요. $detail " +
                "대신 action=open으로 앱을 열 수 있어요.",
            isError = true,
        )

    private fun listInstalledApps(context: Context): ToolResult {
        val installed = LinkedHashMap<String, PackageInfo>()
        for (app in GalaxyCatalog.apps) {
            for (packageName in app.packages) {
                if (installed.containsKey(packageName)) continue
                packageInfo(context, packageName)?.let { installed[packageName] = it }
            }
        }
        if (installed.isEmpty()) {
            return ToolResult(
                "매니페스트에 선언한 삼성 패키지 중 설치된 앱을 찾지 못했어요. " +
                    "삼성 기기가 아니거나 삼성 앱이 설치되어 있지 않을 수 있어요.",
            )
        }
        val lines = installed.entries.joinToString("\n") { (packageName, info) ->
            "- ${applicationLabel(context, info)}: $packageName ${versionName(info)}"
        }
        return ToolResult(
            "설치된 삼성 앱 ${installed.size}개를 찾았어요. " +
                "Android 11 이상의 패키지 공개 범위 때문에 매니페스트에 선언한 패키지만 확인해요.\n$lines",
        )
    }

    private fun deviceStatus(context: Context): ToolResult {
        val samsung = Build.MANUFACTURER.equals("samsung", ignoreCase = true) ||
            Build.BRAND.equals("samsung", ignoreCase = true)
        val semPlatform = if (samsung) semPlatformVersion() else null
        val oneUi = GalaxyCatalog.oneUiVersion(semPlatform)
        val secureFolder = isInstalled(context, GalaxyCatalog.SECURE_FOLDER_PACKAGE)
        val knox = GalaxyCatalog.KNOX_PACKAGES.any { isInstalled(context, it) }
        val securityPatch = Build.VERSION.SECURITY_PATCH.takeIf { it.isNotBlank() } ?: "확인하지 못했어요"
        val lines = listOf(
            "제조사: ${Build.MANUFACTURER}",
            "브랜드: ${Build.BRAND}",
            "모델: ${Build.MODEL}",
            "기기 코드: ${Build.DEVICE}",
            "Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})",
            "빌드: ${Build.DISPLAY}",
            "보안 패치: $securityPatch",
            "One UI: ${oneUi ?: "확인하지 못했어요"}${semPlatform?.let { " (SEM_PLATFORM_INT $it)" } ?: ""}",
            "삼성 기기: ${if (samsung) "예" else "아니요"}",
            "보안 폴더: ${if (secureFolder) "설치됨" else "설치되지 않음"}",
            "Knox 구성 요소: ${if (knox) "설치됨" else "확인되지 않음"}",
        )
        val note = if (samsung) "" else "\n삼성 기기가 아니어서 갤럭시 전용 앱·설정 화면이 없을 수 있어요."
        return ToolResult(lines.joinToString("\n") + note)
    }

    private fun packageInfo(context: Context, packageName: String): PackageInfo? =
        runCatching { context.packageManager.getPackageInfo(packageName, 0) }.getOrNull()

    private fun isInstalled(context: Context, packageName: String): Boolean =
        packageInfo(context, packageName) != null

    private fun versionName(info: PackageInfo): String =
        info.versionName?.takeIf { it.isNotBlank() } ?: "버전 확인 불가"

    private fun applicationLabel(context: Context, info: PackageInfo): String =
        info.applicationInfo?.let { application ->
            runCatching { context.packageManager.getApplicationLabel(application).toString() }.getOrNull()
        }?.takeIf { it.isNotBlank() } ?: info.packageName

    private fun installHint(packageName: String): String =
        "삼성 스토어(${GalaxyIntents.samsungStoreUri(packageName)}) 또는 " +
            "Play 스토어(${GalaxyIntents.playStoreUri(packageName)})에서 설치한 뒤 다시 시도해요."

    private fun start(context: Context, spec: IntentSpec): Result<Unit> = runCatching {
        val intent = spec.toIntent() ?: error("Intent 컴포넌트를 해석하지 못했어요: ${spec.component}")
        if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /**
     * Android Context가 없으면 가짜 성공 대신 명확한 오류를 돌려줘요.
     */
    private inline fun android(block: (Context) -> ToolResult): ToolResult {
        val context = host?.androidContext as? Context
            ?: return ToolResult(
                "Android Context를 사용할 수 없어 갤럭시 기능을 실행할 수 없어요. Android 앱에서 다시 시도해 주세요.",
                isError = true,
            )
        return block(context)
    }

    private fun Result<*>.failureMessage(): String =
        exceptionOrNull()?.let { "${it.javaClass.simpleName}: ${it.message ?: "알 수 없는 오류"}" }
            ?: "알 수 없는 오류"

    private fun semPlatformVersion(): Int? = runCatching {
        Build.VERSION::class.java.getField("SEM_PLATFORM_INT").getInt(null)
    }.getOrNull()

    private companion object {
        val PHONE_PATTERN = Regex("^[+0-9()\\-,\\s]+$")
    }
}

private fun IntentSpec.toIntent(): Intent? {
    val intent = Intent()
    action?.let { intent.action = it }
    data?.let { intent.data = Uri.parse(it) }
    type?.let { intent.type = it }
    packageName?.let { intent.setPackage(it) }
    component?.let { intent.component = ComponentName.unflattenFromString(it) ?: return null }
    extras.forEach { (key, value) -> intent.putExtra(key, value) }
    return intent
}
