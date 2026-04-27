package at.wenzina

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.config.Configurator
import java.nio.file.Path
import java.util.Properties
import kotlin.io.path.exists
import kotlin.io.path.reader

fun main(args: Array<String>) = App().main(args)

class App : CliktCommand(name = "biodata2atlas") {

    private val configPath: String by option(
        "-c", "--config",
        help = "Pfad zur Konfigurationsdatei (Properties-Format)",
        envvar = "BIODATA2ATLAS_CONFIG"
    ).default("application.properties")

    private val verbose: Boolean by option(
        "-v", "--verbose",
        help = "Debug-Ausgabe aktivieren (setzt Root-Logger auf DEBUG)"
    ).flag()

    override fun run() {
        if (verbose) Configurator.setRootLevel(Level.DEBUG)

        val path = Path.of(configPath)
        if (!path.exists()) {
            echo("Fehler: Konfigurationsdatei '$configPath' nicht gefunden.", err = true)
            return
        }

        val properties = Properties()
        path.reader().use { properties.load(it) }

        val config = AppConfig.from(properties)
        BiodataSyncWorkflow(config).run()
    }
}