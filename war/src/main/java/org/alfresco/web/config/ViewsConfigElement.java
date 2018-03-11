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
package org.alfresco.web.config;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.extensions.config.ConfigElement;
import org.springframework.extensions.config.ConfigException;
import org.springframework.extensions.config.element.ConfigElementAdapter;

/**
 * Custom config element that represents config values for views in the client
 * 
 * @author Gavin Cornwell
 */
public class ViewsConfigElement extends ConfigElementAdapter implements Serializable
{
   private static final long serialVersionUID = -503735723795178986L;

   public static final String CONFIG_ELEMENT_ID = "views";

   public static final String VIEW_DETAILS = "details";
   public static final String VIEW_ICONS = "icons";
   public static final String VIEW_LIST = "list";
   public static final String VIEW_BUBBLE = "bubble";
   public static final String SORT_ASCENDING = "ascending";
   public static final String SORT_DESCENDING = "descending";
   
   private static final String SEPARATOR = ":";
   
   // defaults
   private int defaultPageSize = 10;
   private String defaultView = "details";
   private String defaultSortColumn = "name";
   
   // list to store all the configured views
   private List<String> views = new ArrayList<String>(4);

   // map to store all the default views 
   private Map<String, String> defaultViews = new HashMap<String, String>(4);
   
   // map to store all default page sizes for configured client views
   private Map<String, Integer> pageSizes = new HashMap<String, Integer>(10);
   
   // map to store default sort columns for configured views
   private Map<String, String> sortColumns = new HashMap<String, String>(4);
   
   // list of pages that have been configured to have ascending sorts
   private Map<String, String> sortDirections = new HashMap<String, String>(1);
   
   /**
    * Default Constructor
    */
   public ViewsConfigElement()
   {
      super(CONFIG_ELEMENT_ID);
      
      // add the default page sizes to the map
      this.pageSizes.put(VIEW_DETAILS, defaultPageSize);
      this.pageSizes.put(VIEW_LIST, defaultPageSize);
      this.pageSizes.put(VIEW_ICONS, 9);
      this.pageSizes.put(VIEW_BUBBLE, 5);
   }
   
   /**
    * Constructor
    * 
    * @param name Name of the element this config element represents
    */
   public ViewsConfigElement(String name)
   {
      super(name);
   }

   /**
    * @see org.springframework.extensions.config.element.ConfigElementAdapter#getChildren()
    */
   @Override
   public List<ConfigElement> getChildren()
   {
      throw new ConfigException("Reading the views config via the generic interfaces is not supported");
   }
   
   /**
    * @see org.springframework.extensions.config.element.ConfigElementAdapter#combine(org.springframework.extensions.config.ConfigElement)
    */
   public ConfigElement combine(ConfigElement configElement)
   {
      ViewsConfigElement newElement = (ViewsConfigElement)configElement;
      ViewsConfigElement combinedElement = new ViewsConfigElement();
      
      // copy all the config from this element into the new one
      for (String viewImpl : this.views)
      {
         combinedElement.addView(viewImpl);
      }
      
      for (String page : this.defaultViews.keySet())
      {
         combinedElement.addDefaultView(page, this.defaultViews.get(page));
      }
      
      for (String pageView : this.pageSizes.keySet())
      {
         if (pageView.indexOf(SEPARATOR) != -1)
         {
            String page = pageView.substring(0, pageView.indexOf(SEPARATOR));
            String view = pageView.substring(pageView.indexOf(SEPARATOR)+1);
            combinedElement.addDefaultPageSize(page, view, this.pageSizes.get(pageView).intValue());
         }
      }
      
      for (String page : this.sortColumns.keySet())
      {
         combinedElement.addDefaultSortColumn(page, this.sortColumns.get(page));
      }
      
      for (String page : this.sortDirections.keySet())
      {
         combinedElement.addSortDirection(page, this.sortDirections.get(page));
      }
      
      // copy all the config from the element to be combined into the new one
      for (String viewImpl : newElement.getViews())
      {
         combinedElement.addView(viewImpl);
      }
      
      Map<String, String> newDefaultViews = newElement.getDefaultViews();
      for (String page : newDefaultViews.keySet())
      {
         combinedElement.addDefaultView(page, newDefaultViews.get(page));
      }
      
      Map<String, Integer> newPageSizes = newElement.getDefaultPageSizes();
      for (String pageView : newPageSizes.keySet())
      {
         if (pageView.indexOf(SEPARATOR) != -1)
         {
            String page = pageView.substring(0, pageView.indexOf(SEPARATOR));
            String view = pageView.substring(pageView.indexOf(SEPARATOR)+1);
            combinedElement.addDefaultPageSize(page, view, newPageSizes.get(pageView).intValue());
         }
      }
      
      Map<String, String> newSortColumns = newElement.getDefaultSortColumns();
      for (String page : newSortColumns.keySet())
      {
         combinedElement.addDefaultSortColumn(page, newSortColumns.get(page));
      }
      
      Map<String, String> existingSortDirs = newElement.getSortDirections();
      for (String page : existingSortDirs.keySet())
      {
         combinedElement.addSortDirection(page, existingSortDirs.get(page));
      }
      
      return combinedElement;
   }

