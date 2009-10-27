// $Id$
// $Log: not supported by cvs2svn $

/*
COPYRIGHT STATUS:
  Dec 1st 2001, Fermi National Accelerator Laboratory (FNAL) documents and
  software are sponsored by the U.S. Department of Energy under Contract No.
  DE-AC02-76CH03000. Therefore, the U.S. Government retains a  world-wide
  non-exclusive, royalty-free license to publish or reproduce these documents
  and software for U.S. Government purposes.  All documents and software
  available from this server are protected under the U.S. and Foreign
  Copyright Laws, and FNAL reserves all rights.
 
 
 Distribution of the software available from this server is free of
 charge subject to the user following the terms of the Fermitools
 Software Legal Information.
 
 Redistribution and/or modification of the software shall be accompanied
 by the Fermitools Software Legal Information  (including the copyright
 notice).
 
 The user is asked to feed back problems, benefits, and/or suggestions
 about the software to the Fermilab Software Providers.
 
 
 Neither the name of Fermilab, the  URA, nor the names of the contributors
 may be used to endorse or promote products derived from this software
 without specific prior written permission.
 
 
 
  DISCLAIMER OF LIABILITY (BSD):
 
  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
  "AS IS" AND ANY EXPRESS OR IMPLIED  WARRANTIES, INCLUDING, BUT NOT
  LIMITED TO, THE IMPLIED  WARRANTIES OF MERCHANTABILITY AND FITNESS
  FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL FERMILAB,
  OR THE URA, OR THE U.S. DEPARTMENT of ENERGY, OR CONTRIBUTORS BE LIABLE
  FOR  ANY  DIRECT, INDIRECT,  INCIDENTAL, SPECIAL, EXEMPLARY, OR
  CONSEQUENTIAL DAMAGES  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
  OF SUBSTITUTE  GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
  BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY  OF
  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT  OF THE USE OF THIS
  SOFTWARE, EVEN IF ADVISED OF THE  POSSIBILITY OF SUCH DAMAGE.
 
 
  Liabilities of the Government:
 
  This software is provided by URA, independent from its Prime Contract
  with the U.S. Department of Energy. URA is acting independently from
  the Government and in its own private capacity and is not acting on
  behalf of the U.S. Government, nor as its contractor nor its agent.
  Correspondingly, it is understood and agreed that the U.S. Government
  has no connection to this software and in no manner whatsoever shall
  be liable for nor assume any responsibility or obligation for any claim,
  cost, or damages arising out of or resulting from the use of the software
  available from this server.
 
 
  Export Control:
 
  All documents and software available from this server are subject to U.S.
  export control laws.  Anyone downloading information from this server is
  obligated to secure any necessary Government licenses before exporting
  documents or software obtained from this server.
 */


// generated by GLUE/wsdl2java on Mon Jun 17 15:27:13 CDT 2002
package org.dcache.srm.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import org.globus.util.GlobusURL;
import org.ietf.jgss.GSSCredential;
import org.dcache.srm.Logger;
public class SRMClientV1 implements diskCacheV111.srm.ISRM {
    private final static String SFN_STRING="?SFN=";
    private int retries;
    private long retrytimeout;
    
    private final org.dcache.srm.client.axis.ISRM_PortType axis_isrm;
    private GSSCredential user_cred;
    private String service_url;
    private Logger logger;
    private String host;
    private void say(String s) {
        if(logger != null) {
            logger.log("SRMClientV1 : "+s);
        }
    }
    
    private void esay(String s) {
        if(logger != null) {
            logger.elog("SRMClientV1 : "+s);
        }
    }
    
    private void esay(Throwable t) {
        if(logger != null) {
            logger.elog(t);
        }
    }
    public static String unwrapHttpRedirection(String http_url) {
        if(http_url == null || !http_url.startsWith("http://")) {
            return http_url;
        }
        HttpURLConnection http_connection  = null;
        InputStream in = null;
        try {
            URL http_URL = new URL(http_url);
            URLConnection connection = http_URL.openConnection();
            connection.setDoInput(true);
            connection.setDoOutput(false);
            if(connection instanceof  HttpURLConnection) {
                http_connection = (HttpURLConnection) connection;
                http_connection.setInstanceFollowRedirects(true);
                in = http_connection.getInputStream(); //this force the reading of the header
                URL new_url = http_connection.getURL();
                return new_url.toString();
                
            }
            
        }
        catch(IOException ioe) {
            
        }
        finally {
            try {
                if(in != null) {
                    in.close();
                }
                if(http_connection != null) {
                    http_connection.disconnect();
                }
            }
            catch(IOException ioe) {
            }
        }
        return http_url;
    }
    
