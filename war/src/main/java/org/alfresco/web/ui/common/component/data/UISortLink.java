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
package org.alfresco.web.ui.common.component.data;

import java.io.IOException;
import java.util.Map;

import javax.faces.component.NamingContainer;
import javax.faces.component.UICommand;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.el.ValueBinding;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ActionEvent;
import javax.faces.event.FacesEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.alfresco.web.data.IDataContainer;
import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.common.WebResources;

/**
 * @author Kevin Roast
 */
public class UISortLink extends UICommand
{
   /**
    * Default Constructor
    */
   public UISortLink()
   {
      setRendererType(null);
   }
   
   /**
    * @see javax.faces.component.UIComponent#encodeBegin(javax.faces.context.FacesContext)
    */
   public void encodeBegin(FacesContext context) throws IOException
   {
      ResponseWriter out = context.getResponseWriter();
      
      IDataContainer dataContainer = getDataContainer();
      if (dataContainer == null)
      {
         throw new IllegalStateException("Must nest UISortLink inside component implementing IDataContainer!"); 
      }
      
      // swap sort direction if we were last sorted column
      boolean bPreviouslySorted = false;
      boolean descending = true;
      String lastSortedColumn = dataContainer.getCurrentSortColumn();
      if (lastSortedColumn != null && lastSortedColumn.equals(getValue()))
      {
         descending = !dataContainer.isCurrentSortDescending();
         bPreviouslySorted = true;
      }
      
      // render sort link
      StringBuilder buf = new StringBuilder(256);
      buf.append("<nobr><a href='#' onclick=\"");
      // generate some JavaScript to set a hidden form field and submit
      // a form which request attributes that we can decode
      buf.append(Utils.generateFormSubmit(context, this, getHiddenFieldName(context), getClientId(context)));
      buf.append('"');
      
      if (getAttributes().get("style") != null)
      {
         buf.append(" style=\"")
            .append(getAttributes().get("style"))
            .append('"');
      }
      if (getAttributes().get("styleClass") != null)
      {
         buf.append(" class=")
            .append(getAttributes().get("styleClass"));
      }
      if (getAttributes().get("tooltip") != null)
      {
         buf.append(" title=\"")
            .append(getAttributes().get("tooltip"))
            .append('"');
      }
      buf.append('>');
      
      // output column label
      buf.append(Utils.encode((String)getAttributes().get("label")));
      
      if (bPreviouslySorted == true)
      {
         if (descending == true)
         {
            buf.append(" ")
               .append(Utils.buildImageTag(context, WebResources.IMAGE_SORTUP, 10, 6, null));
         }
         else
         {
            buf.append(" ")
               .append(Utils.buildImageTag(context, WebResources.IMAGE_SORTDOWN, 10, 6, null));
         }
      }
      else
      {
         buf.append(" ")
            .append(Utils.buildImageTag(context, WebResources.IMAGE_SORTNONE, 10, 7, null));
      }
      buf.append("</a></nobr>");
      
      out.write(buf.toString());
   }
   
   /**
    * @see javax.faces.component.UIComponent#decode(javax.faces.context.FacesContext)
    */
   public void decode(FacesContext context)
   {
      Map requestMap = context.getExternalContext().getRequestParameterMap();
      String fieldId = getHiddenFieldName(context);
      String value = (String)requestMap.get(fieldId);
      if (value != null && value.equals(getClientId(context)))
      {
         // we were clicked - queue an event to represent the click
         // cannot handle the event here as other components etc. have not had
         // a chance to decode() - we queue an event to be processed later
         SortEvent actionEvent = new SortEvent(this, (String)this.getValue());
         this.queueEvent(actionEvent);
      }
   }
   
   /**
    * @see javax.faces.component.UIComponent#broadcast(javax.faces.event.FacesEvent)
    */
   public void broadcast(FacesEvent event) throws AbortProcessingException
   {
      if (event instanceof SortEvent == false)
      {
         // let the super class handle events which we know nothing about
         super.broadcast(event);
      }
      else if ( ((SortEvent)event).Column.equals(getColumn()) )
      {
         // found a sort event for us!
         if (s_logger.isDebugEnabled())
            s_logger.debug("Handling sort event for column: " + ((SortEvent)event).Column);
         
         if (getColumn().equals(getDataContainer().getCurrentSortColumn()) == true)
         {
            // reverse sort direction
            this.descending = !this.descending;
         }
         else
         {
            // revert to default sort direction
            this.descending = true;
         }
         getDataContainer().sort(getColumn(), this.descending, getMode());
      }  
   }
   
