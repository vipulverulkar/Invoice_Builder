package com.invoicebuilder.core.database.repository

import com.invoicebuilder.core.database.dao.*
import com.invoicebuilder.core.database.entity.*
import com.invoicebuilder.core.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

interface ClientRepository {
    fun observeClients(): Flow<List<Client>>
    suspend fun getClientById(id: Long): Client?
    suspend fun saveClient(client: Client): Long
    suspend fun archiveClient(id: Long, isArchived: Boolean)
    suspend fun dedupeClientsByName()
}

class ClientRepositoryImpl(private val clientDao: ClientDao) : ClientRepository {
    override fun observeClients(): Flow<List<Client>> {
        return clientDao.observeActiveClients().map { entities ->
            entities.map { it.toModel() }
        }
    }

    override suspend fun getClientById(id: Long): Client? {
        return clientDao.getClientById(id)?.toModel()
    }

    override suspend fun saveClient(client: Client): Long {
        val entity = client.toEntity()
        val now = Instant.now().toString()
        return if (client.id == 0L) {
            val existing = clientDao.getActiveClientByName(client.name.trim())
            if (existing != null) {
                clientDao.update(
                    entity.copy(
                        id = existing.id,
                        createdAt = existing.createdAt,
                        updatedAt = now
                    )
                )
                existing.id
            } else {
                clientDao.insert(entity.copy(createdAt = now, updatedAt = now))
            }
        } else {
            clientDao.update(entity.copy(updatedAt = now))
            client.id
        }
    }

    override suspend fun archiveClient(id: Long, isArchived: Boolean) {
        clientDao.setArchived(id, isArchived)
    }

    override suspend fun dedupeClientsByName() {
        val duplicates = clientDao.getActiveClients()
            .groupBy { it.name.trim().lowercase() }
            .values
            .filter { it.size > 1 }
            .map { group -> group.drop(1).map { it.id } }
            .flatten()
        duplicates.forEach { id -> clientDao.setArchived(id, true) }
    }

