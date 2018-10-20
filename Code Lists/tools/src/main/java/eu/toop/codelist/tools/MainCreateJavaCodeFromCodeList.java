package eu.toop.codelist.tools;

import java.io.File;
import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.CodingStyleguideUnaware;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.impl.CommonsHashSet;
import com.helger.commons.collection.impl.ICommonsSet;
import com.helger.commons.functional.IThrowingConsumer;
import com.helger.commons.regex.RegExHelper;
import com.helger.commons.string.StringHelper;
import com.helger.commons.version.Version;
import com.helger.jcodemodel.JBlock;
import com.helger.jcodemodel.JClassAlreadyExistsException;
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
import com.helger.jcodemodel.writer.JCMWriter;
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

  @Nonnull
  private static String _getIdentifier (@Nonnull final String sID)
  {
    String ret = RegExHelper.getAsIdentifier (sID.toUpperCase (Locale.US), "_");
    ret = StringHelper.replaceAllRepeatedly (ret, "__", "_");
    return ret;
  }

  @Nullable
  private static String _createDocTypeShortcut (@Nonnull final String sID)
  {
    String sSubType = StringHelper.getExploded ("##", sID, 2).get (1);
    sSubType = StringHelper.trimStart (sSubType, "urn:eu.toop.");
    sSubType = StringHelper.getExploded ("::", sSubType, 2).get (0);
    return _getIdentifier (sSubType);
  }

  @Nullable
  private static String _createProcessShortcut (@Nonnull final String sID)
  {
    final String sSubType = StringHelper.trimStart (sID, "urn:eu.toop.process.");
    return _getIdentifier (sSubType);
  }

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

  private static void _emitDocumentTypes (final IMicroElement aRootElement) throws JClassAlreadyExistsException
  {
    final JDefinedClass jEnum = s_aCodeModel._package (RESULT_JAVA_PACKAGE)
                                            ._enum ("EPredefinedDocumentTypeIdentifier")
                                            ._implements (s_aCodeModel.ref (RESULT_JAVA_PACKAGE +
                                                                            ".IPredefinedIdentifier"));
    jEnum.annotate (CodingStyleguideUnaware.class);
    jEnum.javadoc ().add (DO_NOT_EDIT);

    // Add all enum constants
    final ICommonsSet <String> aUsedShortcuts = new CommonsHashSet <> ();
    for (final IMicroElement eItem : aRootElement.getAllChildElements ())
    {
      final ToopCLDocTypeItem aItem = ToopCLDocTypeItem.create (eItem);
      final String sName = aItem.getName ();
      final String sID = aItem.getID ();
      final String sSince = aItem.getSince ();
      final boolean bDeprecated = aItem.isDeprecated ();
      final String sDeprecatedSince = aItem.getDeprecatedSince ();

      final JEnumConstant jEnumConst = jEnum.enumConstant (_getIdentifier (sID));
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
      jEnumConst.arg (bDeprecated ? JExpr.TRUE : JExpr.FALSE);
      jEnumConst.arg (StringHelper.hasNoText (sDeprecatedSince) ? JExpr._null ()
                                                                : s_aCodeModel.ref (Version.class)
                                                                              .staticInvoke ("parse")
                                                                              .arg (sDeprecatedSince));
      jEnumConst.javadoc ().add (sName + " - <code>" + sID + "</code><br>");
      jEnumConst.javadoc ().addTag (JDocComment.TAG_SINCE).add ("code list v" + sSince);

      final String sShortcut = _createDocTypeShortcut (sID);
      if (sShortcut != null && aUsedShortcuts.add (sShortcut))
        jEnum.field (JMod.PUBLIC | JMod.STATIC | JMod.FINAL, jEnum, sShortcut, jEnumConst);
    }

    // constants
    final JFieldVar fScheme = jEnum.field (JMod.PUBLIC | JMod.STATIC | JMod.FINAL,
                                           String.class,
                                           "DOC_TYPE_SCHEME",
                                           JExpr.lit ("toop-doctypeid-qns"));

    // fields
    final JFieldVar fName = jEnum.field (JMod.PRIVATE | JMod.FINAL, String.class, "m_sName");
    final JFieldVar fID = jEnum.field (JMod.PRIVATE | JMod.FINAL, String.class, "m_sID");
    final JFieldVar fSince = jEnum.field (JMod.PRIVATE | JMod.FINAL, Version.class, "m_aSince");
    final JFieldVar fDeprecated = jEnum.field (JMod.PRIVATE | JMod.FINAL, boolean.class, "m_bDeprecated");
    final JFieldVar fDeprecatedSince = jEnum.field (JMod.PRIVATE | JMod.FINAL, Version.class, "m_aDeprecatedSince");

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
    final JVar jDeprecated = jCtor.param (JMod.FINAL, boolean.class, "bDeprecated");
    final JVar jDeprecatedSince = jCtor.param (JMod.FINAL, Version.class, "aDeprecatedSince");
    jDeprecatedSince.annotate (Nullable.class);
    jCtor.body ()
         .assign (fName, jName)
         .assign (fID, jID)
         .assign (fSince, jSince)
         .assign (fDeprecated, jDeprecated)
         .assign (fDeprecatedSince, jDeprecatedSince);

    JMethod m;

    // public String getName ()
    m = jEnum.method (JMod.PUBLIC, String.class, "getName");
    m.annotate (Nonnull.class);
    m.annotate (Nonempty.class);
    m.body ()._return (fName);

    // public String getScheme ()
    m = jEnum.method (JMod.PUBLIC, String.class, "getScheme");
    m.annotate (Nonnull.class);
    m.annotate (Nonempty.class);
    m.body ()._return (fScheme);

    // public String getID ()
    m = jEnum.method (JMod.PUBLIC, String.class, "getID");
    m.annotate (Nonnull.class);
    m.annotate (Nonempty.class);
    m.body ()._return (fID);

    // public Version getSince ()
    m = jEnum.method (JMod.PUBLIC, Version.class, "getSince");
    m.annotate (Nonnull.class);
    m.body ()._return (fSince);

    // public boolean isDeprecated ()
    m = jEnum.method (JMod.PUBLIC, boolean.class, "isDeprecated");
    m.body ()._return (fDeprecated);

    // public Version getDeprecatedSince ()
    m = jEnum.method (JMod.PUBLIC, Version.class, "getDeprecatedSince");
    m.annotate (Nullable.class);
    m.body ()._return (fDeprecatedSince);

    // @Nullable
    // public static EPredefinedDocumentTypeIdentifier
    // getFromDocumentTypeIdentifierOrNull(@Nullable final
    // String sDocTypeID)
    m = jEnum.method (JMod.PUBLIC | JMod.STATIC, jEnum, "getFromDocumentTypeIdentifierOrNull");
    {
      m.annotate (Nullable.class);
      final JVar jArgID = m.param (JMod.FINAL, String.class, "sID");
      jArgID.annotate (Nullable.class);
      final JBlock jIf = m.body ()
                          ._if (s_aCodeModel.ref (StringHelper.class).staticInvoke ("hasText").arg (jArgID))
                          ._then ();
      final JForEach jForEach = jIf.forEach (jEnum, "e", jEnum.staticInvoke ("values"));
      jForEach.body ()
              ._if (jForEach.var ().invoke ("getID").invoke ("equals").arg (jArgID))
              ._then ()
              ._return (jForEach.var ());
      m.body ()._return (JExpr._null ());
    }

    // @Nullable
    // public static EPredefinedDocumentTypeIdentifier
    // getFromDocumentTypeIdentifierOrNull(@Nullable final String sScheme,
    // @Nullable final String sID)
    m = jEnum.method (JMod.PUBLIC | JMod.STATIC, jEnum, "getFromDocumentTypeIdentifierOrNull");
    {
      m.annotate (Nullable.class);
      final JVar jArgScheme = m.param (JMod.FINAL, String.class, "sScheme");
      jArgScheme.annotate (Nullable.class);
      final JVar jArgID = m.param (JMod.FINAL, String.class, "sID");
      jArgID.annotate (Nullable.class);
      final JBlock jIf = m.body ()
                          ._if (s_aCodeModel.ref (StringHelper.class)
                                            .staticInvoke ("hasText")
                                            .arg (jArgScheme)
                                            .cand (s_aCodeModel.ref (StringHelper.class)
                                                               .staticInvoke ("hasText")
                                                               .arg (jArgID)))
                          ._then ();
      final JForEach jForEach = jIf.forEach (jEnum, "e", jEnum.staticInvoke ("values"));
      jForEach.body ()
              ._if (jForEach.var ()
                            .invoke ("getScheme")
                            .invoke ("equals")
                            .arg (jArgScheme)
                            .cand (jForEach.var ().invoke ("getID").invoke ("equals").arg (jArgID)))
              ._then ()
              ._return (jForEach.var ());
      m.body ()._return (JExpr._null ());
    }
  }

  private static void _emitParticipantIdentifierSchemes (final IMicroElement aRootElement) throws JClassAlreadyExistsException
  {
    final JDefinedClass jEnum = s_aCodeModel._package (RESULT_JAVA_PACKAGE)
                                            ._enum ("EPredefinedParticipantIdentifierScheme")
                                            ._implements (s_aCodeModel.ref (RESULT_JAVA_PACKAGE + ".IPredefined"));
    jEnum.annotate (CodingStyleguideUnaware.class);
    jEnum.javadoc ().add (DO_NOT_EDIT);

    // enum constants
    for (final IMicroElement eItem : aRootElement.getAllChildElements ())
    {
      final ToopCLParticipantIdentifierSchemeItem aItem = ToopCLParticipantIdentifierSchemeItem.create (eItem);
      final String sID = aItem.getID ();
      final String sName = aItem.getName ();
      final String sSince = aItem.getSince ();
      final boolean bDeprecated = aItem.isDeprecated ();
      final String sDeprecatedSince = aItem.getDeprecatedSince ();
      final String sStructure = aItem.getStructure ();
      final String sDisplay = aItem.getDisplay ();
      final String sUsage = aItem.getUsage ();

      final JEnumConstant jEnumConst = jEnum.enumConstant (_getIdentifier (aItem.getSchemeID ()));
      jEnumConst.arg (JExpr.lit (sName));
      jEnumConst.arg (JExpr.lit (sID));
      jEnumConst.arg (s_aCodeModel.ref (Version.class).staticInvoke ("parse").arg (sSince));
      jEnumConst.arg (bDeprecated ? JExpr.TRUE : JExpr.FALSE);
      jEnumConst.arg (StringHelper.hasNoText (sDeprecatedSince) ? JExpr._null ()
                                                                : s_aCodeModel.ref (Version.class)
                                                                              .staticInvoke ("parse")
                                                                              .arg (sDeprecatedSince));

      jEnumConst.javadoc ().add (sName + " - <code>" + sID + "</code>");
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
      jEnumConst.javadoc ().addTag (JDocComment.TAG_SINCE).add ("code list v" + sSince);
    }

    // fields
    final JFieldVar fName = jEnum.field (JMod.PRIVATE | JMod.FINAL, String.class, "m_sName");
    final JFieldVar fID = jEnum.field (JMod.PRIVATE | JMod.FINAL, String.class, "m_sID");
    final JFieldVar fSince = jEnum.field (JMod.PRIVATE | JMod.FINAL, Version.class, "m_aSince");
    final JFieldVar fDeprecated = jEnum.field (JMod.PRIVATE | JMod.FINAL, boolean.class, "m_bDeprecated");
    final JFieldVar fDeprecatedSince = jEnum.field (JMod.PRIVATE | JMod.FINAL, Version.class, "m_aDeprecatedSince");

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
    final JVar jDeprecated = jCtor.param (JMod.FINAL, boolean.class, "bDeprecated");
    final JVar jDeprecatedSince = jCtor.param (JMod.FINAL, Version.class, "aDeprecatedSince");
    jDeprecatedSince.annotate (Nullable.class);
    jCtor.body ()
         .assign (fName, jName)
         .assign (fID, jID)
         .assign (fSince, jSince)
         .assign (fDeprecated, jDeprecated)
         .assign (fDeprecatedSince, jDeprecatedSince);

    JMethod m;

    // public String getName ()
    m = jEnum.method (JMod.PUBLIC, String.class, "getName");
    m.annotate (Nonnull.class);
    m.annotate (Nonempty.class);
    m.body ()._return (fName);

    // public String getID ()
    m = jEnum.method (JMod.PUBLIC, String.class, "getID");
    m.annotate (Nonnull.class);
    m.annotate (Nonempty.class);
    m.body ()._return (fID);

    // public Version getSince ()
    m = jEnum.method (JMod.PUBLIC, Version.class, "getSince");
    m.annotate (Nonnull.class);
    m.body ()._return (fSince);

    // public boolean isDeprecated ()
    m = jEnum.method (JMod.PUBLIC, boolean.class, "isDeprecated");
    m.body ()._return (fDeprecated);

    // public Version getDeprecatedSince ()
    m = jEnum.method (JMod.PUBLIC, Version.class, "getDeprecatedSince");
    m.annotate (Nullable.class);
    m.body ()._return (fDeprecatedSince);
  }

  private static void _emitProcessIdentifiers (final IMicroElement aRootElement) throws JClassAlreadyExistsException
  {
    final JDefinedClass jEnum = s_aCodeModel._package (RESULT_JAVA_PACKAGE)
                                            ._enum ("EPredefinedProcessIdentifier")
                                            ._implements (s_aCodeModel.ref (RESULT_JAVA_PACKAGE +
                                                                            ".IPredefinedIdentifier"));
    jEnum.annotate (CodingStyleguideUnaware.class);
    jEnum.javadoc ().add (DO_NOT_EDIT);

    // enum constants
    final ICommonsSet <String> aUsedShortcuts = new CommonsHashSet <> ();
    for (final IMicroElement eItem : aRootElement.getAllChildElements ())
    {
      final ToopCLProcessItem aItem = ToopCLProcessItem.create (eItem);
      final String sID = aItem.getID ();
      final String sName = aItem.getName ();
      final String sSince = aItem.getSince ();
      final boolean bDeprecated = aItem.isDeprecated ();
      final String sDeprecatedSince = aItem.getDeprecatedSince ();

      final JEnumConstant jEnumConst = jEnum.enumConstant (_getIdentifier (sID));
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
      jEnumConst.arg (bDeprecated ? JExpr.TRUE : JExpr.FALSE);
      jEnumConst.arg (StringHelper.hasNoText (sDeprecatedSince) ? JExpr._null ()
                                                                : s_aCodeModel.ref (Version.class)
                                                                              .staticInvoke ("parse")
                                                                              .arg (sDeprecatedSince));
      jEnumConst.javadoc ().add (sName + " - <code>" + sID + "</code><br>");
      jEnumConst.javadoc ().addTag (JDocComment.TAG_SINCE).add ("code list v" + sSince);

      final String sShortcut = _createProcessShortcut (sID);
      if (sShortcut != null && aUsedShortcuts.add (sShortcut))
        jEnum.field (JMod.PUBLIC | JMod.STATIC | JMod.FINAL, jEnum, sShortcut, jEnumConst);
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
    final JFieldVar fDeprecated = jEnum.field (JMod.PRIVATE | JMod.FINAL, boolean.class, "m_bDeprecated");
    final JFieldVar fDeprecatedSince = jEnum.field (JMod.PRIVATE | JMod.FINAL, Version.class, "m_aDeprecatedSince");

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
    final JVar jDeprecated = jCtor.param (JMod.FINAL, boolean.class, "bDeprecated");
    final JVar jDeprecatedSince = jCtor.param (JMod.FINAL, Version.class, "aDeprecatedSince");
    jDeprecatedSince.annotate (Nullable.class);
    jCtor.body ()
         .assign (fName, jName)
         .assign (fID, jID)
         .assign (fSince, jSince)
         .assign (fDeprecated, jDeprecated)
         .assign (fDeprecatedSince, jDeprecatedSince);

    JMethod m;

    // public String getName ()
    m = jEnum.method (JMod.PUBLIC, String.class, "getName");
    m.annotate (Nonnull.class);
    m.annotate (Nonempty.class);
    m.body ()._return (fName);

    // public String getScheme ()
    m = jEnum.method (JMod.PUBLIC, String.class, "getScheme");
    m.annotate (Nonnull.class);
    m.annotate (Nonempty.class);
    m.body ()._return (fScheme);

    // public String getID ()
    m = jEnum.method (JMod.PUBLIC, String.class, "getID");
    m.annotate (Nonnull.class);
    m.annotate (Nonempty.class);
    m.body ()._return (fID);

    // public Version getSince ()
    m = jEnum.method (JMod.PUBLIC, Version.class, "getSince");
    m.annotate (Nonnull.class);
    m.body ()._return (fSince);

    // public boolean isDeprecated ()
    m = jEnum.method (JMod.PUBLIC, boolean.class, "isDeprecated");
    m.body ()._return (fDeprecated);

    // public Version getDeprecatedSince ()
    m = jEnum.method (JMod.PUBLIC, Version.class, "getDeprecatedSince");
    m.annotate (Nullable.class);
    m.body ()._return (fDeprecatedSince);

    // @Nullable public static EPredefinedProcessIdentifier
    // getFromProcessIdentifierOrNull(@Nullable final String sID)
    m = jEnum.method (JMod.PUBLIC | JMod.STATIC, jEnum, "getFromProcessIdentifierOrNull");
    {
      m.annotate (Nullable.class);
      final JVar jArgID = m.param (JMod.FINAL, String.class, "sID");
      jArgID.annotate (Nullable.class);
      final JBlock jIf = m.body ()
                          ._if (s_aCodeModel.ref (StringHelper.class).staticInvoke ("hasText").arg (jArgID))
                          ._then ();
      final JForEach jForEach = jIf.forEach (jEnum, "e", jEnum.staticInvoke ("values"));
      jForEach.body ()
              ._if (jForEach.var ().invoke ("getID").invoke ("equals").arg (jArgID))
              ._then ()
              ._return (jForEach.var ());
      m.body ()._return (JExpr._null ());
    }

    // @Nullable public static EPredefinedProcessIdentifier
    // getFromProcessIdentifierOrNull(@Nullable final String sScheme, @Nullable
    // final String sID)
    m = jEnum.method (JMod.PUBLIC | JMod.STATIC, jEnum, "getFromProcessIdentifierOrNull");
    {
      m.annotate (Nullable.class);
      final JVar jArgScheme = m.param (JMod.FINAL, String.class, "sScheme");
      jArgScheme.annotate (Nullable.class);
      final JVar jArgID = m.param (JMod.FINAL, String.class, "sID");
      jArgID.annotate (Nullable.class);
      final JBlock jIf = m.body ()
                          ._if (s_aCodeModel.ref (StringHelper.class)
                                            .staticInvoke ("hasText")
                                            .arg (jArgScheme)
                                            .cand (s_aCodeModel.ref (StringHelper.class)
                                                               .staticInvoke ("hasText")
                                                               .arg (jArgID)))
                          ._then ();
      final JForEach jForEach = jIf.forEach (jEnum, "e", jEnum.staticInvoke ("values"));
      jForEach.body ()
              ._if (jForEach.var ()
                            .invoke ("getScheme")
                            .invoke ("equals")
                            .arg (jArgScheme)
                            .cand (jForEach.var ().invoke ("getID").invoke ("equals").arg (jArgID)))
              ._then ()
              ._return (jForEach.var ());
      m.body ()._return (JExpr._null ());
    }
  }

  private static void _emitTransportProfiles (final IMicroElement aRootElement) throws JClassAlreadyExistsException
  {
    final JDefinedClass jEnum = s_aCodeModel._package (RESULT_JAVA_PACKAGE)
                                            ._enum ("EPredefinedTransportProfile")
                                            ._implements (s_aCodeModel.ref (RESULT_JAVA_PACKAGE + ".IPredefined"));
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

      final JEnumConstant jEnumConst = jEnum.enumConstant (_getIdentifier (sID));
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
      jEnumConst.arg (bDeprecated ? JExpr.TRUE : JExpr.FALSE);
      jEnumConst.arg (StringHelper.hasNoText (sDeprecatedSince) ? JExpr._null ()
                                                                : s_aCodeModel.ref (Version.class)
                                                                              .staticInvoke ("parse")
                                                                              .arg (sDeprecatedSince));
      jEnumConst.javadoc ().add (sName + " - <code>" + sID + "</code><br>");
      jEnumConst.javadoc ().addTag (JDocComment.TAG_SINCE).add ("code list v" + sSince);
    }

    // fields
    final JFieldVar fName = jEnum.field (JMod.PRIVATE | JMod.FINAL, String.class, "m_sName");
    final JFieldVar fID = jEnum.field (JMod.PRIVATE | JMod.FINAL, String.class, "m_sID");
    final JFieldVar fSince = jEnum.field (JMod.PRIVATE | JMod.FINAL, Version.class, "m_aSince");
    final JFieldVar fDeprecated = jEnum.field (JMod.PRIVATE | JMod.FINAL, boolean.class, "m_bDeprecated");
    final JFieldVar fDeprecatedSince = jEnum.field (JMod.PRIVATE | JMod.FINAL, Version.class, "m_aDeprecatedSince");

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
    final JVar jDeprecated = jCtor.param (JMod.FINAL, boolean.class, "bDeprecated");
    final JVar jDeprecatedSince = jCtor.param (JMod.FINAL, Version.class, "aDeprecatedSince");
    jDeprecatedSince.annotate (Nullable.class);
    jCtor.body ()
         .assign (fName, jName)
         .assign (fID, jID)
         .assign (fSince, jSince)
         .assign (fDeprecated, jDeprecated)
         .assign (fDeprecatedSince, jDeprecatedSince);

    JMethod m;

    // public String getName ()
    m = jEnum.method (JMod.PUBLIC, String.class, "getName");
    m.annotate (Nonnull.class);
    m.annotate (Nonempty.class);
    m.body ()._return (fName);

    // public String getID()
    m = jEnum.method (JMod.PUBLIC, String.class, "getID");
    m.annotate (Nonnull.class);
    m.annotate (Nonempty.class);
    m.body ()._return (fID);

    // public Version getSince ()
    m = jEnum.method (JMod.PUBLIC, Version.class, "getSince");
    m.annotate (Nonnull.class);
    m.body ()._return (fSince);

    // public boolean isDeprecated ()
    m = jEnum.method (JMod.PUBLIC, boolean.class, "isDeprecated");
    m.body ()._return (fDeprecated);

    // public Version getDeprecatedSince ()
    m = jEnum.method (JMod.PUBLIC, Version.class, "getDeprecatedSince");
    m.annotate (Nullable.class);
    m.body ()._return (fDeprecatedSince);

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
              ._if (jForEach.var ().invoke ("getID").invoke ("equals").arg (jValue))
              ._then ()
              ._return (jForEach.var ());
      m.body ()._return (JExpr._null ());
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
        throw new IllegalStateException ("The XML file '" + aCLF.m_aFile.getAbsolutePath () + "' could not be read!");

      aCLF.m_aHandler.accept (aDoc.getDocumentElement ());
    }

    // Write all Java source files
    new JCMWriter (s_aCodeModel).build (new File ("../../../toop-commons/toop-commons/src/main/java"));

    s_aLogger.info ("Done creating code");
    s_aLogger.info ("Don't forget to invoke mvn license:format");
  }
}
