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
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

import java.net.URL;
import java.util.Map;

import org.junit.Test;
import org.modeshape.jcr.JcrLexicon;
import org.modeshape.modeler.Model;
import org.modeshape.modeler.ModelObject;
import org.modeshape.modeler.ModelType;
import org.modeshape.modeler.test.BaseTest;

@SuppressWarnings( "javadoc" )
public class ModelImplTest extends BaseTest {
    
    private static final String ARTIFACT_NAME = "artifact";
    
    private Model model;
    
    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.modeler.test.BaseTest#before()
     */
    @Override
    public void before() throws Exception {
        super.before();
        modelTypeManager.registerModelTypeRepository( MODEL_TYPE_REPOSITORY );
        modelTypeManager.install( "xml" );
        final String path = modeler.importArtifact( new URL( "File:" + ARTIFACT_NAME ), stream( XML_ARTIFACT ), null );
        model = modeler.createModel( path, modelTypeManager.modelType( XML_MODEL_TYPE_NAME ) );
        assertThat( model, notNullValue() );
    }
    
    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToGetBooleanValueIfNonBooleanProperty() throws Exception {
        model.booleanValue( JcrLexicon.PRIMARY_TYPE.toString() );
    }
    
    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToGetBooleanValueIfPropertyEmpty() throws Exception {
        model.booleanValue( " " );
    }
    
    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToGetBooleanValueIfPropertyNull() throws Exception {
        model.booleanValue( null );
    }
    
    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToGetBooleanValuesIfNonBooleanProperty() throws Exception {
        model.booleanValues( JcrLexicon.PRIMARY_TYPE.toString() );
    }
    
    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToGetBooleanValuesIfPropertyEmpty() throws Exception {
        model.booleanValues( " " );
    }
    
    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToGetBooleanValuesIfPropertyNull() throws Exception {
        model.booleanValues( null );
    }
    
    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToGetChildIfNameEmpty() throws Exception {
        model.child( " " );
    }
    
    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToGetChildIfNameNull() throws Exception {
        model.child( null );
    }
    
    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToGetChildrenIfNameEmpty() throws Exception {
        model.children( " " );
    }
    
    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToGetChildrenIfNameNull() throws Exception {
        model.children( null );
    }
    
    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToGetLongValueIfNonLongProperty() throws Exception {
        model.longValue( JcrLexicon.PRIMARY_TYPE.toString() );
    }
    
    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToGetLongValueIfPropertyEmpty() throws Exception {
        model.longValue( " " );
    }
    
    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToGetLongValueIfPropertyNull() throws Exception {
        model.longValue( null );
    }
    
    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToGetLongValuesIfNonLongProperty() throws Exception {
        model.longValues( JcrLexicon.PRIMARY_TYPE.toString() );
    }
    
    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToGetLongValuesIfPropertyEmpty() throws Exception {
        model.longValues( " " );
    }
    
    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToGetLongValuesIfPropertyNull() throws Exception {
        model.longValues( null );
    }
    
    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToGetStringValueIfMultiValuedProperty() throws Exception {
        model.stringValue( JcrLexicon.MIXIN_TYPES.toString() );
    }
    
    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToGetStringValueIfPropertyEmpty() throws Exception {
        model.stringValue( " " );
    }
    
    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToGetStringValueIfPropertyNull() throws Exception {
        model.stringValue( null );
    }
    
    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToGetStringValuesIfPropertyEmpty() throws Exception {
        model.stringValues( " " );
    }
    
    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToGetStringValuesIfPropertyNull() throws Exception {
        model.stringValues( null );
    }
    
    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToIndicateIfChildHasSameNameSiblingsIfNameEmpty() throws Exception {
        model.childHasSameNameSiblings( " " );
    }
    
    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToIndicateIfChildHasSameNameSiblingsIfNameNull() throws Exception {
        model.childHasSameNameSiblings( null );
    }
    
    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToIndicateIfPropertyHasMultipleValuesIfNameEmpty() throws Exception {
        model.propertyHasMultipleValues( " " );
    }
    
    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToIndicateIfPropertyHasMultipleValuesIfNameNull() throws Exception {
        model.propertyHasMultipleValues( null );
    }
    
    @Test
    public void shouldGetChild() throws Exception {
        final ModelObject root = model.child( XML_ROOT );
        assertThat( root, notNullValue() );
        assertThat( root.name(), is( XML_ROOT ) );
        assertThat( root.child( XML_SAME_NAME_SIBLING ), notNullValue() );
    }
    
    @Test
    public void shouldGetChildName() throws Exception {
        assertThat( model.childrenByName().get( XML_ROOT ).name(), is( XML_ROOT ) );
    }
    
