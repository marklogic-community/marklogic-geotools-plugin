#MarkLogic GeoTools Driver & GeoServer Installation

---
##Step 1. Install GeoServer

1. Download Platform Independent Binary version of GeoServer 2.16.1 from http://geoserver.org/release/2.16.1/ 
2. Using the Operating System user that you want to run geoserver under, unzip the file:

        unzip geoserver-2.16.1-bin.zip -d /var/geoserver

3. Create link so the path to geoserver is /var/geoserver/latest:
        
        ln -s /var/geoserver/geoserver-2.16.1 /var/geoserver/latest

4. (Optional) Setup GeoServer as a linux service

*Geoserver Installation Notes:*
* This can be using either the standalone version OR the WAR deployment, however the war must be exploded for the MarkLogic geotools plugin to be deployed.
* The plugin has **NOT** been tested with newer releases, and will not work with earlier versions of geoserver due to dependency changes.



---
##Step 2:  Deploy the MarkLogic GeoTools Driver
1. Add the MarkLogic GeoTools Driver and Dependencies to the WAR deployed in Step 1.
    * Copy the MarkLogicGeoToolsDriver*.jar and all jars in the dependency folder to the geoserver war/WEB-INF/lib directory
    * OR, use the ./gradlw deployOffline  or the ./gradlew deployGeoServerPluginJars

*GeoTools Driver Notes:*
* There is a gradle task that will copy the jars if the gradle.properties is pointing in the correct location for your Tomcat.


---
##Step 3. Start GeoServer
Depending on how you have setup your service use:

`systemctl start geoserver.service`
or
`service geoserver start`

---
##Step 4. Setup GeoServer 
1. Navigate to `http://{host}:{port}/geoserver/web`
2. Login as an administrator (default is admin/geoserver)
3. Create your workspace(s) where you are going to use the MarkLogic DataStore
    1. On the welcome page, click `Create workspaces`
    2. Provide a workspace name (eg. "MarkLogic")
    3. Provide a Namespace URI  (eg. "http://marklogic.com/geoserver/som/wfs")
    4. If you desire this to be the default workspace, click the corresponding checkbox.
    5. Click the `submit` button
    6. Click on the newly created workspace name on the `Workspaces` list
    7. Click on the `Enabled` checkbox
    8. Click on any checkboxes for services you want enabled on this workspace (eg. `WFS`)
    9. Fill out Contact Information for this service.
    10. Click `save` button
4. Create a new Store
    1. On the left column, click the `Stores` link under `Data`
    2. Click on the `Add new Store` link at the top of the `Stores` list
    3. Select the `MarkLogic (Basic)` type from the Vector Data Sources
    4. Select the workspace created in Step 3
    5. Provide a Data Source Name (eg. "MarkLogic Store")
    6. Provide a Description of the Data Store
    7. Provide the Connection Parameters:
        * Hostname is the host running MarkLogic Server 
        * Port is the geo-data-services-admin server on your MarkLogic Cluster
        * Username is an administrative user on the MarkLogic Cluster
        * Password is the adminsitrative user's password on the MarkLogic Cluster
        * Database is the corresponding database (eg. `som-content`)
        * User-Auth-Type should be `PreAuthenticatedHeader` to enable GeoAxis to be the Authentication mechanism
        * User-Hostname is the host running the MarkLogic Server for user queries (may be the same as hostname above)
        * User-Port is the port for the application server that will use the custom authentication mechanism for GeoAxis.
    8. Click `save` button
        * This should show you a list of available Service Descriptors for publishing.
5. Setup GeoServer Security 
    1. Create a new Role called "USERS"
        1. On the left column, click the `Users, Groups, Roles` link.
        2. Click on the `Roles` tab
        3. Click `Add new role` link
        4. Provide the name `USERS`
        5. Click the `save` button        
        ***Note:** This role will be passed in on all calls to the WFS APIs.*
    2. Create a new Authentication Filter
        1. On the left column, click the `Authentication` link.
        2. In the `Authentication Filters` section, click the `Add new` link
        3. Click the `HTTP Header` link
        4. Provide a name for your proxy (eg. `proxy`)
        5. Provide the Request Header Attribute (eg. `authentication`)
        6. Select the `Request Header` Role Source
        7. Provide the name of the header that will be passed in (eg. `gs-role`)
        8. Click `save` button
    3. Modify the rest, and default Filter Chains
        1. If not already there, on the left column, click the `Authentication` link
        2. In the `Filter Chains` section, find the `rest` chain, and click on that link
        3. In the bottom section, move the authentication filter you created earlier to the selected list, as the first entry
        4. In the bottom section, if `anonymous` is in the selected list, remove `anonymous` from the selected list
        5. Click the `close` button
        6. Back In the `Filter Chains` section, find the `default` chain, and click on that link
        7. In the bottom section, move the authentication filter you created earlier to the selected list, as the first entry
        8. In the bottom section, if `anonymous` is in the selected list, remove `anonymous` from the selected list
        9. Click the `close` button
        10. Scroll to the bottom of the window and click the `save` button

----


