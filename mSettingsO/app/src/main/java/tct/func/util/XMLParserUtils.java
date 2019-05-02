package tct.func.util;

import android.content.res.XmlResourceParser;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * Created by zengjie on 18-07-19.
 */

public class XMLParserUtils {
    private XMLParserUtils() {
    }

    public static final void beginDocument(XmlPullParser parser, String firstElementName) throws XmlPullParserException, IOException
    {
        int type;
        while ((type=parser.next()) != parser.START_TAG
                && type != parser.END_DOCUMENT) {

        }

        if (type != parser.START_TAG) {
            throw new XmlPullParserException("No start tag found");
        }

        if (!parser.getName().equals(firstElementName)) {
            throw new XmlPullParserException("Unexpected start tag: found " + parser.getName() +
                    ", expected " + firstElementName);
        }
    }

    public static String getAttributeValue(XmlResourceParser parser,String nameSpace, String attribute) {
        String value = parser.getAttributeValue(nameSpace, attribute);
        if (value == null) {
            value = parser.getAttributeValue(null, attribute);
        }
        return value;
    }
    public static int getAttributeIntValue(XmlResourceParser parser,String nameSpace, String attribute) {
        int value = parser.getAttributeIntValue(nameSpace,attribute,-1);

        return value;
    }
    public static boolean getAttributeBoolean(XmlResourceParser parser,String nameSpace, String attribute) {
        boolean value = parser.getAttributeBooleanValue(nameSpace, attribute,false);
        return value;
    }
}