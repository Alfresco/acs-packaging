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
import java.util.ArrayList;
import java.util.List;

import javax.faces.component.NamingContainer;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.UIOutput;
import javax.faces.component.UIPanel;
import javax.faces.component.UISelectBoolean;
import javax.faces.component.UISelectItems;
import javax.faces.component.UISelectOne;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.el.ValueBinding;
import javax.faces.model.SelectItem;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.dictionary.constraint.ListOfValuesConstraint;
import org.alfresco.service.cmr.dictionary.AspectDefinition;
import org.alfresco.service.cmr.dictionary.Constraint;
import org.alfresco.service.cmr.dictionary.ConstraintDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryException;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.dictionary.TypeDefinition;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.app.Application;
import org.alfresco.web.bean.repository.DataDictionary;
import org.alfresco.web.bean.repository.Repository;
import org.alfresco.web.config.AdvancedSearchConfigElement;
import org.alfresco.web.config.AdvancedSearchConfigElement.CustomProperty;
import org.alfresco.web.ui.common.ComponentConstants;
import org.alfresco.web.ui.common.Utils;
import org.springframework.extensions.webscripts.ui.common.component.SelfRenderingComponent;
import org.alfresco.web.ui.repo.RepoConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Kevin Roast
 */
public class UISearchCustomProperties extends SelfRenderingComponent implements NamingContainer
{
   public static final String PREFIX_DATE_TO    = "to_";
   public static final String PREFIX_DATE_FROM  = "from_";
   public static final String PREFIX_LOV_ITEM   = "item_";
   
   private static final String VALUE = "value";
   
   private static final String MSG_TO   = "to";
   private static final String MSG_FROM = "from";
   
   private static Log logger = LogFactory.getLog(UISearchCustomProperties.class);
   
   private DataDictionary dataDictionary;

   // ------------------------------------------------------------------------------
   // Component implementation
   
   /**
    * @see javax.faces.component.UIComponent#getFamily()
    */
   public String getFamily()
   {
      return "org.alfresco.faces.AdvancedSearch";
   }

   /**
    * @see javax.faces.component.UIComponentBase#encodeBegin(javax.faces.context.FacesContext)
    */
   @SuppressWarnings("unchecked")
   public void encodeBegin(FacesContext context) throws IOException
   {
      if (isRendered() == false)
      {
         return;
      }
      
      ResponseWriter out = context.getResponseWriter();
      
      if (getChildCount() == 0)
      {
         createComponentsFromConfig(context);
      }
      
      // encode the components in a 3 column table
      out.write("<table cellspacing=2 cellpadding=2 border=0");
      outputAttribute(out, getAttributes().get("styleClass"), "class");
      outputAttribute(out, getAttributes().get("style"), "style");
      out.write('>');
      List<UIComponent> children = getChildren();
      int colCounter = 0;
      for (int i=0; i<children.size(); i++)
      {
         UIComponent component = children.get(i);
         if (component instanceof UIPanel)
         {
            out.write("<tr><td colspan=3>");
            Utils.encodeRecursive(context, component);
            out.write("</td></tr>");
            colCounter += 3;
         }
         else
         {
            if ((colCounter % 3) == 0)
            {
               out.write("<tr>");
            }
            out.write("<td>");
            Utils.encodeRecursive(context, component);
            out.write("</td>");
            if ((colCounter % 3 ) == 2)
            {
               out.write("</tr>");
            }
            colCounter++;
         }
      }
      out.write("</table>");
   }
   
