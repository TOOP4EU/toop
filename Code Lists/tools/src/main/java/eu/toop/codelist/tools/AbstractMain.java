package eu.toop.codelist.tools;

import javax.annotation.Nonnull;

import com.helger.commons.version.Version;

/**
 * Abstract base class with shared ideas
 *
 * @author Philip Helger
 */
abstract class AbstractMain
{
  public static final Version CODELIST_VERSION = new Version (1, 0, 0);
  protected static final String CL_XML_DIRECTORY = "../";
  public static final String DO_NOT_EDIT = "This file was automatically generated.\nDo NOT edit!";

  protected AbstractMain ()
  {}

  @Nonnull
  protected static final String getDocTypFilePrefix ()
  {
    return CL_XML_DIRECTORY + "ToopDocumentTypeIdentifiers-v" + CODELIST_VERSION.getAsString (false);
  }

  @Nonnull
  protected static final String getParticipantIdentifierSchemesFilePrefix ()
  {
    return CL_XML_DIRECTORY + "ToopParticipantIdentifierSchemes-v" + CODELIST_VERSION.getAsString (false);
  }

  @Nonnull
  protected static final String getProcessFilePrefix ()
  {
    return CL_XML_DIRECTORY + "ToopProcessIdentifiers-v" + CODELIST_VERSION.getAsString (false);
  }

  @Nonnull
  protected static final String getTransportProfilesPrefix ()
  {
    return CL_XML_DIRECTORY + "ToopTransportProfiles-v" + CODELIST_VERSION.getAsString (false);
  }
}
