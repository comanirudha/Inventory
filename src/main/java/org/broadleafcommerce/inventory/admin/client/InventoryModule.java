/*
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

package org.broadleafcommerce.inventory.admin.client;

import org.broadleafcommerce.openadmin.client.AbstractModule;
import org.broadleafcommerce.openadmin.client.BLCMain;

import java.util.ArrayList;
import java.util.List;


/**
 * 
 * @author Phillip Verheyden
 */
public class InventoryModule extends AbstractModule {

    @Override
    public void onModuleLoad() {
        setModuleKey("BLCMerchandising");
        
        List<String> fulfillmentLocationPermissions = new ArrayList<String>();
        fulfillmentLocationPermissions.add("PERMISSION_CREATE_FULFILLMENT_LOCATION");
        fulfillmentLocationPermissions.add("PERMISSION_UPDATE_FULFILLMENT_LOCATION");
        fulfillmentLocationPermissions.add("PERMISSION_DELETE_FULFILLMENT_LOCATION");
        fulfillmentLocationPermissions.add("PERMISSION_READ_FULFILLMENT_LOCATION");
        setSection(
                BLCMain.getMessageManager().getString("fulfillmentLocationMainTitle"),
                "fulfillmentLocations",
                "org.broadleafcommerce.inventory.admin.client.view.FulfillmentLocationView",
                "fulfillmentLocationPresenter",
                "org.broadleafcommerce.inventory.admin.client.presenter.FulfillmentLocationPresenter",
                fulfillmentLocationPermissions
        );
    }
    
}
