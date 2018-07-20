package schedule1200;

import java.io.File;
import java.io.FileOutputStream;
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
			if(db.executeSql("Delete from Recommend Where DATEDIFF(NOW(), Rtime)=2")) {
				s+="OK----->Delete from Recommend Where DATEDIFF(NOW(), Rtime)=2\r\n";
			}else {
				s+="Error----->Delete from Recommend Where DATEDIFF(NOW(), Rtime)=2\r\n";
			}
			String n[]= {"Count"};
			ArrayList<ArrayList<String>> tmp=db.SelectTable2("Select count(Sid) as Count from Store", n);
			if(tmp!=null) {
				s+="OK----->Select count(Sid) as Count from Store\r\n";
				for(int i=0;i<Integer.parseInt(tmp.get(0).get(0));i++) {
					if(db.executeSql("Update Store set Star=(Select AVG(Escore) from Sevaevaluatetest Where S_sid="+(i+1)+") Where Sid="+(i+1))) {
						s+="OK----->Update Store set Star=(Select AVG(Escore) from Sevaevaluatetest Where S_sid="+(i+1)+") Where Sid="+(i+1)+"\r\n";
					}else {
						s+="Error----->Update Store set Star=(Select AVG(Escore) from Sevaevaluatetest Where S_sid="+(i+1)+") Where Sid="+(i+1)+"\r\n";
					}
				}
			}else {
				s+="Error----->Select count(Sid) as Count from Store\r\n";
			}
			out.write(s.getBytes());
			out.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