    //this is the cleint that will use the axis version of the 
    // client underneath
    
    public SRMClientV1(GlobusURL srmurl,
            GSSCredential user_cred,
            long retrytimeout,
            int numberofretries,
            Logger logger,
            boolean do_delegation,
            boolean full_delegation,
            String gss_expected_name,
            String webservice_path) throws IOException,InterruptedException,
            javax.xml.rpc.ServiceException    {
	
	say("In Server Side: webservice_path= "+webservice_path);
	
	esay("constructor: srmurl = "+srmurl+" user_cred= "+ user_cred+" retrytimeout="+retrytimeout+" msec numberofretries="+numberofretries);
        this.retrytimeout = retrytimeout;
        this.retries = numberofretries;
        this.user_cred = user_cred;
        this.logger = logger;
        if(user_cred == null) {
            throw new NullPointerException("user credential is null");
        }
        try {
	    say("user credentials are: "+user_cred.getName());
            if(user_cred.getRemainingLifetime() < 60) {
                throw new IOException("credential remaining lifetime is less then a minute ");
            }
        }
        catch(org.ietf.jgss.GSSException gsse) {
            throw new IOException(gsse.toString());
        }
        
        
        //say("constructor: obtained socket factory");
        host = srmurl.getHost();
        host = InetAddress.getByName(host).getCanonicalHostName();
        int port = srmurl.getPort();
        String path = srmurl.getPath();
        if(path==null) {
            path="/";
        }
        service_url = ((port == 80)?"http://":"httpg://")+host + ":" +port ;
        int indx=path.indexOf(SFN_STRING);
        
        if(indx >0) {
            String service_postfix = path.substring(0,indx);
            if(!service_postfix.startsWith("/")){
                service_url += "/";
            }
            service_url += service_postfix;
	    //say("SFN Exists: service_url: "+service_url);
        }
        else {
	    
	    service_url += "/"+webservice_path;
	    //say("SFN doesnot Exist: service_url: "+service_url);	
        }
        say("SRMClientV1 calling org.globus.axis.util.Util.registerTransport() ");
        org.globus.axis.util.Util.registerTransport();
        org.apache.axis.configuration.SimpleProvider provider = 
            new org.apache.axis.configuration.SimpleProvider();

        org.apache.axis.SimpleTargetedChain c = null;

        c = new org.apache.axis.SimpleTargetedChain(new org.globus.axis.transport.GSIHTTPSender());
        provider.deployTransport("httpg", c);

        c = new org.apache.axis.SimpleTargetedChain(new  org.apache.axis.transport.http.HTTPSender());
        provider.deployTransport("http", c);
        org.dcache.srm.client.axis.SRMServerV1Locator sl = new org.dcache.srm.client.axis.SRMServerV1Locator(provider);
        java.net.URL url = new java.net.URL(service_url);
        say("connecting to srm at "+service_url);
        axis_isrm = sl.getISRM(url);
        if(axis_isrm instanceof org.apache.axis.client.Stub) {
            org.apache.axis.client.Stub axis_isrm_as_stub = (org.apache.axis.client.Stub)axis_isrm;
       		axis_isrm_as_stub._setProperty(org.globus.axis.transport.GSIHTTPTransport.GSI_CREDENTIALS,user_cred);
	        // sets authorization type
                axis_isrm_as_stub._setProperty(org.globus.axis.transport.GSIHTTPTransport.GSI_AUTHORIZATION,
                        new PromiscuousHostAuthorization());//HostAuthorization(gss_expected_name));
        	//axis_isrm_as_stub._setProperty(org.globus.axis.transport.GSIHTTPTransport.GSI_AUTHORIZATION,org.globus.gsi.gssapi.auth.HostAuthorization.getInstance());
                if (do_delegation) {
                    if(full_delegation) {
                        // sets gsi mode
                        axis_isrm_as_stub._setProperty(org.globus.axis.transport.GSIHTTPTransport.GSI_MODE,org.globus.axis.transport.GSIHTTPTransport.GSI_MODE_FULL_DELEG);
                    } else {
                        axis_isrm_as_stub._setProperty(org.globus.axis.transport.GSIHTTPTransport.GSI_MODE,org.globus.axis.transport.GSIHTTPTransport.GSI_MODE_LIMITED_DELEG);
                    }                    
                    
                } else {
                    // sets gsi mode
                    axis_isrm_as_stub._setProperty(org.globus.axis.transport.GSIHTTPTransport.GSI_MODE,org.globus.axis.transport.GSIHTTPTransport.GSI_MODE_NO_DELEG);
                }
        }
        else {
            throw new java.io.IOException("can't set properties to the axis_isrm");
        }
    }

