import java.util.List;
import java.util.Map;
import java.io.IOException;
import java.util.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.stream.Collectors;
import org.apache.log4j.Logger;
import io.ManageFile;
import logger.MyLog4J;
import rest.RestApi;
import utils.FieldsQuery;
import utils.ListComparator;
import utils.Strings;

public class MainActivity {

   public static Date getLastDate(Date d1,Date d2) {
	   //Comparing dates
	   if(d1.compareTo(d2) > 0) {
	         return d1;
	      } else if(d1.compareTo(d2) < 0) {
	         return d2;
	      } 
	   return d1;
   }
   
   public static Date createNewDate(String date,SimpleDateFormat sdformat) {
	   //split in date
	   String[] split=date.split("-");
	   Date newDate=null;
	   try {
			newDate = sdformat.parse(split[0]+"-"+split[1]+"-"+split[2]);
		} catch (ParseException e) {
			Logger.getLogger(MyLog4J.class).error(e);
		} 
	   return newDate;
   }
 
   public static Map<String,Date>  merge( List<String> lBugs, List<String> lCommits) {
	   //Add in hashmap <ticketId,Date>  if TicketId is in the comment i-esimo,
	   //then take the most recent date of the comment
	   
	   //map idTicket-Date
	   HashMap<String,Date> mapIdDate = new HashMap<>();
	   SimpleDateFormat sdformat = new SimpleDateFormat("yyyy-MM-dd");

	   lBugs.forEach(key -> {
		   for(String word: lCommits) {
			   //Check if the ticket is in the comment
			   if(word.contains(key)) {
				   Date newDate = createNewDate(word,sdformat);
				   
				   //Add item if it doesn't exist
				   if(mapIdDate.get(key)==null) { 
					   mapIdDate.put(key,newDate );
				   }
				   else {
					   //Check if date is last
					   Date oldDate=mapIdDate.get(key);
					   if(getLastDate(oldDate,newDate).equals(newDate))
						   mapIdDate.put(key,newDate );
				   }
			   }
			 }
	   });
	   return mapIdDate;
   }
	
   
   public static List<FieldsQuery> sumTicket(Map<String,Date> map){
	   ArrayList<FieldsQuery> listDateNum = new ArrayList<>();
	   ListComparator comparator =  new ListComparator();
	   map.entrySet().forEach(entry->{
		   boolean exists=false;
   		   Calendar newDate = Calendar.getInstance();
   		   newDate.setTime(entry.getValue());

		   for(int i = 0 ; i!= listDateNum.size();i++) {
			   //if Date,NumTickets exists then increment count
			   if(comparator.compare(listDateNum.get(i),new FieldsQuery(newDate,1))==0) {
				   exists=true;
				   listDateNum.get(i).setCount(listDateNum.get(i).getCount()+1);
				   break;
			   }			      
		   }
		   //if Date,NumTickets does not exist then create and reorder list
		   if (!exists) {
			   listDateNum.add(new FieldsQuery(newDate,1));
			   //order
			   Collections.sort(listDateNum, comparator); 
		   }  
	   });
	   return listDateNum;
   }
   
   public static void removeDuplicateElement(List<String> lBugs) {
       lBugs.stream().distinct().collect(Collectors.toList());
   }
   
   public static void filterFile(List<String> lCommits) {
       lCommits.removeIf((String n) -> (n==null || !n.contains(Strings.KEY)   )   ); 
   }
   
   public static void configureLogger() {
	   MyLog4J.setProperties();
   }
   
   public static List<String> getCommits(String choise) throws IOException {
	   ArrayList<String> lCommits=null;
	   if(choise.equals(Strings.REST)) {
		   lCommits= (ArrayList<String>)RestApi.getCommitsFromGithub();
	   }else if(choise.equals(Strings.FILE)) {
		   lCommits = (ArrayList<String>)ManageFile.readFile();
	   }
	   return lCommits;
   }
   
   public static void main(String[] args) throws IOException {  
	   
	   String choise = Strings.REST;
	   //Configure log
	   configureLogger();

	   //Get bugs
	   ArrayList<String> lBugs = (ArrayList<String>) RestApi.getProjectBug();
	   //Remove probably duplicate key
	   removeDuplicateElement(lBugs);
	   
	   //Get Commits
	   ArrayList<String> lCommits =( ArrayList<String>) getCommits(choise);
	   //Filter comment without id
	   try{
		   filterFile(lCommits);
	   }catch(NullPointerException e) {
		   Logger.getLogger(MyLog4J.class).error(e.getMessage());
	   }
	   
	   System.out.println(lCommits.size());
	   
	   //Create map<IdTicket,Date commit> and get most recent commit by Ticket
	   HashMap<String,Date> mapIdDate = (HashMap<String,Date>) merge(lBugs,lCommits);
	   //Create ArrayList of Date,NumTicket in 1 Month
	   ArrayList<FieldsQuery> listDateNum = (ArrayList<FieldsQuery>)sumTicket(mapIdDate);
	   //Write on csv
	   ManageFile.writeCSVOnFile(listDateNum);  
   }
}
