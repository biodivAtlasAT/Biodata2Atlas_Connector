package at.wenzina

import java.util.Properties

data class AppConfig(
    val apiBaseUrl: String,
    val apiAtlasUrl: String,
    val apiUsername: String,
    val apiPassword: String,
    val ingestScript: String,
    val outputDir: String
) {
    companion object {
        fun from(props: Properties) = AppConfig(
            apiBaseUrl   = props.require("api.base.url"),
            apiAtlasUrl  = props.require("api.atlas.url"),
            apiUsername  = props.require("api.username"),
            apiPassword  = props.require("api.password"),
            ingestScript = props.getProperty("ingest.script", "/data/biodata-stack/ativN8NIngest.sh"),
            outputDir    = props.getProperty("output.dir", ".")
        )

        private fun Properties.require(key: String): String =
            getProperty(key) ?: error("Fehlende Pflicht-Property: $key")
    }
}