    public diskCacheV111.srm.RequestStatus put( String[] sources,
    String[] dests,
    long[] sizes,
    boolean[] wantPerm,
    String[] protocols ) {
        for(int i = 0 ; i<sources.length;++i) {
            say("\tput, sources["+i+"]=\""+sources[i]+"\"");
        }
        for(int i = 0 ; i<dests.length;++i) {
            say("\tput, dests["+i+"]=\""+dests[i]+"\"");
        }
        for(int i = 0 ; i<protocols.length;++i) {
            say("\tput, protocols["+i+"]=\""+protocols[i]+"\"");
        }
        say(" put, contacting service " + service_url);
        int i = 0;
        while(true) {

            try {
                if(user_cred.getRemainingLifetime() < 60) {
                    throw new RuntimeException(
                       "credential remaining lifetime is less " +
                       "than one minute ");
                }
            }
            catch(org.ietf.jgss.GSSException gsse) {
                throw new RuntimeException(gsse);
            }


            try {
                try {
                    org.dcache.srm.client.axis.RequestStatus rs =
                       axis_isrm.put(sources, dests,
                                     sizes, wantPerm, protocols);
                    return ConvertUtil.axisRS2RS(rs);
                }
                catch(java.rmi.RemoteException re) {
                    throw new RuntimeException(re);
                }
            }
            catch(RuntimeException e) {
                esay("put: try # "+i+" failed with error");
                esay(e.getMessage());
                throw e;
            }
        }
    }
    
    public diskCacheV111.srm.RequestStatus get( String[] surls,String[] protocols ) {

        for(int i = 0 ; i<surls.length;++i) {
            say("\tget: surls["+i+"]=\""+surls[i]+"\"");
        }
        for(int i = 0 ; i<protocols.length;++i) {
            say("\tget: protocols["+i+"]=\""+protocols[i]+"\"");
        }
        int i = 0;
        while(true) {

            try {
                if(user_cred.getRemainingLifetime() < 60) {
                    throw new RuntimeException("credential remaining lifetime is less then a minute ");
                }
            }
            catch(org.ietf.jgss.GSSException gsse) {
                throw new RuntimeException(gsse);
            }

            try {
                try
                {
        org.dcache.srm.client.axis.RequestStatus rs = axis_isrm.get(surls,protocols);
                    return ConvertUtil.axisRS2RS(rs);
                }catch(java.rmi.RemoteException re) {
                    esay(re.toString());
                    throw new RuntimeException (re.toString());
                }

            }
            catch(RuntimeException e) {
                esay("get : try # "+i+" failed with error");
                esay(e.getMessage());
                throw e;
            }
        }
    }
    
    public diskCacheV111.srm.RequestStatus copy( String[] srcSURLS,
    String[] destSURLS,
    boolean[] wantPerm ) {
        for(int i = 0 ; i<srcSURLS.length;++i) {
            say("\tcopy, srcSURLS["+i+"]=\""+srcSURLS[i]+"\"");
        }
        for(int i = 0 ; i<destSURLS.length;++i) {
            say("\tcopy, destSURLS["+i+"]=\""+destSURLS[i]+"\"");
        }
        say(" copy, contacting service "+service_url);
        int i = 0;
        while(true) {

            try {
                if(user_cred.getRemainingLifetime() < 60) {
                    throw new RuntimeException("credential remaining lifetime is less then a minute ");
                }
            }
            catch(org.ietf.jgss.GSSException gsse) {
                throw new RuntimeException(gsse);
            }


            try {
                try
                {
                    org.dcache.srm.client.axis.RequestStatus rs = axis_isrm.copy(srcSURLS,destSURLS,wantPerm);
                    return ConvertUtil.axisRS2RS(rs);
                }catch(java.rmi.RemoteException re) {
                    //esay(re);
                    throw new RuntimeException (re.toString());
                }

            }
            catch(RuntimeException e) {
                esay("copy: try # "+i+" failed with error");
                esay(e.getMessage());
                throw e;
            }
        }
    }
    
