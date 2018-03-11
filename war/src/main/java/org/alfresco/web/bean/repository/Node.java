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
package org.alfresco.web.bean.repository;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.context.FacesContext;

import org.alfresco.model.ContentModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.lock.LockStatus;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.Path;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.NamespacePrefixResolver;
import org.alfresco.service.namespace.NamespacePrefixResolverProvider;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.web.app.Application;

/**
 * Lighweight client side representation of a node held in the repository. 
 * 
 * @author gavinc
 */
public class Node implements Serializable, NamespacePrefixResolverProvider
{
   private static final long serialVersionUID = 3544390322739034170L;

   protected NodeRef nodeRef;
   protected String name;
   protected QName type;
   protected Path path;
   protected String id;
   protected Set<QName> aspects = null;
   protected Map<String, Boolean> permissions;
   protected Boolean locked = null;
   protected Boolean workingCopyOwner = null;
   protected QNameNodeMap<String, Object> properties;
   protected boolean propsRetrieved = false;
   protected transient ServiceRegistry services = null;
   protected boolean childAssocsRetrieved = false;
   protected QNameNodeMap childAssociations;
   protected boolean assocsRetrieved = false;
   protected QNameNodeMap associations;
   
   private Map<String, Map<String, ChildAssociationRef>> childAssociationsAdded;
   private Map<String, Map<String, ChildAssociationRef>> childAssociationsRemoved;
   private Map<String, Map<String, AssociationRef>> associationsAdded;
   private Map<String, Map<String, AssociationRef>> associationsRemoved;
   
   /**
    * Constructor
    * 
    * @param nodeRef The NodeRef this Node wrapper represents
    */
   public Node(NodeRef nodeRef)
   {
      if (nodeRef == null)
      {
         throw new IllegalArgumentException("NodeRef must be supplied for creation of a Node.");
      }
      
      this.nodeRef = nodeRef;
      this.id = nodeRef.getId();
      
      this.properties = new QNameNodeMap<String, Object>(this, this);
   }

   /**
    * @return All the properties known about this node.
    */
   public Map<String, Object> getProperties()
   {
      if (this.propsRetrieved == false)
      {
         Map<QName, Serializable> props = getServiceRegistry().getNodeService().getProperties(this.nodeRef);
         
         for (QName qname: props.keySet())
         {
            Serializable propValue = props.get(qname);
            
            // Lists returned from the node service could be unmodifiable,
            // therefore create copies for modification purposes
            if (propValue instanceof List)
            {
               propValue = new ArrayList((List)propValue);
            }
            
            this.properties.put(qname.toString(), propValue);
         }
         
         this.propsRetrieved = true;
      }
      
      return this.properties;
   }
   
   /**
    * @return All the associations this node has as a Map, using the association
    *         type as the key
    */
   public final Map getAssociations()
   {
      if (this.assocsRetrieved == false)
      {
         this.associations = new QNameNodeMap(this, this);
         
         List<AssociationRef> assocs = getServiceRegistry().getNodeService().getTargetAssocs(this.nodeRef, RegexQNamePattern.MATCH_ALL);
         
         for (AssociationRef assocRef: assocs)
         {
            String assocName = assocRef.getTypeQName().toString();
            
            List<AssociationRef> list = (List<AssociationRef>)this.associations.get(assocName);
            // create the list if this is first association with 'assocName'
            if (list == null)
            {
               list = new ArrayList<AssociationRef>();
               this.associations.put(assocName, list);
            }
            
            // add the association to the list
            list.add(assocRef);
         }
         
         this.assocsRetrieved = true;
      }
      
      return this.associations;
   }
   
   /**
    * Returns all the associations added to this node in this UI session
    * 
    * @return Map of Maps of AssociationRefs
    */
   public final Map<String, Map<String, AssociationRef>> getAddedAssociations()
   {
      if (this.associationsAdded == null)
      {
         this.associationsAdded = new HashMap<String, Map<String, AssociationRef>>();
      }
      return this.associationsAdded;
   }
   
