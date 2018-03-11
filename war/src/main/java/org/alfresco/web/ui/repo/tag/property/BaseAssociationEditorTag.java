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
package org.alfresco.web.ui.repo.tag.property;

import javax.faces.component.UIComponent;

import org.springframework.extensions.webscripts.ui.common.tag.BaseComponentTag;

/**
 * Base class for all association editor tag implementations
 * 
 * @author gavinc
 */
public abstract class BaseAssociationEditorTag extends BaseComponentTag
{
   private String associationName;
   private String availableOptionsSize;
   private String selectItemMsg;
   private String selectItemsMsg;
   private String selectedItemsMsg;
   private String noSelectedItemsMsg;
   private String disabled;
   private String value;
   
   /**
    * @see javax.faces.webapp.UIComponentTag#getRendererType()
    */
   public String getRendererType()
   {
      return null;
   }

   /**
    * @see javax.faces.webapp.UIComponentTag#setProperties(javax.faces.component.UIComponent)
    */
   protected void setProperties(UIComponent component)
   {
      super.setProperties(component);
      
      setStringStaticProperty(component, "availableOptionsSize", this.availableOptionsSize);
      setStringProperty(component, "associationName", this.associationName);
      setStringProperty(component, "selectItemMsg", this.selectItemMsg);
      setStringProperty(component, "selectItemsMsg", this.selectItemsMsg);
      setStringProperty(component, "selectedItemsMsg", this.selectedItemsMsg);
      setStringProperty(component, "noSelectedItemsMsg", this.noSelectedItemsMsg);
      setStringProperty(component, "value", this.value);
      setBooleanProperty(component, "disabled", this.disabled);
   }
   
   /**
    * @param value The value to set.
    */
   public void setValue(String value)
   {
      this.value = value;
   }
   
   /**
    * Sets the association name
    * 
    * @param associationName The association name
    */
   public void setAssociationName(String associationName)
   {
      this.associationName = associationName;
   }
   
   /**
    * @param availableOptionsSize Sets the size of the available options size when 
    *        multiple items can be selected
    */
   public void setAvailableOptionsSize(String availableOptionsSize)
   {
      this.availableOptionsSize = availableOptionsSize;
   }
   
   /**
    * Sets the message to display for the no selected items
    * 
    * @param noSelectedItemsMsg The message
    */
   public void setNoSelectedItemsMsg(String noSelectedItemsMsg)
   {
      this.noSelectedItemsMsg = noSelectedItemsMsg;
   }
   
   /**
    * Sets the message to display for the selected items
    * 
    * @param selectedItemsMsg The message
    */
   public void setSelectedItemsMsg(String selectedItemsMsg)
   {
      this.selectedItemsMsg = selectedItemsMsg;
   }

   /**
    * Sets the message to display for inviting the user to select an item
    * 
    * @param selectItemMsg The message
    */
   public void setSelectItemMsg(String selectItemMsg)
   {
      this.selectItemMsg = selectItemMsg;
   }
   
   /**
    * Sets the message to display for inviting the user to select items
    * 
    * @param selectItemsMsg The message
    */
   public void setSelectItemsMsg(String selectItemsMsg)
   {
      this.selectItemsMsg = selectItemsMsg;
   }
   
   /**
    * Sets whether the component should be rendered in a disabled state
    * 
    * @param disabled true to render the component in a disabled state
    */
   public void setDisabled(String disabled)
   {
      this.disabled = disabled;
   }

   /**
    * @see javax.faces.webapp.UIComponentTag#release()
    */
   public void release()
   {
      this.associationName = null;
      this.availableOptionsSize = null;
      this.selectItemMsg = null;
      this.selectItemsMsg = null;
      this.selectedItemsMsg = null;
      this.noSelectedItemsMsg = null;
      this.disabled = null;
      this.value = null;

      super.release();
   }
}
