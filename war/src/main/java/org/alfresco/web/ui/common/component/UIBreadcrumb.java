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

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.faces.component.UICommand;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.el.ValueBinding;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ActionEvent;
import javax.faces.event.FacesEvent;

/**
 * @author kevinr
 */
public class UIBreadcrumb extends UICommand
{
   // ------------------------------------------------------------------------------
   // Construction 
   
   /**
    * Default Constructor
    */
   public UIBreadcrumb()
   {
      setRendererType("org.alfresco.faces.BreadcrumbRenderer");
   }


   // ------------------------------------------------------------------------------
   // Component implementation 
   
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
      this.separator = (String)values[1];
      this.showRoot = (Boolean)values[2];
   }
   
   /**
    * @see javax.faces.component.StateHolder#saveState(javax.faces.context.FacesContext)
    */
   public Object saveState(FacesContext context)
   {
      Object values[] = new Object[3];
      // standard component attributes are saved by the super class
      values[0] = super.saveState(context);
      values[1] = this.separator;
      values[2] = this.showRoot;
      return (values);
   }
   
   /**
    * @see javax.faces.component.UICommand#broadcast(javax.faces.event.FacesEvent)
    */
   public void broadcast(FacesEvent event) throws AbortProcessingException
   {
      if (event instanceof BreadcrumbEvent)
      {
         setSelectedPathIndex( ((BreadcrumbEvent)event).SelectedIndex );
      }
      
      // default ActionEvent processing for a UICommand
      super.broadcast(event);
   }

   /**
    * Set the selected path index. This modifies the current path value.
    */
   public void setSelectedPathIndex(int index)
   {
      // getValue() will return a List of IBreadcrumbHandler (see impl below)
      List<IBreadcrumbHandler> elements = (List)getValue();
      
      if (elements.size() >= index)
      {
         // copy path elements up to the selected index to a new List
         List<IBreadcrumbHandler> path = new ArrayList<IBreadcrumbHandler>(index + 1);
         path.addAll(elements.subList(0, index + 1));
         
         // set the new List as our new path value
         setValue(path);
         
         // call the app logic for the element handler and perform any required navigation
         String outcome = path.get(index).navigationOutcome(this);
         if (outcome != null)
         {
            String viewId = getFacesContext().getViewRoot().getViewId();
            getFacesContext().getApplication().getNavigationHandler().handleNavigation(
                  getFacesContext(), viewId, outcome);
         }
      }
   }
   
   /**
    * Override getValue() to deal with converting a String path into a valid List of IBreadcrumbHandler
    */
   public Object getValue()
   {
      List<IBreadcrumbHandler> elements = null;
      
      Object value = super.getValue();
      if (value instanceof String)
      {
         elements = new ArrayList(8);
         // found a String based path - convert to List of IBreadcrumbHandler instances
         StringTokenizer t = new StringTokenizer((String)value, SEPARATOR);
         while (t.hasMoreTokens() == true)
         {
            IBreadcrumbHandler handler = new DefaultPathHandler(t.nextToken());
            elements.add(handler);
         }
         
         // save result so we don't need to repeat the conversion
         setValue(elements);
      }
      else if (value instanceof List)
      {
         elements = (List)value;
      }
      else if (value != null)
      {
         throw new IllegalArgumentException("UIBreadcrumb value must be a String path or List of IBreadcrumbHandler!");
      }
      else
      {
         elements = new ArrayList(8);
      }
      
      return elements;
   }
   
   /**
    * Append a handler object to the current breadcrumb structure
    * 
    * @param handler    The IBreadcrumbHandler to append
    */
   public void appendHandler(IBreadcrumbHandler handler)
   {
      if (handler == null)
      {
         throw new NullPointerException("IBreadcrumbHandler instance cannot be null!");
      }
      
      List elements = (List)getValue();
      elements.add(handler);
   }
   
   
   // ------------------------------------------------------------------------------
   // Strongly typed component property accessors 
   
   /**
    * Get the separator string to output between each breadcrumb element
    * 
    * @return separator string
    */
   public String getSeparator()
   {
      ValueBinding vb = getValueBinding("separator");
      if (vb != null)
      {
         this.separator = (String)vb.getValue(getFacesContext());
      }
      
      return this.separator;
   }
   
   /**
    * Set separator
    * 
    * @param separator     the separator string to output between each breadcrumb element
    */
   public void setSeparator(String separator)
   {
      this.separator = separator;
   }
   
   /**
    * Get whether to show the root of the path
    * 
    * @return true to show the root of the path, false to hide it
    */
   public boolean getShowRoot()
   {
      ValueBinding vb = getValueBinding("showRoot");
      if (vb != null)
      {
         this.showRoot = (Boolean)vb.getValue(getFacesContext());
      }
      
      if (this.showRoot != null)
      {
         return this.showRoot.booleanValue();
      }
      else
      {
         // return default
         return true;
      }
   }
   
   /**
    * Set whether to show the root of the path
    * 
    * @param showRoot      Whether to show the root of the path
    */
   public void setShowRoot(boolean showRoot)
   {
      this.showRoot = Boolean.valueOf(showRoot);
   }
   
   
   // ------------------------------------------------------------------------------
   // Inner classes
   
   /**
    * Class representing the clicking of a breadcrumb element.
    */
   public static class BreadcrumbEvent extends ActionEvent
   {
      public BreadcrumbEvent(UIComponent component, int selectedIndex)
      {
         super(component);
         SelectedIndex = selectedIndex;
      }
      
      public int SelectedIndex = 0;
   }
   
   /**
    * Class representing a handler for the default String path based breadcrumb
    */
   private static class DefaultPathHandler implements IBreadcrumbHandler
   {
      /**
       * Constructor
       * 
       * @param label      The element display label
       */
      public DefaultPathHandler(String label)
      {
         this.label = label;
      }
      
      /**
       * Return the element display label
       */
      public String toString()
      {
         return this.label;
      }
      
      /**
       * @see org.alfresco.web.ui.common.component.IBreadcrumbHandler#navigationOutcome(org.alfresco.web.ui.common.component.UIBreadcrumb)
       */
      public String navigationOutcome(UIBreadcrumb breadcrumb)
      {
         // no outcome for the default handler - return to current page
         return null;
      }
      
      private String label;
   }
   
   
   // ------------------------------------------------------------------------------
   // Private data
   
   /** visible separator value */
   private String separator = null;
   
   /** true to show the root of the breadcrumb path, false otherwise */
   private Boolean showRoot = null;
   
   /** the separator for a breadcrumb path value */
   public final static String SEPARATOR = "/";
}