   /**
    * Returns all the associations removed from this node is this UI session
    * 
    * @return Map of Maps of AssociationRefs
    */
   public final Map<String, Map<String, AssociationRef>> getRemovedAssociations()
   {
      if (this.associationsRemoved == null)
      {
         this.associationsRemoved = new HashMap<String, Map<String, AssociationRef>>();
      }
      return this.associationsRemoved;
   }
   
   /**
    * @return All the child associations this node has as a Map, using the association
    *         type as the key
    */
   public final Map getChildAssociations()
   {
      if (this.childAssocsRetrieved == false)
      {
         this.childAssociations = new QNameNodeMap(this, this);
         
         List<ChildAssociationRef> assocs = getServiceRegistry().getNodeService().getChildAssocs(this.nodeRef);
         
         for (ChildAssociationRef assocRef: assocs)
         {
            String assocName = assocRef.getTypeQName().toString();
            
            List list = (List)this.childAssociations.get(assocName);
            // create the list if this is first association with 'assocName'
            if (list == null)
            {
               list = new ArrayList<ChildAssociationRef>();
               this.childAssociations.put(assocName, list);
            }
            
            // add the association to the list
            list.add(assocRef);
         }
         
         this.childAssocsRetrieved = true;
      }
      
      return this.childAssociations;
   }
   
   /**
    * Returns all the child associations added to this node in this UI session
    * 
    * @return Map of Maps of ChildAssociationRefs
    */
   public final Map<String, Map<String, ChildAssociationRef>> getAddedChildAssociations()
   {
      if (this.childAssociationsAdded == null)
      {
         this.childAssociationsAdded = new HashMap<String, Map<String, ChildAssociationRef>>();
      }
      return this.childAssociationsAdded;
   }
   
   /**
    * Returns all the child associations removed from this node is this UI session
    * 
    * @return Map of Maps of ChildAssociationRefs
    */
   public final Map<String, Map<String, ChildAssociationRef>> getRemovedChildAssociations()
   {
      if (this.childAssociationsRemoved == null)
      {
         this.childAssociationsRemoved = new HashMap<String, Map<String, ChildAssociationRef>>();
      }
      return this.childAssociationsRemoved;
   }
   
   /**
    * Register a property resolver for the named property.
    * 
    * @param name       Name of the property this resolver is for
    * @param resolver   Property resolver to register
    */
   public final void addPropertyResolver(String name, NodePropertyResolver resolver)
   {
      this.properties.addPropertyResolver(name, resolver);
   }
   
   /**
    * Returns if a property resolver with a specific name has been applied to the Node
    *  
    * @param name of property resolver to look for
    * 
    * @return true if a resolver with the name is found, false otherwise
    */
   public final boolean containsPropertyResolver(String name)
   {
      return this.properties.containsPropertyResolver(name);
   }
   
   /**
    * Determines whether the given property name is held by this node 
    * 
    * @param propertyName Property to test existence of
    * @return true if property exists, false otherwise
    */
   public final boolean hasProperty(String propertyName)
   {
      return getProperties().containsKey(propertyName);
   }

   /**
    * @return Returns the NodeRef this Node object represents
    */
   public final NodeRef getNodeRef()
   {
      return this.nodeRef;
   }
   
   /**
    * @return Returns the string form of the NodeRef this Node represents
    */
   public final String getNodeRefAsString()
   {
      return this.nodeRef.toString();
   }
   
   /**
    * @return Returns the type.
    */
   public QName getType()
   {
      if (this.type == null)
      {
         this.type = getServiceRegistry().getNodeService().getType(this.nodeRef);
      }
      
      return type;
   }
   
   /**
    * @return The display name for the node
    */
   public String getName()
   {
      if (this.name == null)
      {
         // try and get the name from the properties first
         this.name = (String)getProperties().get("cm:name");
         
         // if we didn't find it as a property get the name from the association name
         if (this.name == null)
         {
            this.name = getServiceRegistry().getNodeService().getPrimaryParent(this.nodeRef).getQName().getLocalName(); 
         }
      }
      
      return this.name;
   }

   /**
    * @return The list of aspects applied to this node
    */
   public final Set<QName> getAspects()
   {
      if (this.aspects == null)
      {
         this.aspects = getServiceRegistry().getNodeService().getAspects(this.nodeRef);
      }
      
      return this.aspects;
   }
   
