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
package org.alfresco.web.ui.common.renderer.data;

import java.io.IOException;
import java.io.Serializable;

import javax.faces.context.FacesContext;

import org.alfresco.web.ui.common.component.data.UIColumn;
import org.alfresco.web.ui.common.component.data.UIRichList;

/**
 * Contract for implementations capable of rendering the columns for a Rich List
 * component.
 * 
 * @author kevinr
 */
public interface IRichListRenderer extends Serializable
{
   /**
    * Callback executed by the RichList component to render any adornments before
    * the main list rows are rendered. This is generally used to output header items.
    * 
    * @param context       FacesContext
    * @param richList      The parent RichList component
    * @param columns       Array of columns to be shown
    * 
    * @throws IOException
    */
   public void renderListBefore(FacesContext context, UIRichList richList, UIColumn[] columns)
      throws IOException;
   
   /**
    * Callback executed by the RichList component once per row of data to be rendered.
    * The bean used as the current row data is provided, but generally rendering of the
    * column data will be performed by recursively encoding Column child components. 
    * 
    * @param context       FacesContext
    * @param richList      The parent RichList component
    * @param columns       Array of columns to be shown
    * @param row           The data bean for the current row
    * 
    * @throws IOException
    */
   public void renderListRow(FacesContext context, UIRichList richList, UIColumn[] columns, Object row)
      throws IOException;
   
   /**
    * Callback executed by the RichList component to render any adornments after
    * the main list rows are rendered. This is generally used to output footer items.
    * 
    * @param context       FacesContext
    * @param richList      The parent RichList component
    * @param columns       Array of columns to be shown
    * 
    * @throws IOException
    */
   public void renderListAfter(FacesContext context, UIRichList richList, UIColumn[] columns)
      throws IOException;
   
   /**
    * Return the unique view mode identifier that this renderer is responsible for. 
    * 
    * @return Unique view mode identifier for this renderer e.g. "icons" or "details"
    */
   public String getViewModeID();
}
