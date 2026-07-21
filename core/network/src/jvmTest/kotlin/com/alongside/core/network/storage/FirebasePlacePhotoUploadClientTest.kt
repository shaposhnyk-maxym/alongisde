package com.alongside.core.network.storage

import com.alongside.core.domain.place.importing.PlacePhotoUploadResult
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

// runBlocking, not runTest: see the M3 note in docs/roadmap.md.
class FirebasePlacePhotoUploadClientTest {
    @Test
    fun `upload success maps to Uploaded with the constructed download URL`() =
        runBlocking {
            val api =
                testFirebaseStorageApi {
                    respondJson(
                        """{"name":"photos/place_place-1_0","bucket":"test-bucket","downloadTokens":"abc123"}""",
                    )
                }
            val client = FirebasePlacePhotoUploadClient(api, FirebaseStorageConfig(bucket = "test-bucket"))

            val result = client.upload(placeCandidateId = "place-1", photoIndex = 0, bytes = byteArrayOf(1))

            val uploaded = assertIs<PlacePhotoUploadResult.Uploaded>(result)
            assertEquals(
                "https://firebasestorage.googleapis.com/v0/b/test-bucket/o/photos%2Fplace_place-1_0" +
                    "?alt=media&token=abc123",
                uploaded.remoteUrl,
            )
        }

    @Test
    fun `upload response with no downloadTokens maps to Failure`() {
        runBlocking {
            val api =
                testFirebaseStorageApi {
                    respondJson("""{"name":"photos/place_place-1_0","bucket":"test-bucket"}""")
                }
            val client = FirebasePlacePhotoUploadClient(api, FirebaseStorageConfig(bucket = "test-bucket"))

            val result = client.upload(placeCandidateId = "place-1", photoIndex = 0, bytes = byteArrayOf(1))

            assertIs<PlacePhotoUploadResult.Failure>(result)
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
            val client = FirebasePlacePhotoUploadClient(api, FirebaseStorageConfig(bucket = "test-bucket"))

            val result = client.upload(placeCandidateId = "place-1", photoIndex = 0, bytes = byteArrayOf(1))

            val failure = assertIs<PlacePhotoUploadResult.Failure>(result)
            assertIs<FirebaseStorageException.ServerError>(failure.cause)
        }
    }
}
