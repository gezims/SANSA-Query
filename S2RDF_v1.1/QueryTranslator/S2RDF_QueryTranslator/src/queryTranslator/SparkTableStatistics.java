/* Copyright Simon Skilevic
 * Master Thesis for Chair of Databases and Information Systems
 * Uni Freiburg
 */
package queryTranslator;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;

import queryTranslator.sql.SqlStatement;

/**
 * This static class manages table statistics generated by creation of ExtVP model  
 * @author Simon Skilevic
 *
 */
public class SparkTableStatistics {
	public static int numberOfStoredTables = 0;
	
	// This map contains 
	// as keys place holder names in SQL query
	// as values to the place holder corresponding SqlTableCandidatesList 
	public static HashMap<String, SqlTableCandidatesList> tableCandidatesMap = new HashMap<String, SqlTableCandidatesList>();
	
	// These maps contain 
	// as keys table names
	// as values to the table corresponding statistics 
	public static HashMap<String, TStat> vpStats = new HashMap<String, TStat>();
	public static HashMap<String, TStat> soStats = new HashMap<String, TStat>();
	public static HashMap<String, TStat> osStats = new HashMap<String, TStat>();
	public static HashMap<String, TStat> ssStats = new HashMap<String, TStat>();
	
	/**
	 * Read statistics from corresponding statistics files
	 */
	public static void init(){
		long storedTriples=0;
		
		storedTriples += readVPStatistic();
		storedTriples += readExtVPStatistic("OS");
		storedTriples += readExtVPStatistic("SO");
		storedTriples += readExtVPStatistic("SS");

		System.out.println("THE NUMBER OF ALL SAVED (< ScaleUB) TRIPLES IS -> " + storedTriples);
		System.out.println("THE NUMBER OF ALL SAVED (< ScaleUB) TABLES IS -> " + numberOfStoredTables);
	}
	/**
	 * Compose tableName
 	 * VP <pred1>
	 * ExtVP <pred1><pred2>
	 * @param candidate String{[relType], [pred1], [pred2]}
	 * @return table name
	 */
	private static String composeTableName(String[] candidate){
		String ret="";
		if (candidate[0].equals("VP")){
			ret = ("<"+candidate[1].replace(":", "__")+">").replace("<<", "<").replace(">>", ">");
		} else {
			ret = ("<"
					+candidate[1].replace(":", "__")
					+"><"
					+candidate[2].replace(":", "__")+">").replace("<<", "<").replace(">>", ">");
		}
		return ret;
	}
	/** Get statistic of "best"(smallest) table
	 * 
	 * @param placeHolderName for certain table in SQL query
	 * @return TStat of the best Table
	 */
	public static TStat getBestCandidateTableStatistic(String placeHolderName){
		int bestCandidateID = tableCandidatesMap.get(placeHolderName).bestCandidateID;
		String[] bestCandidate = tableCandidatesMap.get(placeHolderName).getSqlTableCandidates().get(bestCandidateID);

		String tName = composeTableName(bestCandidate);
		
		// Return statistic of the best candidate
		if (bestCandidate[0].equals("SO") && soStats.containsKey(tName)){
			return soStats.get(tName);
		} else if (bestCandidate[0].equals("OS") && osStats.containsKey(tName)){
			return osStats.get(tName);
		} else if (bestCandidate[0].equals("SS") && ssStats.containsKey(tName)){
			return ssStats.get(tName);
		} else if (bestCandidate[0].equals("VP") && vpStats.containsKey(tName)){
			return vpStats.get(tName);
		}

		return null;
	}

	/**
	 * Determine the relation type of two triple patterns
	 * @param elmTP1 = linked triple element(sub/obj) of the main(first) TP
	 * @param elmTP2 = linked triple element(sub/obj) sub/obj of the slave(second) TP
	 * @return relation Type
	 */
	private static String getRelationType(String elmTP1, String elmTP2){
		if (elmTP1.equals("sub") && elmTP2.equals("sub"))
			return "SS";
		else if (elmTP1.equals("obj") && elmTP2.equals("sub"))
			return "OS";
		else if (elmTP1.equals("sub") && elmTP2.equals("obj"))
			return "SO";
		else return "UNDEFINED"; 
	}
	
	/**
	 * Get original table name in the form how it appeared in SQL query
	 * before it was replaced by corresponding place holder
	 * 
	 * By the way, place holder format: 
	 * <original_tablename>$$<place_holder_ID>$$
	 * @param tableName (can be place holder(if already replaced) or original name)
	 * @return Original table name
	 */
	private static String getOriginalTableName(String tableName){
		if (!tableName.contains("$$"))
			return tableName;
		else
			return tableName.substring(0, tableName.indexOf("$$"));
	}
	
