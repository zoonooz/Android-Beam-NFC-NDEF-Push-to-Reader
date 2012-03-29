package com.zoonooz.nfc;

import ds.nfcip.NFCIPUtils;

/**
 *
 * @author amornchaikanokpullwad
 */
public class test {
    public static void main(String[] args) {
        Connection connection = new Connection();
        connection.start();
        
        //System.out.println(NFCIPUtils.byteToString((byte) (long)(137 + 1)));
    }
}
