/*
 * Copyright (c) 2013. Knowledge Media Institute - The Open University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.open.kmi.iserve.sal.manager;

import com.google.common.collect.Multimap;
import uk.ac.open.kmi.iserve.sal.exception.ServiceException;
import uk.ac.open.kmi.msm4j.Service;

import java.net.URI;
import java.util.Set;

/**
 * Interface that defines the operations a Service Manager should offer
 * If other implementations backed by different types of storage systems are
 * needed we should update this.
 *
 * @author <a href="mailto:carlos.pedrinaci@open.ac.uk">Carlos Pedrinaci</a> (KMi - The Open University)
 * @author <a href="mailto:luca.panziera@open.ac.uk">Luca Panziera</a> (KMi - The Open University)
 */
public interface ServiceManager extends iServeComponent {

    // Create Methods

    /**
     * Creates a Service Description in the system.
     * Only needs to be fed with an MSM Service description.
     * <p/>
     * After successfully adding a service, implementations of this method should raise a {@code ServiceCreatedEvent}
     *
     * @param service the input service description in terms of MSM
     * @return the URI this service description was saved to
     * @throws ServiceException
     */
    public URI addService(Service service) throws ServiceException;

    // Update Methods

    /**
     * Relates a given document to a service. The relationship is just a generic one.
     *
     * @param serviceUri      the service URI
     * @param relatedDocument the related document URI
     * @return True if successful or False otherwise
     * @throws uk.ac.open.kmi.iserve.sal.exception.SalException
     *
     */
    public abstract boolean addRelatedDocumentToService(URI serviceUri, URI relatedDocument) throws ServiceException;

    // Read Methods

    /**
     * Obtains the service description of the service identified by the URI
     *
     * @param serviceUri the URI of the service to obtain
     * @return the service description if it exists or null otherwise
     * @throws ServiceException
     */
    public abstract Service getService(URI serviceUri)
            throws ServiceException;

    /**
     * Obtains the service descriptions for all the services identified by the URI Set
     *
     * @param serviceUris the URIs of the service to obtain
     * @return the list of all services that could be obtained. If none could be obtained the list will be empty.
     * @throws ServiceException
     */
    public abstract Set<Service> getServices(Set<URI> serviceUris)
            throws ServiceException;

    // Listing Methods

    /**
     * Obtains a list of service URIs with all the services known to the system
     *
     * @return list of URIs with all the services in the registry
     */
    public abstract Set<URI> listServices();

    /**
     * Obtains the list of operation URIs for a given Operation
     *
     * @param serviceUri the service URI
     * @return a Set of URIs with the operations provided by the service. If there are no operations, the Set should be empty NOT null.
     */
    public abstract Set<URI> listOperations(URI serviceUri);

    /**
     * Obtains the list of input URIs for a given Operation
     *
     * @param operationUri the operation URI
     * @return a Set of URIs with the inputs of the operation. If no input is necessary the Set should be empty NOT null.
     */
    public abstract Set<URI> listInputs(URI operationUri);

    /**
     * Given an operation, this method obtains the list of input URIs mapped to their annotated types (i.e., modelReferences).
     * Note that the same input may have several annotations indicating the type.
     *
     * @param operationUri the URI for which we want to obtain the inputs and their annotated types
     * @return a Multimap with the inputs and their corresponding types.
     */
    public abstract Multimap<URI, URI> listTypedInputs(URI operationUri);

    /**
     * Obtains the list of output URIs for a given Operation
     *
     * @param operationUri the operation URI
     * @return a Set of URIs with the outputs of the operation. If no output is provided the Set should be empty NOT null.
     */
    public abstract Set<URI> listOutputs(URI operationUri);

    /**
     * Given an operation, this method obtains the list of output URIs mapped to their annotated types (i.e., modelReferences).
     * Note that the same input may have several annotations indicating the type.
     *
     * @param operationUri the URI for which we want to obtain the outputs and their annotated types
     * @return a Multimap with the outputs and their corresponding types.
     */
    public abstract Multimap<URI, URI> listTypedOutputs(URI operationUri);

    /**
     * Obtains the list of mandatory parts for a given Message Content
     *
     * @param messageContent the message content URI
     * @return a Set of URIs with the mandatory parts of the message content. If there are no parts the Set should be empty NOT null.
     */
    public abstract Set<URI> listMandatoryParts(URI messageContent);

    /**
     * Obtains the list of optional parts for a given Message Content
     *
     * @param messageContent the message content URI
     * @return a Set of URIs with the optional parts of the message content. If there are no parts the Set should be empty NOT null.
     */
    public abstract Set<URI> listOptionalParts(URI messageContent);

