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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadleafcommerce.admin.server.service.handler.SkuCustomPersistenceHandler;
import org.broadleafcommerce.core.catalog.domain.SkuImpl;
import org.broadleafcommerce.core.catalog.service.CatalogService;
import org.broadleafcommerce.inventory.service.InventoryService;
import org.broadleafcommerce.openadmin.client.dto.PersistencePackage;

import com.anasoft.os.daofusion.criteria.PersistentEntityCriteria;
import com.anasoft.os.daofusion.cto.client.CriteriaTransferObject;

import javax.annotation.Resource;

public class InventorySkuCustomPersistenceHandler extends SkuCustomPersistenceHandler {

    private static final Log LOG = LogFactory.getLog(InventorySkuCustomPersistenceHandler.class);

    @Resource(name = "blInventoryService")
    protected InventoryService inventoryService;

    @Resource(name = "blCatalogService")
    protected CatalogService catalogService;

    @Override
    public Boolean canHandleInspect(PersistencePackage persistencePackage) {
        String className = persistencePackage.getCeilingEntityFullyQualifiedClassname();
        String[] customCriteria = persistencePackage.getCustomCriteria();
        return customCriteria != null && customCriteria.length > 0 && SkuImpl.class.getName().equals(className) && "inventoryFilteredSkuList".equals(customCriteria[0]);
    }

    @Override
    public Boolean canHandleFetch(PersistencePackage persistencePackage) {
        return canHandleInspect(persistencePackage);
    }

    @Override
    public void applyAdditionalFetchCriteria(PersistentEntityCriteria queryCriteria, CriteriaTransferObject cto, PersistencePackage persistencePackage) {
        super.applyAdditionalFetchCriteria(queryCriteria, cto, persistencePackage);
    }

}