   /**
    * Build the components from the Advanced Search config entries
    * 
    * @param context FacesContext
    */
   @SuppressWarnings("unchecked")
   private void createComponentsFromConfig(FacesContext context)
   {
      DictionaryService dd = Repository.getServiceRegistry(context).getDictionaryService();
      AdvancedSearchConfigElement config = (AdvancedSearchConfigElement)Application.getConfigService(
            context).getConfig("Advanced Search").getConfigElement(AdvancedSearchConfigElement.CONFIG_ELEMENT_ID);
      
      // create an appropriate component for each custom property
      // using the DataDictionary to look-up labels and value types
      String beanBinding = (String)getAttributes().get("bean") + '.' + (String)getAttributes().get("var");
      List<CustomProperty> props = config.getCustomProperties();
      if (props != null)
      {
         for (CustomProperty property : props)
         {
            try
            {
               // try to find the Property definition for the specified Type or Aspect
               PropertyDefinition propDef = null;
               if (property.Type != null)
               {
                  QName type = Repository.resolveToQName(property.Type);
                  TypeDefinition typeDef = dd.getType(type);
                  if (typeDef == null)
                  {
                     logger.warn("No Type Definition found for: " + property.Type + " - Was an Aspect expected?");
                     continue;
                  }
                  propDef = typeDef.getProperties().get(Repository.resolveToQName(property.Property));
               }
               else if (property.Aspect != null)
               {
                  QName aspect = Repository.resolveToQName(property.Aspect);
                  AspectDefinition aspectDef = dd.getAspect(aspect);
                  if (aspectDef == null)
                  {
                     logger.warn("No Aspect Definition found for: " + property.Aspect + " - Was a Type expected?");
                     continue;
                  }
                  propDef = aspectDef.getProperties().get(Repository.resolveToQName(property.Property));
               }
               
               // if we found a def, then we can build components to represent it
               if (propDef != null)
               {
                  // resolve display label I18N message
                  String label;
                  if (property.LabelId != null && property.LabelId.length() != 0)
                  {
                     label = Application.getMessage(context, property.LabelId);
                  }
                  else
                  {
                     // or use dictionary label or QName as last resort
                     label = propDef.getTitle(dd) != null ? propDef.getTitle(dd) : propDef.getName().getLocalName();
                  }
                  
                  // special handling for Date and DateTime
                  DataTypeDefinition dataTypeDef = propDef.getDataType();
                  if (DataTypeDefinition.DATE.equals(dataTypeDef.getName()) || DataTypeDefinition.DATETIME.equals(dataTypeDef.getName()))
                  {
                     getChildren().add( generateControl(context, propDef, label, beanBinding) );
                  }
                  else
                  {
                     // add ListOfValues constraint components
                     ListOfValuesConstraint constraint = getListOfValuesConstraint(propDef);
                     if (constraint != null && propDef != null && propDef.isProtected() == false)
                     {
                        getChildren().add( generateCheck(context, propDef, beanBinding) );
                        getChildren().add( generateLabel(context, label + ": ") );
                     }
                     else
                     {
                        getChildren().add( generateLabel(context, "" ) );
                        getChildren().add( generateLabel(context, label + ": ") );
                     }
                     
                     getChildren().add( generateControl(context, propDef, null, beanBinding) );
                  }
               }
            }
            catch (DictionaryException ddErr)
            {
               logger.warn("Error building custom properties for Advanced Search: " + ddErr.getMessage());
            }
         }
      }
   }

   /**
    * Generates a JSF OutputText component/renderer
    * 
    * @param context JSF context
    * @param propDef PropertyDefinition
    * @param beanBinding String
    * 
    * @return UIComponent
    */
   private UIComponent generateCheck(FacesContext context, PropertyDefinition propDef, String beanBinding)
   {
		// enabled state checkbox
		UIInput checkbox = (UIInput)context.getApplication().createComponent(ComponentConstants.JAVAX_FACES_SELECT_BOOLEAN);
		checkbox.setRendererType(ComponentConstants.JAVAX_FACES_CHECKBOX);
		checkbox.setId(context.getViewRoot().createUniqueId());
		ValueBinding vbCheckbox = context.getApplication().createValueBinding(
		   "#{" + beanBinding + "[\"" + propDef.getName().toString() + "\"]}");
		checkbox.setValueBinding(VALUE, vbCheckbox);

		return checkbox;
   }
   
   /**
    * Generates a JSF OutputText component/renderer
    * 
    * @param context JSF context
    * @param displayLabel The display label text
    * 
    * @return UIComponent
    */
   private UIComponent generateLabel(FacesContext context, String displayLabel)
   {
      UIOutput label = (UIOutput)context.getApplication().createComponent(ComponentConstants.JAVAX_FACES_OUTPUT);
      label.setId(context.getViewRoot().createUniqueId());
      label.setRendererType(ComponentConstants.JAVAX_FACES_TEXT);
      label.setValue(displayLabel);
      return label;
   }
      
