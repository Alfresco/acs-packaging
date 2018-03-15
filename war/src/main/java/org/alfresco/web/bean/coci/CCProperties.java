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
package org.alfresco.web.bean.coci;

import java.io.File;
import java.io.Serializable;

import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.coci.CheckOutCheckInService;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.cmr.workflow.WorkflowService;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.Repository;

public class CCProperties implements Serializable
{

    private static final long serialVersionUID = -79530354521757202L;

    /** The VersionOperationsService to be used by the bean */
    transient protected CheckOutCheckInService versionOperationsService;
    
    /** The VersionQueryService to be used by the bean */
    transient protected VersionService versionQueryService;

    /** The ContentService to be used by the bean */
    transient protected ContentService contentService;

    /** The WorkflowService to be used by the bean */
    transient protected WorkflowService workflowService;

    /** Content of the document returned from in-line editing */
    private String editorOutput;

    /** Content of the document used for HTML in-line editing */
    private String documentContent;

    /** The working copy of the document we are checking out */
    private Node workingDocument;

    /** The current document */
    private Node document;

    /** transient form and upload properties */
    private File file;
    private String fileName;
    private String webdavUrl;
    private String cifsPath;
    private boolean keepCheckedOut = false;
    private boolean minorChange = true;
    private boolean isWorkflowAction = false;
    private String workflowTaskId;
    private NodeRef selectedSpaceId = null;
    
    /** constants for copy location selection */
    public static final String COPYLOCATION_CURRENT = "current";
    public static final String COPYLOCATION_OTHER = "other";
    
    private String versionNotes = "";
    private String copyLocation = COPYLOCATION_CURRENT;

    /**
     * @return Returns the VersionOperationsService.
     */
    public CheckOutCheckInService getVersionOperationsService()
    {
       //check for null in cluster environment
       if (versionOperationsService == null)
       {
          versionOperationsService = Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getCheckOutCheckInService();
       }
       return versionOperationsService;
    }

    /**
     * @param versionOperationsService
     *            The VersionOperationsService to set.
     */
    public void setVersionOperationsService(CheckOutCheckInService versionOperationsService)
    {
        this.versionOperationsService = versionOperationsService;
    }
    
    /**
     * @return Returns the VersionQueryService.
     */
    public VersionService getVersionQueryService()
    {
        if (this.versionQueryService == null)
        {
           this.versionQueryService = Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getVersionService();
        }
        
        return this.versionQueryService;
    }

    /**
     * @param versionQueryService
     *            The VersionQueryService to set.
     */
    public void setVersionQueryService(VersionService versionQueryService)
    {
        this.versionQueryService = versionQueryService;
    }

    /**
     * @return Returns the ContentService.
     */
    public ContentService getContentService()
    {
     //check for null in cluster environment
       if (contentService == null)
       {
          contentService = Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getContentService();
       }
       return contentService;
    }

    /**
     * @param contentService
     *            The ContentService to set.
     */
    public void setContentService(ContentService contentService)
    {
        this.contentService = contentService;
    }

    /**
     * @param workflowService
     *            The WorkflowService to set.
     */
    public void setWorkflowService(WorkflowService workflowService)
    {
        this.workflowService = workflowService;
    }

    /**
     * @return the workflowService
     */
    public WorkflowService getWorkflowService()
    {
     //check for null for cluster environment
       if (workflowService == null)
       {
          workflowService = Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getWorkflowService();
       }
       return workflowService;
    }

    /**
     * @return Returns output from the in-line editor page.
     */
    public String getEditorOutput()
    {
        return this.editorOutput;
    }

    /**
     * @param editorOutput
     *            The output from the in-line editor page
     */
    public void setEditorOutput(String editorOutput)
    {
        this.editorOutput = editorOutput;
    }

    /**
     * @return Returns the document content used for HTML in-line editing.
     */
    public String getDocumentContent()
    {
        return this.documentContent;
    }

    /**
     * @param documentContent
     *            The document content for HTML in-line editing.
     */
    public void setDocumentContent(String documentContent)
    {
        this.documentContent = documentContent;
    }

    /**
     * @return Returns the working copy Document.
     */
    public Node getWorkingDocument()
    {
        return this.workingDocument;
    }

