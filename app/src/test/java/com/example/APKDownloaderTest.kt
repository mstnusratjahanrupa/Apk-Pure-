package com.example

import com.example.updater.APKDownloader
import com.example.updater.DownloadProgress
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class APKDownloaderTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `test formatBytes helper`() {
        assertEquals("0 B", APKDownloader.formatBytes(0))
        assertEquals("500.0 B", APKDownloader.formatBytes(500))
        assertEquals("1.0 KB", APKDownloader.formatBytes(1024))
        assertEquals("10.0 MB", APKDownloader.formatBytes(10 * 1024 * 1024))
    }

    @Test
    fun `test formatSpeed helper`() {
        assertEquals("0.0 KB/s", APKDownloader.formatSpeed(0))
        assertEquals("500 KB/s", APKDownloader.formatSpeed(500 * 1024))
        assertEquals("2.5 MB/s", APKDownloader.formatSpeed((2.5 * 1024 * 1024).toLong()))
    }

    @Test
    fun `test download successful using mock interceptor`() = runBlocking {
        val testContent = "Test APK binary payload data simulating whatsapp.apk package".toByteArray()
        val mockClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(testContent.toResponseBody("application/vnd.android.package-archive".toMediaType()))
                    .build()
            }
            .build()

        val downloader = APKDownloader(mockClient)
        val targetFile = File(tempFolder.root, "whatsapp.apk")

        val events = downloader.downloadAsFlow(
            url = "https://github.com/installerapp-oss/whatsapp-live-stream-/releases/download/whatsapp-live-stream.apk/whatsapp.apk",
            destinationFile = targetFile
        ).toList()

        assertTrue("Should have Starting event", events.any { it is DownloadProgress.Starting })
        val successEvent = events.filterIsInstance<DownloadProgress.Success>().firstOrNull()
        assertTrue("Should have Success event", successEvent != null)
        assertEquals(targetFile.absolutePath, successEvent?.file?.absolutePath)
        assertEquals(testContent.size.toLong(), successEvent?.totalBytes)
        assertTrue(targetFile.exists())
        assertEquals(testContent.size.toLong(), targetFile.length())
    }

    @Test
    fun `test download HTTP error failure`() = runBlocking {
        val mockClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(404)
                    .message("Not Found")
                    .body("File not found".toResponseBody("text/plain".toMediaType()))
                    .build()
            }
            .build()

        val downloader = APKDownloader(mockClient)
        val targetFile = File(tempFolder.root, "failed.apk")

        val events = downloader.downloadAsFlow(
            url = "https://example.com/invalid.apk",
            destinationFile = targetFile
        ).toList()

        val failureEvent = events.filterIsInstance<DownloadProgress.Failure>().firstOrNull()
        assertTrue("Should have Failure event", failureEvent != null)
        assertTrue(failureEvent!!.errorMessage.contains("404"))
        assertFalse(targetFile.exists())
    }
}
