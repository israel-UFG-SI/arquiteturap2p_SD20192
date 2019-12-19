
import com.sun.corba.se.impl.protocol.giopmsgheaders.FragmentMessage;
import com.sun.corba.se.impl.protocol.giopmsgheaders.MessageHandler;
import com.sun.corba.se.spi.ior.iiop.GIOPVersion;
import com.sun.xml.internal.ws.api.server.Module;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Random;
import java.util.Vector;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.swing.JOptionPane;
import org.omg.CORBA.portable.InputStream;
import org.omg.CORBA.portable.OutputStream;

public class P2PChat implements DiscoveryListener, PipeMsgListener {
	private final NetworkManager manager;
	private PeerGroup netPeerGroup;
	private final Vector<HashSet<PeerID>> peers;
	private final MessageRelay messages;
	private final String peerName;
	private final PeerID peerID;
	private PeerGroup chatGroup;
	private PipeID unicastID;
	private PipeID multicastID;
	private PipeID serviceID;
	private PipeService pipeService;
	private DiscoveryService discovery;
	
	private static final String groupName = "myP2Pchat";
	private static final String groupDesc = "P2P chat";
	private static final PeerGroupID groupID = IDFactory.newPeerGroupID(
			PeerGroupID.defaultNetPeerGroupID, groupName.getBytes());
	private static final String unicastName = "uniP2PChat";
	private static final String multicastName = "multiP2PChat";
	private static final String serviceName = "P2PChat";
	
	public P2PChat(MessageRelay messages) throws IOException, PeerGroupException {
		this.messages = messages;
		
		peerName = "Peer " + new Random().nextInt(1000000);
		peerID = IDFactory.newPeerID(PeerGroupID.defaultNetPeerGroupID, peerName.getBytes());
		
		manager = new NetworkManager(NetworkManager.ConfigMode.ADHOC,
				peerName, new File(new File(".cache"), "Chat").toURI());
		
		int port = 9000 + new Random().nextInt(100);
		
		NetworkConfigurator config = manager.getConfigurator();
		config.setTcpPort(port);
		config.setTcpEnabled(true);
		config.setTcpIncoming(true);
		config.setTcpOutgoing(true);
		config.setUseMulticast(true);
		config.setPeerID(peerID);
		
		
		peers = new Vector<HashSet<PeerID>>();
	}

	public void start() {
            netPeerGroup = manager.startNetwork();
		
		ModuleImplAdvertisement mAdv = null;
	  
		try {
			mAdv = netPeerGroup.getAllPurposePeerGroupImplAdvertisement();
			chatGroup = netPeerGroup.newGroup(groupID, mAdv, groupName, groupDesc);
		} 
		catch (Exception ex) {
			System.err.println(ex.toString());
		}
		  
        if (Module.START_OK != chatGroup.startApp(new String[0]))
            System.err.println("Cannot start child peergroup");
        
        unicastID = IDFactory.newPipeID(chatGroup.getPeerGroupID(), unicastName.getBytes());
        multicastID = IDFactory.newPipeID(chatGroup.getPeerGroupID(), multicastName.getBytes());
        
        pipeService = chatGroup.getPipeService();
        pipeService.createInputPipe(getAdvertisement(unicastID, false), this);
        pipeService.createInputPipe(getAdvertisement(multicastID, true), this);
        
        discovery = chatGroup.getDiscoveryService();
        discovery.addDiscoveryListener(this);
        
        ModuleClassAdvertisement mcadv;
            mcadv = (ModuleClassAdvertisement)
                    AdvertisementFactory.newAdvertisement(ModuleClassAdvertisement.getAdvertisementType());
        mcadv.setName("P2PChat");
        mcadv.setDescription("P2PChat Module Advertisement");
        
        ModuleClassID mcID = IDFactory.newModuleClassID();
        mcadv.setModuleClassID(mcID);
        
        discovery.publish(mcadv);
        discovery.remotePublish(mcadv);
        
        ModuleSpecAdvertisement mdadv;
            mdadv = (ModuleSpecAdvertisement)
                    AdvertisementFactory.newAdvertisement(ModuleSpecAdvertisement.getAdvertisementType());
        mdadv.setName("P2PChat");
        mdadv.setVersion("Version 1.0");
        mdadv.setCreator("4c0n.nl");
        mdadv.setModuleSpecID(IDFactory.newModuleSpecID(mcID));
        mdadv.setSpecURI("http://www.4c0n.nl");
        
        serviceID = (PipeID) IDFactory.newPipeID(chatGroup.getPeerGroupID(), serviceName.getBytes());
        PipeAdvertisement pipeAdv = getAdvertisement(serviceID, false);
        mdadv.setPipeAdvertisement(pipeAdv);
        
        discovery.publish(mdadv);
        discovery.remotePublish(mdadv);
        pipeService.createInputPipe(pipeAdv, this);
	}
	
