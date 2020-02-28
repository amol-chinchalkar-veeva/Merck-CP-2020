/* 
 * Copyright (c) 2018 Veeva Systems Inc.
 * All Rights Reserved.
 * The Controlled-Print-Queue is developed and owned by Veeva Systems Inc.;
 * it is not to be copied, reproduced, or transmitted in any form, by any means, in whole or in part,
 * and may only be used in connection with the deliverable with which it was provided to Customer.
 */
package com.veeva.controlledprintqueue;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.veeva.controlledprintqueue.models.ControlCopyRequest;
import com.veeva.model.Authorization;
import com.veeva.model.DocumentRole;
import com.veeva.model.User;
import com.veeva.model.VaultInfo;
import com.veeva.model.VaultObjectID;
import com.veeva.model.VaultQueryResult;
import com.veeva.model.Version;
import com.veeva.model.response.JobResponse;
import com.veeva.vaultclient.clients.VaultDocumentClient183;
import com.veeva.vaultclient.clients.VaultObjectClient173;
import com.veeva.vaultclient.clients.VaultPicklistClient;
import com.veeva.vaultclient.clients.VaultQueryClient100;
import com.veeva.vaultclient.clients.VaultSessionClient;
import com.veeva.vaultclient.clients.VaultUserClient;
import com.veeva.vaultclient.exceptions.VaultClientAuthenticationFailure;

@Controller
@Scope("request")
public class HomeController {

	private Logger logger = LoggerFactory.getLogger(HomeController.class);

	

	/** API Version */
	@Value("${vault.apiversion}")
	private String apiVersion;

	/** Vault Username */
	@Value("${vault.username}")
	private String username;

	/** Vault Password (Encrypted) */
	@Value("${vault.password}")
	private String password;

	/** Vault Proxy */
	@Value("${vault.proxy}")
	private String proxy;

	/** Vault ProxyPort */
	@Value("${vault.proxyPort}")
	private String proxyPort;
	/**
	 * Valid roles allowed to print the documents
	 * 
	 */
	@Value("${vault.validroles}")
	private String validRoles;
	/**
	 * Location of the JSON config file
	 */
	@Value("${vault.configlocation}")
	private String configLocation;

	/**
	 * The maximum number of retries to call the Job API to check if the job is
	 * ready to download.
	 */
	@Value("${vault.maxretries}")
	private int maxRetries;

	/** Email address for request result confirmation page */
	@Value("${print.emailaddress}")
	private String printEmailAddress;

	/** Vault API User Client */
	@Autowired
	@Qualifier("vaultUserClient")
	private VaultUserClient userClient;

	/** Vault API Session Client */
	@Autowired
	@Qualifier("vaultSessionClient")
	private VaultSessionClient sessionClient;

	/** Vault API Document Client */
	@Autowired
	@Qualifier("vaultDocumentClient")
	private VaultDocumentClient183 vaultDocumentClient;

	/** Vault API Query Client */
	@Autowired
	@Qualifier("vaultQueryClient")
	VaultQueryClient100 vaultQueryClient;

	/** Vault API Picklist Client */
	@Autowired
	@Qualifier("vaultPicklistClient")
	VaultPicklistClient vaultPicklistClient;

	/** Vault API Object Client */
	@Autowired
	@Qualifier("vaultObjectClient")
	VaultObjectClient173 vaultObjectClient;

	/** Email Utility to send an Email */
	@Autowired
	EmailUtil emailUtil;
	
	/** Federated ID */
	private String userFederatedId = "";

