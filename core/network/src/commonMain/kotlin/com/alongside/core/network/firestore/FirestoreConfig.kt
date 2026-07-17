package com.alongside.core.network.firestore

public class FirestoreConfig(
    public val projectId: String,
    public val databaseId: String = "(default)",
) {
    public val documentsBaseUrl: String
        get() = "https://firestore.googleapis.com/v1/projects/$projectId/databases/$databaseId/documents"
}
