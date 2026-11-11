package com.invoicebuilder.core.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec
import com.invoicebuilder.core.database.dao.*
import com.invoicebuilder.core.database.entity.*

/**
 * Specification for the v1 -> v2 auto migration.
 *
 * v2 introduces the Indian GST master data (GSTIN, PAN, HSN/SAC, place of
 * supply, IRN/e-way bill references) and immutable line-item snapshots. Room
 * generates the column additions from the entity diff; only the removal of the
 * European e-invoicing flags needs to be declared explicitly.
 */
class AutoMigration1To2 : AutoMigrationSpec

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
    version = 2,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2, spec = AutoMigration1To2::class)
    ]
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
