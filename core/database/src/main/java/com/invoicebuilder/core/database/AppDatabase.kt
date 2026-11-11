package com.invoicebuilder.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.invoicebuilder.core.database.dao.*
import com.invoicebuilder.core.database.entity.*

@Database(
    entities = [
        SettingsEntity::class,
        BusinessEntity::class,
        ClientEntity::class,
        UnitEntity::class,
        CategoryEntity::class,
        CurrencyEntity::class,
        ItemEntity::class,
        LayoutEntity::class,
        StyleProfileEntity::class,
        BankEntity::class,
        PresetEntity::class,
        InvoiceEntity::class,
        InvoiceItemEntity::class,
        InvoicePaymentEntity::class,
        InvoiceSequenceEntity::class,
        InvoiceBusinessSnapshotEntity::class,
        InvoiceClientSnapshotEntity::class,
        InvoiceCurrencySnapshotEntity::class,
        InvoiceBankSnapshotEntity::class,
        InvoiceCustomizationEntity::class,
        InvoiceStyleProfileSnapshotEntity::class,
        InvoiceItemSnapshotEntity::class,
        InvoiceLayoutSnapshotEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun clientDao(): ClientDao
    abstract fun businessDao(): BusinessDao
    abstract fun itemDao(): ItemDao
    abstract fun bankDao(): BankDao
    abstract fun currencyDao(): CurrencyDao
    abstract fun settingsDao(): SettingsDao
    abstract fun invoiceDao(): InvoiceDao
}
