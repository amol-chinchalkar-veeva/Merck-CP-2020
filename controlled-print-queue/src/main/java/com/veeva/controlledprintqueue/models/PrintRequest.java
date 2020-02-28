/* 
 * Copyright (c) 2018 Veeva Systems Inc.
 * All Rights Reserved.
 * The Controlled-Print-Queue is developed and owned by Veeva Systems Inc.;
 * it is not to be copied, reproduced, or transmitted in any form, by any means, in whole or in part,
 * and may only be used in connection with the deliverable with which it was provided to Customer.
 */
package com.veeva.controlledprintqueue.models;

/**
 * Model to hold the Print Request data
 * @author amol.chinchalkar
 *
 */
public class PrintRequest {

	String PrintReason;
	String PrinterId;
	String Document;
	int NumberOfCopies;
	long DocumentId;
	long MajorVersion;
	long MinorVersion;
	long UserId;

	public long getUserId() {
		return UserId;
	}

	public void setUserId(long userId) {
		UserId = userId;
	}

	public long getDocumentId() {
		return DocumentId;
	}

	public void setDocumentId(long documentId) {
		DocumentId = documentId;
	}
	
	public long getMajorVersion() {
		return MajorVersion;
	}

	public void setMajorVersion(long majorVersion) {
		MajorVersion = majorVersion;
	}

	public long getMinorVersion() {
		return MinorVersion;
	}

	public void setMinorVersion(long minorVersion) {
		MinorVersion = minorVersion;
	}

	public int getNumberOfCopies() {
		return NumberOfCopies;
	}

	public void setNumberOfCopies(int numberOfCopies) {
		NumberOfCopies = numberOfCopies;
	}

	public String getPrinterId() {
		return PrinterId;
	}

	public void setPrinterId(String printerId) {
		PrinterId = printerId;
	}

	public String getPrintReason() {
		return PrintReason;
	}

	public void setPrintReason(String printReason) {
		PrintReason = printReason;
	}
}