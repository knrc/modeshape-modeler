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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.zip.ZipInputStream;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;

import org.modeshape.common.collection.Problem;
import org.modeshape.common.collection.Problems;
import org.modeshape.jcr.JcrLexicon;
import org.modeshape.jcr.ModeShapeEngine;
import org.modeshape.jcr.NoSuchRepositoryException;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.modeler.ModelerException;
import org.modeshape.modeler.ModelerI18n;
import org.polyglotter.common.CommonI18n;
import org.polyglotter.common.Logger;

/**
 * 
 */
public final class Manager {
    
    /**
     * 
     */
    public static final String DEFAULT_MODESHAPE_CONFIGURATION_PATH = "jcr/modeShapeConfig.json";
    
    /**
     * 
     */
    public static final String NS = "mm:";
    
    /**
     * 
     */
    public static final String UNSTRUCTURED_MIXIN = NS + "unstructured";
    
    @SuppressWarnings( "javadoc" )
    public final String modeShapeConfigurationPath;
    private ModeShapeEngine modeShape;
    private Repository repository;
    final ModelTypeManagerImpl modelTypeMgr = new ModelTypeManagerImpl( this );
    
    /**
     * Uses a default ModeShape configuration.
     */
    public Manager() {
        modeShapeConfigurationPath = DEFAULT_MODESHAPE_CONFIGURATION_PATH;
    }
    
    /**
     * @param modeShapeConfigurationPath
     *        the path to a ModeShape configuration file
     */
    public Manager( final String modeShapeConfigurationPath ) {
        this.modeShapeConfigurationPath = modeShapeConfigurationPath;
    }
    
    Binary content( final Node fileNode ) throws ValueFormatException, PathNotFoundException, RepositoryException {
        return fileNode.getNode( JcrLexicon.CONTENT.getString() ).getProperty( JcrLexicon.DATA.getString() ).getBinary();
    }
    
    byte[] content( final ZipInputStream zip ) throws IOException {
        try ( final ByteArrayOutputStream stream = new ByteArrayOutputStream() ) {
            final byte[] buf = new byte[ 1024 ];
            for ( int bytesRead; ( bytesRead = zip.read( buf, 0, buf.length ) ) > -1; )
                stream.write( buf, 0, bytesRead );
            return stream.toByteArray();
        }
    }
    
    /**
     * @param session
     *        a session
     * @param filePath
     *        a file's repository path
     * @return the node with the supplied filePath
     * @throws Exception
     *         if any problem occurs
     */
    public Node fileNode( final Session session,
                          final String filePath ) throws Exception {
        // Return an absolute path
        return session.getNode( filePath.charAt( 0 ) == '/' ? filePath : '/' + filePath );
    }
    
    /**
     * @return the model type manager
     */
    public ModelTypeManagerImpl modelTypeManager() {
        return modelTypeMgr;
    }
    
    Repository repository() throws ModelerException {
        if ( modeShape == null ) {
            try {
                modeShape = new ModeShapeEngine();
                modeShape.start();
                final RepositoryConfiguration config = RepositoryConfiguration.read( modeShapeConfigurationPath );
                final Problems problems = config.validate();
                if ( problems.hasProblems() ) {
                    for ( final Problem problem : problems )
                        Logger.getLogger( getClass() ).error( problem.getThrowable(), CommonI18n.text, problem.getMessage().text() );
                    throw problems.iterator().next().getThrowable();
                }
                try {
                    repository = modeShape.getRepository( config.getName() );
                } catch ( final NoSuchRepositoryException err ) {
                    repository = modeShape.deploy( config );
                }
                Logger.getLogger( getClass() ).info( ModelerI18n.modelerStarted );
            } catch ( final Throwable e ) {
                throw new ModelerException( e );
            }
        }
        return repository;
    }
    
    /**
     * @param task
     *        a task
     * @return the return value of the supplied task
     * @throws ModelerException
     *         if any problem occurs
     */
    public < T > T run( final Task< T > task ) throws ModelerException {
        final Session session = session();
        try {
            return task.run( session );
        } catch ( final RuntimeException e ) {
            throw e;
        } catch ( final Exception e ) {
            throw new ModelerException( e );
        } finally {
            session.logout();
        }
    }
    
    /**
     * @return a new session
     * @throws ModelerException
     *         if any problem occurs
     */
    public Session session() throws ModelerException {
        try {
            return repository().login( "default" );
        } catch ( final Throwable e ) {
            throw new ModelerException( e );
        }
    }
    
    /**
     * @throws ModelerException
     *         if any problem occurs
     */
    public void stop() throws ModelerException {
        if ( modeShape == null )
            Logger.getLogger( getClass() ).debug( "Attempt to stop ModeShape Modeler when it is already stopped" );
        else {
            try {
                modeShape.shutdown().get();
            } catch ( InterruptedException | ExecutionException e ) {
                throw new ModelerException( e );
            }
            modeShape = null;
            Logger.getLogger( getClass() ).info( ModelerI18n.modelerStopped );
        }
    }
}