    private static PipeAdvertisement getAdvertisement(PipeID id, boolean isMulticast) {
        PipeAdvertisement adv;
            adv = (PipeAdvertisement )AdvertisementFactory.
                    newAdvertisement(PipeAdvertisement.getAdvertisementType());
        adv.setPipeID(id);
        if (isMulticast)
            adv.setType(PipeService.PropagateType); 
        else 
            adv.setType(PipeService.UnicastType); 
        adv.setName("P2PChatPipe");
        adv.setDescription("Pipe for p2p chat messages");
        return adv;
    }
	
	public void discoveryEvent(DiscoveryEvent event) { // Peer/pipe found
		String addr = "urn:jxta:" + event.getSource().toString().substring(7);
		System.out.println("Discovered: " + addr);
		PeerID peer;
		try {
			URI uri = new URI(addr);
			System.out.println("to: " + uri.toString());
			peer = (PeerID) IDFactory.fromURI(uri);
			HashSet<PeerID> pids = new HashSet<>();
			pids.add(peer);
			if(!peers.contains(pids)) {
				peers.add(pids);
				sendUsername(pids);
			}
		} 
		catch (URISyntaxException e) {
		}
	}
	
	private void sendUsername(HashSet<PeerID> pids) {
		PipeAdvertisement pipeAdv = getAdvertisement(unicastID, false);
                OutputPipe out = pipeService.createOutputPipe(pipeAdv, pids, 10000);
                Message msg;
            msg = new Message() {
                public GIOPVersion getGIOPVersion() {
                    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }
                
                public byte getEncodingVersion() {
                    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }
                
                public boolean isLittleEndian() {
                    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }
                
                public boolean moreFragmentsToFollow() {
                    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }
                
                public int getType() {
                    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }
                
                public int getSize() {
                    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }
                
                public ByteBuffer getByteBuffer() {
                    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }
                
                public int getThreadPoolToUse() {
                    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }
                
                public void read(InputStream in) {
                    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }
                
                public void write(OutputStream out) {
                    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }
                
                public void setSize(ByteBuffer bb, int i) {
                    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }
                
                public FragmentMessage createFragmentMessage() {
                    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }
                
                public void callback(MessageHandler mh) throws IOException {
                    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }
                
                public void setByteBuffer(ByteBuffer bb) {
                    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }
                
                public void setEncodingVersion(byte b) {
                    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }
            };
                StringMessageElement username = new StringMessageElement("username", messages.getName(), null);
                msg.addMessageElement(username);
                out.send(msg);
	}
	
