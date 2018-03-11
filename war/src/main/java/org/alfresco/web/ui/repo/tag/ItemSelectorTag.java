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
/*
 * Created on 25-May-2005
 */
package org.alfresco.web.ui.repo.tag;

import javax.faces.component.UIComponent;

import org.alfresco.web.ui.common.tag.HtmlComponentTag;

/**
 * Base class for the item selector tag
 * 
 * @author gavinc
 */
public abstract class ItemSelectorTag extends HtmlComponentTag
{
   /** the value */
   private String value;

   /** the label */
   private String label;

   /** the spacing */
   private String spacing;
   
   /** the node style */
   private String nodeStyle;
   
   /** the node style class */
   private String nodeStyleClass;
   
   /** the id of initial selection */
   private String initialSelection;
   
   /** Whether the component is disabled */
   private String disabled;
   
   /**
    * @see javax.faces.webapp.UIComponentTag#getComponentType()
    */
   public abstract String getComponentType();
   
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
      
      setStringBindingProperty(component, "value", this.value);
      setStringBindingProperty(component, "initialSelection", this.initialSelection);
      setStringProperty(component, "label", this.label);
      setStringProperty(component, "nodeStyle", this.nodeStyle);
      setStringProperty(component, "nodeStyleClass", this.nodeStyleClass);
      setIntProperty(component, "spacing", this.spacing);
      setBooleanProperty(component, "disabled", this.disabled);
   }
   
   /**
    * @see org.alfresco.web.ui.common.tag.HtmlComponentTag#release()
    */
   public void release()
   {
      super.release();
      
      this.value = null;
      this.label = null;
      this.spacing = null;
      this.nodeStyle = null;
      this.nodeStyleClass = null;
      this.initialSelection = null;
      this.disabled = null;
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
    * Set the label
    *
    * @param label     the label
    */
   public void setLabel(String label)
   {
      this.label = label;
   }

   /**
    * Set the spacing
    *
    * @param spacing     the spacing
    */
   public void setSpacing(String spacing)
   {
      this.spacing = spacing;
   }

   /**
    * Set the node style
    * 
    * @param nodeStyle  the node style
    */
   public void setNodeStyle(String nodeStyle)
   {
      this.nodeStyle = nodeStyle;
   }

   /**
    * Set the node style class
    * 
    * @param nodeStyleClass   the node style class
    */
   public void setNodeStyleClass(String nodeStyleClass)
   {
      this.nodeStyleClass = nodeStyleClass;
   }
   
   /**
    * Sets the id of the item to be initially selected, this is overridden
    * however if a value is supplied
    * 
    * @param initialSelection The id of the initial selected item
    */
   public void setInitialSelection(String initialSelection)
   {
      this.initialSelection = initialSelection;
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
}
