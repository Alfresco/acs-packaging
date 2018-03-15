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
package org.alfresco.web.ui.common.tag;

import javax.faces.component.UICommand;
import javax.faces.component.UIComponent;

/**
 * @author kevinr
 */
public class BreadcrumbTag extends HtmlComponentTag
{
   /**
    * @see javax.faces.webapp.UIComponentTag#getComponentType()
    */
   public String getComponentType()
   {
      return "org.alfresco.faces.Breadcrumb";
   }

   /**
    * @see javax.faces.webapp.UIComponentTag#getRendererType()
    */
   public String getRendererType()
   {
      return "org.alfresco.faces.BreadcrumbRenderer";
   }

   /**
    * @see javax.faces.webapp.UIComponentTag#setProperties(javax.faces.component.UIComponent)
    */
   protected void setProperties(UIComponent component)
   {
      super.setProperties(component);
      
      setActionProperty((UICommand)component, this.action);
      setActionListenerProperty((UICommand)component, this.actionListener);
      setStringProperty(component, "separator", this.separator);
      setBooleanProperty(component, "showRoot", this.showRoot);
      setBooleanProperty(component, "immediate", this.immediate);
      setStringProperty(component, "value", this.value);
   }

   /**
    * @see javax.servlet.jsp.tagext.Tag#release()
    */
   public void release()
   {
      super.release();
      this.action = null;
      this.actionListener = null;
      this.separator = ">";
      this.showRoot = "true";
      this.immediate = null;
      this.value = null;
   }
   
   /**
    * Set the action
    *
    * @param action     the action
    */
   public void setAction(String action)
   {
      this.action = action;
   }

   /**
    * Set the actionListener
    *
    * @param actionListener     the actionListener
    */
   public void setActionListener(String actionListener)
   {
      this.actionListener = actionListener;
   }

   /**
    * Set the separator
    *
    * @param separator     the separator
    */
   public void setSeparator(String separator)
   {
      this.separator = separator;
   }

   /**
    * Set the show root value
    *
    * @param showRoot     the showRoot
    */
   public void setShowRoot(String showRoot)
   {
      this.showRoot = showRoot;
   }
   
   /**
    * Set if the action event fired is immediate
    *
    * @param immediate     true if the action event fired is immediate
    */
   public void setImmediate(String immediate)
   {
      this.immediate = immediate;
   }
   
   /**
    * Set the value. The value for a breadcrumb is either a '/' separated String path
    * or a List of IBreadcrumb handler instances.
    *
    * @param value     the value
    */
   public void setValue(String value)
   {
      this.value = value;
   }


   /** the value */
   private String value;
   
   /** the action */
   private String action;

   /** the actionListener */
   private String actionListener;

   /** the separator */
   private String separator = ">";

   /** the showRoot value */
   private String showRoot = "true";
   
   /** true if the action event fired is immediate */
   private String immediate;
}
