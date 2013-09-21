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
package org.modeshape.files;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.Set;

import javax.jcr.Session;

import org.junit.Test;
import org.modeshape.modeler.ModelerException;
import org.modeshape.modeler.ModelType;
import org.modeshape.modeler.integration.BaseIntegrationTest;

public class ITModeler extends BaseIntegrationTest {
    
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
        modeler.modelTypeManager().installSequencers( sequencerUrl( "xml" ) );
        modeler.modelTypeManager().installSequencers( sequencerUrl( "sramp" ) );
        modeler.modelTypeManager().installSequencers( sequencerUrl( "xsd" ) );
        final String path = upload( "Books.xsd" );
        modeler.createModel( path, "Xml Model" );
        final Session session = session();
        assertThat( session.getNode( path ).getNode( "Xml Model" ), notNullValue() );
        session.logout();
    }
    
    @Test( expected = ModelerException.class )
    public void shouldFailToCreateModelIfFileIsInvalid() throws Exception {
        modeler.modelTypeManager().installSequencers( sequencerUrl( "xml" ) );
        modeler.createDefaultModel( upload( "bad.xml" ) );
    }
    
    @Test( expected = ModelerException.class )
    public void shouldFailToCreateModelIfTypeIsInapplicable() throws Exception {
        modeler.modelTypeManager().installSequencers( sequencerUrl( "xml" ) );
        modeler.createModel( upload( "LICENSE" ), "Xml Model" );
    }
    
    @Test
    public void shouldGetApplicableModelTypes() throws Exception {
        modeler.modelTypeManager().installSequencers( sequencerUrl( "sramp" ) );
        modeler.modelTypeManager().installSequencers( sequencerUrl( "xsd" ) );
        final Set< ModelType > types = modeler.applicableModelTypes( upload( "Books.xsd" ) );
        assertThat( types, notNullValue() );
        assertThat( types.isEmpty(), is( false ) );
    }
    
    private String upload( final String fileName ) throws Exception {
        return modeler.upload( new File( getClass().getClassLoader().getResource( fileName ).toURI() ), null );
    }
}
