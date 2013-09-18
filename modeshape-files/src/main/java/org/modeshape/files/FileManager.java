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
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;

import org.infinispan.util.ReflectionUtil;
import org.modeshape.common.collection.Problem;
import org.modeshape.common.collection.Problems;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.ExtensionLogger;
import org.modeshape.jcr.JcrLexicon;
import org.modeshape.jcr.ModeShapeEngine;
import org.modeshape.jcr.NoSuchRepositoryException;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.jcr.api.JcrTools;
import org.modeshape.jcr.api.ValueFactory;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;
import org.modeshape.jcr.api.sequencer.Sequencer;
import org.modeshape.sequencer.xml.XmlSequencer;
import org.modeshape.sequencer.xsd.XsdSequencer;

public final class FileManager {
    
    public static final String DEFAULT_MODESHAPE_CONFIGURATION_PATH = "jcr/modeShapeConfig.json";
    
    private String modeShapeConfigurationPath = DEFAULT_MODESHAPE_CONFIGURATION_PATH;
    private ModeShapeEngine modeShape;
    private Repository repository;
    final Map< Sequencer, List< String > > sequencers = new HashMap<>();
    
    public Set< String > applicableModelTypes( final String filePath ) throws FileManagerException {
        CheckArg.isNotEmpty( filePath, "filePath" );
        return run( new Task< Set< String > >() {
            
            @Override
            public Set< String > run( final Session session ) throws Exception {
                final Set< String > types = new HashSet<>();
                for ( final Entry< Sequencer, List< String >> entry : applicableSequencers( fileNode( session, filePath ) ) )
                    types.add( entry.getKey().getName() );
                return types;
            }
        } );
    }
    
    Set< Entry< Sequencer, List< String > > > applicableSequencers( final Node fileNode ) throws ValueFormatException,
                    PathNotFoundException, RepositoryException {
        final Set< Entry< Sequencer, List< String >> > applicableSequencers = new HashSet<>();
        for ( final Entry< Sequencer, List< String >> entry : sequencers.entrySet() )
            if ( entry.getKey().isAccepted( fileNode.getNode( JcrLexicon.CONTENT.toString() )
                                                    .getProperty( JcrLexicon.MIMETYPE.toString() ).getString() ) ) {
                applicableSequencers.add( entry );
            }
        return applicableSequencers;
    }
    
    public void createDefaultModel( final String filePath ) throws FileManagerException {
        createModel( filePath, null );
    }
    
    public void createModel( final String filePath,
                             final String modelType ) throws FileManagerException {
        CheckArg.isNotEmpty( filePath, "filePath" );
        run( new Task< Void >() {
            
            @Override
            public Void run( final Session session ) throws Exception {
                final Node fileNode = fileNode( session, filePath );
                Sequencer sequencer = null;
                if ( modelType == null ) {
                    // If no model type supplied, use default sequencer if one exists
                    sequencer = defaultSequencer( fileNode, applicableSequencers( fileNode ) );
                    if ( sequencer == null ) throw new IllegalArgumentException(
                                                                                 FileManagerI18n.unableToDetermineDefaultModelType.text( filePath ) );
                } else {
                    // Find sequencer with the supplied name
                    for ( final Sequencer availableSequencer : sequencers.keySet() )
                        if ( modelType.equals( availableSequencer.getName() ) ) {
                            sequencer = availableSequencer;
                            break;
                        }
                    if ( sequencer == null ) throw new IllegalArgumentException( FileManagerI18n.unknownModelType.text( modelType ) );
                }
                // Build the model
                // Sequence file
                final ValueFactory valueFactory = ( ValueFactory ) session.getValueFactory();
                final Calendar cal = Calendar.getInstance();
                sequencer.execute( fileNode.getNode( JcrLexicon.CONTENT.toString() ).getProperty( JcrLexicon.DATA.toString() ),
                                   fileNode.addNode( sequencer.getName() ), new Sequencer.Context() {
                                       
                                       @Override
                                       public Calendar getTimestamp() {
                                           return cal;
                                       }
                                       
                                       @Override
                                       public ValueFactory valueFactory() {
                                           return valueFactory;
                                       }
                                   } );
                session.save();
                return null;
            }
        } );
    }
    
    public String defaultModelType( final String filePath ) throws FileManagerException {
        CheckArg.isNotEmpty( filePath, "filePath" );
        return run( new Task< String >() {
            
            @Override
            public String run( final Session session ) throws Exception {
                final Node node = fileNode( session, filePath );
                final Sequencer sequencer = defaultSequencer( node, applicableSequencers( node ) );
                return sequencer == null ? null : sequencer.getName();
            }
        } );
    }
    
