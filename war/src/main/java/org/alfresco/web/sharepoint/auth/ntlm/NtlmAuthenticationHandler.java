/*
 * #%L
 * Alfresco Repository WAR Community
 * %%
 * Copyright (C) 2005 - 2016 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software. 
 * If the software was purchased under a paid Alfresco license, the terms of 
 * the paid license agreement will prevail.  Otherwise, the software is 
 * provided under the following open source license terms:
 * 
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
package org.alfresco.web.sharepoint.auth.ntlm;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.alfresco.repo.SessionUser;
import org.alfresco.repo.web.auth.WebCredentials;
import org.alfresco.repo.webdav.auth.BaseNTLMAuthenticationFilter;
import org.alfresco.repo.webdav.auth.SharepointConstants;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.repository.User;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <p>
 * NTLM SSO web authentication implementation.
 * </p>
 */
public class NtlmAuthenticationHandler extends BaseNTLMAuthenticationFilter
{
    // Debug logging
    private static Log logger = LogFactory.getLog(NtlmAuthenticationHandler.class);

    @Override
    protected void init() throws ServletException
    {
        setUserAttributeName(SharepointConstants.USER_SESSION_ATTRIBUTE);
        super.init();
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.repo.webdav.auth.BaseSSOAuthenticationFilter#onValidateFailed(javax.servlet.ServletContext, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, javax.servlet.http.HttpSession)
     */
    @Override
    protected void onValidateFailed(ServletContext sc, HttpServletRequest req, HttpServletResponse res, HttpSession session, WebCredentials credentials)
        throws IOException
    {
        super.onValidateFailed(sc, req, res, session, credentials);
        
        // Restart the login challenge process if validation fails
        restartLoginChallenge(sc, req, res);
    }    

    /* (non-Javadoc)
     * @see org.alfresco.repo.webdav.auth.BaseAuthenticationFilter#createUserObject(java.lang.String, java.lang.String, org.alfresco.service.cmr.repository.NodeRef, org.alfresco.service.cmr.repository.NodeRef)
     */
    @Override
    protected SessionUser createUserObject(String userName, String ticket, NodeRef personNode, NodeRef homeSpaceRef)
    {
        // Create a web client user object
        User user = new User( userName, ticket, personNode);
        user.setHomeSpaceId( homeSpaceRef.getId());
        
        return user;
    }
    
    @Override
    protected Log getLogger()
    {
        return logger;
    }
}