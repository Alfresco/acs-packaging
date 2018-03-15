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
package org.alfresco.web.ui.common.component;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.faces.component.NamingContainer;
import javax.faces.component.UICommand;
import javax.faces.component.UIComponent;
import javax.faces.component.UIForm;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.el.ValueBinding;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.FacesEvent;
import javax.faces.event.FacesListener;
import javax.faces.event.PhaseId;

import org.alfresco.web.ui.common.Utils;

/**
 * The SelectList component displays a graphical list of items, each with a label and icon image.
 * The list has three selection modes; single select (radio), multi-select (checkbox) and active
 * selection mode (child action components).
 * 
 * The value for the component is collection of UIListItem objects or a UIListItems instance.
 * 
 * For passive single and multi-select modes, the selected value(s) can be retrieved from the component.
 * For active selection mode, appropriate child components such as Command buttons or Action Links
 * will be rendered for each item in the list, data-binding to the specified 'var' variable should be
 * used to bind required params. It is then up to the developer to retrieve the selected item param
 * from the actionListener of the appropriate child component.
 * 
 * @author Kevin Roast
 */
public class UISelectList extends UIInput implements NamingContainer
{
   private Boolean multiSelect;
   private Boolean activeSelect;
   private int rowIndex = -1;
   private int itemCount;
   private String onchange = null;
   private Boolean escapeItemLabel;
   private Boolean escapeItemDescription;
   
   // ------------------------------------------------------------------------------
   // Component Impl 
   
   /**
    * Default constructor
    */
   public UISelectList()
   {
      setRendererType(null);
   }
   
   /**
    * @see javax.faces.component.UIComponent#getFamily()
    */
   public String getFamily()
   {
      return "org.alfresco.faces.Controls";
   }
   
   /**
    * @see javax.faces.component.StateHolder#restoreState(javax.faces.context.FacesContext, java.lang.Object)
    */
   public void restoreState(FacesContext context, Object state)
   {
      Object values[] = (Object[])state;
      // standard component attributes are restored by the super class
      super.restoreState(context, values[0]);
      this.multiSelect = (Boolean)values[1];
      this.activeSelect = (Boolean)values[2];
      this.itemCount = (Integer)values[3];
      this.onchange = (String)values[4];
      this.escapeItemLabel = (Boolean)values[5];
      this.escapeItemDescription = (Boolean)values[6];
   }
   
   /**
    * @see javax.faces.component.StateHolder#saveState(javax.faces.context.FacesContext)
    */
   public Object saveState(FacesContext context)
   {
      return new Object[] 
      { 
         super.saveState(context),
         this.multiSelect,
         this.activeSelect,
         this.itemCount,
         this.onchange,
         this.escapeItemLabel,
         this.escapeItemDescription
      };
   }
   
   /**
    * @return the client Id for this naming container component - based on the current row context.
    *         This allows a single component rendered multiple times in a list to dynamically base
    *         their ID on the current row - so that the 'correct' component is decoded and event is
    *         queued with the current row value. 
    */
   @Override
   public String getClientId(FacesContext context)
   {
      String clientId = super.getClientId(context);
      int rowIndex = getRowIndex();
      return (rowIndex == -1 ? clientId : clientId + "_" + rowIndex);
   }
   
   /**
    * Override the processing of child component decodes - we set the current row context so any
    * events queued by child components wrapped in FacesEventWrapper have current row value. 
    */
   @Override
   public void processDecodes(FacesContext context)
   {
      if (!isRendered())
      {
         return;
      }
      
      setRowIndex(-1);
      for (UIComponent child : (List<UIComponent>)this.getChildren())
      {
         if (child instanceof UIListItem == false && child instanceof UIListItems == false)
         {
            for (int i=0; i<this.itemCount; i++)
            {
               setRowIndex(i);
               child.processDecodes(context);
            }
         }
      }
      setRowIndex(-1);
      try
      {
         decode(context);
      }
      catch (RuntimeException e)
      {
         context.renderResponse();
         throw e;
      }
   }
   
   /**
    * Override event queueing from child components - wrap and add current row value
    */
   @Override
   public void queueEvent(FacesEvent event)
   {
      super.queueEvent(new FacesEventWrapper(event, getRowIndex(), this));
   }
   
