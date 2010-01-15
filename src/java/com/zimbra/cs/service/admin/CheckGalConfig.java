/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Jun 17, 2004
 */
package com.zimbra.cs.service.admin;

import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.GalContact;
import com.zimbra.cs.account.gal.GalOp;
import com.zimbra.cs.account.ldap.Check;
import com.zimbra.cs.service.account.SearchGal;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author schemers
 */
public class CheckGalConfig extends AdminDocumentHandler {

	public Element handle(Element request, Map<String, Object> context) throws ServiceException {

        ZimbraSoapContext lc = getZimbraSoapContext(context);

        Element q = request.getOptionalElement(AdminConstants.E_QUERY);
        String query = null;
        long limit = 0;
        if (q != null) {
            query = q.getText();
            limit = q.getAttributeLong(AdminConstants.A_LIMIT, 10);
        }
        
        Element action = request.getOptionalElement(AdminConstants.E_ACTION);
        GalOp galOp = GalOp.search;
        if (action != null)
            galOp = GalOp.fromString(action.getText());
                
	    Map attrs = AdminService.getAttrs(request, true);


        Element response = lc.createElement(AdminConstants.CHECK_GAL_CONFIG_RESPONSE);
        Check.Result r = Check.checkGalConfig(attrs, query, (int)limit, galOp);
        
        response.addElement(AdminConstants.E_CODE).addText(r.getCode());
        String message = r.getMessage();
        if (message != null)
            response.addElement(AdminConstants.E_MESSAGE).addText(message);

        if (r instanceof Check.GalResult) {
            List<GalContact> contacts = ((Check.GalResult)r).getContacts();
            if (contacts != null) {
                for (GalContact contact : contacts) {
                    SearchGal.addContact(response, contact);
                }
            }
        }
	    return response;
	}
}
