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

import java.util.Set;

import org.modeshape.modeler.impl.ModelTypeManagerImpl;

public interface ModelTypeManager {
    
    String JBOSS_SEQUENCER_REPOSITORY = "https://repository.jboss.org/nexus/content/groups/public-jboss"
                                        + ModelTypeManagerImpl.MODESHAPE_GROUP;
    String MAVEN_SEQUENCER_REPOSITORY = "http://repo1.maven.org/maven2" + ModelTypeManagerImpl.MODESHAPE_GROUP;
    
    void addSequencerRepository( final String repositoryUrl );
    
    void installSequencers( final String archiveUrl ) throws ModelerException;
    
    Set< ModelType > modelTypes();
    
    void removeSequencerRepository( final String repositoryUrl );
    
    Set< String > sequencerArchives( final String groupUrl ) throws ModelerException;
    
    Set< String > sequencerGroups( final String repositoryUrl ) throws ModelerException;
    
    Set< String > sequencerRepositories();
}
