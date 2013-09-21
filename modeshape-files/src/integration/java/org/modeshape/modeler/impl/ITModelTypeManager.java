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
package org.modeshape.modeler.impl;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.util.Map;

import org.junit.Test;
import org.modeshape.modeler.ModelTypeManager;
import org.modeshape.modeler.integration.BaseIntegrationTest;

/**
 * Tests for {@link ModelTypeManagerImpl}.
 */
public class ITModelTypeManager extends BaseIntegrationTest {
    
    private ModelTypeManager modelTypes;
    
    @Override
    public void before() {
        super.before();
        modelTypes = fileMgr.modelTypeManager();
    }
    
    @Test
    public void shouldGetSequencerArchives() throws Exception {
        assertThat( modelTypes.sequencerArchives( SEQUENCER_REPOSITORY + "modeshape-sequencer-zip/" ).isEmpty(), is( false ) );
    }
    
    @Test
    public void shouldGetSequencerGroups() throws Exception {
        assertThat( modelTypes.sequencerGroups( SEQUENCER_REPOSITORY ).isEmpty(), is( false ) );
    }
    
    @Test
    public void shouldIniitializeSequencerRepositories() {
        assertThat( modelTypes.sequencerRepositories().contains( ModelTypeManager.JBOSS_SEQUENCER_REPOSITORY ), is( true ) );
        assertThat( modelTypes.sequencerRepositories().contains( ModelTypeManager.MAVEN_SEQUENCER_REPOSITORY ), is( true ) );
    }
    
    @Test
    public void shouldInstallSequencerArchiveOfJars() throws Exception {
        modelTypes.installSequencers( sequencerUrl( "java" ) );
    }
    
    @Test
    public void shouldInstallSequencerjar() throws Exception {
        final String version = modeShapeVersion();
        modelTypes.installSequencers( SEQUENCER_REPOSITORY + "modeshape-sequencer-zip/" + version + "/modeshape-sequencer-zip-"
                                      + version + ".jar" );
    }
    
    @Test
    public void shouldNotInstallDuplicatejars() throws Exception {
        final String version = modeShapeVersion();
        modelTypes.installSequencers( SEQUENCER_REPOSITORY + "modeshape-sequencer-zip/" + version + "/modeshape-sequencer-zip-"
                                      + version + ".jar" );
        final Map< String, String > jarsByClass = ( ( ModelTypeManagerImpl ) fileMgr.modelTypeManager() ).classLoader.jarsByClass;
        final int size = jarsByClass.size();
        modelTypes.installSequencers( SEQUENCER_REPOSITORY + "modeshape-sequencer-zip/" + version + "/modeshape-sequencer-zip-"
                                      + version + ".jar" );
        assertThat( jarsByClass.size(), is( size ) );
    }
}
