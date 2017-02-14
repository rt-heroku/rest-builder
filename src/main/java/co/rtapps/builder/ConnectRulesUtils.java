package co.rtapps.builder;

import java.util.StringTokenizer;

import org.telosys.tools.repository.rules.RulesUtils;

public class ConnectRulesUtils extends RulesUtils{
	
    //------------------------------------------------------------------------------
    /**
     * Transform the given string to "CamelCase", using the given word separator  
     * ie : "ORDER_ITEM" --> "OrderItem"
     * @param inputString eg "ORDER_ITEM"
     * @param separator   eg "_"
     * @return
     */
    public String camelCaseCustomObject(String inputString, String separator) {
        if (inputString != null)
        {
            StringBuffer sb = new StringBuffer( inputString.length() );
            String sToken = null;
            String s = inputString.trim(); // to be secure
            StringTokenizer st = new StringTokenizer(s, separator);
            while (st.hasMoreTokens())
            {
                sToken = st.nextToken();
                sb.append( capitalizeFully( sToken ) );
            }
            return sb.toString();
        }
        else
        {
            return "";
        }
    }
}
