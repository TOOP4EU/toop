# TOOP Connector Configuration template

This page contains the template configuration for TOOP Connector (TC( to be deployed at member state level.

The main WAR file of the TC to be used in a Java Application Server (like Tomcat) can be retrieved from http://repo2.maven.apache.org/maven2/eu/toop/toop-connector-webapp/0.9.1/toop-connector-webapp-0.9.1.war. 

## Tasks

* Create a new JKS keystore with a single TC key and name the file `playground-keystore.jks`
    * This file must be referenced in the files below
* Edit `toop-connector.properties` (TOOP Connector configuration file)
    * Property `toop.instancename` should contain the memberstate country code and name. Short free text no longer than 25 chars.
    * Property `toop.mem.as4.endpoint` contains the URL to the AS4 implementation
    * Property `toop.mem.as4.gw.partyid` contains the Party ID that must match the AS4 config
    * ...

Note: all properties that need modification are also marked with `[CHANGEME]` in the respective files.