	public void sendMessages() {
		new Thread("Message sender thread") {
			@Override
			public void run() {
				while(true) {
					// Create Message
					PipeAdvertisement pipeAdv = getAdvertisement(unicastID, false);
					Vector<String> msgs = messages.getOutgoingMessages();
					if(!msgs.isEmpty()) {
						StringMessageElement from = new StringMessageElement("from", messages.getName(), null);
                                                peers.stream().map((pids) -> pipeService.createOutputPipe(pipeAdv, pids, 0)).forEachOrdered(new Consumer<OutputPipe>() {
                                                    @Override
                                                    public void accept(OutputPipe out) {
                                                        msgs.stream().map(new Function<String, Message>() {
                                                            @Override
                                                            public Message apply(String s) {
                                                                Message msg;
                                                                msg = new Message() {
                                                                    
                                                                     GIOPVersion getGIOPVersion() {
                                                                        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                                                                    }

                                                                     byte getEncodingVersion() {
                                                                        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                                                                    }

                                                                   
                                                                     boolean isLittleEndian() {
                                                                        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                                                                    }

                                                                  
                                                                     boolean moreFragmentsToFollow() {
                                                                        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                                                                    }

                                                                  
                                                                     int getType() {
                                                                        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                                                                    }

                                                                  
                                                                     int getSize() {
                                                                        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                                                                    }

                                                                
                                                                     ByteBuffer getByteBuffer() {
                                                                        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                                                                    }

                                                                
                                                                    public int getThreadPoolToUse() {
                                                                        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                                                                    }

                                                            
                                                                    public void read(InputStream in) {
                                                                        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                                                                    }

                                                                 
                                                                    public void write(OutputStream out) {
                                                                        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                                                                    }

                                                                  
                                                                    public void setSize(ByteBuffer bb, int i) {
                                                                        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                                                                    }

                                                              
                                                                    public FragmentMessage createFragmentMessage() {
                                                                        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                                                                    }

                                                                    public void callback(MessageHandler mh) throws IOException {
                                                                        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                                                                    }

                                                                    
                                                                    public void setByteBuffer(ByteBuffer bb) {
                                                                        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                                                                    }

                                                                    
                                                                    public void setEncodingVersion(byte b) {
                                                                        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                                                                    }
                                                                };
                                                                StringMessageElement body = new StringMessageElement("body", s, null);
                                                                msg.addMessageElement(from);
                                                                msg.addMessageElement(body);
                                                                return msg;
                                                            }
                                                        }).map((msg) -> {
                                                            out.send(msg);
                                                            return msg;
                                                        }).forEachOrdered((_item) -> {
                                                            System.out.println("Message sent!");
                                                        });
                                                    }
                                                });
					}
					try {
						Thread.sleep(1000);
					} 
					catch (InterruptedException e) {
					}
				}
			}
		}.start();
	}
	
    private void fetchAdvertisements() {
        new Thread("fetch advertisements thread") {
           @Override
           public void run() {
              while(true) {
                  discovery.getRemoteAdvertisements(null, DiscoveryService.ADV, "Name", "P2PChat", 1, null);
                  try {
                      sleep(10000);

                  }
                  catch(InterruptedException e) {} 
              }
           }
        }.start();
     }
	
	/**
	 * @param args
     * @throws java.io.IOException
	 */
	public static void main(String[] args) throws IOException {
		MessageRelay messages = new MessageRelay();
		String name = (String) JOptionPane.showInputDialog(null, "Please enter a username", "Username",
				JOptionPane.QUESTION_MESSAGE, null, null, "user");
		if(name.equals("")) {
			name = "user";
		}
		messages.setName(name);
		try {
			P2PChat chat = new P2PChat(messages);
			chat.start();
			chat.fetchAdvertisements();
			chat.sendMessages();
		} 
		catch (PeerGroupException e) {
			System.err.println("Could not be started!");
			return;
		}
		MainFrame mainFrame = new MainFrame(messages);
	}

	public void pipeMsgEvent(PipeMsgEvent event) {
		System.out.println("Message Received!!");
		
		Message msg = event.getMessage();
		Object user = msg.getMessageElement("username");
		if(user != null) {
			messages.addUser(user.toString());
		}
		else {
			String content[] = new String[2];
			content[0] = msg.getMessageElement("from").toString();
			content[1] = msg.getMessageElement("body").toString();
			messages.addIncomingMessage(content);
		}
	}

