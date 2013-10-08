/*
 * Polyglotter (http://polyglotter.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * Polyglotter is free software. Unless otherwise indicated, all code in Polyglotter
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * Polyglotter is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.modeler.internal;

/**
 * The lexicon of the ModeShape modeler.
 */
public interface ModelerLexicon {
    
    /**
     * The ModeShape modeler namespace prefix. Value is {@value} .
     */
    String NAMESPACE_PREFIX = "mm";
    
    /**
     * The ModeShape modeler namespace prefix with the name separator, a colon character, appended. Value is {@value} .
     */
    String NAMESPACE_PREFIX_WITH_SEPARATOR = NAMESPACE_PREFIX + ':';
    
    /**
     * The node type name of the dependencies node. This node will have dependency child nodes.
     */
    String DEPENDENCIES_NODE = NAMESPACE_PREFIX_WITH_SEPARATOR + "dependencies";
    
    /**
     * The node type name of a dependency node.
     */
    String DEPENDENCY_NODE = NAMESPACE_PREFIX_WITH_SEPARATOR + "dependency";
    
    /**
     * The name of the external location property the imported resource was sourced from.
     */
    String EXTERNAL_LOCATION = NAMESPACE_PREFIX_WITH_SEPARATOR + "externalLocation";
    
    /**
     * The name of the property for the fully qualified path, including name, of the imported artifact.
     */
    String ORIGIN_PATH_PROPERTY = NAMESPACE_PREFIX_WITH_SEPARATOR + "origin";
    
    /**
     * The name of a dependency node's derived path property.
     */
    String PATH_PROPERTY = NAMESPACE_PREFIX_WITH_SEPARATOR + "path";
    
    /**
     * The mixin type of a model node.
     */
    String MODEL_MIXIN = NAMESPACE_PREFIX_WITH_SEPARATOR + "model";
    
    /**
     * The name of a dependency node's source reference property.
     */
    String SOURCE_REFERENCE_PROPERTY = NAMESPACE_PREFIX_WITH_SEPARATOR + "sourceReference";
    
    /**
     * The mixin for the node where model nodes can be added to.
     */
    String UNSTRUCTURED_MIXIN = NAMESPACE_PREFIX_WITH_SEPARATOR + "unstructured";
    
}
