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
 * Tag to represent the combination of a PropertySheet component
 * and a Grid renderer
 * 
 * @author gavinc
 */
public class PropertySheetGridTag extends BaseComponentTag
{
   private String value;
   private String var;
   private String columns;
   private String externalConfig;
   private String configArea;
   private String readOnly;
   private String mode;
   private String validationEnabled;
   private String labelStyleClass;
   private String cellpadding;
   private String cellspacing;
   private String finishButtonId;
   private String nextButtonId;
   
   /**
    * @see javax.faces.webapp.UIComponentTag#getComponentType()
    */
   public String getComponentType()
   {
      return "org.alfresco.faces.PropertySheet";
   }

   /**
    * @see javax.faces.webapp.UIComponentTag#getRendererType()
    */
   public String getRendererType()
   {
      return "javax.faces.Grid";
   }
   
   /**
    * @param value The value to set.
    */
   public void setValue(String value)
   {
      this.value = value;
   }
   
   /**
    * @param var The var to set.
    */
   public void setVar(String var)
   {
      this.var = var;
   }

   /**
    * @param columns The columns to set.
    */
   public void setColumns(String columns)
   {
      this.columns = columns;
   }

   /**
    * @param externalConfig The externalConfig to set.
    */
   public void setExternalConfig(String externalConfig)
   {
      this.externalConfig = externalConfig;
   }
   
   /**
    * @param configArea Sets the named config area to use
    */
   public void setConfigArea(String configArea)
   {
      this.configArea = configArea;
   }

   /**
    * @param mode The mode, either "edit" or "view"
    */
   public void setMode(String mode)
   {
      this.mode = mode;
   }

   /**
    * @param readOnly The readOnly to set.
    */
   public void setReadOnly(String readOnly)
   {
      this.readOnly = readOnly;
   }
   
   /**
    * @param validationEnabled The validationEnabled to set.
    */
   public void setValidationEnabled(String validationEnabled)
   {
      this.validationEnabled = validationEnabled;
   }
   
   /**
    * @param labelStyleClass Sets the style class for the label column
    */
   public void setLabelStyleClass(String labelStyleClass)
   {
      this.labelStyleClass = labelStyleClass;
   }

   /**
    * @param cellpadding Sets the cellpadding for the grid
    */
   public void setCellpadding(String cellpadding)
   {
      this.cellpadding = cellpadding;
   }

   /**
    * @param cellspacing Sets the cellspacing for the grid
    */
   public void setCellspacing(String cellspacing)
   {
      this.cellspacing = cellspacing;
   }

   /**
    * @param nextButtonId Sets the next button id
    */
   public void setNextButtonId(String nextButtonId)
   {
      this.nextButtonId = nextButtonId;
   }
   
   /**
    * @param finishButtonId Sets the finish button id
    */
   public void setFinishButtonId(String finishButtonId)
   {
      this.finishButtonId = finishButtonId;
   }

   /**
    * @see javax.faces.webapp.UIComponentTag#setProperties(javax.faces.component.UIComponent)
    */
   protected void setProperties(UIComponent component)
   {
      super.setProperties(component);
      
      setStringProperty(component, "value", this.value);
      setStringProperty(component, "mode", this.mode);
      setStringProperty(component, "configArea", this.configArea);
      setStringStaticProperty(component, "var", this.var);
      setIntProperty(component, "columns", this.columns);
      setStringStaticProperty(component, "labelStyleClass", this.labelStyleClass);
      setBooleanProperty(component, "externalConfig", this.externalConfig);
      setBooleanProperty(component, "readOnly", this.readOnly);
      setBooleanProperty(component, "validationEnabled", this.validationEnabled);
      setStringStaticProperty(component, "cellpadding", this.cellpadding);
      setStringStaticProperty(component, "cellspacing", this.cellspacing);
      setStringStaticProperty(component, "finishButtonId", this.finishButtonId);
      setStringStaticProperty(component, "nextButtonId", this.nextButtonId);
   }

   /**
    * @see javax.faces.webapp.UIComponentTag#release()
    */
   public void release()
   {
      this.value = null;
      this.var = null;
      this.columns = null;
      this.externalConfig = null;
      this.configArea = null;
      this.readOnly = null;
      this.mode = null;
      this.validationEnabled = null;
      this.labelStyleClass = null;
      this.cellpadding = null;
      this.cellspacing = null;
      this.finishButtonId = null;
      this.nextButtonId = null;
      
      super.release();
   }
}