	/**
	 * Loading of the print request page when the application is first accessed
	 * 
	 * @param modelMap     The model returned to the view
	 * @param sessionId    Vault Session Id of user
	 * @param vaultDomain  Domain of the Vault
	 * @param userId       Vault User Id
	 * @param documentId   Vault Document Id
	 * @param documentName Vault Document Name
	 * @param majorVersion Document Major Version Number
	 * @param minorVersion Document Minor Version Number
	 * @param copyType     The Object Type Name in vault (Technical name e.g.
	 *                     issued_batch_record__c)
	 * @return
	 */
	@RequestMapping(value = "/printrequest", method = RequestMethod.GET)
	public String PrintRequestPage(ModelMap modelMap,
			@RequestParam(name = "SessionId", required = true) String sessionId,
			@RequestParam(name = "VaultDomain", required = true) String vaultDomain,
			@RequestParam(name = "UserId", required = true) long userId,
			@RequestParam(name = "DocumentId", required = true) long documentId,
			@RequestParam(name = "DocumentName", required = true) String documentName,
			@RequestParam(name = "MajorVersion", required = true) int majorVersion,
			@RequestParam(name = "MinorVersion", required = true) int minorVersion,
			@RequestParam(name = "CopyType", required = true) String copyType) {

		String viewName = "controlledprintrequest";

		try {

			logger.info("Page Load Started: " + LocalDateTime.now());

			// set the system variables. We might need to add in JVM directly.
			System.setProperty("http.proxyHost", proxy);
			System.setProperty("http.proxyPort", proxyPort);
			System.setProperty("-Djava.net.preferIPv4Stack", "true");
			System.setProperty("Djdk.tls.client.protocols", "TLSv1.2");

			// verify user is in the valid roles and document has valid print role
			// associated
			Boolean userHasAcess = UserHasAccess(documentId, userId, vaultDomain);
			logger.info("User access check complete");

			// user is allowed to controlled print
			if (userHasAcess) {
				logger.info("Loading controls...");
				// switch session to logged in user session for further processing
				setSession(userId, "", "0", vaultDomain, sessionId);
				// read json configuration file
				JSONConfigManager configManager = new JSONConfigManager(configLocation, vaultQueryClient,
						vaultPicklistClient);

				// set up control data when the form is loaded for the first time
				configManager.ProcessControlsData(copyType);
				JSONObject objCopyType = configManager.FindCopyType(copyType);
				// load the model
				modelMap.addAttribute("data", objCopyType);
				modelMap.addAttribute("sessionId", sessionId);
				modelMap.addAttribute("userId", userId);
				modelMap.addAttribute("vaultDomain", vaultDomain);
				modelMap.addAttribute("copyType", copyType);
				modelMap.addAttribute("docName", documentName);
				modelMap.addAttribute("docId", documentId);
				modelMap.addAttribute("friendlyDocName",
						String.format("%1$s (v%2$s.%3$s)", documentName, majorVersion, minorVersion));
				modelMap.addAttribute("majorVersion", majorVersion);
				modelMap.addAttribute("minorVersion", minorVersion);
				modelMap.addAttribute("actionLabel", objCopyType.get("ActionLabel").toString());

				/// add control copy to the model
				ControlCopyRequest controlledCopyRequest = new ControlCopyRequest();
				modelMap.addAttribute("copyrequest", controlledCopyRequest);

			} else {
				viewName = "errorpage";
				modelMap.addAttribute("msg", "Unauthorised access to this page.");
				logger.error("User:" + Long.toString(userId) + "is not authorsed to view this page.");
			}

		} catch (Exception e) {
			viewName = "errorpage";
			modelMap.addAttribute("msg", e.getMessage());
			logger.error(e.getMessage(), e);
		}

		modelMap.addAttribute("emailAddress", printEmailAddress);
		logger.info("Page Load Ended: " + LocalDateTime.now());
		return viewName;
	}