   /**
    * @param aspect The aspect to test for
    * @return true if the node has the aspect false otherwise
    */
   public final boolean hasAspect(QName aspect)
   {
      Set aspects = getAspects();
      return aspects.contains(aspect);
   }
   
   /**
    * Return whether the current user has the specified access permission on this Node
    * 
    * @param permission     Permission to validate against
    * 
    * @return true if the permission is applied to the node for this user, false otherwise
    */
   public boolean hasPermission(String permission)
   {
      Boolean valid = null;
      if (this.permissions != null)
      {
         valid = this.permissions.get(permission);
      }
      else
      {
         this.permissions = new HashMap<String, Boolean>(8, 1.0f);
      }
      
      if (valid == null)
      {
         PermissionService service = Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getPermissionService();
         valid = Boolean.valueOf(service.hasPermission(this.nodeRef, permission) == AccessStatus.ALLOWED);
         this.permissions.put(permission, valid);
      }
      
      return valid.booleanValue();
   }

   /**
    * @return The GUID for the node
    */
   public final String getId()
   {
      return this.id;
   }

   /**
    * @return The simple display path for the node
    */
   public String getPath()
   {
      return getNodePath().toString();
   }
   
   /**
    * @return the repo Path to the node
    */
   public Path getNodePath()
   {
      if (this.path == null)
      {
         this.path = getServiceRegistry().getNodeService().getPath(this.nodeRef);
      }
      return this.path;
   }
   
   /**
    * @return If the node is currently locked
    */
   public final boolean isLocked()
   {
      if (this.locked == null)
      {
         this.locked = Boolean.FALSE;
         
         if (hasAspect(ContentModel.ASPECT_LOCKABLE))
         {
            locked = getServiceRegistry().getLockService().isLocked(getNodeRef());
         }
      }
      
      return this.locked.booleanValue();
   }
   
   /**
    * @return whether a the Node is a WorkingCopy owned by the current User
    */
   public final boolean isWorkingCopyOwner()
   {
      if (this.workingCopyOwner == null)
      {
         this.workingCopyOwner = Boolean.FALSE;
         
         if (hasAspect(ContentModel.ASPECT_WORKING_COPY))
         {
            Object obj = getProperties().get(ContentModel.PROP_WORKING_COPY_OWNER);
            if (obj instanceof String)
            {
               User user = Application.getCurrentUser(FacesContext.getCurrentInstance());
               if ( ((String)obj).equals(user.getUserName()))
               {
                  this.workingCopyOwner = Boolean.TRUE;
               }
            }
         }
      }
      
      return workingCopyOwner.booleanValue();
   }
   
   /**
    * Resets the state of the node to force re-retrieval of the data
    */
   public void reset()
   {
      this.name = null;
      this.type = null;
      this.path = null;
      this.locked = null;
      this.workingCopyOwner = null;
      this.properties.clear();
      this.propsRetrieved = false;
      this.aspects = null;
      this.permissions = null;
      
      this.associations = null;
      this.associationsAdded = null;
      this.associationsRemoved = null;
      this.assocsRetrieved = false;
      
      this.childAssociations = null;
      this.childAssociationsAdded = null;
      this.childAssociationsRemoved = null;
      this.childAssocsRetrieved = false;
   }
   
   /**
    * Override Object.toString() to provide useful debug output
    */
   public String toString()
   {
      if (getServiceRegistry().getNodeService() != null)
      {
         if (getServiceRegistry().getNodeService().exists(nodeRef))
         {
            return "Node Type: " + getType() + 
                   "\nNode Properties: " + this.getProperties().toString() + 
                   "\nNode Aspects: " + this.getAspects().toString();
         }
         else
         {
            return "Node no longer exists: " + nodeRef;
         }
      }
      else
      {
         return super.toString();
      }
   }
   
   protected ServiceRegistry getServiceRegistry()
   {
      if (this.services == null)
      {
          this.services = Repository.getServiceRegistry(FacesContext.getCurrentInstance());
      }
      return this.services;
   }
   
   public NamespacePrefixResolver getNamespacePrefixResolver()
   {
      return getServiceRegistry().getNamespaceService();
   }
}
