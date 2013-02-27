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
package org.broadleafcommerce.inventory.service;

import org.broadleafcommerce.common.persistence.EntityConfiguration;
import org.broadleafcommerce.core.catalog.domain.Sku;
import org.broadleafcommerce.core.inventory.service.type.InventoryType;
import org.broadleafcommerce.inventory.dao.InventoryDao;
import org.broadleafcommerce.inventory.domain.FulfillmentLocation;
import org.broadleafcommerce.inventory.domain.Inventory;
import org.broadleafcommerce.inventory.exception.ConcurrentInventoryModificationException;
import org.broadleafcommerce.inventory.exception.InventoryUnavailableException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

@Service("blInventoryService")
public class InventoryServiceImpl implements InventoryService {

    @Resource(name="blInventoryDao")
    protected InventoryDao inventoryDao;

    @Resource(name = "blFulfillmentLocationService")
    protected FulfillmentLocationService fulfillmentLocationService;

    @Resource(name = "blEntityConfiguration")
    protected EntityConfiguration entityConfiguration;

    @Override
    public boolean isSkuEligibleForInventoryCheck(Sku sku) {
        if (sku.getInventoryType() == null
                && (sku.getProduct().getDefaultCategory() == null
                || sku.getProduct().getDefaultCategory().getInventoryType() == null)) {
            return false;
        } else if (InventoryType.NONE.equals(sku.getInventoryType())
                || (sku.getProduct().getDefaultCategory() != null
                && InventoryType.NONE.equals(sku.getProduct().getDefaultCategory().getInventoryType()))) {
            return false;
        } else if (InventoryType.BASIC.equals(sku.getInventoryType())
                || (sku.getProduct().getDefaultCategory() != null
                && InventoryType.BASIC.equals(sku.getProduct().getDefaultCategory().getInventoryType()))) {
            return true;
        }

        return false;
    }

    @Override
    public boolean isQuantityAvailable(Sku sku, Integer quantity) {
        //if the sku does not exist or is not active, there is no quantity available
        if (!sku.isActive()) {
            return false;
        }

        if (!isSkuEligibleForInventoryCheck(sku)) {
            //This sku is not eligible for inventory checks, so assume it is available
            return true;
        }

        //quantity must be greater than 0
        if (quantity == null || quantity < 0) {
            throw new IllegalArgumentException("Quantity must be a positive integer");
        }

        Inventory inventory = inventoryDao.readInventoryForDefaultFulfillmentLocation(sku);

        return inventory != null && inventory.getQuantityAvailable() >= quantity;
    }

    @Override
    @Transactional("blTransactionManager")
    public boolean isQuantityAvailable(Sku sku, Integer quantity, FulfillmentLocation fulfillmentLocation) {

        //if the sku does not exist or is not active, there is no quantity available
        if (!sku.isActive()) {
            return false;
        }

        if (!isSkuEligibleForInventoryCheck(sku)) {
            //This sku is not eligible for inventory checks, so assume it is available
            return true;
        }

        //quantity must be greater than 0
        if (quantity == null || quantity < 0) {
            throw new IllegalArgumentException("Quantity must be a positive integer");
        }

        Inventory inventory = inventoryDao.readInventory(sku, fulfillmentLocation);

        return inventory != null && inventory.getQuantityAvailable() >= quantity;

    }

    @Override
    @Transactional(propagation= Propagation.REQUIRES_NEW,value="blTransactionManager", rollbackFor={InventoryUnavailableException.class,ConcurrentInventoryModificationException.class})
    public void decrementInventory(Map<Sku, Integer> skuInventory) throws ConcurrentInventoryModificationException, InventoryUnavailableException {
        decrementInventory(skuInventory, null);
    }

