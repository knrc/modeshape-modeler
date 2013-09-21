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
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Session;

import org.junit.Test;
import org.modeshape.jcr.JcrLexicon;
import org.modeshape.modeler.Modeler;
import org.modeshape.modeler.ModelerException;
import org.modeshape.modeler.ModelType;
import org.modeshape.modeler.impl.Manager;
import org.modeshape.modeler.test.BaseTest;

/**
 * Tests for {@link Modeler}.
 */
public final class ModelerTest extends BaseTest {
    
    private static final String TEST_MODESHAPE_CONFIGURATION_PATH = "testModeShapeConfig.json";
    
    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToCreateDefaultModelIfPathIsEmpty() throws Exception {
        modeler.createDefaultModel( "" );
    }
    
    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToCreateDefaultModelIfPathIsNull() throws Exception {
        modeler.createDefaultModel( null );
    }
    
    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToCreateModelIfNoDefaultModelType() throws Exception {
        modeler.createDefaultModel( upload( "LICENSE" ) );
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
    public void shouldFailToCreateModelIfTypeIsUnknown() throws Exception {
        modeler.createModel( upload( "Books.xsd" ), "dummy" );
    }
    
    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToGetApplicableModelTypesIfPathIsEmpty() throws Exception {
        modeler.applicableModelTypes( "" );
    }
    
    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToGetApplicableModelTypesIfPathIsNull() throws Exception {
        modeler.applicableModelTypes( ( String ) null );
    }
    
    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToGetDefaultModelTypeIfPathIsEmpty() throws Exception {
        modeler.defaultModelType( "" );
    }
    
    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToGetDefaultModelTypeIfPathIsNull() throws Exception {
        modeler.defaultModelType( null );
    }
    
    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToUploadFileIfNotFound() throws Exception {
        modeler.upload( new File( "dummy.file" ), null );
    }
    
    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToUploadIfFileIsNull() throws Exception {
        modeler.upload( null, null );
    }
    
    @Test
    public void shouldGetChangedModeShapeConfigurationPath() {
        modeler.setModeShapeConfigurationPath( TEST_MODESHAPE_CONFIGURATION_PATH );
        assertThat( modeler.modeShapeConfigurationPath(), is( TEST_MODESHAPE_CONFIGURATION_PATH ) );
    }
    
    @Test
    public void shouldGetDefaultModeShapeConfigurationPathIfNotSet() {
        assertThat( new Modeler().modeShapeConfigurationPath(), is( Manager.DEFAULT_MODESHAPE_CONFIGURATION_PATH ) );
    }
    
    @Test
    public void shouldGetDefaultModeShapeConfigurationPathIfSetToNull() {
        modeler.setModeShapeConfigurationPath( TEST_MODESHAPE_CONFIGURATION_PATH );
        assertThat( modeler.modeShapeConfigurationPath(), is( TEST_MODESHAPE_CONFIGURATION_PATH ) );
        modeler.setModeShapeConfigurationPath( null );
        assertThat( new Modeler().modeShapeConfigurationPath(), is( Manager.DEFAULT_MODESHAPE_CONFIGURATION_PATH ) );
    }
    
    @Test
    public void shouldGetEmptyApplicableModelTypesIfFileHasUknownMimeType() throws Exception {
        final Set< ModelType > types = modeler.applicableModelTypes( upload( "LICENSE" ) );
        assertThat( types, notNullValue() );
        assertThat( types.isEmpty(), is( true ) );
    }
    
    @Test
    public void shouldGetNoApplicableModelTypes() throws Exception {
        final Set< ModelType > types = modeler.applicableModelTypes( upload( "Books.xsd" ) );
        assertThat( types, notNullValue() );
        assertThat( types.isEmpty(), is( true ) );
    }
    
    @Test
    public void shouldGetNullDefaultModelType() throws Exception {
        assertThat( modeler.defaultModelType( upload( "Books.xsd" ) ), nullValue() );
    }
    
    @Test
    public void shouldGetNullDefaultModelTypeIfFileHasUknownMimeType() throws Exception {
        assertThat( modeler.defaultModelType( upload( "LICENSE" ) ), nullValue() );
    }
    
    @Test
    public void shouldGetSession() throws Exception {
        final Session session = session();
        assertThat( session, notNullValue() );
        final Session newSession = session();
        assertThat( newSession, notNullValue() );
        assertThat( newSession, not( session ) );
        session.logout();
        newSession.logout();
    }
    
    @Test
    public void shouldUploadToSuppliedPath() throws Exception {
        upload( "Books.xsd", "/test" );
    }
    
    private String upload( final String fileName ) throws Exception {
        return upload( fileName, null );
    }
    
    private String upload( final String fileName,
                           final String workspaceParentPath ) throws Exception {
        final String path = modeler.upload( new File( getClass().getClassLoader().getResource( fileName ).toURI() ),
                                            workspaceParentPath );
        String expectedPath = ( workspaceParentPath == null ? "/" : workspaceParentPath );
        if ( !expectedPath.endsWith( "/" ) ) expectedPath += '/';
        expectedPath += fileName;
        assertThat( path, is( expectedPath ) );
        final Session session = session();
        final Node node = session.getNode( path );
        assertThat( node, notNullValue() );
        assertThat( node.getNode( JcrLexicon.CONTENT.getString() ), notNullValue() );
        assertThat( node.getNode( JcrLexicon.CONTENT.getString() ).getProperty( JcrLexicon.DATA.getString() ), notNullValue() );
        session.logout();
        return path;
    }
}
