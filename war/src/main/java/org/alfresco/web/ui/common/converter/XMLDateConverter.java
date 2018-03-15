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

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.DateTimeConverter;

import org.springframework.extensions.surf.util.ISO8601DateFormat;

/**
 * Converter class to convert an XML date representation into a Date
 * 
 * @author gavinc
 */
public class XMLDateConverter extends DateTimeConverter
{
   /**
    * <p>The standard converter id for this converter.</p>
    */
   public static final String CONVERTER_ID = "org.alfresco.faces.XMLDateConverter";

   /**
    * @see javax.faces.convert.Converter#getAsObject(javax.faces.context.FacesContext, javax.faces.component.UIComponent, java.lang.String)
    */
   public Object getAsObject(FacesContext context, UIComponent component, String value)
   {
      return ISO8601DateFormat.parse(value);
   }

   /**
    * @see javax.faces.convert.Converter#getAsString(javax.faces.context.FacesContext, javax.faces.component.UIComponent, java.lang.Object)
    */
   public String getAsString(FacesContext context, UIComponent component, Object value)
   {
      String str = null;
      
      if (value instanceof String)
      {
         Date date = ISO8601DateFormat.parse((String)value);
         str = super.getAsString(context, component, date);
      }
      else if (value instanceof List)
      {
         StringBuilder buffer = new StringBuilder();
         for (Object date : ((List)value))
         {
            if (buffer.length() != 0)
            {
               buffer.append(", ");
            }
            
            buffer.append(super.getAsString(context, component, date));
         }
         
         str = buffer.toString();
      }
      else
      {
         str = super.getAsString(context, component, value);
      }
      
      return str;
   }

   /**
    * @see javax.faces.convert.DateTimeConverter#getTimeZone()
    */
   @Override
   public TimeZone getTimeZone()
   {
      // Note: this forces the display of the date to the server's timezone - it does not
      //       take into account any client specific timezone
      return TimeZone.getDefault();
   }

   /**
    * @see javax.faces.convert.DateTimeConverter#getLocale()
    */
   @Override
   public Locale getLocale()
   {
      // get the locale set in the client
      FacesContext context = FacesContext.getCurrentInstance();
      Locale locale = context.getViewRoot().getLocale();
      if (locale == null)
      {
         // else use server locale as the default
         locale = Locale.getDefault();
      }
      
      return locale;
   }
}
