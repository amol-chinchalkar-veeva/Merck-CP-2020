/* 
 * Copyright (c) 2018 Veeva Systems Inc.
 * All Rights Reserved.
 * The Controlled-Print-Queue is developed and owned by Veeva Systems Inc.;
 * it is not to be copied, reproduced, or transmitted in any form, by any means, in whole or in part,
 * and may only be used in connection with the deliverable with which it was provided to Customer.
 */
package com.veeva.controlledprintqueue;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.veeva.model.PicklistValue;
import com.veeva.model.VaultQueryResult;
import com.veeva.vaultclient.clients.VaultPicklistClient;
import com.veeva.vaultclient.clients.VaultQueryClient100;
import com.veeva.vaultclient.exceptions.VaultClientAuthenticationFailure;
import com.veeva.vaultclient.exceptions.VaultClientDuplicateReferenceException;
import com.veeva.vaultclient.exceptions.VaultClientFailException;
import com.veeva.vaultclient.exceptions.VaultClientParseException;
import com.veeva.vaultclient.exceptions.VaultClientReferencedItemNotFoundException;

/**
 * Manager of the JSON Configuration File that has the UI controls information.
 * 
 * @author amol.chinchalkar
 *
 */

public class JSONConfigManager {

	private JSONParser parser = new JSONParser();
	private VQLBuilder vqlBuilder = new VQLBuilder();
	private VaultQueryClient100 vaultQueryClient;
	private VaultPicklistClient vaultPickListClient;
	private JSONArray copyTypes = null;

	/**
	 * Construct a new instance of the configuration manager
	 * 
	 * @param configLocation      Location of the JSON File
	 * @param vaultQueryClient    Vault API Query Client
	 * @param vaultPicklistClient Vault API Picklist Client
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws ParseException
	 */
	public JSONConfigManager(String configLocation, VaultQueryClient100 vaultQueryClient,
			VaultPicklistClient vaultPicklistClient) throws FileNotFoundException, IOException, ParseException {
		this.vaultQueryClient = vaultQueryClient;
		this.vaultPickListClient = vaultPicklistClient;
		Object obj = parser.parse(new FileReader(configLocation));
		JSONObject jsonConfiguration = (JSONObject) obj;
		copyTypes = (JSONArray) jsonConfiguration.get("CopyTypes");
	}

	/**
	 * Finds the copy type configuration that matches the name requested
	 * 
	 * @param copyType Name of the copy type requested
	 * @return Copy type configuration
	 */
	public JSONObject FindCopyType(String copyType) {
		JSONObject chosenCopyType = null;

		for (int i = 0; i < copyTypes.size(); i++) {
			JSONObject copyTypeConfig = (JSONObject) copyTypes.get(i);
			String currentCopyType = copyTypeConfig.get("CopyType").toString();

			if (copyType.equalsIgnoreCase(currentCopyType)) {
				chosenCopyType = copyTypeConfig;
				break;
			}
		}

		return chosenCopyType;
	}

	/**
	 * Retrieves the LAZY Dropdown data
	 * 
	 * @param copyType   Name of the copy type the dropdown belongs to
	 * @param name       Name of the field to search on
	 * @param searchText Search text to be applied
	 * @return Records that match the search
	 * @throws VaultClientFailException
	 * @throws VaultClientParseException
	 * @throws VaultClientAuthenticationFailure
	 */
	@SuppressWarnings("unchecked")
	public String ProcessLazyDropdown(String copyType, String name, String searchText)
			throws VaultClientFailException, VaultClientParseException, VaultClientAuthenticationFailure {
		JSONObject controlObj = this.GetControlByCopyTypeAndName(copyType, name);
		String idColumn = controlObj.get("IdColumn").toString();
		String textColumn = controlObj.get("TextColumn").toString();
		String vqlQuery = this.vqlBuilder.BuildDropdownVQLQuery(idColumn, textColumn,
				controlObj.get("ObjectName").toString(), searchText);

		VaultQueryResult resultSet = vaultQueryClient.getQueryResults(vqlQuery);
		JSONObject objResults = new JSONObject();
		JSONArray resultArray = new JSONArray();

		for (HashMap<String, Object> result : resultSet.getRowData()) {
			JSONObject itemObject = new JSONObject();
			itemObject.put("id", result.get(idColumn).toString());
			itemObject.put("text", result.get(textColumn).toString());
			resultArray.add(itemObject);
		}

		objResults.put("results", resultArray);

		return objResults.toJSONString();
	}

