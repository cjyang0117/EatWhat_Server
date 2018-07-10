package eatwhat;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class eatwhat {
    private static Thread th_close;                //�銵��
    private static int serverport = 5050;
    private static ServerSocket serverSocket;    //隡箸�垢��ocket
    private static ArrayList<Socket> socketlist=new ArrayList<Socket>();
    public static void main(String[] args){
        try {
            serverSocket = new ServerSocket(serverport);    //���erver���ort��
            System.out.println("Server���銵�");
            th_close=new Thread(Judge_Close);                //鞈虫�銵�極雿�(��socketlist�����恥�蝡舐雯頝舀蝺�)
            th_close.start();                                //霈銵���銵�
            // �Server���葉���
            while (!serverSocket.isClosed()) {
                // ��蝑���恥�蝡舫���
                waitNewSocket();
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    private static Runnable Judge_Close=new Runnable(){    //霈銵��蝘�銝�甈﹖ocketList�����恥�蝡臬撥��蝺�
        @Override
        public void run() {                                //�甇斗������ifi蝑蝺��� 
            // TODO Auto-generated method stub
            try {
                while(true){
                    Thread.sleep(2000);
                    System.out.println("�����蝺"+socketlist.size());
                    for(int i=0; i<socketlist.size();i++){ //Socket close:socketlist
                        if(isServerClose(socketlist.get(i))) {        //�閰脣恥�蝡舐雯頝舀蝺��,敺ocketList��
                        	System.out.println("���ocket");
                            socketlist.remove(i);
                        }    
                    }
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    };
    private static Boolean isServerClose(Socket socket){    //����蝺�銝剜
        try{  
            socket.sendUrgentData(0);        //����������交���,暺����瘝�����交�����,銝蔣�甇�撣賊��蝺�
            return false;                    //憒迤撣詨��false
        }catch(Exception e){
            return true;                     //憒��蝺葉����true
        }  
    }  
    // 蝑���恥�蝡舫���
    public static void waitNewSocket() {
        try {
            Socket socket = serverSocket.accept();
            System.out.println("��蝺���");
            // ������蝙���
            createNewThread(socket);
        } catch (Exception e) {
            e.printStackTrace();
        }
    } 
    // ����蝙���
    public static void createNewThread(final Socket socket) {
        // 隞交��銵��銵�
        Thread t = new Thread(new Runnable() {
        	BufferedWriter bw;
			//BufferedWriter bw;
        	BufferedReader br;
        	String tmp;
        	JSONObject json_read,json_write;
        	DB db;
            @Override
            public void run() { //Server����� App蝡航��鞈��!
                try {
                    // 憓���蝙���
                    socketlist.add(socket);
                    //���雯頝航撓�銝脫��
                    bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                    // ���雯頝航撓�銝脫��
                    br = new BufferedReader(new InputStreamReader(socket.getInputStream()));                                   
                    // �Socket撌脤�������蝥銵�
                    json_write=new JSONObject();
                    db=new DB(); //Server��云銋���銝DB!!???? ���銋��恐��B��銝 銝��摰�������
                	send("sys", "��蝺���");
                    while (socket.isConnected()) { 
                    	String x=receive("action");
                    	System.out.println(x);
                    	if(x.equals("show")) {	//select���誘����                   	    
                    	    /*String[] n= {"Sid", "Sname"};
                    	    Boolean[] y= {false, true};
                    	    bw.write(db.SelectTable("Select Sid, Sname from Store", n, y)+"\n");
                            bw.flush();*/
                    		String name=json_read.getString("data");
                    		String[] n= {"Sid", "Sname"};
                    	    Boolean[] y= {false, true};
                    	    bw.write(db.SelectTable("Select Sid, Sname from Store where Sname like \"%"+name+"%\"", n, y)+"\n");
                            bw.flush();
                    	}else if(x.equals("show1")) {
                    		int id=json_read.getInt("data");         
                    		String[] n= {"Mname", "Price"};
                    	    Boolean[] y= {true, false};
                    	    bw.write(db.SelectTable("Select Mname, Price from Store, Menu, Storemenu Where Sid="+id+" And Mid=S_mid And Sid=Ssid", n, y)+"\n");
                            bw.flush();
                    	}else if(x.equals("show2")) {
                    		/*String[] n= {"Sname", "Mname", "Price"}; 
                    	    Boolean[] y= {true, true, false}; 
                    	    bw.write(db.SelectTable("Select Sname, Mname, Price from Store, Menu, Storemenu Where Mid=S_mid And Sid=Ssid", n, y)+"\n");
                            bw.flush();*/
                    		String name=json_read.getString("data");
                    		String[] n= {"Sid", "Sname", "Mname", "Price"}; 
                    	    Boolean[] y= {false, true, true, false}; 
                    	    bw.write(db.SelectTable("Select Sid, Sname, Mname, Price from Store, Menu, Storemenu Where Mname like \"%"+name+"%\" And Mid=S_mid And Sid=Ssid", n, y)+"\n");
                    	    //bw.write(db.SelectTable("Select Sid, Sname, Mname, Price from FFF2", n, y)+"\n");
                    	    bw.flush();
                    	}else if(x.equals("login")) {                 
                    		String account=json_read.getString("Account");
                    		String password=json_read.getString("Password");
                    		String[] n= {"Password"};
                    		Boolean[] y= {true};
                    		ArrayList<ArrayList<String>> tmp=db.SelectTable2("Select Password from User Where Account=\""+account+"\"", n, y);
                    		json_write=new JSONObject();
                    		if(db.SelectNum()==0) {                  			
                    			json_write.put("Checklogin", false);    
                    			System.out.println("�甇文董���");
                    		}else {
                    			if(password.equals(tmp.get(0).get(0))) {
                        			json_write.put("Checklogin", true);   
                        			System.out.println("撣喳�迤蝣�");
                    			}else {
                        			json_write.put("Checklogin", false);
                        			System.out.println("�甇文�Ⅳ");
                    			}
                    		}
                    		bw.write(json_write+"\n");
                			bw.flush();
                    	}else if(x.equals("Signup")) {
                    		String account=json_read.getString("Saccount");
                    		String name=json_read.getString("Name");
                    		String password=json_read.getString("Spassword");
                    		String mail=json_read.getString("Email");
                    		String uname=json_read.getString("Uname");
                    		String phone=json_read.getString("Uphone");
                    		System.out.println(account);
                    		System.out.println(name);
                    		System.out.println(password);
                    		System.out.println(mail);
                    		System.out.println(uname);
                    		System.out.println(phone);
                    		//String account="asdfff"; String name="123"; String password="123"; String mail="tyuffi"; String uname="123"; String phone="123";
                    		json_write=new JSONObject();
                    		String tmp=db.insertTable(account, name, password, mail, uname, phone);
                    		//System.out.println("tmp: "+tmp);
                    		if(tmp.indexOf("Account")!=-1) {
                    			json_write.put("check", false);
                    			json_write.put("data", "��董��歇蝬�犖雿輻");
                    		}else if(tmp.indexOf("Mail")!=-1) {
                    			json_write.put("check", false);
                    			json_write.put("data", "靽∠拳撌脰◤閮餃��");
                    		}else {
                    			json_write.put("check", true);
                    		}
                    		bw.write(json_write+"\n");
                			bw.flush();
                    	}else if(x.equals("Random")) {
                    		String n[]= {"Sid", "Address"};
                    		Boolean y[]= {false, true};
                    		ArrayList<ArrayList<String>> tmp=db.SelectTable2("Select Sid, Address From Store Order By Rand()", n, y); //Select Sid, Address From Store  Order By Rand() Limit 100
                    		ArrayList<ArrayList<String>> tmp2;
                    		double lon=json_read.getDouble("Longitude");
                    		double lat=json_read.getDouble("Latitude");
                    		double dis=json_read.getDouble("Distlimit");
                    		int typ=json_read.getInt("Eatype");
                    		JSONArray dont=json_read.getJSONArray("Dontwant");
                    		String n1[]= {"Sname", "Address", "Mname", "Price", "Kind1"};
                    		Boolean y1[]= {true, true, true, true, true, true};
                    		//ArrayList<String> dont=(ArrayList<String>)json_read.get("Dontwant");
                    		json_write=new JSONObject();
                    		
                    		String s="";
                    		switch(typ) {
                    			case 1:
                    				s="Having (Kind1 like \"%����%\" Or Kind1 like \"%����%\") ";
                    				break;
                    			case 2:
                    				s="Having Kind1 like \"%�擗�%\" ";
                    				break;
                    			case 3:
                    				s="Having Kind1 like \"%暺��%\" ";
                    				break;
                    		}
                    		for(int i=0;i<dont.length();i++) {
                    			s+="And Kind1 not like \"%"+dont.get(i).toString()+"%\" ";
                    		}
                    		//System.out.println("�撌望��蝬楝摨�: "+lat+","+lon);
                    		//System.out.println("����: "+dis);
                    		for(int i=0;i<tmp.size();i++) {	 
                    			String ss=tmp.get(i).get(1);
                    			//System.out.println("���: "+ss);
                				double[] cal=getGPFromAddress(ss);
                				//System.out.println("閰脣��楝摨�: "+cal[0]+","+cal[1]);
                				//System.out.println("�頝�: "+Distance(cal[0], cal[1], lat, lon));
                				if(Distance(cal[0], cal[1], lat, lon)<=dis) {
                					int id=Integer.parseInt(tmp.get(i).get(0));     					
                					String sql="Select Sname, Address, Mname, Price, group_concat(Kkind) as Kind1 from Store, Menu, Storemenu, Menukind, Kind Where Sid="+id+" And Mid=S_mid And Sid=Ssid And K_mid=Mid And M_kid=Kid group by Mname, Price "+s+"Order by Rand() Limit 1";
                					System.out.println("sql: "+sql);
                					tmp2=db.SelectTable2(sql, n1, y1);
                					if(db.SelectNum()==0) {
                						continue;
                					}else {
                						json_write.put("check", true);
                						json_write.put("data", tmp2);
                						break;
                					}
                				}
                				json_write.put("check", false);
                				json_write.put("data", "蝭��蝚血�����");
                    		}
                    		//System.out.println(json_write.toString());
                    		bw.write(json_write+"\n");
                			bw.flush();
                    	}else if(x.equals("close")) {               	
                    		socketlist.remove(socket);
                    	}
                    	//send("sys", "��蝺���");
                    	//System.out.println(receive("sys"));
                        /*tmp = br.readLine();        //摰�����楨銵�,敺r銝脫������
                        // 憒��蝛箄�
                        if(tmp!=null){
                            //撠���tring���}蝭����
                            tmp=tmp.substring(tmp.indexOf("{"), tmp.lastIndexOf("}") + 1);
                            json_read=new JSONObject(tmp);
                            //敺恥�蝡臬���澆���圾,�雿輻switch�������������
                        }else{    //�甇斗���雿輻雿輻撘瑕���pp��恥�蝡�(����null蝯存erver)
                            //�socket撘瑕���pp��宏�摰Ｘ蝡�
                            socketlist.remove(socket);
                            break;    //頝喳餈游���府�銵��    
                        }*/
                    }
                    System.out.println("撌脫蝺�");
                } catch (Exception e) {
                    e.printStackTrace();
                }    
            }
            public void send(String n, String s) throws JSONException, IOException {              
            	json_write.put(n, URLEncoder.encode(s, "utf-8"));
                bw.write(json_write+"\n");
                bw.flush();
            }
            public String receive(String n) throws JSONException, IOException {
                tmp=br.readLine();
                tmp=tmp.substring(tmp.indexOf("{"), tmp.lastIndexOf("}") + 1);
                json_read=new JSONObject(tmp);
                return URLDecoder.decode(json_read.getString(n), "utf-8");//�銝隞亙���撘�
            }
            //���頧�楝
            public double[] getGPFromAddress(String addr) {
            	try {
        			String tmp = URLEncoder.encode(addr, "UTF-8");       			        			
        			InputStream is = new URL("http://maps.googleapis.com/maps/api/geocode/json?address="+tmp+"&language=zh-tw").openStream();
        			BufferedReader rd = new BufferedReader(new InputStreamReader(is,"utf-8")); //���葉���Ⅳ����
                    StringBuilder sb = new StringBuilder();
                    int cp;
                    while ((cp = rd.read()) != -1) {
                        sb.append((char) cp);
                    }
                    JSONObject json = new JSONObject(sb.toString());
                    JSONArray j2=json.getJSONArray("results");
                    json=j2.getJSONObject(0);
                    json=json.getJSONObject("geometry");
                    json=json.getJSONObject("location");   
                    double r[]= {json.getDouble("lat"), json.getDouble("lng")};
                    //System.out.println(json.getDouble("lat"));
                    //System.out.println(json.getDouble("lng"));
                    return r;
        		} catch (Exception e) {
        			// TODO Auto-generated catch block
        			e.printStackTrace();
        			return null;
        		}
            }
            //撣嗅雿輻��暺�振蝬楝摨血閮�頝
            public double Distance(double longitude1, double latitude1, double longitude2,double latitude2) {
                double radLatitude1 = latitude1 * Math.PI / 180;
                double radLatitude2 = latitude2 * Math.PI / 180;
                double l = radLatitude1 - radLatitude2;
                double p = longitude1 * Math.PI / 180 - longitude2 * Math.PI / 180;
                double distance = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(l / 2), 2)
                        + Math.cos(radLatitude1) * Math.cos(radLatitude2)
                        * Math.pow(Math.sin(p / 2), 2)));
                distance = distance * 6378137.0;
                distance = Math.round(distance * 10000) / 10000;

                return distance ;
            }
        });
        // ���銵��
        t.start();
    }
}