   /**
    * Override event broadcasting to look for event wrappers to set the current row context
    * correctly for components that have been rendered multiple times in the list.
    */
   @Override
   public void broadcast(FacesEvent event) throws AbortProcessingException
   {
      if (event instanceof FacesEventWrapper)
      {
         FacesEvent originalEvent = ((FacesEventWrapper)event).getWrappedFacesEvent();
         int eventRowIndex = ((FacesEventWrapper)event).getRowIndex();
         int currentRowIndex = getRowIndex();
         setRowIndex(eventRowIndex);
         try
         {
            originalEvent.getComponent().broadcast(originalEvent);
         }
         finally
         {
            setRowIndex(currentRowIndex);
         }
      }
      else
      {
         super.broadcast(event);
      }
   }
   
   /**
    * @see javax.faces.component.UIComponentBase#decode(javax.faces.context.FacesContext)
    */
   public void decode(FacesContext context)
   {
      Map requestMap = context.getExternalContext().getRequestParameterMap();
      Map valuesMap = context.getExternalContext().getRequestParameterValuesMap();
      
      // save the selected values that match our component Id
      setSubmittedValue((String[])valuesMap.get(getClientId(context)));
   }
   
   /**
    * @see javax.faces.component.UIComponentBase#encodeChildren(javax.faces.context.FacesContext)
    */
   public void encodeChildren(FacesContext context) throws IOException
   {
      // we encode child components explicity
   }

   /**
    * @see javax.faces.component.UIComponentBase#getRendersChildren()
    */
   public boolean getRendersChildren()
   {
      return true;
   }

   /**
    * @see javax.faces.component.UIComponentBase#encodeBegin(javax.faces.context.FacesContext)
    */
   public void encodeBegin(FacesContext context) throws IOException
   {
      if (!isRendered())
      {
         return;
      }
      
      // Prepare the data-binding variable "var" ready for the each cycle of
      // renderering for the child components. 
      String var = (String)getAttributes().get("var");
      Map requestMap = context.getExternalContext().getRequestMap();
      
      ResponseWriter out = context.getResponseWriter();
      
      out.write("<table cellspacing=0 cellpadding=0");
      String style = (String)getAttributes().get("style");
      if (style != null)
      {
         out.write(" style='");
         out.write(style);
         out.write('\'');
      }
      String styleClass = (String)getAttributes().get("styleClass");
      if (styleClass != null)
      {
         out.write(" class=");
         out.write(styleClass);
      }
      out.write('>');
      
      // get the child components and look for compatible ListItem objects
      this.itemCount = 0;
      setRowIndex(-1);
      for (final UIComponent child : (List<UIComponent>)this.getChildren())
      {
         if (child instanceof UIListItems)
         {
            // get the value of the list items component and iterate through it's collection
            Object listItems = ((UIListItems)child).getValue();
            if (listItems instanceof Collection)
            {
               for (final UIListItem item : (Collection<UIListItem>)listItems)
               {
                  if (item.isRendered())
                  {
                     if (var != null)
                     {
                        requestMap.put(var, item);
                     }
                     setRowIndex(this.itemCount);
                     renderItem(context, out, item);
                  }
                  this.itemCount++;
               }
            }
         }
         else if (child instanceof UIListItem)
         {
            if (child.isRendered())
            {
               // found a valid UIListItem child to render
               UIListItem item = (UIListItem)child;
               if (var != null)
               {
                  requestMap.put(var, item);
               }
               setRowIndex(this.itemCount);
               renderItem(context, out, item);
            }
            this.itemCount++;
         }
      }
      setRowIndex(-1);
      if (var != null)
      {
         requestMap.remove(var);
      }
      
      out.write("</table>");
   }
   