	/**
	 * !!!Debugging Output!!!
	 * Prints all candidate tables for all place holder in SQL query 
	 */
	private static void listAllCandidateTables(){
		for (String plcHolder : tableCandidatesMap.keySet()){
			SqlTableCandidatesList candList = tableCandidatesMap.get(plcHolder);
			for (int i=0; i < candList.getSqlTableCandidates().size();i++){
				String[] candidate = candList.getSqlTableCandidates().get(i);
				System.out.println("Name: "+candList.placeHolderName+" Type: "+candidate[0] + " "+candidate[1] + " "+candidate[2] + " ");
			}
		}
	}	
	/**
	 * The function extract TP-relations of SQL subqueries from the joinList and
	 * generates a list of table candidates(ExtVP/VP tables) for every original SQL table, which can
	 * be found in from clause of corresponding subquery.
	 * 
	 * @param list: A list of SQL subqueries objects, which have to be joined
	 */
	public static void generateCandidatesTableLists(ArrayList<SqlStatement> joinList){
		
		// for every subquery determine its relations to the other subqueries
		// and generate a list of candidates based on determined relations.
		for (int i = 0; i < joinList.size(); i++){
			SqlTableCandidatesList candList = new SqlTableCandidatesList(joinList.get(i).getFrom());
			
			// replace from clause of given subquery by the place holder name 
			joinList.get(i).setFrom(candList.placeHolderName);
			
			for (int j=0; j < joinList.size(); j++)
				
				// we do it only for SELECT subqueries due to simplicity 
				if (i!=j && joinList.get(i).getType().equals("Select") && joinList.get(j).getType().equals("Select")){
					
					// maps of column names[keys] and corresponding alias(variables)[values] 
					// for the first and second subqueries
					HashMap<String, String> tr1 = joinList.get(i).getAliasToColumn();
					HashMap<String, String> tr2 = joinList.get(j).getAliasToColumn();
					
					for (String alias1:tr1.keySet()){						
						for (String alias2:tr2.keySet()){
							
							// determine relation Type (SO,OS,SS)
							String relationType = getRelationType(alias1, alias2);
							
							// if two columns correspond to the same variable 
							// then relation exists -> add corresponding ExtVP table  
							// to the candidates list
							if (tr1.get(alias1).equals(tr2.get(alias2)) && !relationType.equals("UNDEFINED")){
								candList.addExtVPCandidateTable(relationType, getOriginalTableName(joinList.get(i).getFrom()), getOriginalTableName(joinList.get(j).getFrom()));									
							}
						}						
					}
					
				}

			// save generated candidates list to the map
			tableCandidatesMap.put(candList.placeHolderName, candList);
		}
		// print all candidates lists
		// TODO: must be probably removed, since it is only interesting for 
		// debugging. The information can be found anyway in output SQL query.
		// listAllCandidateTables();
	}

	/**
	 * Read VP tables statistics
	 * FileFormat:
	 * ...
	 * ------------------------... // Start table statistics
	 * <tableName>\t<VP table size>\t<TT table size>\t<scale(VP/TT)>
	 * ... 
	 * ------------------------... // End table statistics
	 * ... 
	 * @return Number of read Triples
	 */
	private static long readVPStatistic(){
		long allTriplesNumber = 0;
		try
		{
		  BufferedReader reader = new BufferedReader(new FileReader(Tags.VP_TABLE_STAT));
		  String line;
		  while ((line = reader.readLine()) != null && !line.contains("-------------")){}
		  while ((line = reader.readLine()) != null && !line.contains("-------------")){
			  String[] temp = line.split("\t");
			  TStat newStat = new TStat(temp[1], temp[2], temp[3]);
			  allTriplesNumber += newStat.size;
			  numberOfStoredTables++;
			  vpStats.put(temp[0].replace(":", "__").replace("<<", "<").replace(">>", ">"), newStat);
		  }
		  reader.close();
		}
		catch (Exception e)
		{
		  System.err.format("Exception occurred trying to read '%s'.", Tags.VP_TABLE_STAT);
		  e.printStackTrace();
		}
		System.out.println("VP STAT Size = "+vpStats.size());
		//System.out.println(vpStats.get("<mo:conductor>").size+"/"+vpStats.get("<mo:conductor>").sizeSource+" "+vpStats.get("<mo:conductor>").freq);
		return allTriplesNumber;
	}

