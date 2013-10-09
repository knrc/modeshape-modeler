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
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.modeshape.jcr.JcrLexicon;
import org.modeshape.modeler.ModelObject;

@SuppressWarnings( "javadoc" )
public class ModelObjectImplTest extends BaseModelObjectImplTest {
    
    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.modeler.internal.BaseModelObjectImplTest#before()
     */
    @Override
    public void before() throws Exception {
        super.before();
        modelObject = modelObject.child( XML_ROOT );
    }
    
    @Test
    public void shouldGetAbsolutePath() {
        assertThat( modelObject.absolutePath(), is( '/' + MODEL_NAME + '/' + XML_ROOT ) );
    }
    
    @Test
    public void shouldGetChild() throws Exception {
        final ModelObject child = modelObject.child( XML_LEAF );
        assertThat( child, notNullValue() );
        assertThat( child.name(), is( XML_LEAF ) );
    }
    
    @Test
    public void shouldGetChildren() throws Exception {
        final ModelObject[] children = modelObject.children( XML_SAME_NAME_SIBLING );
        assertThat( children, notNullValue() );
        assertThat( children.length, is( 2 ) );
        assertThat( children[ 0 ].name(), is( XML_SAME_NAME_SIBLING ) );
        assertThat( children[ 1 ].name(), is( XML_SAME_NAME_SIBLING ) );
    }
    
    @Test
    public void shouldGetChildrenByName() throws Exception {
        assertThat( modelObject.childrenByName(), notNullValue() );
        assertThat( modelObject.childrenByName().size(), is( 2 ) );
    }
    
    @Test
    public void shouldGetEmptyChildrenByNameMapIfNoChildren() throws Exception {
        final Map< String, List< ModelObject > > childrenByName = modelObject.child( XML_LEAF ).childrenByName();
        assertThat( childrenByName, notNullValue() );
        assertThat( childrenByName.isEmpty(), is( true ) );
    }
    
    @Test
    public void shouldGetMixinTypes() throws Exception {
        final String[] types = modelObject.mixinTypes();
        assertThat( types, notNullValue() );
        assertThat( types.length, is( 0 ) );
    }
    
    @Test
    public void shouldGetModel() throws Exception {
        assertThat( modelObject.model(), notNullValue() );
    }
    
    @Test
    public void shouldGetModelPath() throws Exception {
        assertThat( modelObject.modelRelativePath(), is( XML_ROOT ) );
    }
    
    @Test
    public void shouldGetName() throws Exception {
        assertThat( modelObject.name(), is( XML_ROOT ) );
    }
    
    @Test
    public void shouldGetPrimaryType() throws Exception {
        assertThat( modelObject.primaryType(), is( "modexml:element" ) );
    }
    
    @Test
    public void shouldGetPropertyValuesByName() throws Exception {
        assertThat( modelObject.propertyValuesByName().isEmpty(), is( false ) );
        assertThat( modelObject.child( XML_LEAF ).propertyValuesByName().isEmpty(), is( true ) );
    }
    
    @Test
    public void shouldGetStringValue() throws Exception {
        assertThat( modelObject.stringValue( XML_ROOT_PROPERTY ), is( XML_STRING_VALUE ) );
    }
    
    @Test
    public void shouldIndicateIfChildHasSameNameSiblings() throws Exception {
        assertThat( modelObject.childHasSameNameSiblings( XML_LEAF ), is( false ) );
        assertThat( modelObject.childHasSameNameSiblings( XML_SAME_NAME_SIBLING ), is( true ) );
        assertThat( modelObject.childHasSameNameSiblings( "bogus" ), is( false ) );
    }
    
    @Test
    public void shouldIndicateIfPropertyHasMultipleValues() throws Exception {
        assertThat( modelObject.propertyHasMultipleValues( JcrLexicon.PRIMARY_TYPE.toString() ), is( false ) );
        assertThat( modelObject.propertyHasMultipleValues( JcrLexicon.MIXIN_TYPES.toString() ), is( false ) );
        assertThat( modelObject.propertyHasMultipleValues( "bogus" ), is( false ) );
    }
}