    private fun ClientEntity.toModel() = Client(
        id = id,
        name = name,
        shortName = shortName,
        address = address,
        email = email,
        phone = phone,
        code = code,
        additional = additional,
        vatCode = vatCode,
        peppolEndpointId = peppolEndpointId,
        countryCode = countryCode,
        peppolEndpointSchemeId = peppolEndpointSchemeId,
        buyerReference = buyerReference,
        description = description,
        gstin = gstin,
        tradeName = tradeName,
        stateCode = stateCode,
        tdsSection = tdsSection,
        tdsRate = tdsRate,
        tcsApplicable = tcsApplicable,
        isArchived = isArchived,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun Client.toEntity() = ClientEntity(
        id = id,
        name = name,
        shortName = shortName,
        address = address,
        email = email,
        phone = phone,
        code = code,
        additional = additional,
        vatCode = vatCode,
        peppolEndpointId = peppolEndpointId,
        countryCode = countryCode,
        peppolEndpointSchemeId = peppolEndpointSchemeId,
        buyerReference = buyerReference,
        description = description,
        gstin = gstin,
        tradeName = tradeName,
        stateCode = stateCode,
        tdsSection = tdsSection,
        tdsRate = tdsRate,
        tcsApplicable = tcsApplicable,
        isArchived = isArchived,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

interface BusinessRepository {
    fun observeBusinesses(): Flow<List<Business>>
    suspend fun getBusinessById(id: Long): Business?
    suspend fun saveBusiness(business: Business): Long
    suspend fun archiveBusiness(id: Long, isArchived: Boolean)
}

class BusinessRepositoryImpl(private val businessDao: BusinessDao) : BusinessRepository {
    override fun observeBusinesses(): Flow<List<Business>> {
        return businessDao.observeActiveBusinesses().map { entities ->
            entities.map { it.toModel() }
        }
    }

    override suspend fun getBusinessById(id: Long): Business? {
        return businessDao.getBusinessById(id)?.toModel()
    }

    override suspend fun saveBusiness(business: Business): Long {
        val entity = business.toEntity()
        return if (business.id == 0L) {
            businessDao.insert(entity.copy(createdAt = Instant.now().toString(), updatedAt = Instant.now().toString()))
        } else {
            businessDao.update(entity.copy(updatedAt = Instant.now().toString()))
            business.id
        }
    }

    override suspend fun archiveBusiness(id: Long, isArchived: Boolean) {
        businessDao.setArchived(id, isArchived)
    }

    private fun BusinessEntity.toModel() = Business(
        id = id,
        name = name,
        shortName = shortName,
        address = address,
        role = role,
        email = email,
        phone = phone,
        website = website,
        additional = additional,
        paymentInformation = paymentInformation,
        logo = logo,
        fileSize = fileSize,
        fileType = fileType,
        fileName = fileName,
        vatCode = vatCode,
        code = code,
        peppolEndpointId = peppolEndpointId,
        countryCode = countryCode,
        peppolEndpointSchemeId = peppolEndpointSchemeId,
        description = description,
        gstin = gstin,
        pan = pan,
        stateCode = stateCode,
        registrationType = registrationType?.let { runCatching { GstRegistrationType.valueOf(it) }.getOrNull() },
        declaration = declaration,
        isArchived = isArchived,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun Business.toEntity() = BusinessEntity(
        id = id,
        name = name,
        shortName = shortName,
        address = address,
        role = role,
        email = email,
        phone = phone,
        website = website,
        additional = additional,
        paymentInformation = paymentInformation,
        logo = logo,
        fileSize = fileSize,
        fileType = fileType,
        fileName = fileName,
        vatCode = vatCode,
        code = code,
        peppolEndpointId = peppolEndpointId,
        countryCode = countryCode,
        peppolEndpointSchemeId = peppolEndpointSchemeId,
        description = description,
        gstin = gstin,
        pan = pan,
        stateCode = stateCode,
        registrationType = registrationType?.name,
        declaration = declaration,
        isArchived = isArchived,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

interface ItemRepository {
    fun observeItems(): Flow<List<Item>>
    suspend fun getItemById(id: Long): Item?
    suspend fun saveItem(item: Item): Long
    suspend fun archiveItem(id: Long, isArchived: Boolean)
    suspend fun dedupeItemsByName()
}

class ItemRepositoryImpl(private val itemDao: ItemDao) : ItemRepository {
    override fun observeItems(): Flow<List<Item>> {
        return itemDao.observeActiveItems().map { entities ->
            entities.map { it.toModel() }
        }
    }

    override suspend fun getItemById(id: Long): Item? {
        return itemDao.getItemById(id)?.toModel()
    }

    override suspend fun saveItem(item: Item): Long {
        val entity = item.toEntity()
        val now = Instant.now().toString()
        return if (item.id == 0L) {
            val existing = itemDao.getActiveItemByName(item.name.trim())
            if (existing != null) {
                itemDao.update(
                    entity.copy(
                        id = existing.id,
                        createdAt = existing.createdAt,
                        updatedAt = now
                    )
                )
                existing.id
            } else {
                itemDao.insert(entity.copy(createdAt = now, updatedAt = now))
            }
        } else {
            itemDao.update(entity.copy(updatedAt = now))
            item.id
        }
    }

    override suspend fun archiveItem(id: Long, isArchived: Boolean) {
        itemDao.setArchived(id, isArchived)
    }

    override suspend fun dedupeItemsByName() {
        val duplicates = itemDao.getActiveItems()
            .groupBy { it.name.trim().lowercase() }
            .values
            .filter { it.size > 1 }
            .map { group -> group.drop(1).map { it.id } }
            .flatten()
        duplicates.forEach { id -> itemDao.setArchived(id, true) }
    }

    private fun ItemEntity.toModel() = Item(
        id = id,
        name = name,
        amountCents = amount,
        unitId = unitId,
        categoryId = categoryId,
        description = description,
        hsnCode = hsnCode,
        gstRate = gstRate,
        unitCode = unitCode?.let { runCatching { UnitOfMeasure.valueOf(it) }.getOrNull() },
        itemType = itemType?.let { runCatching { ItemType.valueOf(it) }.getOrNull() },
        isArchived = isArchived,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun Item.toEntity() = ItemEntity(
        id = id,
        name = name,
        amount = amountCents,
        unitId = unitId,
        categoryId = categoryId,
        description = description,
        hsnCode = hsnCode,
        gstRate = gstRate,
        unitCode = unitCode?.name,
        itemType = itemType?.name,
        isArchived = isArchived,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
