package com.m_obj.mdswbrowser;

import java.io.*;
import java.lang.reflect.Array;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.StatusLine;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.cookie.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultRedirectHandler;
import org.apache.http.impl.cookie.RFC2965Spec;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

public class Nym {
	private static final int HTTP_STATUS_OK = 200;
	public static final String  NymNAMESPACE = "http://m-obj.com/nym";
	public static final String  NymPREFIX = "nym:";
	DefaultHttpClient client;
	Momo momo;
	Document progDom;
//	Document dataDom;
	HashMap<String,String> varTbl;
    DocumentBuilder docBuilder;
	StringBuffer outStr;
	List<Cookie> cookies;
	String resHeaders;
	String scanTarget;
	String paramStr;
	int	scanPos;

	@SuppressWarnings("deprecation")
	public Nym( String srcxml, Momo _momo, String param ) throws SAXException, IOException, ParserConfigurationException {
		momo = _momo;
		String src = includeProc(srcxml);
	//	momo.put("data", "srcxml", src);
		StringBufferInputStream srcstream = new StringBufferInputStream( 
				"<nym xmlns:nym='"+ NymNAMESPACE +"' >"+ src +"</nym>");
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		docBuilder = factory.newDocumentBuilder();
		progDom = docBuilder.parse(srcstream);
		//dataDom = docBuilder.newDocument();	//~docBuilder.getDOMImplementation().createDocument("", "", null);
		cookies = new ArrayList<Cookie>();
		outStr = new StringBuffer();
		varTbl = new HashMap<String, String>();
		paramStr = param;
		if( paramStr == null )
			paramStr = "";
		String vars[] = paramStr.split("&");
		for( int n=0; n<vars.length; ++n ){
			String vp[] = vars[n].split("=");
			if( vp.length == 2){
				varTbl.put(vp[0], vp[1]);
			}
		}
	    client = new DefaultHttpClient();
	}

	private String includeProc( String src ){
		Pattern IncludePtn = Pattern.compile("(.*)<nym:include momo=\"([\\./A-Za-z0-9]*)\"/>(.*)", Pattern.MULTILINE|Pattern.DOTALL);
		for(;;) {
			Matcher m = IncludePtn.matcher(src);
			if( m.matches() ){
				String incstr = momo.getItem( m.group(2) );
			//	momo.put("data", "incstr", incstr);
				src = m.group(1)+incstr+m.group(3);
			}else{
				return src;
			}
		}
	}
	
	public String Run(){
		outStr.append("<momo>");
		doScript( (Node)progDom.getDocumentElement() );
		outStr.append("</momo>");
		return outStr.toString();
	}