    @Override
    @Transactional(propagation= Propagation.REQUIRES_NEW,value="blTransactionManager", rollbackFor={InventoryUnavailableException.class,ConcurrentInventoryModificationException.class})
    public void decrementInventory(Map<Sku, Integer> skuInventory, FulfillmentLocation fulfillmentLocation) throws ConcurrentInventoryModificationException, InventoryUnavailableException {

        Set<Sku> skus = skuInventory.keySet();
        Map<Long, Integer> unavailableInventoryHolder = new HashMap<Long, Integer>();

        for (Sku sku : skus) {

            Integer quantity = skuInventory.get(sku);

            /*
             * If the inventory type of the sku or category is null or InventoryType.NONE, do not adjust inventory
             */
            if (!isSkuEligibleForInventoryCheck(sku)) {
                //Don't adjust inventory for this Sku
                continue;
            }

            //quantity must not be null
            if (quantity == null || quantity < 0) {
                throw new IllegalArgumentException("Quantity must not be a positive integer");
            }

            if (quantity == 0) {
                continue;
            }

            //check available inventory
            Inventory inventory = null;
            if (fulfillmentLocation != null) {
                inventory = inventoryDao.readInventoryForUpdate(sku, fulfillmentLocation);
            } else {
                inventory = inventoryDao.readInventoryForUpdateForDefaultFulfillmentLocation(sku);
            }

            if (inventory != null) {
                Integer quantityAvailable = inventory.getQuantityAvailable();

                int qtyToUpdate = quantityAvailable - quantity;
                if (qtyToUpdate < 0) {
                    //there is not enough inventory available
                    unavailableInventoryHolder.put(sku.getId(), quantityAvailable);
                } else {
                    inventory.setQuantityAvailable(qtyToUpdate);
                    inventoryDao.save(inventory); //this call could throw ConcurrentInventoryModificationException
                }

            } else {
               unavailableInventoryHolder.put(sku.getId(), 0);
            }

        }

        if (!unavailableInventoryHolder.isEmpty()) {
            InventoryUnavailableException ex = new InventoryUnavailableException("Inventory is unavailable for " + unavailableInventoryHolder.size() + " skus");
            ex.setSkuInventoryAvailable(unavailableInventoryHolder);
            throw ex;
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, value = "blTransactionManager", rollbackFor = { InventoryUnavailableException.class, ConcurrentInventoryModificationException.class })
    public void decrementInventoryOnHand(Map<Sku, Integer> skuInventory) throws ConcurrentInventoryModificationException, InventoryUnavailableException {
        decrementInventoryOnHand(skuInventory, null);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, value = "blTransactionManager", rollbackFor = { InventoryUnavailableException.class, ConcurrentInventoryModificationException.class })
    public void decrementInventoryOnHand(Map<Sku, Integer> skuInventory, FulfillmentLocation fulfillmentLocation) throws ConcurrentInventoryModificationException, InventoryUnavailableException {
        Set<Sku> skus = skuInventory.keySet();
        Map<Long, Integer> unavailableInventoryHolder = new HashMap<Long, Integer>();

        for (Sku sku : skus) {

            Integer quantity = skuInventory.get(sku);

            /*
             * If the inventory type of the sku or category is null or InventoryType.NONE, do not adjust inventory
             */
            if (!isSkuEligibleForInventoryCheck(sku)) {
                //Don't check inventory for this Sku
                continue;
            }

            //quantity must not be null
            if (quantity == null || quantity < 0) {
                throw new IllegalArgumentException("Quantity must not be a positive integer");
            }

            if (quantity == 0) {
                continue;
            }

            //check available inventory
            Inventory inventory = null;
            if (fulfillmentLocation != null) {
                inventory = inventoryDao.readInventoryForUpdate(sku, fulfillmentLocation);
            } else {
                inventory = inventoryDao.readInventoryForUpdateForDefaultFulfillmentLocation(sku);
            }

            if (inventory != null) {
                Integer quantityOnHand = inventory.getQuantityOnHand();

                int qtyToUpdate = quantityOnHand - quantity;
                if (qtyToUpdate < 0) {
                    //there is not enough inventory available
                    unavailableInventoryHolder.put(sku.getId(), quantityOnHand);
                } else {
                    inventory.setQuantityOnHand(qtyToUpdate);
                    inventoryDao.save(inventory); //this call could throw ConcurrentInventoryModificationException
                }
            } else {
                unavailableInventoryHolder.put(sku.getId(), 0);
            }

        }

        if (!unavailableInventoryHolder.isEmpty()) {
            InventoryUnavailableException ex = new InventoryUnavailableException("Inventory is unavailable for " + unavailableInventoryHolder.size() + " skus");
            ex.setSkuInventoryAvailable(unavailableInventoryHolder);
            throw ex;
        }
    }

    @Override
    @Transactional(propagation= Propagation.REQUIRES_NEW,value="blTransactionManager", rollbackFor={InventoryUnavailableException.class,ConcurrentInventoryModificationException.class})
    public void incrementInventory(Map<Sku, Integer> skuInventory, FulfillmentLocation fulfillmentLocation) throws ConcurrentInventoryModificationException {
        Set<Sku> skus = skuInventory.keySet();
        
        for (Sku sku : skus) {
            Integer quantity = skuInventory.get(sku);

            /*
             * If the inventory type of the sku or category is null or InventoryType.NONE, do not adjust inventory
             */
            if (!isSkuEligibleForInventoryCheck(sku)) {
                //Don't adjust inventory for this Sku
                continue;
            }

            //quantity must not be null
            if (quantity == null || quantity < 0) {
                throw new IllegalArgumentException("Quantity must not be a positive integer");
            }

            if (quantity == 0) {
                continue;
            }

            Inventory inventory = null;
            if (fulfillmentLocation != null) {
                inventory = inventoryDao.readInventoryForUpdate(sku, fulfillmentLocation);
            }

            if (inventory != null) {
                inventory.setQuantityAvailable(inventory.getQuantityAvailable() + quantity);
                inventoryDao.save(inventory);
            } else {
                /*
                 * create a new inventory record if one does not exist
                 */
                inventory = (Inventory) entityConfiguration.createEntityInstance(Inventory.class.getName());
                inventory.setQuantityAvailable(quantity);
                inventory.setQuantityOnHand(quantity);
                inventory.setSku(sku);
                inventory.setFulfillmentLocation(fulfillmentLocation);
                inventoryDao.save(inventory);
            }

        }
    }
    
    @Override
    @Transactional(propagation= Propagation.REQUIRES_NEW,value="blTransactionManager", rollbackFor={InventoryUnavailableException.class,ConcurrentInventoryModificationException.class})
    public void incrementInventory(Map<Sku, Integer> skuInventory) throws ConcurrentInventoryModificationException {
        
        Set<Sku> skus = skuInventory.keySet();
        for (Sku sku : skus) {
            Integer quantity = skuInventory.get(sku);

            /*
             * If the inventory type of the sku or category is null or InventoryType.NONE, do not adjust inventory
             */
            if (!isSkuEligibleForInventoryCheck(sku)) {
                //Don't adjust inventory for this Sku
                continue;
            }

            //quantity must not be null
            if (quantity == null || quantity < 0) {
                throw new IllegalArgumentException("Quantity must not be a positive integer");
            }

            if (quantity == 0) {
                continue;
            }

            Inventory inventory = inventoryDao.readInventoryForUpdateForDefaultFulfillmentLocation(sku);

            if (inventory != null) {
                inventory.setQuantityAvailable(inventory.getQuantityAvailable() + quantity);
                inventoryDao.save(inventory);
            } else {
                throw new IllegalStateException("There was a call to InventoryServiceImpl.incrementInventory for a default fulfillment location, but no default " +
                        "inventory for the sku: " + sku.getId() + " could be found!");
            }

        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, value = "blTransactionManager", rollbackFor = { InventoryUnavailableException.class, ConcurrentInventoryModificationException.class })
    public void incrementInventoryOnHand(Map<Sku, Integer> skuInventory, FulfillmentLocation fulfillmentLocation) throws ConcurrentInventoryModificationException {
        Set<Sku> skus = skuInventory.keySet();
        for (Sku sku : skus) {
            Integer quantity = skuInventory.get(sku);

            /*
             * If the inventory type of the sku or category is null or InventoryType.NONE, do not adjust inventory
             */
            if (!isSkuEligibleForInventoryCheck(sku)) {
                //Don't adjust inventory for this Sku
                continue;
            }

            //quantity must not be null
            if (quantity == null || quantity < 0) {
                throw new IllegalArgumentException("Quantity must not be a positive integer");
            }

            if (quantity == 0) {
                continue;
            }

            Inventory inventory = inventoryDao.readInventoryForUpdate(sku, fulfillmentLocation);

            if (inventory != null) {
                inventory.setQuantityOnHand(inventory.getQuantityOnHand() + quantity);
                inventoryDao.save(inventory);
            } else {
                /*
                 * create a new inventory record if one does not exist
                 */
                inventory = (Inventory) entityConfiguration.createEntityInstance(Inventory.class.getName());
                inventory.setQuantityAvailable(quantity);
                inventory.setQuantityOnHand(quantity);
                inventory.setSku(sku);
                inventory.setFulfillmentLocation(fulfillmentLocation);
                inventoryDao.save(inventory);
            }

        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, value = "blTransactionManager", rollbackFor = { InventoryUnavailableException.class, ConcurrentInventoryModificationException.class })
    public void incrementInventoryOnHand(Map<Sku, Integer> skuInventory) throws ConcurrentInventoryModificationException {
        Set<Sku> skus = skuInventory.keySet();
        for (Sku sku : skus) {
            Integer quantity = skuInventory.get(sku);

            /*
             * If the inventory type of the sku or category is null or InventoryType.NONE, do not adjust inventory
             */
            if (!isSkuEligibleForInventoryCheck(sku)) {
                //Don't adjust inventory for this Sku
                continue;
            }

            //quantity must not be null
            if (quantity == null || quantity < 0) {
                throw new IllegalArgumentException("Quantity must not be a positive integer");
            }

            if (quantity == 0) {
                continue;
            }

            Inventory inventory = inventoryDao.readInventoryForUpdateForDefaultFulfillmentLocation(sku);

            if (inventory != null) {
                inventory.setQuantityOnHand(inventory.getQuantityOnHand() + quantity);
                inventoryDao.save(inventory);
            } else {
                throw new IllegalStateException("There was a call to InventoryServiceImpl.incrementInventoryOnHand for a default fulfillment location, but no default " +
                        "inventory for the sku: " + sku.getId() + " could be found!");
            }
        }
    }

    @Override
    @Transactional(value = "blTransactionManager")
    public Inventory readInventory(Sku sku, FulfillmentLocation fulfillmentLocation) {
        return inventoryDao.readInventory(sku, fulfillmentLocation);
    }

    @Override
    @Transactional(value = "blTransactionManager")
    public Inventory readInventory(Sku sku) {
        return inventoryDao.readInventoryForDefaultFulfillmentLocation(sku);
    }

    @Override
    @Transactional(value = "blTransactionManager")
    public List<Inventory> readInventoryForFulfillmentLocation(FulfillmentLocation fulfillmentLocation) {
        return inventoryDao.readInventoryForFulfillmentLocation(fulfillmentLocation);
    }

    @Override
    @Transactional(value = "blTransactionManager")
    public Inventory save(Inventory inventory) throws ConcurrentInventoryModificationException {
        return inventoryDao.save(inventory);
    }

    @Override
    @Transactional(value = "blTransactionManager")
    public List<Sku> readSkusNotAtFulfillmentLocation(FulfillmentLocation fulfillmentLocation) {
        return inventoryDao.readSkusNotAtFulfillmentLocation(fulfillmentLocation);
    }

}
