/*
 * NdefMessage - Ndef Message data format 
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


/**
 *
 * @author amornchaikanokpullwad
 */
public class NdefMessage {

    private int MB; 
    private int ME;
    private int CF;
    private int SR;
    private int IL;
    private int TNF;
    private int TYPE_LENGTH;
    private int PAYLOAD_LENGTH;
    private byte[] TYPE;
    private byte[] ID;
    private byte ID_LENGTH = 0;
    private byte[] PAYLOAD;
    private byte[] bytedata = {};
    private int length;

    public NdefMessage(int length) {
        this.length = length;
    }

    
    public boolean appendByte(byte[] data) throws NdefException {
        if (bytedata.length < length) {
            
            bytedata = NFCIPUtils.appendToByteArray(bytedata, data);

            if (bytedata.length == length) {
                setNdefMessageFromByte();
                return true;
            }
            return false;
        } else {
            throw new NdefException("Ndef error");
        }

    }

    //init byte to message format
    private void setNdefMessageFromByte() {
        int indexCurrent = 0;
        int headerByte = bytedata[0];               //hex to decimal
        String header = Integer.toBinaryString(headerByte); 
        header = header.substring(24);

        MB = Integer.parseInt(header.charAt(0) + "");
        ME = Integer.parseInt(header.charAt(1) + "");
        CF = Integer.parseInt(header.charAt(2) + "");
        SR = Integer.parseInt(header.charAt(3) + "");
        IL = Integer.parseInt(header.charAt(4) + "");
        TNF = Integer.parseInt(header.substring(5, 8));
        TYPE_LENGTH = (int) bytedata[1];

        if (SR == 1) { //Short Record
            PAYLOAD_LENGTH = (bytedata[2] & 0xff);
            indexCurrent = 2;
        } else {
            PAYLOAD_LENGTH = (((bytedata[2] & 0xff) << 24) | ((bytedata[3] & 0xff) << 16) | (bytedata[4] & 0xff) << 8) | (bytedata[5] & 0xff);
            indexCurrent = 5;
        }

        if (IL == 1) { //ID length present
            indexCurrent++;
            ID_LENGTH = bytedata[indexCurrent];
        }
        
        if (TYPE_LENGTH != 0) {
            indexCurrent++;
            TYPE = NFCIPUtils.subByteArray(bytedata, indexCurrent, TYPE_LENGTH);
            indexCurrent += (TYPE_LENGTH - 1);
        }
        
        if (ID_LENGTH != 0){
            indexCurrent++;
            ID = NFCIPUtils.subByteArray(bytedata, indexCurrent, ID_LENGTH);
            indexCurrent += (ID_LENGTH - 1);
        }
        indexCurrent++;

        PAYLOAD = NFCIPUtils.subByteArray(bytedata, indexCurrent, PAYLOAD_LENGTH);
    }
    
    public byte[] getPayload() {
        return PAYLOAD;
    }
    
    public int getSize () {
        return length;
    }
}
