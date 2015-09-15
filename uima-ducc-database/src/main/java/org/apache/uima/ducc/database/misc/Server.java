package org.apache.uima.ducc.database.misc;

import java.io.File;
import java.io.FileInputStream;

import com.orientechnologies.orient.server.OServer;
import com.orientechnologies.orient.server.OServerMain;

public class Server
{
    String config = null;
    OServer server = null;
    public Server()
    	throws Exception
    {
        File f = new File("/home/challngr/ducc_runtime/resources/database.xml");
        int len = (int) f.length();
        byte[] buf = new byte[len];
        FileInputStream fis = new FileInputStream(f);
        fis.read(buf, 0, len);
        fis.close();
        config = new String(buf);
    }

    public void run()
    	throws Exception
    {

        server = OServerMain.create();
        server.startup( config );
        server.activate();

        /**
                       "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                       + "<orient-server>"
                       + "<network>"
                       + "<protocols>"
                       + "<protocol name=\"binary\" implementation=\"com.orientechnologies.orient.server.network.protocol.binary.ONetworkProtocolBinary\"/>"
                       + "<protocol name=\"http\" implementation=\"com.orientechnologies.orient.server.network.protocol.http.ONetworkProtocolHttpDb\"/>"
                       + "</protocols>"
                       + "<listeners>"
                       + "<listener ip-address=\"0.0.0.0\" port-range=\"2424-2430\" protocol=\"binary\"/>"
                       + "<listener ip-address=\"0.0.0.0\" port-range=\"2480-2490\" protocol=\"http\"/>"
                       + "</listeners>"
                       + "</network>"
                       + "<users>"
                       + "<user name=\"root\" password=\"asdfasdf\" resources=\"*\"/>"
                       + "</users>"
                       + "<properties>"
                       + "<entry name=\"orientdb.www.path\" value=\"C:/work/dev/orientechnologies/orientdb/releases/1.0rc1-SNAPSHOT/www/\"/>"
                       + "<entry name=\"orientdb.config.file\" value=\"C:/work/dev/orientechnologies/orientdb/releases/1.0rc1-SNAPSHOT/config/orientdb-server-config.xml\"/>"
                       + "<entry name=\"server.cache.staticResources\" value=\"false\"/>"
                       + "<entry name=\"log.console.level\" value=\"info\"/>"
                       + "<entry name=\"log.file.level\" value=\"fine\"/>"
                       + "<entry value=\"/home/challngr/ducc_runtime/database/DuccHistory\" name=\"server.database.path\" />"
                       //The following is required to eliminate an error or warning "Error on resolving property: ORIENTDB_HOME"
                       + "<entry name=\"plugin.dynamic\" value=\"false\"/>"
                       + "</properties>" + "</orient-server>");
        */

    }

    public static void main(String[] args)
        throws Exception 
    {
        try {
            Server s = new Server();
            s.run();
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }

}
