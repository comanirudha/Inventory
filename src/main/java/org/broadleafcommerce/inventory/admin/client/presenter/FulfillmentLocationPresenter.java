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
package org.broadleafcommerce.inventory.admin.client.presenter;

import org.broadleafcommerce.inventory.admin.client.datasource.FulfillmentLocationDataSourceFactory;
import org.broadleafcommerce.inventory.admin.client.datasource.InventoryDataSourceFactory;
import org.broadleafcommerce.inventory.admin.client.view.FulfillmentLocationDisplay;
import org.broadleafcommerce.openadmin.client.BLCMain;
import org.broadleafcommerce.openadmin.client.datasource.dynamic.CustomCriteriaListGridDataSource;
import org.broadleafcommerce.openadmin.client.datasource.dynamic.DynamicEntityDataSource;
import org.broadleafcommerce.openadmin.client.datasource.dynamic.ListGridDataSource;
import org.broadleafcommerce.openadmin.client.presenter.entity.DynamicEntityPresenter;
import org.broadleafcommerce.openadmin.client.reflection.Instantiable;
import org.broadleafcommerce.openadmin.client.setup.AsyncCallbackAdapter;
import org.broadleafcommerce.openadmin.client.setup.PresenterSetupItem;

import com.smartgwt.client.data.DataSource;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;

public class FulfillmentLocationPresenter extends DynamicEntityPresenter implements Instantiable {

    protected InventoryPresenter inventoryPresenter;

    @Override
    protected void changeSelection(final Record selectedRecord) {
        inventoryPresenter.load(selectedRecord, getPresenterSequenceSetupManager().getDataSource("inventoryDS"));

        String fulfillmentLocationId = selectedRecord.getAttribute("id");
        CustomCriteriaListGridDataSource skuDS = (CustomCriteriaListGridDataSource) getSkuLookupDatasource();
        skuDS.setCustomCriteria(new String[]{skuDS.getCustomCriteria()[0], fulfillmentLocationId});

    }

    @Override
    public void bind() {
        super.bind();
        removeClickHandlerRegistration.removeHandler();
        removeClickHandlerRegistration = display.getListDisplay().getRemoveButton().addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                if (event.isLeftButtonDown()) {
                    String message = "Are your sure you want to delete this entity?";
                    Boolean defaultLocation = display.getListDisplay().getGrid().getSelectedRecord().getAttributeAsBoolean("defaultLocation");
                    if (defaultLocation) {
                        message = BLCMain.getMessageManager().getString("deleteDefaultLocationPrompt");
                    }
                    SC.confirm(message, new BooleanCallback() {
                        @Override
                        public void execute(Boolean value) {
                            if (value) {
                                removeClicked();
                            }
                        }
                    });
                }
            }
        });
        inventoryPresenter.bind();
    }

    @Override
    public void setup() {

        //setup FulfillmentLocation DataSource
        getPresenterSequenceSetupManager().addOrReplaceItem(new PresenterSetupItem("fulfillmentLocationDS", new FulfillmentLocationDataSourceFactory(), new AsyncCallbackAdapter() {
            @Override
            public void onSetupSuccess(DataSource top) {
                setupDisplayItems(top);
                ((ListGridDataSource) top).setupGridFields(new String[]{}, new Boolean[]{});
            }
        }));

        //setup Inventory DataSource
        getPresenterSequenceSetupManager().addOrReplaceItem(new PresenterSetupItem("inventoryDS", new InventoryDataSourceFactory(), new AsyncCallbackAdapter() {
            @Override
            public void onSetupSuccess(DataSource result) {
                inventoryPresenter = new InventoryPresenter(getDisplay().getInventoryDisplay(), BLCMain.getMessageManager().getString("newInventory"));
                inventoryPresenter.setDataSource((CustomCriteriaListGridDataSource) result, new String[]{"sku.id", "sku.name", "quantityAvailable", "quantityOnHand"}, new Boolean[]{false, false, false, false});
            }
        }));

    }

    public DynamicEntityDataSource getSkuLookupDatasource() {
        String name = getDisplay().getInventoryDisplay().getGrid().getDataSource().getDataURL() + "_" + "sku" + "Lookup";
        return getPresenterSequenceSetupManager().getDataSource(name);
    }

    @Override
    public FulfillmentLocationDisplay getDisplay() {
        return (FulfillmentLocationDisplay) display;
    }

    @Override
    protected void saveClicked() {
        super.saveClicked();
        display.getListDisplay().getGrid().invalidateCache();
    }
}