	/**
	 * The processing of a Print Request
	 * 
	 * @param copyRequest Model that holds the form data related to the copy request
	 * @param vaultDomain Domain of the Vault
	 * @return The Result View
	 */
	@RequestMapping(value = "/result", method = RequestMethod.POST)
	public ModelAndView ResultPage(@ModelAttribute("copyrequest") ControlCopyRequest copyRequest,
			@RequestParam(name = "vaultDomain", required = true) String vaultDomain) {

		String viewName = "result";
		ModelAndView mav = new ModelAndView();
		// populate result success/error page
		mav.addObject("documentName", copyRequest.getDocumentName());
		mav.addObject("emailAddress", printEmailAddress);

		JSONParser parser = new JSONParser();

		try {
			Object obj = parser.parse(copyRequest.getControlText());
			JSONObject jsonObject = (JSONObject) obj;

			// assemble parameter map for the controlled copy request job
			Map<String, String> initiationParameters = new LinkedHashMap<>();
			initiationParameters.put("requested_by__v", Integer.toString(copyRequest.getUserId()));
			initiationParameters.put("user_action_label__v", copyRequest.getActionLabel());
			initiationParameters.put("delivery_method__c", "print__c");
			initiationParameters.put("printed_by__c", Integer.toString(copyRequest.getUserId()));

			// add the printer objects, copy request parameters to job request
			String printerId = jsonObject.get("printer__c").toString();
			for (Object key : jsonObject.keySet()) {
				String keyStr = (String) key;
				Object keyvalue = jsonObject.get(keyStr);

				if (keyvalue != null && StringUtils.isNotBlank(keyvalue.toString())) {
					initiationParameters.put(keyStr, keyvalue.toString());
				}
			}
			// get the document version
			Version version = new Version(copyRequest.getMajorVersion(), copyRequest.getMinorVersion());
			VaultObjectID documentID = new VaultObjectID(copyRequest.getDocumentId());

			// set integration user session to run the job
			setSession(vaultDomain);

			/**
			 * test user id
			 */

			VaultQueryResult resultUser = vaultQueryClient
					.getQueryResults("SELECT federated_id__v FROM users WHERE id =" + copyRequest.getUserId());

			String federatedId = resultUser.getRowData().get(0).get("federated_id__v").toString();

			logger.debug("federated Id: " + federatedId);

			// invoke the control copy job in a lifecycle action
			logger.info("Creating Job for document Id: " + copyRequest.getDocumentId());

			JobResponse response = vaultDocumentClient.invokeJobLifecycleAction(documentID, version,
					copyRequest.getActionLabel(), initiationParameters, false);

			logger.info("Job successfully created. Job Id: " + response.getJobID());
			logger.debug("Job response:" + response.getResponseStatus());

			if (response.getResponseStatus().equals("SUCCESS")) {
				logger.info("Requesting Printer Details for printer Id: " + printerId);
				VaultQueryResult result = vaultQueryClient.getQueryResults(
						"SELECT name__v, printer_folder__c, printer_url__c, printer_name__c FROM printer__c WHERE id = '"
								+ printerId + "'");

				// get folder location to copy
				String printFolderLocation = result.getRowData().get(0).get("printer_folder__c").toString();
				// get host name for the queue
				String printerHostName = result.getRowData().get(0).get("printer_url__c").toString();
				// get the printer name
				String printerName = result.getRowData().get(0).get("printer_name__c").toString();

				logger.info("Retrieved Printer Details.");

				mav.addObject("printerName", result.getRowData().get(0).get("name__v").toString());

				// start document printing as an individual thread
				DocumentMover mover = new DocumentMover(vaultDocumentClient, emailUtil, response.getJobID(), documentID,
						version, copyRequest.getActionLabel(), printFolderLocation, maxRetries, federatedId,
						printerHostName, printerName);

				// start thread per copy
				Thread thread = new Thread(mover);
				thread.start();
			} // if response:success

		} catch (Exception e) {

			viewName = "requesterrorpage";
			logger.error("Error while creating a controlled copy request. Document Id: " + copyRequest.getDocumentId(),
					e);
			mav.addObject("msg", e.getMessage());
			try {
				emailUtil.GenerateFailureEmail(e.getMessage(), copyRequest.getDocumentId());
			} catch (Exception ex) {
				logger.error("Error while processing the main error." + "", ex);
			}
		}

		mav.setViewName(viewName);
		return mav;
	}

