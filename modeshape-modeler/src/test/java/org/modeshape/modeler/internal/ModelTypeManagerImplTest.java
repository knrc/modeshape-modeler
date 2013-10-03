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
package org.modeshape.modeler.internal;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

import java.net.URL;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.Value;

import org.junit.Test;
import org.modeshape.modeler.ModelType;
import org.modeshape.modeler.Modeler;
import org.modeshape.modeler.TestUtil;
import org.modeshape.modeler.test.BaseTest;

@SuppressWarnings( "javadoc" )
public class ModelTypeManagerImplTest extends BaseTest {
    
    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToGetApplicableModelTypesIfPathIsEmpty() throws Exception {
        modelTypeManager.modelTypesForArtifact( " " );
    }
    
    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToGetApplicableModelTypesIfPathIsNull() throws Exception {
        modelTypeManager.modelTypesForArtifact( ( String ) null );
    }
    
    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToGetDefaultModelTypeIfPathIsEmpty() throws Exception {
        modelTypeManager.defaultModelType( " " );
    }
    
    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToGetDefaultModelTypeIfPathIsNull() throws Exception {
        modelTypeManager.defaultModelType( null );
    }
    
    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToGetModelTypesForCategoryIfCategoryEmpty() {
        modelTypeManager.modelTypesForCategory( " " );
    }
    
    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToGetModelTypesForCategoryIfCategoryNull() {
        modelTypeManager.modelTypesForCategory( null );
    }
    
    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToInstallModelTypesIfCategoryIsEmpty() throws Exception {
        modelTypeManager.install( " " );
    }
    
    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToInstallModelTypesIfCategoryIsNull() throws Exception {
        modelTypeManager.install( null );
    }
    
    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToInstallModelTypesIfCategoryNotFound() throws Exception {
        modelTypeManager.install( "bogus" );
    }
    
    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToRegisterModelTypeRepositoryIfUrlIsNull() throws Exception {
        modelTypeManager.registerModelTypeRepository( null );
    }
    
    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToUnregisterModelTypeRepositoryIfUrlIsNull() throws Exception {
        modelTypeManager.unregisterModelTypeRepository( null );
    }
    
    @Test
    public void shouldGetDefaultRegisteredModelTypeRepositories() {
        final List< URL > repos = modelTypeManager.modelTypeRepositories();
        assertThat( repos, notNullValue() );
        assertThat( repos.isEmpty(), is( false ) );
    }
    
    @Test
    public void shouldGetEmptyApplicableModelTypesIfFileHasUknownMimeType() throws Exception {
        final Set< ModelType > types = modelTypeManager.modelTypesForArtifact( importArtifact( "stuff" ) );
        assertThat( types, notNullValue() );
        assertThat( types.isEmpty(), is( true ) );
    }
    
    @Test
    public void shouldGetExistingRegisteredModelTypeRepositoriesIfRegisteringRegisteredUrl() throws Exception {
        final List< URL > origRepos = modelTypeManager.modelTypeRepositories();
        final List< URL > repos = modelTypeManager.registerModelTypeRepository( MODEL_TYPE_REPOSITORY );
        assertThat( repos, notNullValue() );
        assertThat( repos.equals( origRepos ), is( true ) );
    }
    
    @Test
    public void shouldGetExistingRegisteredModelTypeRepositoriesIfUnregisteringUnregisteredUrl() throws Exception {
        final int size = modelTypeManager.modelTypeRepositories().size();
        final List< URL > repos = modelTypeManager.unregisterModelTypeRepository( new URL( "file:" ) );
        assertThat( repos, notNullValue() );
        assertThat( repos.size(), is( size ) );
    }
    
    @Test
    public void shouldGetModelTypeCategories() throws Exception {
        modelTypeManager.registerModelTypeRepository( MODEL_TYPE_REPOSITORY );
        modelTypeManager.install( "java" );
        assertThat( modelTypeManager.modelTypeCategories().size(), is( 1 ) );
        assertThat( modelTypeManager.modelTypeCategories().contains( "java" ), is( true ) );
    }
    
    @Test
    public void shouldGetNullDefaultModelTypeIfFileHasUknownMimeType() throws Exception {
        assertThat( modelTypeManager.defaultModelType( importArtifact( "stuff" ) ), nullValue() );
    }
    
