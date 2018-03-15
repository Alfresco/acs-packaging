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
package org.alfresco.web.ui.repo.component;

import java.io.IOException;
import java.text.MessageFormat;

import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.el.ValueBinding;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.lock.LockStatus;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.web.app.Application;
import org.alfresco.web.bean.repository.Repository;
import org.alfresco.web.ui.common.Utils;
import org.springframework.extensions.webscripts.ui.common.component.SelfRenderingComponent;
import org.alfresco.web.ui.repo.WebResources;
import org.springframework.util.StringUtils;

/**
 * @author Kevin Roast
 */
public class UILockIcon extends SelfRenderingComponent
{
   private static final String MSG_LOCKED_YOU  = "locked_you";
   private static final String MSG_LOCKED_USER = "locked_user";
   
   // ------------------------------------------------------------------------------
   // Component implementation
   
   /**
    * @see javax.faces.component.UIComponent#getFamily()
    */
   public String getFamily()
   {
      return "org.alfresco.faces.LockIcon";
   }
   
   /**
    * @see javax.faces.component.StateHolder#restoreState(javax.faces.context.FacesContext, java.lang.Object)
    */
   public void restoreState(FacesContext context, Object state)
   {
      Object values[] = (Object[])state;
      // standard component attributes are restored by the super class
      super.restoreState(context, values[0]);
      this.lockImage = (String)values[1];
      this.lockOwnerImage = (String)values[2];
      this.align = (String)values[3];
      this.width = ((Integer)values[4]).intValue();
      this.height = ((Integer)values[5]).intValue();
      this.value = values[6];
   }
   
   /**
    * @see javax.faces.component.StateHolder#saveState(javax.faces.context.FacesContext)
    */
   public Object saveState(FacesContext context)
   {
      return new Object[] 
      {
         // standard component attributes are saved by the super class
         super.saveState(context),
         this.lockImage,
         this.lockOwnerImage,
         this.align,
         this.width,
         this.height,
         this.value
      };
   }
   
   /**
    * @see javax.faces.component.UIComponentBase#encodeBegin(javax.faces.context.FacesContext)
    */
   public void encodeBegin(FacesContext context) throws IOException
   {
      if (isRendered() == false)
      {
         return;
      }

      // get the value and see if the image is locked
      NodeService nodeService = getNodeService(context);
      String lockUser = null;
      Object val = getValue();
      NodeRef ref = null;
      if (val instanceof NodeRef)
      {
         ref = (NodeRef)val;
         if (nodeService.exists(ref))
         {
             LockStatus lockStatus = Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getLockService().getLockStatus(ref);
             if (lockStatus == LockStatus.LOCK_OWNER || lockStatus == LockStatus.LOCKED)
                 lockUser = (String)nodeService.getProperty(ref, ContentModel.PROP_LOCK_OWNER);
         }
      }
      final boolean locked = lockUser != null;
      final boolean lockedOwner = locked && (lockUser.equals(Application.getCurrentUser(context).getUserName()));

      this.encodeBegin(context, locked, lockedOwner, new String[] { lockUser });
   }

   protected void encodeBegin(final FacesContext context,
                              final boolean locked,
                              final boolean lockedOwner,
                              final String[] lockUser)
      throws IOException
   {
      if (isRendered() == false)
      {
         return;
      }
      ResponseWriter out = context.getResponseWriter();
      String msg = null;
      
      if (locked == true)
      {
         out.write("&nbsp;<img");
         
         outputAttribute(out, getAttributes().get("style"), "style");
         outputAttribute(out, getAttributes().get("styleClass"), "class");
         
         outputAttribute(out, getAlign(), "align");
         outputAttribute(out, getWidth(), "width");
         outputAttribute(out, getHeight(), "height");
         
         out.write("src=\"");
         out.write(context.getExternalContext().getRequestContextPath());
         String lockImage = getLockImage();
         if (lockedOwner == true && getLockOwnerImage() != null)
         {
            lockImage = getLockOwnerImage();
         }
         out.write(lockImage);
         out.write("\" border=0");
         
         if (lockedOwner == true)
         {
            msg = Application.getMessage(context, MSG_LOCKED_YOU);
            if (getLockedOwnerTooltip() != null)
            {
               msg = getLockedOwnerTooltip();
            }
         }
         else
         {
            msg = MessageFormat.format(Application.getMessage(context, MSG_LOCKED_USER), lockUser.length);
            if (getLockedUserTooltip() != null)
            {
               msg = getLockedUserTooltip();
            }
            StringBuilder buf = new StringBuilder(32);
            msg = buf.append(msg).append(" '")
                     .append(StringUtils.arrayToDelimitedString(lockUser, ", "))
                     .append("'").toString();
         }
         
         msg = Utils.encode(msg);
         out.write(" alt=\"");
         out.write(msg);
         out.write("\" title=\"");
         out.write(msg);
         out.write("\">");
      }
   }
   
