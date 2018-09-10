package eatwhat;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
            
            new Thread(new Runnable() {
                @Override
                public void run() {
                	BufferedReader cmd=new BufferedReader(new InputStreamReader(System.in));
                    String str;
                    System.out.println("*********************************"); 
            		System.out.println("**     <<EatWhat  Server>>     **"); 
            		System.out.println("*********************************"); 
            		System.out.println("**    0.剔除 1.人數 2.結束     **"); 
            		//System.out.println("**  4. 修改   5.瀏覽    6.結束 **"); 
            		System.out.println("*********************************"); 
                	while (true){
                		System.out.print("\r\n請輸入指令編號: ");
                		int res;
                		try {
							str = cmd.readLine(); System.out.println();
							res = Integer.parseInt(str);
						} catch (Exception e) {
							res=-1;
							/*System.out.println("*********************************"); 
	                		System.out.println("**     <<EatWhat  Server>>     **"); 
	                		System.out.println("*********************************"); 
	                		System.out.println("**    0.剔除 1.人數 2.結束     **"); 
	                		//System.out.println("**  4. 修改   5.瀏覽    6.結束 **"); 
	                		System.out.println("*********************************");
							this.run();*/
						}
                		
                    	switch(res) {
							case 0:
								System.out.println("維修中");
								/*System.out.println("請輸入剔除編號:");
								str = cmd.readLine();
								socketlist.get(Integer.parseInt(str)).close();
	                            socketlist.remove(Integer.parseInt(str));*/
	                            
								break;
							case 1:
								System.out.println("目前連線數: "+socketlist.size());
								break;
							case 2:
								System.out.println("伺服器關閉中......");
								DB db=new DB();
								db.executeSql("Update User set Online=false");
								System.exit(0);
								break;
							default :
								System.out.println("*********************************"); 
		                		System.out.println("**     <<EatWhat  Server>>     **"); 
		                		System.out.println("*********************************"); 
		                		System.out.println("**    0.剔除 1.人數 2.結束     **"); 
		                		//System.out.println("**  4. 修改   5.瀏覽    6.結束 **"); 
		                		System.out.println("*********************************"); 
								break;
                    	}
                    }
                }
            }).start();
            
            //當Server運作中時
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
                    Thread.sleep(2000);
                    //System.out.println("目前連線數"+socketlist.size());
                    for(int i=0; i<socketlist.size();i++){ //Socket close:socketlist
                        if(isServerClose(socketlist.get(i))) {        //當該客戶端網路斷線時,從SocketList剔除
                        	//System.out.println("關閉Socket");
                        	//socketlist.get(i).close();
                            socketlist.remove(i);
                            i-=1;
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
            socket.sendUrgentData(0xFF);        //發送一個字節的緊急數據,默認情況下是沒有開啟緊急數據處理,不影響正常連線
            return false;                    //如正常則回傳false
        }catch(Exception e){
            return true;                     //如連線中斷則回傳true
        }  
    }  
    // 等待接受客戶端連接
    public static void waitNewSocket() {
        try {
            Socket socket = serverSocket.accept();
            socket.setSoTimeout(1800*1000);
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
        	BufferedReader br;
        	JSONObject json_read,json_write;
        	DB db;
        	String sql1, sql2;
        	String[] re=new String[3];
        	int qcount=0;
        	String UserId;
        	int recmdTime;
        	int nowStoreId;
        	boolean isComment=false;
        	boolean isCon=false;
        	boolean isSend=true;
        	String[] signData;
        	//int timer=0;
            @Override
            public void run() { //Server剛啟動 App端要不到資料! 應該是解決了
            	
                try {
                    // 增加新的使用者
                    socketlist.add(socket);
                    //取得網路輸出串流
                    bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                    // 取得網路輸入串流
                    br = new BufferedReader(new InputStreamReader(socket.getInputStream()));                                   
                    // 當Socket已連接時連續執行
                    db=new DB(); 
                	send("sys", "連線成功");
                    while (socket.isConnected()) { 
                    	String x=receive("action");
                    	if(x!=null) {
	                    	//System.out.println(x);
	                    	json_write=new JSONObject();
	                    	if(x.equals("show")) {	              
	                    		String name=json_read.getString("data");
	                    		double lon=json_read.getDouble("Longitude");
	                    		double lat=json_read.getDouble("Latitude");
	                    		ArrayList<ArrayList<String>> tmp=db.SelectTable2("Select Sid, Sname, Star, lat, lng from Store where Sname like \"%"+name+"%\"", new String[] {"Sid", "Sname", "Star", "lat", "lng"});
	                    		if(tmp!=null) {
	                    			for(int i=0;i<db.SelectNum();i++) {
	                    				tmp.get(i).add(String.valueOf(Distance(Double.parseDouble(tmp.get(i).get(3)), Double.parseDouble(tmp.get(i).get(4)), lat, lon)));
	                    				tmp.get(i).remove(3);
	                    				tmp.get(i).remove(3);
	                    			}
	                    			json_write.put("check", true);
	                    			json_write.put("data", tmp);
	                    		}else {
	                    			json_write.put("check", false);
	                    			json_write.put("data", "查無資料");
	                    		}
	                    	}else if(x.equals("show2")) {
	                    		String name=json_read.getString("data"); if(name.equals("a")) name="";
	                    		ArrayList<ArrayList<String>> tmp=db.SelectTable2("Select Sid, Mid, Sname, Mname, Price from Store, new_Menu Where Mname like \"%"+name+"%\" And Sid=Ssid", new String[] {"Sid", "Mid", "Sname", "Mname", "Price"});
	                    		if(tmp!=null) {
	                    			json_write.put("check", true);
	                    			json_write.put("data", tmp);
	                    		}else {
	                    			json_write.put("check", false);
	                    			json_write.put("data", "查無資料");
	                    		}
	                    	}else if(x.equals("Store")) {
	                    		nowStoreId=json_read.getInt("Id");
	                    		json_write.put("Menu", db.SelectTable2("Select Mname, Price, Mid from Store, new_Menu Where Sid="+nowStoreId+" And Sid=Ssid", new String[] {"Mname", "Price", "Mid"}));
	                    		json_write.put("Evaluation", db.SelectTable2("Select Uname, Evaluation, DATE_FORMAT(Etime,'%Y/%c/%e') as t from Sevaevaluate, User, Store Where Sid="+nowStoreId+" And S_uid=Uid And S_sid=Sid Order by Etime DESC", new String[] {"Uname", "Evaluation", "t"}));
	                    		json_write.put("Time", db.SelectTable2("Select DAYOFWEEK.name, Open_hour, Close_hour from Business, DAYOFWEEK where Day_id=DAYOFWEEK.id And Business_id="+nowStoreId+" Order by Day_id", new String[] {"DAYOFWEEK.name", "Open_hour", "Close_hour"}));
	                    		ArrayList<ArrayList<String>> tmp=db.SelectTable2("Select Evaluation, Escore, DATE_FORMAT(Etime,'%Y/%c/%e') as t from Sevaevaluate Where S_uid="+UserId+" And S_sid="+nowStoreId, new String[] {"Evaluation", "Escore", "t"});
	                    		if(db.SelectNum()!=0) {
	                    			json_write.put("check", true);
	                    			json_write.put("myEvaluation", tmp);
	                    			isComment=true;
	                    		}else {
	                    			json_write.put("check", false);
	                    			isComment=false;
	                    		}
	                    	}else if(x.equals("Store2")) {
	                    		nowStoreId=json_read.getInt("Id");
	                    		json_write.put("Store", db.SelectTable2("Select Sname, Address, Sphone, Star from Store Where Sid="+nowStoreId, new String[] {"Sname", "Address", "Sphone", "Star"}));
	                    		json_write.put("Menu", db.SelectTable2("Select Mname, Price, Mid from Store, new_Menu Where Sid="+nowStoreId+" And Sid=Ssid", new String[] {"Mname", "Price", "Mid"}));
	                    		json_write.put("Evaluation", db.SelectTable2("Select Uname, Evaluation, DATE_FORMAT(Etime,'%Y/%c/%e') as t from Sevaevaluate, User, Store Where Sid="+nowStoreId+" And S_uid=Uid And S_sid=Sid Order by Etime DESC", new String[] {"Uname", "Evaluation", "t"}));
	                    		json_write.put("Time", db.SelectTable2("Select DAYOFWEEK.name, Open_hour, Close_hour from Business, DAYOFWEEK where Day_id=DAYOFWEEK.id And Business_id="+nowStoreId+" Order by Day_id", new String[] {"DAYOFWEEK.name", "Open_hour", "Close_hour"}));
	                    		ArrayList<ArrayList<String>> tmp=db.SelectTable2("Select Evaluation, Escore, DATE_FORMAT(Etime,'%Y/%c/%e') as t from Sevaevaluate Where S_uid="+UserId+" And S_sid="+nowStoreId, new String[] {"Evaluation", "Escore", "t"});
	                    		if(db.SelectNum()!=0) {
	                    			json_write.put("check", true);
	                    			json_write.put("myEvaluation", tmp);
	                    			isComment=true;
	                    		}else {
	                    			json_write.put("check", false);
	                    			isComment=false;
	                    		}            		
	                    	}else if(x.equals("login")) {    //登入放在這裡 會不會被客戶端修改程式碼入侵             
	                    		String a=json_read.getString("Account");
	                    		String p=json_read.getString("Password");
	                    		ArrayList<ArrayList<String>> tmp=db.SelectTable2("Select Uid, Password, Online from User Where Account=\""+a+"\"", new String[] {"Uid", "Password", "Online"});
	                    		if(db.SelectNum()==0) {                  			
	                    			json_write.put("Checklogin", false);
	                    			json_write.put("data", "帳號或密碼錯誤");
	                    			//System.out.println("無此帳號");
            						bw.write(json_write+"\n"); bw.flush();
	                    			break;
	                    		}else {
	                    			if(p.equals(tmp.get(0).get(1))) {
	                    				if(Integer.parseInt(tmp.get(0).get(2))==0) {
		                        			json_write.put("Checklogin", true);   
		                        			UserId=tmp.get(0).get(0); //if(Integer.parseInt(UserId)==246) socket.setSoTimeout(10*1000);
		                        			db.executeSql("Update User set Online=true Where Uid="+UserId);
		                        			db.executeSql("Update User set Actived=true Where Uid="+UserId);
		                        			
		                        			db.SelectTable2("Select R_uid from new_Recommend Where DATEDIFF(NOW(), Rtime)=0 And R_uid="+UserId, new String[] {"R_uid"});
		                        			recmdTime=db.SelectNum();
		                        			json_write.put("recmdTime", recmdTime);
		                        			json_write.put("cnum", Integer.parseInt(db.SelectTable2("Select COUNT(Uid) as c from Utal where Uid="+UserId+" And Score>=0.5", new String[] {"c"}).get(0).get(0).toString()));
		                        			json_write.put("mail", db.SelectTable2("Select Mail from User Where Uid="+UserId, new String[] {"Mail"}).get(0).get(0).toString());
		                        			json_write.put("name", db.SelectTable2("Select Uname from User Where Uid="+UserId, new String[] {"Uname"}).get(0).get(0).toString());
	                    				}else {
	                    					json_write.put("Checklogin", false);
	                    					json_write.put("data", "帳號已登入");
	                    					//System.out.println("帳號已登入");
	                    					bw.write(json_write+"\n"); bw.flush();
	                    					break;
	                    				}
	                    			}else {
	                        			json_write.put("Checklogin", false);
	                        			json_write.put("data", "帳號或密碼錯誤");
	                        			//System.out.println("無此密碼");
	                        			bw.write(json_write+"\n"); bw.flush();
	                        			break;
	                    			}
	                    		}
	                    	}else if(x.equals("Signup")) {
	                    		JSONArray j=json_read.getJSONArray("signData");
	                    		ArrayList<ArrayList<String>> tmp=db.SelectTable2("Select Uid from User Where Account=\""+j.get(0).toString()+"\"", new String[] {"Uid"});
	                    		ArrayList<ArrayList<String>> tmp2=db.SelectTable2("Select Uid from User Where Mail=\""+j.get(3).toString()+"\"", new String[] {"Uid"});
	                    		ArrayList<ArrayList<String>> tmp3=db.SelectTable2("Select Uid from User Where Uname=\""+j.get(4).toString()+"\"", new String[] {"Uid"});
	                    		if(tmp!=null) {
	                    			json_write.put("check", false);
	                    			json_write.put("data", "這個帳號已經有人使用");
	                    		}else if(tmp2!=null) {
	                    			json_write.put("check", false);
	                    			json_write.put("data", "信箱已被註冊");
	                    		}else if(tmp3!=null) {
	                    			json_write.put("check", false);
	                    			json_write.put("data", "暱稱已有人使用");
	                    		}else {
	                    			json_write.put("check", true);
	                    			
	                    			signData=new String[6];
	                    			for(int i=0;i<signData.length;i++) {
	                    				signData[i]=j.get(i).toString();
	                    			}
	                    		}
	                    	}else if(x.equals("Signup2")) {
	                    		db.insertTable(signData);
	                    		ArrayList<ArrayList<String>> tmp=db.SelectTable2("Select Uid from User Where Account=\""+signData[0]+"\"", new String[] {"Uid"});
	                    		UserId=tmp.get(0).get(0).toString();
	                    		db.executeSql("Insert into Uprefer(U_uid, U_kid) Select Uid, Kid from User, Kind Where Uid="+UserId);
	                    		db.executeSql("Insert into Utal(Uid, U_mid) Select Uid, K_mid from User, new_Menukind Where M_kid=2 And Uid="+UserId);
	                    		
	                    		int kid[]= {9,15,16,18,19,25,30,36};
	                    		JSONArray score=json_read.getJSONArray("score");
	                    		for(int j=0;j<kid.length;j++) {
	                    			db.executeSql("Update Uprefer set Seltime="+score.get(j).toString()+" Where U_kid="+kid[j]+" And U_uid="+UserId);
	                    		}
	                    		
	                    		JSONArray weight=json_read.getJSONArray("weight");
	                    		db.executeSql("Update User set Wp="+weight.get(0).toString()+", Wc="+weight.get(1).toString()+", Wl="+weight.get(2).toString()+" Where Uid="+UserId);
	                    		json_write.put("check", true);
	                    	}else if(x.equals("reset")) {
	                    		JSONArray weight=json_read.getJSONArray("weight");
	                    		if(db.executeSql("Update User set Wp="+weight.get(0).toString()+", Wc="+weight.get(1).toString()+", Wl="+weight.get(2).toString()+" Where Uid="+UserId)) {
	                    			json_write.put("check", true);
	                    		}else {
	                    			json_write.put("check", false);
	                    			json_write.put("data", "權重更新失敗");
	                    		}
                    		}else if(x.equals("weight")) {
                    			json_write.put("data", db.SelectTable2("Select Wp, Wc, Wl from User where Uid="+UserId, new String[] {"Wp", "Wc", "Wl"}));
                    		}else if(x.equals("Random")) {
	                    		db.executeSql("Drop table IF EXISTS Kind2");
	                    		boolean isTime=json_read.getBoolean("isTime");
	                    		String sql;
	                    		if(isTime){
	                    			sql="Select Sid, lng, lat From Store where Open=true Order By Rand()";
	                    		}else {
	                    			sql="Select Sid, lng, lat From Store Order By Rand()";
	                    		}
	                    		ArrayList<ArrayList<String>> tmp=db.SelectTable2(sql, new String[] {"Sid", "lng", "lat"});
	                    		
	                    		if(tmp!=null) {
		                    		ArrayList<ArrayList<String>> tmp2;
		                    		double lon=json_read.getDouble("Longitude");
		                    		double lat=json_read.getDouble("Latitude");
		                    		double dis=json_read.getDouble("Distlimit");
		                    		int typ=json_read.getInt("Eatype");
		                    		JSONArray dont=json_read.getJSONArray("Dontwant");
		                    		
		                    		db.executeSql("CREATE TEMPORARY TABLE Kind2 (Kid INT(6) NOT NULL AUTO_INCREMENT, Prefer INT(6) default 0, PRIMARY KEY(Kid)) ENGINE=INNODB DEFAULT CHARSET=utf8");
		                			db.executeSql("Insert into Kind2 (Kid) Select Kid from Kind");
		                			db.executeSql("Update Kind2 set Prefer=1 Where Kid="+typ);	//1早餐 2正餐 33點心
		                    		
		                			if(Integer.parseInt(dont.get(0).toString())!=-1) {
		                				sql="Update Kind2 set Prefer=Prefer-1 Where Kid IN(";
			                    		for(int i=0;i<dont.length();i++) {
			                    			sql+=Integer.parseInt(dont.get(i).toString());
			                    			if(i!=dont.length()-1) {
			                    				sql+=", ";
			                    			}else {
			                    				sql+=")";
			                    			}
			                    		}
			                    		db.executeSql(sql);
		                			}
		                    		
		                    		Double tmpD;
		                    		for(int i=0;i<tmp.size();i++) {	 //取符合距離範圍的店家，且該店家有符合需求之菜色，然後隨機取一道
		                    			tmpD=Distance(Double.parseDouble(tmp.get(i).get(2)), Double.parseDouble(tmp.get(i).get(1)), lat, lon);
		                    			if(tmpD<=dis) {
		                					int id=Integer.parseInt(tmp.get(i).get(0));   
		                					sql="Select Sid, Sname, Address, Sphone, Star, Mid, Mname, Price, SUM(Prefer) as P from Store, new_Menu, new_Menukind, Kind2 where K_mid=Mid And M_kid=Kid And Sid=Ssid And Sid="+id+" group by Sid, Mid Having P=1 Order by Rand() Limit 1";
		                					tmp2=db.SelectTable2(sql, new String[] {"Sid", "Sname", "Address", "Sphone", "Star", "Mid", "Mname", "Price", "P"});
		                					if(db.SelectNum()!=0) {
		                						json_write.put("check", true);
		                						json_write.put("data", tmp2);
		                						break;
		                					}
		                				}
		                    			if(i==tmp.size()-1) {
		                    				json_write.put("check", false);
		                    				json_write.put("data", "範圍內無符合之料理");
		                    			}
		                    		}
	                    		}else {
	                    			json_write.put("check", false);
                    				json_write.put("data", "查無資料");
	                    		}
	                       	}else if(x.equals("Question2")) {
	                      		JSONArray like=json_read.getJSONArray("Like");
	                      		JSONArray normal=json_read.getJSONArray("Soso");
	                      		JSONArray nlike=json_read.getJSONArray("Dont");
	                    		boolean qfirst=json_read.getBoolean("First");
	                    		if(qfirst) {
	                    			db.executeSql("Drop table IF EXISTS Kind2"); 
	                    			sql1=""; sql2=""; qcount=0;
	                    			double lon=json_read.getDouble("Longitude");
	                        		double lat=json_read.getDouble("Latitude");
	                        		double dis=json_read.getDouble("Distlimit");
	                        		int typ=json_read.getInt("Eatype");
	                        		
	                    			boolean isTime=json_read.getBoolean("isTime");
		                    		String sql;
		                    		if(isTime){
		                    			sql="Select Sid, lng, lat From Store where Open=true Order By Rand()";
		                    		}else {
		                    			sql="Select Sid, lng, lat From Store Order By Rand()";
		                    		}
		                    		ArrayList<ArrayList<String>> tmp=db.SelectTable2(sql, new String[] {"Sid", "lng", "lat"});
	                        		
		                    		if(tmp!=null) {
		                        		int[] id=new int[30];
		                        		for(int i=0;i<tmp.size();i++) { //取符合距離範圍的店家最多30筆
		                        			Double ttt=Distance(Double.parseDouble(tmp.get(i).get(2)), Double.parseDouble(tmp.get(i).get(1)), lat, lon);
		                        			if(ttt<=dis) {	
		                        				id[qcount]=Integer.parseInt(tmp.get(i).get(0));
		                        				qcount++;                    				
		                        				if(qcount==30) break;
		                        			}
		                        		}
		                        		if(qcount!=0) {
		                        			db.executeSql("CREATE TEMPORARY TABLE Kind2 (Kid INT(6) NOT NULL AUTO_INCREMENT, Prefer INT(6) default 0, PRIMARY KEY(Kid)) ENGINE=INNODB DEFAULT CHARSET=utf8");
		                        			db.executeSql("Insert into Kind2 (Kid) Select Kid from Kind");
		                        			sql1="Select Sid, Sname, Address, Sphone, Star, Mid, Mname, Price, SUM(Prefer) as P from Store, new_Menu, new_Menukind, Kind2 where Sid IN(";
		                        			for(int i=0;i<qcount;i++) {
		                            			sql1+=id[i];
		                            			if(i!=qcount-1) {
		                            				sql1+=", ";
		                            			}else {
		                            				sql1+=") ";
		                            			}
		                            		}
		                            		sql2="And K_mid=Mid And M_kid=Kid And Sid=Ssid group by Sid, Mid Having P>100 Order by P DESC, Rand() Limit 10";
		                            		
		                            		db.executeSql("Update Kind2 set Prefer=100 where Kid="+typ);
		                        		}
		                    		}
	                    		}else {
	                    			int idx=sql1.indexOf("Mname NOT IN(");
	                    			if(idx!=-1) {
	                    				sql1=sql1.substring(0,sql1.length()-2)+", ";
	                    			}else {
	                    				sql1+="And Mname NOT IN(";
	                    			}
	                    			for(int i=0;i<re.length;i++) {
	                    				sql1+="\""+re[i]+"\" ";
	                    				if(i!=re.length-1) {
	                    					sql1+=", ";
	                    				}else {
	                    					sql1+=") ";
	                    				}
	                    			}
	                    		}
	                    		if(qcount!=0) {
	                    			String sql="";
		                    		if(Integer.parseInt(like.get(0).toString())!=-1) {
		                    			sql="Update Kind2 set Prefer=Prefer+2 where Kid IN(";
		                        		for(int i=0;i<like.length();i++) {
		                        			sql+=Integer.parseInt(like.get(i).toString());
		                        			if(i!=like.length()-1) {
		                        				sql+=", ";
		                        			}else {
		                        				sql+=")";
		                        			}
		                        		}
			                    		db.executeSql(sql);
		                    		}
		                    		if(Integer.parseInt(normal.get(0).toString())!=-1) {
		                    			sql="Update Kind2 set Prefer=Prefer+1 where Kid IN(";
		                        		for(int i=0;i<normal.length();i++) {
		                        			sql+=Integer.parseInt(normal.get(i).toString());
		                        			if(i!=normal.length()-1) {
		                        				sql+=", ";
		                        			}else {
		                        				sql+=")";
		                        			}
		                        		}
			                    		db.executeSql(sql);
		                    		} 
		                    		if(Integer.parseInt(nlike.get(0).toString())!=-1) {
		                    			sql="Update Kind2 set Prefer=-999 where Kid IN(";
		                        		for(int i=0;i<nlike.length();i++) {
		                        			sql+=Integer.parseInt(nlike.get(i).toString());
		                        			if(i!=nlike.length()-1) {
		                        				sql+=", ";
		                        			}else {
		                        				sql+=")";
		                        			}
		                        		}
			                    		db.executeSql(sql);
		                    		} 
		                    		
		                    		ArrayList<ArrayList<String>> tmp=db.SelectTable2(sql1+sql2, new String[] {"Sid", "Sname", "Address", "Sphone", "Star", "Mid", "Mname", "Price", "P"});
		                    		if(tmp!=null) { //解決菜名重複卻屬於不同ID問題
		                    			if(tmp.size()>2) { 
			                    			int j=1; 
				                    		for(int i=0;i<j;i++) {
				                    			if(tmp.get(i).get(6).toString().equals(tmp.get(j).get(6).toString())) {
				                    				tmp.remove(j);
				                    				i=-1;
				                    				if(j!=tmp.size()) {
				                    					continue;
				                    				}else {
				                    					break;
				                    				}
				                    			}else if(i==j-1) {
				                    				i=-1; j++;
				                    			}
				                    			if(j==3) {
				                    				for(int k=j;k<tmp.size();k++) {
				                    					tmp.remove(k);
				                    					k-=1;
				                    				}
				                    				break;
				                    			}
				                    		}
		                    			}
		                    			
		                    			for(int i=0;i<tmp.size();i++) 
			                    			re[i]=tmp.get(i).get(6);
		                    			json_write.put("check", true);
		                    			json_write.put("data", tmp);
		                    		}else {
		                    			json_write.put("check", false);
		                    			json_write.put("data", "查無料理");
		                    		}
	                    		}else {
	                    			json_write.put("check", false);
	                				json_write.put("data", "範圍內無符合之料理");
	                    		}                  		
	                    	}else if(x.equals("User")) {
	                    		boolean isUser=json_read.getBoolean("isUser");
	                    		ArrayList<ArrayList<String>> tmp;
	                    		if(isUser) {
	                    			tmp=db.SelectTable2("Select Uid, Uname, Sid, Sname, Address, Sphone, Star, Mid, Mname, Price from User, Store, new_Menu, new_Recommend Where Uid=R_uid And R_mid=Mid And Sid=Ssid Order by Rtime DESC", new String[] {"Uid", "Uname", "Sid", "Sname", "Address", "Sphone", "Star","Mid", "Mname", "Price"});
	                    		}else {
	                    			tmp=db.SelectTable2("Select Uid, Uname, Sid, Sname, Address, Sphone, Star, Mid, Mname, Price from Store, User, new_Menu, new_Recommend, Usertrack Where Sid=Ssid And Uid=R_uid And R_mid=Mid And Uid=Uid_ed And T_uid="+UserId+" Order by Rtime DESC", new String[] {"Uid", "Uname", "Sid", "Sname", "Address", "Sphone", "Star","Mid", "Mname", "Price"});
	                    		}
	                    		if(tmp!=null) {
	                    			json_write.put("check", true);
	                    			json_write.put("data", tmp);
	                    		}else {
	                    			json_write.put("check", false);
	                    			json_write.put("data", "查無資料");
	                    		}
	                    	}else if(x.equals("Comment")) {
	                    		String Evaluation=json_read.getString("Evaluation");
	                    		float Escore=json_read.getFloat("Escore");
	                    		
	                    		if(isComment) {
	                    			if(db.executeSql("UPDATE Sevaevaluate SET Evaluation=\""+Evaluation+"\", Escore="+Escore+" Where S_sid="+nowStoreId+" And S_uid="+UserId)) {
	                    				json_write.put("check", true);
		                				json_write.put("data", "評論已修改");
	                    			}else {
	                    				json_write.put("check", false);
		                    			json_write.put("data", "評論修改失敗");
	                    			}
	                    		}else {
	                    			if(db.executeSql("Insert into Sevaevaluate(S_sid, S_uid, Evaluation, Escore) Values ("+nowStoreId+", "+UserId+", \""+Evaluation+"\", "+Escore+")")) {
		                    			json_write.put("check", true);
		                				json_write.put("data", "評論已新增");
		                    		}else {
		                    			json_write.put("check", false);
		                    			json_write.put("data", "評論新增失敗");
		                    		}                   			
	                    		}
	                    	}else if(x.equals("Recommend")) {
	                    		if(recmdTime<2) {
	                    			int mid=json_read.getInt("Mid");
	                    			if(db.executeSql("Insert into new_Recommend (R_uid, R_mid) Values("+UserId+","+mid+")")) {
	                    				recmdTime++;
	                    				json_write.put("check", true);
	                    				json_write.put("data", "料理已推薦");
	                    			}else {
	                    				json_write.put("check", false);
	                    				json_write.put("data", "料理已重複推薦");
	                    			}
	                    		}else {
	                    			json_write.put("check", false);
	                    			json_write.put("data", "已達每日推薦次數");
	                    		}
	                    		json_write.put("recmdTime", recmdTime);
	                    	}else if(x.equals("isTrack")) {
	                    		int id=json_read.getInt("Id");
	                    		ArrayList<ArrayList<String>> tmp=db.SelectTable2("Select T_uid from Usertrack Where T_uid="+UserId+" And Uid_ed="+id, new String[] {"T_uid"});
	                    		if(tmp==null) {
	                    			json_write.put("check", false);
	                    		}else {
	                    			json_write.put("check", true);
	                    		}
	                    	}else if(x.equals("Track")) {
	                    		boolean isTrack=json_read.getBoolean("isTrack");
	                    		int id=json_read.getInt("Id");
	                    		if(isTrack) {
	                    			if(db.executeSql("Delete from Usertrack Where T_uid="+UserId+" And Uid_ed="+id)) {
	                    				json_write.put("check", true);
	                    			}else {
	                    				json_write.put("check", false);
	                    			}
	                    		}else {
	                    			if(db.executeSql("Insert into Usertrack Values("+UserId+","+id+")")) {
	                    				json_write.put("check", true);
	                    			}else {
	                    				json_write.put("check", false);
	                    			}
	                    		}
	                    	}else if(x.equals("Content")) {
	                    		int idx=json_read.getInt("idx");
	                    		ArrayList<ArrayList<String>> tmp=db.SelectTable2("Select Sid, Sname, Address, Sphone, Star, Mid, Mname, Price, CAST(Score*100 as signed) as score from Utal, Store, new_Menu Where Uid="+UserId+" And U_mid=Mid And Sid=Ssid And Score>=0.5 Order by Score DESC limit "+idx+",50", new String[] {"Sid", "Sname", "Address", "Sphone", "Star", "Mid", "Mname", "Price", "score"});
	                    		//ArrayList<ArrayList<String>> tmp=db.SelectTable2("Select Sid, Sname, Address, Sphone, Star, Mid, Mname, Price from Utal, Store, new_Menu Where Uid="+UserId+" And U_mid=Mid And Sid=Ssid And Score>=0.5 Order by Score DESC limit "+idx+",50", new String[] {"Sid", "Sname", "Address", "Sphone", "Star", "Mid", "Mname", "Price"});
	                    		
	                    		if(tmp!=null) {
	                    			json_write.put("check", true);
	                    			json_write.put("data", tmp);
	                    		}else {
	                    			json_write.put("check", false);
	                    			json_write.put("data", "Oh沒有資料，明天再來看看吧!");
	                    		}
                    		}else if(x.equals("eat")) {
                    			int mid=json_read.getInt("mid");
                    			if(db.executeSql("Update Uprefer set Seltime=Seltime+1 Where U_uid="+UserId+" And U_kid IN(Select M_kid from new_Menukind Where K_mid="+mid+")")) {
                    				json_write.put("check", true);
                    			}else {
                    				json_write.put("check", false);
                    				json_write.put("data", "紀錄失敗");
                    			}
                    		}else if(x.equals("feedback")) {
                    			String op=json_read.getString("feedback");
                    			if(db.executeSql("Insert into Feedback(Opinion, F_uid) Values(\""+op+"\", "+UserId+")")) {
                    				json_write.put("check", true);
                    			}else {
                    				json_write.put("check", false);
                    				json_write.put("data", "傳送失敗");
                    			}
                    	    }else if(x.equals("useLog")) { //需定期匯出備份
                    	    	int id=json_read.getInt("Fid");
                    	    	db.executeSql("Insert into useLog(L_uid, L_fid) Values("+UserId+", "+id+")");
                    	    	isSend=false;
                    		}else if(x.equals("eatLog")) { //需定期匯出備份
                    			int id=json_read.getInt("Fid");
                    	    	db.executeSql("Insert into eatLog(L_uid, L_fid) Values("+UserId+", "+id+")");
                    	    	isSend=false;
                    		}else if(x.equals("close")) {
	                    		//System.out.println("正常關閉: "+UserId);
	                    		break;
	                    	}
	                    	if(isSend) {
	                    		bw.write(json_write+"\n");
	                    		bw.flush();
	                    	}
	                    	isSend=true;
                    	} else {
                    		//System.out.println("null移除: "+UserId);
                    		break;
                    	}
                    }
                } catch(SocketTimeoutException e) {
                	//System.out.println("超時移除: "+UserId);
                	json_write.put("data", "timeout");
                	try {
						bw.write(json_write+"\n");
						bw.flush();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
            	} catch (SocketException e) {
                    if(!e.toString().contains("Connection reset")) e.printStackTrace();
                } catch (Exception e) {
                	e.printStackTrace();
                } finally {
                	db.executeSql("Update User set Online=false Where Uid="+UserId);
                	try {
                    	socketlist.remove(socket);
                    	br.close(); bw.close(); socket.close();
    				} catch (IOException e) {
    					e.printStackTrace();
    				}
                }
                
            }
            public void send(String n, String s) throws JSONException, IOException { 
            	json_write=new JSONObject();
            	json_write.put(n, URLEncoder.encode(s, "utf-8"));
                bw.write(json_write+"\n");
                bw.flush();
            }
            public String receive(String n) throws JSONException, IOException {
                String tmp=br.readLine();
                if(tmp!=null) {
	                tmp=tmp.substring(tmp.indexOf("{"), tmp.lastIndexOf("}") + 1);
	                json_read=new JSONObject(tmp);
	                return URLDecoder.decode(json_read.getString(n), "utf-8");//可不可以做成判斷式
                }else {
                	return null;
                }
            }
            //地址轉經緯
            public double[] getGPFromAddress(String addr) { //該服務不能一次太頻繁的請求，不然會OVER_QUERY_LIMIT
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
            //取得今天日期
            public String getDateTime(){
            	SimpleDateFormat sdFormat = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss");
            	Date date = new Date();
            	String strDate = sdFormat.format(date);
            	//System.out.println(strDate);
            	return strDate;
            }
        });
        // 啟動執行緒
        t.start();
    }
}
