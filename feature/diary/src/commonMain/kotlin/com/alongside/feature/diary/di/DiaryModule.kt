package com.alongside.feature.diary.di

import com.alongside.core.domain.diary.processing.EpisodeProcessingPipeline
import com.alongside.feature.diary.capture.PhotoByteReader
import com.alongside.feature.diary.capture.PhotoCompressor
import com.alongside.feature.diary.presentation.DiaryCaptureCoordinator
import com.alongside.feature.diary.presentation.DiaryTimelineContainer
import com.alongside.feature.diary.presentation.DiaryTimelineDataSource
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

public val diaryFeatureModule =
    module {
        single {
            val photoByteReader = get<PhotoByteReader>()
            val photoCompressor = get<PhotoCompressor>()
            EpisodeProcessingPipeline(
                geocodingClient = get(),
                visionDescriptionClient = get(),
                // A real device crashed with OutOfMemoryError JSON-encoding an uncompressed
                // ~10-20MB camera photo straight into the Gemini request - Gemini's captioning
                // doesn't need full sensor resolution, so this now downscales/compresses first.
                imageBytesLoader = { photo -> photoCompressor.compress(photoByteReader.readBytes(photo.uri)) },
                photoUploadClient = get(),
            )
        }
        single { DiaryCaptureCoordinator(get(), get(), get(), get(), get(), get()) }
        single { DiaryTimelineDataSource(get(), get(), get(), get(), get()) }
        viewModel { DiaryTimelineContainer(get(), get(), get()) }
    }
