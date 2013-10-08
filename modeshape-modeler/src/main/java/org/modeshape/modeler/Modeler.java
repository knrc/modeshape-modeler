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
package org.modeshape.modeler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.Session;

import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.JcrLexicon;
import org.modeshape.jcr.api.JcrTools;
import org.modeshape.jcr.api.ValueFactory;
import org.modeshape.jcr.api.sequencer.Sequencer;
import org.modeshape.modeler.internal.DependencyProcessor;
import org.modeshape.modeler.internal.Manager;
import org.modeshape.modeler.internal.ModelImpl;
import org.modeshape.modeler.internal.ModelTypeImpl;
import org.modeshape.modeler.internal.ModelerLexicon;
import org.modeshape.modeler.internal.Task;
import org.polyglotter.common.Logger;

/**
 * 
 */
public final class Modeler implements AutoCloseable {
    
    /**
     * 
     */
    public static final String DEFAULT_MODESHAPE_CONFIGURATION_PATH = "jcr/modeShapeConfig.json";
    
    final Manager manager;
    
    /**
     * Uses a default ModeShape configuration.
     * 
     * @param repositoryStoreParentPath
     *        the path to the folder that should contain the ModeShape repository store
     * @throws ModelerException
     *         if any error occurs
     */
    public Modeler( final String repositoryStoreParentPath ) throws ModelerException {
        this( repositoryStoreParentPath, DEFAULT_MODESHAPE_CONFIGURATION_PATH );
    }
    
    /**
     * @param repositoryStoreParentPath
     *        the path to the folder that should contain the ModeShape repository store
     * @param modeShapeConfigurationPath
     *        the path to a ModeShape configuration file
     * @throws ModelerException
     *         if any error occurs
     */
    public Modeler( final String repositoryStoreParentPath,
                    final String modeShapeConfigurationPath ) throws ModelerException {
        manager = new Manager( repositoryStoreParentPath, modeShapeConfigurationPath );
    }
    
    Modeler accessThis() {
        return this;
    }
    
    /**
     * {@inheritDoc}
     * 
     * @throws ModelerException
     *         if any problem occurs
     * @see java.lang.AutoCloseable#close()
     */
    @Override
    public void close() throws ModelerException {
        manager.close();
    }
    
    /**
     * @param artifactPath
     *        the repository path to an artifact; must not be empty.
     * @return a new model of the default type, determined by the artifact's content, and if the artifact is a file, its file
     *         extension; never <code>null</code>
     * @throws ModelerException
     *         if any problem occurs
     */
    public Model createDefaultModel( final String artifactPath ) throws ModelerException {
        return createModel( artifactPath, null );
    }
    
    /**
     * @param artifactPath
     *        the repository path to an artifact; must not be empty.
     * @param modelType
     *        the type of model to be created for the supplied artifact; may be <code>null</code>.
     * @return a new model of the supplied type; never <code>null</code>
     * @throws ModelerException
     *         if any problem occurs
     */
    public Model createModel( final String artifactPath,
                              final ModelType modelType ) throws ModelerException {
        CheckArg.isNotEmpty( artifactPath, "artifactPath" );
        return manager.run( new Task< Model >() {
            
            @Override
            public Model run( final Session session ) throws Exception {
                final Node artifactNode = manager.artifactNode( session, artifactPath );
                ModelType type = modelType;
                if ( modelType == null ) {
                    // If no model type supplied, use default model type if one exists
                    type = manager.modelTypeManager.defaultModelType( artifactNode,
                                                                      manager.modelTypeManager.modelTypes( artifactNode ) );
                    if ( type == null )
                        throw new IllegalArgumentException( ModelerI18n.unableToDetermineDefaultModelType.text( artifactPath ) );
                }
                // Build the model
                final ValueFactory valueFactory = ( ValueFactory ) session.getValueFactory();
                final Calendar cal = Calendar.getInstance();
                final ModelTypeImpl modelType = ( ModelTypeImpl ) type;
                final Node modelNode = artifactNode.addNode( type.name() );
                modelNode.addMixin( ModelerLexicon.MODEL_MIXIN );
                modelNode.setProperty( ModelerLexicon.EXTERNAL_LOCATION,
                                       artifactNode.getProperty( ModelerLexicon.EXTERNAL_LOCATION ).getString() );
                final boolean save = modelType.sequencer().execute( artifactNode.getNode( JcrLexicon.CONTENT.getString() )
                                                                                .getProperty( JcrLexicon.DATA.getString() ),
                                                                    modelNode,
                                                                    new Sequencer.Context() {
                                                                        
                                                                        @Override
                                                                        public Calendar getTimestamp() {
                                                                            return cal;
                                                                        }
                                                                        
                                                                        @Override
                                                                        public ValueFactory valueFactory() {
                                                                            return valueFactory;
                                                                        }
                                                                    } );
                if ( save ) {
                    processDependencies( modelNode, modelType );
                    session.save();
                    return new ModelImpl( manager, modelNode.getPath() );
                }
                throw new ModelerException( ModelerI18n.sessionNotSavedWhenCreatingModel, artifactPath );
            }
        } );
    }
    
