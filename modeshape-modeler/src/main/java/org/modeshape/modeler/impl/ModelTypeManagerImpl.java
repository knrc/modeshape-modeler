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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.NodeType;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.modeshape.common.collection.Collections;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.JcrLexicon;
import org.modeshape.jcr.api.sequencer.Sequencer;
import org.modeshape.modeler.ModelType;
import org.modeshape.modeler.ModelTypeManager;
import org.modeshape.modeler.Modeler;
import org.modeshape.modeler.ModelerException;
import org.modeshape.modeler.ModelerI18n;
import org.polyglotter.common.Logger;

/**
 * 
 */
public final class ModelTypeManagerImpl implements ModelTypeManager {
    
    static final Logger LOGGER = Logger.getLogger( ModelTypeManagerImpl.class );
    
    /**
     * 
     */
    public static final String MODESHAPE_GROUP = "/org/modeshape/";
    
    static final URL[] EMPTY_URLS = new URL[ 0 ];
    
    final Manager manager;
    private final Set< String > sequencerRepositories = new HashSet<>( Arrays.asList( JBOSS_SEQUENCER_REPOSITORY,
                                                                                      MAVEN_SEQUENCER_REPOSITORY ) );
    final Set< ModelType > modelTypes = new HashSet<>();
    final LibraryClassLoader libraryClassLoader = new LibraryClassLoader();
    final Set< String > potentialSequencerClassNames = new HashSet<>();
    Path library;
    
    /**
     * Key is the model type name.
     */
    final Map< String, DependencyProcessor > dependencyProcessors = new HashMap< String, DependencyProcessor >();
    
    /**
     * @param manager
     *        The {@link Modeler Modeler's} manager
     */
    public ModelTypeManagerImpl( final Manager manager ) {
        this.manager = manager;
    }
    
    private void checkHttpUrl( final String url ) {
        if ( !url.startsWith( "http" ) ) throw new IllegalArgumentException( ModelerI18n.mustBeHttpUrl.text( url ) );
    }
    
    Binary content( final Node fileNode ) throws ValueFormatException, PathNotFoundException, RepositoryException {
        return fileNode.getNode( JcrLexicon.CONTENT.getString() ).getProperty( JcrLexicon.DATA.getString() ).getBinary();
    }
    
    byte[] content( final ZipInputStream zip ) throws IOException {
        try ( final ByteArrayOutputStream stream = new ByteArrayOutputStream() ) {
            final byte[] buf = new byte[ 1024 ];
            for ( int bytesRead; ( bytesRead = zip.read( buf, 0, buf.length ) ) > 0; )
                stream.write( buf, 0, bytesRead );
            return stream.toByteArray();
        }
    }
    
