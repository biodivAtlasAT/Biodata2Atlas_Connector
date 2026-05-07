**Connects the BIODATA application to Atlas**

The app makes the following steps:
1. Login (to get authorization token)
2. fetch all projects, which should be synchronized with the atlas
3. for each project start the atlas ingest process (delete and import)
4. the run is documented in the log files

The configuration file is: "Biodata2Atlas.properties" which is a parmeter for the script
example: java -jar Biodata2Atlas_Connector-1.0.jar -v -c Biodata2Atlas.properties

The app is compiled for Java 11