	private void doScript( Node elm ) {
		NodeList nlist = elm.getChildNodes();
		for( int n = 0; n < nlist.getLength(); ++n ){
			Node e = nlist.item(n);
		//	String ens = e.getNamespaceURI();  // ens=null
			if( e.getNodeName().equals("nym:get") ){
		        doGet( nlist.item(n) );
			}else
			if( e.getNodeName().equals("nym:scan") ){
				doScan( nlist.item(n) );
			}else
			if( e.getNodeName().equals("nym:put") ){
				doPut( nlist.item(n) );
			}else
			if( e.getNodeName().equals("nym:if") ){
		        doIf( nlist.item(n) );
			}else
			if( e.getNodeName().equals("nym:for-each") ){
				doForEach( nlist.item(n) );
			}
		}
	}
	//
	private void doGet( Node elm ){
	    String from = getElemParam( elm, "from" );

//	    if( from.equals("sql") ) {
//	    	makeXmlBySQL( elm );
//	    }else
	    if( from.equals("http") ) {
	    	makeXmlByHTTP( elm );
/***
	    }else
	    if( from.equals("imm") ) {
	    //	contentBack( dataDom.getDocumentElement(), tagstr(getElemParam( elm, "outtag", seldn ),getElemText( elm, seldn )));
			String outtag = getElemParam( elm, "outtag" );
			if(outtag.length()==0)
				outtag = "nym";
			outStr.append("<"+outtag+">")
					.append(tagstr(getElemParam( elm, "outtag" ),getElemText( elm )))
				.append("</"+outtag+">");
***/
	    }
	}
	private void doPut( Node elm ){
		//TODO:
	}
	private void doIf( Node elm ){
		if( evaltest( elm ) ){
			doScript( elm );
		}else{
			NodeList elsElm = ((Element)elm).getElementsByTagName("nym:else");
			if( elsElm.getLength() != 0 ){
				doScript( elsElm.item(0) );
			}
		}
	}
	private void doForEach( Node elm ){
		//TODO: doForEach
	}
	private void doOutput( Node elm ){
		//TODO:
	}
	//-------------------------
	// <get from="sql" query="dtclass='[nym]'" outtag="tw"/>
/***
	private void makeXmlBySQL( Node elm ){
		String outtag = getElemParam( elm, "outtag" );
		String query = getElemParam( elm, "query" );
		String order = getElemParam( elm, "order" );
	//	contentBack( dataDom.getDocumentElement(), tagstr(outtag, momo.query2xml(query, order)) );
//		outStr.append("<"+outtag+">")
//				.append(momo.query2xml(query, order))
//			.append("</"+outtag+">");
	}
***/
	//-------------------------
	// <get from="http" url="http://m-obj.com/" outtag="tw" method="get" mkxml="scan|load"/>
	private void makeXmlByHTTP( Node elm ){
		String outtagO = "";
		String outtagC = "";
		String outtag = getElemParam( elm, "outtag" );
		if( !outtag.equals("")){
			outtagO = "<"+outtag+">";
			outtagC = "</"+outtag+">";
		}
		String url = getElemParam( elm, "url" );
		String user = getElemParam( elm, "user" );
		String pass = getElemParam( elm, "pass" );
		String entity = getElemParam( elm, "entity" ).trim();
		String encoded = getElemParam( elm, "encoded" );
		NodeList scelms = ((Element)elm).getElementsByTagName("nym:scan");
		if( scelms.getLength() > 0 ){
			initScan(sendUrlContent(url, user, pass, entity, encoded));
			doScan(scelms.item(0));
		}else{
			outStr.append(outtagO)
					.append(sendUrlContent(url, user, pass, entity, encoded))
				.append(outtagC);
		}
	}
	private void initScan( String st ){
		scanTarget = st;
		scanPos = 0;
	}
	private boolean doScan( Node elm ){
		NodeList nlist = elm.getChildNodes();
		boolean scango = true;
		for( int n = 0; n < nlist.getLength(); ++n ){
			Node e = nlist.item(n);
			if( e.getNodeName().equals("nym:pickup") ){
				String from = getElemParam(e, "from");
				String to = getElemParam(e, "to");
				String name = getElemParam(e, "name");
				String cdata = getElemParam(e, "cdata");
				if( name.length()==0 || to.length()==0 ){
					outStr.append(tagstr("error", "name["+name+"], to["+to+"]"));
					return false;
				}
				int frompos = scanPos;
				if( from.length()!=0 ){
					frompos = scanTarget.indexOf(from, scanPos);
					if( frompos==-1 )
						return false;
					frompos = frompos + from.length();
				}
				scanPos = frompos;
				int topos = scanTarget.indexOf(to, scanPos);
				if( topos==-1 )
					return false;
				String resultstr=scanTarget.substring(frompos, topos);
				if(cdata.equals("no")){
					resultstr = resultstr.replaceAll("<[^>]*>", "");
				}
				resultstr = subconvert(e,resultstr);
				scanPos = topos + to.length();

				if( cdata.equals("no"))
					outStr.append(tagstr(name, resultstr));
				else
					outStr.append(tagstr(name, "<![CDATA["+resultstr+"]]>"));
				varTbl.put(name, resultstr);
			}else
			if( e.getNodeName().equals("nym:loop") ){
				String limitStr = getElemParam( e, "limit" );
				int limit = 10000;
				if( limitStr.length()!=0 )
					limit = Integer.parseInt( limitStr );
				if( limit <= 0 )
					limit = 10000;
				for( int i=0; i < limit; ++i ) {
					if( !doScan(e) )
						break;
				}
			}else
			if( e.getNodeName().equals("nym:if") ){
				if( evaltest( e ) ){
					scango = doScan( e );
				}else{
					NodeList elsElm = ((Element)e).getElementsByTagName("nym:else");
					if( elsElm.getLength() != 0 ){
						scango = doScan( elsElm.item(0) );
					}
				}
			}else
			if( e.getNodeName().equals("nym:tag") ){
				String tagname = getElemParam( e, "name" );
				String value = getElemParam( e, "value" );
				if( value.length()==0 ){
					outStr.append("<"+tagname+">");
					scango = doScan( e );
					outStr.append("</"+tagname+">");
				}else{
					String cdata = getElemParam(e, "cdata");
					if( !cdata.equals("no")){
						value = "<![CDATA["+value+"]]>";
					}
					outStr.append("<"+tagname+">");
					outStr.append(value);
					outStr.append("</"+tagname+">");
				}
			}else
			if( e.getNodeName().equals("nym:skip") ){
				String brptn = getElemParam( e, "brptn" );
				String to = getElemParam( e, "to" );
				String name = getElemParam(e, "name");
				int topos = scanTarget.indexOf(to, scanPos);
				if( brptn.length()!= 0 ){
					int brpos = scanTarget.indexOf(brptn, scanPos);
					if( brpos!=-1 && brpos < topos ){
						scanPos = brpos + brptn.length();
						if(name.equals(""))
							return false;
						varTbl.put(name, "brptn");
					}else{
						if( topos == -1 )
							return false;
						scanPos = topos + to.length();
						varTbl.put(name, "to");
					}
				}else{
					if( topos == -1 )
						return false;
					scanPos = topos + to.length();
					varTbl.put(name, "to");
				}
			}else
			if( e.getNodeName().equals("nym:check") ){
				String name = getElemParam(e, "name");
				if(name.equals(""))
					return false;
				int nearestPos = scanTarget.length();
				varTbl.put(name, "");
				for( int n1=0; n1<10;++n1 ){
					String brname = "br"+String.valueOf(n1);
					String brptn = getElemParam(e, brname);
					if(!brptn.equals("")){
						int pos =scanTarget.indexOf(brptn, scanPos);
						if( pos!=-1 && pos < nearestPos ){
							nearestPos = pos;
							varTbl.put(name, brname);
						}
					}
				}
				scanPos = nearestPos;
			}else
			if( e.getNodeName().equals("nym:break") ){
				return false;
			}
		}
		return scango;
	}
	
