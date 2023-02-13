This guide is intended to facilitate local development and testing, whereas the INSTALL.md guide is focused more on 
running geoserver. 

# Running GeoServer with the plugin

First, download [geoserver 2.22.1](https://sourceforge.net/projects/geoserver/files/GeoServer/2.22.1/geoserver-2.22.
1-bin.zip). You can try a later version, but the dependencies in this project's build.gradle are currently tied to this
release. Extract this to any directory you want. 

Next, Geoserver seems happier if `GEOSERVER_HOME` is defined, so add that to your environment - e.g.

    export GEOSERVER_HOME=/path/to/geoserver

Next, create `gradle-local.properties` in the same directory where you have checked out this project, and add the following to it:

    geoserverHome=/path/to/geoserver

Then run the following Gradle task to build a jar containing the marklogic-geotools plugin and copy it your geoserver
installation:

    ./gradlew deploy

You can now startup geoserver (you may need to do a `chmod 755 $GEOSERVER_HOME/bin/*.sh` to make these files 
executable):

    $GEOSERVER_HOME/bin/startup.sh

Once the server has completed started, point your browser to http://localhost:8080/geoserver/web to begin using the GeoServer.

To learn how to setup a workspace and data store, go to the "Step 4. Setup GeoServer" section in the INSTALL.md guide. 
It's recommended to point the data store against the test app deployed by the [marklogic-geo-data-services]
(https://github.com/marklogic-community/marklogic-geo-data-services) project, which will listen on port 8096. In that 
project, be sure to run `./gradlew mlDeploy loadTestData` so that there are feature services and data available in the 
geo-data-services-test-content database. 

After you create a workspace and data store, you can add a layer:

1. Click on Layers in the left sidebar in the GeoServer UI
2. Click on "Add new layer"
3. Select your MarkLogic store
4. Choose the layer to publish

# Running the tests

You can run `./gradlew test`, but not all tests pass as they currently depend on an unknown configuration and dataset. 
This will soon be improved to depend on a normal test app deployed via ml-gradle.
