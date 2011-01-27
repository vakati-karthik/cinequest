package edu.sjsu.cinequest.android;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import android.webkit.WebView;
import android.webkit.WebViewClient;

import edu.sjsu.cinequest.comm.Platform;
import edu.sjsu.cinequest.comm.WebConnection;

public class AndroidWebConnection extends WebConnection {
	private String url;
	private Hashtable postData;
    private HttpResponse response;
    private static HttpParams params;
    private static ClientConnectionManager conman;
    
    static {
    	params = new BasicHttpParams();
    	HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
    	HttpProtocolParams.setContentCharset(params, HTTP.DEFAULT_CONTENT_CHARSET);
    	HttpProtocolParams.setUseExpectContinue(params, true);
    	HttpConnectionParams.setConnectionTimeout(params, 1000);
    	HttpConnectionParams.setSoTimeout(params, 1000);
    	HttpProtocolParams.setUserAgent(params, "Mozilla/5.0 (Linux; U; Android 1.0; en-us; generic)");
    	ConnManagerParams.setTimeout(params, 1000);

    	SchemeRegistry registry = new SchemeRegistry();
    	registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
    	registry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));

    	conman = new ThreadSafeClientConnManager(params, registry);
    }
    
    public AndroidWebConnection(String url) throws IOException
    {
        Platform.getInstance().log("Opening connection to " + url);
        this.url = url;
    }
    
    
    public void setPostParameters(Hashtable postData) throws IOException {
    	this.postData = postData;
    }
    
    private void execute() throws IOException
    {
    	if (response != null) return;
    	HttpClient client = new DefaultHttpClient(conman, params);
    	Platform.getInstance().log("user-agent: " + client.getParams().getParameter("user-agent"));

    	// HttpClient client = new DefaultHttpClient();
    	if (postData == null) 
    	{
            HttpGet request = new HttpGet(url);
            response = client.execute(request);    		
    	}
    	else
    	{
    		HttpPost request = new HttpPost(url);
            Enumeration keys = postData.keys();
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            while (keys.hasMoreElements()) 
            {            	            	
               String key = keys.nextElement().toString();
               String value = postData.get(key).toString();
               nameValuePairs.add(new BasicNameValuePair(key, value));  
            }         
            request.setEntity(new UrlEncodedFormEntity(nameValuePairs));                 
            response = client.execute(request);    		            
    	}
    }
        
    protected InputStream getInputStream() throws IOException
    {
    	execute();
    	return new BufferedHttpEntity(response.getEntity()).getContent(); 
    }
    
    public byte[] getBytes() throws IOException 
    {
    	execute();
    	return EntityUtils.toByteArray(response.getEntity());
    }
    
    public String getHeaderField(String name) throws IOException
    {   
    	execute();
    	String value = response.getFirstHeader(name).getValue();
    	return value;
    }

    public void close() throws IOException
    {
    	if (response != null) response.getEntity().consumeContent();
        Platform.getInstance().log("Closing connection");
    }
}