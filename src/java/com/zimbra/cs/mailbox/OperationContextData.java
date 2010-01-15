/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailbox;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.SetUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Mailbox.FolderNode;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;

public abstract class OperationContextData {
    
    protected OperationContext mOctxt;
    
    protected OperationContextData(OperationContext octxt) {
        mOctxt = octxt;
    }
    
    /**
     * 
     * bug 35079: avoid potential excessive LDAP searches while XML
     *            encoding folder ACLs 
     * 
     * - collect grantee ids on all folder user shares of the mailbox
     * - resolve all ids into names (LDAP search if necessary)
     * - set the ids-to-names map in the OperationContext
     *     
     * 
     * @param octxt
     * @param mbox
     */
    public static void addGranteeNames(OperationContext octxt, Mailbox.FolderNode node) {
        if (octxt == null || node == null)
            return;
        
        GranteeNames data = getGranteeNames(octxt);
        if (data == null) {
            data = new GranteeNames(octxt);
            octxt.SetCtxtData(GranteeNames.getKey(), data);
        } 
        data.addRootNode(node);
    }
    
    public static GranteeNames getGranteeNames(OperationContext octxt) {
        if (octxt == null)
            return null;
        else
            return (GranteeNames)octxt.getCtxtData(GranteeNames.getKey());
    }
    
    /**
     * 
     * GranteeNames
     *
     */
    public static class GranteeNames extends OperationContextData {
        private static String getKey() {
            return "GranteeNames";
        }
        
        private static final int USR_GRANTEES = 0;
        private static final int GRP_GRANTEES = 1;
        private static final int COS_GRANTEES = 2;
        private static final int DOM_GRANTEES = 3;
        private static final int NUM_GRANTEE_TYPES = 4;
        
        private Set<Mailbox.FolderNode> mUnresolvedRootNodes; // unresolved root nodes
        private Set<Mailbox.FolderNode> mResolvedRootNodes;   // resolved root nodes
        
        // id-to-name map
        private Map<String, String>[] mIdsToNamesMap = new Map[NUM_GRANTEE_TYPES];
       
        private GranteeNames(OperationContext octxt) {
            super(octxt);
        }
        
        private void addRootNode(Mailbox.FolderNode node) {
            if (mUnresolvedRootNodes == null)
                mUnresolvedRootNodes = new HashSet<Mailbox.FolderNode>();
            
            /*
             * We resolve the hierarchy lazily.  
             * When a root node is added, it is put in the unresolved set.
             * When a root node is resolved, move it to the resolved set.
             */
            
            boolean alreadyResolved = false;
            if (mResolvedRootNodes != null) {
                for (Mailbox.FolderNode resolvedNode : mResolvedRootNodes) {
                    if (resolvedNode.mId == node.mId) {
                        alreadyResolved = true;
                        break;
                    }
                }
            }
            
            // root node already added but not yet resolved
            boolean alreadyAdded = false;
            for (Mailbox.FolderNode unresolvedNode : mUnresolvedRootNodes) {
                if (unresolvedNode.mId == node.mId) {
                    alreadyAdded = true;
                    break;
                }
            }
            
            // add it to the unresolved set if it had not been added before
            if (!alreadyResolved && !alreadyAdded)
                mUnresolvedRootNodes.add(node);
        }
        
        private void resolveIfNecessary() {
            if (mUnresolvedRootNodes == null || mUnresolvedRootNodes.isEmpty())
                return;
            
            for (Mailbox.FolderNode unresolvedNode : mUnresolvedRootNodes) {
                // get all grantees of this folder and all sub-folders
                Set[] idHolders = new Set[NUM_GRANTEE_TYPES];
                collectGranteeIds(unresolvedNode, idHolders);
                    
                // minus the ids already in our map
                for (int bucket = 0; bucket < NUM_GRANTEE_TYPES; bucket++) {
                    if (idHolders[bucket] != null && mIdsToNamesMap[bucket] != null) {
                        idHolders[bucket] = SetUtil.subtract(idHolders[bucket], mIdsToNamesMap[bucket].keySet());
                    }
                }
                populateIdToNameMaps(idHolders);
            }
            
            // move nodes to resolved set
            if (mResolvedRootNodes == null)
                mResolvedRootNodes = new HashSet<Mailbox.FolderNode>();
            mResolvedRootNodes.addAll(mUnresolvedRootNodes);
            mUnresolvedRootNodes.clear();
        }
        
