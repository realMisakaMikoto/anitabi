package cn.anitabi.navigator.data.local

import cn.anitabi.navigator.core.model.StoredTourV2
import java.io.FileNotFoundException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

class RoomSchemaV2Test {
    @Test
    fun `Room and stored tour schemas remain at version two`() {
        val schemaFile = schemaFile()
        val exportedVersion = Files.newBufferedReader(schemaFile, StandardCharsets.UTF_8).use { reader ->
            Json.parseToJsonElement(reader.readText()).jsonObject
                .getValue("database").jsonObject
                .getValue("version").jsonPrimitive.int
        }
        val exportedVersions = Files.newDirectoryStream(schemaFile.parent, "*.json").use { files ->
            files.map { path -> path.fileName.toString().substringBefore('.').toInt() }.sorted()
        }

        assertEquals(2, StoredTourV2.SCHEMA_VERSION)
        assertEquals(2, exportedVersion)
        assertEquals(listOf(1, 2), exportedVersions)
    }

    private fun schemaFile(): Path = sequenceOf(
        Path.of("schemas", DATABASE_NAME, "2.json"),
        Path.of("app", "schemas", DATABASE_NAME, "2.json"),
    ).firstOrNull(Files::isRegularFile)
        ?: throw FileNotFoundException("Room schema 2 export is missing")

    companion object {
        private const val DATABASE_NAME = "cn.anitabi.navigator.data.local.AnitabiDatabase"
    }
}
