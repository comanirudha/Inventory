/*
 * Copyright 2008-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.broadleafcommerce.inventory.admin.client.datasource;

import org.broadleafcommerce.openadmin.client.datasource.dynamic.ListGridDataSource;
import org.broadleafcommerce.openadmin.client.datasource.dynamic.module.BasicClientEntityModule;
import org.broadleafcommerce.openadmin.client.dto.PersistencePerspective;
import org.broadleafcommerce.openadmin.client.service.DynamicEntityServiceAsync;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSource;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.tree.TreeNode;

/**
 * This entity module is responsible for setting all of the other displayed fulfillment locations to not be the default location
 * if I am updating/adding a location that should be the new default. This happens already on the backend, but the frontend
 * has these values cached in the other records
 * 
 * @author Phillip Verheyden (phillipuniverse)
 *
 */
public class FulfillmentLocationClientEntityModule extends BasicClientEntityModule {


    public FulfillmentLocationClientEntityModule(String ceilingEntityFullyQualifiedClassname, PersistencePerspective persistencePerspective, DynamicEntityServiceAsync service) {
        super(ceilingEntityFullyQualifiedClassname, persistencePerspective, service);
    }
    
    @Override
    public void executeUpdate(final String requestId, final DSRequest request, final DSResponse response, final String[] customCriteria, final AsyncCallback<DataSource> cb) {
        JavaScriptObject data = request.getData();
        TreeNode record = new TreeNode(data);
        super.executeUpdate(requestId, request, response, customCriteria, getCallback(record, request, cb));
    }

    public void executeAdd(final String requestId, final DSRequest request, final DSResponse response, final String[] customCriteria, final AsyncCallback<DataSource> cb) {
        JavaScriptObject data = request.getData();
        TreeNode record = new TreeNode(data);
        super.executeAdd(requestId, request, response, customCriteria, getCallback(record, request, cb));
    }
    
    protected AsyncCallback<DataSource> getCallback(TreeNode changedRecord, final DSRequest request, final AsyncCallback<DataSource> originalCallback) {
        final Long id = changedRecord.getAttribute("id") != null && changedRecord.getAttribute("id").length() > 0 ?
                Long.parseLong(changedRecord.getAttribute("id")) : null;
        final Boolean defaultLocation = changedRecord.getAttribute("defaultLocation") != null && changedRecord.getAttribute("defaultLocation").length() > 0 ?
                Boolean.parseBoolean(changedRecord.getAttribute("defaultLocation")) : null;
        final ListGrid grid = (ListGrid) ((ListGridDataSource) dataSource).getAssociatedGrid();
        return new AsyncCallback<DataSource>() {

            @Override
            public void onFailure(Throwable caught) {
                if (originalCallback != null) {
                    originalCallback.onFailure(caught);
                }
            }

            @Override
            public void onSuccess(DataSource result) {
                if (defaultLocation != null && defaultLocation) {
                    for (ListGridRecord record : grid.getRecords()) {
                        if (id != null && !id.equals(record.getAttributeAsLong("id"))) {
                            record.setAttribute("defaultLocation", !defaultLocation);
                            grid.refreshRow(grid.getRecordIndex(record));
                        }
                    }
                }
                if (originalCallback != null) {
                    originalCallback.onSuccess(result);
                }
            }
        };
    }

}
