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

import java.util.List;
import java.util.Map;

/**
 * 
 */
public interface ModelObject {
    
    /**
     * @return this model object's absolute workspace path; never <code>null</code>
     */
    String absolutePath();
    
    /**
     * @param propertyName
     *        the name of one of this model object's single-valued properties
     * @return the Boolean value of the supplied property, or <code>null</code> if the property doesn't exist
     * @throws ModelerException
     *         if any error occurs
     */
    Boolean booleanValue( String propertyName ) throws ModelerException;
    
    /**
     * @param propertyName
     *        the name of one of this model object's multi-valued properties
     * @return the Boolean values of the supplied property, or <code>null</code> if the property doesn't exist
     * @throws ModelerException
     *         if any error occurs
     */
    Boolean[] booleanValues( String propertyName ) throws ModelerException;
    
    /**
     * @param childName
     *        the name of one of this model object's children
     * @return the child model object with the supplied name
     * @throws ModelerException
     *         if any error occurs
     */
    ModelObject child( String childName ) throws ModelerException;
    
    /**
     * @param childName
     *        the name of one of this model object's children
     * @return <code>true</code> if this model object has multiple children with the supplied name
     * @throws ModelerException
     *         if any error occurs
     */
    boolean childHasSameNameSiblings( String childName ) throws ModelerException;
    
    /**
     * @param childName
     *        the name of one of this model object's children
     * @return the child model objects with the supplied name
     * @throws ModelerException
     *         if any error occurs
     */
    ModelObject[] children( String childName ) throws ModelerException;
    
    /**
     * @return the map of children by name for this model object; never <code>null</code>.
     * @throws ModelerException
     *         if any error occurs
     */
    Map< String, List< ModelObject > > childrenByName() throws ModelerException;
    
    /**
     * @param propertyName
     *        the name of one of this model object's single-valued properties
     * @return the Long value of the supplied property, or <code>null</code> if the property doesn't exist
     * @throws ModelerException
     *         if any error occurs
     */
    Long longValue( String propertyName ) throws ModelerException;
    
    /**
     * @param propertyName
     *        the name of one of this model object's multi-valued properties
     * @return the Long values of the supplied property, or <code>null</code> if the property doesn't exist
     * @throws ModelerException
     *         if any error occurs
     */
    Long[] longValues( String propertyName ) throws ModelerException;
    
    /**
     * @return this model object's mixin types; never <code>null</code>
     * @throws ModelerException
     *         if any error occurs
     */
    String[] mixinTypes() throws ModelerException;
    
    /**
     * @return this model object's enclosing model. Never <code>null</code>.
     * @throws ModelerException
     *         if any error occurs
     */
    Model model() throws ModelerException;
    
    /**
     * @return this model object's path relative to its {@link #model() model}; never <code>null</code>
     * @throws ModelerException
     *         if any error occurs
     */
    String modelRelativePath() throws ModelerException;
    
    /**
     * @return this model object's name; never <code>null</code>
     * @throws ModelerException
     *         if any error occurs
     */
    String name() throws ModelerException;
    
    /**
     * @return this model object's primary type; never <code>null</code>
     * @throws ModelerException
     *         if any error occurs
     */
    String primaryType() throws ModelerException;
    
    /**
     * @param propertyName
     *        the name of one of this model object's properties
     * @return <code>true</code> if the supplied property has multiple values
     * @throws ModelerException
     *         if any error occurs
     */
    boolean propertyHasMultipleValues( String propertyName ) throws ModelerException;
    
    /**
     * @return the map of property values by name for this model object; never <code>null</code>.
     * @throws ModelerException
     *         if any error occurs
     */
    Map< String, Object > propertyValuesByName() throws ModelerException;
    
    /**
     * @param propertyName
     *        the name of one of this model object's single-valued properties
     * @return the String value of the supplied property, or <code>null</code> if the property doesn't exist
     * @throws ModelerException
     *         if any error occurs
     */
    String stringValue( String propertyName ) throws ModelerException;
    
    /**
     * @param propertyName
     *        the name of one of this model object's multi-valued properties
     * @return the String values of the supplied property, or <code>null</code> if the property doesn't exist
     * @throws ModelerException
     *         if any error occurs
     */
    String[] stringValues( String propertyName ) throws ModelerException;
}
