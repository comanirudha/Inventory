package org.broadleafcommerce.inventory.service.workflow;

import org.broadleafcommerce.common.logging.SupportLogManager;
import org.broadleafcommerce.common.logging.SupportLogger;
import org.broadleafcommerce.core.catalog.domain.Sku;
import org.broadleafcommerce.core.workflow.Activity;
import org.broadleafcommerce.core.workflow.ProcessContext;
import org.broadleafcommerce.core.workflow.state.RollbackFailureException;
import org.broadleafcommerce.core.workflow.state.RollbackHandler;
import org.broadleafcommerce.inventory.exception.ConcurrentInventoryModificationException;
import org.broadleafcommerce.inventory.exception.InventoryUnavailableException;
import org.broadleafcommerce.inventory.service.InventoryService;

import java.util.Map;

import javax.annotation.Resource;

/**
 * Provides a standard rollback handler to ensure that in the event of something going wrong in a workflow, that 
 * 
 * @author Kelly Tisdell
 *
 */
public class InventoryRollbackHandler implements RollbackHandler {

    private static final SupportLogger LOG = SupportLogManager.getLogger("broadleaf-oms", InventoryRollbackHandler.class);
    
    @Resource(name = "blInventoryService")
    protected InventoryService inventoryService;

    protected int maxRetries = 5;

    @Override
    public void rollbackState(Activity activity, ProcessContext processContext, Map<String, Object> stateConfiguration) throws RollbackFailureException {

        if (stateConfiguration == null ||
                (stateConfiguration.get("ROLLBACK_BLC_INVENTORY_DECREMENTED") == null &&
                stateConfiguration.get("ROLLBACK_BLC_INVENTORY_INCREMENTED") == null)) {
            return;
        }

        String orderId = "(Not Known)";
        if (stateConfiguration.get("ROLLBACK_BLC_ORDER_ID") != null) {
            orderId = String.valueOf(stateConfiguration.get("ROLLBACK_BLC_ORDER_ID"));
        }

        @SuppressWarnings("unchecked")
        Map<Sku, Integer> inventoryToIncrement = (Map<Sku, Integer>) stateConfiguration.get("ROLLBACK_BLC_INVENTORY_DECREMENTED");
        if (inventoryToIncrement != null && !inventoryToIncrement.isEmpty()) {
            int retryCount = 0;

            while (retryCount <= maxRetries) {
                try {
                    inventoryService.incrementInventory(inventoryToIncrement);
                    break;
                } catch (ConcurrentInventoryModificationException ex) {
                    retryCount++;
                    if (retryCount == maxRetries) {
                        LOG.error("After an exception was encountered during checkout, where inventory was decremented. " + maxRetries + " attempts were made to compensate, " +
                                "but were unsuccessful for order ID: " + orderId + ". This should be corrected manually!", ex);
                    }
                } catch (RuntimeException ex) {
                    LOG.error("An unexpected error occured in the error handler of the checkout workflow trying to compensate for inventory. This happend for order ID: " +
                            orderId + ". This should be corrected manually!", ex);
                    break;
                }
            }
        }

        @SuppressWarnings("unchecked")
        Map<Sku, Integer> inventoryToDecrement = (Map<Sku, Integer>) stateConfiguration.get("ROLLBACK_BLC_INVENTORY_INCREMENTED");
        if (inventoryToDecrement != null && !inventoryToDecrement.isEmpty()) {
            int retryCount = 0;

            while (retryCount <= maxRetries) {
                try {
                    inventoryService.decrementInventory(inventoryToDecrement);
                    break;
                } catch (ConcurrentInventoryModificationException ex) {
                    retryCount++;
                    if (retryCount == maxRetries) {
                        LOG.error("After an exception was encountered during checkout, where inventory was incremented. " + maxRetries + " attempts were made to compensate, " +
                                "but were unsuccessful for order ID: " + orderId + ". This should be corrected manually!", ex);
                    }
                } catch (InventoryUnavailableException e) {
                    //This is an awkward, unlikely state.  I just added some inventory, but something happened, and I want to remove it, but it's already gone!
                    LOG.error("While trying roll back (decrement) inventory, we found that there was none left decrement.", e);
                } catch (RuntimeException ex) {
                    LOG.error("An unexpected error occured in the error handler of the checkout workflow trying to compensate for inventory. This happend for order ID: " +
                            orderId + ". This should be corrected manually!", ex);
                    break;
                }
            }
        }
    }

    public void setMaxRetries(int maxRetries) {
        if (this.maxRetries < 0) {
            throw new IllegalArgumentException("Max retries cannot be less than 0.");
        }
        this.maxRetries = maxRetries;
    }
}
