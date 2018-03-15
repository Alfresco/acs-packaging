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
package org.alfresco.web.ui.repo.tag;

import javax.faces.component.UICommand;
import javax.faces.component.UIComponent;

import org.alfresco.web.ui.common.tag.HtmlComponentTag;

/**
 * Tag class for the UINodePath component
 * 
 * @author Kevin Roast
 */
public class NodePathTag extends HtmlComponentTag
{
   /**
    * @see javax.faces.webapp.UIComponentTag#getComponentType()
    */
   public String getComponentType()
   {
      return "org.alfresco.faces.NodePath";
   }

   /**
    * @see javax.faces.webapp.UIComponentTag#getRendererType()
    */
   public String getRendererType()
   {
      return "org.alfresco.faces.NodePathLinkRenderer";
   }

   /**
    * @see javax.faces.webapp.UIComponentTag#setProperties(javax.faces.component.UIComponent)
    */
   protected void setProperties(UIComponent component)
   {
      super.setProperties(component);
      
      setActionProperty((UICommand)component, this.action);
      setActionListenerProperty((UICommand)component, this.actionListener);
      setBooleanProperty(component, "breadcrumb", this.breadcrumb);
      setBooleanProperty(component, "disabled", this.disabled);
      setBooleanProperty(component, "showLeaf", this.showLeaf);
      setStringBindingProperty(component, "value", this.value);
   }
   
   /**
    * @see org.alfresco.web.ui.common.tag.HtmlComponentTag#release()
    */
   public void release()
   {
      super.release();
      
      this.action = null;
      this.actionListener = null;
      this.value = null;
      this.disabled = null;
      this.breadcrumb = null;
      this.showLeaf = null;
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
    * Set the value
    *
    * @param value     the value
    */
   public void setValue(String value)
   {
      this.value = value;
   }
   
   /**
    * Set whether to display the path as a breadcrumb or a single long link (default)
    *
    * @param breadcrumb     breadcrumb true|false
    */
   public void setBreadcrumb(String breadcrumb)
   {
      this.breadcrumb = breadcrumb;
   }
   
   /**
    * Set whether the component is disabled
    *
    * @param disabled     whether the component is disabled
    */
   public void setDisabled(String disabled)
   {
      this.disabled = disabled;
   }
   
   /**
    * Set whether the final leaf node is shown as part of the path
    *
    * @param showLeaf     whether the final leaf node is shown as part of the path
    */
   public void setShowLeaf(String showLeaf)
   {
      this.showLeaf = showLeaf;
   }


   /** the showLeaf boolean */
   private String showLeaf;

   /** the disabled boolean */
   private String disabled;

   /** the breadcrumb boolean */
   private String breadcrumb;

   /** the action */
   private String action;

   /** the actionListener */
   private String actionListener;

   /** the value */
   private String value;
}
