/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008 Zimbra, Inc.
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

package com.zimbra.cs.zclient;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.zclient.event.ZModifyConversationEvent;
import com.zimbra.cs.zclient.event.ZModifyEvent;
import com.zimbra.cs.zclient.event.ZModifyMessageEvent;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class ZConversation implements ZItem, ToZJSONObject {

    public enum Flag {
        unread('u'),
        draft('d'),
        flagged('f'),
        highPriority('!'),
        lowPriority('?'),
        sentByMe('s'),
        replied('r'),
        forwarded('w'),   
        attachment('a');

        private char mFlagChar;
        
        public char getFlagChar() { return mFlagChar; }

        public static String toNameList(String flags) {
            if (flags == null || flags.length() == 0) return "";            
            StringBuilder sb = new StringBuilder();
            for (int i=0; i < flags.length(); i++) {
                String v = null;
                for (Flag f : Flag.values()) {
                    if (f.getFlagChar() == flags.charAt(i)) {
                        v = f.name();
                        break;
                    }
                }
                if (sb.length() > 0) sb.append(", ");
                sb.append(v == null ? flags.substring(i, i+1) : v);
            }
            return sb.toString();
        }
        
        Flag(char flagChar) {
            mFlagChar = flagChar;
            
        }
    }

    private String mId;
    private String mFlags;
    private String mSubject;
    private String mTags;
    private int mMessageCount;
    private List<ZMessageSummary> mMessageSummaries;
        
    public ZConversation(Element e) throws ServiceException {
        mId = e.getAttribute(MailConstants.A_ID);
        mFlags = e.getAttribute(MailConstants.A_FLAGS, null);
        mTags = e.getAttribute(MailConstants.A_TAGS, null);
        mSubject = e.getAttribute(MailConstants.E_SUBJECT, null);
        mMessageCount = (int) e.getAttributeLong(MailConstants.A_NUM);
        
        mMessageSummaries = new ArrayList<ZMessageSummary>();
        for (Element msgEl: e.listElements(MailConstants.E_MSG)) {
            mMessageSummaries.add(new ZMessageSummary(msgEl));
        }        
    }

    public void modifyNotification(ZModifyEvent event) throws ServiceException {
    	if (event instanceof ZModifyConversationEvent) {
    		ZModifyConversationEvent cevent = (ZModifyConversationEvent) event;
            if (cevent.getId().equals(mId)) {
                mFlags = cevent.getFlags(mFlags);
                mTags = cevent.getTagIds(mTags);
                mSubject = cevent.getSubject(mSubject);
                //mFragment = cevent.getFragment(mFragment);
                mMessageCount = cevent.getMessageCount(mMessageCount);
                //mRecipients = cevent.getRecipients(mRecipients);
            }
        }
    }

    public String getId() {
        return mId;
    }

    public ZJSONObject toZJSONObject() throws JSONException {
        ZJSONObject jo = new ZJSONObject();
        jo.put("id", mId);
        jo.put("tags", mTags);
        jo.put("flags", mFlags);
        jo.put("subject", mSubject);
        jo.put("messageCount", mMessageCount);
        jo.put("messages", mMessageSummaries);
        return jo;
    }

    public String toString() {
        return ZJSONObject.toString(this);
    }

    public String getFlags() {
        return mFlags;
    }

    public String getSubject() {
        return mSubject;
    }

    public String getTagIds() {
        return mTags;
    }

    public int getMessageCount() {
        return mMessageCount;
    }

    public List<ZMessageSummary> getMessageSummaries() {
        return mMessageSummaries;
    }
    
    public class ZMessageSummary implements ZItem, ToZJSONObject {

        private long mDate;
        private String mFlags;
        private String mTags;        
        private String mFragment;
        private String mId;
        private String mFolderId;
        private ZEmailAddress mSender;
        private long mSize;
        private Element mElement;
        
        public ZMessageSummary(Element e) throws ServiceException {
            mElement = e;
            mId = e.getAttribute(MailConstants.A_ID);
            mFlags = e.getAttribute(MailConstants.A_FLAGS, null);
            mDate = e.getAttributeLong(MailConstants.A_DATE);
            mTags = e.getAttribute(MailConstants.A_TAGS, null);
            mFolderId = e.getAttribute(MailConstants.A_FOLDER, null);
            mFragment = e.getAttribute(MailConstants.E_FRAG, null);
            mSize = e.getAttributeLong(MailConstants.A_SIZE);
            Element emailEl = e.getOptionalElement(MailConstants.E_EMAIL);
            if (emailEl != null) mSender = new ZEmailAddress(emailEl);
        }
        
        public Element getElement() { return mElement; }
        
        public void modifyNotification(ZModifyEvent event) throws ServiceException {
        	if (event instanceof ZModifyMessageEvent) {
        		ZModifyMessageEvent mevent = (ZModifyMessageEvent) event;
                if (mevent.getId().equals(mId)) {
                    mFlags = mevent.getFlags(mFlags);
                    mTags = mevent.getTagIds(mTags);
                    mFolderId = mevent.getFolderId(mFolderId);
                }
            }
        }

        public ZJSONObject toZJSONObject() throws JSONException {
            ZJSONObject jo = new ZJSONObject();
            jo.put("id", mId);
            jo.put("folderId", mFolderId);
            jo.put("flags", mFlags);
            jo.put("fragment", mFragment);
            jo.put("tags", mTags);
            jo.put("size", mSize);
            jo.put("sender", mSender);
            jo.put("date", mDate);
            return jo;
        }

        public String toString() {
            return ZJSONObject.toString(this);
        }
        public long getDate() {
            return mDate;
        }

        public String getFlags() {
            return mFlags;
        }

        public String getFragment() {
            return mFragment;
        }

        public String getId() {
            return mId;
        }

        public ZEmailAddress getSender() {
            return mSender;
        }

        public long getSize() {
            return mSize;
        }

        public String getFolderId() {
            return mFolderId;
        }

        public String getTagIds() {
            return mTags;
        }              
    }
    
    public boolean hasFlags() {
        return mFlags != null && mFlags.length() > 0;
    }

    public boolean hasTags() {
        return mTags != null && mTags.length() > 0;        
    }

    public boolean hasAttachment() {
        return hasFlags() && mFlags.indexOf(ZConversation.Flag.attachment.getFlagChar()) != -1;
    }

    public boolean isFlagged() {
        return hasFlags() && mFlags.indexOf(ZConversation.Flag.flagged.getFlagChar()) != -1;
    }

    public boolean isSentByMe() {
        return hasFlags() && mFlags.indexOf(ZConversation.Flag.sentByMe.getFlagChar()) != -1;
    }

    public boolean isUnread() {
        return hasFlags() && mFlags.indexOf(ZConversation.Flag.unread.getFlagChar()) != -1;
    }

    public boolean isForwarded() {
        return hasFlags() && mFlags.indexOf(ZConversation.Flag.forwarded.getFlagChar()) != -1;
    }

    public boolean isRepliedTo() {
        return hasFlags() && mFlags.indexOf(ZConversation.Flag.replied.getFlagChar()) != -1;
    }

    public boolean isDraft() {
        return hasFlags() && mFlags.indexOf(ZConversation.Flag.draft.getFlagChar()) != -1;
    }

}
