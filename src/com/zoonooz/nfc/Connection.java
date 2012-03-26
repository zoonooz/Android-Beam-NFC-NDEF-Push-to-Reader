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
                    waitForTouchToBeam();
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
        sendCommand(TG_INIT_AS_TARGET, TG_TARGET_PAYLOAD);
    }

    private void waitForTouchToBeam() {
        byte[] ccPayload = {(byte) 0x85, (byte) 0x81};
        byte[] response = null;
        System.out.println("Waiting for Beam");
        while (true) {
            try {
                Thread.sleep(50);
                response = sendCommand(TG_GET_DATA, null);
                
                if (response[3] == 0x11) {
                    System.out.println("Beam receive");
                    byte[] ccNewPayload = {(byte) 0x81, (byte) 0x84};
                    sendCommand(TG_SET_DATA, ccNewPayload);
                    
                    sendCommand(TG_GET_DATA, null);
                    sendCommand(TG_SET_DATA, ccNewPayload);
                    
                    getDataFromBeam();
                    break;
                }
                
                sendCommand(TG_SET_DATA, ccPayload);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }
    
    private byte[] getDataFromBeam(){
        byte[] data = sendCommand(TG_GET_DATA, null);
        System.out.println(new String(NFCIPUtils.subByteArray(data, 3, data.length-5)));
        return data;
    }

    private byte[] sendCommand(byte intruction, byte[] payload) {
        try {
            int payloadLength = (payload != null) ? payload.length : 0;
            byte[] instruction = {(byte) 0xd4, intruction};

            byte[] header = {(byte) 0xff, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) (instruction.length + payloadLength)};

            byte[] cmd = NFCIPUtils.appendToByteArray(header, instruction, 0,
                    instruction.length);

            cmd = NFCIPUtils.appendToByteArray(cmd, payload);

            System.out.println("[Sent]     (" + cmd.length + " bytes): " + NFCIPUtils.byteArrayToString(cmd));

            CommandAPDU c = new CommandAPDU(cmd);
            ResponseAPDU response = ch.transmit(c);

            byte[] responseByte = response.getBytes();
            System.out.println("[Received] (" + responseByte.length + " bytes): " + NFCIPUtils.byteArrayToString(responseByte));

            if (response.getSW1() == 0x63 && response.getSW2() == 0x27) {
                throw new CardException(
                        "wrong checksum from contactless response (0x63 0x27");
            } else if (response.getSW1() == 0x63 && response.getSW2() == 0x7f) {
                throw new CardException("wrong PN53x command (0x63 0x7f)");
            } else if (response.getSW1() != 0x90 && response.getSW2() != 0x00) {
                throw new CardException("unknown error ("
                        + NFCIPUtils.byteToString(response.getSW1()) + " "
                        + NFCIPUtils.byteToString(response.getSW2()));
            }

            return responseByte;
        } catch (CardException ex) {
            System.out.println("[Error] " + ex.getMessage());
            System.exit(0);
            return null;
        }
    }
}
