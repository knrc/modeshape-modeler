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
import static org.junit.Assert.assertThat;

import java.io.File;

import javax.jcr.Node;
import javax.jcr.Session;

import org.junit.Test;
import org.modeshape.jcr.JcrLexicon;
import org.modeshape.modeler.impl.Task;
import org.modeshape.modeler.test.BaseTest;

/**
 * Tests for {@link Modeler}.
 */
@SuppressWarnings( "javadoc" )
public final class ModelerTest extends BaseTest {
    
    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToCreateDefaultModelIfPathIsEmpty() throws Exception {
        modeler.createDefaultModel( " " );
    }
    
    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToCreateDefaultModelIfPathIsNull() throws Exception {
        modeler.createDefaultModel( null );
    }
    
    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToCreateModelIfNoDefaultModelType() throws Exception {
        modeler.createDefaultModel( importContent( "stuff" ) );
    }
    
    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToCreateModelIfPathIsEmpty() throws Exception {
        modeler.createModel( "", null );
    }
    
    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToCreateModelIfPathIsNull() throws Exception {
        modeler.createModel( null, null );
    }
    
    @Test( expected = ModelerException.class )
    public void shouldFailToCreateModelIfPathNotFound() throws Exception {
        modeler.createModel( "dummy", null );
    }
    
    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToImportContentIfNameIsEmpty() throws Exception {
        modeler.importContent( "", stream( "stuff" ), null );
    }
    
    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToImportContentIfNameIsNull() throws Exception {
        modeler.importContent( null, stream( "stuff" ), null );
    }
    
    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToImportContentIfStreamIsNull() throws Exception {
        modeler.importContent( "stuff", null, null );
    }
    
    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToImportFileIfNotFound() throws Exception {
        modeler.importContent( new File( "dummy.file" ), null );
    }
    
    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToImportIfFileIsNull() throws Exception {
        modeler.importContent( null, null );
    }
    
    @Test
    public void shouldGetChangedModeShapeConfigurationPath() {
        assertThat( modeler.modeShapeConfigurationPath(), is( TEST_MODESHAPE_CONFIGURATION_PATH ) );
    }
    
    @Test
    public void shouldGetDefaultModeShapeConfigurationPathIfNotSet() throws Exception {
        this.modeler.close();
        final Modeler modeler = new Modeler( this.modeler.repositoryStoreParentPath() );
        assertThat( modeler.modeShapeConfigurationPath(), is( Modeler.DEFAULT_MODESHAPE_CONFIGURATION_PATH ) );
        modeler.close();
    }
    
    @Test
    public void shouldGetRepositoryStoreParentPath() {
        assertThat( modeler.repositoryStoreParentPath(), is( TEST_REPOSITORY_STORE_PARENT_PATH ) );
    }
    
    @Test
    public void shouldImportContent() throws Exception {
        final String path = modeler.importContent( "stuff", stream( "stuff" ), null );
        assertThat( path, is( "/stuff" ) );
        verifyPathExistsWithContent( path );
    }
    
    @Test
    public void shouldImportContentToSuppliedPath() throws Exception {
        final String path = modeler.importContent( "stuff", stream( "stuff" ), "/test" );
        assertThat( path, is( "/test/stuff" ) );
        verifyPathExistsWithContent( path );
    }
    
    @Test
    public void shouldImportFile() throws Exception {
        final String path = modeler.importContent( new File( getClass().getClassLoader().getResource( "Books.xsd" ).toURI() ),
                                                   null );
        assertThat( path, is( "/Books.xsd" ) );
        verifyPathExistsWithContent( path );
    }
    
    @Test
    public void shouldImportFileToSuppliedPath() throws Exception {
        final String path = modeler.importContent( new File( getClass().getClassLoader().getResource( "Books.xsd" ).toURI() ),
                                                   "/test" );
        assertThat( path, is( "/test/Books.xsd" ) );
        verifyPathExistsWithContent( path );
    }
    
    private void verifyPathExistsWithContent( final String path ) throws Exception {
        manager.run( new Task< Void >() {
            
            @Override
            public Void run( final Session session ) throws Exception {
                final Node node = session.getNode( path );
                assertThat( node, notNullValue() );
                assertThat( node.getNode( JcrLexicon.CONTENT.getString() ), notNullValue() );
                assertThat( node.getNode( JcrLexicon.CONTENT.getString() ).getProperty( JcrLexicon.DATA.getString() ), notNullValue() );
                return null;
            }
        } );
    }
}
