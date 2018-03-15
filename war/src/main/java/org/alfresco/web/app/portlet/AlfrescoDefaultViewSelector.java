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
package org.alfresco.web.app.portlet;

import javax.portlet.PortletContext;
import javax.portlet.PortletException;
import javax.portlet.PortletSession;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.web.app.servlet.AuthenticationHelper;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.repository.User;
import org.apache.myfaces.portlet.DefaultViewSelector;

/**
 * @author Kevin Roast
 */
public class AlfrescoDefaultViewSelector implements DefaultViewSelector
{
   /**
    * Select the appropriate view ID
    */
   public String selectViewId(RenderRequest request, RenderResponse response) throws PortletException
   {
      User user = (User) request.getPortletSession().getAttribute(AuthenticationHelper.AUTHENTICATION_USER,
            PortletSession.APPLICATION_SCOPE);
      if (user != null && user.getUserName().equals(AuthenticationUtil.getGuestUserName()))
      {
         return FacesHelper.BROWSE_VIEW_ID;
      }
      else
      {
         return null;
      }
   }
   
   /**
    * @see org.apache.myfaces.portlet.DefaultViewSelector#setPortletContext(javax.portlet.PortletContext)
    */
   public void setPortletContext(PortletContext portletContext)
   {
   }
}
