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

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.component.UIComponentBase;
import javax.faces.context.FacesContext;
import javax.faces.el.ValueBinding;
import javax.transaction.UserTransaction;

import org.alfresco.web.app.Application;
import org.alfresco.web.bean.repository.Repository;
import org.alfresco.web.config.ViewsConfigElement;
import org.alfresco.web.data.IDataContainer;
import org.alfresco.web.ui.common.renderer.data.IRichListRenderer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Kevin Roast
 */
public class UIRichList extends UIComponentBase implements IDataContainer,Serializable
{
   // ------------------------------------------------------------------------------
   // Construction
   
   private static final long serialVersionUID = 4302199745018058173L;

   /**
    * Default constructor
    */
   public UIRichList()
   {
      setRendererType("org.alfresco.faces.RichListRenderer");
      
      // get the list of views from the client configuration
      ViewsConfigElement viewsConfig = (ViewsConfigElement)Application.getConfigService(
            FacesContext.getCurrentInstance()).getConfig("Views").
            getConfigElement(ViewsConfigElement.CONFIG_ELEMENT_ID);
      List<String> views = viewsConfig.getViews();
      
      // instantiate each renderer and add to the list
      for (String view : views)
      {
         try
         {
            Class clazz = Class.forName(view);
            IRichListRenderer renderer = (IRichListRenderer)clazz.newInstance();
            viewRenderers.put(renderer.getViewModeID(), renderer);
            
            if (logger.isDebugEnabled())
               logger.debug("Added view '" + renderer.getViewModeID() + "' to UIRichList");
         }
         catch (Exception e)
         {
            if (logger.isWarnEnabled())
            {
               logger.warn("Failed to create renderer: " + view, e);
            }
         }
      }
   }


   // ------------------------------------------------------------------------------
   // Component implementation
   
   /**
    * @see javax.faces.component.UIComponent#getFamily()
    */
   public String getFamily()
   {
      return "org.alfresco.faces.Data";
   }
   
   /**
    * @see javax.faces.component.StateHolder#restoreState(javax.faces.context.FacesContext, java.lang.Object)
    */
   public void restoreState(FacesContext context, Object state)
   {
      Object values[] = (Object[])state;
      // standard component attributes are restored by the super class
      super.restoreState(context, values[0]);
      this.currentPage = ((Integer)values[1]).intValue();
      this.sortColumn = (String)values[2];
      this.sortDescending = ((Boolean)values[3]).booleanValue();
      this.value = values[4];                      // not serializable!
      this.dataModel = (IGridDataModel)values[5];  // not serializable!
      this.viewMode = (String)values[6];
      this.pageSize = ((Integer)values[7]).intValue();
      this.initialSortColumn = (String)values[8];
      this.initialSortDescending = ((Boolean)values[9]).booleanValue();
      this.refreshOnBind = ((Boolean)values[10]).booleanValue();
   }
   
   /**
    * @see javax.faces.component.StateHolder#saveState(javax.faces.context.FacesContext)
    */
   public Object saveState(FacesContext context)
   {
      Object values[] = new Object[] {
            // standard component attributes are saved by the super class
            super.saveState(context),
            Integer.valueOf(this.currentPage),
            this.sortColumn,
            (this.sortDescending ? Boolean.TRUE : Boolean.FALSE),
            this.value,
            this.dataModel,
            this.viewMode,
            Integer.valueOf(this.pageSize),
            this.initialSortColumn,
            (this.initialSortDescending ? Boolean.TRUE : Boolean.FALSE),
            this.refreshOnBind};
      
      return (values);
   }
   
   /**
    * Get the value (for this component the value is an object used as the DataModel)
    *
    * @return the value
    */
   public Object getValue()
   {
      if (this.value == null)
      {
         ValueBinding vb = getValueBinding("value");
         if (vb != null)
         {
            this.value = vb.getValue(getFacesContext());
         }
      }
      return this.value;
   }

   /**
    * Set the value (for this component the value is an object used as the DataModel)
    *
    * @param value     the value
    */
   public void setValue(Object value)
   {
      this.dataModel = null;
      this.value = value;
   }
   
   /**
    * Clear the current sorting settings back to the defaults
    */
   public void clearSort()
   {
      this.sortColumn = null;
      this.sortDescending = true;
      this.initialSortColumn = null;
      this.initialSortDescending = false;
   }
   
   /**
    * Get the view mode for this Rich List
    * 
    * @return view mode as a String
    */
   public String getViewMode()
   {
      ValueBinding vb = getValueBinding("viewMode");
      if (vb != null)
      {
         this.viewMode = (String)vb.getValue(getFacesContext());
      }
      
      return this.viewMode;
   }
   
   /**
    * Set the current view mode for this Rich List
    * 
    * @param viewMode      the view mode as a String
    */
   public void setViewMode(String viewMode)
   {
      this.viewMode = viewMode;
   }
   