    /**
     * @param workingDocument
     *            The working copy Document to set.
     */
    public void setWorkingDocument(Node workingDocument)
    {
        this.workingDocument = workingDocument;
    }

    /**
     * @return The document node being used for the current operation
     */
    public Node getDocument()
    {
        return this.document;
    }

    /**
     * @param document
     *            The document node to be used for the current operation
     */
    public void setDocument(Node document)
    {
        this.document = document;
    }

    /**
     * @return Returns the selected Space Id.
     */
    public NodeRef getSelectedSpaceId()
    {
        return this.selectedSpaceId;
    }

    /**
     * @param selectedSpaceId
     *            The selected Space Id to set.
     */
    public void setSelectedSpaceId(NodeRef selectedSpaceId)
    {
        this.selectedSpaceId = selectedSpaceId;
    }

    /**
     * @return the file
     */
    public File getFile()
    {
        return file;
    }

    /**
     * @param file the file to set
     */
    public void setFile(File file)
    {
        this.file = file;
    }

    /**
     * @return the fileName
     */
    public String getFileName()
    {
        return fileName;
    }

    /**
     * @param fileName the fileName to set
     */
    public void setFileName(String fileName)
    {
        this.fileName = fileName;
    }

    /**
     * @param keepCheckedOut
     *            The keepCheckedOut to set.
     */
    public void setKeepCheckedOut(boolean keepCheckedOut)
    {
        this.keepCheckedOut = keepCheckedOut;
    }

    /**
     * @return Returns the keepCheckedOut.
     */
    public boolean getKeepCheckedOut()
    {
        return this.keepCheckedOut;
    }

    /**
     * @param minorChange
     *            The minorChange to set.
     */
    public void setMinorChange(boolean minorChange)
    {
        this.minorChange = minorChange;
    }

    /**
     * @return Returns the minorChange flag.
     */
    public boolean getMinorChange()
    {
        return this.minorChange;
    }

    /**
     * @return the isWorkflowAction
     */
    public boolean isWorkflowAction()
    {
        return isWorkflowAction;
    }

    /**
     * @param isWorkflowAction the isWorkflowAction to set
     */
    public void setWorkflowAction(boolean isWorkflowAction)
    {
        this.isWorkflowAction = isWorkflowAction;
    }

    /**
     * @return the workflowTaskId
     */
    public String getWorkflowTaskId()
    {
        return workflowTaskId;
    }

    /**
     * @param workflowTaskId the workflowTaskId to set
     */
    public void setWorkflowTaskId(String workflowTaskId)
    {
        this.workflowTaskId = workflowTaskId;
    }
    
    /**
     * @return Returns the version history notes.
     */
    public String getVersionNotes()
    {
        return this.versionNotes;
    }

    /**
     * @param versionNotes
     *            The version history notes to set.
     */
    public void setVersionNotes(String versionNotes)
    {
        this.versionNotes = versionNotes;
    }
    
    /**
     * @return Returns the copy location. Either the current or other space.
     */
    public String getCopyLocation()
    {
        if (this.getFileName() == null || this.getFileName().length() == 0)
        {
        	 return this.copyLocation;
        }
        else
        {
            return CCProperties.COPYLOCATION_OTHER;
        }
    }

    /**
     * @param copyLocation
     *            The copy location. Either the current or other space.
     */
    public void setCopyLocation(String copyLocation)
    {
        this.copyLocation = copyLocation;
    }
    
    /**
     * @return Returns WebDav url for online editing. If webdav online editing didn't yet started, returns null
     */
    public String getWebdavUrl()
    {
        return webdavUrl;
    }

    /**
     * @param webdavUrl The webdav url. Using only for online editing
     */
    public void setWebdavUrl(String webdavUrl)
    {
        this.webdavUrl = webdavUrl;
    }

    /**
     * @return Returns CIFS path for online editing. If cifs online editing didn't yet started, returns null
     */
    public String getCifsPath()
    {
        return cifsPath;
    }

    /**
     * @param cifsPath The cifs path. Using only for online editing
     */
    public void setCifsPath(String cifsPath)
    {
        this.cifsPath = cifsPath;
    }
}