    public diskCacheV111.srm.RequestStatus getRequestStatus( int requestId ) {
        int i = 0;
        while(true)
        {
            try {
                if(user_cred.getRemainingLifetime() < 60) {
                    throw new RuntimeException("credential remaining lifetime is less then a minute ");
                }
            }
            catch(org.ietf.jgss.GSSException gsse) {
                throw new RuntimeException(gsse);
            }


            try {

                try
                {
                    org.dcache.srm.client.axis.RequestStatus rs = axis_isrm.getRequestStatus(requestId);
                    return ConvertUtil.axisRS2RS(rs);
                }catch(java.rmi.RemoteException re) {
                    //esay(re);
                    throw new RuntimeException (re.toString());
                }
            }
            catch(RuntimeException e) {
                esay("getRequestStatus: try #"+i+" failed with error");
                esay(e.getMessage());
                if(i <retries) {
                    i++;
                    esay("getRequestStatus: try again");
                }
                else {
                    throw e;
                }
            }

            try {
                say("sleeping for "+(retrytimeout*i)+ " milliseconds before retrying");
                Thread.sleep(retrytimeout*i);
            }
            catch(InterruptedException ie) {
            }
        }
    }

    public boolean ping() {
        say(" ping, contacting service "+service_url);
        int i = 0;
        while(true) {

            try {
                if(user_cred.getRemainingLifetime() < 60) {
                    throw new RuntimeException("credential remaining lifetime is less then a minute ");
                }
            }
            catch(org.ietf.jgss.GSSException gsse) {
                throw new RuntimeException(gsse);
            }


            try {
                try
                {
                      return axis_isrm.ping();
                }catch(java.rmi.RemoteException re) {
                    //esay(re);
                    throw new RuntimeException (re.toString());
                }

            }
            catch(RuntimeException e) {
                esay("ping: try # "+i+" failed with error");
                esay(e.getMessage());
                if(i <retries) {
                    i++;
                    esay("ping: try again");
                }
                else {
                    throw e;
                }
            }
            try {
                say("sleeping for "+(retrytimeout*i)+ " milliseconds before retrying");
                Thread.sleep(retrytimeout*i);
            }
            catch(InterruptedException ie) {
            }

        }
    }
    
    public diskCacheV111.srm.RequestStatus mkPermanent( String[] SURLS ) {
        throw new UnsupportedOperationException("Not Implemented");
    }
    
    public diskCacheV111.srm.RequestStatus pin( String[] TURLS ) {
        throw new UnsupportedOperationException("Not Implemented");
    }
    
    public diskCacheV111.srm.RequestStatus unPin( String[] TURLS ,int requestID) {
        throw new UnsupportedOperationException("Not Implemented");
    }
    
    public diskCacheV111.srm.RequestStatus getEstGetTime( String[] SURLS ,String[] protocols) {
        throw new UnsupportedOperationException("Not Implemented");
    }
    
    public diskCacheV111.srm.RequestStatus getEstPutTime( String[] src_names,
    String[] dest_names,
    long[] sizes,
    boolean[] wantPermanent,
    String[] protocols) {
        throw new UnsupportedOperationException("Not Implemented");
    }
    
