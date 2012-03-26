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
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
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

    private final static byte TG_GET_DATA = (byte) 0x86;
    private final static byte TG_INIT_AS_TARGET = (byte) 0x8c;
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
        (byte) 0x01, (byte) 0x10, (byte) 0x00};
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
                    initAsTarget();
                    try{
                        waitForTouchToBeam();
                    }catch(CardException ex){
                        System.out.println("[Error] "+ex.getMessage());
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
    
    private void initAsTarget() {
        try {
            System.out.println("Init as Target");
            sendCommand(TG_INIT_AS_TARGET, TG_TARGET_PAYLOAD,false);
        } catch (CardException ex) {
            System.out.println("[Error] "+ex.getMessage());
        }
    }

    private void waitForTouchToBeam() throws CardException {
        byte[] ccPayload = {(byte) 0x85, (byte) 0x81};
        byte[] response = null;
        System.out.println("Waiting for Beam");
        while (true) {
            try {
                Thread.sleep(50);
                response = sendCommand(TG_GET_DATA, null ,false);
                
                if (response[3] == 0x11) {
                    System.out.println("Beam receive");
                    byte[] ccNewPayload = {(byte) 0x81, (byte) 0x84};
                    sendCommand(TG_SET_DATA, ccNewPayload,false);
                    
                    sendCommand(TG_GET_DATA, null,false);
                    sendCommand(TG_SET_DATA, ccNewPayload,false);
                    
                    getDataFromBeam();
                    break;
                }
                
                sendCommand(TG_SET_DATA, ccPayload,false);
            
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }
    
    private byte[] getDataFromBeam(){
        try {
            byte[] data = sendCommand(TG_GET_DATA, null,true); //APDU response 
            
            byte[] llcp = NFCIPUtils.subByteArray(data, 3, data.length-5); //LLCP
            byte[] snep = NFCIPUtils.subByteArray(llcp, 3, llcp.length-3); //SNEP protocol
            byte[] ndef = NFCIPUtils.subByteArray(snep, 6, snep.length-6); //NDEF
            byte[] ndef_text = NFCIPUtils.subByteArray(ndef, 7, ndef.length-7); //NDEF text
            
            
            System.out.println(new String(ndef_text));
            
            return data;
        } catch (CardException ex) {
            System.out.println("[Error] "+ex.getMessage());
            return null;
        }
    }

    private byte[] sendCommand(byte intruction, byte[] payload,boolean debug) throws CardException {
        
            int payloadLength = (payload != null) ? payload.length : 0;
            byte[] instruction = {(byte) 0xd4, intruction};

            byte[] header = {(byte) 0xff, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) (instruction.length + payloadLength)};

            byte[] cmd = NFCIPUtils.appendToByteArray(header, instruction, 0,
                    instruction.length);

            cmd = NFCIPUtils.appendToByteArray(cmd, payload);

            if(debug)
                System.out.println("[Sent]     (" + cmd.length + " bytes): " + NFCIPUtils.byteArrayToString(cmd));

            CommandAPDU c = new CommandAPDU(cmd);
            ResponseAPDU response = ch.transmit(c);

            byte[] responseByte = response.getBytes();
            
            
            if(debug)
                System.out.println("[Received] (" + responseByte.length + " bytes): " + NFCIPUtils.byteArrayToString(responseByte));

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
            //System.out.println(NFCIPUtils.byteArrayToString(response.getData()));
            return responseByte;
        
    }
}
