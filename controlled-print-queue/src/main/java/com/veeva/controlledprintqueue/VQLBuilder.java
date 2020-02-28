/* 
 * Copyright (c) 2018 Veeva Systems Inc.
 * All Rights Reserved.
 * The Controlled-Print-Queue is developed and owned by Veeva Systems Inc.;
 * it is not to be copied, reproduced, or transmitted in any form, by any means, in whole or in part,
 * and may only be used in connection with the deliverable with which it was provided to Customer.
 */
package com.veeva.controlledprintqueue;

import org.apache.commons.lang3.StringUtils;

/** VQL Helper to build VQL statements to populate the form dropdowns
 * @author amol.chinchalkar
 *
 */
public class VQLBuilder {
	
	/** Build the VQL query to retrieve the records of an object or picklist
	 * @param idColumn
	 * The object id column (used as id column in a dropdown)
	 * @param textColumn
	 * The object text column (used as text in dropdown and used to search on)
	 * @param objectName
	 * Name of the object
	 * @param searchText
	 * Search text to be applied
	 * @return
	 * The VQL Statement
	 */
	public String BuildDropdownVQLQuery(String idColumn, String textColumn, String objectName, String searchText) {
		StringBuilder vqlBuilder = new StringBuilder();
		vqlBuilder.append(String.format("SELECT %1$s, %2$s FROM %3$s WHERE status__v='active__v'", idColumn, textColumn, objectName));
		
		if (StringUtils.isNotBlank(searchText)) {
			String sanitizedText = searchText.replace("'", "''").replace("%", "");
			
			if (StringUtils.isNotBlank(sanitizedText)) {
				vqlBuilder.append(" AND caseinsensitive(" + textColumn + ") LIKE '" + sanitizedText + "%'");
			}
		}
		
		vqlBuilder.append(" ORDER BY " + textColumn);
		vqlBuilder.append(" LIMIT 100");
		
		return vqlBuilder.toString();
	}
	
	/**Build the VQL Query to retrieve the user records for a dropdown
	 * @param searchText
	 * Search text to be applied
	 * @return
	 * The VQL Statement
	 */
	public String BuildUserVQLQuery(String searchText) {
		StringBuilder vqlBuilder = new StringBuilder();
		vqlBuilder.append("SELECT id, user_first_name__v, user_last_name__v, user_name__v FROM users WHERE active__v=true");
		
		if (StringUtils.isNotBlank(searchText)) {
			String sanitizedText = searchText.replace("'", "''").replace("%", "");
			
			if (StringUtils.isNotBlank(sanitizedText)) {
				vqlBuilder.append(" AND (user_first_name__v LIKE '" + searchText + "%' OR user_last_name__v LIKE '" + searchText + "%')");
			}
		}
		
		vqlBuilder.append(" ORDER BY user_first_name__v");
		vqlBuilder.append(" LIMIT 50");
		
		return vqlBuilder.toString();
	}
	
	/**
	 * Builds the final VQL Statement for a VQL Filtered dropdown by resolving the any tokens if neccassary
	 * @param baseVQL
	 * The VQL statement from config file (may have tokens)
	 * @param textColumn
	 * The object text column (used as text in dropdown and used to search on)
	 * @param documentName
	 * Vault document name (used if the base VQL contains a @document_name token)
	 * @param documentId
	 * Vault document id (used if the base VQL contains a @document_id token)
	 * @param searchText
	 * Search text to be applied
	 * @return
	 * The token resolved VQL query
	 */
	public String BuildVQLFilteredQuery(String baseVQL, String textColumn, String documentName, int documentId, String searchText) {
		StringBuilder vqlBuilder = new StringBuilder();
		
		// resolve tokens
		String vql = baseVQL.replace("@document_name", "'" + documentName + "'")
						.replace("@document_id", "'" + Integer.toString(documentId) + "'");

		vqlBuilder.append(vql);

		if (StringUtils.isNotBlank(searchText)) {
			
			if (vql.contains(" WHERE ")) {
				vqlBuilder.append(" AND ");
			} else {
				vqlBuilder.append(" WHERE ");
			}
			
			String sanitizedText = searchText.replace("'", "''").replace("%", "");
			
			if (StringUtils.isNotBlank(sanitizedText)) {
				vqlBuilder.append("caseinsensitive(" + textColumn + ") LIKE '" + searchText + "%'");
			}
		}
		
		return vqlBuilder.toString();
	}
}
