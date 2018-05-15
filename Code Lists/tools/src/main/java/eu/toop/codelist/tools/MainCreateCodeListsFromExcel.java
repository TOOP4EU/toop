package eu.toop.codelist.tools;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.CodingStyleguideUnaware;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.functional.IThrowingConsumer;
import com.helger.commons.io.resource.FileSystemResource;
import com.helger.commons.io.resource.IReadableResource;
import com.helger.commons.regex.RegExHelper;
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.StringParser;
import com.helger.commons.version.Version;
import com.helger.genericode.Genericode10CodeListMarshaller;
import com.helger.genericode.Genericode10Helper;
import com.helger.genericode.excel.ExcelReadOptions;
import com.helger.genericode.excel.ExcelSheetToCodeList10;
import com.helger.genericode.v10.CodeListDocument;
import com.helger.genericode.v10.Row;
import com.helger.genericode.v10.UseType;
import com.helger.jcodemodel.JCodeModel;
import com.helger.jcodemodel.JDefinedClass;
import com.helger.jcodemodel.JDocComment;
import com.helger.jcodemodel.JEnumConstant;
import com.helger.jcodemodel.JExpr;
import com.helger.jcodemodel.JFieldVar;
import com.helger.jcodemodel.JForEach;
import com.helger.jcodemodel.JMethod;
import com.helger.jcodemodel.JMod;
import com.helger.jcodemodel.JVar;
import com.helger.jcodemodel.writer.FileCodeWriter;
import com.helger.xml.microdom.IMicroDocument;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroDocument;
import com.helger.xml.microdom.serialize.MicroWriter;
import com.helger.xml.namespace.MapBasedNamespaceContext;

/**
 * Utility class to create the Genericode files from the Excel code list. Also
 * creates Java source files with the predefined identifiers.
 *
 * @author Philip Helger
 */