   /**
    * Get the refreshOnBind flag.
    *
    * @return the refreshOnBind
    */
   public boolean getRefreshOnBind()
   {
      ValueBinding vb = getValueBinding("refreshOnBind");
      if (vb != null)
      {
         this.refreshOnBind = (Boolean)vb.getValue(getFacesContext());
      }
      return this.refreshOnBind;
   }

   /**
    * Set the refreshOnBind flag. True to force the list to retrieve bound data on bind().
    *
    * @param refreshOnBind     the refreshOnBind
    */
   public void setRefreshOnBind(boolean refreshOnBind)
   {
      this.refreshOnBind = refreshOnBind;
   }
   
   /**
    * Return the UI Component to be used as the "no items available" message
    * 
    * @return UIComponent
    */
   public UIComponent getEmptyMessage()
   {
      return getFacet("empty");
   }
   
   
   // ------------------------------------------------------------------------------
   // IDataContainer implementation 
   
   /**
    * Return the currently sorted column if any
    * 
    * @return current sorted column if any
    */
   public String getCurrentSortColumn()
   {
      return this.sortColumn;
   }
   
   /**
    * @see org.alfresco.web.data.IDataContainer#isCurrentSortDescending()
    */
   public boolean isCurrentSortDescending()
   {
      return this.sortDescending;
   }
   
   /**
    * @return Returns the initialSortColumn.
    */
   public String getInitialSortColumn()
   {
      return this.initialSortColumn;
   }

   /**
    * @param initialSortColumn The initialSortColumn to set.
    */
   public void setInitialSortColumn(String initialSortColumn)
   {
      this.initialSortColumn = initialSortColumn;
   }

   /**
    * @return Returns the initialSortDescending.
    */
   public boolean isInitialSortDescending()
   {
      return this.initialSortDescending;
   }

   /**
    * @param initialSortDescending The initialSortDescending to set.
    */
   public void setInitialSortDescending(boolean initialSortDescending)
   {
      this.initialSortDescending = initialSortDescending;
   }
   
   /**
    * Returns the current page size used for this list, or -1 for no paging.
    */
   public int getPageSize()
   {
      ValueBinding vb = getValueBinding("pageSize");
      if (vb != null)
      {
         int pageSize = ((Integer)vb.getValue(getFacesContext())).intValue();
         if (pageSize != this.pageSize)
         {
            // force a reset of the current page - else the bind may show a page that isn't there
            setPageSize(pageSize);
         }
      }
      
      return this.pageSize;
   }
   
   /**
    * Sets the current page size used for the list.
    * 
    * @param val int
    */
   public void setPageSize(int val)
   {
      if (val >= -1)
      {
         this.pageSize = val;
         setCurrentPage(0);
      }
   }
   
   /**
    * @see org.alfresco.web.data.IDataContainer#getPageCount()
    */
   public int getPageCount()
   {
      return this.pageCount;
   }
   
   /**
    * Return the current page the list is displaying
    * 
    * @return current page zero based index
    */
   public int getCurrentPage()
   {
      return this.currentPage;
   }
   
   /**
    * @see org.alfresco.web.data.IDataContainer#setCurrentPage(int)
    */
   public void setCurrentPage(int index)
   {
      this.currentPage = index;
      this.sortOrPageChanged = true;
   }

   /**
    * Returns true if a row of data is available
    * 
    * @return true if data is available, false otherwise
    */
   public boolean isDataAvailable()
   {
      return this.rowIndex < this.maxRowIndex;
   }
   
   /**
    * Returns the next row of data from the data model
    * 
    * @return next row of data as a Bean object
    */
   public Object nextRow()
   {
      // get next row and increment row count
      Object rowData = getDataModel().getRow(this.rowIndex + 1);
      
      // Prepare the data-binding variable "var" ready for the next cycle of
      // renderering for the child components. 
      String var = (String)getAttributes().get("var");
      if (var != null)
      {
         Map requestMap = getFacesContext().getExternalContext().getRequestMap();
         if (isDataAvailable() == true)
         {
            requestMap.put(var, rowData);
         }
         else
         {
            requestMap.remove(var);
         }
      }
      
      this.rowIndex++;
      
      return rowData;
   }
   
   /**
    * Sort the dataset using the specified sort parameters
    * 
    * @param column        Column to sort
    * @param descending    True for descending sort, false for ascending
    * @param mode          Sort mode to use (see IDataContainer constants)
    */
   public void sort(String column, boolean descending, String mode)
   {
      this.sortColumn = column;
      this.sortDescending = descending;
      this.sortOrPageChanged = true;
      
      // delegate to the data model to sort its contents
      // place in a UserTransaction as we may need to perform a LOT of node calls to complete
      UserTransaction tx = null;
      try
      {
         if (getDataModel().size() > 16)
         {
            FacesContext context = FacesContext.getCurrentInstance();
            tx = Repository.getUserTransaction(context, true);
            tx.begin();
         }
         
         getDataModel().sort(column, descending, mode);
         
         // commit the transaction
         if (tx != null)
         {
            tx.commit();
         }
      }
      catch (Throwable err)
      {
         try { if (tx != null) {tx.rollback();} } catch (Exception tex) {}
      }
   }
   
   
   // ------------------------------------------------------------------------------
   // UIRichList implementation
   
