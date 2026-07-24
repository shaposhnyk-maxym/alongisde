package com.alongside.core.network.storage

import com.alongside.core.domain.pretrip.PreTripPhotoUploadResult
import com.alongside.core.model.SyncStatus
import com.alongside.core.model.pretrip.PreTripPhoto
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Instant

// runBlocking, not runTest: see the M3 note in docs/roadmap.md - runTest's virtual-time scheduler
// falsely times out Ktor's HttpTimeout plugin against MockEngine.
class FirebasePreTripPhotoUploadClientTest {
    private val photo =
        PreTripPhoto(
            id = "photo-1",
            tripId = "trip-1",
            userId = "owner-1",
            uri = "content://photos/photo-1",
            takenAt = Instant.fromEpochMilliseconds(0),
            latitude = 49.0,
            longitude = 24.0,
            syncStatus = SyncStatus.PENDING,
        )

    @Test
    fun `upload success maps to Uploaded with the constructed download URL`() =
        runBlocking {
            val api =
                testFirebaseStorageApi {
                    respondJson(
                        """{"name":"photos/pretrip_photo-1","bucket":"test-bucket","downloadTokens":"abc123"}""",
                    )
                }
            val client = FirebasePreTripPhotoUploadClient(api, FirebaseStorageConfig(bucket = "test-bucket"))

            val result = client.upload(photo, byteArrayOf(1))

            val uploaded = assertIs<PreTripPhotoUploadResult.Uploaded>(result)
            assertEquals(
                "https://firebasestorage.googleapis.com/v0/b/test-bucket/o/photos%2Fpretrip_photo-1" +
                    "?alt=media&token=abc123",
                uploaded.remoteUrl,
            )
        }

    @Test
    fun `upload response with no downloadTokens maps to Failure`() {
        runBlocking {
            val api =
                testFirebaseStorageApi {
                    respondJson("""{"name":"photos/pretrip_photo-1","bucket":"test-bucket"}""")
                }
            val client = FirebasePreTripPhotoUploadClient(api, FirebaseStorageConfig(bucket = "test-bucket"))

            val result = client.upload(photo, byteArrayOf(1))

            assertIs<PreTripPhotoUploadResult.Failure>(result)
        }
    }

    @Test
    fun `HTTP 4xx maps to Failure wrapping ClientError`() {
        runBlocking {
            val api =
                testFirebaseStorageApi {
                    respondJson(
                        """{"error":{"code":403,"message":"Permission denied","status":"PERMISSION_DENIED"}}""",
                        HttpStatusCode.Forbidden,
                    )
                }
            val client = FirebasePreTripPhotoUploadClient(api, FirebaseStorageConfig(bucket = "test-bucket"))

            val result = client.upload(photo, byteArrayOf(1))

            val failure = assertIs<PreTripPhotoUploadResult.Failure>(result)
            assertIs<FirebaseStorageException.ClientError>(failure.cause)
        }
    }

    @Test
    fun `HTTP 5xx maps to Failure wrapping ServerError`() {
        runBlocking {
            val api =
                testFirebaseStorageApi {
                    respondJson(
                        """{"error":{"code":500,"message":"internal error","status":"INTERNAL"}}""",
                        HttpStatusCode.InternalServerError,
                    )
                }
            val client = FirebasePreTripPhotoUploadClient(api, FirebaseStorageConfig(bucket = "test-bucket"))

            val result = client.upload(photo, byteArrayOf(1))

            val failure = assertIs<PreTripPhotoUploadResult.Failure>(result)
            assertIs<FirebaseStorageException.ServerError>(failure.cause)
        }
    }

    @Test
    fun `a request that never responds in time maps to Failure wrapping NetworkTimeout`() {
        runBlocking {
            val api =
                testFirebaseStorageApi(requestTimeoutMillis = 200L) {
                    delay(2_000)
                    respondJson("""{"name":"photos/pretrip_photo-1","bucket":"test-bucket"}""")
                }
            val client = FirebasePreTripPhotoUploadClient(api, FirebaseStorageConfig(bucket = "test-bucket"))

            val result = client.upload(photo, byteArrayOf(1))

            val failure = assertIs<PreTripPhotoUploadResult.Failure>(result)
            assertIs<FirebaseStorageException.NetworkTimeout>(failure.cause)
        }
    }
}
