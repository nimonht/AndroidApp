package com.example.androidapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Base64
import android.util.Log
import com.example.androidapp.domain.console.OutputStyle
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

/**
 * ADB-to-console bridge that allows the Quizzez MCP server to execute
 * in-app console commands on a real device or emulator in **any build type**
 * (debug or release).
 *
 * ## Why this exists
 *
 * The MCP server is an intentional AI-agent integration layer for Quizzez.
 * It must work in production builds so that agents can interact with real
 * Firebase data, not just a local emulator. Restricting this receiver to
 * debug builds would break that contract.
 *
 * ## Communication protocol
 *
 * **Trigger (MCP server → app):**
 * ```
 * adb shell am broadcast --ordered \
 *   -a com.example.androidapp.CONSOLE_COMMAND \
 *   -n com.example.androidapp/.ConsoleBroadcastReceiver \
 *   -e command "<cmd>"
 * ```
 *
 * **Primary transport — ordered broadcast result data:**
 * The receiver calls [android.content.BroadcastReceiver.PendingResult.setResultData]
 * with a Base64-encoded JSON payload before calling
 * [android.content.BroadcastReceiver.PendingResult.finish].
 * `am broadcast --ordered` blocks until `finish()` is called and then prints:
 * ```
 * Broadcast completed: result=0, data="<base64>"
 * ```
 * The MCP server (`adb_bridge.py`) regex-extracts the Base64 token and decodes
 * it to raw JSON. This transport requires **no file-system access** and works
 * identically in debug and release builds.
 *
 * **Secondary transport — internal file (convenience for manual inspection):**
 * The receiver also writes the JSON payload to [OUTPUT_FILE] inside the app's
 * private `filesDir`. On debug builds this file can be read via
 * `adb shell run-as <package> cat files/console_output.json`. On release builds
 * the file exists but is not accessible to ADB without root; the primary
 * ordered-broadcast transport should be used instead.
 *
 * **Result JSON shape:**
 * ```json
 * {
 *   "success": true,
 *   "exitCode": 0,
 *   "output": [
 *     { "text": "Connection OK", "style": "SUCCESS" },
 *     { "text": "Latency: 42ms",  "style": "NORMAL"  }
 *   ]
 * }
 * ```
 *
 * ## Android 8+ (API 26+) note
 *
 * App-defined custom broadcast actions are **not** subject to the implicit-
 * broadcast restrictions introduced in API 26 (those only apply to system-
 * defined broadcasts). The `-n` component flag in the `am broadcast` call
 * makes the intent explicit anyway, guaranteeing delivery on all API levels.
 *
 * ## Threading
 *
 * [onReceive] is called on the main thread. [goAsync] is used to extend the
 * receiver's lifetime while a coroutine executes the command on [Dispatchers.IO].
 * [android.content.BroadcastReceiver.PendingResult.finish] is always called in
 * a `finally` block so the broadcast is never left pending.
 */
class ConsoleBroadcastReceiver : BroadcastReceiver() {

    private val gson = Gson()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return

        val command = intent.getStringExtra(EXTRA_COMMAND)?.trim()
        if (command.isNullOrEmpty()) {
            Log.w(TAG, "Received $ACTION but '$EXTRA_COMMAND' extra was missing or blank.")
            val errorJson = buildErrorJson("Lenh trong. Truyen them '-e command <lenh>'.")
            writeSidecarFile(context, errorJson)
            // setResultData on the calling thread is safe before goAsync();
            // we return immediately so no goAsync() is needed for this path.
            setResultData(Base64.encodeToString(errorJson.toByteArray(Charsets.UTF_8), Base64.NO_WRAP))
            return
        }

        Log.d(TAG, "Received console command via ADB: \"$command\"")

        // goAsync() extends the BroadcastReceiver lifetime past onReceive() so
        // the coroutine can complete before the process becomes eligible for reclaim.
        // finish() MUST be called in the finally block.
        val pendingResult = goAsync()

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val appContainer =
                    (context.applicationContext as QuizzezApplication).appContainer

                val result = appContainer.commandExecutor.execute(
                    rawInput = command,
                    commandHistory = emptyList(),
                    aliases = emptyMap()
                )

                val payload = CommandOutputPayload(
                    success = result.isSuccess,
                    exitCode = result.exitCode,
                    output = result.output.map { line ->
                        OutputLinePayload(
                            text = line.text,
                            style = line.style.name
                        )
                    }
                )

                val json = gson.toJson(payload)
                val base64 = Base64.encodeToString(
                    json.toByteArray(Charsets.UTF_8),
                    Base64.NO_WRAP
                )