    @Test
    public void shouldInstallModelTypes() throws Exception {
        modelTypeManager.registerModelTypeRepository( MODEL_TYPE_REPOSITORY );
        final Set< String > potentialSequencerClassNames = modelTypeManager.install( "java" );
        assertThat( potentialSequencerClassNames.isEmpty(), is( true ) );
        assertThat( modelTypeManager.modelTypes().isEmpty(), is( false ) );
        final ModelTypeImpl type = ( ModelTypeImpl ) modelTypeManager.modelTypes().iterator().next();
        assertThat( type.category(), is( "java" ) );
        assertThat( type.sequencerClass, notNullValue() );
    }
    
    @Test
    public void shouldLoadState() throws Exception {
        modeler.close();
        int repos;
        try ( Modeler modeler = new Modeler( Modeler.DEFAULT_MODESHAPE_CONFIGURATION_PATH, TEST_REPOSITORY_STORE_PARENT_PATH ) ) {
            final ModelTypeManagerImpl modelTypeManager = ( ModelTypeManagerImpl ) modeler.modelTypeManager();
            repos = modelTypeManager.modelTypeRepositories().size();
            modelTypeManager.registerModelTypeRepository( MODEL_TYPE_REPOSITORY );
            modelTypeManager.install( "java" );
            modelTypeManager.install( "xsd" );
        }
        try ( Modeler modeler = new Modeler( Modeler.DEFAULT_MODESHAPE_CONFIGURATION_PATH, TEST_REPOSITORY_STORE_PARENT_PATH ) ) {
            final ModelTypeManagerImpl modelTypeManager = ( ModelTypeManagerImpl ) modeler.modelTypeManager();
            assertThat( modelTypeManager.modelTypeRepositories().size(), not( repos ) );
            assertThat( modelTypeManager.modelTypes().isEmpty(), is( false ) );
            assertThat( modelTypeManager.libraryClassLoader.getURLs().length > 0, is( true ) );
            assertThat( modelTypeManager.potentialSequencerClassNames.isEmpty(), is( false ) );
            TestUtil.manager( modeler ).run( modelTypeManager, new SystemTask< Void >() {
                
                @Override
                public Void run( final Session session,
                                 final Node systemNode ) throws Exception {
                    assertThat( systemNode.getProperty( ModelTypeManagerImpl.ZIPS ).getValues().length > 0, is( true ) );
                    return null;
                }
            } );
        }
    }
    
    @Test
    public void shouldNotInstallModelTypeCategoryIfAlreadyInstalled() throws Exception {
        manager.run( modelTypeManager, new SystemTask< Void >() {
            
            @Override
            public Void run( final Session session,
                             final Node systemNode ) throws Exception {
                final String version = manager.repository.getDescriptor( Repository.REP_VERSION_DESC );
                final String archiveName = "modeshape-sequencer-test-" + version + "-module-with-dependencies.zip";
                final Value[] vals = new Value[] { session.getValueFactory().createValue( archiveName ) };
                systemNode.setProperty( ModelTypeManagerImpl.ZIPS, vals );
                session.save();
                return null;
            }
        } );
        modelTypeManager.registerModelTypeRepository( MODEL_TYPE_REPOSITORY );
        modelTypeManager.install( "test" );
        assertThat( modelTypeManager.modelTypes().isEmpty(), is( true ) );
    }
    
    @Test
    public void shouldNotReturnNullModelTypeCategories() {
        assertThat( modelTypeManager.modelTypeCategories(), notNullValue() );
        assertThat( modelTypeManager.modelTypeCategories().isEmpty(), is( true ) );
    }
    
    @Test
    public void shouldNotReturnNullModelTypesForCategory() {
        assertThat( modelTypeManager.modelTypesForCategory( "bogus" ), notNullValue() );
        assertThat( modelTypeManager.modelTypesForCategory( "bogus" ).isEmpty(), is( true ) );
    }
    
    @Test
    public void shouldRegisterModelTypeRepository() throws Exception {
        final int size = modelTypeManager.modelTypeRepositories().size();
        final URL repo = new URL( "file:" );
        final List< URL > repos = modelTypeManager.registerModelTypeRepository( repo );
        assertThat( repos, notNullValue() );
        assertThat( repos.size(), is( size + 1 ) );
        assertThat( repos.contains( repo ), is( true ) );
    }
    
    @Test
    public void shouldUnregisterModelTypeRepository() throws Exception {
        final int size = modelTypeManager.modelTypeRepositories().size();
        final URL repo = new URL( "file:" );
        modelTypeManager.registerModelTypeRepository( repo );
        final List< URL > repos = modelTypeManager.unregisterModelTypeRepository( repo );
        assertThat( repos, notNullValue() );
        assertThat( repos.size(), is( size ) );
    }
}
