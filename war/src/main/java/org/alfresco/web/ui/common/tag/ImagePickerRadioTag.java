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
 * Tag to combine the image picker component and radio renderer 
 * 
 * @author gavinc
 */
public class ImagePickerRadioTag extends HtmlComponentTag
{
   /** the labelStyle */
   private String labelStyle;

   /** the labelStyleClass */
   private String labelStyleClass;

   /** the spacing */
   private String spacing;

   /** the columns */
   private String columns;
   
   /** the label */
   private String label;

   /** the value */
   private String value;
   
   /** the image */
   private String image;
   
   /** the onclick handler */
   private String onclick;
   
   /** the name of the config section to lookup to get the icons */
   private String configSection;
   
   /**
    * @see javax.faces.webapp.UIComponentTag#getComponentType()
    */
   public String getComponentType()
   {
      return "org.alfresco.faces.ImagePicker";
   }

   /**
    * @see javax.faces.webapp.UIComponentTag#getRendererType()
    */
   public String getRendererType()
   {
      return "org.alfresco.faces.Radio";
   }
   
   /**
    * @see javax.faces.webapp.UIComponentTag#setProperties(javax.faces.component.UIComponent)
    */
   protected void setProperties(UIComponent component)
   {
      super.setProperties(component);
      setStringProperty(component, "labelStyle", this.labelStyle);
      setStringProperty(component, "labelStyleClass", this.labelStyleClass);
      setStringProperty(component, "label", this.label);
      setStringProperty(component, "value", this.value);
      setStringProperty(component, "image", this.image);
      setStringProperty(component, "onclick", this.onclick);
      setStringProperty(component, "configSection", this.configSection);
      setIntProperty(component, "spacing", this.spacing);
      setIntProperty(component, "columns", this.columns);
   }
   
   /**
    * @see javax.servlet.jsp.tagext.Tag#release()
    */
   public void release()
   {
      super.release();
      this.labelStyle = null;
      this.labelStyleClass = null;
      this.spacing = null;
      this.label = null;
      this.value = null;
      this.image = null;
      this.columns = null;
      this.onclick = null;
      this.configSection = null;
   }   

   /**
    * @return Returns the image.
    */
   public String getImage()
   {
      return image;
   }

   /**
    * @param image The image to set.
    */
   public void setImage(String image)
   {
      this.image = image;
   }

   /**
    * @return Returns the label.
    */
   public String getLabel()
   {
      return label;
   }

   /**
    * @param label The label to set.
    */
   public void setLabel(String label)
   {
      this.label = label;
   }

   /**
    * @return Returns the labelStyle.
    */
   public String getLabelStyle()
   {
      return labelStyle;
   }

   /**
    * @param labelStyle The labelStyle to set.
    */
   public void setLabelStyle(String labelStyle)
   {
      this.labelStyle = labelStyle;
   }

   /**
    * @return Returns the labelStyleClass.
    */
   public String getLabelStyleClass()
   {
      return labelStyleClass;
   }

   /**
    * @param labelStyleClass The labelStyleClass to set.
    */
   public void setLabelStyleClass(String labelStyleClass)
   {
      this.labelStyleClass = labelStyleClass;
   }

   /**
    * @return Returns the spacing.
    */
   public String getSpacing()
   {
      return spacing;
   }

   /**
    * @param spacing The spacing to set.
    */
   public void setSpacing(String spacing)
   {
      this.spacing = spacing;
   }

   /**
    * @return Returns the value.
    */
   public String getValue()
   {
      return value;
   }

   /**
    * @param value The value to set.
    */
   public void setValue(String value)
   {
      this.value = value;
   }

   /**
    * @return Returns the columns.
    */
   public String getColumns()
   {
      return columns;
   }
   
   /**
    * @param columns The columns to set.
    */
   public void setColumns(String columns)
   {
      this.columns = columns;
   }

   /**
    * @return Returns the onclick.
    */
   public String getOnclick()
   {
      return onclick;
   }

   /**
    * @param onclick The onclick to set.
    */
   public void setOnclick(String onclick)
   {
      this.onclick = onclick;
   }

   /**
    * @return Returns the config section to lookup
    */
   public String getConfigSection()
   {
      return this.configSection;
   }

   /**
    * @param configSection The config section to lookup
    */
   public void setConfigSection(String configSection)
   {
      this.configSection = configSection;
   }
}