    public diskCacheV111.srm.FileMetaData[] getFileMetaData( String[] SURLS ) {
        if (axis_isrm == null) { throw new NullPointerException ("both isrms are null!!!!");}
        say(" getFileMetaData, contacting service "+service_url);
        int i = 0;
        while(true) {

            try {
                if(user_cred.getRemainingLifetime() < 60) {
                    throw new RuntimeException("credential remaining lifetime is less then a minute ");
                }
            }
            catch(org.ietf.jgss.GSSException gsse) {
                throw new RuntimeException(gsse);
            }


            try {
                try
                {
                    org.dcache.srm.client.axis.FileMetaData[] fmd = axis_isrm.getFileMetaData(SURLS);
                     return ConvertUtil.axisFMDs2FMDs(fmd);
                }catch(java.rmi.RemoteException re) {
                    //esay(re);
                    throw new RuntimeException (re.toString());
                }

            }
            catch(RuntimeException e) {
                esay("copy: try # "+i+" failed with error");
                esay(e.getMessage());
                if(i <retries) {
                    i++;
                    esay("copy: try again");
                }
                else {
                    throw e;
                }
            }
            try {
                say("sleeping for "+(retrytimeout*i)+ " milliseconds before retrying");
                Thread.sleep(retrytimeout*i);
            }
            catch(InterruptedException ie) {
            }
        }
    }
    
    public diskCacheV111.srm.RequestStatus setFileStatus( int requestId,
    int fileId,
    String state ) {
        int i = 0;
        while(true)
        {
            try {
                if(user_cred.getRemainingLifetime() < 60) {
                    throw new RuntimeException("credential remaining lifetime is less then a minute ");
                }
            }
            catch(org.ietf.jgss.GSSException gsse) {
                throw new RuntimeException(gsse);
            }


            try {

                try
                {
                    org.dcache.srm.client.axis.RequestStatus rs = axis_isrm.setFileStatus(requestId,fileId,state);
                    return ConvertUtil.axisRS2RS(rs);
                }catch(java.rmi.RemoteException re) {
                    //esay(re);
                    throw new RuntimeException (re.toString());
                }
                //say("getRequestStatus returned");
            }
            catch(RuntimeException e) {
                esay("getRequestStatus: try #"+i+" failed with error");
                esay(e.getMessage());
                /*
                 * we do not retry in case of setFileStatus for reasons of performanse
                 * and because the setFileStatus fails too often for castor implementation
                 *
                if(i <retries)
                 */
                 if(false)
                 {
                    i++;
                    esay("getRequestStatus: try again");
                }
                else

                 {
                    throw e;
                }
            }

            try {
                say("sleeping for "+(retrytimeout*i)+ " milliseconds before retrying");
                Thread.sleep(retrytimeout*i);
            }
            catch(InterruptedException ie) {
            }
        }
    }
    
    public void advisoryDelete( String[] SURLS) {
        for(int i = 0 ; i<SURLS.length;++i) {
            say("\tadvisoryDelete SURLS["+i+"]=\""+SURLS[i]+"\"");
        }
        say(" advisoryDelete, contacting service "+service_url);
        int i = 0;

        try {
            if(user_cred.getRemainingLifetime() < 60) {
                throw new RuntimeException("credential remaining lifetime is less then a minute ");
            }
        }
        catch(org.ietf.jgss.GSSException gsse) {
            throw new RuntimeException(gsse);
        }


        try
        {
             axis_isrm.advisoryDelete(SURLS);
             return;
        }catch(java.rmi.RemoteException re) {
            //esay(re);
            String message = re.getMessage();
            if(message != null)  throw new RuntimeException (message);
            throw new RuntimeException (re);
        }
   }
    
    public String[] getProtocols() {
        say(" getProtocols, contacting service "+service_url);
        int i = 0;
        while(true) {

            try {
                if(user_cred.getRemainingLifetime() < 60) {
                    throw new RuntimeException("credential remaining lifetime is less then a minute ");
                }
            }
            catch(org.ietf.jgss.GSSException gsse) {
                throw new RuntimeException(gsse);
            }


            try {
                try
                {
                      String protocols[] =axis_isrm.getProtocols();
                      return protocols;
                }catch(java.rmi.RemoteException re) {
                    //esay(re);
                    throw new RuntimeException (re.toString());
                }

            }
            catch(RuntimeException e) {
                esay("getProtocols: try # "+i+" failed with error");
                esay(e.getMessage());
                if(i <retries) {
                    i++;
                    esay("getProtocols: try again");
                }
                else {
                    throw e;
                }
            }
            try {
                say("sleeping for "+(retrytimeout*i)+ " milliseconds before retrying");
                Thread.sleep(retrytimeout*i);
            }
            catch(InterruptedException ie) {
            }
        }
    }
}
