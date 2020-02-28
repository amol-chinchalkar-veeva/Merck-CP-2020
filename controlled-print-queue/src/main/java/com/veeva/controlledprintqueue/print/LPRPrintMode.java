/* 
 * Copyright (c) 2018 Veeva Systems Inc.
 * All Rights Reserved.
 * The Controlled-Print-Queue is developed and owned by Veeva Systems Inc.;
 * it is not to be copied, reproduced, or transmitted in any form, by any means, in whole or in part,
 * and may only be used in connection with the deliverable with which it was provided to Customer.
 */
package com.veeva.controlledprintqueue.print;

/**
 *
 *The package com.printscript.lpr is a group of open source classes providing
 *lpr client and lpd server functions.
 *
 */
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.BindException;
import java.net.InetAddress;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <P>
 * <B>jLpr</B> provides an interface to LPD print servers running on network
 * hosts and network connected printers.
 * </P>
 * <P>
 * It conforms to RFC 1179.
 * </P>
 */

public class LPRPrintMode {

	private static boolean printRaw = true;
	private static boolean useOutOfBoundsPorts = false;
	private static int jobNumber = 0;
	private static Logger logger = LoggerFactory.getLogger(LPRPrintMode.class);

	/**
	 * By default jLpr prints all files as raw binary data, if you need to use the
	 * text formatting of the spooler on your host set this value to false
	 * 
	 */
	public void setPrintRaw(boolean printRawData) {
		printRaw = printRawData;
	}

	/**
	 * @see #setPrintRaw
	 */
	public boolean getPrintRaw() {
		return (printRaw);
	}

//	public void setUsername(String username) {
//		logger.info("Setting username in jLpr to " + username);
//		this.username = username;
//	}

	/**
	 * The RFC for lpr specifies the use of local ports numbered 721 - 731, however
	 * TCP/IP also requires that any port that is used will not be released for 3
	 * minutes which means that jLpr will get stuck on the 12th job if prints are
	 * sent quickly.
	 * 
	 * To resolve this issue you can use out of bounds ports which most print
	 * servers will support
	 * 
	 * The default for this is off
	 */
	public void setUseOutOfBoundPorts(boolean OutOfBoundsPorts) {
		useOutOfBoundsPorts = OutOfBoundsPorts;
	}

	/**
	 * @see #setUseOutOfBoundPorts
	 */

	public boolean getUseOutOfBoundPorts() {
		return (useOutOfBoundsPorts);
	}

	/**
	 * create socket with the print server host at 515 PORT
	 * @param hostName
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */

	private static Socket getSocket(String hostName) throws IOException, InterruptedException {
		if (useOutOfBoundsPorts) {
			return (new Socket(hostName, 515));
		} else {
			Socket tmpSocket = null;
			for (int j = 0; (j < 30) && (tmpSocket == null); j++) {
				for (int i = 721; (i <= 731) && (tmpSocket == null); i++) {
					try {
						//bind to socket, 
						tmpSocket = new Socket(hostName, 515, InetAddress.getLocalHost(), i);
					} catch (BindException be) {
					}
				}
				if (tmpSocket == null) {
					Thread.sleep(10000);
				}

			}
			if (tmpSocket == null) {
				throw new BindException("jLpr Can't bind to local port/address");
			}
			return (tmpSocket);
		}
	}

	/**
	 * Print a file to a network host or printer
	 * 
	 * @see #printFile(String , String , String , String )
	 */

//	public void printFile(String fileName, String hostName, String printerName)
//			throws IOException, InterruptedException {
//		printFile(fileName, hostName, printerName, fileName);
//	}

	/**
	 * Print a file to a network host or printer
	 * 
	 * @param fileName     The path to the file to be printed
	 * @param hostName     The host name or IP address of the print server
	 * @param printerName  The name of the remote queue or the port on the print
	 *                     server
	 * @param documentName The name of the document as displayed in the spooler of
	 *                     the host
	 */
	public static void printFile(String filePath, String hostName, String printerName, String documentName,
			String userName) throws IOException, InterruptedException {
		logger.info("Begin printing..");

		String controlFile = "";
		// String printFile = "";
		File fileToPrint = null;
		byte buffer[] = new byte[1000];
		int bytesRead;
		String printCommand;
		String strJobNumber;

		// Job number cycles from 001 to 999
		if (++jobNumber >= 1000) {
			jobNumber = 1;
		}
		strJobNumber = "" + jobNumber;
		logger.info("LPR Print :Job Number:" + strJobNumber);
		while (strJobNumber.length() < 3) {
			strJobNumber = "0" + strJobNumber;
		}

		if (userName == null) {
			userName = "Unknown";
		}
		//open the socket on print server 
		Socket socketLpr = getSocket(hostName);
		socketLpr.setSoTimeout(30000);
		// I/O streams to send to printer
		OutputStream sOut = socketLpr.getOutputStream();
		InputStream sIn = socketLpr.getInputStream();

		// Open printer
		printCommand = "\002" + printerName + "\n";
		sOut.write(printCommand.getBytes());
		sOut.flush();
		acknowledge(sIn, "lpr Failed to open printer");

		// Send control file with job command
		controlFile += "H" + hostName + "\n";
		controlFile += "P" + userName + "\n";
		controlFile += ((printRaw) ? "o" : "p") + "dfA" + strJobNumber + hostName +"\n";
		controlFile += "UdfA" + strJobNumber + hostName + "\n";
		//controlFile += "-o scaling=100 \n"; 	//" -o " +" fit-to-page"+
		controlFile += "N" + documentName + "\n";

		printCommand = "\002" + (controlFile.length()) + " cfA" + strJobNumber + hostName+ "\n";
		sOut.write(printCommand.getBytes());

		acknowledge(sIn, "lpr Failed to send control header");

		buffer = controlFile.getBytes();
		sOut.write(buffer);
		buffer[0] = 0;
		sOut.write(buffer, 0, 1);
		sOut.flush();

		acknowledge(sIn, "jLpr Failed to send control file");

		// Send print file
		fileToPrint = new File(filePath);
		if (!(fileToPrint.exists() && fileToPrint.isFile() && fileToPrint.canRead())) {
			throw new IOException("jLpr Error opening print file");
		}
		printCommand = "\003" + (fileToPrint.length()) + " dfA" + strJobNumber + hostName +  "\n";
		sOut.write(printCommand.getBytes());
		sOut.flush();
		acknowledge(sIn, "jLpr Failed to send print file command");

		FileInputStream fs = new FileInputStream(fileToPrint);

		int readCounter;
		do {
			readCounter = fs.read(buffer);
			if (readCounter > 0) {
				sOut.write(buffer, 0, readCounter);
			}
		} while (readCounter > 0);
		buffer[0] = 0;
		sOut.write(buffer, 0, 1);
		sOut.flush();
		acknowledge(sIn, "jLpr Failed to send print file");
		socketLpr.close();
	}

	/**
	 * 
	 * @param in
	 * @param alert
	 * @throws IOException
	 */
	private static void acknowledge(InputStream in, String alert) throws IOException {
		if (in.read() != 0) {
			throw new IOException(alert);
		}
	}

}
