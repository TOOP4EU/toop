package eu.toop.codelist.tools;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import javax.annotation.Nonnull;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.functional.IThrowingConsumer;
import com.helger.commons.io.resource.FileSystemResource;
import com.helger.commons.io.resource.IReadableResource;
import com.helger.commons.string.StringParser;
import com.helger.genericode.Genericode10CodeListMarshaller;
import com.helger.genericode.Genericode10Helper;
import com.helger.genericode.excel.ExcelReadOptions;
import com.helger.genericode.excel.ExcelSheetToCodeList10;
import com.helger.genericode.v10.CodeListDocument;
import com.helger.genericode.v10.Row;
import com.helger.genericode.v10.UseType;
import com.helger.xml.microdom.IMicroDocument;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroDocument;
import com.helger.xml.microdom.serialize.MicroWriter;
import com.helger.xml.namespace.MapBasedNamespaceContext;

import eu.toop.codelist.tools.item.AbstractToopCLItem;
import eu.toop.codelist.tools.item.ToopCLDocTypeItem;
import eu.toop.codelist.tools.item.ToopCLParticipantIdentifierSchemeItem;
import eu.toop.codelist.tools.item.ToopCLProcessItem;
import eu.toop.codelist.tools.item.ToopCLTransportProfileItem;

/**
 * Utility class to create the Genericode and XML files from the Excel code
 * list.
 *
 * @author Philip Helger
 */
