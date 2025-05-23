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

package uk.ac.open.kmi.iserve.sal.manager.impl;

import com.google.common.collect.ImmutableSet;
import com.google.common.eventbus.EventBus;
import org.apache.commons.configuration.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.open.kmi.iserve.core.SystemConfiguration;
import uk.ac.open.kmi.iserve.sal.exception.SalException;
import uk.ac.open.kmi.iserve.sal.exception.ServiceException;
import uk.ac.open.kmi.iserve.sal.manager.*;
import uk.ac.open.kmi.iserve.sal.util.UriUtil;
import uk.ac.open.kmi.msm4j.Service;
import uk.ac.open.kmi.msm4j.io.MediaType;
import uk.ac.open.kmi.msm4j.io.ServiceReader;
import uk.ac.open.kmi.msm4j.io.Syntax;
import uk.ac.open.kmi.msm4j.io.TransformationException;
import uk.ac.open.kmi.msm4j.io.impl.ServiceReaderImpl;
import uk.ac.open.kmi.msm4j.io.impl.ServiceTransformationEngine;
import uk.ac.open.kmi.msm4j.io.util.FilenameFilterBySyntax;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * iServe Manager implementation providing a facade to the entire Storage and Access Layer
 * Delegates the actual implementation to each of the specific managers configured.
 * This class implements the Singleton pattern and takes care of all dependency injection transparently for the user.
 *
 * @author Carlos Pedrinaci (Knowledge Media Institute - The Open University)
 */
public class RegistryManagerImpl extends IntegratedComponent implements RegistryManager {

    private static final Logger log = LoggerFactory.getLogger(RegistryManagerImpl.class);

    private final DocumentManager docManager;
    private final ServiceManager serviceManager;
    private final KnowledgeBaseManager kbManager;
    private final ServiceTransformationEngine serviceTransformationEngine;

    @Inject
    private RegistryManagerImpl(EventBus eventBus,
                                @Named(SystemConfiguration.ISERVE_URL_PROP) String iServeUri,
                                DocumentManager docManager,
                                ServiceManager serviceManager,
                                KnowledgeBaseManager kbManager,
                                ServiceTransformationEngine serviceTransformationEngine) throws ConfigurationException, SalException {

        super(eventBus, iServeUri);
        this.docManager = docManager;
        this.serviceManager = serviceManager;
        this.kbManager = kbManager;
        this.serviceTransformationEngine = serviceTransformationEngine;
    }

    /**
     * Register the given object as an Observer for this iServe instance.
     * All events created within iServe would be notified. Currently based on Guava's EventBus.
     * In order to process them, the corresponding observer should only implement a method that takes as sole parameter
     * the event wanted to process. This method should additionally be annotated with {@code @Subscribe} for the system
     * to work.
     *
     * @param obj The observer
     */
    @Override
    public void registerAsObserver(Object obj) {
        this.getEventBus().register(obj);
    }

    /**
     * Unregister observer. Events won't be notified to this observer any longer.
     *
     * @param obj The observer
     */
    @Override
    public void unregisterObserver(Object obj) {
        this.getEventBus().unregister(obj);
    }

    /**
     * Obtains the Service Manager for this instance of iServe
     *
     * @return the Service Manager
     */
    @Override
    public ServiceManager getServiceManager() {
        return this.serviceManager;
    }

    /**
     * Obtains the Knowledge Base Manager for this instance of iServe
     *
     * @return the Knowledge Base Manager
     */
    @Override
    public KnowledgeBaseManager getKnowledgeBaseManager() {
        return this.kbManager;
    }

    /**
     * Obtains the Document Manager for this instance of iServe
     *
     * @return the Document Manager
     */
    @Override
    public DocumentManager getDocumentManager() {
        return this.docManager;
    }

    /**
     * Obtains the Service Transformer for this instance
     *
     * @return the transformer
     */
    @Override
    public ServiceTransformationEngine getServiceTransformationEngine() {
        return this.serviceTransformationEngine;
    }

    /**
     * This method will be called when the server is being shutdown.
     * Ensure a clean shutdown.
     */
    @Override
    public void shutdown() {
        this.serviceManager.shutdown();
        this.docManager.shutdown();
        this.kbManager.shutdown();
    }

    /**
     * Obtains the list of supported media types the engine can import
     *
     * @return the Set of media types
     */
    @Override
    public Set<String> getSupportedInputMediaTypes() {
        ImmutableSet.Builder<String> result = ImmutableSet.builder();
        result.addAll(MediaType.NATIVE_MEDIATYPE_SYNTAX_MAP.keySet());
        result.addAll(this.serviceTransformationEngine.getSupportedMediaTypes());
        return result.build();
    }

