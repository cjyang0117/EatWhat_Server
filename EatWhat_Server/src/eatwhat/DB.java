package eatwhat;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.Connection; 
import java.sql.DriverManager; 
import java.sql.PreparedStatement; 
import java.sql.ResultSet; 
import java.sql.SQLException; 
import java.sql.Statement;
import java.util.ArrayList;
import java.net.URLEncoder;

import org.json.JSONArray;
import org.json.JSONObject;

public class DB {
	  private Connection con = null; //連接object 
	  private Statement stat = null; //執行,傳入之sql為完整字串 
	  private ResultSet rs = null;   //結果集 
	  private PreparedStatement pst = null; 
	  private int count = 0;
	  //執行,傳入之sql為預儲之字申,需要傳入變數之位置 
	  //先利用?來做標示 
	  
	  //private String dropdbSQL = "DROP TABLE User "; 
	  /*private String createdbSQL = "CREATE TABLE User (" + 
	    "    id     INTEGER " + 
	    "  , name    VARCHAR(20) " + 
	    "  , passwd  VARCHAR(20))"; 	*/  
	  /*private String insertdbSQL = "insert into User(id,name,passwd) " + 
	      "select ifNULL(max(id),0)+1,?,? FROM User";*/ 	  
	  //private String selectSQL;
	  
	  public DB(){ 
		try { 
		  Class.forName("com.mysql.cj.jdbc.Driver");
		  //url=jdbc:mysql://localhost:3306/es?autoReconnect=true&useUnicode=true&characterEncoding=utf-8&useSSL=false
		  //註冊driver 
		  con = DriverManager.getConnection( 
		  "jdbc:mysql://localhost/eatwhat?useUnicode=true&characterEncoding=Big5&useSSL=false", 
		  "root","guangzililab"); 
		  //取得connection
		 
		      //jdbc:mysql://localhost/test?useUnicode=true&characterEncoding=Big5
		  //localhost是主機名,test是database名
		  //useUnicode=true&characterEncoding=Big5使用的編碼 
		  
		} 
		catch(ClassNotFoundException e){ 
		  System.out.println("DriverClassNotFound :"+e.toString()); 
		}//有可能會產生sqlexception 
		catch(SQLException x){ 
		  System.out.println("Exception :"+x.toString()); 
		} 	    
	  } 
	  
	  //建立table的方式 
	  //可以看看Statement的使用方式 
	  public void createTable(String sql) 
	  { 
	    try 
	    { 
	      stat = con.createStatement(); 
	      stat.executeUpdate(sql); 
	    } 
	    catch(SQLException e) 
	    { 
	      System.out.println("CreateDB Exception :" + e.toString()); 
	    } 
	    finally 
	    { 
	      Close(); 
	    } 
	  } 
	  
	  //新增資料 
	  //可以看看PrepareStatement的使用方式 
	  public String insertTable( String a,String n, String p, String m, String um, String ph) 
	  { 
	    try 
	    { 
	      pst = con.prepareStatement("insert into User(Account, Name, Password, Mail, Uname, Uphone) Values(?,?,?,?,?,?)"); 
	      pst.setString(1, a); 
	      pst.setString(2, n);
	      pst.setString(3, p);
	      pst.setString(4, m);
	      pst.setString(5, um);
	      pst.setString(6, ph);
	      pst.executeUpdate();
	      return "";
	    } 
	    catch(SQLException e) 
	    { 
	      System.out.println("InsertDB Exception :" + e.toString());
	      return e.toString();
	    } 
	    finally 
	    { 
	      Close(); 
	    } 
	  }
	  
	  //刪除Table, 
	  //跟建立table很像 
	  /*public void dropTable() 
	  { 
	    try 
	    { 
	      stat = con.createStatement(); 
	      stat.executeUpdate(dropdbSQL); 
	    } 
	    catch(SQLException e) 
	    { 
	      System.out.println("DropDB Exception :" + e.toString()); 
	    } 
	    finally 
	    { 
	      Close(); 
	    } 
	  } */
	  
	  //查詢資料 
	  //可以看看回傳結果集及取得資料方式 
	  public JSONObject SelectTable(String selectSQL, String[] name) throws UnsupportedEncodingException{ 
	    JSONObject pck=new JSONObject();
	    JSONArray json_arr=new JSONArray();  
		
	    try{ 
	      stat = con.createStatement(); 
	      rs = stat.executeQuery(selectSQL); 
	      count=0;
	      while(rs.next()) {
	    	  count++;
	    	  String[] tmp=new String[name.length];
	    	  for(int i=0;i<name.length;i++) {
	    		  tmp[i]=rs.getString(name[i]);
	    		  tmp[i]=URLEncoder.encode(tmp[i], "utf-8");
	    	  }
	    	  json_arr.put(tmp);
	      }
	      pck.put("data", json_arr);	
	    } 
	    catch(SQLException e){ 
	      System.out.println("SelectTable Exception :" + e.toString()); 
	      pck.put("data", e.toString());
	    } 
	    finally{
	      Close();	      
	    }
	    return pck;
	  } 
	  public int SelectNum() {
		  return count;
	  }
	  public ArrayList<ArrayList<String>> SelectTable2(String selectSQL, String[] name) throws UnsupportedEncodingException{ 
		ArrayList<ArrayList<String>> tmp = new ArrayList<ArrayList<String>>();		 
		try{ 
		  stat = con.createStatement(); 
		  rs = stat.executeQuery(selectSQL); 
		  count=0;
		  while(rs.next()) {	  
			  tmp.add(new ArrayList<String>());
			  for(int i=0;i<name.length;i++) {
				  tmp.get(count).add(rs.getString(name[i]));
			  }
			  count++;
		  }	
		} 
		catch(SQLException e){ 
		  System.out.println("SelectTable2 Exception :" + e.toString()); 
		} 
		finally{
		  Close();	      
		}
		return tmp;
	  }
	  //完整使用完資料庫後,記得要關閉所有Object 
	  //否則在等待Timeout時,可能會有Connection poor的狀況 
	  private void Close(){ 
	    try{ 
	      if(rs!=null){ 
	        rs.close(); 
	        rs = null; 
	      } 
	      if(stat!=null){ 
	        stat.close(); 
	        stat = null; 
	      } 
	      if(pst!=null){ 
	        pst.close(); 
	        pst = null; 
	      } 
	    } 
	    catch(SQLException e){ 
	      System.out.println("Close Exception :" + e.toString()); 
	    } 
	  }	
	
	/*public static void main(String[] args){
		System.out.println("hello");
	    //測看看是否正常 
	    DB test = new DB(); 
	    String[] x= {"Sid", "Sname", "Address", "Sphone"};
	    Boolean[] y= {false, true, true, true};
	    String tp=test.SelectTable("Select *from Store", x, y).toString();
	    tp=tp.substring(tp.indexOf("{"), tp.lastIndexOf("}") + 1);
	    System.out.println(tp);
	    //test.dropTable(); 
	    //test.createTable(); 
	    //test.insertTable("yku", "12356"); 
	    //test.insertTable("yku2", "7890"); 
	    //test.SelectTable(); 
	}*/
}