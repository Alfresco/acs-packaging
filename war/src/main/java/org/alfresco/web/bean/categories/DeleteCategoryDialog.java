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
package org.alfresco.web.bean.categories;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.CategoryService;
import org.alfresco.service.cmr.search.CategoryService.Depth;
import org.alfresco.service.cmr.search.CategoryService.Mode;
import org.alfresco.web.app.Application;
import org.alfresco.web.app.context.UIContextService;
import org.alfresco.web.bean.categories.CategoriesDialog.CategoryBreadcrumbHandler;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.Repository;
import org.alfresco.web.ui.common.ReportedException;
import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.common.component.IBreadcrumbHandler;
import org.alfresco.web.ui.common.component.data.UIRichList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.extensions.surf.util.ParameterCheck;

public class DeleteCategoryDialog extends BaseDialogBean
{
    private static final long serialVersionUID = -8929785826091612856L;
    
    private static Log logger = LogFactory.getLog(DeleteCategoryDialog.class);
    
    private static final String DEFAULT_OUTCOME = "finish";
    private final static String MSG_DELETE_CATEGORY = "delete_category";
    private final static String MSG_DELETE = "delete";
    private final static String MSG_LEFT_QUOTE = "left_qoute";
    private final static String MSG_RIGHT_QUOTE = "right_quote";    
    private static final String MSG_CATEGORIES = "categories";
    
    transient protected CategoryService categoryService;
    
    /** Dialog properties */
    private String name = null;
    private String description = null;
    
    /** Category path breadcrumb location */
    private List<IBreadcrumbHandler> location = null;
    
    /** Members of the linked items of a category */
    private Collection<ChildAssociationRef> members = null;
    
    /** Current category ref */
    private NodeRef categoryRef = null;
    
    /** Action category node */
    private Node actionCategory = null;
    
    /** Component references */
    protected UIRichList categoriesRichList;
    
    /** Currently visible category Node */
    private Node category = null;
    
    private Boolean categoryFlag = false;
    
    
    @Override
    public void init(Map<String, String> parameters)
    {
       this.isFinished = false;
       this.categoryFlag = false;
       
       // retrieve parameters
       String categoryRef = parameters.get(CategoriesDialog.PARAM_CATEGORY_REF);
       
       // make sure nodeRef was supplied
       ParameterCheck.mandatoryString(CategoriesDialog.PARAM_CATEGORY_REF, categoryRef);
       
       // create the node
       this.category = new Node(new NodeRef(categoryRef));
       
       setActionCategory(category);
    }
    
    @Override
    protected String doPostCommitProcessing(FacesContext context, String outcome)
    {
       // add the category to the request object so it gets picked up by
       // category dialog, this will allow it to be removed from the breadcrumb
       context.getExternalContext().getRequestMap().put(
             CategoriesDialog.KEY_CATEGORY, this.category.getName());
       context.getExternalContext().getRequestMap().put(
             CategoriesDialog.KEY_CATEGORY_FLAG, this.categoryFlag.toString());
       
       return outcome;
    }
    
    public Collection<ChildAssociationRef> getMembers()
    {
        return members;
    }

    public void setMembers(Collection<ChildAssociationRef> members)
    {
        this.members = members;
    }
    
