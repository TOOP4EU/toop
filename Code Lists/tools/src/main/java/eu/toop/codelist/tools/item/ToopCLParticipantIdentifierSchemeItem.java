package eu.toop.codelist.tools.item;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.regex.RegExHelper;
import com.helger.commons.string.StringHelper;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroElement;
import com.helger.xml.microdom.util.MicroHelper;

/**
 * Single item of a participant identifier scheme code list entry
 *
 * @author Philip Helger
 */
public class ToopCLParticipantIdentifierSchemeItem extends AbstractToopCLItem
{
  private final String m_sSchemeID;
  private final String m_sCountryCode;
  private final String m_sSchemeAgency;
  private final String m_sStructure;
  private final String m_sDisplay;
  private final String m_sUsage;

  public ToopCLParticipantIdentifierSchemeItem (@Nonnull @Nonempty final String sSchemeID,
                                                @Nonnull @Nonempty final String sISO6523,
                                                @Nonnull @Nonempty final String sCountryCode,
                                                @Nonnull @Nonempty final String sSchemeName,
                                                @Nullable final String sSchemeAgency,
                                                @Nonnull @Nonempty final String sSince,
                                                final boolean bDeprecated,
                                                @Nullable final String sDeprecatedSince,
                                                @Nullable final String sStructure,
                                                @Nullable final String sDisplay,
                                                @Nullable final String sUsage)
  {
    super (sSchemeName, sISO6523, sSince, bDeprecated, sDeprecatedSince);
    ValueEnforcer.notEmpty (sSchemeID, "SchemeID");
    ValueEnforcer.notEmpty (sCountryCode, "CountryCode");
    ValueEnforcer.isFalse (sSchemeID.indexOf (' ') >= 0, "Scheme IDs are not supposed to contain spaces!");
    ValueEnforcer.isTrue (RegExHelper.stringMatchesPattern ("[0-9]{4}", sISO6523),
                          () -> "The ISO 6523 code '" + sISO6523 + "' does not consist of 4 numbers");
    m_sSchemeID = sSchemeID;
    m_sCountryCode = sCountryCode;
    m_sSchemeAgency = sSchemeAgency;
    m_sStructure = sStructure;
    m_sDisplay = sDisplay;
    m_sUsage = sUsage;
  }

  @Nonnull
  @Nonempty
  public final String getSchemeID ()
  {
    return m_sSchemeID;
  }

  @Nonnull
  @Nonempty
  public final String getCountryCode ()
  {
    return m_sCountryCode;
  }

  @Nullable
  public final String getSchemeAgency ()
  {
    return m_sSchemeAgency;
  }

  @Nullable
  public final String getStructure ()
  {
    return m_sStructure;
  }

  @Nullable
  public final String getDisplay ()
  {
    return m_sDisplay;
  }

  @Nullable
  public final String getUsage ()
  {
    return m_sUsage;
  }

  @Nonnull
  public IMicroElement getAsMicroElement ()
  {
    final IMicroElement ret = new MicroElement ("participant-identifier-scheme");
    fillMicroElement (ret);
    ret.setAttribute ("schemeid", m_sSchemeID);
    ret.setAttribute ("country", m_sCountryCode);
    if (StringHelper.hasText (m_sSchemeAgency))
      ret.setAttribute ("schemeagency", m_sSchemeAgency);
    if (StringHelper.hasText (m_sStructure))
      ret.appendElement ("structure").appendText (m_sStructure);
    if (StringHelper.hasText (m_sDisplay))
      ret.appendElement ("display").appendText (m_sDisplay);
    if (StringHelper.hasText (m_sUsage))
      ret.appendElement ("usage").appendText (m_sUsage);
    return ret;
  }

  @Nonnull
  public static ToopCLParticipantIdentifierSchemeItem create (@Nonnull final IMicroElement aElement)
  {
    return new ToopCLParticipantIdentifierSchemeItem (aElement.getAttributeValue ("schemeid"),
                                                      aElement.getAttributeValue ("id"),
                                                      aElement.getAttributeValue ("country"),
                                                      aElement.getAttributeValue ("name"),
                                                      aElement.getAttributeValue ("schemeagency"),
                                                      aElement.getAttributeValue ("since"),
                                                      aElement.getAttributeValueAsBool ("deprecated",
                                                                                        DEFAULT_DEPRECATED),
                                                      aElement.getAttributeValue ("deprecated-since"),
                                                      MicroHelper.getChildTextContent (aElement, "structure"),
                                                      MicroHelper.getChildTextContent (aElement, "display"),
                                                      MicroHelper.getChildTextContent (aElement, "usage"));
  }
}
