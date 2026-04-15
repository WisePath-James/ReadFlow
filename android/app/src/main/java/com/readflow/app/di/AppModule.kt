// Hilt DI Module
package com.readflow.app.di

import android.content.Context
import com.readflow.app.data.local.FileStorageManager
import com.readflow.app.data.local.PreferencesManager
import com.readflow.app.data.repository.AnnotationRepository
import com.readflow.app.data.repository.ArchiveRepository
import com.readflow.app.data.repository.DocumentRepository
import com.readflow.app.data.repository.ReadingProgressRepository
import com.readflow.app.infrastructure.ai.DeepAnalysisEngine
import com.readflow.app.infrastructure.ai.QuickAskEngine
import com.readflow.app.infrastructure.ink.InkStrokeManager
import com.readflow.app.infrastructure.pdf.PDFRendererCore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun providePreferencesManager(
        @ApplicationContext context: Context
    ): PreferencesManager {
        return PreferencesManager(context)
    }

    @Provides
    @Singleton
    fun provideFileStorageManager(
        @ApplicationContext context: Context
    ): FileStorageManager {
        return FileStorageManager(context)
    }

    @Provides
    @Singleton
    fun provideDocumentRepository(
        preferencesManager: PreferencesManager,
        fileStorageManager: FileStorageManager
    ): DocumentRepository {
        return DocumentRepository(preferencesManager, fileStorageManager)
    }

    @Provides
    @Singleton
    fun provideAnnotationRepository(): AnnotationRepository {
        return AnnotationRepository()
    }

    @Provides
    @Singleton
    fun provideArchiveRepository(): ArchiveRepository {
        return ArchiveRepository()
    }

    @Provides
    @Singleton
    fun provideReadingProgressRepository(): ReadingProgressRepository {
        return ReadingProgressRepository()
    }

    @Provides
    @Singleton
    fun providePDFRendererCore(): PDFRendererCore {
        return PDFRendererCore()
    }

    @Provides
    @Singleton
    fun provideInkStrokeManager(): InkStrokeManager {
        return InkStrokeManager()
    }

    @Provides
    @Singleton
    fun provideQuickAskEngine(): QuickAskEngine {
        return QuickAskEngine()
    }

    @Provides
    @Singleton
    fun provideDeepAnalysisEngine(): DeepAnalysisEngine {
        return DeepAnalysisEngine()
    }
}
