/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009 Zimbra, Inc.
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
package com.zimbra.cs.dav;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

// HTTP protocol extensions and headers for WebDAV
public class DavProtocol {
	public enum Compliance {
		one, two, three, 
		update, bind, access_control, 
		calendar_access, calendar_schedule,
		version_control,
		// Apple extensions
		calendar_proxy,
		calendarserver_principal_property_search
	}
	
	private static HashMap<Compliance,String> sComplianceStrMap;

	// WEBDAV compliances:
	// Each DAV resources must declare the ones they are compliant to.
	//
	// class 1* : RFC 2518 - all "must" features
	// class 2* : RFC 2518 - locking
	// class 3 : draft-ietf-webdav-rfc2518bis
	// calendar-access* : draft-dusseault-caldav
	// calendar-schedule* : draft-desruisseux-caldav-sched
	// version-control* : RFC 3253 - section 3.6 (partial requirement from access-control and caldav)
	// checkout-in-place, version-history, 
	//   workspace, update, label, working-resource,
	//   merge, baseline, activity, version-controlled-collection : RFC 3253
	// ordered-collections : RFC 3648
	// access-control* : RFC 3744
	// redirectrefs : RFC 4437
	// bind : draft-ietf-webdav-bind
	// calendar-proxy, calendarserver-principal-property-search : Apple extension
	
	static {
		sComplianceStrMap = new HashMap<Compliance,String>();
		sComplianceStrMap.put(Compliance.one, "1");
		sComplianceStrMap.put(Compliance.two, "2");
		sComplianceStrMap.put(Compliance.three, "3");
		sComplianceStrMap.put(Compliance.update, "update");
		sComplianceStrMap.put(Compliance.bind, "bind");
		sComplianceStrMap.put(Compliance.access_control, "access-control");
		sComplianceStrMap.put(Compliance.calendar_access, "calendar-access");
		sComplianceStrMap.put(Compliance.calendar_schedule, "calendar-schedule");
		sComplianceStrMap.put(Compliance.version_control, "version-control");
		sComplianceStrMap.put(Compliance.calendar_proxy, "calendar-proxy");
		sComplianceStrMap.put(Compliance.calendarserver_principal_property_search, "calendarserver-principal-property-search");
	}
	
	public static Compliance[] COMPLIANCES = {
		Compliance.one, Compliance.two, Compliance.three, Compliance.calendar_access, Compliance.access_control
	};
	
	public static String getDefaultComplianceString() {
		return getComplianceString(Arrays.asList(COMPLIANCES));
	}
	
	public static String getComplianceString(Collection<Compliance> comp) {
		if (comp == null)
			return null;
		StringBuilder buf = new StringBuilder();
		for (Compliance c : comp) {
			if (buf.length() > 0)
				buf.append(", ");
			buf.append(sComplianceStrMap.get(c));
		}
		return buf.toString();
	}
	
	public static final String DAV_CONTENT_TYPE = "text/xml; charset=\"UTF-8\"";
	public static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";
	public static final String ENCODING_GZIP = "gzip";

	public static final String HEADER_CONTENT_TYPE = "Content-Type";
	public static final String HEADER_CONTENT_LENGTH = "Content-Length";
    public static final String HEADER_CONTENT_ENCODING = "Content-Encoding";
	public static final String HEADER_ALLOW = "Allow";
	public static final String HEADER_CACHE_CONTROL = "Cache-Control";
	public static final String HEADER_ETAG = "ETag";
	public static final String HEADER_USER_AGENT = "User-Agent";
	public static final String HEADER_ACCEPT_ENCODING = "Accept-Encoding";
	
	public static final String NO_CACHE = "no-cache";
	
	// dav extensions
	public static final String HEADER_DAV = "DAV";
	public static final String HEADER_DEPTH = "Depth";
	public static final String HEADER_DESTINATION = "Destination";
	public static final String HEADER_IF = "If";
	public static final String HEADER_LOCK_TOKEN = "Lock-Token";
	public static final String HEADER_OVERWRITE = "Overwrite";
	public static final String HEADER_STATUS_URI = "Status-URI";
	public static final String HEADER_TIMEOUT = "Timeout";
	
	// caldav extensions
	public static final String HEADER_IF_MATCH = "If-Match";
	public static final String HEADER_IF_NONE_MATCH = "If-None-Match";
	public static final String HEADER_ORIGINATOR = "Originator";
	public static final String HEADER_RECIPIENT = "Recipient";
	
	public static final int STATUS_PROCESSING = 102;
	public static final int STATUS_MULTI_STATUS = 207;
	public static final int STATUS_UNPROCESSABLE_ENTITY = 422;
	public static final int STATUS_LOCKED = 423;
	public static final int STATUS_FAILED_DEPENDENCY = 424;
	public static final int STATUS_INSUFFICIENT_STORAGE = 507;
}
