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
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.NodeType;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.modeshape.common.collection.Collections;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.JcrLexicon;
import org.modeshape.jcr.api.JcrTools;
import org.modeshape.jcr.api.sequencer.Sequencer;
import org.modeshape.modeler.ModelType;
import org.modeshape.modeler.ModelTypeManager;
import org.modeshape.modeler.ModelerException;
import org.modeshape.modeler.ModelerI18n;
import org.polyglotter.common.Logger;

/**
 * 
 */
public final class ModelTypeManagerImpl implements ModelTypeManager {
    
    private static final String SEQUENCER_REPOSITORIES = "sequencerRepositories";
    static final String ZIPS = "zips";
    static final String JARS = "jars";
    private static final String MODEL_TYPES = "modelTypes";
    private static final String SEQUENCER_CLASS = "sequencerClass";
    private static final String POTENTIAL_SEQUENCER_CLASS_NAMES = "potentialSequencerClassNames";
    
    static final Logger LOGGER = Logger.getLogger( ModelTypeManagerImpl.class );
    
    /**
     * 
     */
    public static final String MODESHAPE_GROUP = "/org/modeshape/";
    
    static final URL[] EMPTY_URLS = new URL[ 0 ];
    
    final Manager manager;
    final Set< URL > sequencerRepositories = new HashSet<>();
    final Set< ModelType > modelTypes = new HashSet<>();
    final LibraryClassLoader libraryClassLoader = new LibraryClassLoader();
    final Set< String > potentialSequencerClassNames = new HashSet<>();
    final Path library;
    final Map< String, DependencyProcessor > dependencyProcessorsByModelTypeName = new HashMap< String, DependencyProcessor >();
    