	/**
	 * Callback method to retrieve the data for a VQL Filtered Dropdown
	 * 
	 * @param searchTerm   The text used to search
	 * @param field        The controlled copy field name
	 * @param documentId   Vault document Id
	 * @param documentName Vault Document Name
	 * @param copyType     The Object Type Name in vault (Technical name e.g.
	 *                     issued_batch_record__c)
	 * @param vaultDomain  Vault Domain
	 * @param sessionId    Vault Session Id
	 * @param userId       Vault User Id
	 * @return
	 */
	@RequestMapping(value = "vqldropdown", method = RequestMethod.GET)
	public @ResponseBody String ProcessVQLFilteredDropdown(
			@RequestParam(name = "term", required = false) String searchTerm,
			@RequestParam(name = "field", required = true) String field,
			@RequestParam(name = "docId", required = true) int documentId,
			@RequestParam(name = "docName", required = true) String documentName,
			@RequestParam(name = "copytype", required = true) String copyType,
			@RequestParam(name = "vaultdomain", required = true) String vaultDomain,
			@RequestParam(name = "sessionId", required = true) String sessionId,
			@RequestParam(name = "userId", required = true) Long userId) {

		String jsonString = "";

		try {
			setSession(userId, "", "0", vaultDomain, sessionId);
			JSONConfigManager configManager = new JSONConfigManager(configLocation, vaultQueryClient,
					vaultPicklistClient);
			jsonString = configManager.ProcessVQLFilteredDropdown(copyType, field, documentName, documentId,
					searchTerm);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

		return jsonString;
	}

	/**
	 * Callback method to retrieve the data for a Lazy Dropdown
	 * 
	 * @param searchTerm  The text used to search
	 * @param field       The controlled copy field name
	 * @param copyType    The Object Type Name in vault (Technical name e.g.
	 *                    issued_batch_record__c)
	 * @param vaultDomain Vault Domain
	 * @param sessionId   Vault Session Id
	 * @param userId      Vault User Id
	 * @return
	 */
	@RequestMapping(value = "lazydropdown", method = RequestMethod.GET)
	public @ResponseBody String ProcessLazyDropDown(@RequestParam(name = "term", required = false) String searchTerm,
			@RequestParam(name = "field", required = true) String field,
			@RequestParam(name = "copytype", required = true) String copyType,
			@RequestParam(name = "vaultdomain", required = true) String vaultDomain,
			@RequestParam(name = "sessionId", required = true) String sessionId,
			@RequestParam(name = "userId", required = true) Long userId) {

		String jsonString = "";

		try {

			setSession(userId, "", "0", vaultDomain, sessionId);
			JSONConfigManager configManager = new JSONConfigManager(configLocation, vaultQueryClient,
					vaultPicklistClient);
			jsonString = configManager.ProcessLazyDropdown(copyType, field, searchTerm);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

		return jsonString;
	}

	/**
	 * Callback method to retrieve the data for a User Dropdown
	 * 
	 * @param searchTerm  The text used to search
	 * @param vaultDomain The vault Domain
	 * @param sessionId   Vault Session Id
	 * @param userId      Vault User Id
	 * @return
	 */
	@RequestMapping(value = "usersdropdown", method = RequestMethod.GET)
	private @ResponseBody String ProcessUserDropdown(@RequestParam(name = "term", required = false) String searchTerm,
			@RequestParam(name = "vaultdomain", required = true) String vaultDomain,
			@RequestParam(name = "sessionId", required = true) String sessionId,
			@RequestParam(name = "userId", required = true) Long userId) {

		String jsonString = "";

		try {
			setSession(userId, "", "0", vaultDomain, sessionId);
			JSONConfigManager configManager = new JSONConfigManager(configLocation, vaultQueryClient,
					vaultPicklistClient);
			jsonString = configManager.ProcessUsersDropdown(searchTerm);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

		return jsonString;
	}

	/**
	 * Performs checks to see if a User has access to Print Request functionality
	 * 
	 * @param documentId Vault Document ID
	 * @param user       Vault logged in User Id
	 * @return
	 * @throws Exception
	 */
	private Boolean UserHasAccess(long documentId, long userId, String vaultDomain) throws Exception {
		Boolean hasAccess = false;

		// User Integration User to perform the user access check on logged in user id
		// as the logged in user may not have permission to call the document roles API
		// i.e. if the user only has access to older versions only and not the latest
		setSession(vaultDomain);

		logger.info("Retrieving User information");
		User user = userClient.getUserObject(userId);
		logger.info("Retrieved User information");

		logger.info("Checking User Access Rights");
		List<String> validRolesNames = new ArrayList<String>();
//		validRolesNames.add("printer__c");
//		validRolesNames.add("document_control__c");

		logger.info("Checking valid role");
		// split string comma separated and trimmed
		String[] validPrintRolesArray = validRoles.split("\\s*,\\s*");
		// add user roles to validate
		for (String validPrintRoles : validPrintRolesArray) {
			validRolesNames.add(validPrintRoles);
			logger.info("Add Valid Role:" + validPrintRoles);
		}

		// Get ALL the roles for this document
		logger.info("Retrieving Document Roles");
		List<DocumentRole> documentRoles = vaultDocumentClient.getDocumentRoles(new VaultObjectID(documentId));

		// Get the valid roles for this document
		List<DocumentRole> validRoles = documentRoles.stream().filter(r -> validRolesNames.contains(r.getName()))
				.collect(Collectors.toList());

		List<Long> assignedGroupIds = new ArrayList<Long>();
		List<Long> assignedUsers = new ArrayList<Long>();

		// Get the assigned Users and groups to these valid roles
		for (DocumentRole role : validRoles) {
			assignedGroupIds.addAll(role.getAssignedGroups().stream().collect(Collectors.toList()));
			assignedUsers.addAll(role.getAssignedUsers().stream().collect(Collectors.toList()));
		}

		// Check if the user is assigned to this role
		hasAccess = assignedUsers.stream().anyMatch(a -> (userId == a));

		if (!hasAccess) {
			// Check if the user is part of a group that has access
			Object groups = user.getPropertyObject(User.USER_PROPERTY_GROUPS);
			if (groups instanceof List) {

				@SuppressWarnings("unchecked")
				List<Long> groupList = (List<Long>) groups;
				hasAccess = groupList.stream().anyMatch(ug -> (assignedGroupIds.contains(ug)));
			} else {
				throw new Exception("Unknown data type");
			}
		}

		// once user passed all checks
		if (hasAccess) {
			if (!user.getFederatedID().toString().equals("")) {

				// set the federated id in variable
				userFederatedId = user.getFederatedID().toString();
				logger.info("Retrieved User's federated Id");
			} else {
				hasAccess = false;
				logger.info("No federated Id set for the user");
				throw new Exception("No federated Id set for the user");
			}
		}

		return hasAccess;
	}

	/**
	 * 
	 * Sets up the vault session client bean to connect to the appropriate Vault,
	 * using the supplied session ID. Fills Vault related data structures, so that
	 * client code will get the same details as if it had used the /auth API call.
	 * 
	 * @param userID      - Vault user ID
	 * @param userName    - Vault user name
	 * @param vaultID     - Vault ID
	 * @param vaultDomain - Vault domain, determines the target URL
	 * @param sessionId   - Vault session ID, used to authorize the requests.
	 */
	protected void setSession(long userID, String userName, String vaultID, String vaultDomain, String sessionId) {
		sessionClient.setBaseUri("https://" + vaultDomain + "/api/");
		Authorization auth = new Authorization();
		auth.setSessionId(sessionId);
		sessionClient.setAuthorization(auth);
		sessionClient.setLoggedIn(true);
		sessionClient.setUsername(userName);
		sessionClient.setVersionNumber(apiVersion);
		auth.setUserId(userID);
		auth.setCurrentVaultId(vaultID);
		ArrayList<VaultInfo> vaultInfos = new ArrayList<VaultInfo>();
		VaultInfo vaultInfo = new VaultInfo();
		vaultInfo.setId(Integer.parseInt(vaultID));
		vaultInfo.setUrl(vaultDomain);
		vaultInfos.add(vaultInfo);
		auth.setAvailableVaults(vaultInfos);
		sessionClient.setLoggedIn(true);
		sessionClient.setVaultSessionClient(sessionClient);
	}

	/**
	 * Sets up a Vault session using a Username and Password
	 * 
	 * @param vaultDomain - The Vault Domain
	 * @throws VaultClientAuthenticationFailure
	 */
	protected void setSession(String vaultDomain) throws VaultClientAuthenticationFailure {

		sessionClient.setBaseUri("https://" + vaultDomain + "/api/");
		sessionClient.setUsername(username);
		sessionClient.setPassword(password);
		sessionClient.setVersionNumber(apiVersion);
		sessionClient.setClientID("controlled-print-queue");
		sessionClient.setVaultSessionClient(sessionClient);
		logger.info("Authenticating integration user");
		sessionClient.login();
		logger.info("Authenticated integration user");
	}

	/**
	 * Pget Federated ID
	 * 
	 * @param documentId Vault Document ID
	 * @param user       Vault logged in User Id
	 * @return
	 * @throws Exception
	 */
	private String getFederatedId(int userId, String vaultDomain) throws Exception {
		// Boolean hasAccess = false;
		String federatedId = "";
		// User Integration User to perform the user access check on logged in user id
		// as the logged in user may not have permission to call the document roles API
		// i.e. if the user only has access to older versions only and not the latest
		setSession(userId, "", "0", vaultDomain, "");

		logger.info("Retrieving User information");
		User user = userClient.getUserObject(userId);
		logger.info("Retrieved User information");
		federatedId = user.getFederatedID().toString();
		return federatedId;
	}
}
