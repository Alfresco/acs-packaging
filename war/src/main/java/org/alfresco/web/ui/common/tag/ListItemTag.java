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

import javax.faces.component.UIComponent;

import org.springframework.extensions.webscripts.ui.common.tag.BaseComponentTag;

/**
 * @author kevinr
 */
public class ListItemTag extends BaseComponentTag
{
   /**
    * @see javax.faces.webapp.UIComponentTag#getComponentType()
    */
   public String getComponentType()
   {
      return "org.alfresco.faces.ListItem";
   }

   /**
    * @see javax.faces.webapp.UIComponentTag#getRendererType()
    */
   public String getRendererType()
   {
      // this component is rendered by its parent container
      return null;
   }

   /**
    * @see javax.faces.webapp.UIComponentTag#setProperties(javax.faces.component.UIComponent)
    */
   protected void setProperties(UIComponent component)
   {
      super.setProperties(component);
      setStringProperty(component, "tooltip", this.tooltip);
      setStringProperty(component, "label", this.label);
      setStringProperty(component, "description", this.description);
      setStringProperty(component, "image", this.image);
      setStringProperty(component, "value", this.value);
      setBooleanProperty(component, "disabled", this.disabled);
   }
   
   /**
    * @see javax.servlet.jsp.tagext.Tag#release()
    */
   public void release()
   {
      super.release();
      this.tooltip = null;
      this.label = null;
      this.description = null;
      this.image = null;
      this.value = null;
      this.disabled = null;
   }
   
   /**
    * Set the tooltip
    *
    * @param tooltip     the tooltip
    */
   public void setTooltip(String tooltip)
   {
      this.tooltip = tooltip;
   }

   /**
    * Set the label
    *
    * @param label     the label
    */
   public void setLabel(String label)
   {
      this.label = label;
   }
   
   /**
    * Set the description
    *
    * @param description     the description
    */
   public void setDescription(String description)
   {
      this.description = description;
   }

   /**
    * Set the image
    *
    * @param image     the image
    */
   public void setImage(String image)
   {
      this.image = image;
   }

   /**
    * Set the value to be selected initially 
    *
    * @param value     the value to be selected initially
    */
   public void setValue(String value)
   {
      this.value = value;
   }
   
   /**
    * Set the disabled flag
    * 
    * @param disabled true to set this item as disabled
    */
   public void setDisabled(String disabled)
   {
      this.disabled = disabled;
   }

   /** the tooltip */
   private String tooltip;

   /** the label */
   private String label;

   /** the image */
   private String image;

   /** the value to be selected initially */
   private String value;
   
   /** the disabled flag */
   private String disabled;
   
   /** the description */
   private String description;
}
