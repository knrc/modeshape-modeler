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

public final class Modeler {
    
    final Manager mgr = new Manager();
    
    public void createDefaultModel( final String contentPath ) throws ModelerException {
        createModel( contentPath, null );
    }
    
    public void createModel( final String contentPath,
                             final ModelType modelType ) throws ModelerException {
        CheckArg.isNotEmpty( contentPath, "contentPath" );
        mgr.run( new Task< Void >() {
            
            @Override
            public Void run( final Session session ) throws Exception {
                final Node contentNode = mgr.fileNode( session, contentPath );
                ModelType type = modelType;
                if ( modelType == null ) {
                    // If no model type supplied, use default model type if one exists
                    type = mgr.modelTypeManager().defaultModelType( contentNode,
                                                                    mgr.modelTypeManager().applicableModelTypes( contentNode ) );
                    if ( type == null )
                        throw new IllegalArgumentException( ModelerI18n.unableToDetermineDefaultModelType.text( contentPath ) );
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
     *        the name of the content as it should be stored in the repository. Must not be empty.
     * @param stream
     *        the content to be imported. Must not be <code>null</code>.
     * @param workspaceParentPath
     *        the path of the parent path where the content should be imported
     * @return the path the to imported content
     * @throws ModelerException
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
    
    public ModelTypeManager modelTypeManager() {
        return mgr.modelTypeManager();
    }
    
    public String modeShapeConfigurationPath() {
        return mgr.modeShapeConfigurationPath();
    }
    
    public void setModeShapeConfigurationPath( final String modeShapeConfigurationPath ) {
        mgr.setModeShapeConfigurationPath( modeShapeConfigurationPath );
    }
    
    public void stop() throws ModelerException {
        mgr.stop();
    }
}
