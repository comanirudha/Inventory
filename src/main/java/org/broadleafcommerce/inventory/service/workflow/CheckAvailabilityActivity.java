/*
 * Copyright 2008-2012 the original author or authors.
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

package org.broadleafcommerce.inventory.service.workflow;

import org.broadleafcommerce.core.catalog.domain.Sku;
import org.broadleafcommerce.core.catalog.service.CatalogService;
import org.broadleafcommerce.core.order.domain.DiscreteOrderItem;
import org.broadleafcommerce.core.order.domain.OrderItem;
import org.broadleafcommerce.core.order.service.OrderItemService;
import org.broadleafcommerce.core.order.service.workflow.CartOperationContext;
import org.broadleafcommerce.core.order.service.workflow.CartOperationRequest;
import org.broadleafcommerce.core.workflow.BaseActivity;
import org.broadleafcommerce.core.workflow.ModuleActivity;
import org.broadleafcommerce.inventory.exception.InventoryUnavailableException;
import org.broadleafcommerce.inventory.service.InventoryService;

import javax.annotation.Resource;

public class CheckAvailabilityActivity extends BaseActivity<CartOperationContext> implements ModuleActivity {

    @Resource(name = "blCatalogService")
    protected CatalogService catalogService;

    @Resource(name = "blOrderItemService")
    protected OrderItemService orderItemService;

    @Resource(name = "blInventoryService")
    protected InventoryService inventoryService;

    @Override
    public CartOperationContext execute(CartOperationContext context) throws Exception {

        CartOperationRequest request = context.getSeedData();
        Long skuId = request.getItemRequest().getSkuId();

        Sku sku = null;
        if (skuId != null) {
            sku = catalogService.findSkuById(skuId);
        } else {
            OrderItem orderItem = orderItemService.readOrderItemById(request.getItemRequest().getOrderItemId());
            if (orderItem instanceof DiscreteOrderItem) {
                sku = ((DiscreteOrderItem) orderItem).getSku();
                request.getItemRequest().setSkuId(sku.getId());
                skuId = sku.getId();
            }
        }

        //Available inventory will not be decremented for this sku until checkout. This activity is assumed to be
        //part of the add to cart / update cart workflow

        boolean quantityAvailable = inventoryService.isQuantityAvailable(sku, request.getItemRequest().getQuantity());

        if (!quantityAvailable) {
            String errorMessage = "Error: Sku with id of " + skuId + " does not have " +
                    request.getItemRequest().getQuantity() + " items in available inventory.";
            throw new InventoryUnavailableException(errorMessage);
        }
        
        return context;
    }
    
    @Override
    public String getModuleName() {
        return "broadleaf-inventory";
    }

}