   /**
    * Generates an appropriate control for the given property
    * 
    * @param context       JSF context
    * @param propDef       The definition of the property to create the control for
    * @param displayLabel  Display label for the component
    * @param beanBinding   Combined name of the value bound bean and variable used for value binding expression
    * 
    * @return UIComponent
    */
   @SuppressWarnings("unchecked")
   private UIComponent generateControl(FacesContext context, PropertyDefinition propDef, String displayLabel, String beanBinding)
   {
      UIComponent control = null;
      
      DataTypeDefinition dataTypeDef = propDef.getDataType();
      QName typeName = dataTypeDef.getName();
      
      javax.faces.application.Application facesApp = context.getApplication();
      
      // create default value binding to a Map of values with a defined name
      ValueBinding vb = facesApp.createValueBinding(
            "#{" + beanBinding + "[\"" + propDef.getName().toString() + "\"]}");
      
      // generate the appropriate input field
      if (typeName.equals(DataTypeDefinition.BOOLEAN))
      {
         control = (UISelectBoolean)facesApp.createComponent(ComponentConstants.JAVAX_FACES_SELECT_BOOLEAN);
         control.setRendererType(ComponentConstants.JAVAX_FACES_CHECKBOX);
         control.setValueBinding(VALUE, vb);
      }
      else if (typeName.equals(DataTypeDefinition.CATEGORY))
      {
         control = (UICategorySelector)facesApp.createComponent(RepoConstants.ALFRESCO_FACES_TAG_SELECTOR);
         control.setValueBinding(VALUE, vb);
      }
      else if (typeName.equals(DataTypeDefinition.DATETIME) || typeName.equals(DataTypeDefinition.DATE))
      {
         Boolean showTime = Boolean.valueOf(typeName.equals(DataTypeDefinition.DATETIME));
         
         // create value bindings for the start year and year count attributes
         ValueBinding startYearBind = null;
         ValueBinding yearCountBind = null;
         
         if (showTime)
         {
            startYearBind = facesApp.createValueBinding("#{DateTimePickerGenerator.startYear}");
            yearCountBind = facesApp.createValueBinding("#{DateTimePickerGenerator.yearCount}");
         }
         else
         {
            startYearBind = facesApp.createValueBinding("#{DatePickerGenerator.startYear}");
            yearCountBind = facesApp.createValueBinding("#{DatePickerGenerator.yearCount}");
         }
         
         
         // Need to output component for From and To date selectors and labels
         // also neeed checkbox for enable/disable state - requires an outer wrapper component
         control = (UIPanel)facesApp.createComponent(ComponentConstants.JAVAX_FACES_PANEL);
         control.setRendererType(ComponentConstants.JAVAX_FACES_GRID);
         control.getAttributes().put("columns", Integer.valueOf(2));
         
         // enabled state checkbox
         UIInput checkbox = (UIInput)facesApp.createComponent(ComponentConstants.JAVAX_FACES_SELECT_BOOLEAN);
         checkbox.setRendererType(ComponentConstants.JAVAX_FACES_CHECKBOX);
         checkbox.setId(context.getViewRoot().createUniqueId());
         ValueBinding vbCheckbox = facesApp.createValueBinding(
            "#{" + beanBinding + "[\"" + propDef.getName().toString() + "\"]}");
         checkbox.setValueBinding(VALUE, vbCheckbox);
         control.getChildren().add(checkbox);
         
         // main display label
         UIOutput label = (UIOutput)context.getApplication().createComponent(ComponentConstants.JAVAX_FACES_OUTPUT);
         label.setId(context.getViewRoot().createUniqueId());
         label.setRendererType(ComponentConstants.JAVAX_FACES_TEXT);
         label.setValue(displayLabel + ":");
         control.getChildren().add(label);
         
         // from date label
         UIOutput labelFromDate = (UIOutput)context.getApplication().createComponent(ComponentConstants.JAVAX_FACES_OUTPUT);
         labelFromDate.setId(context.getViewRoot().createUniqueId());
         labelFromDate.setRendererType(ComponentConstants.JAVAX_FACES_TEXT);
         labelFromDate.setValue(Application.getMessage(context, MSG_FROM));
         control.getChildren().add(labelFromDate);
         
         // from date control
         UIInput inputFromDate = (UIInput)facesApp.createComponent(ComponentConstants.JAVAX_FACES_INPUT);
         inputFromDate.setId(context.getViewRoot().createUniqueId());
         inputFromDate.setRendererType(RepoConstants.ALFRESCO_FACES_DATE_PICKER_RENDERER);
         inputFromDate.setValueBinding("startYear", startYearBind);
         inputFromDate.setValueBinding("yearCount", yearCountBind);
         inputFromDate.getAttributes().put("initialiseIfNull", Boolean.TRUE);
         inputFromDate.getAttributes().put("showTime", showTime);
         ValueBinding vbFromDate = facesApp.createValueBinding(
            "#{" + beanBinding + "[\"" + PREFIX_DATE_FROM + propDef.getName().toString() + "\"]}");
         inputFromDate.setValueBinding(VALUE, vbFromDate);
         control.getChildren().add(inputFromDate);
         
         // to date label
         UIOutput labelToDate = (UIOutput)context.getApplication().createComponent(ComponentConstants.JAVAX_FACES_OUTPUT);
         labelToDate.setId(context.getViewRoot().createUniqueId());
         labelToDate.setRendererType(ComponentConstants.JAVAX_FACES_TEXT);
         labelToDate.setValue(Application.getMessage(context, MSG_TO));
         control.getChildren().add(labelToDate);
         
         // to date control
         UIInput inputToDate = (UIInput)facesApp.createComponent(ComponentConstants.JAVAX_FACES_INPUT);
         inputToDate.setId(context.getViewRoot().createUniqueId());
         inputToDate.setRendererType(RepoConstants.ALFRESCO_FACES_DATE_PICKER_RENDERER);
         inputToDate.setValueBinding("startYear", startYearBind);
         inputToDate.setValueBinding("yearCount", yearCountBind);
         inputToDate.getAttributes().put("initialiseIfNull", Boolean.TRUE);
         inputToDate.getAttributes().put("showTime", showTime);
         ValueBinding vbToDate = facesApp.createValueBinding(
            "#{" + beanBinding + "[\"" + PREFIX_DATE_TO + propDef.getName().toString() + "\"]}");
         inputToDate.setValueBinding(VALUE, vbToDate);
         control.getChildren().add(inputToDate);
      }
      else if (typeName.equals(DataTypeDefinition.NODE_REF))
      {
         control = (UISpaceSelector)facesApp.createComponent(RepoConstants.ALFRESCO_FACES_SPACE_SELECTOR);
         control.setValueBinding(VALUE, vb);
      }
      else
      {
         ListOfValuesConstraint constraint = getListOfValuesConstraint(propDef);
         if (constraint != null && propDef != null && propDef.isProtected() == false)
         {
            control = (UISelectOne)facesApp.createComponent(UISelectOne.COMPONENT_TYPE);
            
            UISelectItems itemsComponent = (UISelectItems)facesApp.
            createComponent(ComponentConstants.JAVAX_FACES_SELECT_ITEMS);

            List<SelectItem> items = new ArrayList<SelectItem>();
            List<String> values = constraint.getAllowedValues();
            for (String value : values)
            {
               items.add(new SelectItem(value, value));
            }

            itemsComponent.setValue(items);

            // add the items as a child component
            control.getChildren().add(itemsComponent);
            ValueBinding vbItemList = facesApp.createValueBinding(
                  "#{" + beanBinding + "[\"" + PREFIX_LOV_ITEM + propDef.getName().toString() + "\"]}");
            control.setValueBinding(VALUE, vbItemList);
         }
         else
         {
            // any other type is represented as an input text field    	  
            control = (UIInput)facesApp.createComponent(ComponentConstants.JAVAX_FACES_INPUT);
            control.setRendererType(ComponentConstants.JAVAX_FACES_TEXT);
            control.setValueBinding("size", facesApp.createValueBinding("#{TextFieldGenerator.size}"));
            control.setValueBinding("maxlength", facesApp.createValueBinding("#{TextFieldGenerator.maxLength}"));
            control.setValueBinding(VALUE, vb);
         }
      }
      // set up the common aspects of the control
      control.setId(context.getViewRoot().createUniqueId());

      return control;
   }

   /**
    * Retrieves the list of values constraint for the item, if it has one
    * 
    * @param propertyDef The property definition for the constraint
    * @return The constraint if the item has one, null otherwise
    */
   protected ListOfValuesConstraint getListOfValuesConstraint(PropertyDefinition propertyDef)
   {
      ListOfValuesConstraint lovConstraint = null;
      
      // get the property definition for the item
      if (propertyDef != null)
      {
         // go through the constaints and see if it has the
         // list of values constraint
         List<ConstraintDefinition> constraints = propertyDef.getConstraints();
         for (ConstraintDefinition constraintDef : constraints)
         {
            Constraint constraint = constraintDef.getConstraint();

            if (constraint instanceof ListOfValuesConstraint)
            {
               lovConstraint = (ListOfValuesConstraint)constraint;
               break;
            }
         }
      }
      
      return lovConstraint;
   }
}