	/**
	 * Get ExtVP Statistic file destination by relation Type (SO, OS, SS) 
	 * @param relType Relation Type  
	 * @return file destination 
	 */
	private static String getExtVPStatFileDest(String relType){
		if (relType.equals("SO")) return Tags.SO_TABLE_STAT;
		if (relType.equals("OS")) return Tags.OS_TABLE_STAT;
		if (relType.equals("SS")) return Tags.SS_TABLE_STAT;		
		return null;
	}
	/**
	 * Read ExtVP tables statistics
	 * FileFormat:
	 * ...
	 * ------------------------... // Start table statistics
	 * <tableName>\t<ExtVP table size>\t<VP table size>\t<scale(ExtVP/VP)>\t<scale(VP/TT)>
	 * ... 
	 * ------------------------... // End table statistics
	 * ... 
	 * @return Number of read Triples
	 */
	private static long readExtVPStatistic(String relType){
		long allTriplesNumber = 0;
		
		try
		{
		  BufferedReader reader = new BufferedReader(new FileReader(getExtVPStatFileDest(relType)));
		  String line;
		  while ((line = reader.readLine()) != null && !line.contains("-------------")){}
		  while ((line = reader.readLine()) != null && !line.contains("-------------")){
			  String[] temp = line.split("\t");
			  TStat newStat = new TStat(temp[1], temp[2], temp[3]);

			  // count ExtVP tables having Scale < ScaleUB
			  if ((float)newStat.size/(float)newStat.sizeSource < Tags.ScaleUB){
				  allTriplesNumber += newStat.size;
				  numberOfStoredTables++;
			  }

			  // Add ExtVP tables statistics 
			  if (relType.equals("SO")) soStats.put(temp[0].replace(":", "__").replace("<<", "<").replace(">>", ">"), newStat);
			  else if (relType.equals("OS")) osStats.put(temp[0].replace(":", "__").replace("<<", "<").replace(">>", ">"), newStat);
			  else if (relType.equals("SS")) ssStats.put(temp[0].replace(":", "__").replace("<<", "<").replace(">>", ">"), newStat);
		  }
		  reader.close();
		}
		catch (Exception e)
		{
		  System.err.format("Exception occurred trying to read '%s'.", getExtVPStatFileDest(relType));
		  e.printStackTrace();
		}
		
		  if (relType.equals("SO")) System.out.println("SO STAT Size = "+soStats.size());
		  else if (relType.equals("OS")) System.out.println("OS STAT Size = "+osStats.size());
		  else if (relType.equals("SS")) System.out.println("SS STAT Size = "+ssStats.size());

		return allTriplesNumber;
	}

	/**
	 *  Determines id of the best table for every list of candidate tables.
	 *  Best table is a table having smallest number of triples
	 *  Triple numbers of tables can be obtained from read table statistics
	 */
	public static void determineBestCandidateTable(){
		for (String placeHolderName:tableCandidatesMap.keySet()){
			SqlTableCandidatesList candList = tableCandidatesMap.get(placeHolderName);
			long min = Long.MAX_VALUE;
			
			for (int i=0; i < candList.getSqlTableCandidates().size(); i++){
				String[] candidate = candList.getSqlTableCandidates().get(i);
				String tName = composeTableName(candidate);
				System.out.println("TABLE->"+tName);
				if (candidate[0].equals("VP")){ // VP candidate table
					long pSize = vpStats.get(tName).size;
					if (pSize < min){
						min = pSize;
						candList.bestCandidateID = i;
					}
				} else { // ExtVP candidate table					
					long extVpTableSize=0;					
					long vpTableSize = 0;
					
					if (candidate[0].equals("SO")){
						if (!soStats.containsKey(tName)) continue;
						for (String huj:soStats.keySet()) System.out.println("XUJ->"+huj);
						extVpTableSize = soStats.get(tName).size;
						vpTableSize = soStats.get(tName).sizeSource;
						
						// if the size of the extVP table is smaller than actual minimum,
						// and the scale extVP table does not exceed allowed ScaleUB
						// then save the the id of this table as best table id
						if (extVpTableSize < min 
								&& extVpTableSize < vpTableSize * Tags.ScaleUB  
								&& Tags.ALLOW_SO){
							min = extVpTableSize;
							candList.bestCandidateID = i;
						}
					} else if (candidate[0].equals("OS")){
						if (!osStats.containsKey(tName)) continue;
						
						extVpTableSize = osStats.get(tName).size;
						vpTableSize = osStats.get(tName).sizeSource;
						
						if (extVpTableSize < min 
								&& extVpTableSize < vpTableSize * Tags.ScaleUB  
								&& Tags.ALLOW_OS){
							min = extVpTableSize;
							candList.bestCandidateID = i;
						}
					} else if (candidate[0].equals("SS")){
						if (!ssStats.containsKey(tName)) continue;
						
						extVpTableSize = ssStats.get(tName).size;
						vpTableSize = ssStats.get(tName).sizeSource;
						
						if (extVpTableSize < min 
								&& extVpTableSize < vpTableSize * Tags.ScaleUB  
								&& Tags.ALLOW_SS){
							min = extVpTableSize;
							candList.bestCandidateID = i;
						}
					}
				}
			}
		}
	}
	/** Check if the ExtVP table corresponds to an empty relation
	 * Table statistics contains information about all possible ExtVP tables 
	 * (even about tables, which aren't saved) -> if tableName does not occur 
	 * in any statistic then it corresponds to an empty relation
	 * @return true for empty relation or false for non empty
	 */ 
	private static Boolean emptyRelationCheck(String relType, String extVpTableName){
		
		if (relType.equals("SO")){
			if (!soStats.containsKey(extVpTableName)) return true;
		} else if (relType.equals("OS")){
			if (!osStats.containsKey(extVpTableName)) return true;
		} else if (relType.equals("SS")){
			if (!ssStats.containsKey(extVpTableName)) return true;
		}
		
		return false;
	}
	
