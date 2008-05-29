package com.zimbra.cs.account.accesscontrol;

import com.zimbra.common.service.ServiceException;

public class Right {
    
    // known rights
    public static Right RT_invite;
    public static Right RT_private;
    public static Right RT_viewFreeBusy;
    
    private final String mCode;
    private String mDesc;  // a brief description
    private String mDoc;   // a more detailed description, use cases, examples
    
    static void initKnownRights(RightManager rm) throws ServiceException {
        RT_invite = rm.getRight("invite");
        RT_private = rm.getRight("private");;
        RT_viewFreeBusy = rm.getRight("viewFreeBusy");;
    }

    Right(String code) {
        mCode = code;
    }
    
    /**
     * - code stored in the ACE.
     * - code appear in XML
     * - code displayed by CLI
     * 
     * @return 
     */
    public String getCode() {
        return mCode;
    }
    
    public String getDesc() {
        return mDesc;
    }
    
    public String getDoc() {
        return mDoc;
    }
    
    void setDesc(String desc) {
        mDesc = desc;
    }
    
    void setDoc(String doc) {
        mDoc = doc;
    }


}
