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
                    System.out.println("目前連線數"+socketlist.size());
                    for(int i=0; i<socketlist.size();i++){ //Socket close:socketlist
                        if(isServerClose(socketlist.get(i))) {        //當該客戶端網路斷線時,從SocketList剔除
                        	System.out.println("關閉Socket");
                        	
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
        	BufferedReader br;
        	String tmp;
        	JSONObject json_read,json_write;
        	DB db;
        	String sql1, sql2;
        	String[] re=new String[3];
        	int qcount=0;
        	String UserId;
        	int recmdTime;
        	int nowStoreId;
        	boolean isComment=false;
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
                    json_write=new JSONObject();
                    db=new DB(); //Server開太久會連不到DB!!???? 可能是之前把宣告DB放在下面 一直宣告的關係  應該沒問題了
                	send("sys", "連線成功");
                    while (socket.isConnected()) { 
                    	String x=receive("action");
                    	System.out.println(x);
                    	if(x.equals("show")) {	//select當指令集合                 
                    		String name=json_read.getString("data");
                    		String[] n= {"Sid", "Sname"};
                    	    //bw.write(db.SelectTable("Select Sid, Sname from Store where Sname like \"%"+name+"%\"", n)+"\n");
                            //bw.flush();
                    		ArrayList<ArrayList<String>> tmp=db.SelectTable2("Select Sid, Sname from Store where Sname like \"%"+name+"%\"", n);
                    		if(tmp!=null) {
                    			json_write.put("check", true);
                    			json_write.put("data", tmp);
                    		}else {
                    			json_write.put("check", false);
                    			json_write.put("data", "查無資料");
                    		}
                    		
                    		bw.write(json_write+"\n");
                			bw.flush();
                    	}else if(x.equals("show1")) {
                    		/*int id=json_read.getInt("data");         
                    		String[] n= {"Mname", "Price"};
                    	    bw.write(db.SelectTable("Select Mname, Price from Store, Menu, Storemenu Where Sid="+id+" And Mid=S_mid And Sid=Ssid", n)+"\n");
                            bw.flush();*/
                    	}else if(x.equals("show2")) {
                    		String name=json_read.getString("data");
                    		String[] n= {"Sid", "Mid", "Sname", "Mname", "Price"}; 
                    	    //bw.write(db.SelectTable("Select Sid, Mid, Sname, Mname, Price from Store, Menu, Storemenu Where Mname like \"%"+name+"%\" And Mid=S_mid And Sid=Ssid", n)+"\n");
                    		//bw.write(db.SelectTable("Select Sid, Mid, Sname, Mname, Price from Store, new_Menu Where Mname like \"%"+name+"%\" And Sid=Ssid", n)+"\n");
                    	    //bw.flush();
                    		ArrayList<ArrayList<String>> tmp=db.SelectTable2("Select Sid, Mid, Sname, Mname, Price from Store, new_Menu Where Mname like \"%"+name+"%\" And Sid=Ssid", n);
                    		if(tmp!=null) {
                    			json_write.put("check", true);
                    			json_write.put("data", tmp);
                    		}else {
                    			json_write.put("check", false);
                    			json_write.put("data", "查無資料");
                    		}
                    		
                    		bw.write(json_write+"\n");
                			bw.flush();
                    	}else if(x.equals("Store")) {
                    		json_write=new JSONObject();
                    		nowStoreId=json_read.getInt("Id");
                    		String[] n= {"Mname", "Price", "Mid"};
                    		//ArrayList<ArrayList<String>> tmp=db.SelectTable2("Select Mname, Price, Mid from Store, Menu, Storemenu Where Sid="+nowStoreId+" And Mid=S_mid And Sid=Ssid", n);
                    		ArrayList<ArrayList<String>> tmp=db.SelectTable2("Select Mname, Price, Mid from Store, new_Menu Where Sid="+nowStoreId+" And Sid=Ssid", n);
                    		json_write.put("Menu", tmp);
                    		String[] n1= {"Uid", "Evaluation"};
                    		//tmp=db.SelectTable2("Select Uid, Evaluation from Sevaevaluatetest, Usertest, Store Where Sid="+nowStoreId+" And S_uid=Uid And S_sid=Sid", n1);
                    		tmp=db.SelectTable2("Select Uid, Evaluation from Sevaevaluate, User, Store Where Sid="+nowStoreId+" And S_uid=Uid And S_sid=Sid", n1);
                    		json_write.put("Evaluation", tmp);
                    		String[] n2= {"Evaluation", "Escore"};
                    		//tmp=db.SelectTable2("Select Evaluation, Escore from Sevaevaluatetest Where S_uid=1 And S_sid="+nowStoreId, n2);
                    		tmp=db.SelectTable2("Select Evaluation, Escore from Sevaevaluate Where S_uid="+UserId+" And S_sid="+nowStoreId, n2);
                    		if(db.SelectNum()!=0) {
                    			json_write.put("check", true);
                    			json_write.put("myEvaluation", tmp);
                    			isComment=true;
                    			System.out.println("成功: Select Evaluation, Escore from Sevaevaluate Where S_uid="+UserId+" And S_sid="+nowStoreId);
                    		}else {
                    			json_write.put("check", false);
                    			isComment=false;
                    			System.out.println("失敗: Select Evaluation, Escore from Sevaevaluate Where S_uid="+UserId+" And S_sid="+nowStoreId);
                    		}
                    		bw.write(json_write+"\n");
                			bw.flush();
                    	}else if(x.equals("Store2")) {
                    		json_write=new JSONObject();
                    		nowStoreId=json_read.getInt("Id");
                    		String[] n0= {"Sname", "Address", "Sphone", "Star"};
                    		ArrayList<ArrayList<String>> tmp=db.SelectTable2("Select Sname, Address, Sphone, Star from Store Where Sid="+nowStoreId, n0);
                    		json_write.put("Store", tmp);
                    		String[] n= {"Mname", "Price", "Mid"};
                    		//tmp=db.SelectTable2("Select Mname, Price, Mid from Store, Menu, Storemenu Where Sid="+nowStoreId+" And Mid=S_mid And Sid=Ssid", n);
                    		tmp=db.SelectTable2("Select Mname, Price, Mid from Store, new_Menu Where Sid="+nowStoreId+" And Sid=Ssid", n);
                    		json_write.put("Menu", tmp);
                    		String[] n1= {"Uid", "Evaluation"};
                    		//tmp=db.SelectTable2("Select Uid, Evaluation from Sevaevaluatetest, Usertest, Store Where Sid="+nowStoreId+" And S_uid=Uid And S_sid=Sid", n1);
                    		tmp=db.SelectTable2("Select Uid, Evaluation from Sevaevaluate, User, Store Where Sid="+nowStoreId+" And S_uid=Uid And S_sid=Sid", n1);
                    		json_write.put("Evaluation", tmp);
                    		String[] n2= {"Evaluation", "Escore"};
                    		//tmp=db.SelectTable2("Select Evaluation, Escore from Sevaevaluatetest Where S_uid=1 And S_sid="+nowStoreId, n2);
                    		tmp=db.SelectTable2("Select Evaluation, Escore from Sevaevaluate Where S_uid="+UserId+" And S_sid="+nowStoreId, n2);
                    		if(db.SelectNum()!=0) {
                    			json_write.put("check", true);
                    			json_write.put("myEvaluation", tmp);
                    			isComment=true;
                    			System.out.println("成功: Select Evaluation, Escore from Sevaevaluate Where S_uid="+UserId+" And S_sid="+nowStoreId);
                    		}else {
                    			json_write.put("check", false);
                    			isComment=false;
                    			System.out.println("失敗: Select Evaluation, Escore from Sevaevaluate Where S_uid="+UserId+" And S_sid="+nowStoreId);
                    		}
                    		bw.write(json_write+"\n");
                			bw.flush();                    		
                    	}else if(x.equals("login")) {    //登入放在這裡 會不會被客戶端修改程式碼入侵             
                    		String a=json_read.getString("Account");
                    		String p=json_read.getString("Password");
                    		String[] n= {"Uid", "Password"};
                    		ArrayList<ArrayList<String>> tmp=db.SelectTable2("Select Uid, Password from User Where Account=\""+a+"\"", n);
                    		json_write=new JSONObject();
                    		if(db.SelectNum()==0) {                  			
                    			json_write.put("Checklogin", false);    
                    			System.out.println("無此帳號");
                    		}else {
                    			if(p.equals(tmp.get(0).get(1))) {
                        			json_write.put("Checklogin", true);   
                        			System.out.println("帳密正確");
                        			
                        			UserId=tmp.get(0).get(0);
                        			String[] n1= {"R_uid"};
                        			db.SelectTable2("Select R_uid from new_Recommend Where R_uid="+UserId, n1);
                        			recmdTime=db.SelectNum();
                        			json_write.put("recmdTime", recmdTime); 
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
                    		json_write=new JSONObject();
                    		String tmp=db.insertTable(account, name, password, mail, uname, phone);
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
                    	/*}else if(x.equals("Random")) {
                    		String n[]= {"Sid", "lng", "lat"};
                    		ArrayList<ArrayList<String>> tmp=db.SelectTable2("Select Sid, lng, lat From Store Order By Rand()", n);
                    		ArrayList<ArrayList<String>> tmp2;
                    		double lon=json_read.getDouble("Longitude");
                    		double lat=json_read.getDouble("Latitude");
                    		double dis=json_read.getDouble("Distlimit");
                    		int typ=json_read.getInt("Eatype");
                    		JSONArray dont=json_read.getJSONArray("Dontwant");
                    		String n1[]= {"Sid", "Sname", "Address","Sphone","Star", "Mid", "Mname", "Price", "Kind1"};
                    		json_write=new JSONObject();
                    		
                    		String s="";
                    		switch(typ) {
                    			case 1:
                    				s="Having Kind1 like \"%正餐%\" ";
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
                    		Double tmpD;
                    		for(int i=0;i<tmp.size();i++) {	 //取符合距離範圍的店家，且該店家有符合需求之菜色，然後隨機取一道
                    			tmpD=Distance(Double.parseDouble(tmp.get(i).get(2)), Double.parseDouble(tmp.get(i).get(1)), lat, lon);
                    			if(tmpD<=dis) {
                					int id=Integer.parseInt(tmp.get(i).get(0));     					
                					//String sql="Select Sid, Sname, Address, Sphone, Star, Mname, Price, group_concat(Kkind) as Kind1 from Store, Menu, Storemenu, Menukind, Kind Where Sid="+id+" And Mid=S_mid And Sid=Ssid And K_mid=Mid And M_kid=Kid group by Mname, Price "+s+"Order by Rand() Limit 1";
                					String sql="Select Sid, Sname, Address, Sphone, Star, Mid, Mname, Price, group_concat(Kkind) as Kind1 from Store, new_Menu, new_Menukind, Kind Where Sid="+id+" And Sid=Ssid And K_mid=Mid And M_kid=Kid group by Mid, Mname, Price "+s+"Order by Rand() Limit 1";
                					System.out.println("sql: "+sql);
                					tmp2=db.SelectTable2(sql, n1);
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
                    		bw.write(json_write+"\n");
                			bw.flush();*/
                    	}else if(x.equals("Random")) {
                    		json_write=new JSONObject();
                    		String n[]= {"Sid", "lng", "lat"};
                    		ArrayList<ArrayList<String>> tmp=db.SelectTable2("Select Sid, lng, lat From Store Order By Rand()", n);
                    		ArrayList<ArrayList<String>> tmp2;
                    		double lon=json_read.getDouble("Longitude");
                    		double lat=json_read.getDouble("Latitude");
                    		double dis=json_read.getDouble("Distlimit");
                    		int typ=json_read.getInt("Eatype");
                    		JSONArray dont=json_read.getJSONArray("Dontwant");
                    		
                    		db.executeSql("CREATE TEMPORARY TABLE Kind2 (Kid INT(6) NOT NULL AUTO_INCREMENT, Prefer INT(6) default 0, PRIMARY KEY(Kid)) ENGINE=INNODB DEFAULT CHARSET=utf8");
                			db.executeSql("Insert into Kind2 (Kid) Select Kid from Kind");
                			db.executeSql("Update Kind2 set Prefer=1 Where Kid="+typ);	//1早餐 2正餐 33點心
                    		
                			String sql="Update Kind2 set Prefer=Prefer-1 Where ";
                    		for(int i=0;i<dont.length();i++) {
                    			sql+="Kid="+Integer.parseInt(dont.get(i).toString())+" ";
                    			if(i!=dont.length()-1) {
                    				sql+="Or ";
                    			}
                    		}
                    		
                    		Double tmpD;
                    		String n1[]= {"Sid", "Sname", "Address", "Sphone", "Star", "Mid", "Mname", "Price", "P"};
                    		for(int i=0;i<tmp.size();i++) {	 //取符合距離範圍的店家，且該店家有符合需求之菜色，然後隨機取一道
                    			tmpD=Distance(Double.parseDouble(tmp.get(i).get(2)), Double.parseDouble(tmp.get(i).get(1)), lat, lon);
                    			if(tmpD<=dis) {
                					int id=Integer.parseInt(tmp.get(i).get(0));   
                					sql="Select Sid, Sname, Address, Sphone, Star, Mid, Mname, Price, SUM(Prefer) as P from Store, new_Menu, new_Menukind, Kind2 where K_mid=Mid And M_kid=Kid And Sid=Ssid And Sid="+id+" group by Sid, Mid Having P=1 Order by Rand() Limit 1";
                					System.out.println("sql: "+sql);
                					tmp2=db.SelectTable2(sql, n1);
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
                    		db.executeSql("Drop table IF EXISTS Kind2");
                    		bw.write(json_write+"\n");
                			bw.flush();                			
                    	/*}else if(x.equals("Question")) { //1.0廢棄
                    		JSONArray like=json_read.getJSONArray("Like");
                    		JSONArray nlike=json_read.getJSONArray("Dont");
                    		boolean qfirst=json_read.getBoolean("First");
                    		if(qfirst) {
                    			sql1=""; sql2=""; 
                    			System.out.println("第1次提問推薦");
                    			String n[]= {"Sid", "Address"};
                        		ArrayList<ArrayList<String>> tmp=db.SelectTable2("Select Sid, Address From Store Order By Rand()", n);
                        		double lon=json_read.getDouble("Longitude");
                        		double lat=json_read.getDouble("Latitude");
                        		double dis=json_read.getDouble("Distlimit");
                        		int count=0;
                        		int[] id=new int[30];
                        		
                        		for(int i=0;i<tmp.size();i++) { //取符合距離範圍的店家最多30筆  
                        			double[] cal=getGPFromAddress(tmp.get(i).get(1)); //經緯度計算方式以修改
                        			if(Distance(cal[0], cal[1], lat, lon)<=dis) {
                        				id[count]=Integer.parseInt(tmp.get(i).get(0));
                        				count++;                    				
                        				if(count==30) break;
                        			}
                        			Thread.sleep(1);
                        		}
                        		System.out.println("符合距離範圍的店家: "+count);
                        		if(count!=0) {
	                        		sql1="Select Sname, Address, Mname, Price, group_concat(Kkind) as Kind1 from Store, Menu, Storemenu, Menukind, Kind Where (";
	                        		for(int i=0;i<count;i++) {
	                        			sql1+="Sid="+id[i];
	                        			if(i!=count-1) {
	                        				sql1+=" Or ";
	                        			}else {
	                        				sql1+=") And Mid=S_mid And Sid=Ssid And K_mid=Mid And M_kid=Kid group by Sname, Address, Mname, Price ";
	                        			}
	                        		}
	                        		int typ=json_read.getInt("Eatype");
	                        		switch(typ) {
		                    			case 1:
		                    				sql1+="Having Kind1 like \"%早餐%\" ";
		                    				break;
		                    			case 2:
		                    				sql1+="Having Kind1 like \"%點心%\" ";
		                    				break;
		                    			case 3:
		                    				sql1+="Having Kind1 like \"%正餐%\" ";
		                    				break;
		                    			case 4:
		                    				sql1+="Having Kind1 like \"%宵夜%\" ";
		                    				break;
	                        		}
	                        		if(!nlike.get(0).toString().equals("false")) {
		                        		for(int i=0;i<nlike.length();i++) {
		    	                			sql1+="And Kind1 not like \"%"+nlike.get(i).toString()+"%\" ";
		    	                		}
	                        		}
	                        		if(!like.get(0).toString().equals("false")) {
		                        		sql2="And (";
		                        		for(int i=0;i<like.length();i++) {
		                        			sql2+="Kind1 like \"%"+like.get(i).toString()+"%\" ";
		                        			if(i!=like.length()-1) {
		    	                				sql2+="Or ";
		    	                			}else {
		    	                				sql2+=") ";
		    	                			}
		                        		}
	                        		}
	                        		String n1[]= {"Sname", "Address", "Mname", "Price", "Kind1"};
	                        		JSONObject jj=db.SelectTable(sql1+sql2+"Order by Rand() Limit 3", n1);
	                        		if(db.SelectNum()!=0) {
		                        		jj.put("check", true);
		                        		bw.write(jj+"\n");
		                        		bw.flush();
		                        		System.out.println("sql: "+sql1+sql2+"Order by Rand() Limit 3");
		                        		System.out.println("jj: "+jj.toString());
	                        		}else {
	                        			json_write.put("check", false);
	                    				json_write.put("data", "範圍內無符合之料理");
	                    				bw.write(json_write+"\n");
		                        		bw.flush();
	                        		}                    		
                        		}else {
                        			json_write.put("check", false);
                    				json_write.put("data", "範圍內無符合之料理");
                    				bw.write(json_write+"\n");
	                        		bw.flush();
                        		}
                    		}else {
                    			if(!nlike.get(0).toString().equals("false")) {
	                        		for(int i=0;i<nlike.length();i++) {
	    	                			sql1+="And Kind1 not like \"%"+nlike.get(i).toString()+"%\" ";
	    	                		}
                    			}
                    			if(!like.get(0).toString().equals("false")) {
	                        		sql2=sql2.substring(0, sql2.indexOf(")"))+"Or ";
	                        		for(int i=0;i<like.length();i++) {
	                        			sql2+="Kind1 like \"%"+like.get(i).toString()+"%\" ";
	                        			if(i!=like.length()-1) {
	    	                				sql2+="Or ";
	    	                			}else {
	    	                				sql2+=") ";
	    	                			}
	                        		}
                    			}
                        		String n1[]= {"Sname", "Address", "Mname", "Price", "Kind1"};
                        		JSONObject jj=db.SelectTable(sql1+sql2+"Order by Rand() Limit 3", n1);
                        		if(db.SelectNum()!=0) {
                        			jj.put("check", true);
	                        		bw.write(jj+"\n");
	                        		bw.flush(); 
	                        		System.out.println("sql: "+sql1+sql2+"Order by Rand() Limit 3");
	                        		System.out.println("jj2: "+jj.toString());
                        		}else {
                        			json_write.put("check", false);
                    				json_write.put("data", "範圍內無符合之料理");
                    				bw.write(json_write+"\n");
	                        		bw.flush();
                        		}
                    		}     */               		                    		                  		
                       	/*}else if(x.equals("Question2")) {
                      		JSONArray like=json_read.getJSONArray("Like");
                      		JSONArray normal=json_read.getJSONArray("Soso");
                      		JSONArray nlike=json_read.getJSONArray("Dont");
                    		boolean qfirst=json_read.getBoolean("First");
                    		if(qfirst) {
                    			db.executeSql("Drop table IF EXISTS Kind2"); 
                    			sql1=""; sql2=""; qcount=0;
                    			System.out.println("第1次提問推薦");
                    			String n[]= {"Sid", "Address", "lng", "lat"};
                        		ArrayList<ArrayList<String>> tmp=db.SelectTable2("Select Sid, Address, lng, lat From Store Order By Rand()", n);
                        		double lon=json_read.getDouble("Longitude");
                        		double lat=json_read.getDouble("Latitude");
                        		double dis=json_read.getDouble("Distlimit");
                        		int[] id=new int[30];
                        		
                        		for(int i=0;i<tmp.size();i++) { //取符合距離範圍的店家最多30筆
                        			Double ttt=Distance(Double.parseDouble(tmp.get(i).get(3)), Double.parseDouble(tmp.get(i).get(2)), lat, lon);
                        			System.out.println("相距: "+ttt);
                        			if(ttt<=dis) {	
                        				id[qcount]=Integer.parseInt(tmp.get(i).get(0));
                        				qcount++;                    				
                        				if(qcount==30) break;
                        			}
                        		}
                        		System.out.println("符合距離範圍的店家: "+qcount);
                        		if(qcount!=0) {
                        			db.executeSql("CREATE TEMPORARY TABLE Kind2 (Kid INT(6) NOT NULL AUTO_INCREMENT, Kkind CHAR(10), Prefer INT(6) default 0, PRIMARY KEY(Kid)) ENGINE=INNODB DEFAULT CHARSET=utf8");
                        			db.executeSql("Insert into Kind2 (Kid, Kkind) Select Kid, Kkind from Kind");
                        			//sql1="Create TEMPORARY TABLE Kind3 As Select Mid, Mname, Price, SUM(Prefer) as P from Store, Storemenu, Menu, Menukind, Kind2 where (";
                        			sql1="Select Sid, Sname, Address, Sphone, Star, Mid, Mname, Price, SUM(Prefer) as P from Store, new_Menu, new_Menukind, Kind2 where (";
                        			for(int i=0;i<qcount;i++) {
                            			sql1+="Sid="+id[i];
                            			if(i!=qcount-1) {
                            				sql1+=" Or ";
                            			}else {
                            				sql1+=") ";
                            			}
                            		}
                            		//sql2="And K_mid=Mid And M_kid=Kid And Mid=S_mid And Sid=Ssid group by Sname, Mid, Price, Mname Having P>100 Order by P DESC, Rand() Limit 10";
                            		sql2="And K_mid=Mid And M_kid=Kid And Sid=Ssid group by Sid, Mid Having P>100 Order by P DESC, Rand() Limit 10";
                            		String sql="Update Kind2 set Prefer=100 where ";
                            		int typ=json_read.getInt("Eatype");
                            		switch(typ) {
	                            		case 1:
		                    				sql+="Kkind=\"早餐\" ";
		                    				break;
		                    			case 2:
		                    				sql+="Kkind=\"點心\" ";
		                    				break;
		                    			case 3:
		                    				sql+="Kkind=\"正餐\" ";
		                    				break;
		                    			case 4:
		                    				sql+="Kkind=\"宵夜\" ";
		                    				break;
                            		}
                            		db.executeSql(sql);
                            		System.out.println(sql);
                        		}
                    		}else {
                    			for(int i=0;i<3;i++) {
                    				sql1+="And Mname<>\""+re[i]+"\" ";
                    			}
                    		}
                    		if(qcount!=0) {
                    			json_write=new JSONObject();
                    			String sql="";
	                    		if(!like.get(0).toString().equals("false")) {
	                    			sql="Update Kind2 set Prefer=Prefer+2 where ";
	                        		for(int i=0;i<like.length();i++) {
	                        			sql+="Kkind=\""+like.get(i).toString()+"\" ";
	                        			if(i!=like.length()-1) {
	                        				sql+="Or ";
	                        			}
	                        		}
	                        		System.out.println(sql);
		                    		db.executeSql(sql);
	                    		}
	                    		if(!normal.get(0).toString().equals("false")) {
	                    			sql="Update Kind2 set Prefer=Prefer+1 where ";
	                        		for(int i=0;i<normal.length();i++) {
	                        			sql+="Kkind=\""+normal.get(i).toString()+"\" ";
	                        			if(i!=normal.length()-1) {
	                        				sql+="Or ";
	                        			}
	                        		}
	                        		System.out.println(sql);
		                    		db.executeSql(sql);
	                    		} 
	                    		if(!nlike.get(0).toString().equals("false")) {
	                    			sql="Update Kind2 set Prefer=-999 where ";
	                        		for(int i=0;i<nlike.length();i++) {
	                        			sql+="Kkind=\""+nlike.get(i).toString()+"\" ";
	                        			if(i!=nlike.length()-1) {
	                        				sql+="Or ";
	                        			}
	                        		}
	                        		System.out.println(sql);
		                    		db.executeSql(sql);
	                    		}        
	                    		//String n1[]= {"Mid", "Mname", "Price", "P"};
	                    		//db.executeSql(sql1+sql2);
	                    		//ArrayList<ArrayList<String>> jj=db.SelectTable2("Select *from Kind3 group by Mid, Mname, Price, P Limit 3", n1);
	                    		String n1[]= {"Sid", "Sname", "Address", "Sphone", "Star", "Mid", "Mname", "Price", "P"};
	                    		ArrayList<ArrayList<String>> tmp=db.SelectTable2(sql1+sql2, n1);
	                    		System.out.println(sql1+sql2);
	                    		
	                    		if(tmp!=null) {
	                    			if(tmp.size()>2) { 
		                    			int j=1; //int gg=0;
			                    		for(int i=0;i<j;i++) {
			                    			if(tmp.get(i).get(6).toString().equals(tmp.get(j).get(6).toString())) {
			                    				//System.out.println(i+": "+tmp.get(i).get(6).toString()+"="+j+": "+tmp.get(j).get(6).toString());
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
			                    		System.out.println(tmp);
	                    			}
	                    			
	                    			for(int i=0;i<tmp.size();i++) 
		                    			re[i]=tmp.get(i).get(6);
	                    			json_write.put("check", true);
	                    			json_write.put("data", tmp);
	                    		}else {
	                    			json_write.put("check", false);
	                    			json_write.put("data", "查無資料");
	                    		}
                    		}else {
                    			json_write.put("check", false);
                				json_write.put("data", "範圍內無符合之料理");
                    		}
                    		bw.write(json_write+"\n");
                    		bw.flush();*/
                       	}else if(x.equals("Question2")) {
                      		JSONArray like=json_read.getJSONArray("Like");
                      		JSONArray normal=json_read.getJSONArray("Soso");
                      		JSONArray nlike=json_read.getJSONArray("Dont");
                    		boolean qfirst=json_read.getBoolean("First");
                    		if(qfirst) {
                    			db.executeSql("Drop table IF EXISTS Kind2"); 
                    			sql1=""; sql2=""; qcount=0;
                    			System.out.println("第1次提問推薦");
                    			String n[]= {"Sid", "Address", "lng", "lat"};
                        		ArrayList<ArrayList<String>> tmp=db.SelectTable2("Select Sid, Address, lng, lat From Store Order By Rand()", n);
                        		double lon=json_read.getDouble("Longitude");
                        		double lat=json_read.getDouble("Latitude");
                        		double dis=json_read.getDouble("Distlimit");
                        		int[] id=new int[30];
                        		
                        		for(int i=0;i<tmp.size();i++) { //取符合距離範圍的店家最多30筆
                        			Double ttt=Distance(Double.parseDouble(tmp.get(i).get(3)), Double.parseDouble(tmp.get(i).get(2)), lat, lon);
                        			System.out.println("相距: "+ttt);
                        			if(ttt<=dis) {	
                        				id[qcount]=Integer.parseInt(tmp.get(i).get(0));
                        				qcount++;                    				
                        				if(qcount==30) break;
                        			}
                        		}
                        		System.out.println("符合距離範圍的店家: "+qcount);
                        		if(qcount!=0) {
                        			db.executeSql("CREATE TEMPORARY TABLE Kind2 (Kid INT(6) NOT NULL AUTO_INCREMENT, Kkind CHAR(10), Prefer INT(6) default 0, PRIMARY KEY(Kid)) ENGINE=INNODB DEFAULT CHARSET=utf8");
                        			db.executeSql("Insert into Kind2 (Kid, Kkind) Select Kid, Kkind from Kind");
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
                            		
                            		int typ=json_read.getInt("Eatype");
                            		db.executeSql("Update Kind2 set Prefer=100 where Kid="+typ);
                            		
                            		
                        		}
                    		}else {
                    			for(int i=0;i<3;i++) {
                    				sql1+="And Mname<>\""+re[i]+"\" ";
                    			}
                    		}
                    		if(qcount!=0) {
                    			json_write=new JSONObject();
                    			String sql="";
	                    		if(!like.get(0).toString().equals("false")) {
	                    			sql="Update Kind2 set Prefer=Prefer+2 where ";
	                        		for(int i=0;i<like.length();i++) {
	                        			sql+="Kkind=\""+like.get(i).toString()+"\" ";
	                        			if(i!=like.length()-1) {
	                        				sql+="Or ";
	                        			}
	                        		}
	                        		System.out.println(sql);
		                    		db.executeSql(sql);
	                    		}
	                    		if(!normal.get(0).toString().equals("false")) {
	                    			sql="Update Kind2 set Prefer=Prefer+1 where ";
	                        		for(int i=0;i<normal.length();i++) {
	                        			sql+="Kkind=\""+normal.get(i).toString()+"\" ";
	                        			if(i!=normal.length()-1) {
	                        				sql+="Or ";
	                        			}
	                        		}
	                        		System.out.println(sql);
		                    		db.executeSql(sql);
	                    		} 
	                    		if(!nlike.get(0).toString().equals("false")) {
	                    			sql="Update Kind2 set Prefer=-999 where ";
	                        		for(int i=0;i<nlike.length();i++) {
	                        			sql+="Kkind=\""+nlike.get(i).toString()+"\" ";
	                        			if(i!=nlike.length()-1) {
	                        				sql+="Or ";
	                        			}
	                        		}
	                        		System.out.println(sql);
		                    		db.executeSql(sql);
	                    		}        
	                    		//String n1[]= {"Mid", "Mname", "Price", "P"};
	                    		//db.executeSql(sql1+sql2);
	                    		//ArrayList<ArrayList<String>> jj=db.SelectTable2("Select *from Kind3 group by Mid, Mname, Price, P Limit 3", n1);
	                    		String n1[]= {"Sid", "Sname", "Address", "Sphone", "Star", "Mid", "Mname", "Price", "P"};
	                    		ArrayList<ArrayList<String>> tmp=db.SelectTable2(sql1+sql2, n1);
	                    		System.out.println(sql1+sql2);
	                    		
	                    		if(tmp!=null) {
	                    			if(tmp.size()>2) { 
		                    			int j=1; //int gg=0;
			                    		for(int i=0;i<j;i++) {
			                    			if(tmp.get(i).get(6).toString().equals(tmp.get(j).get(6).toString())) {
			                    				//System.out.println(i+": "+tmp.get(i).get(6).toString()+"="+j+": "+tmp.get(j).get(6).toString());
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
			                    		System.out.println(tmp);
	                    			}
	                    			
	                    			for(int i=0;i<tmp.size();i++) 
		                    			re[i]=tmp.get(i).get(6);
	                    			json_write.put("check", true);
	                    			json_write.put("data", tmp);
	                    		}else {
	                    			json_write.put("check", false);
	                    			json_write.put("data", "查無資料");
	                    		}
                    		}else {
                    			json_write.put("check", false);
                				json_write.put("data", "範圍內無符合之料理");
                    		}
                    		bw.write(json_write+"\n");
                    		bw.flush();                        		
                    	}else if(x.equals("User")) {
                    		json_write=new JSONObject();
                    		boolean isUser=json_read.getBoolean("isUser");
                    		String[] n= {"Uid", "Uname", "Sid", "Sname", "Address", "Sphone", "Star","Mid", "Mname", "Price"};
                    		ArrayList<ArrayList<String>> tmp;
                    		if(isUser) {
                    			tmp=db.SelectTable2("Select Uid, Uname, Sid, Sname, Address, Sphone, Star, Mid, Mname, Price from User, Store, new_Menu, new_Recommend Where Uid=R_uid And R_mid=Mid And Sid=Ssid", n);
                    			//tmp=db.SelectTable2("Select Uid, Uname, Sid, Sname, Address, Sphone, Star, Mname, Price from User, Store, Menu, Storemenu, new_Recommend Where Uid=R_uid And R_mid=Mid And Sid=Ssid And Mid=S_mid", n);
                    		}else {
                    			tmp=db.SelectTable2("Select Uid, Uname, Sid, Sname, Address, Sphone, Star, Mid, Mname, Price from Store, User, new_Menu, new_Recommend, Usertrack Where Sid=Ssid And Uid=R_uid And R_mid=Mid And Uid=Uid_ed And T_uid="+UserId, n);
                    			//tmp=db.SelectTable2("Select Uid, Uname, Sid, Sname, Address, Sphone, Star, Mname, Price from Store, User, new_Menu, new_Recommend, Usertrack Where Sid=Ssid And Uid=R_uid And R_mid=Mid And Uid=Uid_ed And T_uid=1", n);
                    			//tmp=db.SelectTable2("Select Uid, Uname, Sid, Sname, Address, Sphone, Star, Mname, Price from Store, Storemenu, User, Menu, new_Recommend, Usertrack Where Sid=Ssid And Mid=S_mid And Uid=R_uid And R_mid=Mid And Uid=Uid_ed And T_uid=2", n);
                    		}
                    		if(tmp!=null) {
                    			json_write.put("check", true);
                    			json_write.put("data", tmp);
                    		}else {
                    			json_write.put("check", false);
                    			json_write.put("data", "查無資料");
                    		}
                    		bw.write(json_write+"\n");
                    		bw.flush();
                    	}else if(x.equals("Comment")) {
                    		json_write=new JSONObject();
                    		String Evaluation=json_read.getString("Evaluation");
                    		float Escore=json_read.getFloat("Escore");
                    		
                    		if(isComment) {
                    			//if(db.executeSql("UPDATE Sevaevaluatetest SET Evaluation=\""+Evaluation+"\", Escore="+Escore+" Where S_sid="+nowStoreId+" And S_uid=1")) {
                    			if(db.executeSql("UPDATE Sevaevaluate SET Evaluation=\""+Evaluation+"\", Escore="+Escore+" Where S_sid="+nowStoreId+" And S_uid="+UserId)) {
                    				System.out.println("成功: UPDATE Sevaevaluate SET Evaluation=\""+Evaluation+"\", Escore="+Escore+" Where S_sid="+nowStoreId+" And S_uid="+UserId);
                    				json_write.put("check", true);
	                				json_write.put("data", "評論已修改");
                    			}else {
                    				json_write.put("check", false);
	                    			json_write.put("data", "評論修改失敗");
	                    			System.out.println("失敗: UPDATE Sevaevaluate SET Evaluation=\""+Evaluation+"\", Escore="+Escore+" Where S_sid="+nowStoreId+" And S_uid="+UserId);
                    			}
                    		}else {
	                    		//if(db.executeSql("Insert into Sevaevaluatetest(S_sid, S_uid, Evaluation, Escore) Values ("+nowStoreId+", "+UserId+", \""+Evaluation+"\", "+Escore+")")) {
                    			if(db.executeSql("Insert into Sevaevaluate(S_sid, S_uid, Evaluation, Escore) Values ("+nowStoreId+", "+UserId+", \""+Evaluation+"\", "+Escore+")")) {
	                    			json_write.put("check", true);
	                				json_write.put("data", "評論已新增");
	                				System.out.println("成功: Insert into Sevaevaluate(S_sid, S_uid, Evaluation, Escore) Values ("+nowStoreId+", "+UserId+", \""+Evaluation+"\", "+Escore+")");
	                    		}else {
	                    			json_write.put("check", false);
	                    			json_write.put("data", "評論新增失敗");
	                    			System.out.println("失敗: Insert into Sevaevaluate(S_sid, S_uid, Evaluation, Escore) Values ("+nowStoreId+", "+UserId+", \""+Evaluation+"\", "+Escore+")");
	                    		}                   			
                    		}
                    		bw.write(json_write+"\n");
                    		bw.flush();
                    	}else if(x.equals("Recommend")) {
                    		json_write=new JSONObject();
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
                    		bw.write(json_write+"\n");
                    		bw.flush();
                    	}else if(x.equals("isTrack")) {
                    		json_write=new JSONObject();
                    		int id=json_read.getInt("Id");
                    		String n[]= {"T_uid"};
                    		ArrayList<ArrayList<String>> tmp=db.SelectTable2("Select T_uid from Usertrack Where T_uid="+UserId+" And Uid_ed="+id, n);
                    		if(tmp==null) {
                    			json_write.put("check", false);
                    		}else {
                    			json_write.put("check", true);
                    		}
                    		bw.write(json_write+"\n");
                    		bw.flush();
                    	}else if(x.equals("Track")) {
                    		json_write=new JSONObject();
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
            	System.out.println(strDate);
            	return strDate;
            }
        });
        // 啟動執行緒
        t.start();
    }
}
