/**
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.broadleafcommerce.inventory.admin.server.service.handler;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadleafcommerce.admin.server.service.handler.SkuCustomPersistenceHandler;
import org.broadleafcommerce.common.exception.ServiceException;
import org.broadleafcommerce.common.presentation.client.SupportedFieldType;
import org.broadleafcommerce.common.presentation.client.VisibilityEnum;
import org.broadleafcommerce.core.catalog.domain.Sku;
import org.broadleafcommerce.inventory.dao.InventoryDao;
import org.broadleafcommerce.inventory.domain.Inventory;
import org.broadleafcommerce.inventory.domain.InventoryImpl;
import org.broadleafcommerce.inventory.exception.ConcurrentInventoryModificationException;
import org.broadleafcommerce.inventory.service.FulfillmentLocationService;
import org.broadleafcommerce.inventory.service.InventoryService;
import org.broadleafcommerce.openadmin.client.dto.BasicFieldMetadata;
import org.broadleafcommerce.openadmin.client.dto.ClassMetadata;
import org.broadleafcommerce.openadmin.client.dto.DynamicResultSet;
import org.broadleafcommerce.openadmin.client.dto.Entity;
import org.broadleafcommerce.openadmin.client.dto.FieldMetadata;
import org.broadleafcommerce.openadmin.client.dto.MergedPropertyType;
import org.broadleafcommerce.openadmin.client.dto.PersistencePackage;
import org.broadleafcommerce.openadmin.client.dto.PersistencePerspective;
import org.broadleafcommerce.openadmin.client.dto.Property;
import org.broadleafcommerce.openadmin.server.cto.BaseCtoConverter;
import org.broadleafcommerce.openadmin.server.dao.DynamicEntityDao;
import org.broadleafcommerce.openadmin.server.service.handler.CustomPersistenceHandlerAdapter;
import org.broadleafcommerce.openadmin.server.service.persistence.module.InspectHelper;
import org.broadleafcommerce.openadmin.server.service.persistence.module.RecordHelper;

import com.anasoft.os.daofusion.criteria.PersistentEntityCriteria;
import com.anasoft.os.daofusion.cto.client.CriteriaTransferObject;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

public class InventoryCustomPersistenceHandler extends CustomPersistenceHandlerAdapter {

    private static final Log LOG = LogFactory.getLog(InventoryCustomPersistenceHandler.class);

    private static final Integer MAX_RETRIES = 5;

    @Resource(name = "blInventoryService")
    protected InventoryService inventoryService;

    @Resource(name = "blInventoryDao")
    protected InventoryDao inventoryDao;

    @Resource(name = "blFulfillmentLocationService")
    protected FulfillmentLocationService fulfillmentLocationService;

    @Resource(name = "blAdminInventoryPersister")
    protected AdminInventoryPersister inventoryPersister;

    protected static final String QUANTITY_AVAILABLE_CHANGE_FIELD_NAME = "quantityAvailableChange";
    protected static final String QUANTITY_ON_HAND_CHANGE_FIELD_NAME = "quantityOnHandChange";

    @Override
    public Boolean canHandleUpdate(PersistencePackage persistencePackage) {
        String className = persistencePackage.getCeilingEntityFullyQualifiedClassname();
        String[] customCriteria = persistencePackage.getCustomCriteria();
        return customCriteria != null && customCriteria.length > 0 && Inventory.class.getName().equals(className) && "inventoryUpdate".equals(customCriteria[0]);
    }

    @Override
    public Boolean canHandleInspect(PersistencePackage persistencePackage) {
        return canHandleUpdate(persistencePackage);
    }

    @Override
    public Boolean canHandleFetch(PersistencePackage persistencePackage) {
        return canHandleUpdate(persistencePackage);
    }

    @Override
    public Boolean canHandleAdd(PersistencePackage persistencePackage) {
        return canHandleUpdate(persistencePackage);
    }

    @Override
    public DynamicResultSet inspect(PersistencePackage persistencePackage, DynamicEntityDao dynamicEntityDao, InspectHelper helper) throws ServiceException {

        try {

            PersistencePerspective persistencePerspective = persistencePackage.getPersistencePerspective();
            Map<MergedPropertyType, Map<String, FieldMetadata>> allMergedProperties = new HashMap<MergedPropertyType, Map<String, FieldMetadata>>();

            //retrieve the default properties for Inventory
            Map<String, FieldMetadata> properties = helper.getSimpleMergedProperties(Inventory.class.getName(), persistencePerspective);

            //create a new field to hold change in quantity available
            BasicFieldMetadata quantityAvailableChangeMetadata = new BasicFieldMetadata();
            quantityAvailableChangeMetadata.setFieldType(SupportedFieldType.INTEGER);
            quantityAvailableChangeMetadata.setMutable(true);
            quantityAvailableChangeMetadata.setInheritedFromType(InventoryImpl.class.getName());
            quantityAvailableChangeMetadata.setAvailableToTypes(new String[] { InventoryImpl.class.getName() });
            quantityAvailableChangeMetadata.setForeignKeyCollection(false);
            quantityAvailableChangeMetadata.setMergedPropertyType(MergedPropertyType.PRIMARY);
            quantityAvailableChangeMetadata.setName(QUANTITY_AVAILABLE_CHANGE_FIELD_NAME);
            quantityAvailableChangeMetadata.setFriendlyName("quantityAvailableChange");
            quantityAvailableChangeMetadata.setHelpText("quantityAvailableChangeHelp");
            quantityAvailableChangeMetadata.setGroup("Quantities");
            quantityAvailableChangeMetadata.setOrder(6);
            quantityAvailableChangeMetadata.setExplicitFieldType(SupportedFieldType.INTEGER);
            quantityAvailableChangeMetadata.setProminent(false);
            quantityAvailableChangeMetadata.setBroadleafEnumeration("");
            quantityAvailableChangeMetadata.setReadOnly(false);
            quantityAvailableChangeMetadata.setVisibility(VisibilityEnum.GRID_HIDDEN);
            quantityAvailableChangeMetadata.setExcluded(false);

            properties.put(QUANTITY_AVAILABLE_CHANGE_FIELD_NAME, quantityAvailableChangeMetadata);

            //create a new field to hold change in quantity available
            BasicFieldMetadata quantityOnHandChangeMetadata = new BasicFieldMetadata();
            quantityOnHandChangeMetadata.setFieldType(SupportedFieldType.INTEGER);
            quantityOnHandChangeMetadata.setMutable(true);
            quantityOnHandChangeMetadata.setInheritedFromType(InventoryImpl.class.getName());
            quantityOnHandChangeMetadata.setAvailableToTypes(new String[] { InventoryImpl.class.getName() });
            quantityOnHandChangeMetadata.setForeignKeyCollection(false);
            quantityOnHandChangeMetadata.setMergedPropertyType(MergedPropertyType.PRIMARY);
            quantityOnHandChangeMetadata.setName(QUANTITY_ON_HAND_CHANGE_FIELD_NAME);
            quantityOnHandChangeMetadata.setFriendlyName("quantityOnHandChange");
            quantityOnHandChangeMetadata.setHelpText("quantityOnHandChangeHelp");
            quantityOnHandChangeMetadata.setGroup("Quantities");
            quantityOnHandChangeMetadata.setOrder(7);
            quantityOnHandChangeMetadata.setExplicitFieldType(SupportedFieldType.INTEGER);
            quantityOnHandChangeMetadata.setProminent(false);
            quantityOnHandChangeMetadata.setBroadleafEnumeration("");
            quantityOnHandChangeMetadata.setReadOnly(false);
            quantityOnHandChangeMetadata.setVisibility(VisibilityEnum.GRID_HIDDEN);
            quantityOnHandChangeMetadata.setExcluded(false);

            properties.put(QUANTITY_ON_HAND_CHANGE_FIELD_NAME, quantityOnHandChangeMetadata);

            //add in the consolidated product options field
            FieldMetadata options = SkuCustomPersistenceHandler.createConsolidatedOptionField(InventoryImpl.class);
            options.setOrder(3);
            properties.put(SkuCustomPersistenceHandler.CONSOLIDATED_PRODUCT_OPTIONS_FIELD_NAME, options);

            allMergedProperties.put(MergedPropertyType.PRIMARY, properties);
            Class<?>[] entityClasses = dynamicEntityDao.getAllPolymorphicEntitiesFromCeiling(Inventory.class);
            ClassMetadata mergedMetadata = helper.getMergedClassMetadata(entityClasses, allMergedProperties);

            return new DynamicResultSet(mergedMetadata, null, null);

        } catch (Exception e) {
            String className = persistencePackage.getCeilingEntityFullyQualifiedClassname();
            ServiceException ex = new ServiceException("Unable to retrieve inspection results for " + className, e);
            LOG.error("Unable to retrieve inspection results for " + className, ex);
            throw ex;
        }
    }

    @Override
    public DynamicResultSet fetch(PersistencePackage persistencePackage, CriteriaTransferObject cto, DynamicEntityDao dynamicEntityDao, RecordHelper helper) throws ServiceException {
        String ceilingEntityFullyQualifiedClassname = persistencePackage.getCeilingEntityFullyQualifiedClassname();
        try {
            PersistencePerspective persistencePerspective = persistencePackage.getPersistencePerspective();
            //get the default properties from Sku and its subclasses
            Map<String, FieldMetadata> originalProps = helper.getSimpleMergedProperties(Inventory.class.getName(), persistencePerspective);

            //Pull back the Skus based on the criteria from the client
            BaseCtoConverter ctoConverter = helper.getCtoConverter(persistencePerspective, cto,
                    ceilingEntityFullyQualifiedClassname, originalProps);
            PersistentEntityCriteria queryCriteria = ctoConverter.convert(cto, ceilingEntityFullyQualifiedClassname);

            List<Serializable> records = dynamicEntityDao.query(queryCriteria,
                    Class.forName(persistencePackage.getCeilingEntityFullyQualifiedClassname()));

            //Convert Inventory into the client-side Entity representation
            Entity[] payload = helper.getRecords(originalProps, records);
            for (int i = 0; i < payload.length; i++) {
                Entity entity = payload[i];
                Inventory inventory = (Inventory) records.get(i);

                correctSkuProperties(entity, inventory.getSku(), helper);
            }

            int totalRecords = helper.getTotalRecords(persistencePackage, cto, ctoConverter);

            return new DynamicResultSet(null, payload, totalRecords);
        } catch (Exception e) {
            throw new ServiceException("There was a problem fetching inventory. See server logs for more details");
        }
    }

    @Override
    public Entity add(PersistencePackage persistencePackage, DynamicEntityDao dynamicEntityDao, RecordHelper helper) throws ServiceException {
        Entity entity = persistencePackage.getEntity();
        try {
            //Fill out the Sku instance from the form
            PersistencePerspective persistencePerspective = persistencePackage.getPersistencePerspective();
            Inventory adminInstance = (Inventory) Class.forName(entity.getType()[0]).newInstance();
            Map<String, FieldMetadata> adminProperties = helper.getSimpleMergedProperties(Inventory.class.getName(), persistencePerspective);
            adminInstance = (Inventory) helper.createPopulatedInstance(adminInstance, entity, adminProperties, false);

            //persist the newly-created Inventory object
            adminInstance = (Inventory) dynamicEntityDao.persist(adminInstance);

            Entity result = helper.getRecord(adminProperties, adminInstance, null, null);

            correctSkuProperties(result, adminInstance.getSku(), helper);

            return result;
        } catch (Exception e) {
            LOG.error("Unable to update entity for " + entity.getType()[0], e);
            throw new ServiceException("Unable to update entity for " + entity.getType()[0], e);
        }
    }

    @Override
    public Entity update(PersistencePackage persistencePackage, DynamicEntityDao dynamicEntityDao, RecordHelper helper) throws ServiceException {

        Entity entity  = persistencePackage.getEntity();

        try {
            PersistencePerspective persistencePerspective = persistencePackage.getPersistencePerspective();
            Map<String, FieldMetadata> adminProperties = helper.getSimpleMergedProperties(Inventory.class.getName(), persistencePerspective);
            Object primaryKey = helper.getPrimaryKey(entity, adminProperties);

            // There is a retry policy set in case of concurrent update exceptions where several
            // requests would try to update the inventory at the same time. The call to decrement inventory,
            // by default creates a new transaction because repeatable reads would occur if it were called
            // inside of the same transaction. Essentially, we want to try to transactionally decrement the
            // inventory, but if it fails due to locking, then we need to leave the transaction and re-read
            // the data to ensure repeatable reads don't prevent us from getting the freshest data. The
            // retry count is in place to handle higher concurrency situations where there may be more than one
            // failure.
            Inventory adminInstance = null;
            int retryCount = 0;
            while (retryCount < MAX_RETRIES) {
                try {
                    //start up a new transaction for reading and updating, cannot use this current transaction because of
                    //repeatable reads
                    adminInstance = inventoryPersister.saveAdminInventory((Long) primaryKey, entity, adminProperties, helper);
                    break;
                } catch (ConcurrentInventoryModificationException ex) {
                    retryCount++;
                    if (retryCount == MAX_RETRIES) {
                        throw new ServiceException("Unable to update the inventory due to too many users" +
                                "concurrently updating this inventory. Please try again.");
                    }
                }

            }
            //if this is null, then there must have been a validation error so don't try to fill out properties
            if (adminInstance == null) {
                return entity;
            }

            Entity result = helper.getRecord(adminProperties, adminInstance, null, null);

            //correctSkuProperties(result, adminInstance.getSku(), helper);

            return result;

        } catch (Exception e) {
            LOG.error("Unable to update entity for " + entity.getType()[0], e);
            throw new ServiceException("Unable to update entity for " + entity.getType()[0], e);
        }
    }

    /**
     * Ensures that Sku properties delegate to their getters as necessary as well as adds a new property
     * for the consolidated product option values
     * 
     * @param entity
     * @throws NoSuchMethodException 
     * @throws InvocationTargetException 
     * @throws IllegalAccessException 
     */
    protected void correctSkuProperties(Entity entity, Sku sku, RecordHelper helper) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        for (Property property : entity.getProperties()) {
            //if some of the sku properties are empty, use the getter
            if (property.getName().startsWith("sku") && StringUtils.isEmpty(property.getValue())) {
                String skuProperty = property.getName().substring("sku.".length());
                String value = SkuCustomPersistenceHandler.getStringValueFromGetter(skuProperty, sku, helper);
                property.setValue(value);
            }
        }

        Property optionsProperty = SkuCustomPersistenceHandler.getConsolidatedOptionProperty(sku.getProductOptionValues());
        entity.addProperty(optionsProperty);
    }

}
