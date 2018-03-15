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

import javax.faces.component.UICommand;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.el.ValueBinding;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;

/**
 * @author Kevin Roast
 */
public class UINodeDescendants extends UICommand
{
   // ------------------------------------------------------------------------------
   // Construction
   
   /**
    * Default constructor
    */
   public UINodeDescendants()
   {
      setRendererType("org.alfresco.faces.NodeDescendantsLinkRenderer");
   }
   
   
   // ------------------------------------------------------------------------------
   // Component implementation
   
   /**
    * @see javax.faces.component.UIComponent#getFamily()
    */
   public String getFamily()
   {
      return "org.alfresco.faces.NodeDescendants";
   }
   
   /**
    * @see javax.faces.component.StateHolder#restoreState(javax.faces.context.FacesContext, java.lang.Object)
    */
   public void restoreState(FacesContext context, Object state)
   {
      Object values[] = (Object[])state;
      // standard component attributes are restored by the super class
      super.restoreState(context, values[0]);
      this.maxChildren = (Integer)values[1];
      this.showEllipses = (Boolean)values[2];
   }
   
   /**
    * @see javax.faces.component.StateHolder#saveState(javax.faces.context.FacesContext)
    */
   public Object saveState(FacesContext context)
   {
      Object values[] = new Object[3];
      // standard component attributes are saved by the super class
      values[0] = super.saveState(context);
      values[1] = this.maxChildren;
      values[2] = this.showEllipses;
      return (values);
   }
   
   
   // ------------------------------------------------------------------------------
   // Strongly typed component property accessors
   
   /**
    * @return the maximum number of child descendants to be displayed, default maximum is 3. 
    */
   public int getMaxChildren()
   {
      ValueBinding vb = getValueBinding("maxChildren");
      if (vb != null)
      {
         this.maxChildren = (Integer)vb.getValue(getFacesContext());
      }
      
      if (this.maxChildren != null)
      {
         return this.maxChildren.intValue();
      }
      else
      {
         // return default
         return 3;
      }
   }
   
   /**
    * @param value      The maximum allowed before the no more links are shown
    */
   public void setMaxChildren(int value)
   {
      if (value > 0 && value <= 256)
      {
         this.maxChildren = Integer.valueOf(value);
      }
   }
   
   /**
    * @return whether to show ellipses "..." if more descendants than the maxChildren value are found
    */
   public boolean getShowEllipses()
   {
      ValueBinding vb = getValueBinding("showEllipses");
      if (vb != null)
      {
         this.showEllipses = (Boolean)vb.getValue(getFacesContext());
      }
      
      if (this.showEllipses != null)
      {
         return this.showEllipses.booleanValue();
      }
      else
      {
         // return default
         return true;
      }
   }
   
   /**
    * @param showEllipses      True to show ellipses "..." if more descendants than maxChildren are found
    */
   public void setShowEllipses(boolean showEllipses)
   {
      this.showEllipses = Boolean.valueOf(showEllipses);
   }
   
   
   // ------------------------------------------------------------------------------
   // Inner classes
   
   /**
    * Class representing the clicking of a node descendant element.
    */
   public static class NodeSelectedEvent extends ActionEvent
   {
      public NodeSelectedEvent(UIComponent component, NodeRef nodeRef, boolean isParent)
      {
         super(component);
         this.NodeReference = nodeRef;
         this.IsParent = isParent;
      }
      
      public NodeRef NodeReference;
      public boolean IsParent;
   }
   
   
   // ------------------------------------------------------------------------------
   // Private data
   
   /** maximum number of child descendants to display */
   private Integer maxChildren = null;
   
   /** whether to show ellipses if more descendants than the maxChildren are found */
   private Boolean showEllipses = null;
}