	/**
	 * Retrieves the VQL Filtered Dropdown data
	 * 
	 * @param copyType     Name of the copy type the dropdown belongs to
	 * @param name         Name of the field to search on
	 * @param documentName Name of the document (Used as a filter in the VQL
	 *                     Statement)
	 * @param documentId   Id of the document (Used as a filter in the VQL
	 *                     Statement)
	 * @param searchText   Search text to be applied
	 * @return Records that match the search
	 * @throws VaultClientFailException
	 * @throws VaultClientParseException
	 * @throws VaultClientAuthenticationFailure
	 */
	@SuppressWarnings("unchecked")
	public String ProcessVQLFilteredDropdown(String copyType, String name, String documentName, int documentId,
			String searchText)
			throws VaultClientFailException, VaultClientParseException, VaultClientAuthenticationFailure {
		JSONObject controlObj = this.GetControlByCopyTypeAndName(copyType, name);

		String idColumn = controlObj.get("IdColumn").toString();
		String textColumn = controlObj.get("TextColumn").toString();
		String baseVQL = controlObj.get("VQL").toString();
		String vqlQuery = this.vqlBuilder.BuildVQLFilteredQuery(baseVQL, textColumn, documentName, documentId,
				searchText);
		VaultQueryResult resultSet = vaultQueryClient.getQueryResults(vqlQuery);
		JSONObject objResults = new JSONObject();
		JSONArray resultArray = new JSONArray();

		for (HashMap<String, Object> result : resultSet.getRowData()) {
			JSONObject itemObject = new JSONObject();
			itemObject.put("id", result.get(idColumn).toString());
			itemObject.put("text", result.get(textColumn).toString());
			resultArray.add(itemObject);
		}

		objResults.put("results", resultArray);

		return objResults.toJSONString();
	}

	/**
	 * Retrieves the Users Dropdown data
	 * 
	 * @param searchText Search text to be applied
	 * @return Records that match the search
	 * @throws VaultClientFailException
	 * @throws VaultClientParseException
	 * @throws VaultClientAuthenticationFailure
	 */
	@SuppressWarnings("unchecked")
	public String ProcessUsersDropdown(String searchText)
			throws VaultClientFailException, VaultClientParseException, VaultClientAuthenticationFailure {

		String vqlQuery = this.vqlBuilder.BuildUserVQLQuery(searchText);
		VaultQueryResult resultSet = vaultQueryClient.getQueryResults(vqlQuery);
		JSONObject objResults = new JSONObject();
		JSONArray resultArray = new JSONArray();

		for (HashMap<String, Object> result : resultSet.getRowData()) {
			JSONObject itemObject = new JSONObject();
			itemObject.put("id", result.get("id").toString());
			itemObject.put("text", String.format("%1$s %2$s (%3$s)", result.get("user_first_name__v").toString(),
					result.get("user_last_name__v").toString(), result.get("user_name__v").toString()));

			resultArray.add(itemObject);
		}

		objResults.put("results", resultArray);

		return objResults.toJSONString();
	}

	/**
	 * Processing (Setup) of the controls data when the form loaded the for the
	 * first time
	 * 
	 * @param copyType Name of the copy type configuration to be loaded
	 * @throws VaultClientFailException
	 * @throws VaultClientParseException
	 * @throws VaultClientAuthenticationFailure
	 * @throws VaultClientDuplicateReferenceException
	 */
	public void ProcessControlsData(String copyType) throws VaultClientFailException, VaultClientParseException,
			VaultClientAuthenticationFailure, VaultClientDuplicateReferenceException {
		JSONArray controls = this.GetControlCollection(copyType);

		for (int i = 0; i < controls.size(); i++) {
			JSONObject control = (JSONObject) controls.get(i);
			String val = control.get("Type").toString();
			FieldType fieldType = FieldType.valueOf(val);

			switch (fieldType) {
			case ObjectDropdown:
				ProcessObjectDropdown(control);
				break;
			case Picklist:
				ProcessPicklist(control);
				break;
			case VQLFilteredDropdown:
				RemoveQuotes(control);
				break;
			case Numeric:
				ProcessNumeric(control);
			default:
				break;
			}
		}
	}

