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
package org.modeshape.modeler.xsd.dependency;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.net.URL;

import javax.jcr.Node;
import javax.jcr.Session;

import org.junit.Before;
import org.junit.Test;
import org.modeshape.modeler.ModelType;
import org.modeshape.modeler.Modeler;
import org.modeshape.modeler.integration.BaseIntegrationTest;
import org.modeshape.modeler.internal.DependencyProcessor;
import org.modeshape.modeler.internal.ModelImpl;
import org.modeshape.modeler.internal.ModelerLexicon;
import org.modeshape.modeler.internal.Task;
import org.modeshape.modeler.xsd.XsdLexicon;

/**
 * An integration test for the {@link XsdDependencyProcessor}.
 */
@SuppressWarnings( "javadoc" )
public class ITXsdDependencyProcessor extends BaseIntegrationTest {
    
    private DependencyProcessor processor;
    
    @Before
    public void constructDependencyProcessor() throws Exception {
        this.processor = new XsdDependencyProcessor();
        this.modelTypeManager.install( "sramp" );
        this.modelTypeManager.install( "xsd" );
    }
    
    Modeler accessModeler() {
        return this.modeler;
    }
    
    DependencyProcessor accessProcessor() {
        return this.processor;
    }
    
    private ModelType xsdModelType() {
        ModelType xsdModelType = null;
        
        for ( final ModelType type : modelTypeManager.modelTypes() ) {
            if ( type.name().equals( XsdLexicon.MODEL_ID ) ) {
                xsdModelType = type;
                break;
            }
        }
        
        assertThat( xsdModelType, notNullValue() );
        return xsdModelType;
    }
    
    @Test
    public void shouldSetDependencyPathsOfMoviesXsd() throws Exception {
        final URL xsdUrl = getClass().getClassLoader().getResource( "Movies/movies.xsd" );
        final String path = this.modeler.importFile( new File( xsdUrl.toURI() ), null );
        assertThat( path, is( "/movies.xsd" ) );
        
        final ModelType xsdModelType = xsdModelType();
        final ModelImpl model = ( ModelImpl ) this.modeler.createModel( path, xsdModelType );
        
        this.manager.run( new Task< Node >() {
            
            @Override
            public Node run( final Session session ) throws Exception {
                final Node modelNode = session.getNode( model.path() );
                final String dependenciesPath = accessProcessor().process( modelNode, xsdModelType, accessModeler() );
                assertThat( dependenciesPath, notNullValue() );
                
                final Node dependenciesNode = session.getNode( dependenciesPath );
                assertThat( dependenciesNode.getNodes().getSize(), is( 1L ) );
                
                final Node dependencyNode = dependenciesNode.getNodes().nextNode();
                assertThat( dependencyNode.getPrimaryNodeType().getName(), is( ModelerLexicon.DEPENDENCY_NODE ) );
                assertThat( dependencyNode.getProperty( ModelerLexicon.PATH_PROPERTY ).getString(), is( "MovieDatatypes.xsd" ) );
                
                final String input = dependencyNode.getProperty( ModelerLexicon.SOURCE_REFERENCE_PROPERTY ).getValues()[ 0 ].getString();
                assertThat( input, is( "MovieDatatypes.xsd" ) );
                
                return null;
            }
        } );
        
    }
    
}
