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
package org.alfresco.web.ui.common.converter;

import java.text.DecimalFormat;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;

import org.alfresco.web.app.Application;

/**
 * Converter class to convert the size of an item in bytes into a readable KB/MB form.
 * 
 * @author Kevin Roast
 */
public class ByteSizeConverter implements Converter
{
   /**
    * <p>The standard converter id for this converter.</p>
    */
   public static final String CONVERTER_ID = "org.alfresco.faces.ByteSizeConverter";

   private static final String MSG_POSTFIX_KB = "kilobyte";
   private static final String MSG_POSTFIX_MB = "megabyte";
   private static final String MSG_POSTFIX_GB = "gigabyte";
   
   private static final String NUMBER_PATTERN = "###,###.##";
   
   /**
    * @see javax.faces.convert.Converter#getAsObject(javax.faces.context.FacesContext, javax.faces.component.UIComponent, java.lang.String)
    */
   public Object getAsObject(FacesContext context, UIComponent component, String value)
   {
      return Long.parseLong(value);
   }

   /**
    * @see javax.faces.convert.Converter#getAsString(javax.faces.context.FacesContext, javax.faces.component.UIComponent, java.lang.Object)
    */
   public String getAsString(FacesContext context, UIComponent component, Object value)
   {
      long size;
      if (value instanceof Long)
      {
         size = (Long)value;
      }
      else if (value instanceof String)
      {
         try
         {
            size = Long.parseLong((String)value);
         }
         catch (NumberFormatException ne)
         {
            return (String)value;
         }
      }
      else
      {
         return "";
      }
      
      // get formatter
      // TODO: can we cache this instance...? DecimalFormat is not threadsafe! Need threadlocal instance.
      DecimalFormat formatter = new DecimalFormat(NUMBER_PATTERN);
      
      StringBuilder buf = new StringBuilder();
      
      if (size < 999999)
      {
         double val = ((double)size) / 1024.0;
         buf.append(formatter.format(val))
            .append(' ')
            .append(Application.getMessage(context, MSG_POSTFIX_KB));
      }
      else if (size < 999999999)
      {
         double val = ((double)size) / 1048576.0;
         buf.append(formatter.format(val))
            .append(' ')
            .append(Application.getMessage(context, MSG_POSTFIX_MB));
      }
      else
      {
         double val = ((double)size) / 1073741824.0;
         buf.append(formatter.format(val))
            .append(' ')
            .append(Application.getMessage(context, MSG_POSTFIX_GB));
      }
      
      return buf.toString();
   }
}