    private static class NetworkManager {

        public NetworkManager() {
        }

        private NetworkConfigurator getConfigurator() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        

        private PeerGroup startNetwork() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        private static class ConfigMode {

            public ConfigMode() {
            }
        }
    }

    private static class PeerGroup {

        public PeerGroup() {
        }

        private ModuleImplAdvertisement getAllPurposePeerGroupImplAdvertisement() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        private PeerGroup newGroup(PeerGroupID groupID, ModuleImplAdvertisement mAdv, String groupName, String groupDesc) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        private boolean startApp(String[] string) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        private PipeService getPipeService() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        private DiscoveryService getDiscoveryService() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }

    private static class PeerID {

        public PeerID() {
        }
    }

    private static class PipeID {

        public PipeID() {
        }
    }

    private static class PipeService {

        public PipeService() {
        }

        private void createInputPipe(PipeAdvertisement advertisement, P2PChat aThis) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        private OutputPipe createOutputPipe(PipeAdvertisement pipeAdv, HashSet<PeerID> pids, int i) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }

    private static class DiscoveryService {

        public DiscoveryService() {
        }

        private void addDiscoveryListener(P2PChat aThis) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        private void publish(ModuleClassAdvertisement mcadv) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        private void remotePublish(ModuleClassAdvertisement mcadv) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        private void publish(ModuleSpecAdvertisement mdadv) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        private void remotePublish(ModuleSpecAdvertisement mdadv) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }

    private static class IDFactory {

        private static ModuleClassID newModuleClassID() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        public IDFactory() {
        }
    }

    private static class PeerGroupID {

        public PeerGroupID() {
        }
    }

    private static class PeerGroupException extends Exception {

        public PeerGroupException() {
        }
    }

    private static class NetworkConfigurator {

        public NetworkConfigurator() {
        }

        private void setTcpPort(int port) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        private void setTcpEnabled(boolean b) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        private void setTcpIncoming(boolean b) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        private void setTcpOutgoing(boolean b) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        private void setUseMulticast(boolean b) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        private void setPeerID(PeerID peerID) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }

    private static class ModuleImplAdvertisement {

        public ModuleImplAdvertisement() {
        }
    }

    private static class ModuleClassAdvertisement {

        public ModuleClassAdvertisement() {
        }

        private void setName(String p2PChat) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        private void setDescription(String p2PChat_Module_Advertisement) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        private void setModuleClassID(ModuleClassID mcID) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }

    private static class AdvertisementFactory {

        public AdvertisementFactory() {
        }
    }

    private static class PipeAdvertisement {

        public PipeAdvertisement() {
        }

        private void setPipeID(PipeID id) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        private void setName(String p2PChatPipe) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        private void setDescription(String pipe_for_p2p_chat_messages) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }

    private static class ModuleClassID {

        public ModuleClassID() {
        }
    }

    private static class ModuleSpecAdvertisement {

        public ModuleSpecAdvertisement() {
        }

        private void setName(String p2PChat) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        private void setVersion(String version_10) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        private void setCreator(String c0nnl) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        private void setSpecURI(String httpwww4c0nnl) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        private void setPipeAdvertisement(PipeAdvertisement pipeAdv) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }

    private static class DiscoveryEvent {

        public DiscoveryEvent() {
        }

        private Object getSource() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }

    private static class OutputPipe {

        public OutputPipe() {
        }

        private void send(Message msg) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }

    private static class MessageElement {

        public MessageElement() {
        }
    }

    private static class PipeMsgEvent {

        public PipeMsgEvent() {
        }

        private Message getMessage() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }

    private static class Message {

        public Message() {
        }

        private void addMessageElement(StringMessageElement username) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        private Object getMessageElement(String username) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }

    private static class StringMessageElement {

        public StringMessageElement() {
        }

        private StringMessageElement(String body, String s, Object object) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }
}