    /**
     * @param fileNode
     *        the file node
     * @param sequencers
     *        the sequencers applicable to the supplied file node
     * @return the default model type for the supplied file node
     * @throws Exception
     *         if any problem occurs
     */
    public ModelType defaultModelType( final Node fileNode,
                                       final Set< ModelType > sequencers ) throws Exception {
        final String ext = fileNode.getName().substring( fileNode.getName().lastIndexOf( '.' ) + 1 );
        for ( final ModelType type : sequencers )
            if ( type.sourceFileExtensions().contains( ext ) ) return type;
        return sequencers.isEmpty() ? null : sequencers.iterator().next();
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.modeler.ModelTypeManager#defaultModelType(java.lang.String)
     */
    @Override
    public ModelType defaultModelType( final String filePath ) throws ModelerException {
        CheckArg.isNotEmpty( filePath, "filePath" );
        return manager.run( new Task< ModelType >() {
            
            @Override
            public ModelType run( final Session session ) throws Exception {
                final Node node = manager.fileNode( session, filePath );
                final ModelType type = defaultModelType( node, modelTypes( node ) );
                return type == null ? null : type;
            }
        } );
    }
    
    /**
     * @param modelNode
     *        the model node whose dependency processor is being requested (cannot be <code>null</code>)
     * @return the dependency processor or <code>null</code> if not found
     * @throws ModelerException
     *         if specified node is not a model node or if an error occurs
     */
    public DependencyProcessor dependencyProcessor( final Node modelNode ) throws ModelerException {
        CheckArg.isNotNull( modelNode, "modelNode" );
        
        try {
            boolean foundMixin = false;
            
            for ( final NodeType mixin : modelNode.getMixinNodeTypes() ) {
                if ( Manager.MODEL_NODE_MIXIN.equals( mixin.getName() ) ) {
                    foundMixin = true;
                    break;
                }
            }
            
            if ( !foundMixin ) {
                throw new ModelerException( ModelerI18n.mustBeModelNode.text( modelNode.getName() ) );
            }
            
            return dependencyProcessors.get( modelNode.getPath() );
        } catch ( final Exception e ) {
            throw new ModelerException( e );
        }
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.modeler.ModelTypeManager#installSequencers(java.lang.String, java.lang.String)
     */
    @Override
    public Set< String > installSequencers( final String repositoryUrl,
                                            final String group ) throws ModelerException {
        CheckArg.isNotEmpty( group, "group" );
        if ( LOGGER.isDebugEnabled() ) LOGGER.debug( "Installing sequencers from group " + group );
        try {
            if ( library == null ) {
                library = Files.createTempDirectory( null );
                library.toFile().deleteOnExit();
            }
            final String version = manager.repository().getDescriptor( Repository.REP_VERSION_DESC );
            final String archiveName = "modeshape-sequencer-" + group + "-" + version + "-module-with-dependencies.zip";
            final Path archivePath = library.resolve( archiveName );
            try ( InputStream stream =
                new URL( repositoryUrl + "modeshape-sequencer-" + group + '/' + version + '/' + archiveName ).openStream() ) {
                Files.copy( stream, archivePath );
            }
            try ( final ZipFile archive = new ZipFile( archivePath.toFile() ) ) {
                for ( final Enumeration< ? extends ZipEntry > archiveIter = archive.entries(); archiveIter.hasMoreElements(); ) {
                    final ZipEntry archiveEntry = archiveIter.nextElement();
                    if ( archiveEntry.isDirectory() ) continue;
                    String name = archiveEntry.getName().toLowerCase();
                    if ( name.contains( "test" ) || name.contains( "source" ) || !name.endsWith( ".jar" ) ) continue;
                    final Path jarPath =
                        library.resolve( archiveEntry.getName().substring( archiveEntry.getName().lastIndexOf( '/' ) + 1 ) );
                    if ( jarPath.toFile().exists() ) {
                        if ( LOGGER.isDebugEnabled() ) LOGGER.debug( "Jar already exists: " + jarPath );
                        continue;
                    }
                    try ( InputStream stream = archive.getInputStream( archiveEntry ) ) {
                        Files.copy( stream, jarPath );
                    }
                    jarPath.toFile().deleteOnExit();
                    libraryClassLoader.addURL( jarPath.toUri().toURL() );
                    if ( LOGGER.isDebugEnabled() ) LOGGER.debug( "Installed jar: " + jarPath );
                    try ( final ZipFile jar = new ZipFile( jarPath.toFile() ) ) {
                        for ( final Enumeration< ? extends ZipEntry > jarIter = jar.entries(); jarIter.hasMoreElements(); ) {
                            final ZipEntry jarEntry = jarIter.nextElement();
                            if ( jarEntry.isDirectory() ) continue;
                            name = jarEntry.getName();
                            if ( jarPath.getFileName().toString().contains( "sequencer" ) && name.endsWith( "Sequencer.class" ) ) {
                                potentialSequencerClassNames.add( name.replace( '/', '.' )
                                                                      .substring( 0, name.length() - ".class".length() ) );
                                if ( LOGGER.isDebugEnabled() ) LOGGER.debug( "Potential sequencer: " + name );
                            }
                        }
                    }
                }
                for ( final Iterator< String > iter = potentialSequencerClassNames.iterator(); iter.hasNext(); )
                    try {
                        final Class< ? > sequencerClass = libraryClassLoader.loadClass( iter.next() );
                        if ( Sequencer.class.isAssignableFrom( sequencerClass )
                             && !Modifier.isAbstract( sequencerClass.getModifiers() ) )
                            modelTypes.add( new ModelTypeImpl( manager, sequencerClass ) );
                        iter.remove();
                    } catch ( final NoClassDefFoundError | ClassNotFoundException ignored ) {
                        // Class will be re-tested as a Sequencer when the next archive is installed
                    }
            }
            archivePath.toFile().delete();
            return potentialSequencerClassNames;
        } catch ( final IOException e ) {
            throw new ModelerException( e );
        }
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.modeler.ModelTypeManager#modelTypes()
     */
    @Override
    public Set< ModelType > modelTypes() {
        return Collections.unmodifiableSet( modelTypes );
    }
    
    /**
     * @param fileNode
     *        the file node
     * @return the model types applicable to the supplied file node
     * @throws Exception
     *         if any problem occurs
     */
    public Set< ModelType > modelTypes( final Node fileNode ) throws Exception {
        final Set< ModelType > applicableSequencers = new HashSet<>();
        for ( final ModelType type : modelTypes() )
            if ( ( ( ModelTypeImpl ) type ).sequencer()
                                           .isAccepted( fileNode.getNode( JcrLexicon.CONTENT.getString() )
                                                                .getProperty( JcrLexicon.MIMETYPE.getString() ).getString() ) )
                applicableSequencers.add( type );
        return applicableSequencers;
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.modeler.ModelTypeManager#modelTypes(java.lang.String)
     */
    @Override
    public Set< ModelType > modelTypes( final String filePath ) throws ModelerException {
        CheckArg.isNotEmpty( filePath, "filePath" );
        return manager.run( new Task< Set< ModelType > >() {
            
            @Override
            public final Set< ModelType > run( final Session session ) throws Exception {
                return modelTypes( manager.fileNode( session, filePath ) );
            }
        } );
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.modeler.ModelTypeManager#registerSequencerRepository(java.lang.String)
     */
    @Override
    public void registerSequencerRepository( final String repositoryUrl ) {
        sequencerRepositories.add( repositoryUrl );
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.modeler.ModelTypeManager#sequencerGroups(java.lang.String)
     */
    @Override
    public Set< String > sequencerGroups( final String repositoryUrl ) throws ModelerException {
        checkHttpUrl( repositoryUrl );
        final Set< String > groups = new HashSet<>();
        try {
            final Document doc = Jsoup.connect( repositoryUrl ).get();
            final Elements elements = doc.getElementsMatchingOwnText( "sequencer-" );
            for ( final Element element : elements ) {
                final String href = element.attr( "href" );
                groups.add( href.substring( href.indexOf( "sequencer-" ) + "sequencer-".length(), href.lastIndexOf( '/' ) ) );
            }
        } catch ( final IOException e ) {
            throw new ModelerException( e );
        }
        return groups;
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.modeler.ModelTypeManager#sequencerRepositories()
     */
    @Override
    public Set< String > sequencerRepositories() {
        return Collections.unmodifiableSet( sequencerRepositories );
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.modeler.ModelTypeManager#unregisterSequencerRepository(java.lang.String)
     */
    @Override
    public void unregisterSequencerRepository( final String repositoryUrl ) {
        sequencerRepositories.remove( repositoryUrl );
    }
    
    class LibraryClassLoader extends URLClassLoader {
        
        LibraryClassLoader() {
            super( EMPTY_URLS, LibraryClassLoader.class.getClassLoader() );
        }
        
        @Override
        protected void addURL( final URL url ) {
            super.addURL( url );
        }
    }
}
