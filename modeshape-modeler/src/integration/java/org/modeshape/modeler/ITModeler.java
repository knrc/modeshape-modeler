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

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

import javax.jcr.Node;
import javax.jcr.Session;

import org.junit.Test;
import org.modeshape.modeler.impl.Manager;
import org.modeshape.modeler.impl.ModelTypeManagerImpl;
import org.modeshape.modeler.impl.Task;
import org.modeshape.modeler.integration.BaseIntegrationTest;

@SuppressWarnings( "javadoc" )
public class ITModeler extends BaseIntegrationTest {
    
    private static final String XSD_SEQUENCER = "org.modeshape.sequencer.xsd.Xsd";
    
    // private void createDefaultModel( final String fileName,
    // final String modelType ) throws Exception {
    // final String path = upload( fileName );
    // modeler.createDefaultModel( path );
    // final Session session = session();
    // assertThat( session.getNode( path ).getNode( modelType ), notNullValue() );
    // session.logout();
    // }
    //
    // @Test
    // public void shouldCreateModelOfDefaultTypeIfNotSupplied() throws Exception {
    // createDefaultModel( "pom.xml", XmlSequencer.class.getSimpleName() );
    // createDefaultModel( "Books.xsd", XsdSequencer.class.getSimpleName() );
    // }
    
    @Test
    public void shouldCreateModelOfSuppliedType() throws Exception {
        modelTypeManager.installSequencers( SEQUENCER_REPOSITORY, "xml" );
        modelTypeManager.installSequencers( SEQUENCER_REPOSITORY, "sramp" );
        modelTypeManager.installSequencers( SEQUENCER_REPOSITORY, "xsd" );
        final String path = importContent( XSD_CONTENT );
        ModelType modelType = null;
        for ( final ModelType type : modelTypeManager.modelTypes( path ) ) {
            if ( type.name().equals( XSD_SEQUENCER ) ) {
                modelType = type;
                break;
            }
        }
        modeler.createModel( path, modelType );
        final Session session = session();
        assertThat( session.getNode( path ).hasNode( XSD_SEQUENCER ), is( true ) );
        session.logout();
    }
    
    @Test( expected = ModelerException.class )
    public void shouldFailProcessingDependenciesWhenNodeIsNotAModelNode() throws Exception {
        final Modeler accessModeler = modeler;
        modeler.manager.run( new Task< Void >() {
            
            @Override
            public Void run( final Session session ) throws Exception {
                final Node rootNode = session().getRootNode();
                accessModeler.processDependencies( rootNode );
                return null;
            }
        } );
    }
    
    @Test( expected = ModelerException.class )
    public void shouldFailToCreateDefaultModelIfFileIsInvalid() throws Exception {
        modelTypeManager.installSequencers( SEQUENCER_REPOSITORY, "xml" );
        modeler.createDefaultModel( importContent( XML_CONTENT + "<stuff>" ) );
    }
    
    @Test( expected = ModelerException.class )
    public void shouldFailToCreateModelIfFileIsInvalid() throws Exception {
        modelTypeManager.installSequencers( SEQUENCER_REPOSITORY, "xml" );
        modeler.createModel( importContent( XML_CONTENT + "<stuff>" ), modelTypeManager.modelTypes().iterator().next() );
    }
    
    @Test( expected = ModelerException.class )
    public void shouldFailToCreateModelIfTypeIsInapplicable() throws Exception {
        modelTypeManager.installSequencers( SEQUENCER_REPOSITORY, "xml" );
        modeler.createModel( importContent( "stuff" ), modelTypeManager.modelTypes().iterator().next() );
    }
    
    @Test
    public void shouldNotFindDependencyProcessorForXsdModelNode() throws Exception {
        modelTypeManager.installSequencers( SEQUENCER_REPOSITORY, "sramp" );
        modelTypeManager.installSequencers( SEQUENCER_REPOSITORY, "xsd" );
        
        // find XSD model type
        ModelType xsdModelType = null;
        
        for ( final ModelType type : modelTypeManager.modelTypes() ) {
            if ( type.name().equals( XSD_SEQUENCER ) ) {
                xsdModelType = type;
                break;
            }
        }
        
        assertThat( xsdModelType, notNullValue() );
        
        final String path = importContent( XSD_CONTENT );
        final String modelNodePath = modeler.createModel( path, xsdModelType );
        final ModelTypeManagerImpl modelTypeMgr = modelTypeManager;
        
        modeler.manager.run( new Task< Void >() {
            
            @Override
            public Void run( final Session session ) throws Exception {
                final Node modelNode = session.getNode( modelNodePath );
                assertThat( modelTypeMgr.dependencyProcessor( modelNode ), nullValue() );
                return null;
            }
        } );
    }
    
    @Test
    public void shouldProcessModelNodeWhenNoDependencyProcessorFound() throws Exception {
        final Modeler accessModeler = modeler;
        modeler.manager.run( new Task< Void >() {
            
            @Override
            public Void run( final Session session ) throws Exception {
                final Node rootNode = session.getRootNode();
                final Node modelNode = rootNode.addNode( "elvis" );
                modelNode.addMixin( Manager.MODEL_NODE_MIXIN );
                assertThat( accessModeler.processDependencies( modelNode ), nullValue() );
                return null;
            }
        } );
    }
    
}
