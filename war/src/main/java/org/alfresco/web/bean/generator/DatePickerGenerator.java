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
package org.alfresco.web.bean.generator;

import java.util.Calendar;

import javax.faces.component.UIComponent;
import javax.faces.component.UIOutput;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;

import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.web.app.Application;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.ui.common.ComponentConstants;
import org.alfresco.web.ui.common.converter.XMLDateConverter;
import org.alfresco.web.ui.repo.RepoConstants;
import org.alfresco.web.ui.repo.component.UIMultiValueEditor;
import org.alfresco.web.ui.repo.component.property.PropertySheetItem;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;

/**
 * Generates a date picker component.
 * 
 * @author gavinc
 */
public class DatePickerGenerator extends BaseComponentGenerator
{
   private boolean initialiseIfNull = false;
   private int yearCount = 30;
   private int startYear = Calendar.getInstance().get(Calendar.YEAR) + 2;
   private String noneLabel = null;
   
   private static final String MSG_DATE = "date_pattern";
   
   /**
    * @return Returns the year to start counting back from
    */
   public int getStartYear()
   {
      return startYear;
   }

   /**
    * @param startYear Sets the year to start counting back from
    */
   public void setStartYear(int startYear)
   {
      this.startYear = startYear;
   }

   /**
    * @return Returns the number of years to show
    */
   public int getYearCount()
   {
      return yearCount;
   }

   /**
    * @param yearCount Sets the number of years to show
    */
   public void setYearCount(int yearCount)
   {
      this.yearCount = yearCount;
   }
   
   /**
    * @return Determines whether the control should initially show
    *         today's date if the model value is null
    */
   public boolean isInitialiseIfNull()
   {
      return initialiseIfNull;
   }

   /**
    * @param initialiseIfNull Determines whether the control should 
    *        initially show today's date if the model value is null.
    *        This will also hide the None button thus disallowing
    *        the user to set the date back to null.
    */
   public void setInitialiseIfNull(boolean initialiseIfNull)
   {
      this.initialiseIfNull = initialiseIfNull;
   }

   /**
    * Returns the label to use when there is no date set.
    * 
    * @return The 'None' label to use
    */
   public String getNoneLabel()
   {
      return this.noneLabel;
   }

   /**
    * Sets the label to use when there is no date set.
    * Setting this to null will use the default label.
    * 
    * @param noneLabel The 'None' label 
    */
   public void setNoneLabel(String noneLabel)
   {
      this.noneLabel = noneLabel;
   }

   @SuppressWarnings("unchecked")
   public UIComponent generate(FacesContext context, String id)
   {
      UIComponent component = context.getApplication().
            createComponent(ComponentConstants.JAVAX_FACES_INPUT);
      component.setRendererType(RepoConstants.ALFRESCO_FACES_DATE_PICKER_RENDERER);
      FacesHelper.setupComponentId(context, component, id);
      component.getAttributes().put("startYear", this.startYear);
      component.getAttributes().put("yearCount", this.yearCount);
      component.getAttributes().put("initialiseIfNull", new Boolean(this.initialiseIfNull));
      component.getAttributes().put("style", "margin-right: 7px;");
      if (this.noneLabel != null)
      {
         component.getAttributes().put("noneLabel", this.noneLabel);
      }
      
      return component;
   }

   @Override
   protected void setupConverter(FacesContext context, 
         UIPropertySheet propertySheet, PropertySheetItem property, 
         PropertyDefinition propertyDef, UIComponent component)
   {
      if (property.getConverter() != null)
      {
         // create and add the custom converter
         createAndSetConverter(context, property.getConverter(), component);
      }
      else
      {
         // use the default converter for the date component
         // we can cast this as we know it is an UIOutput type
         ((UIOutput)component).setConverter(getDefaultConverter(context));
      }
   }

   @Override
   protected void setupMandatoryValidation(FacesContext context, 
         UIPropertySheet propertySheet, PropertySheetItem item, 
         UIComponent component, boolean realTimeChecking, String idSuffix)
   {
      if (component instanceof UIMultiValueEditor)
      {
         // Override the setup of the mandatory validation 
         // so we can send the _current_value id suffix.
         // We also enable real time so the page load
         // check disables the ok button if necessary, as the user
         // adds or removes items from the multi value list the 
         // page will be refreshed and therefore re-check the status.
         
         super.setupMandatoryValidation(context, propertySheet, item, 
               component, true, "_current_value");
      }
      else
      {
         // setup the client validation rule with real time validation enabled
         // so that the initial page load checks the state of the date
         super.setupMandatoryValidation(context, propertySheet, item, 
               component, true, idSuffix);
      }
      
   }
   
   /**
    * Retrieves the default converter for the date component
    * 
    * @param context FacesContext
    * @return XMLDateConverter
    */
   protected Converter getDefaultConverter(FacesContext context)
   {
      XMLDateConverter converter = (XMLDateConverter)context.getApplication().
            createConverter(RepoConstants.ALFRESCO_FACES_XMLDATE_CONVERTER);
      converter.setType("date");
      converter.setPattern(Application.getMessage(context, MSG_DATE));
      return converter;
   }
}
