package at.wenzina

import at.wenzina.client.AtivApiClient
import at.wenzina.client.LoginException
import at.wenzina.client.ProcessTimeoutException
import at.wenzina.client.SshClient
import at.wenzina.model.SyncLogEntry
import org.apache.logging.log4j.LogManager
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class BiodataSyncWorkflow(private val config: AppConfig) {

    private val log = LogManager.getLogger(BiodataSyncWorkflow::class.java)
    private val syncLog = LogManager.getLogger("at.wenzina.SyncLog")

    private val apiClient = AtivApiClient(config)
    private val sshClient = SshClient()

    fun run() {
        val importUUID = UUID.randomUUID().toString()
        log.info("Sync-Lauf gestartet, UUID: $importUUID")

        val authToken = try {
            apiClient.login()
        } catch (e: LoginException) {
            log.error("Login fehlgeschlagen – Sync wird abgebrochen: ${e.message}")
            return
        }
        log.debug("Authentifizierung erfolgreich")

        val projects = apiClient.getProjects(authToken)
        log.debug("${projects.size} Projekte geladen")

        val changedProjects = projects.filter { it.hasChanged }
        log.info("${changedProjects.size} von ${projects.size} Projekten haben Änderungen")

        val processedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val logEntries = mutableListOf<SyncLogEntry>()

        for (project in changedProjects) {
            log.info("Verarbeite Projekt ${project.id} (${project.dataResource})")
            if (project.dataResource == null) {
                log.warn("Projekt ${project.id} hat keine Datenressource und wird nicht verarbeitet!")
                continue
            }
            if (!apiClient.hasZipDownload(authToken, project.id)) {
                log.debug("  Kein ZIP-Download für Projekt ${project.id}, wird übersprungen")
                continue
            }
            try {
                val result = sshClient.execute("${config.ingestScript} ${project.dataResource}")
                log.debug("  Exit-Code: ${result.code}")
                if (result.stderr.isNotBlank()) log.warn("  stderr: ${result.stderr.trim()}")

                logEntries += SyncLogEntry(
                    projectId    = project.id,
                    dataResource = project.dataResource,
                    errorCode    = result.code,
                    uuid         = importUUID,
                    processedAt  = processedAt
                )

                if (result.code == 0) {
                    apiClient.setProjectImported(authToken, project.id)
                    log.debug("  Als importiert markiert")
                } else {
                    log.error("  Ingest fehlgeschlagen (Code ${result.code}), Projekt wird nicht markiert")
                }
            } catch (e: ProcessTimeoutException) {
                log.error("  Timeout bei Projekt ${project.id} (${project.dataResource}): ${e.message}")
                logEntries += SyncLogEntry(
                    projectId    = project.id,
                    dataResource = project.dataResource,
                    errorCode    = -2,
                    uuid         = importUUID,
                    processedAt  = processedAt
                )
            } catch (e: Exception) {
                log.error("  Fehler bei Projekt ${project.id}: ${e.message}")
                logEntries += SyncLogEntry(
                    projectId    = project.id,
                    dataResource = project.dataResource,
                    errorCode    = -1,
                    uuid         = importUUID,
                    processedAt  = processedAt
                )
            }
        }

        writeSyncLog(logEntries)
        log.info("Sync-Lauf beendet, UUID: $importUUID")
    }

    private fun writeSyncLog(entries: List<SyncLogEntry>) {
        val generatedAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
        if (entries.size == 0) {
            syncLog.info("Output generated at: $generatedAt NO projects found!")
        } else {
            entries.forEach { e ->
                val status = if (e.errorCode == 0) "SUCCESS" else "ERROR"
                syncLog.info("${e.uuid},${e.projectId},${e.dataResource},$status,${e.processedAt},$generatedAt")
            }
            log.info("${entries.size} Einträge in Sync-Log geschrieben")
        }
    }
}