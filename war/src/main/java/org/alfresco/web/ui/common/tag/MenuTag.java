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

/**
 * @author kevinr
 */
public class MenuTag extends HtmlComponentTag
{
   /**
    * @see javax.faces.webapp.UIComponentTag#getComponentType()
    */
   public String getComponentType()
   {
      return "org.alfresco.faces.Menu";
   }

   /**
    * @see javax.faces.webapp.UIComponentTag#getRendererType()
    */
   public String getRendererType()
   {
      // the component is self renderering
      return null;
   }

   /**
    * @see javax.faces.webapp.UIComponentTag#setProperties(javax.faces.component.UIComponent)
    */
   protected void setProperties(UIComponent component)
   {
      super.setProperties(component);
      setStringProperty(component, "label", this.label);
      setStringProperty(component, "image", this.image);
      setStringProperty(component, "menuStyle", this.menuStyle);
      setStringProperty(component, "menuStyleClass", this.menuStyleClass);
      setIntProperty(component, "itemSpacing", this.itemSpacing);
   }
   
   /**
    * @see javax.servlet.jsp.tagext.Tag#release()
    */
   public void release()
   {
      super.release();
      this.label = null;
      this.image = null;
      this.menuStyle = null;
      this.menuStyleClass = null;
      this.itemSpacing = null;
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
    * Set the image
    *
    * @param image     the image
    */
   public void setImage(String image)
   {
      this.image = image;
   }
   
   /**
    * Set the menuStyle
    *
    * @param menuStyle     the menuStyle
    */
   public void setMenuStyle(String menuStyle)
   {
      this.menuStyle = menuStyle;
   }

   /**
    * Set the menuStyleClass
    *
    * @param menuStyleClass     the menuStyleClass
    */
   public void setMenuStyleClass(String menuStyleClass)
   {
      this.menuStyleClass = menuStyleClass;
   }

   /**
    * Set the itemSpacing
    *
    * @param itemSpacing     the itemSpacing
    */
   public void setItemSpacing(String itemSpacing)
   {
      this.itemSpacing = itemSpacing;
   }


   /** the menuStyle */
   private String menuStyle;

   /** the menuStyleClass */
   private String menuStyleClass;

   /** the itemSpacing */
   private String itemSpacing;

   /** the image */
   private String image;

   /** the label */
   private String label;
}