   /**
    * Adds a configured view
    * 
    * @param renderer The implementation class of the view (the renderer)
    */
   /*package*/ void addView(String renderer)
   {
      this.views.add(renderer);
   }
   
   /**
    * Returns a map of configured views for the client
    * 
    * @return List of the implementation classes for the configured views
    */
   public List<String> getViews()
   {
      return this.views;
   }
   
   /**
    * Adds a default view setting
    * 
    * @param page The page to set the default view for
    * @param view The view name that will be the default
    */
   /*package*/ void addDefaultView(String page, String view)
   {
      this.defaultViews.put(page, view);
   }
   
   /**
    * Returns the default view for the given page
    * 
    * @param page The page to get the default view for
    * @return The defualt view, if there isn't a configured default for the
    *         given page 'details' will be returned
    */
   public String getDefaultView(String page)
   {
      String view = this.defaultViews.get(page);
      
      if (view == null)
      {
         view = this.defaultView;
      }
      
      return view;
   }

   /**
    * Returns a map of default views for each page
    * 
    * @return Map of default views
    */
   /*package*/ Map<String, String> getDefaultViews()
   {
      return this.defaultViews;
   }
   
   /**
    * Adds a configured page size to the internal store
    * 
    * @param page The name of the page i.e. browse, forums etc.
    * @param view The name of the view the size is for i.e. details, icons etc.
    * @param size The size of the page
    */
   /*package*/ void addDefaultPageSize(String page, String view, int size)
   {
      this.pageSizes.put(page + SEPARATOR + view, new Integer(size));
   }
   
   /**
    * Returns the page size for the given page and view combination
    * 
    * @param page The name of the page i.e. browse, forums etc.
    * @param view The name of the view the size is for i.e. details, icons etc.
    * @return The size of the requested page, if the combination doesn't exist
    *         the default for the view will be used, if the view doesn't exist either
    *         10 will be returned.
    */
   public int getDefaultPageSize(String page, String view)
   {
      Integer pageSize = this.pageSizes.get(page + SEPARATOR + view);
      
      // try just the view if the combination isn't present
      if (pageSize == null)
      {
         pageSize = this.pageSizes.get(view);
         
         // if the view is not present either default to 10
         if (pageSize == null)
         {
            pageSize = new Integer(10);
         }
      }
      
      return pageSize.intValue();
   }
   
   /**
    * Returns a map of page sizes
    * 
    * @return Map of page sizes
    */
   /*package*/ Map<String, Integer> getDefaultPageSizes()
   {
      return this.pageSizes;
   }

   /**
    * Adds a default sorting column for the given page 
    * 
    * @param page The name of the page i.e. browse, forums etc.
    * @param column The name of the column to initially sort by
    */
   /*package*/ void addDefaultSortColumn(String page, String column)
   {
      this.sortColumns.put(page, column);
   }
   
   /**
    * Returns the default sort column for the given page
    * 
    * @param page The name of the page i.e. browse, forums etc.
    * @return The name of the column to sort by, name is returned if 
    *         the page is not found
    */
   public String getDefaultSortColumn(String page)
   {
      String column = this.sortColumns.get(page);
      
      if (column == null)
      {
         column = this.defaultSortColumn;
      }
      
      return column;
   }
   
   /**
    * Returns a map of the sorted columns for each page
    * 
    * @return Map of sort columns
    */
   /*package*/ Map<String, String> getDefaultSortColumns()
   {
      return this.sortColumns;
   }
   
   /**
    * Sets the given page as using the given sort direction
    * 
    * @param page The name of the page i.e. browse, forums etc.
    * @param dir The sort direction
    */
   /*package*/ void addSortDirection(String page, String dir)
   {
      this.sortDirections.put(page, dir);
   }
   
   /**
    * Determines whether the given page has been
    * configured to use descending sorting by default
    * 
    * @param page The name of the page i.e. browse, forums etc.
    * @return true if the page should use descending sorts
    */
   public boolean hasDescendingSort(String page)
   {
      boolean usesDescendingSort = false;
      
      String sortDir = this.sortDirections.get(page);
      if (sortDir != null && sortDir.equalsIgnoreCase(SORT_DESCENDING))
      {
         usesDescendingSort = true;
      }
      
      return usesDescendingSort;
   }
   
   /**
    * Returns a map of the sort directions
    * 
    * @return Map of sort directions
    */
   /*package*/ Map<String, String> getSortDirections()
   {
      return this.sortDirections;
   }
}
