package kutuni.tomcat;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Date;
import java.util.Map;

import org.apache.catalina.Context;
import org.apache.catalina.Globals;
import org.apache.catalina.Session;
import org.apache.catalina.session.StandardSession;
import org.apache.catalina.session.StoreBase;

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

public final class HCStore extends StoreBase {

	private String grupName = "dev";
	private String grupPassword = "dev";
	private String multicast = "true";
	private String networkInterface = "";
	private String port = "5709";
	private String log = "true";
	private String newInstance = "false";
	private String instanceName = "local";

	private HazelcastInstance instance;
	private static final String storeName = "sessionStore";
	private static final String threadName = "HCSessionStore";

	public String getGrupName() {
		return grupName;
	}

	public void setGrupName(String grupName) {
		this.grupName = grupName;
	}

	public String getGrupPassword() {
		return grupPassword;
	}

	public void setGrupPassword(String grupPassword) {
		this.grupPassword = grupPassword;
	}

	public String getMulticast() {
		return multicast;
	}

	public boolean isMulticast() {
		return (this.multicast == "true" ? true : false);
	}

	public void setMulticast(String multicast) {
		this.multicast = multicast;
	}

	public String getNetworkInterface() {
		return networkInterface;
	}

	public void setNetworkInterface(String networkInterface) {
		this.networkInterface = networkInterface;
	}

	public String getPort() {
		return port;
	}

	public void setPort(String port) {
		this.port = port;
	}

	public boolean isLog() {
		return (this.log == "true" ? true : false);
	}

	public String getNewInstance() {
		return newInstance;
	}

	public boolean isNewInstance() {
		return (this.newInstance == "true" ? true : false);
	}

	public void setNewInstance(String newInstance) {
		this.newInstance = newInstance;
	}

	public String getInstanceName() {
		return instanceName;
	}

	public void setInstanceName(String instanceName) {
		this.instanceName = instanceName;
	}

	public String getThreadName() {
		return threadName;
	}

	public int getSize() throws IOException {
		if (getSessionStore() == null) {
            return (0);
        }
		return getSessionStore().size();
	}

	public void clear() throws IOException {
		String[] keys = keys();
		for (int i = 0; i < keys.length; i++) {
			remove(keys[i]);
		}
	}

	public String[] keys() {
		if (getSessionStore() == null) {
            return (new String[0]);
        }
		int count=getSessionStore().size();
		if (count==0) return new String[0];
		String[] keys = getSessionStore().keySet().toArray(new String[count]);
		return keys;
	}

	public Session load(String id) throws ClassNotFoundException, IOException {
		log("Load session:"+id);
		Context context = getManager().getContext();
		ClassLoader oldThreadContextCL = context.bind(Globals.IS_SECURITY_ENABLED, null);
		byte[] bytes=null;
		if (getSessionStore()!=null) bytes= getSessionStore().get(id);
		
		try {
			if (bytes != null) {
				log("Session found cluster");
				ByteArrayInputStream baos=new ByteArrayInputStream(bytes);
				ObjectInputStream ois = new ObjectInputStream(baos);
				StandardSession session = (StandardSession)manager.createEmptySession();
				session.readObjectData(ois);
				session.setManager(manager);
				return session;
			} else
				return null;
		} finally {
			context.unbind(Globals.IS_SECURITY_ENABLED, oldThreadContextCL);
		}
	}

	@Override
	public void remove(String id) throws IOException {
		log("remove :"+id);
		if (getSessionStore() != null) {
			getSessionStore().remove(id);
		}
	}

	@Override
	public void save(Session session) {
		log("Save Session:"+session.getIdInternal());
		if (getSessionStore() == null) return;
		
		ByteArrayOutputStream fos=new ByteArrayOutputStream();
		try{
		   ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(fos));
	       ((StandardSession)session).writeObjectData(oos);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
        byte[] bytes=fos.toByteArray();
        getSessionStore().put(session.getIdInternal(), bytes);
	}

	private void startHCInstance() {
		synchronized(this)
		{
			log("HC Network Plugin Initializing...");
			log("Port:" + getPort());
			log("Instance Name:" + getInstanceName());
			log("Multicast Enable:" + isMulticast());
			log("TCP Interface:" + getNetworkInterface());
			log("Group Name:" + getGrupName());
			
			if (!isNewInstance()) {
				instance = Hazelcast.getHazelcastInstanceByName(getInstanceName());
				log("Hazelcast join allready instance, name:"+ instanceName);
			} else {
				log("Hazelcast creating new instance, name:"+ instanceName);
				System.setProperty("hazelcast.prefer.ipv4.stack", "true");
				Config cfg = new Config();
				cfg.setInstanceName(getInstanceName());
				NetworkConfig network = cfg.getNetworkConfig();
				network.setPort(Integer.valueOf(port));
				network.setReuseAddress(true);
				JoinConfig join = network.getJoin();
				join.getMulticastConfig().setEnabled(isMulticast());
				join.getTcpIpConfig().setEnabled(!isMulticast());
				if ((getNetworkInterface() != null)
						&& (getNetworkInterface().length() > 7))
					network.getInterfaces().setEnabled(true).addInterface(getNetworkInterface());
	
				instance = Hazelcast.getOrCreateHazelcastInstance(cfg);
				log("HC new Cluster Ready");
			}

			try {
				Thread.sleep(3000); //wait for sync
			} catch (InterruptedException e) {}
			
		}
		log("hazelcast instance:"+instance);
	}
	
	public Map<String, byte[]> getSessionStore() {
		if (instance!=null) startHCInstance();
		if (instance==null) return null;
		return instance.getReplicatedMap(storeName);
	}

	private void log(String logStr){
		if (isLog()){
			Date now=new Date();
			System.out.println(now+" DEBUG "+getClass().getName()+" "+logStr);
		}
	}
}