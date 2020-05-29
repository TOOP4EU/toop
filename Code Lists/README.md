# TOOP Code lists

This folder contains the TOOP code lists for:
* Participant identifier schemes
* Document types
* Processes
* Transport profiles

Latest release: **v3**

## Version history

* v3 - 2020-05-29
    * Document type identifiers 
      * Added "EDM Concept Request on RegisteredOrganisation Data" - `RegisteredOrganization::REGISTERED_ORGANIZATION_TYPE::CONCEPT##CCCEV::toop-edm:v2.0`
      * Added "EDM Document Request on Fincancial Record" - `FinancialRecord::FINANCIAL_RECORD_TYPE::UNSTRUCTURED::toop-edm:v2.0`
      * Added "EDM Response" - `QueryResponse::toop-edm:v2.0`
    * Process identifiers
      * Added "TOOP DataQuery" - `urn:eu.toop.process.dataquery`
      * Added "TOOP DocumentQuery" - `urn:eu.toop.process.documentquery`
      * Added "TOOP DocumentReferenceQuery" - `urn:eu.toop.process.documentreferencequery`

* v2 - 2019-03-20
    * Document type identifiers 
        * Added new identifiers for data model version 1.4.0
        * Added new WP3 document type `urn:eu:toop:ns:dataexchange-1p40::Request##urn:eu.toop.request.shipcertificate-list::1.40`
        * Added new WP3 document type `urn:eu:toop:ns:dataexchange-1p40::Response##urn:eu.toop.response.shipcertificate-list::1.40`
        * Added new WP3 document type `urn:eu:toop:ns:dataexchange-1p40::Request##urn:eu.toop.request.shipcertificate::1.40`
        * Added new WP3 document type `urn:eu:toop:ns:dataexchange-1p40::Response##urn:eu.toop.response.shipcertificate::1.40`
        * Added new WP3 document type `urn:eu:toop:ns:dataexchange-1p40::Request##urn:eu.toop.request.crewcertificate-list::1.40`
        * Added new WP3 document type `urn:eu:toop:ns:dataexchange-1p40::Response##urn:eu.toop.response.crewcertificate-list::1.40`
        * Added new WP3 document type `urn:eu:toop:ns:dataexchange-1p40::Request##urn:eu.toop.request.crewcertificate::1.40`
        * Added new WP3 document type `urn:eu:toop:ns:dataexchange-1p40::Response##urn:eu.toop.response.crewcertificate::1.40`
        * Added new WP3 document type `urn:eu:toop:ns:dataexchange-1p40::Request##urn:eu.toop.request.registeredorganization-list::1.40`
        * Added new WP3 document type `urn:eu:toop:ns:dataexchange-1p40::Response##urn:eu.toop.response.registeredorganization-list::1.40`
        * Added new WP3 document type `urn:eu:toop:ns:dataexchange-1p40::Request##urn:eu.toop.request.registeredorganization::1.40`
        * Added new WP3 document type `urn:eu:toop:ns:dataexchange-1p40::Response##urn:eu.toop.response.registeredorganization::1.40`
        * Added new WP3 document type `urn:eu:toop:ns:dataexchange-1p40::Request##urn:eu.toop.request.evidence-list::1.40`
        * Added new WP3 document type `urn:eu:toop:ns:dataexchange-1p40::Response##urn:eu.toop.response.evidence-list::1.40`
        * Added new WP3 document type `urn:eu:toop:ns:dataexchange-1p40::Request##urn:eu.toop.request.evidence::1.40`
        * Added new WP3 document type `urn:eu:toop:ns:dataexchange-1p40::Response##urn:eu.toop.response.evidence::1.40`
    * Participant identifier schemes
        * Added new column C "Country"
        * Added new column D "Scheme name"
        * Added 0130, 0193, 0195, 0196, 9901, 9902, 9904, 9905, 9906, 9907, 9908, 9910, 9913, 9914, 9915, 9918, 9919, 9920, 9921, 9922, 9923, 9924, 9925, 9926, 9927, 9928, 9929, 9930, 9931, 9932, 9933, 9934, 9935, 9936, 9937, 9938, 9939, 9940, 9941, 9942, 9943, 9944, 9945, 9946, 9947, 9948, 9949, 9950, 9951, 9952, 9953, 9955, 9956, 9957, 9958 for alignment with PEPPOL CodeList v4
    * Process identifiers
        * Added `TOOP Two Phased Request Response for Documents`
        
* v1 - 2018-06-01
    * Initial version, since SW release 0.9.1

# Usage notes

It is important to note that this is a dynamic list. Over time new services will be added. Developers should take this into account when designing and implementing solutions for TOOP services.

## Participant Identifier Schemes

Rows marked as "deprecated" should not be used for newly issued documents, as the respective identifier scheme is no longer active/valid. Deprecated scheme IDs may however not be reused for different agencies as existing exchanged documents may refer to them.

## Document types

Rows marked as "deprecated" should not be used for newly issued documents.

## Processes

Rows marked as "deprecated" should not be used for newly issued documents.

# Maintenance notes

The main artefacts are the Excel files (.xlsx).
Afterwards a small Java tool creates the GC and XML file (see `tools`) folder. 
Additionally some Java enumerations can be created from the updated XML files.
