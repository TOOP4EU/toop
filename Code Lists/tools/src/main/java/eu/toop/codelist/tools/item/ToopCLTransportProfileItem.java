package eu.toop.codelist.tools.item;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroElement;

/**
 * Single item of a transport profile code list entry
 *
 * @author Philip Helger
 */
public class ToopCLTransportProfileItem extends AbstractToopCLItem
{
  private final String m_sVersion;

  public ToopCLTransportProfileItem (@Nonnull @Nonempty final String sName,
                                     @Nonnull @Nonempty final String sVersion,
                                     @Nonnull @Nonempty final String sID,
                                     @Nonnull @Nonempty final String sSince,
                                     final boolean bDeprecated,
                                     @Nullable final String sDeprecatedSince)
  {
    super (sName, sID, sSince, bDeprecated, sDeprecatedSince);
    ValueEnforcer.notEmpty (sVersion, "Version");
    m_sVersion = sVersion;
  }

  @Nonnull
  @Nonempty
  public final String getVersion ()
  {
    return m_sVersion;
  }

  @Nonnull
  public IMicroElement getAsMicroElement ()
  {
    final IMicroElement ret = new MicroElement ("transport-profile");
    fillMicroElement (ret);
    ret.setAttribute ("version", m_sVersion);
    return ret;
  }

  @Nonnull
  public static ToopCLTransportProfileItem create (@Nonnull final IMicroElement aElement)
  {
    return new ToopCLTransportProfileItem (aElement.getAttributeValue ("name"),
                                           aElement.getAttributeValue ("version"),
                                           aElement.getAttributeValue ("id"),
                                           aElement.getAttributeValue ("since"),
                                           aElement.getAttributeValueAsBool ("deprecated", DEFAULT_DEPRECATED),
                                           aElement.getAttributeValue ("deprecated-since"));
  }
}
