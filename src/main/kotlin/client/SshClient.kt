package at.wenzina.client

import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

data class SshResult(val stdout: String, val stderr: String, val code: Int)

class ProcessTimeoutException(message: String) : RuntimeException(message)

class SshClient {

    fun execute(command: String): SshResult {
        val process = ProcessBuilder("/bin/sh", "-c", command)
            .redirectErrorStream(false)
            .start()

        val stdoutFuture = CompletableFuture.supplyAsync { process.inputStream.bufferedReader().readText() }
        val stderrFuture = CompletableFuture.supplyAsync { process.errorStream.bufferedReader().readText() }

        val finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)

        if (!finished) {
            process.destroyForcibly()
            stdoutFuture.cancel(true)
            stderrFuture.cancel(true)
            throw ProcessTimeoutException("Prozess nach ${TIMEOUT_SECONDS}s abgebrochen: $command")
        }

        return SshResult(
            stdout = stdoutFuture.get(5, TimeUnit.SECONDS),
            stderr = stderrFuture.get(5, TimeUnit.SECONDS),
            code   = process.exitValue()
        )
    }

    companion object {
        private const val TIMEOUT_SECONDS = 300L  // 5 Minuten
    }
}