    Sequencer defaultSequencer( final Node fileNode,
                                final Set< Entry< Sequencer, List< String > > > applicableSequencers ) throws RepositoryException {
        final String ext = fileNode.getName().substring( fileNode.getName().lastIndexOf( '.' ) + 1 );
        for ( final Entry< Sequencer, List< String > > entry : applicableSequencers )
            if ( entry.getValue().contains( ext ) ) return entry.getKey();
        return applicableSequencers.isEmpty() ? null : applicableSequencers.iterator().next().getKey();
    }
    
    Node fileNode( final Session session,
                   final String filePath ) throws PathNotFoundException, RepositoryException {
        // Return an absolute path
        return session.getNode( filePath.charAt( 0 ) == '/' ? filePath : '/' + filePath );
    }
    
    /**
     * @return the path to the ModeShape configuration file. Default is {@value #DEFAULT_MODESHAPE_CONFIGURATION_PATH}.
     */
    public String modeShapeConfigurationPath() {
        return modeShapeConfigurationPath;
    }
    
    < T > T run( final Task< T > task ) throws FileManagerException {
        final Session session = session();
        try {
            return task.run( session );
        } catch ( final RuntimeException e ) {
            throw e;
        } catch ( final Exception e ) {
            throw new FileManagerException( e );
        } finally {
            session.logout();
        }
    }
    
    Session session() throws FileManagerException {
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
                // TODO: Remove after fixing issue #1
                // Note, the order matters here. For instance, the XSD sequencer will by default accept XML files for sequencing
                sequencers.put( new XmlSequencer(), Arrays.asList( "xml" ) );
                sequencers.put( new XsdSequencer(), Arrays.asList( "xsd" ) );
                final Session session = repository.login( "default" );
                for ( final Sequencer sequencer : sequencers.keySet() ) {
                    ReflectionUtil.setValue( sequencer, "logger", ExtensionLogger.getLogger( sequencer.getClass() ) );
                    ReflectionUtil.setValue( sequencer, "repositoryName", config.getName() );
                    // The sequencer's name will also be the node's name containing the model (i.e., sequenced file)
                    ReflectionUtil.setValue( sequencer, "name", sequencer.getClass().getSimpleName() );
                    sequencer.initialize( session.getWorkspace().getNamespaceRegistry(),
                                          ( NodeTypeManager ) session.getWorkspace().getNodeTypeManager() );
                }
                Logger.getLogger( getClass() ).info( FileManagerI18n.fileManagerStarted );
                return session;
            }
            
            return repository.login( "default" );
        } catch ( final Throwable e ) {
            throw new FileManagerException( e );
        }
    }
    
    public void setModeShapeConfigurationPath( final String modeShapeConfigurationPath ) {
        this.modeShapeConfigurationPath = modeShapeConfigurationPath == null ? DEFAULT_MODESHAPE_CONFIGURATION_PATH
                                                                            : modeShapeConfigurationPath;
    }
    
    /**
     * @throws FileManagerException
     */
    public void stop() throws FileManagerException {
        if ( modeShape == null ) Logger.getLogger( getClass() )
                                       .debug( "Attempt to stop ModeShape File Manager when it is already stopped" );
        else {
            try {
                modeShape.shutdown().get();
            } catch ( InterruptedException | ExecutionException e ) {
                throw new FileManagerException( e );
            }
            modeShape = null;
            Logger.getLogger( getClass() ).info( FileManagerI18n.fileManagerStopped );
        }
    }
    
    public String upload( final File file,
                          final String workspaceParentPath ) throws FileManagerException {
        CheckArg.isNotNull( file, "file" );
        return run( new Task< String >() {
            
            @Override
            public String run( final Session session ) throws Exception {
                // Ensure the path is non-null, ending with a slash
                String path = workspaceParentPath == null ? "/" : workspaceParentPath;
                if ( !path.endsWith( "/" ) ) path += '/';
                final Node fileNode = new JcrTools().uploadFile( session, path + file.getName(), file );
                // Add unstructured mix-in to allow node to contain anything else, like models created later
                fileNode.addMixin( "modefm:unstructured" );
                session.save();
                return fileNode.getPath();
            }
        } );
    }
    
    public static interface Task< T > {
        
        T run( Session session ) throws Exception;
    }
}
