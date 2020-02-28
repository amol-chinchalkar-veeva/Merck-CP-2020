/* 
 * Copyright (c) 2018 Veeva Systems Inc.
 * All Rights Reserved.
 * The Controlled-Print-Queue is developed and owned by Veeva Systems Inc.;
 * it is not to be copied, reproduced, or transmitted in any form, by any means, in whole or in part,
 * and may only be used in connection with the deliverable with which it was provided to Customer.
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.veeva.controlledprintqueue.print;

import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.IOException;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.servlet.http.HttpServletRequest;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.printing.PDFPageable;

/**
 * A sample for demonstration Print PDFs using PDFBox.
 */
public class PDFBoxPrintMode {
	public PDFBoxPrintMode() {

	}

	static PrintService myPrintService = null;

	/**
	 * Entry point.
	 */

	/**
	 * 
	 * @param printerName
	 * @return
	 */
	private static PrintService findPrintService(String printerName) {
		// get the printers configured
		PrintService[] printServices = PrintServiceLookup.lookupPrintServices(null, null);
		for (PrintService printService : printServices) {
			// System.out.println("Printer :" + printService.getName());
			if (printService.getName().trim().equals(printerName)) {
				return printService;
			}
		}
		return null;
	}

	/**
	 * Prints the document at its actual size. This is the recommended way to print.
	 * 
	 * @param filePath
	 * @param printerName
	 * @param request
	 * @throws IOException
	 * @throws PrinterException
	 */
	public static void printFile(String filePath, String printerName, HttpServletRequest request)
			throws IOException, PrinterException {
		//load document
		PDDocument document = PDDocument.load(new File(filePath));
		//find printer if avaialble
		myPrintService = findPrintService(printerName);
		//start printing
		if (!myPrintService.equals(null)) {
			PrinterJob job = PrinterJob.getPrinterJob();
			job.setPageable(new PDFPageable(document));
			job.setPrintService(myPrintService);
			job.print();
		}

	}

}