public final class MainCreateCodeListsFromExcel extends AbstractMain
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (MainCreateCodeListsFromExcel.class);

  private static void _writeGenericodeFile (@Nonnull final CodeListDocument aCodeList, @Nonnull final String sFilename)
  {
    final MapBasedNamespaceContext aNsCtx = new MapBasedNamespaceContext ();
    aNsCtx.setDefaultNamespaceURI ("");
    aNsCtx.addMapping ("gc", "http://docs.oasis-open.org/codelist/ns/genericode/1.0/");
    aNsCtx.addMapping ("ext", "urn:www.helger.com:schemas:genericode-ext");

    final Genericode10CodeListMarshaller aMarshaller = new Genericode10CodeListMarshaller ();
    aMarshaller.setNamespaceContext (aNsCtx);
    aMarshaller.setFormattedOutput (true);
    if (aMarshaller.write (aCodeList, new File (sFilename)).isFailure ())
      throw new IllegalStateException ("Failed to write file " + sFilename);
    s_aLogger.info ("Wrote Genericode file " + sFilename);
  }

  private static void _emitDocumentTypes (final Sheet aDocumentSheet) throws URISyntaxException
  {
    // Create GeneriCode file
    final ExcelReadOptions <UseType> aReadOptions = new ExcelReadOptions <UseType> ().setLinesToSkip (1)
                                                                                     .setLineIndexShortName (0);
    aReadOptions.addColumn (0, "name", UseType.REQUIRED, "string", false);
    aReadOptions.addColumn (1, "doctypeid", UseType.REQUIRED, "string", true);
    aReadOptions.addColumn (2, "since", UseType.REQUIRED, "string", false);
    aReadOptions.addColumn (3, "deprecated", UseType.REQUIRED, "boolean", false);
    aReadOptions.addColumn (4, "deprecated-since", UseType.OPTIONAL, "string", false);
    final CodeListDocument aCodeList = ExcelSheetToCodeList10.convertToSimpleCodeList (aDocumentSheet,
                                                                                       aReadOptions,
                                                                                       "TOOPDocumentTypeIdentifier",
                                                                                       CODELIST_VERSION.getAsString (),
                                                                                       new URI ("urn:toop.eu:names:identifier:documenttypes"),
                                                                                       new URI ("urn:toop.eu:names:identifier:documenttypes-1.0"),
                                                                                       null);
    _writeGenericodeFile (aCodeList, getDocTypFilePrefix () + ".gc");

    // Save as XML
    final IMicroDocument aDoc = new MicroDocument ();
    aDoc.appendComment (DO_NOT_EDIT);
    final IMicroElement eRoot = aDoc.appendElement ("root");
    eRoot.setAttribute ("version", CODELIST_VERSION.getAsString ());
    for (final Row aRow : aCodeList.getSimpleCodeList ().getRow ())
    {
      final String sName = Genericode10Helper.getRowValue (aRow, "name");
      final String sDocTypeID = Genericode10Helper.getRowValue (aRow, "doctypeid");
      final String sSince = Genericode10Helper.getRowValue (aRow, "since");
      final boolean bDeprecated = StringParser.parseBool (Genericode10Helper.getRowValue (aRow, "deprecated"),
                                                          AbstractToopCLItem.DEFAULT_DEPRECATED);
      final String sDeprecatedSince = Genericode10Helper.getRowValue (aRow, "deprecated-since");

      final ToopCLDocTypeItem aItem = new ToopCLDocTypeItem (sName, sDocTypeID, sSince, bDeprecated, sDeprecatedSince);
      eRoot.appendChild (aItem.getAsMicroElement ());
    }
    MicroWriter.writeToFile (aDoc, new File (getDocTypFilePrefix () + ".xml"));
  }

  private static void _emitParticipantIdentifierSchemes (final Sheet aParticipantSheet) throws URISyntaxException
  {
    // Read excel file
    final ExcelReadOptions <UseType> aReadOptions = new ExcelReadOptions <UseType> ().setLinesToSkip (1)
                                                                                     .setLineIndexShortName (0);
    aReadOptions.addColumn (0, "schemeid", UseType.REQUIRED, "string", true);
    aReadOptions.addColumn (1, "iso6523", UseType.REQUIRED, "string", true);
    aReadOptions.addColumn (2, "schemeagency", UseType.REQUIRED, "string", false);
    aReadOptions.addColumn (3, "since", UseType.REQUIRED, "string", false);
    aReadOptions.addColumn (4, "deprecated", UseType.REQUIRED, "boolean", false);
    aReadOptions.addColumn (5, "deprecated-since", UseType.OPTIONAL, "string", false);
    aReadOptions.addColumn (6, "structure", UseType.OPTIONAL, "string", false);
    aReadOptions.addColumn (7, "display", UseType.OPTIONAL, "string", false);
    aReadOptions.addColumn (8, "usage", UseType.OPTIONAL, "string", false);

    // Convert to GeneriCode
    final CodeListDocument aCodeList = ExcelSheetToCodeList10.convertToSimpleCodeList (aParticipantSheet,
                                                                                       aReadOptions,
                                                                                       "ToopIdentifierIssuingAgencies",
                                                                                       CODELIST_VERSION.getAsString (),
                                                                                       new URI ("urn:toop.eu:names:identifier:participantidentifierschemes"),
                                                                                       new URI ("urn:toop.eu:names:identifier:participantidentifierschemes-1.0"),
                                                                                       null);
    _writeGenericodeFile (aCodeList, getParticipantIdentifierSchemesFilePrefix () + ".gc");

    // Save data also as XML
    final IMicroDocument aDoc = new MicroDocument ();
    aDoc.appendComment (DO_NOT_EDIT);
    final IMicroElement eRoot = aDoc.appendElement ("root");
    eRoot.setAttribute ("version", CODELIST_VERSION.getAsString ());
    for (final Row aRow : aCodeList.getSimpleCodeList ().getRow ())
    {
      final String sSchemeID = Genericode10Helper.getRowValue (aRow, "schemeid");
      final String sISO6523 = Genericode10Helper.getRowValue (aRow, "iso6523");
      final String sAgency = Genericode10Helper.getRowValue (aRow, "schemeagency");
      final String sSince = Genericode10Helper.getRowValue (aRow, "since");
      final boolean bDeprecated = StringParser.parseBool (Genericode10Helper.getRowValue (aRow, "deprecated"),
                                                          AbstractToopCLItem.DEFAULT_DEPRECATED);
      final String sDeprecatedSince = Genericode10Helper.getRowValue (aRow, "deprecated-since");
      final String sStructure = Genericode10Helper.getRowValue (aRow, "structure");
      final String sDisplay = Genericode10Helper.getRowValue (aRow, "display");
      final String sUsage = Genericode10Helper.getRowValue (aRow, "usage");

      final ToopCLParticipantIdentifierSchemeItem aItem = new ToopCLParticipantIdentifierSchemeItem (sSchemeID,
                                                                                                     sISO6523,
                                                                                                     sAgency,
                                                                                                     sSince,
                                                                                                     bDeprecated,
                                                                                                     sDeprecatedSince,
                                                                                                     sStructure,
                                                                                                     sDisplay,
                                                                                                     sUsage);
      eRoot.appendChild (aItem.getAsMicroElement ());
    }
    MicroWriter.writeToFile (aDoc, new File (getParticipantIdentifierSchemesFilePrefix () + ".xml"));
  }

  private static void _emitProcessIdentifiers (final Sheet aProcessSheet) throws URISyntaxException
  {
    final ExcelReadOptions <UseType> aReadOptions = new ExcelReadOptions <UseType> ().setLinesToSkip (1)
                                                                                     .setLineIndexShortName (0);
    aReadOptions.addColumn (0, "name", UseType.REQUIRED, "string", true);
    aReadOptions.addColumn (1, "id", UseType.REQUIRED, "string", true);
    aReadOptions.addColumn (2, "since", UseType.REQUIRED, "string", false);
    aReadOptions.addColumn (3, "deprecated", UseType.REQUIRED, "boolean", false);
    aReadOptions.addColumn (4, "deprecated-since", UseType.OPTIONAL, "string", false);

    final CodeListDocument aCodeList = ExcelSheetToCodeList10.convertToSimpleCodeList (aProcessSheet,
                                                                                       aReadOptions,
                                                                                       "ToopProcessIdentifier",
                                                                                       CODELIST_VERSION.getAsString (),
                                                                                       new URI ("urn:toop.eu:names:identifier:process"),
                                                                                       new URI ("urn:toop.eu:names:identifier:process-1.0"),
                                                                                       null);
    _writeGenericodeFile (aCodeList, getProcessFilePrefix () + ".gc");

    // Save as XML
    final IMicroDocument aDoc = new MicroDocument ();
    aDoc.appendComment (DO_NOT_EDIT);
    final IMicroElement eRoot = aDoc.appendElement ("root");
    eRoot.setAttribute ("version", CODELIST_VERSION.getAsString ());
    for (final Row aRow : aCodeList.getSimpleCodeList ().getRow ())
    {
      final String sName = Genericode10Helper.getRowValue (aRow, "name");
      final String sID = Genericode10Helper.getRowValue (aRow, "id");
      final String sSince = Genericode10Helper.getRowValue (aRow, "since");
      final boolean bDeprecated = StringParser.parseBool (Genericode10Helper.getRowValue (aRow, "deprecated"),
                                                          AbstractToopCLItem.DEFAULT_DEPRECATED);
      final String sDeprecatedSince = Genericode10Helper.getRowValue (aRow, "deprecated-since");

      final ToopCLProcessItem aItem = new ToopCLProcessItem (sName, sID, sSince, bDeprecated, sDeprecatedSince);
      eRoot.appendChild (aItem.getAsMicroElement ());
    }
    MicroWriter.writeToFile (aDoc, new File (getProcessFilePrefix () + ".xml"));
  }

  private static void _emitTransportProfiles (final Sheet aProcessSheet) throws URISyntaxException
  {
    final ExcelReadOptions <UseType> aReadOptions = new ExcelReadOptions <UseType> ().setLinesToSkip (1)
                                                                                     .setLineIndexShortName (0);
    aReadOptions.addColumn (0, "name", UseType.REQUIRED, "string", true);
    aReadOptions.addColumn (1, "version", UseType.REQUIRED, "string", true);
    aReadOptions.addColumn (2, "id", UseType.REQUIRED, "string", true);
    aReadOptions.addColumn (3, "since", UseType.REQUIRED, "string", false);
    aReadOptions.addColumn (4, "deprecated", UseType.REQUIRED, "boolean", false);
    aReadOptions.addColumn (5, "deprecated-since", UseType.OPTIONAL, "string", false);

    final CodeListDocument aCodeList = ExcelSheetToCodeList10.convertToSimpleCodeList (aProcessSheet,
                                                                                       aReadOptions,
                                                                                       "ToopTransportProfile",
                                                                                       CODELIST_VERSION.getAsString (),
                                                                                       new URI ("urn:toop.eu:names:identifier:transport-profile"),
                                                                                       new URI ("urn:toop.eu:names:identifier:transport-profile-1.0"),
                                                                                       null);
    _writeGenericodeFile (aCodeList, getTransportProfilesPrefix () + ".gc");

    // Save as XML
    final IMicroDocument aDoc = new MicroDocument ();
    aDoc.appendComment (DO_NOT_EDIT);
    final IMicroElement eRoot = aDoc.appendElement ("root");
    eRoot.setAttribute ("version", CODELIST_VERSION.getAsString ());
    for (final Row aRow : aCodeList.getSimpleCodeList ().getRow ())
    {
      final String sName = Genericode10Helper.getRowValue (aRow, "name");
      final String sVersion = Genericode10Helper.getRowValue (aRow, "version");
      final String sID = Genericode10Helper.getRowValue (aRow, "id");
      final String sSince = Genericode10Helper.getRowValue (aRow, "since");
      final boolean bDeprecated = StringParser.parseBool (Genericode10Helper.getRowValue (aRow, "deprecated"),
                                                          AbstractToopCLItem.DEFAULT_DEPRECATED);
      final String sDeprecatedSince = Genericode10Helper.getRowValue (aRow, "deprecated-since");

      final ToopCLTransportProfileItem aItem = new ToopCLTransportProfileItem (sName,
                                                                               sVersion,
                                                                               sID,
                                                                               sSince,
                                                                               bDeprecated,
                                                                               sDeprecatedSince);
      eRoot.appendChild (aItem.getAsMicroElement ());
    }
    MicroWriter.writeToFile (aDoc, new File (getTransportProfilesPrefix () + ".xml"));
  }

  private static final class CodeListFile
  {
    private final File m_aFile;
    private final IThrowingConsumer <? super Sheet, Exception> m_aHandler;

    public CodeListFile (@Nonnull final String sFilenamePart,
                         @Nonnull final IThrowingConsumer <? super Sheet, Exception> aHandler)
    {
      m_aFile = new File ("../Toop" +
                          sFilenamePart +
                          "-v" +
                          CODELIST_VERSION.getAsString (false) +
                          ".xlsx").getAbsoluteFile ();
      if (!m_aFile.exists ())
        throw new IllegalArgumentException ("File '" + m_aFile.getAbsolutePath () + "' does not exist!");
      m_aHandler = aHandler;
    }
  }

  public static void main (final String [] args) throws Exception
  {
    for (final CodeListFile aCLF : new CodeListFile [] { new CodeListFile ("DocumentTypeIdentifiers",
                                                                           MainCreateCodeListsFromExcel::_emitDocumentTypes),
                                                         new CodeListFile ("ParticipantIdentifierSchemes",
                                                                           MainCreateCodeListsFromExcel::_emitParticipantIdentifierSchemes),
                                                         new CodeListFile ("ProcessIdentifiers",
                                                                           MainCreateCodeListsFromExcel::_emitProcessIdentifiers),
                                                         new CodeListFile ("TransportProfiles",
                                                                           MainCreateCodeListsFromExcel::_emitTransportProfiles) })
    {
      // Where is the Excel?
      final IReadableResource aXls = new FileSystemResource (aCLF.m_aFile);
      if (!aXls.exists ())
        throw new IllegalStateException ("The Excel file '" +
                                         aCLF.m_aFile.getAbsolutePath () +
                                         "' could not be found!");

      // Interpret as Excel
      try (final Workbook aWB = new XSSFWorkbook (aXls.getInputStream ()))
      {
        // Check whether all required sheets are present
        final Sheet aSheet = aWB.getSheetAt (0);
        if (aSheet == null)
          throw new IllegalStateException ("The first sheet could not be found!");

        aCLF.m_aHandler.accept (aSheet);
      }
    }

    s_aLogger.info ("Done creating code lists");
  }
}