    @Test
    public void shouldGetChildren() throws Exception {
        final ModelObject[] children = model.children( XML_ROOT );
        assertThat( children, notNullValue() );
        assertThat( children.length, is( 1 ) );
        assertThat( children[ 0 ].name(), is( XML_ROOT ) );
        assertThat( children[ 0 ].children( XML_SAME_NAME_SIBLING ).length, is( 2 ) );
    }
    
    @Test
    public void shouldGetChildrenByName() throws Exception {
        assertThat( model.childrenByName().isEmpty(), is( false ) );
    }
    
    @Test
    public void shouldGetEmptyChildrenByNameMapIfNoChildren() throws Exception {
        final Map< String, ModelObject > childrenByName = model.child( XML_ROOT ).child( XML_LEAF ).childrenByName();
        assertThat( childrenByName, notNullValue() );
        assertThat( childrenByName.isEmpty(), is( true ) );
    }
    
    @Test
    public void shouldGetMinimumPropertyValuesByNameMapIfNoProperties() throws Exception {
        final Map< String, Object > propertyValuesByName = model.propertyValuesByName();
        assertThat( propertyValuesByName, notNullValue() );
        assertThat( propertyValuesByName.size(), is( 1 ) );
    }
    
    @Test
    public void shouldGetMixinTypes() throws Exception {
        final String[] types = model.mixinTypes();
        assertThat( types, notNullValue() );
        assertThat( types.length > 0, is( true ) );
        assertThat( types[ 0 ], is( "mm:model" ) ); // TODO replace with lexicon value
    }
    
    @Test
    public void shouldGetModelType() throws Exception {
        final ModelType type = model.modelType();
        assertThat( type, notNullValue() );
        assertThat( type.name(), is( XML_MODEL_TYPE_NAME ) );
    }
    
    @Test
    public void shouldGetName() throws Exception {
        assertThat( model.name(), is( ARTIFACT_NAME ) );
    }
    
    @Test
    public void shouldGetNullValueIfBooleanPropertyNotFound() throws Exception {
        assertThat( model.booleanValue( "bogus" ), nullValue() );
    }
    
    @Test
    public void shouldGetNullValueIfChildNotFound() throws Exception {
        assertThat( model.child( "bogus" ), nullValue() );
    }
    
    @Test
    public void shouldGetNullValueIfLongPropertyNotFound() throws Exception {
        assertThat( model.longValue( "bogus" ), nullValue() );
    }
    
    @Test
    public void shouldGetNullValueIfStringPropertyNotFound() throws Exception {
        assertThat( model.stringValue( "bogus" ), nullValue() );
    }
    
    @Test
    public void shouldGetPath() {
        assertThat( model.path(), is( '/' + ARTIFACT_NAME + '/' + XML_MODEL_TYPE_NAME ) );
    }
    
    @Test
    public void shouldGetPrimaryType() throws Exception {
        assertThat( model.primaryType(), is( "modexml:document" ) );
    }
    
    @Test
    public void shouldGetPropertyValuesByName() throws Exception {
        final ModelObject root = model.child( XML_ROOT );
        assertThat( root.propertyValuesByName().isEmpty(), is( false ) );
        assertThat( root.child( XML_LEAF ).propertyValuesByName().isEmpty(), is( true ) );
    }
    
    @Test
    public void shouldGetStringValue() throws Exception {
        assertThat( model.stringValue( JcrLexicon.PRIMARY_TYPE.toString() ), is( "modexml:document" ) );
    }
    
    @Test
    public void shouldGetStringValues() throws Exception {
        final String[] vals = model.stringValues( JcrLexicon.PRIMARY_TYPE.toString() );
        assertThat( vals, notNullValue() );
        assertThat( vals.length, is( 1 ) );
        assertThat( vals[ 0 ], is( "modexml:document" ) );
    }
    
    @Test
    public void shouldIndicateIfChildHasSameNameSiblings() throws Exception {
        assertThat( model.childHasSameNameSiblings( XML_ROOT ), is( false ) );
        assertThat( model.child( XML_ROOT ).childHasSameNameSiblings( XML_SAME_NAME_SIBLING ), is( true ) );
        assertThat( model.childHasSameNameSiblings( "bogus" ), is( false ) );
    }
    
    @Test
    public void shouldIndicateIfPropertyHasMultipleValues() throws Exception {
        assertThat( model.propertyHasMultipleValues( JcrLexicon.PRIMARY_TYPE.toString() ), is( false ) );
        assertThat( model.propertyHasMultipleValues( JcrLexicon.MIXIN_TYPES.toString() ), is( true ) );
        assertThat( model.propertyHasMultipleValues( "bogus" ), is( false ) );
    }
}
