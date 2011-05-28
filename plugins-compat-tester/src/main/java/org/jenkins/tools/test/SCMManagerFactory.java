/*
 * The MIT License
 *
 * Copyright (c) 2004-2010, Sun Microsystems, Inc., Kohsuke Kawaguchi,
 * Erik Ramfelt, Koichi Fujikawa, Red Hat, Inc., Seiji Sogabe,
 * Stephen Connolly, Tom Huybrechts, Yahoo! Inc., Alan Harder, CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jenkins.tools.test;

import org.apache.maven.scm.manager.ScmManager;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

/**
 * Utility class to start plexus
 * @author Frederic Camblor, Olivier Lamy
 */
public class SCMManagerFactory {

	private static final SCMManagerFactory INSTANCE = new SCMManagerFactory();
	
	private DefaultPlexusContainer plexus = null;

	private SCMManagerFactory(){
	}
	
	public void start() throws PlexusContainerException {
		if(plexus == null){
			this.plexus = new DefaultPlexusContainer();
            this.plexus.setLoggerManager( new PluginCompatTesterLoggerManager() );
			try {
				// These will only be useful for Hudson v1.395 and under
				// ... Since the use of sisu-plexus-inject will initialize
				// everything in the constructor
		        PlexusContainer.class.getDeclaredMethod("initialize").invoke(this.plexus);
		        PlexusContainer.class.getDeclaredMethod("start").invoke(this.plexus);
		    } catch (Throwable e) { /* Don't do anything here ... initialize/start methods should be called prior to v1.395 ! */ }
		}
	}
	
	public ScmManager createScmManager() throws ComponentLookupException {
		return (ScmManager)this.plexus.lookup(ScmManager.ROLE);
	}
	
	public void stop() throws Exception {
		this.plexus.dispose();
		this.plexus = null;
	}
	
	public static SCMManagerFactory getInstance(){
		return INSTANCE;
	}
}
