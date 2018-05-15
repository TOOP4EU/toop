package eu.toop.codelist.tools.item;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroElement;

/**
 * Single item of a process ID code list entry
 *
 * @author Philip Helger
 */
public class ToopCLProcessItem extends AbstractToopCLItem
{
  public ToopCLProcessItem (@Nonnull @Nonempty final String sName,
                            @Nonnull @Nonempty final String sID,
                            @Nonnull @Nonempty final String sSince,
                            final boolean bDeprecated,
                            @Nullable final String sDeprecatedSince)
  {
    super (sName, sID, sSince, bDeprecated, sDeprecatedSince);
  }

  @Nonnull
  public IMicroElement getAsMicroElement ()
  {
    final IMicroElement ret = new MicroElement ("process");
    fillMicroElement (ret);
    return ret;
  }

  @Nonnull
  public static ToopCLProcessItem create (@Nonnull final IMicroElement aElement)
  {
    return new ToopCLProcessItem (aElement.getAttributeValue ("name"),
                                  aElement.getAttributeValue ("id"),
                                  aElement.getAttributeValue ("since"),
                                  aElement.getAttributeValueAsBool ("deprecated", DEFAULT_DEPRECATED),
                                  aElement.getAttributeValue ("deprecated-since"));
  }
}