    /**
     * @param url
     *        the name of the artifact as it should be stored in the repository. Must not be empty.
     * @param stream
     *        the artifact's content to be imported. Must not be <code>null</code>.
     * @param workspaceParentPath
     *        the path of the parent path where the artifact should be imported
     * @return the repository path the to imported artifact
     * @throws ModelerException
     *         if any problem occurs
     */
    public String importArtifact( final URL url,
                                  final InputStream stream,
                                  final String workspaceParentPath ) throws ModelerException {
        CheckArg.isNotNull( url, "name" );
        CheckArg.isNotNull( stream, "stream" );
        return manager.run( new Task< String >() {
            
            @Override
            public String run( final Session session ) throws Exception {
                // Ensure the path is non-null, ending with a slash
                String workspacePath = workspaceParentPath == null ? "/" : workspaceParentPath;
                if ( !workspacePath.endsWith( "/" ) ) workspacePath += '/';
                final String urlPath = url.getPath();
                final Node node = new JcrTools().uploadFile( session,
                                                             workspacePath + urlPath.substring( urlPath.lastIndexOf( '/' ) + 1 ),
                                                             stream );
                // Add unstructured mix-in to allow node to contain anything else, like models created later
                node.addMixin( ModelerLexicon.UNSTRUCTURED_MIXIN );
                node.setProperty( ModelerLexicon.EXTERNAL_LOCATION, url.toString() );
                session.save();
                return node.getPath();
            }
        } );
    }
    
    /**
     * @param file
     *        the file to be imported. Must not be <code>null</code>.
     * @param workspaceParentPath
     *        the path of the parent path where the file should be imported
     * @return the repository path the to imported artifact
     * @throws ModelerException
     *         if any problem occurs
     */
    public String importFile( final File file,
                              final String workspaceParentPath ) throws ModelerException {
        CheckArg.isNotNull( file, "file" );
        if ( !file.exists() ) throw new IllegalArgumentException( ModelerI18n.fileNotFound.text( file ) );
        try {
            final URL url = file.toURI().toURL();
            return importArtifact( url, url.openStream(), workspaceParentPath );
        } catch ( final IOException e ) {
            throw new ModelerException( e );
        }
    }
    
    /**
     * @return the model type manager
     */
    public ModelTypeManager modelTypeManager() {
        return manager.modelTypeManager;
    }
    
    /**
     * @return the path to the configuration for the embedded ModeShape repository supplied when this Modeler was instantiated.
     */
    public String modeShapeConfigurationPath() {
        return manager.modeShapeConfigurationPath;
    }
    
    /**
     * @param modelNode
     *        the model node whose dependency processing is being requested (cannot be <code>null</code>)
     * @param modelType
     *        the model type of the model node (cannot be <code>null</code>)
     * @return the path to the dependencies child node or <code>null</code> if no dependencies are found
     * @throws ModelerException
     *         if node is not a model nodel or if an error occurs
     */
    String processDependencies( final Node modelNode,
                                final ModelType modelType ) throws ModelerException {
        CheckArg.isNotNull( modelNode, "modelNode" );
        CheckArg.isNotNull( modelType, "modelType" );
        
        return manager.run( new Task< String >() {
            
            @Override
            public String run( final Session session ) throws Exception {
                final DependencyProcessor dependencyProcessor = manager.modelTypeManager.dependencyProcessor( modelNode );
                
                if ( dependencyProcessor == null ) {
                    Logger.getLogger( getClass() ).debug( "No dependency processor found for model '" + modelNode.getName() + '\'' );
                    return null;
                }
                
                return dependencyProcessor.process( modelNode, modelType, accessThis() );
            }
        } );
    }
    
    /**
     * @return the path to the folder that should contain the ModeShape repository store
     */
    public String repositoryStoreParentPath() {
        return System.getProperty( Manager.REPOSITORY_STORE_PARENT_PATH_PROPERTY );
    }
}