                // Primary transport: ordered broadcast result data.
                // am broadcast --ordered prints:  Broadcast completed: result=0, data="<base64>"
                // The MCP server adb_bridge.py regex-extracts and decodes this.
                pendingResult.setResultData(base64)

                // Secondary transport: write sidecar file for manual debug inspection.
                // On debug builds: adb shell run-as <package> cat files/console_output.json
                writeSidecarFile(context, json)

                Log.d(TAG, "Command \"$command\" completed (exitCode=${result.exitCode}).")

            } catch (e: Exception) {
                Log.e(TAG, "Unhandled exception executing command \"$command\".", e)
                val errorJson = buildErrorJson(
                    "Loi noi bo: ${e.javaClass.simpleName}: ${e.message}"
                )
                val base64 = Base64.encodeToString(
                    errorJson.toByteArray(Charsets.UTF_8),
                    Base64.NO_WRAP
                )
                pendingResult.setResultData(base64)
                writeSidecarFile(context, errorJson)

            } finally {
                pendingResult.finish()
            }
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Builds an error payload JSON string with a single [OutputStyle.ERROR] line.
     *
     * @param message Human-readable error description.
     * @return Serialised JSON string conforming to the result schema.
     */
    private fun buildErrorJson(message: String): String =
        gson.toJson(
            CommandOutputPayload(
                success = false,
                exitCode = 1,
                output = listOf(OutputLinePayload(text = message, style = OutputStyle.ERROR.name))
            )
        )

    /**
     * Atomically writes [json] to [OUTPUT_FILE] inside the app's private
     * `filesDir` for manual inspection on debug builds.
     *
     * The write is performed via a temp-file rename so that a concurrent
     * `adb shell run-as … cat` never observes a partially-written file.
     *
     * Failures here are non-fatal: the primary ordered-broadcast transport
     * is unaffected if the sidecar write fails.
     *
     * @param context Android context used to resolve [Context.getFilesDir].
     * @param json    The JSON string to write.
     */
    private fun writeSidecarFile(context: Context, json: String) {
        try {
            val outputFile = File(context.filesDir, OUTPUT_FILE)
            val tmpFile = File(context.filesDir, "$OUTPUT_FILE.tmp")

            tmpFile.writeText(json, Charsets.UTF_8)

            // Atomic rename so readers never see a partial file.
            if (!tmpFile.renameTo(outputFile)) {
                // renameTo can fail if source and dest are on different mount points;
                // fall back to a direct overwrite.
                outputFile.writeText(json, Charsets.UTF_8)
                tmpFile.delete()
            }

            Log.d(TAG, "Sidecar output written → ${outputFile.absolutePath}")
        } catch (e: IOException) {
            // Non-fatal: primary transport (broadcast result data) is unaffected.
            Log.w(TAG, "Could not write sidecar console_output.json: ${e.message}")
        }
    }

    // -------------------------------------------------------------------------
    // JSON payload data classes (serialised by Gson)
    // -------------------------------------------------------------------------

    /**
     * Top-level JSON payload returned via broadcast result data and written
     * to the sidecar file.
     *
     * @property success  Whether the command completed successfully.
     * @property exitCode Numeric exit code (0 = success).
     * @property output   Ordered list of styled output lines.
     */
    private data class CommandOutputPayload(
        val success: Boolean,
        val exitCode: Int,
        val output: List<OutputLinePayload>
    )

    /**
     * A single styled line within [CommandOutputPayload.output].
     *
     * @property text  Plain text content of the line.
     * @property style Name of the [OutputStyle] enum constant
     *                 (e.g. `"SUCCESS"`, `"ERROR"`, `"NORMAL"`).
     */
    private data class OutputLinePayload(
        val text: String,
        val style: String
    )

    // -------------------------------------------------------------------------
    // Companion
    // -------------------------------------------------------------------------

    companion object {
        private const val TAG = "ConsoleBroadcastReceiver"

        /**
         * Broadcast action sent by the MCP server via `adb shell am broadcast`.
         * Registered in `AndroidManifest.xml` with `android:exported="true"`.
         */
        const val ACTION = "com.example.androidapp.CONSOLE_COMMAND"

        /** Intent extra key carrying the raw command string to execute. */
        const val EXTRA_COMMAND = "command"

        /**
         * File name written inside [Context.getFilesDir] as a secondary,
         * debug-convenience transport.
         *
         * Full path on device:
         * `/data/data/com.example.androidapp/files/console_output.json`
         *
         * Readable on debug builds via:
         * `adb shell run-as com.example.androidapp cat files/console_output.json`
         */
        const val OUTPUT_FILE = "console_output.json"
    }
}
