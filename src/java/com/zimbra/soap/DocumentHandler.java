/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on May 26, 2004
 */
package com.zimbra.soap;

import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.ServerBy;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.session.Session;
import com.zimbra.cs.session.SessionCache;
import com.zimbra.cs.util.EmailUtil;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.cs.util.ZimbraLog;

/**
 * @author schemers
 */
public abstract class DocumentHandler {

    public static String LOCAL_HOST;
    static {
        try {
            LOCAL_HOST = Provisioning.getInstance().getLocalServer().getAttr(Provisioning.A_zimbraServiceHostname);
        } catch (Exception e) {
            Zimbra.halt("could not fetch local server name from LDAP for request proxying");
        }
    }

    public abstract Element handle(Element request, Map<String, Object> context) throws ServiceException, SoapFaultException;

    public static ZimbraSoapContext getZimbraSoapContext(Map<String, Object> context) {
        return (ZimbraSoapContext) context.get(SoapEngine.ZIMBRA_CONTEXT);
    }

    public static Account getRequestedAccount(ZimbraSoapContext lc) throws ServiceException {
        String id = lc.getRequestedAccountId();

        Account acct = Provisioning.getInstance().get(AccountBy.id, id);
        if (acct == null)
            throw ServiceException.AUTH_EXPIRED();
        return acct;
    }

    public static Mailbox getRequestedMailbox(ZimbraSoapContext lc) throws ServiceException {
        String id = lc.getRequestedAccountId();
        Mailbox mbox = Mailbox.getMailboxByAccountId(id);
        if (mbox != null)
            ZimbraLog.addToContext(mbox);
        return mbox; 
    }

    /** Returns whether the command's caller must be authenticated. */
    public boolean needsAuth(Map<String, Object> context) {
        return true;
    }

    /** Returns whether this is an administrative command (and thus requires
     *  a valid admin auth token). */
    public boolean needsAdminAuth(Map<String, Object> context) {
        return false;
    }

    public boolean isDomainAdminOnly(ZimbraSoapContext lc) {
        return AccessManager.getInstance().isDomainAdminOnly(lc.getAuthToken());
    }

    public boolean canAccessAccount(ZimbraSoapContext lc, Account target) throws ServiceException {
        return AccessManager.getInstance().canAccessAccount(lc.getAuthToken(), target);
    }

    public Domain getAuthTokenAccountDomain(ZimbraSoapContext lc) throws ServiceException {
        return AccessManager.getInstance().getDomain(lc.getAuthToken());
    }

    public boolean canAccessDomain(ZimbraSoapContext lc, String domainName) throws ServiceException {
        return AccessManager.getInstance().canAccessDomain(lc.getAuthToken(), domainName);
    }

    public boolean canAccessDomain(ZimbraSoapContext lc, Domain domain) throws ServiceException {
        return canAccessDomain(lc, domain.getName());
    }

    public boolean canAccessEmail(ZimbraSoapContext lc, String email) throws ServiceException {
        String parts[] = EmailUtil.getLocalPartAndDomain(email);
        if (parts == null)
            throw ServiceException.INVALID_REQUEST("must be valid email address: "+email, null);
        return canAccessDomain(lc, parts[1]);
    }

    /**
     * returns true if domain admin auth is sufficient to run this command. This should be overriden only on admin
     * commands that can be run in a restricted "domain admin" mode.
     */
    public boolean domainAuthSufficient(Map<String, Object> context) {
        return false; 
    }

    /** Returns whether the command is in the administration command set. */
    public boolean isAdminCommand() {
        return false;
    }

    /** Returns <code>true</code> if the operation is read-only, or
     *  <code>false</code> if the operation causes backend state change. */
    public boolean isReadOnly() {
        return true;
    }

    /** Returns whether the client making the SOAP request is localhost. */
    protected boolean clientIsLocal(Map<String, Object> context) {
        HttpServletRequest req = (HttpServletRequest) context.get(SoapServlet.SERVLET_REQUEST);
        if (req == null) return true;
        String peerIP = req.getRemoteAddr();
        return "127.0.0.1".equals(peerIP);
    }

    /** Fetches the in-memory {@link Session} object appropriate for this
     *  request.  If none already exists, one is created if possible.
     * 
     * @param context  The Map containing context information for this SOAP
     *                 request.
     * @return A {@link com.zimbra.cs.session.SoapSession}, or
     *         <code>null</code>. */
    public Session getSession(Map<String, Object> context) {
        return getSession(context, SessionCache.SESSION_SOAP);
    }

