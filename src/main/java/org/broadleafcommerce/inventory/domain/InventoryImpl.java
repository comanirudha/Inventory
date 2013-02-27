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
package org.broadleafcommerce.inventory.domain;

import org.broadleafcommerce.common.presentation.AdminPresentation;
import org.broadleafcommerce.common.presentation.AdminPresentationClass;
import org.broadleafcommerce.common.presentation.AdminPresentationToOneLookup;
import org.broadleafcommerce.common.presentation.ConfigurationItem;
import org.broadleafcommerce.common.presentation.PopulateToOneFieldsEnum;
import org.broadleafcommerce.common.presentation.ValidationConfiguration;
import org.broadleafcommerce.common.presentation.client.SupportedFieldType;
import org.broadleafcommerce.common.presentation.client.VisibilityEnum;
import org.broadleafcommerce.common.presentation.override.AdminPresentationOverride;
import org.broadleafcommerce.common.presentation.override.AdminPresentationOverrides;
import org.broadleafcommerce.core.catalog.domain.Sku;
import org.broadleafcommerce.core.catalog.domain.SkuImpl;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;

@Entity
@Table(name = "BLC_INVENTORY", uniqueConstraints = {@UniqueConstraint(columnNames = {"FULFILLMENT_LOCATION_ID", "SKU_ID"})})
//@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region="blInventoryElements")
@Inheritance(strategy = InheritanceType.JOINED)
@AdminPresentationOverrides(
    {
        @AdminPresentationOverride(name = "sku.id", value = @AdminPresentation(friendlyName = "InventoryImpl_skuId", excluded = false, prominent = true, order = 1)),
        @AdminPresentationOverride(name = "sku.name", value = @AdminPresentation(friendlyName ="InventoryImpl_skuName", excluded = false, prominent = true, order = 2, visibility = VisibilityEnum.FORM_HIDDEN)),
        // These properties are declared as @AdminPresentationOverrides in either fulfillmentLocation or address, so we need
        // to ensure they are excluded in Inventory's list of overrides
        @AdminPresentationOverride(name = "fulfillmentLocation.address.addressLine1", value = @AdminPresentation(excluded = true)),
        @AdminPresentationOverride(name = "fulfillmentLocation.address.phonePrimary.phoneNumber", value = @AdminPresentation(excluded = true)),
        @AdminPresentationOverride(name = "fulfillmentLocation.address.phoneSecondary.phoneNumber", value = @AdminPresentation(excluded = true)),
        @AdminPresentationOverride(name = "fulfillmentLocation.address.phoneFax.phoneNumber", value = @AdminPresentation(excluded = true))
    }
)
@AdminPresentationClass(populateToOneFields = PopulateToOneFieldsEnum.TRUE, friendlyName = "InventoryImpl_baseInventory")
public class InventoryImpl implements Inventory {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(generator = "InventoryId", strategy = GenerationType.TABLE)
    @TableGenerator(name = "InventoryId", table = "SEQUENCE_GENERATOR", pkColumnName = "ID_NAME", valueColumnName = "ID_VAL", pkColumnValue = "InventoryImpl", allocationSize = 50)
    @Column(name = "INVENTORY_ID")
    protected Long id;

    @ManyToOne(cascade = CascadeType.ALL, targetEntity = FulfillmentLocationImpl.class, optional = false)
    @JoinColumn(name = "FULFILLMENT_LOCATION_ID", nullable = false)
    @AdminPresentation(excluded=true, visibility = VisibilityEnum.HIDDEN_ALL)
    protected FulfillmentLocation fulfillmentLocation;

    @ManyToOne(cascade = CascadeType.ALL, targetEntity = SkuImpl.class, optional = false)
    @JoinColumn(name = "SKU_ID", nullable = false)
    @AdminPresentation(friendlyName="Sku", group = "Sku", groupOrder = 1, order = 1)
    @AdminPresentationToOneLookup(customCriteria = { "inventoryFilteredSkuList" })
    protected Sku sku;

    @Column(name = "QUANTITY_AVAILABLE", nullable = false)
    @AdminPresentation(friendlyName = "InventoryImpl_quantityAvailable", prominent = true, group = "Quantities",
            groupOrder = 2, order = 1, fieldType = SupportedFieldType.INTEGER,
            validationConfigurations = {
                    @ValidationConfiguration(
                            validationImplementation="com.smartgwt.client.widgets.form.validator.IntegerRangeValidator",
                            configurationItems={
                                    @ConfigurationItem(itemName="min", itemValue="0")
                            }
                    )
            })
    protected Integer quantityAvailable;

    @Column(name = "QUANTITY_ON_HAND", nullable = false)
    @AdminPresentation(friendlyName = "InventoryImpl_quantityOnHand", prominent = true, group = "Quantities",
            groupOrder = 2, order = 2,
            validationConfigurations = {
                    @ValidationConfiguration(
                            validationImplementation="com.smartgwt.client.widgets.form.validator.IntegerRangeValidator",
                            configurationItems={
                                    @ConfigurationItem(itemName="min", itemValue="0")
                            }
                    )
            })
    protected Integer quantityOnHand;

    @Column(name = "EXPECTED_AVAILABILITY_DATE")
    protected Date expectedAvailabilityDate;

    @Version
    @Column(name = "VERSION_NUM", nullable = false)
    @AdminPresentation(excluded = true)
    protected Long version;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public FulfillmentLocation getFulfillmentLocation() {
        return fulfillmentLocation;
    }

    @Override
    public void setFulfillmentLocation(FulfillmentLocation fulfillmentLocation) {
        this.fulfillmentLocation = fulfillmentLocation;
    }

    @Override
    public Sku getSku() {
        return sku;
    }

    @Override
    public void setSku(Sku sku) {
        this.sku = sku;
    }

    @Override
    public Integer getQuantityAvailable() {
        return quantityAvailable;
    }

    @Override
    public void setQuantityAvailable(Integer quantityAvailable) {
        this.quantityAvailable = quantityAvailable;
    }

    @Override
    public Integer getQuantityOnHand() {
        return quantityOnHand;
    }

    @Override
    public void setQuantityOnHand(Integer quantityOnHand) {
        this.quantityOnHand = quantityOnHand;
    }

    @Override
    public Date getExpectedAvailabilityDate() {
        return expectedAvailabilityDate;
    }

    @Override
    public void setExpectedAvailabilityDate(Date expectedAvailabilityDate) {
        this.expectedAvailabilityDate = expectedAvailabilityDate;
    }

    @Override
    public Long getVersion() {
        return version;
    }

}