    /**
     * Checks if the given media type can be imported
     *
     * @param mediaType the media type we wish to check if it can be imported
     * @return true if it is supported or false otherwise.
     */
    @Override
    public boolean canImport(String mediaType) {
        return (MediaType.NATIVE_MEDIATYPE_SYNTAX_MAP.containsKey(mediaType) ||
                this.serviceTransformationEngine.canTransform(mediaType));
    }

    /**
     * Gets the corresponding filename filter to a given media type by checking both native formats
     * and those supported through transformation
     *
     * @param mediaType the media type for which to obtain the file extension
     * @return the filename filter or null if it is not supported. Callers are advised to check
     * first that the media type is supported {@see canTransform} .
     */
    @Override
    public FilenameFilter getFilenameFilter(String mediaType) {
        if (mediaType == null) {
            return null;
        }

        if (MediaType.NATIVE_MEDIATYPE_SYNTAX_MAP.containsKey(mediaType)) {
            return new FilenameFilterBySyntax(MediaType.NATIVE_MEDIATYPE_SYNTAX_MAP.get(mediaType));
        } else if (this.getServiceTransformationEngine().canTransform(mediaType)) {
            return this.getServiceTransformationEngine().getFilenameFilter(mediaType);
        }

        return null;
    }

    /**
     * Imports a new service within iServe. The original document is stored
     * in the server and the transformed version registered within iServe.
     *
     * @param servicesContentStream
     * @param mediaType
     * @return the List of URIs of the services imported
     * @throws SalException
     */
    @Override
    public List<URI> importServices(InputStream servicesContentStream,
                                    String mediaType) throws SalException {

        boolean isNativeFormat = MediaType.NATIVE_MEDIATYPE_SYNTAX_MAP.containsKey(mediaType);
        // Throw error if Format Unsupported
        if (!isNativeFormat && !this.serviceTransformationEngine.canTransform(mediaType)) {
            log.error("The media type {} is not natively supported and has no suitable transformer.", mediaType);
            throw new ServiceException("Unable to import service. Format unsupported.");
        }

        // Obtain the file extension to use
        String fileExtension = findFileExtensionToUse(mediaType, isNativeFormat);

        List<Service> services = null;
        List<URI> importedServices = new ArrayList<URI>();
        URI sourceDocUri = null;
        InputStream localStream = null;
        try {
            // 1st Store the document
            sourceDocUri = this.docManager.createDocument(servicesContentStream, fileExtension);
            if (sourceDocUri == null) {
                throw new ServiceException("Unable to save service document. Operation aborted.");
            }

            // 2nd Parse and Transform the document
            // The original stream may be a one-of stream so save it first and read locally
            localStream = this.docManager.getDocument(sourceDocUri);
            services = getServicesFromStream(mediaType, isNativeFormat, localStream);

            // 3rd - Store the resulting MSM services. There may be more than one
            URI serviceUri = null;
            if (services != null && !services.isEmpty()) {
                log.info("Importing {} services", services.size());
                for (Service service : services) {
                    // The service is being imported -> update the source
                    service.setSource(sourceDocUri);
                    serviceUri = this.serviceManager.addService(service);
                    if (serviceUri != null) {
                        importedServices.add(serviceUri);
                    }
                }
            }

            // 4th Log it was all done correctly
            // TODO: log to the system and notify observers
            log.info("Source document imported: {}", sourceDocUri.toASCIIString());

        } finally {
            // Rollback if something went wrong
            if ((services == null || (services != null && services.size() != importedServices.size()))
                    && sourceDocUri != null) {
                this.docManager.deleteDocument(sourceDocUri);
                for (URI svcUri : importedServices) {
                    this.serviceManager.deleteService(svcUri);
                }
                log.warn("There were problems importing the service. Changes undone.");
            }

            if (localStream != null)
                try {
                    localStream.close();
                } catch (IOException e) {
                    log.error("Error closing the service content stream", e);
                }
        }

        return importedServices;
    }

    private List<Service> getServicesFromStream(String mediaType, boolean nativeFormat, InputStream localStream) throws ServiceException {
        if (localStream == null)
            throw new ServiceException("Unable to parse the saved document. Operation aborted.");

        List<Service> services;
        if (nativeFormat) {
            // Parse it directly
            ServiceReader reader = new ServiceReaderImpl();
            Syntax syntax = MediaType.NATIVE_MEDIATYPE_SYNTAX_MAP.get(mediaType);
            services = reader.parse(localStream, null, syntax);
        } else {
            // Its an external format: use the appropriate importer
            // We should have a suitable importer
            try {
                services = this.serviceTransformationEngine.transform(localStream, mediaType);
            } catch (TransformationException e) {
                throw new ServiceException("Errors transforming the service", e);
            }
        }
        log.debug("Services parsed:", services);
        return services;
    }

