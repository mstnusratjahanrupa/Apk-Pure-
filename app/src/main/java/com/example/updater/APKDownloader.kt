package com.example.updater

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Sealed interface representing each state/event of the download process.
 */
sealed interface DownloadProgress {
    data class Starting(val url: String) : DownloadProgress

    data class Progress(
        val percentage: Int, // 0 to 100
        val progressFraction: Float, // 0.0f to 1.0f
        val bytesDownloaded: Long,
        val totalBytes: Long, // -1 if unknown / chunked
        val speedFormatted: String, // e.g. "2.5 MB/s"
        val bytesPerSecond: Long
    ) : DownloadProgress

    data class Success(
        val file: File,
        val totalBytes: Long
    ) : DownloadProgress

    data class Failure(
        val errorMessage: String,
        val cause: Throwable? = null
    ) : DownloadProgress

    data object Cancelled : DownloadProgress
}

/**
 * APKDownloader component using OkHttp to download packages in the background
 * and track the progress percentage, downloaded bytes, total bytes, and download speed.
 */
class APKDownloader(
    private val client: OkHttpClient = createDefaultOkHttpClient()
) {
    @Volatile
    private var activeCall: Call? = null

    companion object {
        private const val DEFAULT_BUFFER_SIZE = 8 * 1024 // 8 KB
        private const val PROGRESS_EMIT_INTERVAL_MS = 150L

        fun createDefaultOkHttpClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .retryOnConnectionFailure(true)
                .build()
        }

        fun formatBytes(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB")
            var size = bytes.toDouble()
            var unitIndex = 0
            while (size >= 1024.0 && unitIndex < units.lastIndex) {
                size /= 1024.0
                unitIndex++
            }
            return String.format(Locale.US, "%.1f %s", size, units[unitIndex])
        }

        fun formatSpeed(bytesPerSecond: Long): String {
            if (bytesPerSecond <= 0) return "0.0 KB/s"
            val mbps = bytesPerSecond / (1024.0 * 1024.0)
            return if (mbps >= 1.0) {
                String.format(Locale.US, "%.1f MB/s", mbps)
            } else {
                val kbps = bytesPerSecond / 1024.0
                String.format(Locale.US, "%.0f KB/s", kbps)
            }
        }
    }

    /**
     * Cancels the active OkHttp call if in progress.
     */
    fun cancelDownload() {
        try {
            activeCall?.cancel()
        } catch (_: Exception) {}
        activeCall = null
    }

    /**
     * Downloads an APK from [url] into [destinationFile] asynchronously as a cold Flow.
     * Emits [DownloadProgress] updates as bytes are streamed.
     *
     * Files are initially saved to a temporary `.download` file to guarantee that incomplete
     * downloads do not leave corrupted APK files.
     */
    fun downloadAsFlow(
        url: String,
        destinationFile: File,
        fallbackContentLength: Long = -1L
    ): Flow<DownloadProgress> = flow {
        emit(DownloadProgress.Starting(url))

        destinationFile.parentFile?.let { parent ->
            if (!parent.exists()) {
                parent.mkdirs()
            }
        }

        val tempFile = File(destinationFile.parentFile, "${destinationFile.name}.download")
        if (tempFile.exists()) {
            tempFile.delete()
        }

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android) AppUpdater/1.0")
            .header("Accept", "application/vnd.android.package-archive, application/octet-stream, */*")
            .build()

        val call = client.newCall(request)
        activeCall = call

        var response: Response? = null
        try {
            response = call.execute()

            if (!response.isSuccessful) {
                val errorMsg = "HTTP error code: ${response.code} (${response.message})"
                emit(DownloadProgress.Failure(errorMsg, IOException(errorMsg)))
                return@flow
            }

            val body = response.body
            if (body == null) {
                val errorMsg = "Empty response body received from server"
                emit(DownloadProgress.Failure(errorMsg, IOException(errorMsg)))
                return@flow
            }

            val contentLength = body.contentLength().let {
                if (it > 0) it else fallbackContentLength
            }

            var bytesDownloaded = 0L
            var lastEmitTime = System.currentTimeMillis()
            var bytesSinceLastEmit = 0L
            var currentSpeedText = "0.0 KB/s"
            var currentBytesPerSec = 0L

            body.byteStream().use { inputStream: InputStream ->
                FileOutputStream(tempFile).use { outputStream: FileOutputStream ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var read: Int

                    while (inputStream.read(buffer).also { read = it } != -1) {
                        // Check coroutine cancellation
                        if (!currentCoroutineContext().isActive || call.isCanceled()) {
                            tempFile.delete()
                            emit(DownloadProgress.Cancelled)
                            return@flow
                        }

                        outputStream.write(buffer, 0, read)
                        bytesDownloaded += read
                        bytesSinceLastEmit += read

                        val now = System.currentTimeMillis()
                        val elapsed = now - lastEmitTime
                        if (elapsed >= PROGRESS_EMIT_INTERVAL_MS) {
                            val seconds = elapsed / 1000.0
                            if (seconds > 0) {
                                currentBytesPerSec = (bytesSinceLastEmit / seconds).toLong()
                                currentSpeedText = formatSpeed(currentBytesPerSec)
                            }

                            val fraction = if (contentLength > 0) {
                                (bytesDownloaded.toFloat() / contentLength.toFloat()).coerceIn(0f, 1f)
                            } else {
                                0f
                            }
                            val percentage = (fraction * 100).toInt().coerceIn(0, 100)

                            emit(
                                DownloadProgress.Progress(
                                    percentage = percentage,
                                    progressFraction = fraction,
                                    bytesDownloaded = bytesDownloaded,
                                    totalBytes = contentLength,
                                    speedFormatted = currentSpeedText,
                                    bytesPerSecond = currentBytesPerSec
                                )
                            )

                            lastEmitTime = now
                            bytesSinceLastEmit = 0L
                        }
                    }
                    outputStream.flush()
                }
            }

            // Verify completed download and atomically rename
            if (destinationFile.exists()) {
                destinationFile.delete()
            }
            val renamed = tempFile.renameTo(destinationFile)
            if (!renamed) {
                // If rename failed across mount points, fallback to copy
                tempFile.copyTo(destinationFile, overwrite = true)
                tempFile.delete()
            }

            try {
                destinationFile.setReadable(true, false)
            } catch (_: Exception) {}

            emit(
                DownloadProgress.Progress(
                    percentage = 100,
                    progressFraction = 1f,
                    bytesDownloaded = destinationFile.length(),
                    totalBytes = destinationFile.length(),
                    speedFormatted = currentSpeedText,
                    bytesPerSecond = currentBytesPerSec
                )
            )

            emit(
                DownloadProgress.Success(
                    file = destinationFile,
                    totalBytes = destinationFile.length()
                )
            )

        } catch (e: Exception) {
            tempFile.delete()
            if (e is CancellationException || call.isCanceled()) {
                emit(DownloadProgress.Cancelled)
            } else {
                emit(
                    DownloadProgress.Failure(
                        errorMessage = e.localizedMessage ?: "Unknown network error during download",
                        cause = e
                    )
                )
            }
        } finally {
            response?.close()
            activeCall = null
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Suspend convenience function that downloads [url] directly to [destinationFile]
     * invoking [onProgress] on each update.
     */
    suspend fun download(
        url: String,
        destinationFile: File,
        fallbackContentLength: Long = -1L,
        onProgress: (DownloadProgress) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        var finalResult: Result<File> = Result.failure(IOException("Download did not start"))

        downloadAsFlow(url, destinationFile, fallbackContentLength).collect { progress ->
            onProgress(progress)
            when (progress) {
                is DownloadProgress.Success -> {
                    finalResult = Result.success(progress.file)
                }
                is DownloadProgress.Failure -> {
                    finalResult = Result.failure(progress.cause ?: IOException(progress.errorMessage))
                }
                is DownloadProgress.Cancelled -> {
                    finalResult = Result.failure(CancellationException("Download was cancelled"))
                }
                else -> Unit
            }
        }

        finalResult
    }
}
