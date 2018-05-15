package eu.toop.codelist.tools;

import java.io.File;
import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.CodingStyleguideUnaware;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.functional.IThrowingConsumer;
import com.helger.commons.regex.RegExHelper;
import com.helger.commons.string.StringHelper;
import com.helger.commons.version.Version;
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
import com.helger.xml.microdom.serialize.MicroReader;

import eu.toop.codelist.tools.item.ToopCLDocTypeItem;
import eu.toop.codelist.tools.item.ToopCLParticipantIdentifierSchemeItem;
import eu.toop.codelist.tools.item.ToopCLProcessItem;
import eu.toop.codelist.tools.item.ToopCLTransportProfileItem;

/**
 * Main class to create Java source code from the XML file created in
 * {@link #MainCreateJavaCodeFromCodeList()}
 *
 * @author Philip Helger
 */
public final class MainCreateJavaCodeFromCodeList extends AbstractMain
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (MainCreateJavaCodeFromCodeList.class);
  private static final String RESULT_JAVA_PACKAGE = "eu.toop.commons.codelist";
  private static final JCodeModel s_aCodeModel = new JCodeModel ();

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

  private static void _emitDocumentTypes (final IMicroElement aRootElement)
  {
    // Create Java source
    try
    {
      final JDefinedClass jEnum = s_aCodeModel._package (RESULT_JAVA_PACKAGE)
                                              ._enum ("EPredefinedDocumentTypeIdentifier");
      jEnum.annotate (CodingStyleguideUnaware.class);
      jEnum.javadoc ().add (DO_NOT_EDIT);

      // Add all enum constants
      for (final IMicroElement eItem : aRootElement.getAllChildElements ())
      {
        final ToopCLDocTypeItem aItem = ToopCLDocTypeItem.create (eItem);

        final String sEnumConstName = RegExHelper.getAsIdentifier (aItem.getID ());
        final JEnumConstant jEnumConst = jEnum.enumConstant (sEnumConstName);
        if (aItem.isDeprecated ())
        {
          jEnumConst.annotate (Deprecated.class);
          jEnumConst.javadoc ()
                    .add ("<b>This item is deprecated since version " +
                          aItem.getDeprecatedSince () +
                          " and should not be used to issue new identifiers!</b><br>");
        }

        jEnumConst.arg (JExpr.lit (aItem.getName ()));
        jEnumConst.arg (JExpr.lit (aItem.getID ()));
        jEnumConst.arg (s_aCodeModel.ref (Version.class).staticInvoke ("parse").arg (aItem.getSince ()));
        jEnumConst.javadoc ().add ("<code>" + aItem.getID () + "</code><br>");
        jEnumConst.javadoc ().addTag (JDocComment.TAG_SINCE).add ("code list v" + aItem.getSince ());
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

  private static void _emitParticipantIdentifierSchemes (final IMicroElement aRootElement)
  {
    // Create Java source
    try
    {
      final JDefinedClass jEnum = s_aCodeModel._package (RESULT_JAVA_PACKAGE)
                                              ._enum ("EPredefinedParticipantIdentifierScheme");
      jEnum.annotate (CodingStyleguideUnaware.class);
      jEnum.javadoc ().add (DO_NOT_EDIT);

      // enum constants
      for (final IMicroElement eItem : aRootElement.getAllChildElements ())
      {
        final ToopCLParticipantIdentifierSchemeItem aItem = ToopCLParticipantIdentifierSchemeItem.create (eItem);
        final String sSchemeID = aItem.getSchemeID ();
        final String sISO6523 = aItem.getID ();
        final String sAgency = aItem.getName ();
        final String sSince = aItem.getSince ();
        final boolean bDeprecated = aItem.isDeprecated ();
        final String sDeprecatedSince = aItem.getDeprecatedSince ();
        final String sStructure = aItem.getStructure ();
        final String sDisplay = aItem.getDisplay ();
        final String sUsage = aItem.getUsage ();

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

  private static void _emitProcessIdentifiers (final IMicroElement aRootElement)
  {
    // Create Java source
    try
    {
      final JDefinedClass jEnum = s_aCodeModel._package (RESULT_JAVA_PACKAGE)._enum ("EPredefinedProcessIdentifier");
      jEnum.annotate (CodingStyleguideUnaware.class);
      jEnum.javadoc ().add (DO_NOT_EDIT);

      // enum constants
      for (final IMicroElement eItem : aRootElement.getAllChildElements ())
      {
        final ToopCLProcessItem aItem = ToopCLProcessItem.create (eItem);
        final String sID = aItem.getID ();
        final String sName = aItem.getName ();
        final String sSince = aItem.getSince ();
        final boolean bDeprecated = aItem.isDeprecated ();
        final String sDeprecatedSince = aItem.getDeprecatedSince ();

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

  private static void _emitTransportProfiles (final IMicroElement aRootElement)
  {
    // Create Java source
    try
    {
      final JDefinedClass jEnum = s_aCodeModel._package (RESULT_JAVA_PACKAGE)._enum ("EPredefinedTransportProfile");
      jEnum.annotate (CodingStyleguideUnaware.class);
      jEnum.javadoc ().add (DO_NOT_EDIT);

      // enum constants
      for (final IMicroElement eItem : aRootElement.getAllChildElements ())
      {
        final ToopCLTransportProfileItem aItem = ToopCLTransportProfileItem.create (eItem);
        final String sName = aItem.getName () + " " + aItem.getVersion ();
        final String sID = aItem.getID ();
        final String sSince = aItem.getSince ();
        final boolean bDeprecated = aItem.isDeprecated ();
        final String sDeprecatedSince = aItem.getDeprecatedSince ();

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
    private final IThrowingConsumer <? super IMicroElement, Exception> m_aHandler;

    public CodeListFile (@Nonnull final String sFilenamePart,
                         @Nonnull final IThrowingConsumer <? super IMicroElement, Exception> aHandler)
    {
      m_aFile = new File (sFilenamePart + ".xml").getAbsoluteFile ();
      if (!m_aFile.exists ())
        throw new IllegalArgumentException ("File '" + m_aFile.getAbsolutePath () + "' does not exist!");
      m_aHandler = aHandler;
    }
  }

  public static void main (final String [] args) throws Exception
  {
    for (final CodeListFile aCLF : new CodeListFile [] { new CodeListFile (getDocTypFilePrefix (),
                                                                           MainCreateJavaCodeFromCodeList::_emitDocumentTypes),
                                                         new CodeListFile (getParticipantIdentifierSchemesFilePrefix (),
                                                                           MainCreateJavaCodeFromCodeList::_emitParticipantIdentifierSchemes),
                                                         new CodeListFile (getProcessFilePrefix (),
                                                                           MainCreateJavaCodeFromCodeList::_emitProcessIdentifiers),
                                                         new CodeListFile (getTransportProfilesPrefix (),
                                                                           MainCreateJavaCodeFromCodeList::_emitTransportProfiles) })
    {
      final IMicroDocument aDoc = MicroReader.readMicroXML (aCLF.m_aFile);
      if (aDoc == null)
        throw new IllegalStateException ("The XML file '" + aCLF.m_aFile.getAbsolutePath () + "' could not be found!");

      aCLF.m_aHandler.accept (aDoc.getDocumentElement ());
    }

    // Write all Java source files
    final FileCodeWriter aWriter = new FileCodeWriter (new File ("../../../toop-commons/toop-commons/src/main/java"),
                                                       StandardCharsets.UTF_8);
    s_aCodeModel.build (aWriter);

    s_aLogger.info ("Done creating code");
  }
}
