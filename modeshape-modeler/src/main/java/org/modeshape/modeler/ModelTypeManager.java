/*
 * Polyglotter (http://polyglotter.org)
 * See the COPYRIGHT.txt content distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt content in the distribution for a full listing of 
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

import org.modeshape.jcr.api.sequencer.Sequencer;
import org.modeshape.modeler.impl.ModelTypeManagerImpl;

/**
 * 
 */
public interface ModelTypeManager {
    
    /**
     * 
     */
    String JBOSS_SEQUENCER_REPOSITORY = "https://repository.jboss.org/nexus/content/groups/public-jboss"
                                        + ModelTypeManagerImpl.MODESHAPE_GROUP;
    
    /**
     * 
     */
    String MAVEN_SEQUENCER_REPOSITORY = "http://repo1.maven.org/maven2" + ModelTypeManagerImpl.MODESHAPE_GROUP;
    
    /**
     * @param contentPath
     *        the repository path to an artifact's content
     * @return the default model type for the content at the supplied path
     * @throws ModelerException
     *         if any problem occurs
     */
    ModelType defaultModelType( final String contentPath ) throws ModelerException;
    
    /**
     * @param repositoryUrl
     *        a {@link #sequencerRepositories() URL} to an on-line <a href="http://maven.apache.org">Maven</a> {@link Sequencer}
     *        repository
     * @param group
     *        the name of an available {@link #sequencerGroups(String) group} of sequencers from an on-line <a
     *        href="http://maven.apache.org">Maven</a> {@link Sequencer} repository
     * @return the set of names of potential sequencer classes that could not be instantiated, usually due to missing dependencies.
     * @throws ModelerException
     *         if any problem occurs
     */
    Set< String > installSequencers( final String repositoryUrl,
                                     final String group ) throws ModelerException;
    
    /**
     * @return the available model types
     */
    Set< ModelType > modelTypes();
    
    /**
     * @param contentPath
     *        the repository path to an artifact's content
     * @return the model types applicable to the content at the supplied path
     * @throws ModelerException
     *         if any problem occurs
     */
    Set< ModelType > modelTypes( final String contentPath ) throws ModelerException;
    
    /**
     * @param repositoryUrl
     *        a URL to an on-line <a href="http://maven.apache.org">Maven</a> {@link Sequencer} repository
     */
    void registerSequencerRepository( final String repositoryUrl );
    
    /**
     * @param repositoryUrl
     *        a {@link #sequencerRepositories() URL} to an on-line <a href="http://maven.apache.org">Maven</a> {@link Sequencer}
     *        repository
     * @return the available sequencer groups for the repository with the supplied URL
     * @throws ModelerException
     *         if any problem occurs
     */
    Set< String > sequencerGroups( final String repositoryUrl ) throws ModelerException;
    
    /**
     * @return the {@link #registerSequencerRepository(String) registered} <a href="http://maven.apache.org">Maven</a>
     *         {@link Sequencer} repository URLs
     */
    Set< String > sequencerRepositories();
    
    /**
     * @param repositoryUrl
     *        a URL to a {@link #registerSequencerRepository(String) registered} on-line <a href="http://maven.apache.org">Maven</a>
     *        {@link Sequencer} repository
     */
    void unregisterSequencerRepository( final String repositoryUrl );
}