    /** Fetches a {@link Session} object to persist and manage state between
     *  SOAP requests.  If no appropriate session already exists, a new one
     *  is created if possible.
     * 
     * @param context      The Map containing context information for this
     *                     SOAP request.
     * @param sessionType  The type of session needed.
     * @return An in-memory {@link Session} object of the specified type,
     *         fetched from the request's {@link ZimbraSoapContext} object, or
     *         <code>null</code>.
     * @see SessionCache#SESSION_SOAP
     * @see SessionCache#SESSION_ADMIN */
    protected Session getSession(Map<String, Object> context, int sessionType) {
        ZimbraSoapContext lc = getZimbraSoapContext(context);
        return (lc == null ? null : lc.getSession(sessionType));
    }


    /** Returns the {@link Server} object where an Account (specified by ID)
     *  is homed.  This is similar to {@link Provisioning#getServer(Account),
     *  except that the account is specified by ID and exceptions are thrown
     *  on failure rather than returning null.
     * @param acctId  The Zimbra ID of the account.
     * @throws ServiceException  The following error codes are possible:<ul>
     *    <li><code>account.NO_SUCH_ACCOUNT</code> - if there is no Account
     *        with the specified ID
     *    <li><code>account.NO_SUCH_SERVER</code> - if the Server associated
     *        with the Account does not exist</ul> */
    protected static Server getServer(String acctId) throws ServiceException {
        Account acct = Provisioning.getInstance().get(AccountBy.id, acctId);
        if (acct == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(acctId);
        String hostname = acct.getAttr(Provisioning.A_zimbraMailHost);

        Server server = Provisioning.getInstance().get(ServerBy.name, hostname);
        if (server == null)
            throw AccountServiceException.NO_SUCH_SERVER(hostname);
        return server;
    }

    protected static String getXPath(Element request, String[] xpath) {
        int depth = 0;
        while (depth < xpath.length - 1 && request != null)
            request = request.getOptionalElement(xpath[depth++]);
        return (request == null ? null : request.getAttribute(xpath[depth], null));
    }

    protected static void setXPath(Element request, String[] xpath, String value) throws ServiceException {
        if (xpath == null || xpath.length == 0)
            return;
        int depth = 0;
        while (depth < xpath.length - 1 && request != null)
            request = request.getOptionalElement(xpath[depth++]);
        if (request == null)
            throw ServiceException.INVALID_REQUEST("could not find path", null);
        request.addAttribute(xpath[depth], value);
    }

    protected Element proxyIfNecessary(Element request, Map<String, Object> context) throws ServiceException {
        // if the "target account" is remote and the command is non-admin, proxy.
        ZimbraSoapContext lc = getZimbraSoapContext(context);
        String acctId = lc.getRequestedAccountId();
        if (acctId != null && lc.getProxyTarget() != null && !isAdminCommand()) {
            if (!LOCAL_HOST.equalsIgnoreCase(getRequestedAccount(lc).getAttr(Provisioning.A_zimbraMailHost)))
                return proxyRequest(request, context, acctId);
        }

        return null;
    }

    protected static Element proxyRequest(Element request, Map<String, Object> context, String acctId) throws ServiceException {
        ZimbraSoapContext lc = getZimbraSoapContext(context);
        // new context for proxied request has a different "requested account"
        ZimbraSoapContext lcTarget = new ZimbraSoapContext(lc, acctId);

        return proxyRequest(request, context, getServer(acctId), lcTarget);
    }

    protected static Element proxyRequest(Element request, Map<String, Object> context, Server server, ZimbraSoapContext lc)
    throws ServiceException {
        // figure out whether we can just re-dispatch or if we need to proxy via HTTP
        SoapEngine engine = (SoapEngine) context.get(SoapEngine.ZIMBRA_ENGINE);
        boolean isLocal = LOCAL_HOST.equalsIgnoreCase(server.getName()) && engine != null;

        Element response = null;
        request.detach();
        if (isLocal) {
            // executing on same server; just hand back to the SoapEngine
            Map<String, Object> contextTarget = new HashMap<String, Object>(context);
            contextTarget.put(SoapEngine.ZIMBRA_ENGINE, engine);
            contextTarget.put(SoapEngine.ZIMBRA_CONTEXT, lc);
            response = engine.dispatchRequest(request, contextTarget, lc);
            if (lc.getResponseProtocol().isFault(response)) {
                lc.getResponseProtocol().updateArgumentsForRemoteFault(response, lc.getRequestedAccountId());
                throw new SoapFaultException("error in proxied request", true, response);
            }
        } else {
            // executing remotely; find out target and proxy there
            HttpServletRequest httpreq = (HttpServletRequest) context.get(SoapServlet.SERVLET_REQUEST);
            ProxyTarget proxy = new ProxyTarget(server.getId(), lc.getRawAuthToken(), httpreq);
            response = proxy.dispatch(request, lc);
            response.detach();
        }
        return response;
    }
}
