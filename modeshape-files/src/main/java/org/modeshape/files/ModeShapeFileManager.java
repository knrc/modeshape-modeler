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
package org.modeshape.files;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;

import org.modeshape.common.collection.Problem;
import org.modeshape.common.collection.Problems;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.ModeShapeEngine;
import org.modeshape.jcr.NoSuchRepositoryException;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.jcr.api.JcrTools;
import org.modeshape.jcr.api.observation.Event.Sequencing;

public final class ModeShapeFileManager {
    
    public static final String DEFAULT_MODESHAPE_CONFIGURATION_PATH = "jcr/modeShapeConfig.json";
    
    private String modeShapeConfigurationPath = DEFAULT_MODESHAPE_CONFIGURATION_PATH;
    private ModeShapeEngine modeShape;
    private Repository repository;
    
    /**
     * @return the path to the ModeShape configuration file. Default is {@value #DEFAULT_MODESHAPE_CONFIGURATION_PATH}.
     */
    public String modeShapeConfigurationPath() {
        return modeShapeConfigurationPath;
    }
    
    /**
     * @return the JCR repository session (never <code>null</code>)
     * @throws ModeShapeFileManagerException
     */
    public Session session() throws ModeShapeFileManagerException {
        try {
            if ( modeShape == null ) {
                modeShape = new ModeShapeEngine();
                modeShape.start();
                final RepositoryConfiguration config = RepositoryConfiguration.read( modeShapeConfigurationPath );
                final Problems problems = config.validate();
                if ( problems.hasProblems() ) {
                    for ( final Problem problem : problems )
                        Logger.getLogger( getClass() ).error( problem.getMessage(), problem.getThrowable() );
                    throw problems.iterator().next().getThrowable();
                }
                try {
                    repository = modeShape.getRepository( config.getName() );
                } catch ( final NoSuchRepositoryException err ) {
                    repository = modeShape.deploy( config );
                }
                Logger.getLogger( getClass() ).info( ModeShapeFileManagerI18n.modeShapeFileManagerStarted );
            }
            return repository.login( "default" );
        } catch ( final Throwable e ) {
            throw new ModeShapeFileManagerException( e );
        }
    }
    
    public void setModeShapeConfigurationPath( final String modeShapeConfigurationPath ) {
        this.modeShapeConfigurationPath = modeShapeConfigurationPath == null ? DEFAULT_MODESHAPE_CONFIGURATION_PATH
                                                                            : modeShapeConfigurationPath;
    }
    
    /**
     * @throws ModeShapeFileManagerException
     */
    public void stop() throws ModeShapeFileManagerException {
        if ( modeShape == null ) Logger.getLogger( getClass() )
                                       .debug( "Attempt to stop ModeShape File Manager when it is already stopped" );
        else {
            try {
                modeShape.shutdown().get();
            } catch ( InterruptedException | ExecutionException e ) {
                throw new ModeShapeFileManagerException( e );
            }
            modeShape = null;
            Logger.getLogger( getClass() ).info( ModeShapeFileManagerI18n.modeShapeFileManagerStopped );
        }
    }
    
    /**
     * @param file
     * @param workspaceParentPath
     * @return The path to the node representing the imported file
     * @throws ModeShapeFileManagerException
     */
    public String upload( final File file,
                          final String workspaceParentPath ) throws ModeShapeFileManagerException {
        CheckArg.isNotNull( file, "file" );
        final String path = workspaceParentPath == null ? file.getName() : workspaceParentPath.endsWith( "/" )
                                                                                                              ? workspaceParentPath
                                                                                                                + '/'
                                                                                                                + file.getName()
                                                                                                              : workspaceParentPath
                                                                                                                + file.getName();
        try {
            final ObservationManager observationMgr = session().getWorkspace().getObservationManager();
            final CountDownLatch latch = new CountDownLatch( 1 );
            final EventListener listener = new EventListener() {
                
                @Override
                public void onEvent( final EventIterator events ) {
                    final Event event = events.nextEvent();
                    try {
                        try {
                            if ( event.getType() == Sequencing.NODE_SEQUENCING_FAILURE ) {
                                Logger.getLogger( getClass() ).error( ModeShapeFileManagerI18n.unableToSequenceUploadedFile, path,
                                                                      event.getInfo().get( Sequencing.SEQUENCING_FAILURE_CAUSE ) );
                            }
                        } catch ( final RepositoryException e ) {
                            throw new RuntimeException( e );
                        }
                    } finally {
                        latch.countDown();
                    }
                }
            };
            observationMgr.addEventListener( listener, Sequencing.ALL, "/", true, null, null, false );
            final Session session = session();
            final Node node = new JcrTools().uploadFile( session, path, file );
            node.addMixin( "modefm:unstructured" );
            session.save();
            session.logout();
            if ( !latch.await( 15, TimeUnit.SECONDS ) ) Logger.getLogger( getClass() ).debug( "Timed out" );
            observationMgr.removeEventListener( listener );
            return node.getPath();
        } catch ( RepositoryException | IOException | InterruptedException e ) {
            throw new ModeShapeFileManagerException( e );
        }
    }
}
