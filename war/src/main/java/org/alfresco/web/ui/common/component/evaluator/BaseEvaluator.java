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
package org.alfresco.web.ui.common.component.evaluator;

import java.io.IOException;

import javax.faces.context.FacesContext;
import javax.faces.el.ValueBinding;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.extensions.webscripts.ui.common.component.SelfRenderingComponent;

/**
 * @author kevinr
 */
public abstract class BaseEvaluator extends SelfRenderingComponent
{
   /**
    * @see javax.faces.component.UIComponent#getFamily()
    */
   public final String getFamily()
   {
      return "org.alfresco.faces.evaluators";
   }

   /**
    * @see javax.faces.component.UIComponentBase#getRendersChildren()
    */
   public final boolean getRendersChildren()
   {
      return !evaluate();
   }

   /**
    * @see javax.faces.component.UIComponentBase#encodeBegin(javax.faces.context.FacesContext)
    */
   public final void encodeBegin(FacesContext context) throws IOException
   {
      // no output for this component
   }

   /**
    * @see javax.faces.component.UIComponentBase#encodeChildren(javax.faces.context.FacesContext)
    */
   public final void encodeChildren(FacesContext context) throws IOException
   {
      // if this is called, then the evaluate returned false which means
      // the child components show not be allowed to render themselves
   }

   /**
    * @see javax.faces.component.UIComponentBase#encodeEnd(javax.faces.context.FacesContext)
    */
   public final void encodeEnd(FacesContext context) throws IOException
   {
      // no output for this component
   }
   
   /**
    * Get the value for this component to be evaluated against
    *
    * @return the value for this component to be evaluated against
    */
   public Object getValue()
   {
      ValueBinding vb = getValueBinding("value");
      if (vb != null)
      {
         this.value = vb.getValue(getFacesContext());
      }
      
      return this.value;
   }

   /**
    * Set the value for this component to be evaluated against
    *
    * @param value     the value for this component to be evaluated against
    */
   public void setValue(Object value)
   {
      this.value = value;
   }
   
   /**
    * @see javax.faces.component.StateHolder#restoreState(javax.faces.context.FacesContext, java.lang.Object)
    */
   public void restoreState(FacesContext context, Object state)
   {
      Object values[] = (Object[])state;
      // standard component attributes are restored by the super class
      super.restoreState(context, values[0]);
      this.value = values[1];
   }
   
   /**
    * @see javax.faces.component.StateHolder#saveState(javax.faces.context.FacesContext)
    */
   public Object saveState(FacesContext context)
   {
      Object values[] = new Object[2];
      // standard component attributes are saved by the super class
      values[0] = super.saveState(context);
      values[1] = this.value;
      return (values);
   }
   
   /**
    * Evaluate against the component attributes. Return true to allow the inner
    * components to render, false to hide them during rendering.
    * 
    * @return true to allow rendering of child components, false otherwise
    */
   public abstract boolean evaluate();
   
   
   protected static final Log    s_logger = LogFactory.getLog(BaseEvaluator.class);
   
   /** the value to be evaluated against */
   private Object value;
}