	/**
	 * Generates a string containing information about the given ExtVP table for the
	 *  table usage instructions.
	 * @param relType
	 * @param extVpTableName
	 * @return 
	 */
	private static String generateExtVPLine(String relType, String extVpTableName){
		long pSize=0;
		float pSel=0;
		
		if (relType.equals("SS") && ssStats.containsKey(extVpTableName)){
			pSize = ssStats.get(extVpTableName).size;
			pSel = ssStats.get(extVpTableName).scale;
		} else if (relType.equals("OS") && osStats.containsKey(extVpTableName)){
			pSize = osStats.get(extVpTableName).size;
			pSel = osStats.get(extVpTableName).scale;
		} else if (relType.equals("SO") && soStats.containsKey(extVpTableName)){
			pSize = soStats.get(extVpTableName).size;
			pSel = soStats.get(extVpTableName).scale;
		}
		
		return "\t" + relType + "\t" + extVpTableName + "\t" + pSize + "\t"+ pSel + "\n";
	}
	/**
	 * Generate tables usage instructions, which gonna be added to the SQL query.
	 * These instructions contain table candidates lists and corresponding 
	 * best tables for any place holder in SQL query. 
	 * 
	 * If the query contains empty relation (relation, which doesn't exist
	 * in the input RDF set), than function returns empty instructions, what 
	 * indicates an empty result list 
	 * @return Table usage instructions as a String
	 */
	public static String generateTablesUsageInstructions(String query){
		String res="++++++Tables Statistic\n"; 
		for (String placeHolderName:tableCandidatesMap.keySet()){
			SqlTableCandidatesList candList = tableCandidatesMap.get(placeHolderName);
			String[] bestCandidate = candList.getSqlTableCandidates().get(candList.bestCandidateID);						
			// add best candidate information
			String newPlaceholder = placeHolderName.replace("<", "_L_").replace(">", "_B_");
			query = query.replace(placeHolderName, newPlaceholder);
			res+=newPlaceholder+"\t"+candList.bestCandidateID+"\t"+bestCandidate[0]
					+ "\t" + bestCandidate[1].replace("<", "_L_").replace(">", "_B_")
					+ "/" + bestCandidate[2].replace("<", "_L_").replace(">", "_B_")
					+"\n";
			
			// add entire list of all candidates for the corresponding place holder   
			for (int i=0; i < candList.getSqlTableCandidates().size(); i++){				
				String[] candidate = candList.getSqlTableCandidates().get(i);
				String relType = candidate[0]; 
				String tName = composeTableName(candidate);
				if (relType.equals("VP")){					
					res+="\tVP\t"+tName +"\t"+ vpStats.get(tName).size+"\n";
				} else {					
					// Empty relation check					
					if (emptyRelationCheck(relType, tName)){
						return "++++++Tables Statistic\n   \n------\n";
					}
					
					res += generateExtVPLine(relType, tName);
				}					
			}
			res+="------\n";
		}
		return query+"\n"+res;
	}
	
}
