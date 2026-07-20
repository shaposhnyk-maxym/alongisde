package com.alongside.core.network.storage

import com.alongside.core.domain.diary.processing.PhotoUploadResult
import com.alongside.core.model.diary.Photo
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Instant

// runBlocking, not runTest: see the M3 note in docs/roadmap.md.
class FirebaseStorageUploadClientTest {
    private val photo =
        Photo(
            id = "p1",
            uri = "content://photos/p1",
            takenAt = Instant.fromEpochMilliseconds(0),
            latitude = 49.0,
            longitude = 24.0,
        )

    @Test
    fun `upload success maps to Uploaded with the constructed download URL`() =
        runBlocking {
            val api =
                testFirebaseStorageApi {
                    respondJson("""{"name":"photos/p1","bucket":"test-bucket","downloadTokens":"abc123"}""")
                }
            val client = FirebaseStorageUploadClient(api, FirebaseStorageConfig(bucket = "test-bucket"))

            val result = client.upload(photo, byteArrayOf(1))

            val uploaded = assertIs<PhotoUploadResult.Uploaded>(result)
            assertEquals(
                "https://firebasestorage.googleapis.com/v0/b/test-bucket/o/photos%2Fp1?alt=media&token=abc123",
                uploaded.remoteUrl,
            )
        }

    @Test
    fun `upload response with no downloadTokens maps to Failure`() {
        runBlocking {
            val api = testFirebaseStorageApi { respondJson("""{"name":"photos/p1","bucket":"test-bucket"}""") }
            val client = FirebaseStorageUploadClient(api, FirebaseStorageConfig(bucket = "test-bucket"))

            val result = client.upload(photo, byteArrayOf(1))

            assertIs<PhotoUploadResult.Failure>(result)
        }
    }

    @Test
    fun `an exception thrown by the underlying api maps to Failure, not a rethrow`() {
        runBlocking {
            val api =
                testFirebaseStorageApi {
                    respondJson(
                        """{"error":{"code":500,"message":"internal error","status":"INTERNAL"}}""",
                        HttpStatusCode.InternalServerError,
                    )
                }
            val client = FirebaseStorageUploadClient(api, FirebaseStorageConfig(bucket = "test-bucket"))

            val result = client.upload(photo, byteArrayOf(1))

            val failure = assertIs<PhotoUploadResult.Failure>(result)
            assertIs<FirebaseStorageException.ServerError>(failure.cause)
        }
    }
}