   /**
    * Use Spring JSF integration to return the Node Service bean instance
    * 
    * @param context    FacesContext
    * 
    * @return Node Service bean instance or throws exception if not found
    */
   private static NodeService getNodeService(FacesContext context)
   {
      NodeService service = Repository.getServiceRegistry(context).getNodeService();
      if (service == null)
      {
         throw new IllegalStateException("Unable to obtain NodeService bean reference.");
      }
      
      return service;
   }
   
   
   // ------------------------------------------------------------------------------
   // Strongly typed component property accessors 
   
   /**
    * @return the image to display as the lock icon. A default is provided if none is set.
    */
   public String getLockImage()
   {
      ValueBinding vb = getValueBinding("lockImage");
      if (vb != null)
      {
         this.lockImage = (String)vb.getValue(getFacesContext());
      }
      
      return this.lockImage;
   }
   
   /**
    * @param lockImage     the image to display as the lock icon. A default is provided if none is set.
    */
   public void setLockImage(String lockImage)
   {
      this.lockImage = lockImage;
   }
   
   /**
    * @return Returns the image to display if the owner has the lock.
    */
   public String getLockOwnerImage()
   {
      ValueBinding vb = getValueBinding("lockOwnerImage");
      if (vb != null)
      {
         this.lockOwnerImage = (String)vb.getValue(getFacesContext());
      }
      
      return this.lockOwnerImage;
   }

   /**
    * @param lockOwnerImage     the image to display if the owner has the lock.
    */
   public void setLockOwnerImage(String lockOwnerImage)
   {
      this.lockOwnerImage = lockOwnerImage;
   }
   
   /**
    * @return Returns the image alignment value.
    */
   public String getAlign()
   {
      ValueBinding vb = getValueBinding("align");
      if (vb != null)
      {
         this.align = (String)vb.getValue(getFacesContext());
      }
      
      return this.align;
   }

   /**
    * @param align      The image alignment value to set.
    */
   public void setAlign(String align)
   {
      this.align = align;
   }

   /**
    * @return Returns the icon height.
    */
   public int getHeight()
   {
      ValueBinding vb = getValueBinding("height");
      if (vb != null)
      {
         Integer value = (Integer)vb.getValue(getFacesContext());
         if (value != null)
         {
            this.height = value.intValue();
         }
      }
      
      return this.height;
   }

   /**
    * @param height         The icon height to set.
    */
   public void setHeight(int height)
   {
      this.height = height;
   }
   
   /**
    * @return Returns the icon width.
    */
   public int getWidth()
   {
      ValueBinding vb = getValueBinding("width");
      if (vb != null)
      {
         Integer value = (Integer)vb.getValue(getFacesContext());
         if (value != null)
         {
            this.width = value.intValue();
         }
      }
      
      return this.width;
   }

   /**
    * @param width      The iconwidth to set.
    */
   public void setWidth(int width)
   {
      this.width = width;
   }

   /**
    * @return Returns the lockedOwnerTooltip.
    */
   public String getLockedOwnerTooltip()
   {
      ValueBinding vb = getValueBinding("lockedOwnerTooltip");
      if (vb != null)
      {
         this.lockedOwnerTooltip = (String)vb.getValue(getFacesContext());
      }
      
      return this.lockedOwnerTooltip;
   }

   /**
    * @param lockedOwnerTooltip The lockedOwnerTooltip to set.
    */
   public void setLockedOwnerTooltip(String lockedOwnerTooltip)
   {
      this.lockedOwnerTooltip = lockedOwnerTooltip;
   }

   /**
    * @return Returns the lockedUserTooltip.
    */
   public String getLockedUserTooltip()
   {
      ValueBinding vb = getValueBinding("lockedUserTooltip");
      if (vb != null)
      {
         this.lockedUserTooltip = (String)vb.getValue(getFacesContext());
      }
      
      return this.lockedUserTooltip;
   }

   /**
    * @param lockedUserTooltip The lockedUserTooltip to set.
    */
   public void setLockedUserTooltip(String lockedUserTooltip)
   {
      this.lockedUserTooltip = lockedUserTooltip;
   }
   
   /**
    * @return Returns the value (Node or NodeRef)
    */
   public Object getValue()
   {
      ValueBinding vb = getValueBinding("value");
      if (vb != null)
      {
         this.value = vb.getValue(getFacesContext());
      }
      
      return this.value;
   }

   /**
    * @param value The Node or NodeRef value to set.
    */
   public void setValue(Object value)
   {
      this.value = value;
   }
   
   
   private String lockImage = WebResources.IMAGE_LOCK;
   private String lockOwnerImage = WebResources.IMAGE_LOCK_OWNER;
   private String align = null;
   private int width = 16;
   private int height = 16;
   private String lockedOwnerTooltip = null;
   private String lockedUserTooltip = null;
   private Object value = null;
}
