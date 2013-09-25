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
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.Session;

import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.JcrLexicon;
import org.modeshape.jcr.api.JcrTools;
import org.modeshape.jcr.api.ValueFactory;
import org.modeshape.jcr.api.sequencer.Sequencer;
import org.modeshape.modeler.impl.Manager;
import org.modeshape.modeler.impl.ModelTypeImpl;
import org.modeshape.modeler.impl.Task;

/**
 * 
 */
public final class Modeler {
    
    final Manager mgr = new Manager();
    
    /**
     * @param contentPath
     *        the repository path to an artifact
     * @throws ModelerException
     *         if any problem occurs
     */
    public void createDefaultModel( final String contentPath ) throws ModelerException {
        createModel( contentPath, null );
    }
    
    /**
     * @param artifactPath
     *        the repository path to an artifact
     * @param modelType
     *        the type of model to be created for the supplied artifact
     * @throws ModelerException
     *         if any problem occurs
     */
    public void createModel( final String artifactPath,
                             final ModelType modelType ) throws ModelerException {
        CheckArg.isNotEmpty( artifactPath, "contentPath" );
        mgr.run( new Task< Void >() {
            
            @Override
            public Void run( final Session session ) throws Exception {
                final Node contentNode = mgr.fileNode( session, artifactPath );
                ModelType type = modelType;
                if ( modelType == null ) {
                    // If no model type supplied, use default model type if one exists
                    type = mgr.modelTypeManager().defaultModelType( contentNode,
                                                                    mgr.modelTypeManager().modelTypes( contentNode ) );
                    if ( type == null )
                        throw new IllegalArgumentException( ModelerI18n.unableToDetermineDefaultModelType.text( artifactPath ) );
                }
                // Build the model
                final ValueFactory valueFactory = ( ValueFactory ) session.getValueFactory();
                final Calendar cal = Calendar.getInstance();
                ( ( ModelTypeImpl ) type ).sequencer().execute( contentNode.getNode( JcrLexicon.CONTENT.getString() )
                                                                           .getProperty( JcrLexicon.DATA.getString() ),
                                                                contentNode.addNode( type.name() ),
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
                session.save();
                return null;
            }
        } );
    }
    
    /**
     * @param file
     *        the file to be imported. Must not be <code>null</code>.
     * @param workspaceParentPath
     *        the path of the parent path where the file should be imported
     * @return the repository path the to imported content
     * @throws ModelerException
     *         if any problem occurs
     */
    public String importContent( final File file,
                                 final String workspaceParentPath ) throws ModelerException {
        CheckArg.isNotNull( file, "file" );
        if ( !file.exists() ) throw new IllegalArgumentException( ModelerI18n.fileNotFound.text( file ) );
        try {
            return importContent( file.getName(), file.toURI().toURL().openStream(), workspaceParentPath );
        } catch ( final IOException e ) {
            throw new ModelerException( e );
        }
    }
    
    /**
     * @param name
     *        the name of the artifact as it should be stored in the repository. Must not be empty.
     * @param stream
     *        the artifact's content to be imported. Must not be <code>null</code>.
     * @param workspaceParentPath
     *        the path of the parent path where the content should be imported
     * @return the repository path the to imported artifact
     * @throws ModelerException
     *         if any problem occurs
     */
    public String importContent( final String name,
                                 final InputStream stream,
                                 final String workspaceParentPath ) throws ModelerException {
        CheckArg.isNotEmpty( name, "name" );
        CheckArg.isNotNull( stream, "stream" );
        return mgr.run( new Task< String >() {
            
            @Override
            public String run( final Session session ) throws Exception {
                // Ensure the path is non-null, ending with a slash
                String path = workspaceParentPath == null ? "/" : workspaceParentPath;
                if ( !path.endsWith( "/" ) ) path += '/';
                final Node fileNode = new JcrTools().uploadFile( session, path + name, stream );
                // Add unstructured mix-in to allow node to contain anything else, like models created later
                fileNode.addMixin( Manager.UNSTRUCTURED_MIXIN );
                session.save();
                return fileNode.getPath();
            }
        } );
    }
    
    /**
     * @return the model type manager
     */
    public ModelTypeManager modelTypeManager() {
        return mgr.modelTypeManager();
    }
    
    /**
     * @return the ModeShape configuration path
     */
    public String modeShapeConfigurationPath() {
        return mgr.modeShapeConfigurationPath();
    }
    
    /**
     * @param modeShapeConfigurationPath
     *        a ModeShape configuration path
     */
    public void setModeShapeConfigurationPath( final String modeShapeConfigurationPath ) {
        mgr.setModeShapeConfigurationPath( modeShapeConfigurationPath );
    }
    
    /**
     * @throws ModelerException
     *         if any problem occurs
     */
    public void stop() throws ModelerException {
        mgr.stop();
    }
}
