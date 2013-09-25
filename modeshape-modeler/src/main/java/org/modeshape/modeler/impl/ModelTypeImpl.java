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
package org.modeshape.modeler.impl;

import java.util.HashSet;
import java.util.Set;

import javax.jcr.Session;

import org.infinispan.util.ReflectionUtil;
import org.modeshape.jcr.ExtensionLogger;
import org.modeshape.jcr.api.Repository;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;
import org.modeshape.jcr.api.sequencer.Sequencer;
import org.modeshape.modeler.ModelType;
import org.modeshape.modeler.ModelerException;

/**
 * 
 */
public final class ModelTypeImpl implements ModelType {
    
    private final Manager mgr;
    final Class< ? > sequencerClass;
    private String name;
    private final Set< String > sourceFileExtensions = new HashSet<>();
    
    ModelTypeImpl( final Manager manager,
                   final Class< ? > sequencerClass ) {
        mgr = manager;
        this.sequencerClass = sequencerClass;
        name = sequencerClass.getSimpleName();
        name = name.endsWith( "Sequencer" ) ? name.substring( 0, name.length() - "Sequencer".length() ) + " Model" : name
                                                                                                                     + " Model";
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.modeler.ModelType#name()
     */
    @Override
    public String name() {
        return name;
    }
    
    /**
     * @return this model type's sequencer
     * @throws ModelerException
     *         if any problem occurs
     */
    public Sequencer sequencer() throws ModelerException {
        return mgr.run( new Task< Sequencer >() {
            
            @Override
            public Sequencer run( final Session session ) throws Exception {
                final Sequencer sequencer = ( Sequencer ) sequencerClass.newInstance();
                // Initialize
                ReflectionUtil.setValue( sequencer, "logger", ExtensionLogger.getLogger( sequencer.getClass() ) );
                ReflectionUtil.setValue( sequencer, "repositoryName",
                                         session.getRepository().getDescriptor( Repository.REPOSITORY_NAME ) );
                ReflectionUtil.setValue( sequencer, "name", sequencer.getClass().getSimpleName() );
                sequencer.initialize( session.getWorkspace().getNamespaceRegistry(),
                                      ( NodeTypeManager ) session.getWorkspace().getNodeTypeManager() );
                return sequencer;
            }
        } );
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.modeler.ModelType#sourceFileExtensions()
     */
    @Override
    public Set< String > sourceFileExtensions() {
        return sourceFileExtensions;
    }
}