    /**
     * Obtains the list of model references for a given element.
     *
     * @param elementUri the URI of the element for which we want to obtain the model references
     * @return a Set of URIs with the model references of the given element. If there are no model references the Set should be empty NOT null.
     */
    public abstract Set<URI> listModelReferences(URI elementUri);

    /**
     * Given a modelReference, this method finds all the elements that have been annotated with it. This method finds
     * exact annotations. Note that the elements can be services, operations, inputs, etc.
     *
     * @param modelReference the actual annotation we are looking for
     * @return a set of URIs for elements that have been annotated with the requested modelReferences.
     */
    public abstract Set<URI> listElementsAnnotatedWith(URI modelReference);

    /**
     * Given the URI of a type (i.e., a modelReference), this method figures out all the operations
     * that have this as part of their inputs.
     *
     * @param modelReference the type of input sought for
     * @return a Set of URIs of operations that can take this as input type.
     */
    public abstract Set<URI> listOperationsWithInputType(URI modelReference);

    /**
     * Given the URI of a type (i.e., a modelReference), this method figures out all the operations
     * that have this as part of their inputs.
     *
     * @param modelReference the type of output sought for
     * @return a Set of URIs of operations that generate this output type.
     */
    public abstract Set<URI> listOperationsWithOutputType(URI modelReference);

    /**
     * Given the URI of a type (i.e., a modelReference), this method figures out all the services
     * that have this as part of their inputs.
     *
     * @param modelReference the type of input sought for
     * @return a Set of URIs of services that can take this as input type.
     */
    public abstract Set<URI> listServicesWithInputType(URI modelReference);

    /**
     * Given the URI of a type (i.e., a modelReference), this method figures out all the services
     * that have this as part of their inputs.
     *
     * @param modelReference the type of output sought for
     * @return a Set of URIs of services that generate this output type.
     */
    public abstract Set<URI> listServicesWithOutputType(URI modelReference);

    /**
     * Lists all documents related to a given service
     *
     * @param serviceUri the service URI
     * @return the Set of URIs of all the documents related to the service
     * @throws ServiceException
     */
    public abstract Set<URI> listDocumentsForService(URI serviceUri) throws ServiceException;

    /**
     * Given the URI of an element, this method returns the URI of the elements from the Minimal Service Model it
     * corresponds to. That is, it will say if it is a service, operation, etc. Note that in the case of messages
     * parts, the entities will have two types.
     *
     * @param elementUri the URI to get the element type for.
     * @return the URI of the MSM type it corresponds to.
     */
    public abstract Set<URI> getMsmType(URI elementUri);

    // Delete Methods

    /**
     * Deletes the given service
     * <p/>
     * After successfully deleting a service, implementations of this method should raise a {@code ServiceDeletedEvent}
     *
     * @param serviceUri the URI of the service to delete
     * @return True if it was deleted or false otherwise
     * @throws ServiceException
     */
    public abstract boolean deleteService(URI serviceUri)
            throws ServiceException;

    /**
     * Deletes the given service
     * <p/>
     * After successfully deleting a service, implementations of this method should raise a {@code ServiceDeletedEvent}
     *
     * @param service the service to delete
     * @return True if it was deleted or false otherwise
     * @throws ServiceException
     */
    public abstract boolean deleteService(Service service)
            throws ServiceException;

    // General Management Methods

    /**
     * Deletes all the services on the registry.
     * This operation cannot be undone. Use with care.
     * <p/>
     * After successfully clearing the services, implementations of this method should raise a {@code ServicesClearedEvent}
     *
     * @return true if the service registry was cleared.
     * @throws ServiceException
     */
    public abstract boolean clearServices()
            throws ServiceException;

    /**
     * Determines whether a service is known to the registry
     *
     * @param serviceUri the URI of the service being looked up
     * @return True if it is registered in the server
     * @throws ServiceException
     */
    public abstract boolean serviceExists(URI serviceUri) throws ServiceException;

    /**
     * Given the URI of a modelReference, this method figures out all the services
     * that have this as model references.
     *
     * @param modelReference the type of output sought for
     * @return a Set of URIs of operations that generate this output type.
     */
    public abstract Set<URI> listServicesWithModelReference(URI modelReference);

    /**
     * Given the URI of a modelReference, this method figures out all the operations
     * of which services have this as model references.
     *
     * @param modelReference the type of output sought for
     * @return a Set of URIs of operations that generate this output type.
     */
    public abstract Set<URI> listOperationsWithModelReference(URI modelReference);
}