	//-------------------------

	private String getElemText( Node elm ){
		StringBuffer etext = new StringBuffer();
		NodeList elmlist = elm.getChildNodes();
		for( int n = 0; n < elmlist.getLength(); ++n ) {
			Node e = elmlist.item(n);
	    	if( e.getNodeType()!=Node.ELEMENT_NODE){
		        etext.append( e.getNodeValue() );
		    }else
	        if( e.getNodeName().equals("nym:value-of") ) {
	        	String resultstr = "";
	        	String varname = getAttributeValue( e, "var");
	        	String dbname = getAttributeValue( e, "momo");
	        	if( !dbname.equals("") ){
	        		resultstr = momo.getItem(dbname);
	        	}else
	        	if( !varname.equals("") ){
		        	resultstr = varTbl.get(varname);
		        }
		        resultstr = subconvert( e, resultstr );
		        etext.append(resultstr);
	        }else
		    if( e.getNodeName().equals("nym:param")){
        		etext.append(paramStr);
		    }else
			if( e.getNodeName().equals("nym:cgiparam")){
				NodeList cnlist = e.getChildNodes();
				StringBuffer params = new StringBuffer();
				for( int cn=0; cn <cnlist.getLength();++cn ){
					Node ce = cnlist.item(cn);
					String varname = getAttributeValue( ce, "name");
					if(!varname.equals("")){
						String val=varTbl.get(varname);
						if(val!=null){
							if(!params.equals(""))
								params.append("&");
							params.append(varname+"="+val);
						}
					}
				}
				if( !params.equals("")){
					etext.append("?"+params.toString());
				}
			}else
			if( e.getNodeName().equals("nym:else")){
			}else
		    if( e.getNodeName().equals("nym:if")){
		        if( evaltest( e ) ){
		            etext.append( getElemText( e ) );
		        }else{
		            NodeList nlist = ((Element)e).getElementsByTagName("nym:else");
		            if( nlist.getLength() != 0 ){
		              etext.append( getElemText( nlist.item(0) ));
		            }
		        }
		    }else{
		        etext.append( tagstr( e.getNodeName(), getElemText( e ) ));
		    }
		}
	    return etext.toString();
	}
	//-----------------------------------------------------------
	private String getAttributeValue( Node elm, String attr ){
		NamedNodeMap nmap = elm.getAttributes();
		for( int n = 0; n < nmap.getLength(); ++n ){
			Node attrnode = nmap.item(n);
			String attrname = attrnode.getNodeName();
			if( attrname.equals(attr) ) {
				return attrnode.getNodeValue();
			}
		}
		return "";
	}
	private String getSelect( String select){
		String rtv = varTbl.get(select);
		if( rtv == null)
			rtv = "";
        return rtv;
	}
	private boolean evaltest( Node e ) {
	    boolean trzt = false;
	    String test = getElemParam( e, "test" );
	    if( test.indexOf("!=")!=-1 ){
	        String vars[] = test.split("!=");
	        String lv = getTestValue( vars[0] );
	        String rv = getTestValue( vars[1] );
	        trzt = !lv.equals(rv);
	    }else
	    if( test.indexOf('=')!=-1 ){
		    String vars[] = test.split("=");
		    String lv = getTestValue( vars[0] );
		    String rv = getTestValue( vars[1] );
		    trzt = lv.equals(rv);
	    }else{
	    	String lv = getTestValue( test );
	    	trzt = ! lv.equals("");
	    }
	    return trzt;
	}
	private String getTestValue( String str ){
		str = str.trim();
	    if( str.charAt(0)=='"' || str.charAt(0)=='\''){
	      if( str.length() > 2 ) {
	        return str.substring(1, str.length()-1);
	      }
          return "";
	    }
        return getSelect( str );
	}
	private String subconvert( Node e, String resultstr ){
		NodeList elmlist = e.getChildNodes();
		for( int n = 0; n < elmlist.getLength(); ++n ) {
			Node elm = elmlist.item(n);
			if( elm.getNodeName().equals("nym:convert-date") ){
				resultstr = NymFormatter.DateFormat(resultstr, 9);
			}else
			if( elm.getNodeName().equals("nym:replace") ){
				String ptnstr =  getElemParam(elm, "ptn");
				String repstr =  getElemParam(elm, "repstr");
				if( ptnstr.length()!=0 && repstr.length()!=0){
					Pattern ptn = Pattern.compile(ptnstr, Pattern.MULTILINE|Pattern.DOTALL);
					String targetstr = resultstr;
					resultstr = "";
					while(true){
						Matcher m = ptn.matcher(targetstr);
						if(m.find()){
							String beforeStr = targetstr.substring(0, m.start());
							String afterStr = targetstr.substring(m.end());
							String currepstr = repstr;
							for(int i=1;i<=m.groupCount();++i){
								String pstr = "\\$"+Integer.toString(i);
								String mstr = m.group(i);
								currepstr = currepstr.replaceAll(pstr, mstr);
							}
							targetstr = afterStr;
							resultstr = resultstr + beforeStr+currepstr;
						}else{
							resultstr = resultstr + targetstr;
							break;
						}
					}
				}
			}
		}
		return resultstr;	
	}
	private String getElemParam( Node elm, String name ){
	    String attr = getAttributeValue( elm, name );
	    if( ! attr.equals("")){
	    	return attr;
	    }
	    NodeList nlist = ((Element) elm).getChildNodes();
	    for( int n=0; n < nlist.getLength(); ++n ){
	    	if( nlist.item(n).getNodeName().equals("nym:"+name)  ){
	    		return getElemText( nlist.item(n) );
	    	}
	    }
        return "";
	}
	private String getAttributes( Node node ){
		String rtnstr = "";
		NamedNodeMap attrs = node.getAttributes();
		for( int n = 0; n < attrs.getLength(); ++n ){
			rtnstr= rtnstr+" "+attrs.item(n).getNodeName()+"='"+attrs.item(n).getNodeValue();
		}
		return rtnstr;
	}
	private String tagstr( String tag, String cont ){
		return "<"+tag+">"+cont+"</"+tag+">";
	}
	private String tagstr( String tag, String cont, String attrs ){
		return "<"+tag+" "+attrs+">"+cont+"</"+tag+">";
	}
	private void dumpDom( Node dom ){
		NodeList domlist = dom.getChildNodes();
		for( int n = 0; n < domlist.getLength(); ++n ){
			Node node = domlist.item(n);
			if( node.getNodeType()==Node.ELEMENT_NODE ){
				outStr.append("<"+node.getNodeName()+getAttributes(node)+">");
				dumpDom(node);
				outStr.append("</"+node.getNodeName()+">");
			}else{
				outStr.append(node.getNodeValue());
			}
		}
	}
	//-------------------------
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

