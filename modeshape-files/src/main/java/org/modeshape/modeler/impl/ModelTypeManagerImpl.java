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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.modeshape.common.collection.Collections;
import org.modeshape.files.FileManagerException;
import org.modeshape.files.FileManagerI18n;
import org.modeshape.jcr.JcrLexicon;
import org.modeshape.jcr.JcrNtLexicon;
import org.modeshape.jcr.api.JcrTools;
import org.modeshape.jcr.api.ValueFactory;
import org.modeshape.jcr.api.sequencer.Sequencer;
import org.modeshape.modeler.ModelType;
import org.modeshape.modeler.ModelTypeManager;

/**
 * 
 */
public final class ModelTypeManagerImpl implements ModelTypeManager {
    
    public static final String MODESHAPE_GROUP = "/org/modeshape/";
    
    private static final String LIBRARY_PATH = '/' + Manager.NS + "library/";
    
    final Manager mgr;
    private final Set< String > sequencerRepositories = new HashSet<>( Arrays.asList( JBOSS_SEQUENCER_REPOSITORY,
                                                                                      MAVEN_SEQUENCER_REPOSITORY ) );
    final Set< ModelType > modelTypes = new HashSet<>();
    final LibraryClassLoader classLoader = new LibraryClassLoader();
    final Set< String > potentialSequencerClassNames = new HashSet<>();
    