	/**
	 * Processing (Setup) of the numeric text boxes when form is loaded first time
	 * 03-02
	 * @param control
	 */
	@SuppressWarnings("unchecked")
	private void ProcessNumeric(JSONObject control) {
		String controlName = control.get("Name").toString();
		String printMax = control.get("MaxValue").toString();
		int maxInt = Integer.parseInt(printMax);
		// The max number of copies allowed for a CC request is 50 so override the value
		// in the JSON file .
		if (controlName.equals("number_of_copies_in_package__v")) {
			if (maxInt > 50) {
				maxInt = 50;
			}
			control.put("MaxValue", maxInt);
		}
	}

	/**
	 * Remove the single quotes from the VQL Statements
	 * 
	 * @param vqlFilteredDropdown
	 */
	private void RemoveQuotes(JSONObject vqlFilteredDropdown) {
		// Hack to Remove the VQL statement as they could contain quotes and Javascript
		// parser does not like them
		vqlFilteredDropdown.remove("VQL");
	}

	/**
	 * Processing (Setup) of the object type dropdowns when form is loaded first
	 * time
	 * 
	 * @param objectDropdown The object dropdown configuration
	 * @throws VaultClientFailException
	 * @throws VaultClientParseException
	 * @throws VaultClientAuthenticationFailure
	 */
	@SuppressWarnings("unchecked")
	private void ProcessObjectDropdown(JSONObject objectDropdown)
			throws VaultClientFailException, VaultClientParseException, VaultClientAuthenticationFailure {

		String idColumn = objectDropdown.get("IdColumn").toString();
		String textColumn = objectDropdown.get("TextColumn").toString();
		String objectName = objectDropdown.get("ObjectName").toString();
		String vqlStatement = vqlBuilder.BuildDropdownVQLQuery(idColumn, textColumn, objectName, "");

		VaultQueryResult resultSet = vaultQueryClient.getQueryResults(vqlStatement);

		JSONArray items = new JSONArray();
		for (HashMap<String, Object> result : resultSet.getRowData()) {

			JSONObject itemObject = new JSONObject();
			itemObject.put("Id", result.get(idColumn).toString());
			itemObject.put("Name", result.get(textColumn).toString());
			items.add(itemObject);
		}

		// Add the dropdown data to the JSON so it can be rendered by JQuery later
		objectDropdown.put("Items", items);
	}

	/**
	 * Processing (Setup) of the picklist type dropdowns when form is loaded first
	 * time
	 * 
	 * @param picklistDropdown The picklist dropdown configuration
	 * @throws VaultClientReferencedItemNotFoundException
	 * @throws VaultClientFailException
	 * @throws VaultClientParseException
	 * @throws VaultClientAuthenticationFailure
	 * @throws VaultClientDuplicateReferenceException
	 */
	@SuppressWarnings("unchecked")
	private void ProcessPicklist(JSONObject picklistDropdown)
			throws VaultClientReferencedItemNotFoundException, VaultClientFailException, VaultClientParseException,
			VaultClientAuthenticationFailure, VaultClientDuplicateReferenceException {

		String pickListName = picklistDropdown.get("PicklistName").toString();
		List<PicklistValue> pickListValues = vaultPickListClient.getPicklistValues(pickListName);

		JSONArray items = new JSONArray();

		for (PicklistValue picklistValue : pickListValues) {
			JSONObject itemObject = new JSONObject();
			itemObject.put("Id", picklistValue.getName());
			itemObject.put("Name", picklistValue.getLabel());
			items.add(itemObject);
		}

		// Add the dropdown data to the JSON so it can be rendered by JQuery later
		picklistDropdown.put("Items", items);
	}

	/**
	 * Find the control configuration based on the copy type name and control copy
	 * field name
	 * 
	 * @param copyType Name of the copy type the field belongs to
	 * @param name     Field name of the control configuration required
	 * @return The control configuration for the field in the copy type
	 */
	private JSONObject GetControlByCopyTypeAndName(String copyType, String name) {
		JSONObject controlObj = null;
		JSONArray controlCollection = this.GetControlCollection(copyType);

		for (int i = 0; i < controlCollection.size(); i++) {
			controlObj = (JSONObject) controlCollection.get(i);

			if (controlObj.get("Name").toString().equals(name)) {
				break;
			}
		}

		return controlObj;
	}

	/**
	 * Find the control collection for a copy type
	 * 
	 * @param copyTypeName Name of the copy type the control belongs to
	 * @return The collection of controls
	 */
	private JSONArray GetControlCollection(String copyTypeName) {
		JSONObject copyType = this.FindCopyType(copyTypeName);
		JSONArray controls = (JSONArray) copyType.get("Controls");
		return controls;
	}
}