public final class MainCreateCodeListsFromExcel
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (MainCreateCodeListsFromExcel.class);
  private static final Version CODELIST_VERSION = new Version (1, 0, 0);
  private static final String RESULT_XML_DIRECTORY = "../";
  private static final String RESULT_JAVA_PACKAGE = "eu.toop.commons.codelist";
  private static final JCodeModel s_aCodeModel = new JCodeModel ();
  private static final String DO_NOT_EDIT = "This file was automatically generated.\nDo NOT edit!";
  private static final boolean DEFAULT_DEPRECATED = false;

  @Nullable
  private static String _maskHtml (@Nullable final String s)
  {
    if (s == null)
      return null;
    String ret = s;
    ret = StringHelper.replaceAll (ret, "&", "&amp;");
    ret = StringHelper.replaceAll (ret, "<", "&lt;");
    ret = StringHelper.replaceAll (ret, ">", "&gt;");
    ret = StringHelper.replaceAll (ret, "\"", "&quot;");
    return ret;
  }

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
    _writeGenericodeFile (aCodeList,
                          RESULT_XML_DIRECTORY +
                                     "ToopDocumentTypeIdentifier-v" +
                                     CODELIST_VERSION.getAsString (false) +
                                     ".gc");

    // Save as XML
    final IMicroDocument aDoc = new MicroDocument ();
    aDoc.appendComment (DO_NOT_EDIT);
    final IMicroElement eRoot = aDoc.appendElement ("root");
    eRoot.setAttribute ("version", CODELIST_VERSION.getAsString ());
    for (final Row aRow : aCodeList.getSimpleCodeList ().getRow ())
    {
      final String sName = Genericode10Helper.getRowValue (aRow, "name");
      final String sDocID = Genericode10Helper.getRowValue (aRow, "doctypeid");
      final String sSince = Genericode10Helper.getRowValue (aRow, "since");
      final boolean bDeprecated = StringParser.parseBool (Genericode10Helper.getRowValue (aRow, "deprecated"),
                                                          DEFAULT_DEPRECATED);
      final String sDeprecatedSince = Genericode10Helper.getRowValue (aRow, "deprecated-since");
      if (bDeprecated && StringHelper.hasNoText (sDeprecatedSince))
        throw new IllegalStateException ("Code list entry is deprecated but there is no deprecated-since entry");

      final IMicroElement eAgency = eRoot.appendElement ("document-type");
      eAgency.setAttribute ("id", sDocID);
      eAgency.setAttribute ("name", sName);
      eAgency.setAttribute ("since", sSince);
      eAgency.setAttribute ("deprecated", bDeprecated);
      eAgency.setAttribute ("deprecated-since", sDeprecatedSince);
    }
    MicroWriter.writeToFile (aDoc,
                             new File (RESULT_XML_DIRECTORY +
                                       "ToopDocumentTypeIdentifier-v" +
                                       CODELIST_VERSION.getAsString (false) +
                                       ".xml"));

    // Create Java source
    try
    {
      final JDefinedClass jEnum = s_aCodeModel._package (RESULT_JAVA_PACKAGE)
                                              ._enum ("EPredefinedDocumentTypeIdentifier");
      jEnum.annotate (CodingStyleguideUnaware.class);
      jEnum.javadoc ().add (DO_NOT_EDIT);

      // Add all enum constants
      for (final Row aRow : aCodeList.getSimpleCodeList ().getRow ())
      {
        final String sDocTypeID = Genericode10Helper.getRowValue (aRow, "doctypeid");
        final String sName = Genericode10Helper.getRowValue (aRow, "name");
        final String sSince = Genericode10Helper.getRowValue (aRow, "since");
        final boolean bDeprecated = StringParser.parseBool (Genericode10Helper.getRowValue (aRow, "deprecated"),
                                                            DEFAULT_DEPRECATED);
        final String sDeprecatedSince = Genericode10Helper.getRowValue (aRow, "deprecated-since");
        if (bDeprecated && StringHelper.hasNoText (sDeprecatedSince))
          throw new IllegalStateException ("Code list entry is deprecated but there is no deprecated-since entry");

        final String sEnumConstName = RegExHelper.getAsIdentifier (sDocTypeID);
        final JEnumConstant jEnumConst = jEnum.enumConstant (sEnumConstName);
        if (bDeprecated)
        {
          jEnumConst.annotate (Deprecated.class);
          jEnumConst.javadoc ()
                    .add ("<b>This item is deprecated since version " +
                          sDeprecatedSince +
                          " and should not be used to issue new identifiers!</b><br>");
        }

        jEnumConst.arg (JExpr.lit (sName));
        jEnumConst.arg (JExpr.lit (sDocTypeID));
        jEnumConst.arg (s_aCodeModel.ref (Version.class).staticInvoke ("parse").arg (sSince));
        jEnumConst.javadoc ().add ("<code>" + sDocTypeID + "</code><br>");
        jEnumConst.javadoc ().addTag (JDocComment.TAG_SINCE).add ("code list " + sSince);
      }

      // constants
      final JFieldVar fScheme = jEnum.field (JMod.PUBLIC | JMod.STATIC | JMod.FINAL,
                                             String.class,
                                             "DOC_TYPE_SCHEME",
                                             JExpr.lit ("toop-doctypeid-qns"));

      // fields
      final JFieldVar fCommonName = jEnum.field (JMod.PRIVATE | JMod.FINAL, String.class, "m_sCommonName");
      final JFieldVar fDocTypeID = jEnum.field (JMod.PRIVATE | JMod.FINAL, String.class, "m_sDocTypeID");
      final JFieldVar fSince = jEnum.field (JMod.PRIVATE | JMod.FINAL, Version.class, "m_aSince");

      // Constructor
      final JMethod jCtor = jEnum.constructor (JMod.PRIVATE);
      final JVar jCommonName = jCtor.param (JMod.FINAL, String.class, "sCommonName");
      jCommonName.annotate (Nonnull.class);
      jCommonName.annotate (Nonempty.class);
      final JVar jDocTypeID = jCtor.param (JMod.FINAL, String.class, "sDocTypeID");
      jDocTypeID.annotate (Nonnull.class);
      jDocTypeID.annotate (Nonempty.class);
      final JVar jSince = jCtor.param (JMod.FINAL, Version.class, "aSince");
      jSince.annotate (Nonnull.class);
      jCtor.body ().assign (fCommonName, jCommonName).assign (fDocTypeID, jDocTypeID).assign (fSince, jSince);

      // public String getScheme ()
      JMethod m = jEnum.method (JMod.PUBLIC, String.class, "getScheme");
      m.annotate (Nonnull.class);
      m.annotate (Nonempty.class);
      m.body ()._return (fScheme);

      // public String getValue ()
      m = jEnum.method (JMod.PUBLIC, String.class, "getValue");
      m.annotate (Nonnull.class);
      m.annotate (Nonempty.class);
      m.body ()._return (fDocTypeID);

      // public String getCommonName ()
      m = jEnum.method (JMod.PUBLIC, String.class, "getCommonName");
      m.annotate (Nonnull.class);
      m.annotate (Nonempty.class);
      m.body ()._return (fCommonName);

      // public Version getSince ()
      m = jEnum.method (JMod.PUBLIC, Version.class, "getSince");
      m.annotate (Nonnull.class);
      m.body ()._return (fSince);

      // @Nullable
      // public static EPredefinedDocumentTypeIdentifier
      // getFromDocumentTypeIdentifierOrNull(@Nullable final
      // String sDocTypeID)
      m = jEnum.method (JMod.PUBLIC | JMod.STATIC, jEnum, "getFromDocumentTypeIdentifierOrNull");
      {
        m.annotate (Nullable.class);
        final JVar jValue = m.param (JMod.FINAL, String.class, "sDocTypeID");
        jValue.annotate (Nullable.class);
        final JForEach jForEach = m.body ().forEach (jEnum, "e", jEnum.staticInvoke ("values"));
        jForEach.body ()
                ._if (jForEach.var ().invoke ("getValue").invoke ("equals").arg (jValue))
                ._then ()
                ._return (jForEach.var ());
        m.body ()._return (JExpr._null ());
      }
    }
    catch (final Exception ex)
    {
      s_aLogger.warn ("Failed to create source", ex);
    }
  }

  private static void _emitParticipantIdentifierSchemes (final Sheet aParticipantSheet) throws URISyntaxException
  {
    // Read excel file
    final ExcelReadOptions <UseType> aReadOptions = new ExcelReadOptions <UseType> ().setLinesToSkip (1)
                                                                                     .setLineIndexShortName (0);
    aReadOptions.addColumn (0, "schemeid", UseType.REQUIRED, "string", true);
    aReadOptions.addColumn (1, "iso6523", UseType.REQUIRED, "string", true);
    aReadOptions.addColumn (2, "schemeagency", UseType.OPTIONAL, "string", false);
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
    _writeGenericodeFile (aCodeList,
                          RESULT_XML_DIRECTORY +
                                     "ToopParticipantIdentifierSchemes-v" +
                                     CODELIST_VERSION.getAsString (false) +
                                     ".gc");

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
                                                          DEFAULT_DEPRECATED);
      final String sDeprecatedSince = Genericode10Helper.getRowValue (aRow, "deprecated-since");
      final String sStructure = Genericode10Helper.getRowValue (aRow, "structure");
      final String sDisplay = Genericode10Helper.getRowValue (aRow, "display");
      final String sUsage = Genericode10Helper.getRowValue (aRow, "usage");

      if (StringHelper.hasNoText (sSchemeID))
        throw new IllegalArgumentException ("schemeID");
      if (sSchemeID.indexOf (' ') >= 0)
        throw new IllegalArgumentException ("Scheme IDs are not supposed to contain spaces!");
      if (StringHelper.hasNoText (sISO6523))
        throw new IllegalArgumentException ("ISO6523Code");
      if (!RegExHelper.stringMatchesPattern ("[0-9]{4}", sISO6523))
        throw new IllegalArgumentException ("The ISO 6523 code '" + sISO6523 + "' does not consist of 4 numbers");
      if (bDeprecated && StringHelper.hasNoText (sDeprecatedSince))
        throw new IllegalStateException ("Code list entry is deprecated but there is no deprecated-since entry");

      final IMicroElement eAgency = eRoot.appendElement ("identifier-scheme");
      eAgency.setAttribute ("schemeid", sSchemeID);
      eAgency.setAttribute ("agencyname", sAgency);
      eAgency.setAttribute ("iso6523", sISO6523);
      eAgency.setAttribute ("since", sSince);
      eAgency.setAttribute ("deprecated", bDeprecated);
      eAgency.setAttribute ("deprecated-since", sDeprecatedSince);
      if (StringHelper.hasText (sStructure))
        eAgency.appendElement ("structure").appendText (sStructure);
      if (StringHelper.hasText (sDisplay))
        eAgency.appendElement ("display").appendText (sDisplay);
      if (StringHelper.hasText (sUsage))
        eAgency.appendElement ("usage").appendText (sUsage);
    }
    MicroWriter.writeToFile (aDoc,
                             new File (RESULT_XML_DIRECTORY +
                                       "ToopParticipantIdentifierSchemes-v" +
                                       CODELIST_VERSION.getAsString (false) +
                                       ".xml"));

    // Create Java source
    try
    {
      final JDefinedClass jEnum = s_aCodeModel._package (RESULT_JAVA_PACKAGE)
                                              ._enum ("EPredefinedParticipantIdentifierScheme");
      jEnum.annotate (CodingStyleguideUnaware.class);
      jEnum.javadoc ().add (DO_NOT_EDIT);

      // enum constants
      for (final Row aRow : aCodeList.getSimpleCodeList ().getRow ())
      {
        final String sSchemeID = Genericode10Helper.getRowValue (aRow, "schemeid");
        final String sISO6523 = Genericode10Helper.getRowValue (aRow, "iso6523");
        final String sAgency = Genericode10Helper.getRowValue (aRow, "schemeagency");
        final String sSince = Genericode10Helper.getRowValue (aRow, "since");
        final boolean bDeprecated = StringParser.parseBool (Genericode10Helper.getRowValue (aRow, "deprecated"),
                                                            DEFAULT_DEPRECATED);
        final String sDeprecatedSince = Genericode10Helper.getRowValue (aRow, "deprecated-since");
        final String sStructure = Genericode10Helper.getRowValue (aRow, "structure");
        final String sDisplay = Genericode10Helper.getRowValue (aRow, "display");
        final String sUsage = Genericode10Helper.getRowValue (aRow, "usage");

        final JEnumConstant jEnumConst = jEnum.enumConstant (RegExHelper.getAsIdentifier (sSchemeID));
        jEnumConst.arg (JExpr.lit (sSchemeID));
        jEnumConst.arg (sAgency == null ? JExpr._null () : JExpr.lit (sAgency));
        jEnumConst.arg (JExpr.lit (sISO6523));
        jEnumConst.arg (bDeprecated ? JExpr.TRUE : JExpr.FALSE);
        jEnumConst.arg (s_aCodeModel.ref (Version.class).staticInvoke ("parse").arg (sSince));

        jEnumConst.javadoc ()
                  .add ("Prefix <code>" + sISO6523 + "</code>, scheme ID <code>" + sSchemeID + "</code><br>");
        if (bDeprecated)
        {
          jEnumConst.annotate (Deprecated.class);
          jEnumConst.javadoc ()
                    .add ("\n<b>This item is deprecated since version " +
                          sDeprecatedSince +
                          " and should not be used to issue new identifiers!</b><br>");
        }
        if (StringHelper.hasText (sStructure))
          jEnumConst.javadoc ().add ("\nStructure of the code: " + _maskHtml (sStructure) + "<br>");
        if (StringHelper.hasText (sDisplay))
          jEnumConst.javadoc ().add ("\nDisplay requirements: " + _maskHtml (sDisplay) + "<br>");
        if (StringHelper.hasText (sUsage))
          jEnumConst.javadoc ().add ("\nUsage information: " + _maskHtml (sUsage) + "<br>");
        jEnumConst.javadoc ().addTag (JDocComment.TAG_SINCE).add ("code list " + sSince);
      }

      // fields
      final JFieldVar fSchemeID = jEnum.field (JMod.PRIVATE | JMod.FINAL, String.class, "m_sSchemeID");
      final JFieldVar fSchemeAgency = jEnum.field (JMod.PRIVATE | JMod.FINAL, String.class, "m_sSchemeAgency");
      final JFieldVar fISO6523 = jEnum.field (JMod.PRIVATE | JMod.FINAL, String.class, "m_sISO6523");
      final JFieldVar fDeprecated = jEnum.field (JMod.PRIVATE | JMod.FINAL, boolean.class, "m_bDeprecated");
      final JFieldVar fSince = jEnum.field (JMod.PRIVATE | JMod.FINAL, Version.class, "m_aSince");

      // Constructor
      final JMethod jCtor = jEnum.constructor (JMod.PRIVATE);
      final JVar jSchemeID = jCtor.param (JMod.FINAL, String.class, "sSchemeID");
      jSchemeID.annotate (Nonnull.class);
      jSchemeID.annotate (Nonempty.class);
      final JVar jSchemeAgency = jCtor.param (JMod.FINAL, String.class, "sSchemeAgency");
      jSchemeAgency.annotate (Nullable.class);
      final JVar jISO6523 = jCtor.param (JMod.FINAL, String.class, "sISO6523");
      jISO6523.annotate (Nonnull.class);
      jISO6523.annotate (Nonempty.class);
      final JVar jDeprecated = jCtor.param (JMod.FINAL, boolean.class, "bDeprecated");
      final JVar jSince = jCtor.param (JMod.FINAL, Version.class, "aSince");
      jSince.annotate (Nonnull.class);
      jCtor.body ()
           .assign (fSchemeID, jSchemeID)
           .assign (fSchemeAgency, jSchemeAgency)
           .assign (fISO6523, jISO6523)
           .assign (fDeprecated, jDeprecated)
           .assign (fSince, jSince);

      // public String getSchemeID ()
      JMethod m = jEnum.method (JMod.PUBLIC, String.class, "getSchemeID");
      m.annotate (Nonnull.class);
      m.annotate (Nonempty.class);
      m.body ()._return (fSchemeID);

      // public String getSchemeAgency ()
      m = jEnum.method (JMod.PUBLIC, String.class, "getSchemeAgency");
      m.annotate (Nullable.class);
      m.body ()._return (fSchemeAgency);

      // public String getISO6523Code ()
      m = jEnum.method (JMod.PUBLIC, String.class, "getISO6523Code");
      m.annotate (Nonnull.class);
      m.annotate (Nonempty.class);
      m.body ()._return (fISO6523);

      // public boolean isDeprecated ()
      m = jEnum.method (JMod.PUBLIC, boolean.class, "isDeprecated");
      m.body ()._return (fDeprecated);

      // public Version getSince ()
      m = jEnum.method (JMod.PUBLIC, Version.class, "getSince");
      m.annotate (Nonnull.class);
      m.body ()._return (fSince);
    }
    catch (final Exception ex)
    {
      s_aLogger.warn ("Failed to create source", ex);
    }
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
    _writeGenericodeFile (aCodeList,
                          RESULT_XML_DIRECTORY +
                                     "ToopProcessIdentifier-v" +
                                     CODELIST_VERSION.getAsString (false) +
                                     ".gc");

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
                                                          DEFAULT_DEPRECATED);
      final String sDeprecatedSince = Genericode10Helper.getRowValue (aRow, "deprecated-since");

      if (bDeprecated && StringHelper.hasNoText (sDeprecatedSince))
        throw new IllegalStateException ("Code list entry is deprecated but there is no deprecated-since entry");

      final IMicroElement eAgency = eRoot.appendElement ("process");
      eAgency.setAttribute ("name", sName);
      eAgency.setAttribute ("id", sID);
      eAgency.setAttribute ("since", sSince);
      eAgency.setAttribute ("deprecated", bDeprecated);
      eAgency.setAttribute ("deprecated-since", sDeprecatedSince);
    }
    MicroWriter.writeToFile (aDoc,
                             new File (RESULT_XML_DIRECTORY +
                                       "ToopProcessIdentifier-v" +
                                       CODELIST_VERSION.getAsString (false) +
                                       ".xml"));

    // Create Java source
    try
    {
      final JDefinedClass jEnum = s_aCodeModel._package (RESULT_JAVA_PACKAGE)._enum ("EPredefinedProcessIdentifier");
      jEnum.annotate (CodingStyleguideUnaware.class);
      jEnum.javadoc ().add (DO_NOT_EDIT);

      // enum constants
      for (final Row aRow : aCodeList.getSimpleCodeList ().getRow ())
      {
        final String sName = Genericode10Helper.getRowValue (aRow, "name");
        final String sID = Genericode10Helper.getRowValue (aRow, "id");
        final String sSince = Genericode10Helper.getRowValue (aRow, "since");
        final boolean bDeprecated = StringParser.parseBool (Genericode10Helper.getRowValue (aRow, "deprecated"),
                                                            DEFAULT_DEPRECATED);
        final String sDeprecatedSince = Genericode10Helper.getRowValue (aRow, "deprecated-since");

        final String sEnumConstName = RegExHelper.getAsIdentifier (sID);
        final JEnumConstant jEnumConst = jEnum.enumConstant (sEnumConstName);
        if (bDeprecated)
        {
          jEnumConst.annotate (Deprecated.class);
          jEnumConst.javadoc ()
                    .add ("<b>This item is deprecated since version " +
                          sDeprecatedSince +
                          " and should not be used to issue new identifiers!</b><br>");
        }
        jEnumConst.arg (JExpr.lit (sName));
        jEnumConst.arg (JExpr.lit (sID));
        jEnumConst.arg (s_aCodeModel.ref (Version.class).staticInvoke ("parse").arg (sSince));
        jEnumConst.javadoc ().add ("<code>" + sID + "</code><br>");
        jEnumConst.javadoc ().addTag (JDocComment.TAG_SINCE).add ("code list " + sSince);
      }

      // constants
      final JFieldVar fScheme = jEnum.field (JMod.PUBLIC | JMod.STATIC | JMod.FINAL,
                                             String.class,
                                             "PROCESS_SCHEME",
                                             JExpr.lit ("toop-procid-agreement"));

      // fields
      final JFieldVar fName = jEnum.field (JMod.PRIVATE | JMod.FINAL, String.class, "m_sName");
      final JFieldVar fID = jEnum.field (JMod.PRIVATE | JMod.FINAL, String.class, "m_sID");
      final JFieldVar fSince = jEnum.field (JMod.PRIVATE | JMod.FINAL, Version.class, "m_aSince");

      // Constructor
      final JMethod jCtor = jEnum.constructor (JMod.PRIVATE);
      final JVar jName = jCtor.param (JMod.FINAL, String.class, "sName");
      jName.annotate (Nonnull.class);
      jName.annotate (Nonempty.class);
      final JVar jID = jCtor.param (JMod.FINAL, String.class, "sID");
      jID.annotate (Nonnull.class);
      jID.annotate (Nonempty.class);
      final JVar jSince = jCtor.param (JMod.FINAL, Version.class, "aSince");
      jSince.annotate (Nonnull.class);
      jCtor.body ().assign (fName, jName).assign (fID, jID).assign (fSince, jSince);

      // public String getScheme ()
      JMethod m = jEnum.method (JMod.PUBLIC, String.class, "getScheme");
      m.annotate (Nonnull.class);
      m.annotate (Nonempty.class);
      m.body ()._return (fScheme);

      // public String getValue ()
      m = jEnum.method (JMod.PUBLIC, String.class, "getValue");
      m.annotate (Nonnull.class);
      m.annotate (Nonempty.class);
      m.body ()._return (fID);

      // public String getCommonName ()
      m = jEnum.method (JMod.PUBLIC, String.class, "getCommonName");
      m.annotate (Nonnull.class);
      m.annotate (Nonempty.class);
      m.body ()._return (fName);

      // public Version getSince ()
      m = jEnum.method (JMod.PUBLIC, Version.class, "getSince");
      m.annotate (Nonnull.class);
      m.body ()._return (fSince);

      // @Nullable public static EPredefinedProcessIdentifier
      // getFromProcessIdentifierOrNull(@Nullable final String
      // sProcessID)
      m = jEnum.method (JMod.PUBLIC | JMod.STATIC, jEnum, "getFromProcessIdentifierOrNull");
      {
        m.annotate (Nullable.class);
        final JVar jValue = m.param (JMod.FINAL, String.class, "sProcessID");
        jValue.annotate (Nullable.class);
        final JForEach jForEach = m.body ().forEach (jEnum, "e", jEnum.staticInvoke ("values"));
        jForEach.body ()
                ._if (jForEach.var ().invoke ("getValue").invoke ("equals").arg (jValue))
                ._then ()
                ._return (jForEach.var ());
        m.body ()._return (JExpr._null ());
      }
    }
    catch (

    final Exception ex)
    {
      s_aLogger.warn ("Failed to create source", ex);
    }
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
    _writeGenericodeFile (aCodeList,
                          RESULT_XML_DIRECTORY +
                                     "ToopTransportProfile-v" +
                                     CODELIST_VERSION.getAsString (false) +
                                     ".gc");

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
                                                          DEFAULT_DEPRECATED);
      final String sDeprecatedSince = Genericode10Helper.getRowValue (aRow, "deprecated-since");

      if (bDeprecated && StringHelper.hasNoText (sDeprecatedSince))
        throw new IllegalStateException ("Code list entry is deprecated but there is no deprecated-since entry");

      final IMicroElement eAgency = eRoot.appendElement ("transport-profile");
      eAgency.setAttribute ("name", sName);
      eAgency.setAttribute ("version", sVersion);
      eAgency.setAttribute ("id", sID);
      eAgency.setAttribute ("since", sSince);
      eAgency.setAttribute ("deprecated", bDeprecated);
      eAgency.setAttribute ("deprecated-since", sDeprecatedSince);
    }
    MicroWriter.writeToFile (aDoc,
                             new File (RESULT_XML_DIRECTORY +
                                       "ToopTransportProfile-v" +
                                       CODELIST_VERSION.getAsString (false) +
                                       ".xml"));

    // Create Java source
    try
    {
      final JDefinedClass jEnum = s_aCodeModel._package (RESULT_JAVA_PACKAGE)._enum ("EPredefinedTransportProfile");
      jEnum.annotate (CodingStyleguideUnaware.class);
      jEnum.javadoc ().add (DO_NOT_EDIT);

      // enum constants
      for (final Row aRow : aCodeList.getSimpleCodeList ().getRow ())
      {
        final String sName = Genericode10Helper.getRowValue (aRow, "name") +
                             " " +
                             Genericode10Helper.getRowValue (aRow, "version");
        final String sID = Genericode10Helper.getRowValue (aRow, "id");
        final String sSince = Genericode10Helper.getRowValue (aRow, "since");
        final boolean bDeprecated = StringParser.parseBool (Genericode10Helper.getRowValue (aRow, "deprecated"),
                                                            DEFAULT_DEPRECATED);
        final String sDeprecatedSince = Genericode10Helper.getRowValue (aRow, "deprecated-since");

        final String sEnumConstName = RegExHelper.getAsIdentifier (sID);
        final JEnumConstant jEnumConst = jEnum.enumConstant (sEnumConstName);
        if (bDeprecated)
        {
          jEnumConst.annotate (Deprecated.class);
          jEnumConst.javadoc ()
                    .add ("<b>This item is deprecated since version " +
                          sDeprecatedSince +
                          " and should not be used to issue new identifiers!</b><br>");
        }
        jEnumConst.arg (JExpr.lit (sName));
        jEnumConst.arg (JExpr.lit (sID));
        jEnumConst.arg (s_aCodeModel.ref (Version.class).staticInvoke ("parse").arg (sSince));
        jEnumConst.javadoc ().add ("<code>" + sID + "</code><br>");
        jEnumConst.javadoc ().addTag (JDocComment.TAG_SINCE).add ("code list " + sSince);
      }

      // fields
      final JFieldVar fName = jEnum.field (JMod.PRIVATE | JMod.FINAL, String.class, "m_sName");
      final JFieldVar fID = jEnum.field (JMod.PRIVATE | JMod.FINAL, String.class, "m_sID");
      final JFieldVar fSince = jEnum.field (JMod.PRIVATE | JMod.FINAL, Version.class, "m_aSince");

      // Constructor
      final JMethod jCtor = jEnum.constructor (JMod.PRIVATE);
      final JVar jName = jCtor.param (JMod.FINAL, String.class, "sBISID");
      jName.annotate (Nonnull.class);
      jName.annotate (Nonempty.class);
      final JVar jID = jCtor.param (JMod.FINAL, String.class, "sID");
      jID.annotate (Nonnull.class);
      jID.annotate (Nonempty.class);
      final JVar jSince = jCtor.param (JMod.FINAL, Version.class, "aSince");
      jSince.annotate (Nonnull.class);
      jCtor.body ().assign (fName, jName).assign (fID, jID).assign (fSince, jSince);

      // public String getValue ()
      JMethod m = jEnum.method (JMod.PUBLIC, String.class, "getValue");
      m.annotate (Nonnull.class);
      m.annotate (Nonempty.class);
      m.body ()._return (fID);

      // public String getCommonName ()
      m = jEnum.method (JMod.PUBLIC, String.class, "getCommonName");
      m.annotate (Nonnull.class);
      m.annotate (Nonempty.class);
      m.body ()._return (fName);

      // public Version getSince ()
      m = jEnum.method (JMod.PUBLIC, Version.class, "getSince");
      m.annotate (Nonnull.class);
      m.body ()._return (fSince);

      // @Nullable public static EPredefinedProcessIdentifier
      // getFromProcessIdentifierOrNull(@Nullable final String
      // sProcessID)
      m = jEnum.method (JMod.PUBLIC | JMod.STATIC, jEnum, "getFromTransportProfileOrNull");
      {
        m.annotate (Nullable.class);
        final JVar jValue = m.param (JMod.FINAL, String.class, "sTransportProfileID");
        jValue.annotate (Nullable.class);
        final JForEach jForEach = m.body ().forEach (jEnum, "e", jEnum.staticInvoke ("values"));
        jForEach.body ()
                ._if (jForEach.var ().invoke ("getValue").invoke ("equals").arg (jValue))
                ._then ()
                ._return (jForEach.var ());
        m.body ()._return (JExpr._null ());
      }
    }
    catch (

    final Exception ex)
    {
      s_aLogger.warn ("Failed to create source", ex);
    }
  }

  private static final class CodeListFile
  {
    private final File m_aFile;
    private final IThrowingConsumer <? super Sheet, Exception> m_aHandler;

    public CodeListFile (@Nonnull final String sFilenamePart,
                         @Nonnull final IThrowingConsumer <? super Sheet, Exception> aHandler)
    {
      m_aFile = new File ("../TOOP CL " +
                          sFilenamePart +
                          " v" +
                          CODELIST_VERSION.getAsString (false) +
                          ".xlsx").getAbsoluteFile ();
      if (!m_aFile.exists ())
        throw new IllegalArgumentException ("File '" + m_aFile.getAbsolutePath () + "' does not exist!");
      m_aHandler = aHandler;
    }

  }

  public static void main (final String [] args) throws Exception
  {
    for (final CodeListFile aCLF : new CodeListFile [] { new CodeListFile ("Document types",
                                                                           MainCreateCodeListsFromExcel::_emitDocumentTypes),
                                                         new CodeListFile ("Participant identifier schemes",
                                                                           MainCreateCodeListsFromExcel::_emitParticipantIdentifierSchemes),
                                                         new CodeListFile ("Processes",
                                                                           MainCreateCodeListsFromExcel::_emitProcessIdentifiers),
                                                         new CodeListFile ("Transport profiles",
                                                                           MainCreateCodeListsFromExcel::_emitTransportProfiles) })
    {
      // Where is the Excel?
      final IReadableResource aXls = new FileSystemResource (aCLF.m_aFile);
      if (!aXls.exists ())
        throw new IllegalStateException ("The Excel file '" +
                                         aCLF.m_aFile.getAbsolutePath () +
                                         "' could not be found!");

      // Interprete as Excel
      try (final Workbook aWB = new XSSFWorkbook (aXls.getInputStream ()))
      {
        // Check whether all required sheets are present
        final Sheet aSheet = aWB.getSheetAt (0);
        if (aSheet == null)
          throw new IllegalStateException ("The first sheet could not be found!");

        aCLF.m_aHandler.accept (aSheet);
      }
    }

    // Write all Java source files
    final FileCodeWriter aWriter = new FileCodeWriter (new File ("../../../toop-commons/toop-commons/src/main/java"),
                                                       StandardCharsets.UTF_8);
    s_aCodeModel.build (aWriter);

    s_aLogger.info ("Done creating code");
  }
}
