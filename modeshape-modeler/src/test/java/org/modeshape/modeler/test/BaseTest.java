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
package org.modeshape.modeler.test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.jcr.Session;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.modeshape.modeler.Modeler;
import org.modeshape.modeler.ModelerException;
import org.modeshape.modeler.TestUtil;
import org.modeshape.modeler.impl.ModelTypeManagerImpl;

/**
 * Superclass for all test classes
 */
@RunWith( TestRunner.class )
public abstract class BaseTest {
    
    protected Modeler modeler;
    protected ModelTypeManagerImpl modelTypeManager;
    
    @After
    public void after() throws Exception {
        modeler.stop();
    }
    
    @Before
    public void before() {
        modeler = new Modeler();
        modelTypeManager = ( ModelTypeManagerImpl ) modeler.modelTypeManager();
    }
    
    protected String importContent( final String content ) throws Exception {
        return modeler.importContent( "stuff", new ByteArrayInputStream( content.getBytes() ), null );
    }
    
    protected Session session() throws ModelerException {
        return TestUtil.session( modeler );
    }
    
    protected InputStream stream( final String content ) {
        return new ByteArrayInputStream( content.getBytes() );
    }
}
