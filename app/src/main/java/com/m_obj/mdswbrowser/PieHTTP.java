package com.m_obj.mdswbrowser;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.StatusLine;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;

/**
 * @author pie
 *
 */
public class PieHTTP {
	private static final int HTTP_STATUS_OK = 200;
	DefaultHttpClient client ;

	public synchronized String sendUrlContent( String url ) {
		return sendUrlContent( url, null, null, null, null );
	}

	public synchronized String sendUrlContent( String url, String ientity ) {
		return sendUrlContent( url, ientity, null, null, null );
	}

	public synchronized String sendUrlContent(String url, String ientity, String encoded, String user, String pass ) {
		// Create client and set our specific user-agent string
		HttpRequestBase request;
		client = new DefaultHttpClient();

        if( ientity != null && ientity.length() != 0 ){
			request = new HttpPost(url);
			try {
				StringEntity sentity = new StringEntity(ientity,"UTF-8");
				sentity.setContentEncoding("UTF-8");
				//TODO: Mime  <nym:head>
				sentity.setContentType("application/x-www-form-urlencoded; charset=UTF-8;");
				((HttpPost)request).setEntity(sentity);
				request.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8;");
				client.getParams().setParameter(CoreProtocolPNames.USE_EXPECT_CONTINUE, false);
			} catch (UnsupportedEncodingException e) {
				return "<error>UnsupportedEncodingException "+e.getMessage()+"</error>";
			}
        }else{
        	request = new HttpGet(url);
        }

        URI uri;
		try {
			uri = new URI(url);
		} catch (URISyntaxException e1) {
             return "<error>URI error</error>";
		}
		int port = uri.getPort();
		if( port < 0 ){
			port = 80;
		}
		client.getParams().setParameter(CoreProtocolPNames.USER_AGENT, "AIRi");

        if(user != null && !user.equals("")){
        	request.setHeader(BasicScheme.authenticate(new UsernamePasswordCredentials(user, pass), "UTF-8", false));
        }
        // Cookie

        try {
        	//******************************************************
            HttpResponse response = client.execute(request);

            // Check if server response is valid
            StatusLine status = response.getStatusLine();
            if( status.getStatusCode() < HTTP_STATUS_OK || status.getStatusCode() >= 300 ) {
            	//TODO:
            	String errmsg = status.getStatusCode()+" "+status.getReasonPhrase();
                return errmsg;
            }
            // Cookie

            //
            HttpEntity entity = response.getEntity();
            String enc = "UTF-8";
            if( encoded == null ){
            	encoded = "";
            }
            if( encoded.equals("")){
	            if( entity.getContentType() != null ){
	            	int p = entity.getContentType().getValue().indexOf("=");
	            	if( p != -1 ){
	            		enc = entity.getContentType().getValue().substring(p+1).trim();
	            	}
	            }
            }else{
            	enc = encoded;
            }
            
            InputStreamReader istreamReader;
            try {
            	istreamReader = new InputStreamReader( entity.getContent(), enc );
            }catch(UnsupportedEncodingException e){
            	istreamReader = new InputStreamReader( entity.getContent(), "UTF-8" );
            }
            // Pull content stream from response
            int readBytes = 0;
            char[] sBuffer = new char[20480];
            StringWriter sw = new StringWriter( 204800 );
            sw.write("<header>"); 
            sw.write(getAllHeaders(response));
            sw.write("</header>\n<entity><![CDATA[");
            while ((readBytes = istreamReader.read(sBuffer)) != -1) {
            	sw.write(sBuffer,0,readBytes);
            }
            sw.write("]]></entity>");
        	//TODO:
            return sw.toString();
        } catch (IOException e) {
            return "<error>IOException "+e.getMessage()+"/ "+
	            e.getLocalizedMessage()+"/ "+
	            e.getCause()+"/ "+
	            e.getStackTrace()+"</error>";
        }
    }

	
	
	private String getAllHeaders(HttpResponse response) {
		Header[] headers = response.getAllHeaders();
		StringBuffer allheaders = new StringBuffer("");
		try {
			allheaders.append("redirect:"+client.getRedirectHandler().getLocationURI(response, null)+";/ ");
		} catch (ProtocolException e) {
			allheaders.append(e.getMessage()+";/ ");
		}
		allheaders.append("status:"+response.getStatusLine().toString()+";/ ");
		for( int n=0; n<headers.length; ++n){
			allheaders.append(headers[n].getName()+":"+headers[n].getValue()+";/ ");
		}
		return allheaders.toString();
	}


}