   /**
    * Method called to bind the RichList component state to the data model value
    */
   public void bind()
   {
      if (getRefreshOnBind() == true)
      {
         this.value = null;
         this.dataModel = null;
      }
      int rowCount = getDataModel().size();
      // if a page size is specified, then we use that
      int pageSize = getPageSize();
      if (pageSize != -1 && pageSize != 0)
      {
         // calc total number of pages available
         this.pageCount = (rowCount / this.pageSize) + 1;
         
         if (rowCount % pageSize == 0 && this.pageCount != 1)
         {
            this.pageCount--;
         }
         
         // set currentPage as lastPage if input digit in UIDataPager > pageCount
         if (this.currentPage >= this.pageCount)
         {
            this.currentPage = this.pageCount - 1;
         }
         
         // calc start row index based on current page index
         this.rowIndex = (this.currentPage * pageSize) - 1;
         
         // calc the maximum row index that can be returned
         this.maxRowIndex = this.rowIndex + pageSize;
         if (this.maxRowIndex >= rowCount)
         {
            this.maxRowIndex = rowCount - 1;
         }
      }
      // else we are not paged so show all data from start
      else
      {
         this.rowIndex = -1;
         this.pageCount = 1;
         this.maxRowIndex = (rowCount - 1);
      }
      if (logger.isDebugEnabled())
         logger.debug("Bound datasource: PageSize: " + pageSize + "; CurrentPage: " + this.currentPage + "; RowIndex: " + this.rowIndex + "; MaxRowIndex: " + this.maxRowIndex + "; RowCount: " + rowCount);
   }
   
   /**
    * @return A new IRichListRenderer implementation for the current view mode
    */
   public IRichListRenderer getViewRenderer()
   {
      // get type from current view mode, then create an instance of the renderer
      IRichListRenderer renderer = null;
      if (getViewMode() != null)
      {
         renderer = (IRichListRenderer)viewRenderers.get(getViewMode());
      }
      return renderer;
   }
   
   /**
    * Return the data model wrapper
    * 
    * @return IGridDataModel 
    */
   private IGridDataModel getDataModel()
   {
      if (this.dataModel == null)
      {
         // build the appropriate data-model wrapper object
         Object val = getValue();
         if (val instanceof List)
         {
            this.dataModel = new GridListDataModel((List)val);
         }
         else if ( (java.lang.Object[].class).isAssignableFrom(val.getClass()) )
         {
            this.dataModel = new GridArrayDataModel((Object[])val);
         }
         else
         {
            throw new IllegalStateException("UIRichList 'value' attribute binding should specify data model of a supported type!"); 
         }
         
         // sort first time on initially sorted column if set
         if (this.sortColumn == null)
         {
            String initialSortColumn = getInitialSortColumn();
            if (initialSortColumn != null && initialSortColumn.length() != 0)
            {
               boolean descending = isInitialSortDescending();
               
               // TODO: add support for retrieving correct column sort mode here
               this.sortColumn = initialSortColumn;
               this.sortDescending = descending;
            }
         }
         if (this.sortColumn != null)
         {
            // delegate to the data model to sort its contents
            this.dataModel.sort(this.sortColumn, this.sortDescending, IDataContainer.SORT_CASEINSENSITIVE);
         }
         
         // reset current page
         if (this.sortOrPageChanged == false)
         {
            this.currentPage = 0;
         }
         this.sortOrPageChanged = false;
      }
      
      return this.dataModel;
   }
   
   
   // ------------------------------------------------------------------------------
   // Private data
   
   /** map of available IRichListRenderer instances */
   protected final Map<String, IRichListRenderer> viewRenderers = new HashMap<String, IRichListRenderer>(4, 1.0f);
   
   // component state
   private int currentPage = 0;
   private String sortColumn = null;
   private boolean sortDescending = true;
   private Object value = null;
   private IGridDataModel dataModel = null;
   private String viewMode = null;
   private int pageSize = -1;
   private String initialSortColumn = null;
   private boolean initialSortDescending = false;
   private boolean refreshOnBind = false;
   
   // transient component state that exists during a single page refresh only
   private int rowIndex = -1;
   private int maxRowIndex = -1;
   private int pageCount = 1;
   private boolean sortOrPageChanged = false;
   
   private static Log logger = LogFactory.getLog(IDataContainer.class);
}
