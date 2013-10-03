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

import java.net.URL;
import java.util.Set;

import org.modeshape.jcr.api.sequencer.Sequencer;
import org.modeshape.modeler.internal.ModelTypeManagerImpl;

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
     * @param artifactPath
     *        the repository path to an artifact
     * @return the default model type for the artifact at the supplied path
     * @throws ModelerException
     *         if any problem occurs
     */
    ModelType defaultModelType( final String artifactPath ) throws ModelerException;
    
    /**
     * @param repositoryUrl
     *        a {@link #sequencerRepositories() URL} to an on-line <a href="http://maven.apache.org">Maven</a> {@link Sequencer}
     *        repository
     * @param group
     *        the name of an available {@link #sequencerGroups(URL) group} of sequencers from an on-line <a
     *        href="http://maven.apache.org">Maven</a> {@link Sequencer} repository
     * @return the set of names of potential sequencer classes that could not be instantiated, usually due to missing dependencies.
     * @throws ModelerException
     *         if any problem occurs
     */
    Set< String > installSequencers( final URL repositoryUrl,
                                     final String group ) throws ModelerException;
    
    /**
     * @return the available model types
     */
    Set< ModelType > modelTypes();
    
    /**
     * @param artifactPath
     *        the repository path to an artifact
     * @return the model types applicable to the artifact at the supplied path
     * @throws ModelerException
     *         if any problem occurs
     */
    Set< ModelType > modelTypes( final String artifactPath ) throws ModelerException;
    
    /**
     * @param repositoryUrl
     *        a URL to an on-line <a href="http://maven.apache.org">Maven</a> {@link Sequencer} repository
     * @return the {@link #registerSequencerRepository(URL) registered} <a href="http://maven.apache.org">Maven</a>
     *         {@link Sequencer} repository URLs
     * @throws ModelerException
     *         if any error occurs
     */
    Set< URL > registerSequencerRepository( final URL repositoryUrl ) throws ModelerException;
    
    /**
     * @param repositoryUrl
     *        a {@link #sequencerRepositories() URL} to an on-line <a href="http://maven.apache.org">Maven</a> {@link Sequencer}
     *        repository
     * @return the available sequencer groups for the repository with the supplied URL
     * @throws ModelerException
     *         if any problem occurs
     */
    Set< String > sequencerGroups( final URL repositoryUrl ) throws ModelerException;
    
    /**
     * @return the {@link #registerSequencerRepository(URL) registered} <a href="http://maven.apache.org">Maven</a>
     *         {@link Sequencer} repository URLs
     */
    Set< URL > sequencerRepositories();
    
    /**
     * @param repositoryUrl
     *        a URL to a {@link #registerSequencerRepository(URL) registered} on-line <a href="http://maven.apache.org">Maven</a>
     *        {@link Sequencer} repository
     * @return the {@link #registerSequencerRepository(URL) registered} <a href="http://maven.apache.org">Maven</a>
     *         {@link Sequencer} repository URLs
     * @throws ModelerException
     *         if any error occurs
     */
    Set< URL > unregisterSequencerRepository( final URL repositoryUrl ) throws ModelerException;
}