        private void populateIdToNameMaps(Set<String>[] idHolders) {
            Map<String, String> result = null;
            
            for (int bucket = 0; bucket < NUM_GRANTEE_TYPES; bucket++) {
                if (idHolders[bucket] == null || idHolders[bucket].isEmpty())
                    continue;
                
                try {
                    Provisioning.EntryType entryType = null;
                    if (bucket == USR_GRANTEES)
                        entryType = Provisioning.EntryType.account;
                    else if (bucket == GRP_GRANTEES)
                        entryType = Provisioning.EntryType.group;
                    else if (bucket == COS_GRANTEES)
                        entryType = Provisioning.EntryType.cos;
                    else if (bucket == DOM_GRANTEES)
                        entryType = Provisioning.EntryType.domain;
                    
                    if (entryType != null)  // should not
                        result = Provisioning.getInstance().getNamesForIds(idHolders[bucket], entryType);
                } catch (ServiceException e) {
                    // log a warning, return an empty map, and let the flow continue
                    ZimbraLog.mailbox.warn("cannot lookup user grantee names", e);
                }
                
                if (result != null) {
                    if (mIdsToNamesMap[bucket] == null)
                        mIdsToNamesMap[bucket] = result;
                    else
                        mIdsToNamesMap[bucket].putAll(result);
                }
            }
        }
        
        private void collectGranteeIds(FolderNode node, Set<String>[] idHolders) {
            if (node.mFolder != null) {
                ACL acl = node.mFolder.getEffectiveACL();
                collectGranteeIdsOnACL(acl, idHolders);
            }
            
            for (FolderNode subNode : node.mSubfolders)
                collectGranteeIds(subNode, idHolders);
        }
        
        int getGranteeBucket(byte granteeType) {
            switch (granteeType) {
            case ACL.GRANTEE_USER:
                return USR_GRANTEES;
            case ACL.GRANTEE_GROUP:
                return GRP_GRANTEES;
            case ACL.GRANTEE_COS:
                return COS_GRANTEES;
            case ACL.GRANTEE_DOMAIN:
                return DOM_GRANTEES;
            default:
                return -1;
            }
        }
        
        private void collectGranteeIdsOnACL(ACL acl, Set<String>[] idHolders) {
            if (acl != null) {
                for (ACL.Grant grant : acl.getGrants()) {
                    int idx = getGranteeBucket(grant.getGranteeType());
                    if (idx != -1) {
                        if (idHolders[idx] == null)
                            idHolders[idx] = new HashSet<String>();
                        idHolders[idx].add(grant.getGranteeId());    
                    }
                }
            }
        }
        
        public String getNameById(String id, byte granteeType) {
            resolveIfNecessary();
            
            int idx = getGranteeBucket(granteeType);
            if (idx != -1) {
                // it's one of the grantee types we are responsible for (usr, grp, cos, dom)
                // mIdsToNamesMap[idx] should not be null, but if for whatever reason 
                // (some callsite missed calling us to populate?),
                // return null and let caller to look it up.
                if (mIdsToNamesMap[idx] == null)
                    return null;
                else {
                    String name = mIdsToNamesMap[idx].get(id);
                    // We've searched but didn't find the id, the grantee might have been deleted,
                    // return empty string so caller won't try to search for it again (bug 39804).
                    if (name == null)
                        return "";
                    else
                        return name;
                }
            } else
                return null;
        }

    }
}
