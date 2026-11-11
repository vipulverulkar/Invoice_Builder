package com.invoicebuilder.mobile.di

import android.content.Context
import androidx.room.Room
import com.invoicebuilder.core.database.AppDatabase
import com.invoicebuilder.core.database.dao.*
import com.invoicebuilder.core.database.repository.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "invoice_builder.db"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun provideClientDao(db: AppDatabase): ClientDao = db.clientDao()

    @Provides
    fun provideBusinessDao(db: AppDatabase): BusinessDao = db.businessDao()

    @Provides
    fun provideItemDao(db: AppDatabase): ItemDao = db.itemDao()

    @Provides
    fun provideBankDao(db: AppDatabase): BankDao = db.bankDao()

    @Provides
    fun provideCurrencyDao(db: AppDatabase): CurrencyDao = db.currencyDao()

    @Provides
    fun provideSettingsDao(db: AppDatabase): SettingsDao = db.settingsDao()

    @Provides
    fun provideInvoiceDao(db: AppDatabase): InvoiceDao = db.invoiceDao()

    @Provides
    @Singleton
    fun provideClientRepository(dao: ClientDao): ClientRepository = ClientRepositoryImpl(dao)

    @Provides
    @Singleton
    fun provideBusinessRepository(dao: BusinessDao): BusinessRepository = BusinessRepositoryImpl(dao)

    @Provides
    @Singleton
    fun provideItemRepository(dao: ItemDao): ItemRepository = ItemRepositoryImpl(dao)

    @Provides
    @Singleton
    fun provideInvoiceRepository(dao: InvoiceDao): InvoiceRepository = InvoiceRepositoryImpl(dao)
}
