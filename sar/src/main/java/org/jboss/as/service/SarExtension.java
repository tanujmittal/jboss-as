/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.service;

import java.util.List;
import java.util.Locale;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import org.jboss.as.controller.descriptions.common.CommonDescriptions;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.transform.AbstractOperationTransformer;
import org.jboss.as.controller.transform.SubSystemTransformersRegistry;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.common.beans.property.PropertyEditors;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * Extension used to enable SAR deployments.
 *
 * @author John Bailey
 * @author Emanuel Muckenhuber
 */
public class SarExtension implements Extension {

    public static final String NAMESPACE = "urn:jboss:domain:sar:1.0";
    public static final String SUBSYSTEM_NAME = "sar";
    static final SarSubsystemParser parser = new SarSubsystemParser();

    private static final int MANAGEMENT_API_MAJOR_VERSION = 1;
    private static final int MANAGEMENT_API_MINOR_VERSION = 0;
    private static final int MANAGEMENT_API_MICRO_VERSION = 0;

    /** {@inheritDoc} */
    @Override
    public void initialize(ExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, MANAGEMENT_API_MAJOR_VERSION,
                MANAGEMENT_API_MINOR_VERSION, MANAGEMENT_API_MICRO_VERSION);
        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(SarSubsystemProviders.SUBSYSTEM);
        registration.registerOperationHandler(ADD, SarSubsystemAdd.INSTANCE, SarSubsystemProviders.SUBSYSTEM_ADD, false);
        registration.registerOperationHandler(DESCRIBE, SarDescribeHandler.INSTANCE, SarDescribeHandler.INSTANCE, false, OperationEntry.EntryType.PRIVATE);
        registration.registerOperationHandler(REMOVE, ReloadRequiredRemoveStepHandler.INSTANCE, SarSubsystemProviders.SUBSYSTEM_REMOVE, false);
        subsystem.registerXMLElementWriter(parser);
        PropertyEditors.init();

        registration.registerReadWriteAttribute("test", null, new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                context.completeStep();
            }
        }, AttributeAccess.Storage.CONFIGURATION);
        final SubSystemTransformersRegistry registry = subsystem.registerModelTransformers(ModelVersion.create(1, 0, 0));
        registry.registerOperationTransformer(PathAddress.EMPTY_ADDRESS, "read-resource", new AbstractOperationTransformer() {

            @Override
            protected ModelNode transform(TransformationContext context, PathAddress address, ModelNode operation) {
                System.out.print("transforming " + operation);
                return operation;
            }

        });
        registry.registerOperationTransformer(PathAddress.EMPTY_ADDRESS, "write-attribute", new AbstractOperationTransformer() {

            @Override
            protected ModelNode transform(TransformationContext context, PathAddress address, ModelNode operation) {
                System.out.print("transforming write-attribute" + operation);
                return operation;
            }

        });

    }

    /** {@inheritDoc} */
    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, NAMESPACE, parser);
    }

    private static ModelNode createAddOperation() {
        final ModelNode subsystem = new ModelNode();
        subsystem.get(OP).set(ADD);
        subsystem.get(OP_ADDR).add(SUBSYSTEM, SUBSYSTEM_NAME);
        return subsystem;
    }

    static class SarSubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>, XMLElementWriter<SubsystemMarshallingContext> {

        /** {@inheritDoc} */
        @Override
        public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
            //TODO seems to be a problem with empty elements cleaning up the queue in FormattingXMLStreamWriter.runAttrQueue
            //context.startSubsystemElement(NewSarExtension.NAMESPACE, true);
            context.startSubsystemElement(SarExtension.NAMESPACE, false);
            writer.writeEndElement();
}

        /** {@inheritDoc} */
        @Override
        public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
            // Require no content
            ParseUtils.requireNoContent(reader);
            list.add(createAddOperation());
        }

    }


    private static class SarDescribeHandler implements OperationStepHandler, DescriptionProvider {
        static final SarDescribeHandler INSTANCE = new SarDescribeHandler();

        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            context.getResult().add(createAddOperation());
            context.completeStep();
        }

        @Override
        public ModelNode getModelDescription(Locale locale) {
            return CommonDescriptions.getSubsystemDescribeOperation(locale);
        }
    }
}
