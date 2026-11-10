package com.sleepysoong.logent.galaxy

import com.sleepysoong.logent.plugin.ToolAccess
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GalaxyPluginTest {
    private val plugin = GalaxyPlugin()

    @Test
    fun `플러그인 식별자는 계약과 같아요`() {
        assertEquals("com.sleepysoong.logent.galaxy", plugin.id)
        assertEquals("Galaxy", plugin.name)
        assertEquals(BuildConfig.PLUGIN_VERSION, plugin.version)
        assertEquals(4, plugin.apiVersion)
    }

    @Test
    fun `도구 6개는 쓰기 권한이고 서브에이전트에 노출되지 않아요`() {
        val tools = plugin.tools()

        assertEquals(
            listOf(
                "galaxy_apps",
                "galaxy_notes",
                "galaxy_reminder",
                "galaxy_messages",
                "galaxy_settings",
                "galaxy_quick_actions",
            ),
            tools.map { it.name },
        )
        tools.forEach { tool ->
            assertEquals("function", tool.spec.type)
            assertEquals("object", tool.spec.function.parameters["type"]!!.jsonPrimitive.content)
            assertEquals("쓰기 도구: ${tool.name}", setOf(ToolAccess.WRITE), tool.access)
            assertFalse("자식 노출: ${tool.name}", tool.childVisible)
            assertTrue("빈 라벨: ${tool.name}", tool.label.isNotBlank())
            assertTrue("빈 설명: ${tool.name}", tool.spec.function.description.isNotBlank())
            assertTrue("action 필수: ${tool.name}", required(tool.name).contains("action"))
        }
        assertEquals(
            mapOf(
                "galaxy_apps" to "갤럭시 앱",
                "galaxy_notes" to "삼성 노트",
                "galaxy_reminder" to "삼성 리마인더",
                "galaxy_messages" to "삼성 메시지",
                "galaxy_settings" to "갤럭시 설정",
                "galaxy_quick_actions" to "갤럭시 빠른 동작",
            ),
            tools.associate { it.name to it.label },
        )
    }

    @Test
    fun `galaxy_apps의 app enum은 카탈로그 키와 같아요`() {
        assertEquals(GalaxyCatalog.appKeys, enumOf("galaxy_apps", "app"))
        assertEquals(listOf("open", "list", "status"), enumOf("galaxy_apps", "action"))
    }

    @Test
    fun `galaxy_settings의 section enum은 카탈로그 섹션과 같아요`() {
        assertEquals(GalaxyCatalog.settingSectionKeys, enumOf("galaxy_settings", "section"))
        assertEquals(listOf("open"), enumOf("galaxy_settings", "action"))
    }

    @Test
    fun `설정 도구는 section을 필수로 받아요`() {
        assertEquals(listOf("action", "section"), required("galaxy_settings"))
    }

    @Test
    fun `노트와 리마인더는 지원하지 않는 list를 명시적으로 선언해요`() {
        assertEquals(listOf("open", "create", "list"), enumOf("galaxy_notes", "action"))
        assertEquals(listOf("open", "create", "list"), enumOf("galaxy_reminder", "action"))
        assertTrue(properties("galaxy_notes").containsKey("title"))
        assertTrue(properties("galaxy_notes").containsKey("body"))
        assertTrue(properties("galaxy_reminder").containsKey("title"))
    }

    @Test
    fun `메시지 도구는 작성·검색 한계를 노출해요`() {
        assertEquals(listOf("open", "compose", "search"), enumOf("galaxy_messages", "action"))
        assertTrue(properties("galaxy_messages").containsKey("to"))
        assertTrue(properties("galaxy_messages").containsKey("body"))
    }

    @Test
    fun `빠른 동작 enum은 네 가지 공개 동작만 가져요`() {
        assertEquals(
            listOf("bixby", "smartthings", "modes", "dex_mode"),
            enumOf("galaxy_quick_actions", "action"),
        )
    }

    private fun toolArguments(name: String) =
        plugin.tools().single { it.name == name }.spec.function.parameters

    private fun properties(name: String) =
        toolArguments(name)["properties"]!!.jsonObject

    private fun required(name: String): List<String> =
        toolArguments(name)["required"]?.jsonArray.orEmpty().map { it.jsonPrimitive.content }

    private fun enumOf(tool: String, field: String): List<String> =
        properties(tool)[field]!!.jsonObject["enum"]!!.jsonArray.map { it.jsonPrimitive.content }
}
