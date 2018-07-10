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
    private static Thread th_close;                //執行緒
    private static int serverport = 5050;
    private static ServerSocket serverSocket;    //伺服端的Socket
    private static ArrayList<Socket> socketlist=new ArrayList<Socket>();
    public static void main(String[] args){
        try {
            serverSocket = new ServerSocket(serverport);    //啟動Server開啟Port接口
            System.out.println("Server開始執行");
            th_close=new Thread(Judge_Close);                //賦予執行緒工作(判斷socketlist內有沒有客戶端網路斷線)
            th_close.start();                                //讓執行緒開始執行
            // 當Server運作中時
            while (!serverSocket.isClosed()) {
                // 呼叫等待接受客戶端連接
                waitNewSocket();
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    private static Runnable Judge_Close=new Runnable(){    //讓執行緒每兩秒判斷一次SocketList內是否有客戶端強制斷線
        @Override
        public void run() {                                //在此抓取的是關閉wifi等斷線動作 
            // TODO Auto-generated method stub
            try {
                while(true){
                    Thread.sleep(2001);
                    System.out.println("目前連線數"+socketlist.size());
                    for(int i=0; i<socketlist.size();i++){ //Socket close:socketlist
                        if(isServerClose(socketlist.get(i))) {        //當該客戶端網路斷線時,從SocketList剔除
                        	System.out.println("關閉Socket");
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
    private static Boolean isServerClose(Socket socket){    //判斷連線是否中斷
        try{  
            socket.sendUrgentData(0);        //發送一個字節的緊急數據,默認情況下是沒有開啟緊急數據處理,不影響正常連線
            return false;                    //如正常則回傳false
        }catch(Exception e){
            return true;                     //如連線中斷則回傳true
        }  
    }  
    // 等待接受客戶端連接
    public static void waitNewSocket() {
        try {
            Socket socket = serverSocket.accept();
            System.out.println("連線成功");
            // 呼叫創造新的使用者
            createNewThread(socket);
        } catch (Exception e) {
            e.printStackTrace();
        }
    } 
    // 創造新的使用者
    public static void createNewThread(final Socket socket) {
        // 以新的執行緒來執行
        Thread t = new Thread(new Runnable() {
        	BufferedWriter bw;
			//BufferedWriter bw;
        	BufferedReader br;
        	String tmp;
        	JSONObject json_read,json_write;
        	DB db;
            @Override
            public void run() { //Server剛啟動 App端要不到資料!
                try {
                    // 增加新的使用者
                    socketlist.add(socket);
                    //取得網路輸出串流
                    bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                    // 取得網路輸入串流
                    br = new BufferedReader(new InputStreamReader(socket.getInputStream()));                                   
                    // 當Socket已連接時連續執行
                    json_write=new JSONObject();
                    db=new DB(); //Server開太久會連不到DB!!???? 可能是之前把宣告DB放在下面 一直宣告的關係
                	send("sys", "連線成功");
                    while (socket.isConnected()) { 
                    	String x=receive("action");
                    	System.out.println(x);
                    	if(x.equals("show")) {	//select當指令集合                   	    
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
                    			System.out.println("無此帳號");
                    		}else {
                    			if(password.equals(tmp.get(0).get(0))) {
                        			json_write.put("Checklogin", true);   
                        			System.out.println("帳密正確");
                    			}else {
                        			json_write.put("Checklogin", false);
                        			System.out.println("無此密碼");
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
                    			json_write.put("data", "這個帳號已經有人使用");
                    		}else if(tmp.indexOf("Mail")!=-1) {
                    			json_write.put("check", false);
                    			json_write.put("data", "信箱已被註冊");
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
                    				s="Having (Kind1 like \"%午餐%\" Or Kind1 like \"%晚餐%\") ";
                    				break;
                    			case 2:
                    				s="Having Kind1 like \"%早餐%\" ";
                    				break;
                    			case 3:
                    				s="Having Kind1 like \"%點心%\" ";
                    				break;
                    		}
                    		for(int i=0;i<dont.length();i++) {
                    			s+="And Kind1 not like \"%"+dont.get(i).toString()+"%\" ";
                    		}
                    		//System.out.println("自己所在經緯度: "+lat+","+lon);
                    		//System.out.println("指定距離: "+dis);
                    		for(int i=0;i<tmp.size();i++) {	 
                    			String ss=tmp.get(i).get(1);
                    			//System.out.println("地址: "+ss);
                				double[] cal=getGPFromAddress(ss);
                				//System.out.println("該店經緯度: "+cal[0]+","+cal[1]);
                				//System.out.println("相距: "+Distance(cal[0], cal[1], lat, lon));
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
                				json_write.put("data", "範圍內無符合之料理");
                    		}
                    		//System.out.println(json_write.toString());
                    		bw.write(json_write+"\n");
                			bw.flush();
                    	}else if(x.equals("close")) {               	
                    		socketlist.remove(socket);
                    	}
                    	//send("sys", "連線成功");
                    	//System.out.println(receive("sys"));
                        /*tmp = br.readLine();        //宣告一個緩衝,從br串流讀取值
                        // 如果不是空訊息
                        if(tmp!=null){
                            //將取到的String抓取{}範圍資料
                            tmp=tmp.substring(tmp.indexOf("{"), tmp.lastIndexOf("}") + 1);
                            json_read=new JSONObject(tmp);
                            //從客戶端取得值後做拆解,可使用switch做不同動作的處理與回應
                        }else{    //在此抓取的是使用使用強制關閉app的客戶端(會不斷傳null給server)
                            //當socket強制關閉app時移除客戶端
                            socketlist.remove(socket);
                            break;    //跳出迴圈結束該執行緒    
                        }*/
                    }
                    System.out.println("已斷線");
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
                return URLDecoder.decode(json_read.getString(n), "utf-8");//可不可以做成判斷式
            }
            //地址轉經緯
            public double[] getGPFromAddress(String addr) {
            	try {
        			String tmp = URLEncoder.encode(addr, "UTF-8");       			        			
        			InputStream is = new URL("http://maps.googleapis.com/maps/api/geocode/json?address="+tmp+"&language=zh-tw").openStream();
        			BufferedReader rd = new BufferedReader(new InputStreamReader(is,"utf-8")); //避免中文亂碼問題
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
            //帶入使用者及景點店家經緯度可計算出距離
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
        // 啟動執行緒
        t.start();
    }
}
