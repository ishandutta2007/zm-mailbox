package com.zimbra.common.net;

/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import javax.net.ssl.SSLSocketFactory;

import com.sun.net.ssl.SSLContext;
import com.sun.net.ssl.TrustManager;
import com.zimbra.common.net.GullibleTrustManager;
import com.zimbra.common.util.SystemUtil;

/**
<p>
* EasySSLProtocolSocketFactory can be used to creats SSL {@link Socket}s 
* that accept self-signed certificates. 
* </p>
* <p>
* This socket factory SHOULD NOT be used for productive systems 
* due to security reasons, unless it is a concious decision and 
* you are perfectly aware of security implications of accepting 
* self-signed certificates
* </p>
*/
public class EasySSLSocketFactory extends SSLSocketFactory {
    private SSLSocketFactory factory = null;
    
    public EasySSLSocketFactory() {
        try {
            SSLContext sslcontext = SSLContext.getInstance("TLS");
            sslcontext.init(null,
                            new TrustManager[] { new GullibleTrustManager(null) },
                            null);
            factory = sslcontext.getSocketFactory();
        } catch(Exception ex) {
            // Use System.out here instead of Log4j, since this class is likely
            // to be used by client code and Log4J may not be available.
            System.out.println("Unable to initialize SSL:\n" + SystemUtil.getStackTrace(ex));
        }
    }
    
    public static SSLSocketFactory getDefault() {
        return new EasySSLSocketFactory();
    }
    
    public Socket createSocket(Socket socket, String s, int i, boolean flag) throws IOException {
        return factory.createSocket(socket, s, i, flag);
    }
    
    public Socket createSocket(InetAddress addr, int port,
                               InetAddress localAddr, int localPort) throws IOException {
        return factory.createSocket(addr, port, localAddr, localPort);
    }
    
    public Socket createSocket(InetAddress addr, int port) throws IOException {
        return factory.createSocket(addr, port);
    }

    public Socket createSocket(String host, int port, InetAddress localAddr, int localPort) throws IOException {
        return factory.createSocket(host, port, localAddr, localPort);
    }

    public Socket createSocket(String host, int port) throws IOException {
        return factory.createSocket(host, port);
    }
    
    public Socket createSocket() throws IOException {
        return factory.createSocket();
    }

    public String[] getDefaultCipherSuites() {
        return factory.getDefaultCipherSuites();
    }

    public String[] getSupportedCipherSuites() {
        return factory.getSupportedCipherSuites();
    }
    

}