    private String findFileExtensionToUse(String mediaType, boolean nativeFormat) {
        String fileExtension = null;
        if (nativeFormat) {
            fileExtension = MediaType.NATIVE_MEDIATYPE_SYNTAX_MAP.get(mediaType).getExtension();
            log.debug("The media type is natively supported. File extension {}", fileExtension);
        } else {
            // should not be null since it is supported
            fileExtension = this.serviceTransformationEngine.getFileExtension(mediaType);
            log.debug("The media type is supported through transformations. File extension {}", fileExtension);
        }
        return fileExtension;
    }

    /**
     * Clears the registry entirely: all documents and services are deleted
     * This operation cannot be undone. Use with care.
     *
     * @return true if the registry was cleared.
     * @throws SalException
     */
    @Override
    public boolean clearRegistry() throws SalException {
        boolean result = this.serviceManager.clearServices();
        // Only try if we could delete the services
        if (result)
            result = result & this.docManager.clearDocuments();

        // Only try if we could delete the services and the docs
        if (result)
            result = result & this.kbManager.clearKnowledgeBase();

        return result;
    }

    /**
     * Registers all the services found on a given source document. This operation takes care of performing the
     * appropriate format transformation. The source document is not stored on the server. For keeping a copy of the
     * source document within the server use @see importServices instead.
     *
     * @param sourceDocumentUri
     * @param mediaType
     * @return the List of URIs of the services registered
     * @throws SalException
     */
    @Override
    public List<URI> registerServices(URI sourceDocumentUri, String mediaType) throws SalException {

        boolean isNativeFormat = MediaType.NATIVE_MEDIATYPE_SYNTAX_MAP.containsKey(mediaType);
        // Throw error if Format Unsupported
        if (!isNativeFormat && !this.serviceTransformationEngine.canTransform(mediaType)) {
            log.error("The media type {} is not natively supported and has no suitable transformer.", mediaType);
            throw new ServiceException("Unable to import service. Format unsupported.");
        }

        List<URI> registeredServices = new ArrayList<URI>();

        try {
            log.debug("Registering services from document: {}", sourceDocumentUri.toASCIIString());
            // 1st Obtain the document and parse/transform it
            InputStream is = sourceDocumentUri.toURL().openStream();
            List<Service> services = getServicesFromStream(mediaType, isNativeFormat, is);

            if (services != null && !services.isEmpty()) {
                log.debug("Services found: {}. Registering ...", services.size());
                URI serviceUri;
                for (Service service : services) {
                    // 2nd Add the service
                    serviceUri = this.serviceManager.addService(service);
                    if (serviceUri != null) {
                        registeredServices.add(serviceUri);
                    }
                }
            }

            if (registeredServices.size() != services.size()) {
                log.warn("Found {} services in document {} but only imported {} services", services.size(),
                        sourceDocumentUri, registeredServices.size());
            }

        } catch (MalformedURLException e) {
            log.error("Error obtaining the source document. Incorrect URL. " + sourceDocumentUri.toString());
            throw new ServiceException("Unable to register service. Incorrect source document URL.", e);
        } catch (IOException e) {
            log.error("Error obtaining the source document.");
            throw new ServiceException("Unable to register service. Unable to retrieve source document.", e);
        }

        return registeredServices;
    }


    /**
     * Unregisters a service from the registry. Effectively this will delete the service description and remove any
     * related documents on the server.
     *
     * @param serviceUri the URI of the service to unregister
     * @return true if it was properly unregistered, false otherwise.
     * @throws SalException
     */
    @Override
    public boolean unregisterService(URI serviceUri) throws SalException {

        // Check the URI is correct and belongs to the server
        if (serviceUri == null ||
                !UriUtil.isResourceLocalToServer(serviceUri, this.getIserveUri())) {
            return false;
        }

        if (this.serviceManager.deleteService(serviceUri)) {
            // delete documents
            Set<URI> docs = this.serviceManager.listDocumentsForService(serviceUri);
            for (URI doc : docs) {
                this.docManager.deleteDocument(doc);
            }
            // Some documents may not have been deleted properly. TODO: handle properly this case
            return true;
        }

        return false;
    }

    /**
     * Exports a given service in the media type requested
     *
     * @param serviceUri the URI of the service to export
     * @param mediaType  the media type to use for the export
     * @return the String representation of the service in the given format
     * @throws ServiceException
     */
    @Override
    public String exportService(URI serviceUri, String mediaType)
            throws ServiceException {
        //		return this.serviceManager.getServiceSerialisation(serviceUri, syntax);
        // TODO
        return null;
    }

}
