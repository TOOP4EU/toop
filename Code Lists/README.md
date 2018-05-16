# TOOP Code lists

This folder contains the TOOP code lists for:
* Participant identifier schemes
* Document types
* Processes
* Transport profiles

## Version history

* v1 - work in progress
    * Initial version

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
