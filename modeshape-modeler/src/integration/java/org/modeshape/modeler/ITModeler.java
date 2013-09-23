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

import java.util.Set;

import javax.jcr.Session;

import org.junit.Test;
import org.modeshape.modeler.integration.BaseIntegrationTest;

public class ITModeler extends BaseIntegrationTest {
    
    private static final String XML_CONTENT = "<?xml version='1.0' encoding='UTF-8'?>";
    private static final String XSD_CONTENT = XML_CONTENT + "<schema></schema>";
    
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
        final ModelTypeManager mgr = modeler.modelTypeManager();
        mgr.installSequencers( sequencerUrl( "xml" ) );
        mgr.installSequencers( sequencerUrl( "sramp" ) );
        mgr.installSequencers( sequencerUrl( "xsd" ) );
        final String path = importContent( XSD_CONTENT );
        modeler.createModel( path, mgr.applicableModelTypes( path ).iterator().next() );
        final Session session = session();
        assertThat( session.getNode( path ).getNode( "Xml Model" ), notNullValue() );
        session.logout();
    }
    
    @Test( expected = ModelerException.class )
    public void shouldFailToCreateDefaultModelIfFileIsInvalid() throws Exception {
        modeler.modelTypeManager().installSequencers( sequencerUrl( "xml" ) );
        modeler.createDefaultModel( importContent( XML_CONTENT + "<stuff>" ) );
    }
    
    @Test( expected = ModelerException.class )
    public void shouldFailToCreateModelIfFileIsInvalid() throws Exception {
        modeler.modelTypeManager().installSequencers( sequencerUrl( "xml" ) );
        modeler.createModel( importContent( XML_CONTENT + "<stuff>" ), modeler.modelTypeManager().modelTypes().iterator().next() );
    }
    
    @Test( expected = ModelerException.class )
    public void shouldFailToCreateModelIfTypeIsInapplicable() throws Exception {
        final ModelTypeManager mgr = modeler.modelTypeManager();
        mgr.installSequencers( sequencerUrl( "xml" ) );
        modeler.createModel( importContent( "stuff" ), mgr.modelTypes().iterator().next() );
    }
    
    @Test
    public void shouldGetApplicableModelTypes() throws Exception {
        final ModelTypeManager mgr = modeler.modelTypeManager();
        mgr.installSequencers( sequencerUrl( "sramp" ) );
        mgr.installSequencers( sequencerUrl( "xsd" ) );
        final Set< ModelType > types = modelTypeManager.applicableModelTypes( importContent( XSD_CONTENT ) );
        assertThat( types, notNullValue() );
        assertThat( types.isEmpty(), is( false ) );
    }
}