   /**
    * We use a hidden field name based on the parent data container component Id and
    * the string "sort" to give a field name that can be shared by all sort links
    * within a single data container component.
    * 
    * @param context    FacesContext
    * 
    * @return hidden field name
    */
   private String getHiddenFieldName(FacesContext context)
   {
      UIComponent dataContainer = (UIComponent)Utils.getParentDataContainer(context, this);
      return dataContainer.getClientId(context) + NamingContainer.SEPARATOR_CHAR + "sort";
   }
   
   /**
    * Column name referenced by this link
    * 
    * @return column name
    */
   public String getColumn()
   {
      return (String)getValue();
   }
   
   /**
    * Sorting mode - see IDataContainer constants
    * 
    * @return sorting mode - see IDataContainer constants
    */
   public String getMode()
   {
      return this.mode;
   }
   
   /**
    * Set the sorting mode - see IDataContainer constants
    * 
    * @param sortMode      the sorting mode- see IDataContainer constants
    */
   public void setMode(String sortMode)
   {
      this.mode = sortMode;
   }
   
   /**
    * Returns true for descending sort, false for ascending
    * 
    * @return true for descending sort, false for ascending
    */
   public boolean isDescending()
   {
      return this.descending;
   }
   
   /**
    * @return Returns the label.
    */
   public String getLabel()
   {
      ValueBinding vb = getValueBinding("label");
      if (vb != null)
      {
         this.label = (String)vb.getValue(getFacesContext());
      }
      return this.label;
   }

   /**
    * @param label The label to set.
    */
   public void setLabel(String label)
   {
      this.label = label;
   }

   /**
    * @return Returns the tooltip.
    */
   public String getTooltip()
   {
      ValueBinding vb = getValueBinding("tooltip");
      if (vb != null)
      {
         this.tooltip = (String)vb.getValue(getFacesContext());
      }
      return this.tooltip;
   }

   /**
    * @param tooltip The tooltip to set.
    */
   public void setTooltip(String tooltip)
   {
      this.tooltip = tooltip;
   }

   /**
    * @see javax.faces.component.StateHolder#restoreState(javax.faces.context.FacesContext, java.lang.Object)
    */
   public void restoreState(FacesContext context, Object state)
   {
      Object values[] = (Object[])state;
      // standard component attributes are restored by the super class
      super.restoreState(context, values[0]);
      this.descending = ((Boolean)values[1]).booleanValue();
      this.mode = (String)values[2];
      this.label = (String)values[3];
      this.tooltip = (String)values[4];
   }
   
   /**
    * @see javax.faces.component.StateHolder#saveState(javax.faces.context.FacesContext)
    */
   public Object saveState(FacesContext context)
   {
      Object values[] = new Object[5];
      // standard component attributes are saved by the super class
      values[0] = super.saveState(context);
      values[1] = (this.descending ? Boolean.TRUE : Boolean.FALSE);
      values[2] = this.mode;
      values[3] = this.label;
      values[4] = this.tooltip;
      return values;
   }
   
   /**
    * Return the parent data container for this component
    */
   private IDataContainer getDataContainer()
   {
      return Utils.getParentDataContainer(getFacesContext(), this);
   }
   
   
   // ------------------------------------------------------------------------------
   // Inner classes
   
   /**
    * Class representing the clicking of a sortable column.
    */
   private static class SortEvent extends ActionEvent
   {
      public SortEvent(UIComponent component, String column)
      {
         super(component);
         Column = column;
      }
      
      public String Column = null;
   }
   
   
   // ------------------------------------------------------------------------------
   // Constants
   
   private static Log    s_logger = LogFactory.getLog(IDataContainer.class);
   
   /** sorting mode */
   private String mode = IDataContainer.SORT_CASEINSENSITIVE;
   
   private String label;
   
   private String tooltip;
   
   /** true for descending sort, false for ascending */
   private boolean descending = false;
}
