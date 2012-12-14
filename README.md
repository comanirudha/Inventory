Inventory
=========

This module provides basic inventory functions to Broadleaf.  Some of the concepts provided here include:
* Inventory across multiple Fulfillment Locations - relevant for multi-warehouse scenarios. You can track inventory
for a Sku across many locations
* Optimistic locking - while decrementing inventory, the system attempts to obtain an optimistic row lock

If you are including this module at the beginning of your
project, simply switch to the `inventory` branch of DemoSite.  All of the necessary changes are implemented there.

If you already have a site up and running from a vanilla version of DemoSite and would like to include inventory, here's
how to get it integrated:

Get your Maven dependencies situated
------------------------------------
In the root pom.xml of your project:
```xml
<dependency>
    <groupId>org.broadleafcommerce</groupId>
    <artifactId>broadleaf-inventory</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```
Reference the dependency in both the `admin` and `site` pom.xmls:
```xml
<dependency>
    <groupId>org.broadleafcommerce</groupId>
    <artifactId>broadleaf-inventory</artifactId>
</dependency>
```

Ensure that the proper admin permissions are associated
-------------------------------------------------------
depending on the current state of your database/import.sql files the primary keys might have to be changed:
```sql
INSERT INTO BLC_ADMIN_PERMISSION (ADMIN_PERMISSION_ID, DESCRIPTION, NAME, PERMISSION_TYPE) VALUES (69,'Create FulfillmentLocation','PERMISSION_CREATE_FULFILLMENT_LOCATION', 'CREATE');
INSERT INTO BLC_ADMIN_PERMISSION (ADMIN_PERMISSION_ID, DESCRIPTION, NAME, PERMISSION_TYPE) VALUES (70,'Update FulfillmentLocation','PERMISSION_UPDATE_FULFILLMENT_LOCATION', 'UPDATE');
INSERT INTO BLC_ADMIN_PERMISSION (ADMIN_PERMISSION_ID, DESCRIPTION, NAME, PERMISSION_TYPE) VALUES (71,'Delete FulfillmentLocation','PERMISSION_DELETE_FULFILLMENT_LOCATION', 'DELETE');
INSERT INTO BLC_ADMIN_PERMISSION (ADMIN_PERMISSION_ID, DESCRIPTION, NAME, PERMISSION_TYPE) VALUES (72,'Read FulfillmentLocation','PERMISSION_READ_FULFILLMENT_LOCATION', 'READ');
INSERT INTO BLC_ADMIN_PERMISSION (ADMIN_PERMISSION_ID, DESCRIPTION, NAME, PERMISSION_TYPE) VALUES (73,'All FulfillmentLocation','PERMISSION_ALL_FULFILLMENT_LOCATION','ALL');
INSERT INTO BLC_ADMIN_PERMISSION (ADMIN_PERMISSION_ID, DESCRIPTION, NAME, PERMISSION_TYPE) VALUES (74,'Create Inventory','PERMISSION_CREATE_INVENTORY', 'CREATE');
INSERT INTO BLC_ADMIN_PERMISSION (ADMIN_PERMISSION_ID, DESCRIPTION, NAME, PERMISSION_TYPE) VALUES (75,'Update Inventory','PERMISSION_UPDATE_INVENTORY', 'UPDATE');
INSERT INTO BLC_ADMIN_PERMISSION (ADMIN_PERMISSION_ID, DESCRIPTION, NAME, PERMISSION_TYPE) VALUES (76,'Delete Inventory','PERMISSION_DELETE_INVENTORY', 'DELETE');
INSERT INTO BLC_ADMIN_PERMISSION (ADMIN_PERMISSION_ID, DESCRIPTION, NAME, PERMISSION_TYPE) VALUES (77,'Read Inventory','PERMISSION_READ_INVENTORY', 'READ');
INSERT INTO BLC_ADMIN_PERMISSION (ADMIN_PERMISSION_ID, DESCRIPTION, NAME, PERMISSION_TYPE) VALUES (78,'All Inventory','PERMISSION_ALL_INVENTORY','ALL');

INSERT INTO BLC_ADMIN_PERMISSION_ENTITY (ADMIN_PERMISSION_ENTITY_ID, CEILING_ENTITY, ADMIN_PERMISSION_ID) VALUES (230, 'org.broadleafcommerce.core.inventory.domain.FulfillmentLocation', 64);
INSERT INTO BLC_ADMIN_PERMISSION_ENTITY (ADMIN_PERMISSION_ENTITY_ID, CEILING_ENTITY, ADMIN_PERMISSION_ID) VALUES (231, 'org.broadleafcommerce.core.inventory.domain.FulfillmentLocation', 65);
INSERT INTO BLC_ADMIN_PERMISSION_ENTITY (ADMIN_PERMISSION_ENTITY_ID, CEILING_ENTITY, ADMIN_PERMISSION_ID) VALUES (232, 'org.broadleafcommerce.core.inventory.domain.FulfillmentLocation', 66);
INSERT INTO BLC_ADMIN_PERMISSION_ENTITY (ADMIN_PERMISSION_ENTITY_ID, CEILING_ENTITY, ADMIN_PERMISSION_ID) VALUES (233, 'org.broadleafcommerce.core.inventory.domain.FulfillmentLocation', 67);
INSERT INTO BLC_ADMIN_PERMISSION_ENTITY (ADMIN_PERMISSION_ENTITY_ID, CEILING_ENTITY, ADMIN_PERMISSION_ID) VALUES (234, 'org.broadleafcommerce.core.inventory.domain.FulfillmentLocation', 68);
INSERT INTO BLC_ADMIN_PERMISSION_ENTITY (ADMIN_PERMISSION_ENTITY_ID, CEILING_ENTITY, ADMIN_PERMISSION_ID) VALUES (235, 'org.broadleafcommerce.core.inventory.domain.Inventory', 69);
INSERT INTO BLC_ADMIN_PERMISSION_ENTITY (ADMIN_PERMISSION_ENTITY_ID, CEILING_ENTITY, ADMIN_PERMISSION_ID) VALUES (236, 'org.broadleafcommerce.core.inventory.domain.Inventory', 70);
INSERT INTO BLC_ADMIN_PERMISSION_ENTITY (ADMIN_PERMISSION_ENTITY_ID, CEILING_ENTITY, ADMIN_PERMISSION_ID) VALUES (237, 'org.broadleafcommerce.core.inventory.domain.Inventory', 71);
INSERT INTO BLC_ADMIN_PERMISSION_ENTITY (ADMIN_PERMISSION_ENTITY_ID, CEILING_ENTITY, ADMIN_PERMISSION_ID) VALUES (238, 'org.broadleafcommerce.core.inventory.domain.Inventory', 72);
INSERT INTO BLC_ADMIN_PERMISSION_ENTITY (ADMIN_PERMISSION_ENTITY_ID, CEILING_ENTITY, ADMIN_PERMISSION_ID) VALUES (239, 'org.broadleafcommerce.core.inventory.domain.Inventory', 73);

-- ensure that super user and catalog maintainer can modify inventory
INSERT INTO BLC_ADMIN_ROLE_PERMISSION_XREF (ADMIN_ROLE_ID, ADMIN_PERMISSION_ID) VALUES (1,73);
INSERT INTO BLC_ADMIN_ROLE_PERMISSION_XREF (ADMIN_ROLE_ID, ADMIN_PERMISSION_ID) VALUES (2,73);
INSERT INTO BLC_ADMIN_ROLE_PERMISSION_XREF (ADMIN_ROLE_ID, ADMIN_PERMISSION_ID) VALUES (1,78);
INSERT INTO BLC_ADMIN_ROLE_PERMISSION_XREF (ADMIN_ROLE_ID, ADMIN_PERMISSION_ID) VALUES (2,78);

-- tell the admin it needs to load this section also and associate permissions
INSERT INTO BLC_ADMIN_SECTION (ADMIN_SECTION_ID, ADMIN_MODULE_ID, NAME, SECTION_KEY, URL, USE_DEFAULT_HANDLER) VALUES (15, 1, 'Inventory', 'Inventory', '/inventory', TRUE);
INSERT INTO BLC_ADMIN_SECTION_PERMISSION_XREF (ADMIN_SECTION_ID, ADMIN_PERMISSION_ID) VALUES (15,74);
INSERT INTO BLC_ADMIN_SECTION_PERMISSION_XREF (ADMIN_SECTION_ID, ADMIN_PERMISSION_ID) VALUES (15,75);
INSERT INTO BLC_ADMIN_SECTION_PERMISSION_XREF (ADMIN_SECTION_ID, ADMIN_PERMISSION_ID) VALUES (15,76);
INSERT INTO BLC_ADMIN_SECTION_PERMISSION_XREF (ADMIN_SECTION_ID, ADMIN_PERMISSION_ID) VALUES (15,77);
```

Include the CheckAvailabilityInventoryActivity in the BLC merge process
in web.xml under the `patchConfigLocations` parameter, include this:
```
classpath:/bl-inventory-applicationContext.xml
```

Include
