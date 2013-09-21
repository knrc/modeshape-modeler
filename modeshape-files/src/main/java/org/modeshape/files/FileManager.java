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
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Session;

import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.JcrLexicon;
import org.modeshape.jcr.api.JcrTools;
import org.modeshape.jcr.api.ValueFactory;
import org.modeshape.jcr.api.sequencer.Sequencer;
import org.modeshape.modeler.ModelType;
import org.modeshape.modeler.ModelTypeManager;
import org.modeshape.modeler.impl.Manager;
import org.modeshape.modeler.impl.Task;

public final class FileManager {
    
    final Manager mgr = new Manager();
    
    Set< ModelType > applicableModelTypes( final Node fileNode ) throws Exception {
        final Set< ModelType > applicableSequencers = new HashSet<>();
        for ( final ModelType type : mgr.modelTypeManager().modelTypes() )
            if ( type.sequencer().isAccepted( fileNode.getNode( JcrLexicon.CONTENT.getString() )
                                                      .getProperty( JcrLexicon.MIMETYPE.getString() ).getString() ) ) {
                applicableSequencers.add( type );
            }
        return applicableSequencers;
    }
    
    public Set< ModelType > applicableModelTypes( final String filePath ) throws FileManagerException {
        CheckArg.isNotEmpty( filePath, "filePath" );
        return mgr.run( new Task< Set< ModelType > >() {
            
            @Override
            public final Set< ModelType > run( final Session session ) throws Exception {
                return applicableModelTypes( fileNode( session, filePath ) );
            }
        } );
    }
    
    public void createDefaultModel( final String filePath ) throws FileManagerException {
        createModel( filePath, null );
    }
    
    public void createModel( final String filePath,
                             final String modelTypeName ) throws FileManagerException {
        CheckArg.isNotEmpty( filePath, "filePath" );
        mgr.run( new Task< Void >() {
            
            @Override
            public Void run( final Session session ) throws Exception {
                final Node fileNode = fileNode( session, filePath );
                ModelType modelType = null;
                if ( modelTypeName == null ) {
                    // If no model type supplied, use default sequencer if one exists
                    modelType = defaultSequencer( fileNode, applicableModelTypes( fileNode ) );
                    if ( modelType == null ) throw new IllegalArgumentException(
                                                                                 FileManagerI18n.unableToDetermineDefaultModelType.text( filePath ) );
                } else {
                    // Find sequencer with the supplied name
                    for ( final ModelType type : mgr.modelTypeManager().modelTypes() )
                        if ( modelTypeName.equals( type.name() ) ) {
                            modelType = type;
                            break;
                        }
                    if ( modelType == null ) throw new IllegalArgumentException( FileManagerI18n.unknownModelType.text( modelType ) );
                }
                // Build the model
                final ValueFactory valueFactory = ( ValueFactory ) session.getValueFactory();
                final Calendar cal = Calendar.getInstance();
                modelType.sequencer().execute( fileNode.getNode( JcrLexicon.CONTENT.getString() ).getProperty( JcrLexicon.DATA.getString() ),
                                               fileNode.addNode( modelType.name() ), new Sequencer.Context() {
                                                   
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
    
    public ModelType defaultModelType( final String filePath ) throws FileManagerException {
        CheckArg.isNotEmpty( filePath, "filePath" );
        return mgr.run( new Task< ModelType >() {
            
            @Override
            public ModelType run( final Session session ) throws Exception {
                final Node node = fileNode( session, filePath );
                final ModelType type = defaultSequencer( node, applicableModelTypes( node ) );
                return type == null ? null : type;
            }
        } );
    }
    
    ModelType defaultSequencer( final Node fileNode,
                                final Set< ModelType > applicableSequencers ) throws Exception {
        final String ext = fileNode.getName().substring( fileNode.getName().lastIndexOf( '.' ) + 1 );
        for ( final ModelType type : applicableSequencers )
            if ( type.sourceFileExtensions().contains( ext ) ) return type;
        return applicableSequencers.isEmpty() ? null : applicableSequencers.iterator().next();
    }
    
    Node fileNode( final Session session,
                   final String filePath ) throws Exception {
        // Return an absolute path
        return session.getNode( filePath.charAt( 0 ) == '/' ? filePath : '/' + filePath );
    }
    
    public ModelTypeManager modelTypeManager() {
        return mgr.modelTypeManager();
    }
    
    public String modeShapeConfigurationPath() {
        return mgr.modeShapeConfigurationPath();
    }
    
    public void setModeShapeConfigurationPath( final String modeShapeConfigurationPath ) {
        mgr.setModeShapeConfigurationPath( modeShapeConfigurationPath );
    }
    
    public void stop() throws FileManagerException {
        mgr.stop();
    }
    
    public String upload( final File file,
                          final String workspaceParentPath ) throws FileManagerException {
        CheckArg.isNotNull( file, "file" );
        return mgr.run( new Task< String >() {
            
            @Override
            public String run( final Session session ) throws Exception {
                // Ensure the path is non-null, ending with a slash
                String path = workspaceParentPath == null ? "/" : workspaceParentPath;
                if ( !path.endsWith( "/" ) ) path += '/';
                final Node fileNode = new JcrTools().uploadFile( session, path + file.getName(), file );
                // Add unstructured mix-in to allow node to contain anything else, like models created later
                fileNode.addMixin( Manager.UNSTRUCTURED_MIXIN );
                session.save();
                return fileNode.getPath();
            }
        } );
    }
}