    public int getMembersSize()
    {
        return getMembers().size();
    }
    
    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }
    
    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }
    
    /**
     * @return the categoryService
     */
    private CategoryService getCategoryService()
    {
       //check for null in cluster environment
       if(categoryService == null)
       {
          categoryService = Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getCategoryService();
       }
       return categoryService;
    }

    public void setCategoryService(CategoryService categoryService)
    {
        this.categoryService = categoryService;
    }
    
    public NodeRef getCategoryRef()
    {
        return categoryRef;
    }

    public void setCategoryRef(NodeRef categoryRef)
    {
        this.categoryRef = categoryRef;
    }
    
    public Node getCategory()
    {
        return category;
    }

    public void setCategory(Node category)
    {
        this.category = category;
    }
    
    public Boolean getCategoryFlag()
    {
        return categoryFlag;
    }

    public void setCategoryFlag(Boolean categoryFlag)
    {
        this.categoryFlag = categoryFlag;
    }
    
    /**
     * @param node    Set the Node to be used for the current category screen action.
     */
    @SuppressWarnings("unchecked")
    public void setActionCategory(Node node)
    {
       this.actionCategory = node;
       
       if (node != null)
       {
          // setup form properties
          setName(node.getName());
          setDescription((String)node.getProperties().get(ContentModel.PROP_DESCRIPTION));
          setMembers(getCategoryService().getChildren(node.getNodeRef(), Mode.MEMBERS, Depth.ANY));
       }
       else
       {
          setName(null);
          setDescription(null);
          Object emptyCollection = Collections.emptyList();
          setMembers((Collection<ChildAssociationRef>) emptyCollection);
       }
    }
    
    public Node getActionCategory()
    {
        return actionCategory;
    }
    
    public void setLocation(List<IBreadcrumbHandler> location)
    {
        this.location = location;
    }
    
    /**
     * @return Breadcrumb location list
     */
    public List<IBreadcrumbHandler> getLocation()
    {
       return this.location;
    }
    
    @Override
    public boolean getFinishButtonDisabled()
    {
        return false;
    }
    
    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Exception
    {
        finishDelete();
        return outcome;
    }

    @Override
    public String getContainerTitle()
    {
        FacesContext fc = FacesContext.getCurrentInstance();
        return Application.getMessage(fc, MSG_DELETE_CATEGORY) + " " + Application.getMessage(fc, MSG_LEFT_QUOTE)
                + getActionCategory().getName() + Application.getMessage(fc, MSG_RIGHT_QUOTE);
    }

    @Override
    public String getFinishButtonLabel()
    {

        return Application.getMessage(FacesContext.getCurrentInstance(), MSG_DELETE);
    }
    
    public UIRichList getCategoriesRichList()
    {
        return categoriesRichList;
    }

    public void setCategoriesRichList(UIRichList categoriesRichList)
    {
        this.categoriesRichList = categoriesRichList;
    }

    /**
     * @see org.alfresco.web.app.context.IContextListener#contextUpdated()
     */
    public void contextUpdated()
    {
       if (logger.isDebugEnabled())
          logger.debug("Invalidating Category Management Components...");
       
       // force a requery of the current category ref properties
       setCategory(null);
       
       // force a requery of the richlist dataset
       if (this.categoriesRichList != null)
       {
          this.categoriesRichList.setValue(null);
       }
       
    }
    
    /**
     * @return The ID of the currently displayed category or null if at the root.
     */
    public String getCurrentCategoryId()
    {
       if (getCategoryRef() != null)
       {
          return getCategoryRef().getId();
       }
       else
       {
          return null;
       }
    }
    
    /**
     * Set the current category node.
     * <p>
     * Setting this value causes the UI to update and display the specified node as current.
     * 
     * @param ref     The current category node.
     */
    public void setCurrentCategory(NodeRef ref)
    {
       if (logger.isDebugEnabled())
          logger.debug("Setting current category: " + ref);
       
       // set the current NodeRef for our UI context operations
       setCategoryRef(ref);
       
       // clear current node context
       setCategory(null);
       
       // inform that the UI needs updating after this change 
       contextUpdated();
    }
    
    
    public String finishDelete()
    {
        String outcome = DEFAULT_OUTCOME;

        if (getActionCategory() != null)
        {
            try
            {
                FacesContext context = FacesContext.getCurrentInstance();
                RetryingTransactionHelper txnHelper = Repository.getRetryingTransactionHelper(context);
                RetryingTransactionCallback<NodeRef> callback = new RetryingTransactionCallback<NodeRef>()
                {
                    @SuppressWarnings("unchecked")
                    public NodeRef execute() throws Throwable
                    {
                        // delete the category node using the nodeservice
                        NodeRef categoryNodeRef = getActionCategory().getNodeRef();
                        getCategoryService().deleteCategory(categoryNodeRef);

                        // if there are other items in the repository using this category
                        // all the associations to the category should be removed too
                        if (getMembers() != null && getMembers().size() > 0)
                        {
                            for (ChildAssociationRef childRef : getMembers())
                            {
                                List<NodeRef> list = new ArrayList<NodeRef>(getMembers().size());

                                NodeRef member = childRef.getChildRef();
                                Collection<NodeRef> categories = (Collection<NodeRef>)getNodeService().getProperty(member, ContentModel.PROP_CATEGORIES);

                                for (NodeRef category : categories)
                                {
                                    if (category.equals(categoryNodeRef) == false)
                                    {
                                        list.add(category);
                                    }
                                }

                                // persist the list back to the repository
                                getNodeService().setProperty(member, ContentModel.PROP_CATEGORIES, (Serializable) list);
                            }
                        }
                        return categoryNodeRef;
                    }
                };
                NodeRef categoryNodeRef = txnHelper.doInTransaction(callback);
                
                // Figure out if the deletion is made by an icon or by a list of actions
                CategoriesDialog categoriesDialog = (CategoriesDialog) UIContextService.getInstance(FacesContext.getCurrentInstance())
                        .getRegisteredBean(CategoriesDialog.CATEGORIES_DIALOG_CLASS_NAME);
                setLocation(categoriesDialog.getLocation());
                List<IBreadcrumbHandler> location = getLocation();
                CategoryBreadcrumbHandler handler = (CategoryBreadcrumbHandler) location.get(location.size() - 1);
                setCategoryFlag(!handler.toString().equals(getCategory().getName()));
                
                // clear action context
                setActionCategory(null);
            }
            catch (Throwable err)
            {
                Utils.addErrorMessage(MessageFormat.format(Application.getMessage(FacesContext.getCurrentInstance(), Repository.ERROR_GENERIC), err.getMessage()), err);
                outcome = null;
                ReportedException.throwIfNecessary(err);
            }
        }

        return outcome;
    }

}