	public synchronized String sendUrlContent(String url, String user, String pass, String ientity, String encoded ) {
		// Create client and set our specific user-agent string
		HttpRequestBase request;
		momo.put("data", "#url", url);
		momo.put("data", "#ientity", ientity);
		if( ientity != null && ientity.length() != 0 ){
			request = new HttpPost(url);
			try {
				StringEntity sentity = new StringEntity(ientity,"UTF-8");
				sentity.setContentEncoding("UTF-8");
				//TODO: Mime �Ƃ��̃T�|�[�g <nym:head>
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
        // Cookie �̐ݒ�

        try {
        	//********** �T�[�o�[�փA�N�Z�X *********************************************
            HttpResponse response = client.execute(request);

            // Check if server response is valid
            StatusLine status = response.getStatusLine();
            if( status.getStatusCode() < HTTP_STATUS_OK || status.getStatusCode() >= 300 ) {
            	//TODO: �G���[���̓X�e�[�^�X�ƃT�[�o�[���̉����𗼕�Ԃ��悤�ɂ���
                return "<error>"+status.getStatusCode()+" "+status.getReasonPhrase()+"</error>";
            }
            // Cookie �����������i�[����

            // �T�[�o�[�����̃R�[�h�n���擾
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
            sw.write("</header><entity>");
            while ((readBytes = istreamReader.read(sBuffer)) != -1) {
            	sw.write(sBuffer,0,readBytes);
            }
            sw.write("</entity>");
        	//TODO: �G���[���̓X�e�[�^�X�ƃT�[�o�[���̉����𗼕�Ԃ��悤�ɂ���
momo.put("data", "#result", sw.toString());
            return sw.toString();
        } catch (IOException e) {
//momo.put("data", "#result", e.getMessage());
            return "<error>IOException "+e.getMessage()+"/ "+
	            e.getLocalizedMessage()+"/ "+
	            e.getCause()+"/ "+
	            e.getStackTrace()+"</error>";
        }
    }
}
class NymFormatter {
	//DatePtn01 : Tue, 01 Feb 2010 23:30:55 +0000
	static Pattern DatePtn01 = Pattern.compile("[,a-zA-Z]+\\s*(\\d+)\\s+(\\w+)\\s+(\\d+)\\s+(\\d+):(\\d+):(\\d+)\\s+(.\\d+)",Pattern.CASE_INSENSITIVE);
	//DatePtn00 : Tue Nov 17 23:30:55 +0000 2009
	static Pattern DatePtn00 = Pattern.compile("\\w*\\s*(\\w+)\\s+(\\d+)\\s+(\\d+):(\\d+):(\\d+)\\s+(.\\d+)\\s+(\\d+)",Pattern.CASE_INSENSITIVE);

	static String DateFormat( String datestr, int utc ){
		GregorianCalendar gcal;
		int year,mon,day,hour,min,sec;
		try { //Tue Nov 17 23:30:55 +0000 2009
			Matcher m = DatePtn00.matcher(datestr);
			if( m.matches() ){
				year = Integer.parseInt(m.group(7));
				mon = ("JAN FEB MAR APR MAY JUN JUL AUG SEP OCT NOV DEC".indexOf(m.group(1).toUpperCase())/4 );
				day = Integer.parseInt(m.group(2));
				hour = Integer.parseInt(m.group(3));
				min = Integer.parseInt(m.group(4));
				sec = Integer.parseInt(m.group(5));
			}else{
				Matcher m1 = DatePtn01.matcher(datestr);
				if( m1.matches()){
					year = Integer.parseInt(m1.group(3));
					mon = ("JAN FEB MAR APR MAY JUN JUL AUG SEP OCT NOV DEC".indexOf(m1.group(2).toUpperCase())/4 );
					day = Integer.parseInt(m1.group(1));
					hour = Integer.parseInt(m1.group(4));
					min = Integer.parseInt(m1.group(5));
					sec = Integer.parseInt(m1.group(6));
				}else{
					Date d = SimpleDateFormat.getDateInstance().parse(datestr);
					year = d.getYear();
					mon = d.getMonth();
					day = d.getDate();
					hour = d.getHours();
					min = d.getMinutes();
					sec = d.getSeconds();
				}
			}
			gcal = new GregorianCalendar(year,mon,day,hour,min,sec);
		} catch (ParseException e1) {
			return e1.getMessage();
		}
		gcal.add(GregorianCalendar.HOUR_OF_DAY, utc);
			//	SimpleDateFormat formatter = new SimpleDateFormat(formatstr);
			//	String ret =formatter.format(gcal);
			//	return ret;  �g���Ȃ�����
		return
			DecimalFormat(gcal.get(GregorianCalendar.YEAR),"0000")+"-"+
			DecimalFormat(gcal.get(GregorianCalendar.MONTH)+1,"00")+"-"+
			DecimalFormat(gcal.get(GregorianCalendar.DAY_OF_MONTH),"00")+" "+
			DecimalFormat(gcal.get(GregorianCalendar.HOUR_OF_DAY),"00")+":"+
			DecimalFormat(gcal.get(GregorianCalendar.MINUTE),"00")+":"+
			DecimalFormat(gcal.get(GregorianCalendar.SECOND),"00");
	}
	// �������������낦��
	// val �ɏo�͂��������l formatstr �Ɍ������� "0" ����ׂ������� " " ����ׂ�΃[���T�v���X
	static String DecimalFormat( int val, String formatstr ){
		String valstr = Integer.toString(val);
		return (formatstr+valstr).substring(valstr.length());
	}
}