    public ModelTypeManagerImpl( final Manager manager ) {
        mgr = manager;
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.modeler.ModelTypeManager#addSequencerRepository(java.lang.String)
     */
    @Override
    public void addSequencerRepository( final String repositoryUrl ) {
        sequencerRepositories.add( repositoryUrl );
    }
    
    private void checkHttpUrl( final String url ) {
        if ( !url.startsWith( "http" ) ) throw new IllegalArgumentException( FileManagerI18n.mustBeHttpUrl.text( url ) );
    }
    
    Binary content( final Node fileNode ) throws ValueFormatException, PathNotFoundException, RepositoryException {
        return fileNode.getNode( JcrLexicon.CONTENT.getString() ).getProperty( JcrLexicon.DATA.getString() ).getBinary();
    }
    
    byte[] content( final ZipInputStream zip ) throws IOException {
        try ( final ByteArrayOutputStream stream = new ByteArrayOutputStream() ) {
            final byte[] buf = new byte[ 1024 ];
            for ( int bytesRead; ( bytesRead = zip.read( buf, 0, buf.length ) ) > -1; )
                stream.write( buf, 0, bytesRead );
            return stream.toByteArray();
        }
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.modeler.ModelTypeManager#installSequencers(java.lang.String)
     */
    @Override
    public void installSequencers( final String archiveUrl ) throws FileManagerException {
        checkHttpUrl( archiveUrl );
        mgr.run( new Task< Void >() {
            
            private void installSequencers( final Node jarNode ) throws Exception {
                if ( jarNode.getName().contains( "sequencer" ) )
                    try ( final ZipInputStream jar = new ZipInputStream( content( jarNode ).getStream() ) ) {
                        for ( ZipEntry entry = jar.getNextEntry(); entry != null; entry = jar.getNextEntry() ) {
                            final String name = entry.getName().replace( '/', '.' );
                            if ( entry.isDirectory() ) continue;
                            classLoader.jarsByClass.put( entry.getName(), jarNode.getName() );
                            if ( entry.getName().endsWith( ".class" ) )
                                potentialSequencerClassNames.add( name.substring( 0, name.length() - 6 ) );
                        }
                    }
            }
            
            @Override
            public Void run( final Session session ) throws Exception {
                final String path = LIBRARY_PATH + archiveUrl.substring( archiveUrl.lastIndexOf( '/' ) + 1 );
                if ( session.nodeExists( path ) ) return null;
                final JcrTools tools = new JcrTools();
                final Node fileNode = tools.uploadFile( session, path, new URL( archiveUrl ) );
                // Determine if we're dealing with a simple jar or an archive of jars
                boolean archiveOfJars = false;
                final Binary content = content( fileNode );
                try ( final ZipInputStream zip = new ZipInputStream( content.getStream() ) ) {
                    for ( ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry() ) {
                        if ( entry.isDirectory() ) continue;
                        if ( entry.getName().endsWith( ".class" ) ) break;
                        if ( entry.getName().endsWith( ".jar" ) ) {
                            archiveOfJars = true;
                            break;
                        }
                    }
                }
                if ( archiveOfJars ) {
                    try ( final ZipInputStream zip = new ZipInputStream( content.getStream() ) ) {
                        for ( ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry() ) {
                            if ( entry.isDirectory() ) continue;
                            String name = entry.getName().toLowerCase();
                            if ( name.contains( "test" ) || name.contains( "source" ) || !name.endsWith( ".jar" ) ) continue;
                            name = entry.getName().substring( entry.getName().lastIndexOf( '/' ) + 1 );
                            final Node jarNode = tools.findOrCreateNode( session.getRootNode(),
                                                                         LIBRARY_PATH + name,
                                                                         JcrNtLexicon.FOLDER.getString(),
                                                                         JcrNtLexicon.FILE.getString() );
                            final Node contentNode = tools.findOrCreateChild( jarNode,
                                                                              JcrLexicon.CONTENT.getString(),
                                                                              JcrNtLexicon.RESOURCE.getString() );
                            contentNode.setProperty( JcrLexicon.DATA.getString(),
                                                     ( ( ValueFactory ) session.getValueFactory() ).createBinary( content( zip ) ) );
                            installSequencers( jarNode );
                        }
                    }
                    fileNode.remove();
                } else installSequencers( fileNode );
                session.save();
                for ( final Iterator< String > iter = potentialSequencerClassNames.iterator(); iter.hasNext(); )
                    try {
                        final Class< ? > sequencerClass = classLoader.loadClass( iter.next() );
                        if ( Sequencer.class.isAssignableFrom( sequencerClass )
                             && !Modifier.isAbstract( sequencerClass.getModifiers() ) )
                            modelTypes.add( new ModelTypeImpl( mgr, sequencerClass ) );
                        iter.remove();
                    } catch ( final NoClassDefFoundError ignored ) {
                        // Class will be re-tested as a Sequencer when the next archive is installed
                    }
                return null;
            }
        } );
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
     * {@inheritDoc}
     * 
     * @see org.modeshape.modeler.ModelTypeManager#removeSequencerRepository(java.lang.String)
     */
    @Override
    public void removeSequencerRepository( final String repositoryUrl ) {
        sequencerRepositories.remove( repositoryUrl );
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.modeler.ModelTypeManager#sequencerArchives(java.lang.String)
     */
    @Override
    public Set< String > sequencerArchives( final String groupUrl ) throws FileManagerException {
        checkHttpUrl( groupUrl );
        final String versionUrl = mgr.run( new Task< String >() {
            
            @Override
            public String run( final Session session ) {
                final String url = groupUrl.endsWith( "/" ) ? groupUrl.substring( 0, groupUrl.length() - 1 ) : groupUrl;
                final String version = session.getRepository().getDescriptor( Repository.REP_VERSION_DESC );
                return url + '/' + ( url.endsWith( version ) ? "" : version + '/' );
            }
        } );
        final Set< String > urls = new HashSet<>();
        try {
            final Document doc = Jsoup.connect( versionUrl ).get();
            final Elements elements = doc.getElementsMatchingOwnText( "\\.(zip|jar)$" );
            for ( final Element element : elements )
                if ( !element.ownText().contains( "test" ) && !element.ownText().contains( "source" ) )
                    urls.add( element.absUrl( "href" ) );
        } catch ( final IOException e ) {
            throw new FileManagerException( e );
        }
        return urls;
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.modeler.ModelTypeManager#sequencerGroups(java.lang.String)
     */
    @Override
    public Set< String > sequencerGroups( final String repositoryUrl ) throws FileManagerException {
        checkHttpUrl( repositoryUrl );
        final Set< String > urls = new HashSet<>();
        try {
            final Document doc = Jsoup.connect( repositoryUrl ).get();
            final Elements elements = doc.getElementsMatchingOwnText( "sequencer" );
            for ( final Element element : elements )
                urls.add( element.absUrl( "href" ) );
        } catch ( final IOException e ) {
            throw new FileManagerException( e );
        }
        return urls;
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
    
    class LibraryClassLoader extends ClassLoader {
        
        final ConcurrentHashMap< String, String > jarsByClass = new ConcurrentHashMap<>();
        final URLStreamHandler urlStreamHandler = new URLStreamHandler() {
            
            @Override
            protected URLConnection openConnection( final URL url ) {
                return new URLConnection( url ) {
                    
                    InputStream zip;
                    ZipEntry zipEntry;
                    
                    @Override
                    public void connect() throws IOException {
                        if ( connected ) return;
                        try {
                            zip = mgr.run( new Task< InputStream >() {
                                
                                @Override
                                public InputStream run( final Session session ) throws Exception {
                                    final String path = getURL().getPath();
                                    final ZipInputStream zip =
                                        new ZipInputStream( content( session.getNode( LIBRARY_PATH + jarsByClass.get( path ) ) ).getStream() );
                                    for ( ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry() ) {
                                        if ( entry.getName().equals( path ) ) {
                                            zipEntry = entry;
                                            return zip;
                                        }
                                    }
                                    zip.close();
                                    return null;
                                }
                            } );
                            connected = true;
                        } catch ( final FileManagerException e ) {
                            if ( e.getCause() instanceof IOException ) throw ( IOException ) e.getCause();
                            throw new IOException( url.getPath(), e );
                        }
                    }
                    
                    @Override
                    public int getContentLength() {
                        return ( int ) zipEntry.getSize();
                    }
                    
                    @Override
                    public String getContentType() {
                        String type = guessContentTypeFromName( zipEntry.getName() );
                        if ( type == null )
                            try {
                                if ( !connected ) connect();
                                if ( zip.markSupported() ) type = guessContentTypeFromStream( zip );
                            } catch ( final IOException ignored ) {}
                        return type;
                    }
                    
                    @Override
                    public InputStream getInputStream() throws IOException {
                        if ( !connected ) connect();
                        return zip;
                    }
                    
                    @Override
                    public long getLastModified() {
                        return zipEntry.getTime();
                    }
                };
            }
        };
        
        LibraryClassLoader() {
            super( ModelTypeManagerImpl.this.getClass().getClassLoader() );
        }
        
        @Override
        protected Class< ? > findClass( final String name ) throws ClassNotFoundException {
            try {
                return mgr.run( new Task< Class< ? > >() {
                    
                    @SuppressWarnings( { "synthetic-access", "resource" } )
                    @Override
                    public Class< ? > run( final Session session ) throws Exception {
                        final String path = name.replace( '.', '/' ) + ".class";
                        try ( final ZipInputStream zip =
                            new ZipInputStream( content( session.getNode( LIBRARY_PATH + jarsByClass.get( path ) ) ).getStream() ) ) {
                            for ( ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry() ) {
                                if ( !entry.getName().equals( path ) ) continue;
                                final byte[] contents = content( zip );
                                return defineClass( name, contents, 0, contents.length );
                            }
                        }
                        throw new ClassNotFoundException( name );
                    }
                } );
            } catch ( final FileManagerException e ) {
                if ( e.getCause() instanceof ClassNotFoundException ) throw ( ClassNotFoundException ) e.getCause();
                throw new ClassNotFoundException( name, e );
            }
        }
        
        @Override
        protected URL findResource( final String name ) {
            try {
                return new URL( null, "resource:" + name, urlStreamHandler );
            } catch ( final MalformedURLException e ) {
                Logger.getLogger( getClass() ).error( e );
            }
            return null;
        }
    }
}
