/*
 * Copyright (c) 2006-2007 Sun Microsystems, Inc.  All rights reserved.
 *  
 *  The Sun Project JXTA(TM) Software License
 *  
 *  Redistribution and use in source and binary forms, with or without 
 *  modification, are permitted provided that the following conditions are met:
 *  
 *  1. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *  
 *  2. Redistributions in binary form must reproduce the above copyright notice, 
 *     this list of conditions and the following disclaimer in the documentation 
 *     and/or other materials provided with the distribution.
 *  
 *  3. The end-user documentation included with the redistribution, if any, must 
 *     include the following acknowledgment: "This product includes software 
 *     developed by Sun Microsystems, Inc. for JXTA(TM) technology." 
 *     Alternately, this acknowledgment may appear in the software itself, if 
 *     and wherever such third-party acknowledgments normally appear.
 *  
 *  4. The names "Sun", "Sun Microsystems, Inc.", "JXTA" and "Project JXTA" must 
 *     not be used to endorse or promote products derived from this software 
 *     without prior written permission. For written permission, please contact 
 *     Project JXTA at http://www.jxta.org.
 *  
 *  5. Products derived from this software may not be called "JXTA", nor may 
 *     "JXTA" appear in their name, without prior written permission of Sun.
 *  
 *  THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 *  INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND 
 *  FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL SUN 
 *  MICROSYSTEMS OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, 
 *  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, 
 *  OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF 
 *  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING 
 *  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 *  EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 *  JXTA is a registered trademark of Sun Microsystems, Inc. in the United 
 *  States and other countries.
 *  
 *  Please see the license information page at :
 *  <http://www.jxta.org/project/www/license.html> for instructions on use of 
 *  the license in source files.
 *  
 *  ====================================================================
 *  
 *  This software consists of voluntary contributions made by many individuals 
 *  on behalf of Project JXTA. For more information on Project JXTA, please see 
 *  http://www.jxta.org.
 *  
 *  This license is based on the BSD license adopted by the Apache Foundation. 
 */
//package tutorial.helloworld;
import java.text.MessageFormat;
import java.io.File;

/**
 * A simple example which demonstrates the most basic JXTA operations. This
 * program configures the peer, starts JXTA, waits until the peer joins the
 * network and then concludes by stopping JXTA. 
 */
public class HelloWorld {

    /**
     * Main method
     *
     * @param args Command line arguments. none defined
     */
    public static void main(String args[]) {
        
        try {
            // Set the main thread name for debugging.
            Thread.currentThread().setName(HelloWorld.class.getSimpleName());

            // Configure this peer as an ad-hoc peer named "HelloWorld" and
            // store configuration into ".cache/HelloWorld" directory.
            System.out.println("Configuring JXTA");
            NetworkManager manager = new NetworkManager(NetworkManager.ConfigMode.ADHOC, "HelloWorld", new File(new File(".cache"), "HelloWorld").toURI());
            
            // Start the JXTA 
            System.out.println("Starting JXTA");
            manager.startNetwork();
            System.out.println("JXTA Started");
            
            // Wait up to 20 seconds for a connection to the JXTA Network.
            System.out.println("Waiting for a rendezvous connection");
            boolean connected = manager.waitForRendezvousConnection(20 * 1000);
            System.out.println(MessageFormat.format("Connected :{0}", connected));
            
            // Stop JXTA
            System.out.println("Stopping JXTA");
            manager.stopNetwork();
            System.out.println("JXTA stopped");            
        } catch (Throwable e) {
            // Some type of error occurred. Print stack trace and quit.
            System.err.println("Fatal error -- Quitting");
            e.printStackTrace(System.err);
            System.exit(-1);
        }
    }

    private static class NetworkManager {

        public NetworkManager() {
        }

        private void stopNetwork() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        private boolean waitForRendezvousConnection(int i) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        private void startNetwork() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        private static class ConfigMode {

            public ConfigMode() {
            }
        }
    }
}