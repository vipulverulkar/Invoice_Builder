package com.invoicebuilder.core.database.dao

import androidx.room.*
import com.invoicebuilder.core.database.entity.*
import com.invoicebuilder.core.model.InvoiceType
import kotlinx.coroutines.flow.Flow

@Dao
interface ClientDao {
    @Query("SELECT * FROM clients WHERE isArchived = 0 ORDER BY name ASC")
    fun observeActiveClients(): Flow<List<ClientEntity>>

    @Query("SELECT * FROM clients ORDER BY name ASC")
    fun observeAllClients(): Flow<List<ClientEntity>>

    @Query("SELECT * FROM clients WHERE id = :id")
    suspend fun getClientById(id: Long): ClientEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(client: ClientEntity): Long

    @Update
    suspend fun update(client: ClientEntity)

    @Query("UPDATE clients SET isArchived = :isArchived WHERE id = :id")
    suspend fun setArchived(id: Long, isArchived: Boolean)
}

@Dao
interface BusinessDao {
    @Query("SELECT * FROM businesses WHERE isArchived = 0 ORDER BY name ASC")
    fun observeActiveBusinesses(): Flow<List<BusinessEntity>>

    @Query("SELECT * FROM businesses WHERE id = :id")
    suspend fun getBusinessById(id: Long): BusinessEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(business: BusinessEntity): Long

    @Update
    suspend fun update(business: BusinessEntity)

    @Query("UPDATE businesses SET isArchived = :isArchived WHERE id = :id")
    suspend fun setArchived(id: Long, isArchived: Boolean)
}

@Dao
interface ItemDao {
    @Query("SELECT * FROM items WHERE isArchived = 0 ORDER BY name ASC")
    fun observeActiveItems(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun getItemById(id: Long): ItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ItemEntity): Long

    @Update
    suspend fun update(item: ItemEntity)

    @Query("UPDATE items SET isArchived = :isArchived WHERE id = :id")
    suspend fun setArchived(id: Long, isArchived: Boolean)
}

@Dao
interface BankDao {
    @Query("SELECT * FROM banks WHERE isArchived = 0 ORDER BY name ASC")
    fun observeActiveBanks(): Flow<List<BankEntity>>

    @Query("SELECT * FROM banks WHERE id = :id")
    suspend fun getBankById(id: Long): BankEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bank: BankEntity): Long

    @Update
    suspend fun update(bank: BankEntity)

    @Query("UPDATE banks SET isArchived = :isArchived WHERE id = :id")
    suspend fun setArchived(id: Long, isArchived: Boolean)
}

@Dao
interface CurrencyDao {
    @Query("SELECT * FROM currencies WHERE isArchived = 0 ORDER BY code ASC")
    fun observeActiveCurrencies(): Flow<List<CurrencyEntity>>

    @Query("SELECT * FROM currencies WHERE id = :id")
    suspend fun getCurrencyById(id: Long): CurrencyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(currency: CurrencyEntity): Long
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
    fun observeSettings(): Flow<SettingsEntity?>

    @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
    suspend fun getSettings(): SettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: SettingsEntity)
}

@Dao
interface InvoiceDao {
    @Query("SELECT * FROM invoices WHERE isArchived = 0 AND invoiceType = :type ORDER BY issuedAt DESC")
    fun observeInvoicesByType(type: InvoiceType): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE id = :id")
    suspend fun getInvoiceById(id: Long): InvoiceEntity?

    @Query("SELECT * FROM invoice_items WHERE parentInvoiceId = :invoiceId")
    suspend fun getInvoiceItems(invoiceId: Long): List<InvoiceItemEntity>

    @Query("SELECT * FROM invoice_payments WHERE parentInvoiceId = :invoiceId ORDER BY paidAt ASC")
    fun observePayments(invoiceId: Long): Flow<List<InvoicePaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertInvoice(invoice: InvoiceEntity): Long

    @Update
    suspend fun updateInvoice(invoice: InvoiceEntity)

    @Query("UPDATE invoices SET isArchived = 1 WHERE id = :id")
    suspend fun archiveInvoice(id: Long)

    @Insert
    suspend fun insertItems(items: List<InvoiceItemEntity>)

    @Insert
    suspend fun insertPayment(payment: InvoicePaymentEntity): Long

    // Snapshots Insertion
    @Insert
    suspend fun insertBusinessSnapshot(snapshot: InvoiceBusinessSnapshotEntity)

    @Insert
    suspend fun insertClientSnapshot(snapshot: InvoiceClientSnapshotEntity)

    @Insert
    suspend fun insertCurrencySnapshot(snapshot: InvoiceCurrencySnapshotEntity)

    @Insert
    suspend fun insertBankSnapshot(snapshot: InvoiceBankSnapshotEntity)

    @Insert
    suspend fun insertCustomization(customization: InvoiceCustomizationEntity)

    @Insert
    suspend fun insertLayoutSnapshot(snapshot: InvoiceLayoutSnapshotEntity)

    @Insert
    suspend fun insertItemSnapshots(snapshots: List<InvoiceItemSnapshotEntity>)

    // Sequence Generator
    @Query("SELECT * FROM invoice_sequences WHERE businessId = :businessId AND clientId = :clientId AND invoiceType = :type LIMIT 1")
    suspend fun getSequence(businessId: Long, clientId: Long, type: InvoiceType): InvoiceSequenceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSequence(sequence: InvoiceSequenceEntity)
}
