package com.alongside.data.di

import com.alongside.core.database.AlongsideDatabase
import com.alongside.core.database.diaryEntryRepository
import com.alongside.core.database.episodeRepository
import com.alongside.core.database.pairingTripLocalDataSource
import com.alongside.core.database.sync.SyncOperationStore
import com.alongside.core.database.syncOperationStore
import com.alongside.core.database.tripRepository
import com.alongside.core.domain.diary.DiaryEntryRepository
import com.alongside.core.domain.diary.EpisodeRepository
import com.alongside.core.domain.pairing.PairingTripDataSource
import com.alongside.core.domain.trip.TripRepository
import com.alongside.core.network.firestore.FirestoreApi
import com.alongside.core.network.queue.FirestoreSyncNetworkClient
import com.alongside.core.network.queue.SyncNetworkClient
import com.alongside.core.network.queue.SyncQueueProcessor
import com.alongside.data.diary.DiaryEntrySyncEntityBinding
import com.alongside.data.diary.SyncingDiaryEntryRepository
import com.alongside.data.episode.EpisodeSyncEntityBinding
import com.alongside.data.episode.SyncingEpisodeRepository
import com.alongside.data.pairing.FirestorePairingRemoteDataSource
import com.alongside.data.pairing.FirestorePairingTripDataSource
import com.alongside.data.pairing.PairingRemoteDataSource
import com.alongside.data.sync.FirestoreRemoteDocumentReader
import com.alongside.data.sync.RemoteDocumentReader
import com.alongside.data.sync.SyncCoordinator
import com.alongside.data.sync.SyncEntityBinding
import com.alongside.data.trip.SyncingTripRepository
import com.alongside.data.trip.TripSyncEntityBinding
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * The data layer's bindings. Expects the assembling app to provide: the Ktor `HttpClient`,
 * `FirestoreConfig`, `FirestoreTokenProvider`, and `AlongsideDatabase`. The plain Room-backed
 * repositories are built inline from database factories and deliberately not bound -
 * `TripRepository` must resolve to the syncing decorator everywhere outside this module.
 */
public val dataModule: Module =
    module {
        single { FirestoreApi(get(), get(), get()) }
        single<SyncNetworkClient> { FirestoreSyncNetworkClient(get()) }
        single { SyncQueueProcessor(get()) }
        single<SyncOperationStore> { get<AlongsideDatabase>().syncOperationStore() }
        single<RemoteDocumentReader> { FirestoreRemoteDocumentReader(get()) }
        single<TripRepository> {
            SyncingTripRepository(local = get<AlongsideDatabase>().tripRepository(), store = get())
        }
        single<DiaryEntryRepository> {
            SyncingDiaryEntryRepository(local = get<AlongsideDatabase>().diaryEntryRepository(), store = get())
        }
        single<EpisodeRepository> {
            SyncingEpisodeRepository(local = get<AlongsideDatabase>().episodeRepository(), store = get())
        }
        single<SyncEntityBinding> { TripSyncEntityBinding(get<AlongsideDatabase>().tripRepository()) }
        single<SyncEntityBinding> { DiaryEntrySyncEntityBinding(get<AlongsideDatabase>().diaryEntryRepository()) }
        single<SyncEntityBinding> { EpisodeSyncEntityBinding(get<AlongsideDatabase>().episodeRepository()) }
        single {
            SyncCoordinator(
                store = get(),
                processor = get(),
                remoteReader = get(),
                bindings = getAll<SyncEntityBinding>(),
            )
        }
        single<PairingRemoteDataSource> { FirestorePairingRemoteDataSource(get()) }
        single<PairingTripDataSource> {
            FirestorePairingTripDataSource(
                trips = get(),
                localLookup = get<AlongsideDatabase>().pairingTripLocalDataSource(),
                remote = get(),
                syncCoordinator = get(),
            )
        }
    }
