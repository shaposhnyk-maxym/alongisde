package com.alongside.data.pairing

import com.alongside.core.model.trip.Trip
import com.alongside.core.network.firestore.FirestoreApi
import com.alongside.core.network.firestore.model.CollectionSelector
import com.alongside.core.network.firestore.model.CompositeFilter
import com.alongside.core.network.firestore.model.FieldFilter
import com.alongside.core.network.firestore.model.FieldReference
import com.alongside.core.network.firestore.model.FirestoreValue
import com.alongside.core.network.firestore.model.QueryFilter
import com.alongside.core.network.firestore.model.StructuredQuery
import com.alongside.data.trip.TripFirestoreMapper

public class FirestorePairingRemoteDataSource(
    private val api: FirestoreApi,
) : PairingRemoteDataSource {
    override suspend fun findTripByInviteCode(code: String): Trip? =
        api
            .runQuery(
                StructuredQuery(
                    from = listOf(CollectionSelector(TripFirestoreMapper.COLLECTION_PATH)),
                    where = QueryFilter(fieldFilter = equalsFilter("inviteCode", code)),
                    limit = 1,
                ),
            ).firstOrNull()
            ?.let(TripFirestoreMapper::fromDocument)

    override suspend fun findTripByUserId(userId: String): Trip? =
        api
            .runQuery(
                StructuredQuery(
                    from = listOf(CollectionSelector(TripFirestoreMapper.COLLECTION_PATH)),
                    where =
                        QueryFilter(
                            compositeFilter =
                                CompositeFilter(
                                    op = "OR",
                                    filters =
                                        listOf(
                                            QueryFilter(fieldFilter = equalsFilter("ownerId", userId)),
                                            QueryFilter(fieldFilter = equalsFilter("memberId", userId)),
                                        ),
                                ),
                        ),
                    limit = 1,
                ),
            ).firstOrNull()
            ?.let(TripFirestoreMapper::fromDocument)

    private fun equalsFilter(
        fieldPath: String,
        value: String,
    ): FieldFilter =
        FieldFilter(
            field = FieldReference(fieldPath),
            op = "EQUAL",
            value = FirestoreValue.StringValue(value),
        )
}
