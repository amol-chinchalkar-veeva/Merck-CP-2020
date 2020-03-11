/* 
 * Copyright (c) 2018 Veeva Systems Inc.
 * All Rights Reserved.
 * The Controlled-Print-Queue is developed and owned by Veeva Systems Inc.;
 * it is not to be copied, reproduced, or transmitted in any form, by any means, in whole or in part,
 * and may only be used in connection with the deliverable with which it was provided to Customer.
 */
package com.veeva.controlledprintqueue;

import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import com.veeva.controlledprintqueue.print.LPRPrintMode;
import com.veeva.model.VaultObjectID;
import com.veeva.model.Version;
import com.veeva.vaultclient.clients.VaultDocumentClient183;

/**
 * The downloading and saving of the Controlled copy to disk
 * 
 * @author amol.chinchalkar
 *
 */
public class DocumentMover implements Runnable {

	private Logger logger = LoggerFactory.getLogger(DocumentMover.class);
	private EmailUtil emailUtil;
	private VaultDocumentClient183 vaultDocumentClient;
	private long jobId;
	private VaultObjectID documentId;
	private Version version;
	private String actionLabel;
	private String printFolderLocation;
	private int maxRetries;
	private String userfederatedId;
	private String printerHostName;
	private String printerName;

	/** Print properties */
	@Value("${email.from.address}")
	private String emailFrom;
	/** Print properties */
	@Value("${vault.ecc.manifest}")
	private String manifestFileName;

	/**
	 * Constructs a new instance of the Document Mover
	 * 
	 * @param vaultDocumentClient   Vault API Document Client
	 * @param emailUtil             Application Email Utility
	 * @param jobId                 Vault Job Id
	 * @param documentId            Vault Document Id
	 * @param version               Vault Document Version
	 * @param actionLabel           Vault Control Copy Action label
	 * @param printerFolderLocation Printer Folder Location
	 * @param maxRetries            The maximum number of retries allowed for the
	 *                              Job
	 * @param userFederatedId       User Federated Id
	 */
	DocumentMover(VaultDocumentClient183 vaultDocumentClient, EmailUtil emailUtil, long jobId, VaultObjectID documentId,
			Version version, String actionLabel, String printerFolderLocation, int maxRetries, String userFederatedId,
			String printerHostName, String printerName) {
		this.vaultDocumentClient = vaultDocumentClient;
		this.emailUtil = emailUtil;
		this.documentId = documentId;
		this.jobId = jobId;
		this.version = version;
		this.actionLabel = actionLabel;
		this.printFolderLocation = printerFolderLocation;
		this.maxRetries = maxRetries;
		this.userfederatedId = userFederatedId;
		this.printerHostName = printerHostName;
		this.printerName = printerName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		try {

			logger.info("Checking if Job is ready. Job Id: " + this.jobId);
			Boolean jobReady = vaultDocumentClient.isJobFinished(Long.toString(jobId), 30, maxRetries);
			// zip file stream for the control copy download
			ZipEntry zipEntryFile;
			Map<String, String> filePathList = new HashMap<String, String>();

			// job is created begin download
			if (jobReady) {

				logger.info("Job is ready");
				logger.info("Initiate downloading of the print copies for document ID: " + this.documentId.getId());
				File outDir = new File(printFolderLocation);
				byte[] buffer = new byte[1024];
				boolean hasPrintFiles = false;

				try (InputStream stream = vaultDocumentClient.downloadByLifecycleJobAction(this.documentId,
						this.version, this.actionLabel, this.jobId);
						ZipInputStream zStream = new ZipInputStream(stream);) {

					while ((zipEntryFile = zStream.getNextEntry()) != null) {

						// logger.info("Unzipping started...Mainfest File Name:" + manifestFileName);
						// Remove the delivery manifest file as a Print copy is not needed "Delivery
						// Manifest.pdf"
						if (!zipEntryFile.getName().equalsIgnoreCase("Delivery Manifest.pdf")) {
							File newFile = new File(outDir, zipEntryFile.getName());
							// write to fos
							try (FileOutputStream fos = new FileOutputStream(newFile);) {

								int len;
								while ((len = zStream.read(buffer)) > 0) {
									fos.write(buffer, 0, len);
								}
								hasPrintFiles = true;
								// Added 03-09
								fos.close();
								// End 03-09
							}

							filePathList.put(zipEntryFile.getName(), outDir + "\\" + zipEntryFile.getName());
//							// initiate the printing using federated Id & PDF Box
//							LPRPrintMode.printFile(outDir + "\\" + zipEntryFile.getName(), printerHostName, printerName,
//									zipEntryFile.getName(), userfederatedId);
//
//							logger.info("File Successfully delivered to printer:" + zipEntryFile.getName());
//
//							// house keeping: delete the print download; Added "\\" on 02/28/2020
//							File file = new File(outDir + "\\" + zipEntryFile.getName());
//							if (file.exists()) {
//								logger.info("Deleting the file:" + outDir + "\\" + zipEntryFile.getName());
//								file.delete();
//							}

						} // if
					} // while
					stream.close();
				} // try
				logger.info("Finished Download of document:");
				if (hasPrintFiles) {
					// after downlaod all the files in the zip, send them to print queue
					initiateLPRPrint(filePathList, printerHostName, printerName, userfederatedId);
				}
			} else {

				throw new Exception(
						"Controlled Print Application has timed out and the document has not been sent to the printer");
			}
		} catch (EOFException eof) {
			//
			logger.error("EOFException while downloading the file", eof);
		} catch (Exception e) {
			try {
				logger.error("Error trying to download the controlled copy to the print folder", e);
				emailUtil.GenerateFailureEmail(e.getMessage(), Long.valueOf(this.documentId.getId()));
			} catch (Exception ex) {
				logger.error("Error while processing the printing error.", ex);
			}
		}

		logger.info("Download/Print Process Complete");

	}

	/**
	 * 
	 * @param filePathList
	 * @param printerHostName
	 * @param printerName
	 * @param fileName
	 * @param userfederatedId
	 */
	private void initiateLPRPrint(Map<String, String> filePathList, String printerHostName, String printerName,
			String userfederatedId) {

		logger.info("Initiate Print Process.. ");
		for (String fileName : filePathList.keySet()) {

			// initiate the printing using federated Id & PDF Box
			try {
				String filepath = filePathList.get(fileName);

				LPRPrintMode.printFile(filepath, printerHostName, printerName, fileName, userfederatedId);

				logger.info("File Successfully delivered to printer:" + fileName);

				// house keeping: delete the print download; Added "\\" on 02/28/2020
				File file = new File(filepath);
				if (file.exists()) {
					logger.info("Deleting the file:" + filepath);
					file.delete();
				}
			} catch (IOException | InterruptedException e) {
				logger.error("Error while processing the printing: ", e);
			}
		}
		logger.info("Print Process Complete..");
	}
}
