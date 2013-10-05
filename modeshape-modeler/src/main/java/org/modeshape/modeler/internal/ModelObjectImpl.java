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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.JcrLexicon;
import org.modeshape.modeler.ModelObject;
import org.modeshape.modeler.ModelerException;

/**
 * 
 */
public class ModelObjectImpl implements ModelObject {
    
    final Manager manager;
    final String path;
    
    ModelObjectImpl( final Manager manager,
                     final String path ) {
        this.manager = manager;
        this.path = path;
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.modeler.ModelObject#booleanValue(java.lang.String)
     */
    @Override
    public Boolean booleanValue( final String propertyName ) throws ModelerException {
        CheckArg.isNotEmpty( propertyName, "propertyName" );
        return manager.run( new Task< Boolean >() {
            
            @Override
            public Boolean run( final Session session ) throws Exception {
                try {
                    return session.getNode( path ).getProperty( propertyName ).getBoolean();
                } catch ( final ValueFormatException e ) {
                    throw new IllegalArgumentException( e );
                } catch ( final PathNotFoundException e ) {
                    return null;
                }
            }
        } );
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.modeler.ModelObject#booleanValues(java.lang.String)
     */
    @Override
    public Boolean[] booleanValues( final String propertyName ) throws ModelerException {
        CheckArg.isNotEmpty( propertyName, "propertyName" );
        return manager.run( new Task< Boolean[] >() {
            
            @Override
            public Boolean[] run( final Session session ) throws Exception {
                try {
                    final Property prop = session.getNode( path ).getProperty( propertyName );
                    if ( !prop.isMultiple() ) return new Boolean[] { prop.getBoolean() };
                    final Value[] vals = prop.getValues();
                    final Boolean[] booleanVals = new Boolean[ vals.length ];
                    for ( int ndx = 0; ndx < booleanVals.length; ndx++ )
                        booleanVals[ ndx ] = vals[ ndx ].getBoolean();
                    return booleanVals;
                } catch ( final ValueFormatException e ) {
                    throw new IllegalArgumentException( e );
                } catch ( final PathNotFoundException e ) {
                    return null;
                }
            }
        } );
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.modeler.ModelObject#child(java.lang.String)
     */
    @Override
    public ModelObject child( final String childName ) throws ModelerException {
        CheckArg.isNotEmpty( childName, "childName" );
        return manager.run( new Task< ModelObject >() {
            
            @Override
            public ModelObject run( final Session session ) throws Exception {
                try {
                    return new ModelObjectImpl( manager, session.getNode( path ).getNode( childName ).getPath() );
                } catch ( final PathNotFoundException e ) {
                    return null;
                }
            }
        } );
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.modeler.ModelObject#childHasSameNameSiblings(java.lang.String)
     */
    @Override
    public boolean childHasSameNameSiblings( final String childName ) throws ModelerException {
        return children( childName ).length > 1;
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.modeler.ModelObject#children(java.lang.String)
     */
    @Override
    public ModelObject[] children( final String childName ) throws ModelerException {
        CheckArg.isNotEmpty( childName, "childName" );
        return manager.run( new Task< ModelObject[] >() {
            
            @Override
            public ModelObject[] run( final Session session ) throws Exception {
                final NodeIterator iter = session.getNode( path ).getNodes( childName );
                final ModelObject[] obj = new ModelObject[ ( int ) iter.getSize() ];
                for ( int ndx = 0; iter.hasNext(); ndx++ )
                    obj[ ndx ] = new ModelObjectImpl( manager, iter.nextNode().getPath() );
                return obj;
            }
        } );
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.modeler.ModelObject#childrenByName()
     */
    @Override
    public Map< String, ModelObject > childrenByName() throws ModelerException {
        return manager.run( new Task< Map< String, ModelObject > >() {
            
            @Override
            public Map< String, ModelObject > run( final Session session ) throws Exception {
                final Map< String, ModelObject > childrenByName = new HashMap<>();
                for ( final NodeIterator iter = session.getNode( path ).getNodes(); iter.hasNext(); ) {
                    final Node node = iter.nextNode();
                    childrenByName.put( node.getName(), new ModelObjectImpl( manager, node.getPath() ) );
                }
                return childrenByName;
            }
        } );
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.modeler.ModelObject#longValue(java.lang.String)
     */
    @Override
    public Long longValue( final String propertyName ) throws ModelerException {
        CheckArg.isNotEmpty( propertyName, "propertyName" );
        return manager.run( new Task< Long >() {
            
            @Override
            public Long run( final Session session ) throws Exception {
                try {
                    return session.getNode( path ).getProperty( propertyName ).getLong();
                } catch ( final ValueFormatException e ) {
                    throw new IllegalArgumentException( e );
                } catch ( final PathNotFoundException e ) {
                    return null;
                }
            }
        } );
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.modeler.ModelObject#longValues(java.lang.String)
     */
    @Override
    public Long[] longValues( final String propertyName ) throws ModelerException {
        CheckArg.isNotEmpty( propertyName, "propertyName" );
        return manager.run( new Task< Long[] >() {
            
            @Override
            public Long[] run( final Session session ) throws Exception {
                try {
                    final Property prop = session.getNode( path ).getProperty( propertyName );
                    if ( !prop.isMultiple() ) return new Long[] { prop.getLong() };
                    final Value[] vals = prop.getValues();
                    final Long[] longVals = new Long[ vals.length ];
                    for ( int ndx = 0; ndx < longVals.length; ndx++ )
                        longVals[ ndx ] = vals[ ndx ].getLong();
                    return longVals;
                } catch ( final ValueFormatException e ) {
                    throw new IllegalArgumentException( e );
                } catch ( final PathNotFoundException e ) {
                    return null;
                }
            }
        } );
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.modeler.ModelObject#mixinTypes()
     */
    @Override
    public String[] mixinTypes() throws ModelerException {
        return manager.run( new Task< String[] >() {
            
            @Override
            public String[] run( final Session session ) throws Exception {
                final Value[] vals = session.getNode( path ).getProperty( JcrLexicon.MIXIN_TYPES.toString() ).getValues();
                final String[] types = new String[ vals.length ];
                for ( int ndx = 0; ndx < types.length; ndx++ )
                    types[ ndx ] = vals[ ndx ].getString();
                return types;
            }
        } );
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.modeler.ModelObject#name()
     */
    @Override
    public String name() throws ModelerException {
        return manager.run( new Task< String >() {
            
            @Override
            public String run( final Session session ) throws Exception {
                return session.getNode( path ).getName();
            }
        } );
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.modeler.ModelObject#path()
     */
    @Override
    public String path() {
        return path;
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.modeler.ModelObject#primaryType()
     */
    @Override
    public String primaryType() throws ModelerException {
        return manager.run( new Task< String >() {
            
            @Override
            public String run( final Session session ) throws Exception {
                return session.getNode( path ).getProperty( JcrLexicon.PRIMARY_TYPE.toString() ).getString();
            }
        } );
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.modeler.ModelObject#propertyHasMultipleValues(java.lang.String)
     */
    @Override
    public boolean propertyHasMultipleValues( final String propertyName ) throws ModelerException {
        CheckArg.isNotEmpty( propertyName, "propertyName" );
        return manager.run( new Task< Boolean >() {
            
            @Override
            public Boolean run( final Session session ) throws Exception {
                try {
                    return session.getNode( path ).getProperty( propertyName ).isMultiple();
                } catch ( final PathNotFoundException e ) {
                    return false;
                }
            }
        } );
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.modeler.ModelObject#propertyValuesByName()
     */
    @Override
    public Map< String, Object > propertyValuesByName() throws ModelerException {
        return manager.run( new Task< Map< String, Object > >() {
            
            @Override
            public Map< String, Object > run( final Session session ) throws Exception {
                final Map< String, Object > valsByName = new HashMap<>();
                for ( final PropertyIterator iter = session.getNode( path ).getProperties(); iter.hasNext(); ) {
                    final Property prop = iter.nextProperty();
                    if ( prop.getName().startsWith( JcrLexicon.Namespace.PREFIX ) ) continue;
                    final Object val;
                    switch ( prop.getType() ) {
                        case PropertyType.LONG: {
                            if ( prop.isMultiple() ) {
                                final List< Long > list = new ArrayList<>();
                                for ( final Value listVal : prop.getValues() )
                                    list.add( listVal.getLong() );
                                val = list;
                            }
                            else val = prop.getLong();
                            break;
                        }
                        default: {
                            if ( prop.isMultiple() ) {
                                final List< String > list = new ArrayList<>();
                                for ( final Value listVal : prop.getValues() )
                                    list.add( listVal.getString() );
                                val = list;
                            }
                            else val = prop.getString();
                            break;
                        }
                    }
                    valsByName.put( prop.getName(), val );
                }
                return valsByName;
            }
        } );
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.modeler.ModelObject#stringValue(java.lang.String)
     */
    @Override
    public String stringValue( final String propertyName ) throws ModelerException {
        CheckArg.isNotEmpty( propertyName, "propertyName" );
        return manager.run( new Task< String >() {
            
            @Override
            public String run( final Session session ) throws Exception {
                try {
                    return session.getNode( path ).getProperty( propertyName ).getString();
                } catch ( final ValueFormatException e ) {
                    throw new IllegalArgumentException( e );
                } catch ( final PathNotFoundException e ) {
                    return null;
                }
            }
        } );
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.modeler.ModelObject#stringValues(java.lang.String)
     */
    @Override
    public String[] stringValues( final String propertyName ) throws ModelerException {
        CheckArg.isNotEmpty( propertyName, "propertyName" );
        return manager.run( new Task< String[] >() {
            
            @Override
            public String[] run( final Session session ) throws Exception {
                try {
                    final Property prop = session.getNode( path ).getProperty( propertyName );
                    if ( !prop.isMultiple() ) return new String[] { prop.getString() };
                    final Value[] vals = prop.getValues();
                    final String[] stringVals = new String[ vals.length ];
                    for ( int ndx = 0; ndx < stringVals.length; ndx++ )
                        stringVals[ ndx ] = vals[ ndx ].getString();
                    return stringVals;
                } catch ( final ValueFormatException e ) {
                    throw new IllegalArgumentException( e );
                } catch ( final PathNotFoundException e ) {
                    return null;
                }
            }
        } );
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return path;
    }
}
