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
package org.broadleafcommerce.inventory.admin.client.datasource;

import org.broadleafcommerce.common.presentation.client.OperationType;
import org.broadleafcommerce.common.presentation.client.PersistencePerspectiveItemType;
import org.broadleafcommerce.openadmin.client.datasource.DataSourceFactory;
import org.broadleafcommerce.openadmin.client.datasource.dynamic.CustomCriteriaListGridDataSource;
import org.broadleafcommerce.openadmin.client.datasource.dynamic.module.BasicClientEntityModule;
import org.broadleafcommerce.openadmin.client.datasource.dynamic.module.DataSourceModule;
import org.broadleafcommerce.openadmin.client.dto.ForeignKey;
import org.broadleafcommerce.openadmin.client.dto.OperationTypes;
import org.broadleafcommerce.openadmin.client.dto.PersistencePerspective;
import org.broadleafcommerce.openadmin.client.service.AppServices;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DataSource;

public class InventoryDataSourceFactory implements DataSourceFactory {

    public static CustomCriteriaListGridDataSource dataSource = null;

    @Override
    public void createDataSource(String name, OperationTypes operationTypes, Object[] additionalItems, AsyncCallback<DataSource> cb) {
        if (dataSource == null) {
            operationTypes = new OperationTypes(OperationType.BASIC,
                                                OperationType.BASIC,
                                                OperationType.BASIC,
                                                OperationType.BASIC,
                                                OperationType.BASIC);
            PersistencePerspective persistencePerspective = new PersistencePerspective(operationTypes, 
                                                                                       new String[]{}, new ForeignKey[]{});
            ForeignKey locationKey = new ForeignKey("fulfillmentLocation", InventoryEntityImplementations.FULFILLMENT_LOCATION, null);
            persistencePerspective.addPersistencePerspectiveItem(PersistencePerspectiveItemType.FOREIGNKEY, locationKey);
            DataSourceModule[] modules = new DataSourceModule[]{
                    new BasicClientEntityModule(InventoryCeilingEntities.INVENTORY, persistencePerspective, AppServices.DYNAMIC_ENTITY)
            };
            dataSource = new CustomCriteriaListGridDataSource(name, persistencePerspective, AppServices.DYNAMIC_ENTITY, modules, false, true, false, false, true);
            dataSource.setCustomCriteria(new String[]{"inventoryUpdate"});
            dataSource.buildFields(null, false, cb);
        } else {
            if (cb != null) {
                cb.onSuccess(dataSource);
            }
        }
    }

}