   /**
    * Render a list item in the appropriate selection mode
    * 
    * @param context    FacesContext
    * @param out        ResponseWriter
    * @param item       UIListItem representing the item to render
    */
   private void renderItem(FacesContext context, ResponseWriter out, UIListItem item)
      throws IOException
   {
      boolean activeSelect = isActiveSelect();
      boolean escapeLabel = getEscapeItemLabel();
      boolean escapeDescription = getEscapeItemDescription();
      
      // begin the row, add tooltip if present
      String tooltip = item.getTooltip();
      out.write("<tr title=\"");
      out.write(tooltip != null ? tooltip : "");
      out.write("\">");
      
      if (activeSelect == false)
      {
         // we are rendering passive select list, so either multi or single selection using
         // checkboxes or radio button control respectively
         boolean multiSelect = isMultiSelect();
         String id = getClientId(context);
         String itemValue = item.getValue().toString();
         out.write("<td");
         Utils.outputAttribute(out, getAttributes().get("itemStyle"), "style");
         Utils.outputAttribute(out, getAttributes().get("itemStyleClass"), "class");
         out.write(" width=16><input type='");
         out.write(multiSelect ? "checkbox" : "radio");
         out.write("' name='");
         out.write(id);
         out.write("' id='");
         out.write(id);
         out.write("' value='");
         out.write(Utils.encode(itemValue));
         out.write("'");
         if (this.onchange != null)
         {
            out.write(" onchange='" + this.onchange + "'");
         }
         String[] value = (String[])getValue();
         if (multiSelect)
         {
            if (value != null)
            {
               for (int i=0; i<value.length; i++)
               {
                  if (value[i].equals(itemValue))
                  {
                     out.write(" CHECKED");
                     break;
                  }
               }
            }
         }
         else
         {
            if (value != null && value.length == 1 && value[0].equals(itemValue))
            {
               out.write(" CHECKED");
            }
         }
         out.write("></td>");
      }
      
      // optional 32x32 pixel icon
      String icon = item.getImage();
      if (icon != null)
      {
         out.write("<td");
         Utils.outputAttribute(out, getAttributes().get("itemStyle"), "style");
         Utils.outputAttribute(out, getAttributes().get("itemStyleClass"), "class");
         out.write(" width=34>");   // give pixel space around edges
         out.write(Utils.buildImageTag(context, icon, 32, 32, ""));
         out.write("</td>");
      }
      
      // label and description text
      String label = item.getLabel();
      String description = item.getDescription();
      out.write("<td width=100%");
      Utils.outputAttribute(out, getAttributes().get("itemStyle"), "style");
      Utils.outputAttribute(out, getAttributes().get("itemStyleClass"), "class");
      out.write("><div style='padding:2px'>");
      if (escapeLabel)
      {
         out.write(Utils.encode(label));
      }
      else
      {
         out.write(label);
      }
      out.write("</div>");
      if (description != null)
      {
         out.write("<div style='padding:2px'>");
         if (escapeDescription)
         {
            out.write(Utils.encode(description));
         }
         else
         {
            out.write(description);
         }
         out.write("</div>");
      }
      out.write("</td>");
      
      if (activeSelect)
      {
         // we are rendering an active select list with child components next to each item
         // get the child components and look for compatible Command objects
         out.write("<td");
         Utils.outputAttribute(out, getAttributes().get("itemStyle"), "style");
         Utils.outputAttribute(out, getAttributes().get("itemStyleClass"), "class");
         out.write('>');
         for (Iterator i = getChildren().iterator(); i.hasNext(); /**/)
         {
            UIComponent child = (UIComponent)i.next();
            if (child instanceof UICommand)
            {
               out.write("<span style='padding:1px'>");
               Utils.encodeRecursive(context, child);
               out.write("</span>");
            }
         }
         out.write("</td>");
      }
   }
   
   
   // ------------------------------------------------------------------------------
   // Strongly typed property accessors 
   
   /**
    * @return current row index
    */
   public int getRowIndex()
   {
      return this.rowIndex;
   }

   /**
    * Set the transient current row index. Setting this value causes all child components to
    * have their ID values reset - so that cached clientID values are regenerated when next requested.
    * 
    * @param rowIndex int
    */
   public void setRowIndex(int rowIndex)
   {
      if (isActiveSelect())
      {
         this.rowIndex = rowIndex;
         for (UIComponent child : (List<UIComponent>)this.getChildren())
         {
            if (child instanceof UIListItem == false && child instanceof UIListItems == false)
            {
               // forces a reset of the clientId for the component
               // This is then regenerated - relative to this naming container which itself uses the
               // current row index as part of the Id. This is what facilities the correct component
               // rendering submit script and then identified during the decode() phase.
               child.setId(child.getId());
            }
         }
      }
   }

   /**
    * Get the multi-select rendering flag
    *
    * @return true for multi-select rendering, false otherwise
    */
   public boolean isMultiSelect()
   {
      ValueBinding vb = getValueBinding("multiSelect");
      if (vb != null)
      {
         this.multiSelect = (Boolean)vb.getValue(getFacesContext());
      }
      
      return this.multiSelect != null ? this.multiSelect.booleanValue() : false;
   }

