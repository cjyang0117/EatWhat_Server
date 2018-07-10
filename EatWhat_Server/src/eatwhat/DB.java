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
	  private Connection con = null; //�s��object 
	  private Statement stat = null; //����,�ǤJ��sql������r�� 
	  private ResultSet rs = null;   //���G�� 
	  private PreparedStatement pst = null; 
	  private int count = 0;
	  //����,�ǤJ��sql���w�x���r��,�ݭn�ǤJ�ܼƤ���m 
	  //���Q��?�Ӱ��Х� 
	  
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
		  //���Udriver 
		  con = DriverManager.getConnection( 
		  "jdbc:mysql://localhost/eatwhat?useUnicode=true&characterEncoding=Big5&useSSL=false", 
		  "root","guangzililab"); 
		  //���oconnection
		 
		      //jdbc:mysql://localhost/test?useUnicode=true&characterEncoding=Big5
		  //localhost�O�D���W,test�Odatabase�W
		  //useUnicode=true&characterEncoding=Big5�ϥΪ��s�X 
		  
		} 
		catch(ClassNotFoundException e){ 
		  System.out.println("DriverClassNotFound :"+e.toString()); 
		}//���i��|����sqlexception 
		catch(SQLException x){ 
		  System.out.println("Exception :"+x.toString()); 
		} 	    
	  } 
	  
	  //�إ�table���覡 
	  //�i�H�ݬ�Statement���ϥΤ覡 
	  /*public void createTable() 
	  { 
	    try 
	    { 
	      stat = con.createStatement(); 
	      stat.executeUpdate(createdbSQL); 
	    } 
	    catch(SQLException e) 
	    { 
	      System.out.println("CreateDB Exception :" + e.toString()); 
	    } 
	    finally 
	    { 
	      Close(); 
	    } 
	  } */
	  
	  //�s�W��� 
	  //�i�H�ݬ�PrepareStatement���ϥΤ覡 
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
	  
	  //�R��Table, 
	  //��إ�table�ܹ� 
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
	  
	  //�d�߸�� 
	  //�i�H�ݬݦ^�ǵ��G���Ψ��o��Ƥ覡 
	  public JSONObject SelectTable(String selectSQL, String[] name, Boolean[] type) throws UnsupportedEncodingException{ 
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
	    		  if(type[i]) {
	    			  tmp[i]=rs.getString(name[i]);
	    		  }else {
	    			  tmp[i]=String.valueOf(rs.getInt(name[i]));
	    		  }
	    		  tmp[i]=URLEncoder.encode(tmp[i], "utf-8");
	    	  }
	    	  json_arr.put(tmp);
	      }
	      pck.put("data", json_arr);	
	    } 
	    catch(SQLException e){ 
	      System.out.println("DropDB Exception :" + e.toString()); 
	    } 
	    finally{
	      Close();	      
	    }
	    return pck;
	  } 
	  public int SelectNum() {
		  return count;
	  }
	  public ArrayList<ArrayList<String>> SelectTable2(String selectSQL, String[] name, Boolean[] type) throws UnsupportedEncodingException{ 
		//String[] tmp=new String[name.length];
		ArrayList<ArrayList<String>> tmp = new ArrayList<ArrayList<String>>();		 
		try{ 
		  stat = con.createStatement(); 
		  rs = stat.executeQuery(selectSQL); 
		  count=0;
		  while(rs.next()) {	  
			  tmp.add(new ArrayList<String>());
			  for(int i=0;i<name.length;i++) {
				  if(type[i]) {
					  //tmp[i]=rs.getString(name[i]);
					  tmp.get(count).add(rs.getString(name[i]));
				  }else {
					  //tmp[i]=String.valueOf(rs.getInt(name[i]));
					  tmp.get(count).add(String.valueOf(rs.getInt(name[i])));
				  }
			  }
			  count++;
		  }	
		} 
		catch(SQLException e){ 
		  System.out.println("DropDB Exception :" + e.toString()); 
		} 
		finally{
		  Close();	      
		}
		return tmp;
	  }
	  //����ϥΧ���Ʈw��,�O�o�n�����Ҧ�Object 
	  //�_�h�b����Timeout��,�i��|��Connection poor�����p 
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
	    //���ݬݬO�_���` 
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
