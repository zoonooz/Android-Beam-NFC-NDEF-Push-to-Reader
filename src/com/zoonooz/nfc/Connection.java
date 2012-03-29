/*
 * Connection - Connect and receive data ACR122u 
 * 
 * Copyright (C) 2012  Amornchai Kanokpullwad <amornchai.zoon@gmail.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses/>.
 */
package com.zoonooz.nfc;

import ds.nfcip.NFCIPUtils;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.smartcardio.*;

/**
 *
 * @author amornchai kanokpullwad
 */
public class Connection {

    private final byte IN_DATA_EXCHANGE = (byte) 0x40;
    private final byte IN_RELEASE = (byte) 0x52;
    private final static byte TG_GET_DATA = (byte) 0x86;
    public final static byte TG_INIT_AS_TARGET = (byte) 0x8c;
    public final static byte TG_INIT_AS_INITIATOR = (byte) 0x50;
    private final static byte TG_SET_DATA = (byte) 0x8e;
    
    private final static byte[] TG_TARGET_PAYLOAD = {
        (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x40, (byte) 0x01, (byte) 0xfe, (byte) 0x0f,
        (byte) 0xbb, (byte) 0xba, (byte) 0xa6, (byte) 0xc9, (byte) 0x89,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xff, (byte) 0xff,
        (byte) 0x01, (byte) 0xfe, (byte) 0x0f, (byte) 0xbb, (byte) 0xba,
        (byte) 0xa6, (byte) 0xc9, (byte) 0x89, (byte) 0x00, (byte) 0x00,
        (byte) 0x06, (byte) 0x46, (byte) 0x66, (byte) 0x6D, (byte) 0x01,
        (byte) 0x01, (byte) 0x11, (byte) 0x02, (byte) 0x02, (byte) 0x00,
        (byte) 0x80, (byte) 0x04, (byte) 0x01, (byte) 0xfa };
    private final static byte[] TG_INITIATOR_PAYLOAD = {
        (byte) 0x01, (byte) 0x02, (byte) 0x46, (byte) 0x66, (byte) 0x6D, 
        (byte) 0x01, (byte) 0x01, (byte) 0x11, (byte) 0x02, (byte) 0x02, 
        (byte) 0x00, (byte) 0x80, (byte) 0x04, (byte) 0x01, (byte) 0xfa };
    
    private CardTerminal terminal;
    private CardChannel ch;

    public Connection() {
        try {
            terminal = TerminalFactory.getDefault().terminals().list().get(0);
            System.out.println("Terminal name: " + terminal.getName());

        } catch (CardException ex) {
            System.out.println("[Error] Can't find terminal");
            System.exit(0);
        } catch (IndexOutOfBoundsException ex) {
            System.out.println("[Error] No terminal");
            System.exit(0);
        }
    }

    public void start() {
        try {
            System.out.println("Waiting...");
            if (terminal.waitForCardPresent(0)) {
                Thread.sleep(300);
                System.out.println("Found target");
                ch = terminal.connect("*").getBasicChannel();
                if (ch != null) {
                    initAsInitator();
                    try {
                        waitForTouchToBeam();
                    } catch (CardException ex) {
                        System.out.println("[Error] " + ex.getMessage());
                        if (terminal.waitForCardAbsent(0)) {
                            start();
                        }

                    }
                } else {
                    System.out.println("[Error] no channel");
                }
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(Connection.class.getName()).log(Level.SEVERE, null, ex);
        } catch (CardException ex) {
            System.out.println("[Error] Problem with connecting to Reader");
            System.exit(0);
        }
    }

    private void initAsInitator() {
        try {
            System.out.println("Init as Initiator");
            sendCommand(TG_INIT_AS_INITIATOR, TG_INITIATOR_PAYLOAD, false);

        } catch (CardException ex) {
            System.out.println("[Error] " + ex.getMessage());
        }
    }

    private void waitForTouchToBeam() throws CardException {
        byte[] response ;
        byte[] targetConnect = {0x01, (byte) 0x05, (byte) 0x00}; //target + connect APDU
        byte[] targetCC = {0x01, (byte) 0x81, (byte) 0x84}; //target + cc APDU

        System.out.println("Waiting for Beam");

        while (true) {
            try {

                Thread.sleep(200);
                response = sendCommand(IN_DATA_EXCHANGE, targetConnect, false);

                if (response[3] == 0x11) { // Wait for android connection
                    System.out.println("Beam receive");
                    sendCommand(IN_DATA_EXCHANGE, targetCC, false);
                    getDataFromBeam();
                    break;
                }

            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void getDataFromBeam() {
        NdefMessage ndefMessage ;

        byte[] target = {0x01};
        byte[] targetCC = {0x01, (byte) 0x81, (byte) 0x84};
        byte[] targetDISC = {0x01, (byte) 0x01, (byte) 0x40};
        byte[] targetDM = {0x01, (byte) 0x81, (byte) 0xc4, 0x00};
        byte[] targetRR = {0x01, (byte) 0x83, (byte) 0x44, 0x00};
        byte[] targetResponseSuccess = {0x01, (byte) 0x83, (byte) 0x04, 0x00, (byte) 0x10, (byte) 0x81}; //success no more fragment 
        byte[] targetResponse = {0x01, (byte) 0x83, (byte) 0x04, 0x00, 0x10, (byte) 0x80}; //success send me more fragment
        byte[] targetResponseNDEF = {0x01, (byte) 0x83, (byte) 0x04, (byte) 0x00, 
            0x10, (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x00, 0x00
        }; //send empty NDEF to Android to tell him we finished

        byte[] data ;
        boolean ndefSuccess ;

        int sent = 0; // use for sequence number of LLCP Information PDU
        int receive = 0;

        try {
            
            //Get first Put Request SNEP
            data = sendCommand(IN_DATA_EXCHANGE, targetCC, false); 
            receive++; 
            
            //Received LLCP
            byte[] llcp = NFCIPUtils.subByteArray(data, 3, data.length - 5);
            
            //to SNEP protocol message
            byte[] snep = NFCIPUtils.subByteArray(llcp, 3, llcp.length - 3); 
            
            // new NDEF message with size from SNEP
            int size = (((snep[2] & 0xff) << 24) | ((snep[3] & 0xff) << 16) | (snep[4] & 0xff) << 8) | (snep[5] & 0xff);
            ndefMessage = new NdefMessage(size); 

            //SNEP to NDEF Message
            byte[] ndef = NFCIPUtils.subByteArray(snep, 6, snep.length - 6); 
            ndefSuccess = ndefMessage.appendByte(ndef); 
            
            
            //Our NDEF received all byte ?
            if (ndefSuccess) {
                
                System.out.println("Received Success");
                sendCommand(IN_DATA_EXCHANGE, targetResponseNDEF, false);
                sendCommand(IN_RELEASE, target, false);

            } else {
                
                targetResponse[3] = (byte) ((sent * 16) + receive);
                sendCommand(IN_DATA_EXCHANGE, targetResponse, false);
                sent++;

                // for next fragment if more
                while (!ndefSuccess) {
                    
                    targetResponse[3] = (byte) ((sent * 16) + receive);
                    targetResponseNDEF[3] = (byte) ((sent * 16) + receive);

                    data = sendCommand(IN_DATA_EXCHANGE, targetResponseNDEF, false);
                    sent++;
                    
                    // for I PDU Data
                    if (data[3] == 0x13 & data[4] == 0x20) {
                        
                        System.out.println("Received fragment");
                        receive++;
                        
                        // Received LLCP
                        llcp = NFCIPUtils.subByteArray(data, 3, data.length - 5); 
                        
                        //to SNEP protocol message (NDEF Fragement)
                        byte[] fragement = NFCIPUtils.subByteArray(llcp, 3, llcp.length - 3); 

                        //add to our NDEF message
                        ndefSuccess = ndefMessage.appendByte(fragement);

                    }

                    // Received all bytes of NDEF
                    if (ndefSuccess == true) {

                        targetResponseSuccess[3] = (byte) ((sent * 16) + receive);
                        sendCommand(IN_DATA_EXCHANGE, targetResponseSuccess, false);

                        //Disconnect mode 00h
                        sendCommand(IN_DATA_EXCHANGE, targetDM, false);

                        //Release target 0x01
                        sendCommand(IN_RELEASE, target, false);

                    }


                }


            }
            
            //Show text payload
            System.out.println(new String(ndefMessage.getPayload()));
            
        } catch (NdefException ex) {
            System.out.println("[Error] " + ex.getMessage());
        } catch (CardException ex) {
            System.out.println("[Error] " + ex.getMessage());
        }
    }

    private byte[] sendCommand(byte intruction, byte[] payload, boolean debug) throws CardException {

        int payloadLength = (payload != null) ? payload.length : 0;
        byte[] instruction = {(byte) 0xd4, intruction};

        byte[] header = {(byte) 0xff, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) (instruction.length + payloadLength)};

        byte[] cmd = NFCIPUtils.appendToByteArray(header, instruction, 0,
                instruction.length);

        cmd = NFCIPUtils.appendToByteArray(cmd, payload);

        if (debug) {
            System.out.println("[Sent]     (" + cmd.length + " bytes): " + NFCIPUtils.byteArrayToString(cmd));
        }

        CommandAPDU c = new CommandAPDU(cmd);
        ResponseAPDU response = ch.transmit(c);

        byte[] responseByte = response.getBytes();


        if (debug) {
            System.out.println("[Received] (" + responseByte.length + " bytes): " + NFCIPUtils.byteArrayToString(responseByte));
        }

        if (response.getSW1() == 0x63 && response.getSW2() == 0x27) {
            throw new CardException(
                    "wrong checksum from contactless response (0x63 0x27");
        } else if (response.getSW1() == 0x63 && response.getSW2() == 0x7f) {
            throw new CardException("wrong PN53x command (0x63 0x7f)");
        } else if (response.getBytes()[2] == 0x29) {
            throw new CardException("Card remove ?");
        } else if (response.getSW1() != 0x90 && response.getSW2() != 0x00) {
            throw new CardException("unknown error ("
                    + NFCIPUtils.byteToString(response.getSW1()) + " "
                    + NFCIPUtils.byteToString(response.getSW2()));
        }

        return responseByte;

    }
}