   /**
    * Set true for multi-select rendering, false otherwise
    *
    * @param multiSelect      True for multi-select
    */
   public void setMultiSelect(boolean multiSelect)
   {
      this.multiSelect = multiSelect;
   }
   
   /**
    * Get the active selection mode flag
    *
    * @return true for active selection mode, false otherwise
    */
   public boolean isActiveSelect()
   {
      ValueBinding vb = getValueBinding("activeSelect");
      if (vb != null)
      {
         this.activeSelect = (Boolean)vb.getValue(getFacesContext());
      }
      
      return this.activeSelect != null ? this.activeSelect.booleanValue() : false;
   }

   /**
    * Set true for active selection mode, false otherwise
    *
    * @param activeSelect      True for active selection
    */
   public void setActiveSelect(boolean activeSelect)
   {
      this.activeSelect = activeSelect;
   }
   
   /**
    * Get the escape item label flag
    *
    * @return true if the items label should be escaped, false otherwise
    */
   public boolean getEscapeItemLabel()
   {
      ValueBinding vb = getValueBinding("escapeItemLabel");
      if (vb != null)
      {
         this.escapeItemLabel = (Boolean)vb.getValue(getFacesContext());
      }
      
      return this.escapeItemLabel != null ? this.escapeItemLabel.booleanValue() : true;
   }

   /**
    * Set true to escape the items label, false otherwise
    *
    * @param escapeItemLabel true to escape the items label
    */
   public void setEscapeItemLabel(boolean escapeItemLabel)
   {
      this.escapeItemLabel = escapeItemLabel;
   }
   
   /**
    * Get the escape item description flag
    *
    * @return true if the items description should be escaped, false otherwise
    */
   public boolean getEscapeItemDescription()
   {
      ValueBinding vb = getValueBinding("escapeItemDescription");
      if (vb != null)
      {
         this.escapeItemDescription = (Boolean)vb.getValue(getFacesContext());
      }
      
      return this.escapeItemDescription != null ? this.escapeItemDescription.booleanValue() : true;
   }

   /**
    * Set true to escape the items description, false otherwise
    *
    * @param escapeItemDescription true to escape the items description
    */
   public void setEscapeItemDescription(boolean escapeItemDescription)
   {
      this.escapeItemDescription = escapeItemDescription;
   }
   
   /**
    * We use a hidden field name based on the parent form component Id and
    * the string "selectlist" to give a hidden field name that can be shared by all
    * SelectList components within a single UIForm component.
    * 
    * @return hidden field name
    */
   private static String getHiddenFieldName(FacesContext context, UIComponent component)
   {
      UIForm form = Utils.getParentForm(context, component);
      return form.getClientId(context) + NamingContainer.SEPARATOR_CHAR + "selectlist";
   }
   
   
   /**
    * Wrapper for a FacesEvent to hold current row value when the event was fired
    */
   private static class FacesEventWrapper extends FacesEvent
   {
      private FacesEvent wrappedFacesEvent;
      private int rowIndex;
      
      public FacesEventWrapper(FacesEvent facesEvent, int rowIndex, UISelectList redirectComponent)
      {
         super(redirectComponent);
         wrappedFacesEvent = facesEvent;
         this.rowIndex = rowIndex;
      }
      
      public PhaseId getPhaseId()
      {
         return wrappedFacesEvent.getPhaseId();
      }
      
      public void setPhaseId(PhaseId phaseId)
      {
         wrappedFacesEvent.setPhaseId(phaseId);
      }
      
      public void queue()
      {
         wrappedFacesEvent.queue();
      }
      
      public String toString()
      {
         return wrappedFacesEvent.toString();
      }
      
      public boolean isAppropriateListener(FacesListener faceslistener)
      {
         return wrappedFacesEvent.isAppropriateListener(faceslistener);
      }
      
      public void processListener(FacesListener faceslistener)
      {
         wrappedFacesEvent.processListener(faceslistener);
      }
      
      public FacesEvent getWrappedFacesEvent()
      {
         return wrappedFacesEvent;
      }
      
      public int getRowIndex()
      {
         return rowIndex;
      }
   }
}