    ModelTypeManagerImpl( final Manager manager ) throws ModelerException {
        this.manager = manager;
        try {
            library = Files.createTempDirectory( null );
        } catch ( final IOException e ) {
            throw new ModelerException( e );
        }
        library.toFile().deleteOnExit();
        manager.run( this, new SystemTask< Void >() {
            
            @Override
            public Void run( final Session session,
                             final Node systemNode ) throws Exception {
                // Load sequencer repositories
                if ( !systemNode.hasProperty( SEQUENCER_REPOSITORIES ) ) {
                    final Value[] vals = new Value[ 2 ];
                    vals[ 0 ] = session.getValueFactory().createValue( JBOSS_SEQUENCER_REPOSITORY );
                    vals[ 1 ] = session.getValueFactory().createValue( MAVEN_SEQUENCER_REPOSITORY );
                    systemNode.setProperty( SEQUENCER_REPOSITORIES, vals );
                    session.save();
                }
                for ( final Value val : systemNode.getProperty( SEQUENCER_REPOSITORIES ).getValues() )
                    sequencerRepositories.add( new URL( val.getString() ) );
                // Load jars
                if ( !systemNode.hasNode( JARS ) ) {
                    systemNode.addNode( JARS );
                    session.save();
                }
                for ( final NodeIterator iter = systemNode.getNode( JARS ).getNodes(); iter.hasNext(); ) {
                    final Node node = iter.nextNode();
                    final Path jarPath = library.resolve( node.getName() );
                    try ( InputStream stream =
                        node.getNode( JcrLexicon.CONTENT.getString() ).getProperty( JcrLexicon.DATA.getString() ).getBinary()
                            .getStream() ) {
                        Files.copy( stream, jarPath );
                    }
                    jarPath.toFile().deleteOnExit();
                    libraryClassLoader.addURL( jarPath.toUri().toURL() );
                }
                // Load model types
                if ( !systemNode.hasNode( MODEL_TYPES ) ) {
                    systemNode.addNode( MODEL_TYPES );
                    session.save();
                }
                for ( final NodeIterator iter = systemNode.getNode( MODEL_TYPES ).getNodes(); iter.hasNext(); ) {
                    final Node node = iter.nextNode();
                    modelTypes.add( new ModelTypeImpl( manager,
                                                       node.getName(),
                                                       libraryClassLoader.loadClass( node.getProperty( SEQUENCER_CLASS ).getString() ) ) );
                }
                // Load potential sequencer class names
                if ( !systemNode.hasProperty( POTENTIAL_SEQUENCER_CLASS_NAMES ) ) {
                    systemNode.setProperty( POTENTIAL_SEQUENCER_CLASS_NAMES, new Value[ 0 ] );
                    session.save();
                }
                for ( final Value val : systemNode.getProperty( POTENTIAL_SEQUENCER_CLASS_NAMES ).getValues() )
                    potentialSequencerClassNames.add( val.getString() );
                return null;
            }
        } );
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
            
            return dependencyProcessorsByModelTypeName.get( modelNode.getPath() );
        } catch ( final Exception e ) {
            throw new ModelerException( e );
        }
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.modeler.ModelTypeManager#installSequencers(java.net.URL, java.lang.String)
     */
    @Override
    public Set< String > installSequencers( final URL repositoryUrl,
                                            final String group ) throws ModelerException {
        CheckArg.isNotNull( repositoryUrl, "repositoryUrl" );
        CheckArg.isNotEmpty( group, "group" );
        if ( LOGGER.isDebugEnabled() ) LOGGER.debug( "Installing sequencers from group " + group );
        try {
            final String version = manager.repository.getDescriptor( Repository.REP_VERSION_DESC );
            final String archiveName = "modeshape-sequencer-" + group + "-" + version + "-module-with-dependencies.zip";
            // Return if archive has already been installed
            if ( manager.run( this, new SystemTask< Boolean >() {
                
                @Override
                public Boolean run( final Session session,
                                    final Node systemNode ) throws Exception {
                    if ( systemNode.hasProperty( ZIPS ) )
                        for ( final Value val : systemNode.getProperty( ZIPS ).getValues() )
                            if ( val.getString().equals( archiveName ) ) {
                                if ( LOGGER.isDebugEnabled() ) LOGGER.debug( "Archive already installed: " + archiveName );
                                return true;
                            }
                    return false;
                }
            } ) ) return Collections.unmodifiableSet( potentialSequencerClassNames );
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
                        if ( LOGGER.isDebugEnabled() ) LOGGER.debug( "Jar already installed: " + jarPath );
                        continue;
                    }
                    manager.run( this, new SystemTask< Void >() {
                        
                        @Override
                        public Void run( final Session session,
                                         final Node systemNode ) throws Exception {
                            try ( InputStream stream = archive.getInputStream( archiveEntry ) ) {
                                new JcrTools().uploadFile( session,
                                                           systemNode.getPath() + '/' + JARS + '/' + jarPath.getFileName().toString(),
                                                           stream );
                                session.save();
                            }
                            return null;
                        }
                    } );
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
                             && !Modifier.isAbstract( sequencerClass.getModifiers() ) ) {
                            String name = sequencerClass.getName();
                            name = name.endsWith( "Sequencer" ) ? name.substring( 0, name.length() - "Sequencer".length() ) : name;
                            final ModelTypeImpl type = new ModelTypeImpl( manager, name, sequencerClass );
                            modelTypes.add( type );
                            manager.run( this, new SystemTask< Void >() {
                                
                                @Override
                                public Void run( final Session session,
                                                 final Node systemNode ) throws Exception {
                                    final Node node = systemNode.getNode( MODEL_TYPES ).addNode( type.name() );
                                    node.setProperty( SEQUENCER_CLASS, sequencerClass.getName() );
                                    session.save();
                                    return null;
                                }
                            } );
                        }
                        iter.remove();
                    } catch ( final NoClassDefFoundError | ClassNotFoundException ignored ) {
                        // Class will be re-tested as a Sequencer when the next archive is installed
                    }
            }
            archivePath.toFile().delete();
            manager.run( this, new SystemTask< Void >() {
                
                @Override
                public Void run( final Session session,
                                 final Node systemNode ) throws Exception {
                    // Save that archive has been installed
                    Value[] vals = systemNode.hasProperty( ZIPS ) ? systemNode.getProperty( ZIPS ).getValues() : new Value[ 0 ];
                    final Value[] newVals = new Value[ vals.length + 1 ];
                    System.arraycopy( vals, 0, newVals, 0, vals.length );
                    newVals[ vals.length ] = session.getValueFactory().createValue( archiveName );
                    systemNode.setProperty( ZIPS, newVals );
                    // Save potential class names
                    vals = new Value[ potentialSequencerClassNames.size() ];
                    int ndx = 0;
                    for ( final String name : potentialSequencerClassNames )
                        vals[ ndx++ ] = session.getValueFactory().createValue( name );
                    systemNode.setProperty( POTENTIAL_SEQUENCER_CLASS_NAMES, vals );
                    session.save();
                    return null;
                }
            } );
            return Collections.unmodifiableSet( potentialSequencerClassNames );
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
     * @see org.modeshape.modeler.ModelTypeManager#registerSequencerRepository(java.net.URL)
     */
    @Override
    public Set< URL > registerSequencerRepository( final URL repositoryUrl ) throws ModelerException {
        CheckArg.isNotNull( repositoryUrl, "repositoryUrl" );
        if ( sequencerRepositories.add( repositoryUrl ) )
            manager.run( this, new SystemTask< Void >() {
                
                @Override
                public Void run( final Session session,
                                 final Node systemNode ) throws Exception {
                    final Value[] vals = systemNode.getProperty( SEQUENCER_REPOSITORIES ).getValues();
                    final Value[] newVals = new Value[ vals.length + 1 ];
                    System.arraycopy( vals, 0, newVals, 0, vals.length );
                    newVals[ vals.length ] = session.getValueFactory().createValue( repositoryUrl.toString() );
                    systemNode.setProperty( SEQUENCER_REPOSITORIES, newVals );
                    session.save();
                    return null;
                }
            } );
        return sequencerRepositories;
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.modeler.ModelTypeManager#sequencerGroups(java.net.URL)
     */
    @Override
    public Set< String > sequencerGroups( final URL repositoryUrl ) throws ModelerException {
        CheckArg.isNotNull( repositoryUrl, "repositoryUrl" );
        final Set< String > groups = new HashSet<>();
        try {
            final Document doc = Jsoup.connect( repositoryUrl.toString() ).get();
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
    public Set< URL > sequencerRepositories() {
        return Collections.unmodifiableSet( sequencerRepositories );
    }
    
    /**
     * {@inheritDoc}
     * 
     * @throws ModelerException
     *         if any error occurs
     * 
     * @see org.modeshape.modeler.ModelTypeManager#unregisterSequencerRepository(java.net.URL)
     */
    @Override
    public Set< URL > unregisterSequencerRepository( final URL repositoryUrl ) throws ModelerException {
        CheckArg.isNotNull( repositoryUrl, "repositoryUrl" );
        if ( sequencerRepositories.remove( repositoryUrl ) )
            manager.run( this, new SystemTask< Void >() {
                
                @Override
                public Void run( final Session session,
                                 final Node systemNode ) throws Exception {
                    final Value[] vals = systemNode.getProperty( SEQUENCER_REPOSITORIES ).getValues();
                    final Value[] newVals = new Value[ vals.length - 1 ];
                    final String url = repositoryUrl.toString();
                    for ( int ndx = 0, newNdx = 0; ndx < vals.length; ++ndx )
                        if ( !vals[ ndx ].getString().equals( url ) ) newVals[ newNdx++ ] = vals[ ndx ];
                    systemNode.setProperty( SEQUENCER_REPOSITORIES, newVals );
                    session.save();
                    return null;
                }
            } );
        return sequencerRepositories;
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
