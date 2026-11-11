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
        return if (client.id == 0L) {
            clientDao.insert(entity.copy(createdAt = Instant.now().toString(), updatedAt = Instant.now().toString()))
        } else {
            clientDao.update(entity.copy(updatedAt = Instant.now().toString()))
            client.id
        }
    }

    override suspend fun archiveClient(id: Long, isArchived: Boolean) {
        clientDao.setArchived(id, isArchived)
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
        return if (item.id == 0L) {
            itemDao.insert(entity.copy(createdAt = Instant.now().toString(), updatedAt = Instant.now().toString()))
        } else {
            itemDao.update(entity.copy(updatedAt = Instant.now().toString()))
            item.id
        }
    }

    override suspend fun archiveItem(id: Long, isArchived: Boolean) {
        itemDao.setArchived(id, isArchived)
    }

    private fun ItemEntity.toModel() = Item(
        id = id,
        name = name,
        amountCents = amount,
        unitId = unitId,
        categoryId = categoryId,
        description = description,
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
        isArchived = isArchived,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
