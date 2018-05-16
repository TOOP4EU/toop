# SMP Configuration template

This page contains the template configuration for phoss SMP to be deployed at member state level.

The main WAR file of the phoss SMP server to be used in a Java Application Server (like Tomcat) can be retrieved from http://repo2.maven.org/maven2/com/helger/peppol-smp-server-webapp-xml/5.0.5/peppol-smp-server-webapp-xml-5.0.5.war or see https://github.com/phax/peppol-smp-server/wiki/Download for further options (like a standalone Docker image). 

## Tasks

* Create a new JKS keystore with a single SMP key and name the file `playground-keystore.jks`
    * This file must be referenced in the files below
* Edit `smp-server.properties` (SMP Server configuration file)
    * Property `sml.smpid` must be set
    * All properties starting with `smp.keystore.` must be adopted
* Edit `pd-client.properties` (TOOP Directory Client configuration file)
    * All properties starting with `keystore.` must be adopted. The values are identical to the ones in `smp-server.properties`
* Edit `webapp.properties` (SMP Server UI configuration file)
    * Property `webapp.datapath` must be set to the absolute directory, where the persistent runtime data is store. When using the provided Docker images, you can stick to `/toop-dir/smp/data` and don't need to change anything.

Note: all properties that need modification are also marked with `[CHANGEME]` in the respective files.

Note: a description on all phoss SMP server configuration items is available at https://github.com/phax/peppol-smp-server/wiki/Configuration
