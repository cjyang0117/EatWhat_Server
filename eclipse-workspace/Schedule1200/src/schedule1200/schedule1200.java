package schedule1200;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import schedule1200.DB;

public class schedule1200 {
	public static void main(String[] args){
		
		
		try {
			DB db =new DB();
			FileOutputStream out;
			out=new FileOutputStream(new File("/eatwhat1/scheduleLog.txt"), true);
			String s=getDateTime()+"\r\n";
			if(db.executeSql("Delete from new_Recommend Where DATEDIFF(NOW(), Rtime)>=15")) {
				s+="OK----->Delete from new_Recommend Where DATEDIFF(NOW(), Rtime)>=15\r\n";
			}else {
				s+="Error----->Delete from new_Recommend Where DATEDIFF(NOW(), Rtime)>=15\r\n";
			}
			String n[]= {"Count"};
			ArrayList<ArrayList<String>> tmp=db.SelectTable2("Select count(Sid) as Count from Store", n);
			if(tmp!=null) {
				s+="OK----->Select count(Sid) as Count from Store\r\n";
				for(int i=0;i<Integer.parseInt(tmp.get(0).get(0));i++) {
					if(db.executeSql("Update Store set Star=(Select AVG(Escore) from Sevaevaluate Where S_sid="+(i+1)+") Where Sid="+(i+1))) {
						s+="OK----->Update Store set Star=(Select AVG(Escore) from Sevaevaluate Where S_sid="+(i+1)+") Where Sid="+(i+1)+"\r\n";
					}else {
						s+="Error----->Update Store set Star=(Select AVG(Escore) from Sevaevaluate Where S_sid="+(i+1)+") Where Sid="+(i+1)+"\r\n";
					}
				}
			}else {
				s+="Error----->Select count(Sid) as Count from Store\r\n";
			}
			out.write(s.getBytes());
			out.close();
			
			/*BufferedReader cmd=new BufferedReader(new InputStreamReader(System.in));
			int beg,end;
			boolean cpt=true;
			System.out.println("請輸入檔案開始編號:");
			beg = Integer.parseInt(cmd.readLine());
			System.out.println("請輸入檔案結束編號:");
			end = Integer.parseInt(cmd.readLine());
			
			DB db =new DB();
			FileOutputStream out;
			out=new FileOutputStream(new File("/eatwhat1/newStoreLog.txt"), true);
			String s=getDateTime()+"\r\n";
			for(int i=beg;i<=end;i++) {
				FileInputStream in=new FileInputStream("/eatwhat1/Store/"+i+".txt");
				BufferedReader br=new BufferedReader(new InputStreamReader(in));
				String tmp=br.readLine();
				String[] store=new String[5];
				int idx=0;
				for(int j=0;j<4;j++) {
					idx=tmp.indexOf(",");
					if(j==0) {
						tmp=tmp.substring(idx+1);
						idx=tmp.indexOf(",");
					}
					store[j]=tmp.substring(0,idx);
					tmp=tmp.substring(idx+1);
				}
				store[4]=tmp;
				if(db.executeSql("Insert into Store(Sname, Address, Sphone, lat, lng, Star) Values(\""+store[0]+"\",\""+store[1]+"\",\""+store[2]+"\",\""+store[3]+"\",\""+store[4]+"\",5)")) {
					s+="OK---->Insert into Store(Sname, Address, Sphone, lat, lng, Star) Values(\""+store[0]+"\",\""+store[1]+"\",\""+store[2]+"\",\""+store[3]+"\",\""+store[4]+"\",5)\r\n";
				}else {
					s+="Error---->Insert into Store(Sname, Address, Sphone, lat, lng, Star) Values(\""+store[0]+"\",\""+store[1]+"\",\""+store[2]+"\",\""+store[3]+"\",\""+store[4]+"\",5)\r\n";
					cpt=false;
				}
				String sid=db.SelectTable2("Select Sid from Store where Address=\""+store[1]+"\"", new String[]{"Sid"}).get(0).get(0).toString();
				
				String[] menu=new String[4];
				menu[2]=sid;
				tmp=br.readLine();
				while(tmp!=null) {
					for(int j=0;j<2;j++) {
						idx=tmp.indexOf(",");
						menu[j]=tmp.substring(0,idx);
						tmp=tmp.substring(idx+1);
					}
					int p=Integer.parseInt(menu[1]);
					menu[3]=pScore(p);
					if(db.executeSql("Insert into new_Menu(Mname, Price, Ssid, PV) Values(\""+menu[0]+"\","+menu[1]+","+menu[2]+","+menu[3]+")")) {
						s+="OK---->Insert into new_Menu(Mname, Price, Ssid, PV) Values(\""+menu[0]+"\","+menu[1]+","+menu[2]+","+menu[3]+")\r\n";
					}else {
						s+="Error---->Insert into new_Menu(Mname, Price, Ssid, PV) Values(\""+menu[0]+"\","+menu[1]+","+menu[2]+","+menu[3]+")\r\n";
						cpt=false;
					}
					
					String mid=db.SelectTable2("Select Mid from new_Menu where Mname=\""+menu[0]+"\" And Ssid="+sid, new String[] {"Mid"}).get(0).get(0).toString();
					while(tmp.contains(",")) {
						idx=tmp.indexOf(",");
						String kid=tmp.substring(0,idx);
						tmp=tmp.substring(idx+1);
						if(db.executeSql("Insert into new_Menukind Values("+mid+","+kid+")")) {
							s+="OK---->Insert into new_Menukind Values("+mid+","+kid+")\r\n";
						}else {
							s+="Error---->Insert into new_Menukind Values("+mid+","+kid+")\r\n";
							cpt=false;
						}
					}
					if(db.executeSql("Insert into new_Menukind Values("+mid+","+tmp+")")) {
						s+="OK---->Insert into new_Menukind Values("+mid+","+tmp+")\r\n";
					}else {
						s+="Error---->Insert into new_Menukind Values("+mid+","+tmp+")\r\n";
						cpt=false;
					}
					tmp=br.readLine();
				}
			}
			out.write(s.getBytes());
			out.close();
			if(cpt) System.out.println("新增完成");*/
			
			/*BufferedReader cmd=new BufferedReader(new InputStreamReader(System.in));
			int beg,end;
			boolean cpt=false;
			System.out.println("請輸入檔案開始編號:");
			beg = Integer.parseInt(cmd.readLine());
			System.out.println("請輸入檔案結束編號:");
			end = Integer.parseInt(cmd.readLine());
			
			DB db =new DB();
			for(int i=beg;i<=end;i++) {
				FileInputStream in=new FileInputStream("/eatwhat1/time/"+i+".txt");
				BufferedReader br=new BufferedReader(new InputStreamReader(in));
				String tmp=br.readLine();
				int id=Integer.parseInt(tmp.substring(tmp.indexOf(",")+1));
				while(true) {
					tmp=br.readLine();
					int day=day(tmp.substring(2));
					while(true) {
						tmp=br.readLine();
						if(tmp!=null) {
							if(tmp.contains("–")) {
								String open,close;
								open=tmp.substring(0, tmp.indexOf("–"))+":00";
								close=tmp.substring(tmp.indexOf("–")+1)+":00";
								if(db.executeSql("Insert into Business(Business_id,Day_id,Open_hour,Close_hour) Values("+id+","+day+",\""+open+"\",\""+close+"\")")) {
									System.out.println("OK--->Insert into Business(Business_id,Day_id,Open_hour,Close_hour) Values("+id+","+day+",\""+open+"\",\""+close+"\")");
								}else {
									System.out.println("Error--->Insert into Business(Business_id,Day_id,Open_hour,Close_hour) Values("+id+","+day+",\""+open+"\",\""+close+"\")");
								}
							}else if(tmp.contains("休息")) {
								br.readLine();
								break;
							}else {
								break;
							}
						}else {
							cpt=true;
							break;
						}
					}
					if(cpt) {
						cpt=true;
						System.out.println("OK---->ID: "+id);
						break;
					}
				}
			}*/
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public static int day(String s) {
		int d=0;
		switch(s) {
			case "一":
				d=1;
				break;
			case "二":
				d=2;
				break;
			case "三":
				d=3;
				break;
			case "四":
				d=4;
				break;
			case "五":
				d=5;
				break;
			case "六":
				d=6;
				break;
			case "日":
				d=7;
				break;
		}
		return d;
	}
	public static String pScore(int p) {
		String ps=null;
		if(p<=40) {
			ps="1";
		}else if(p>40&&p<61) {
			ps="0.9";
		}else if(p>60&&p<81) {
			ps="0.8";
		}else if(p>80&p<101) {
			ps="0.7";
		}else if(p>100&&p<121) {
			ps="0.6";
		}else if(p>120&&p<141) {
			ps="0.5";
		}else if(p>140&&p<161) {
			ps="0.4";
		}else if(p>160&&p<181) {
			ps="0.3";
		}else if(p>180&&p<201) {
			ps="0.2";
		}else if(p>200) {
			ps="0.1";
		}
		return ps;
	}
	//取得今天日期
    public static String getDateTime(){
    	SimpleDateFormat sdFormat = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss");
    	Date date = new Date();
    	String strDate = sdFormat.format(date);
    	System.out.println(strDate);
    	return strDate;
    }
}
