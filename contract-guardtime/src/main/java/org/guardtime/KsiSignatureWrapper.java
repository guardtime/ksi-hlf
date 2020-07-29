package org.guardtime;

import static java.nio.charset.StandardCharsets.UTF_8;

import org.example.ledgerapi.State;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.json.JSONObject;
import org.json.JSONPropertyIgnore;
import org.json.JSONPropertyName;

import com.guardtime.ksi.unisignature.KSISignature;
import com.guardtime.ksi.SignatureReader;

import java.util.Base64;
import java.io.ByteArrayOutputStream;

// /root/.m2/repository/org/hyperledger/fabric-chaincode-java/fabric-chaincode-shim
// /root/.gradle/caches/modules-2/metadata-2.71/descriptors/org.hyperledger.fabric-chaincode-java/fabric-chaincode-shim

/**
 * Notes:
 *  Json field name must match with the variable. In that case Java Object returned by the
 *  transaction is represented correctly. Somehow the serialize function works even when the
 *  variable name and json field name differs.
 */
@DataType()
public class KsiSignatureWrapper extends State {
    @Property()
    private String ksig;
    
    @Property()
    private boolean extended;
    
    @Property()
    private boolean test;

    @Property()
    private int myint;

    @Property()
    private long block;
    
    @Property()
    private String org;

    static public String getKey(long block, String org) {
        // return State.makeKey(new String[]{org, Long.toString(block)});
        return "." + org + "." + block;
    }

    public static KsiSignatureWrapper newFromBase64(String base64, long blockNumber, String org) {
        return newFromBin(base64ToBin(base64), base64, blockNumber, org);
    }
    
    public static KsiSignatureWrapper newFromHex(String hex, long blockNumber, String org) {
        byte[] bin = hexToBin(hex);
        return newFromBin(bin, binToBase64(bin), blockNumber, org);
    }

    public static KsiSignatureWrapper newFromKSI(KSISignature sig, long blockNumber, String org) {
        if (blockNumber < 0) {
            throw new ChaincodeException("Block number can not be negative! " + blockNumber);
        } else if (org == null || org == "") {
            throw new ChaincodeException("Org can not be null nor empty string!");
        }
        
        try {
            ByteArrayOutputStream arrayBuilder = new ByteArrayOutputStream(0x2000);
            sig.writeTo(arrayBuilder);
            byte[] bin = arrayBuilder.toByteArray();
            return newFromBin(bin, binToBase64(bin), blockNumber, org);
        } catch (Exception e) {
            throw new ChaincodeException("Unable to parse KSI signature:" + e.toString());
        }
    }

    @JSONPropertyIgnore
    public KSISignature getKsi() {
        return sig;
    }
    
    @JSONPropertyIgnore
    public String getKsiHex() {
        return binToHex(base64ToBin(ksig));
    }

    @JSONPropertyName("org")
    public String getOrg() {
        return org;
    }
    
    @JSONPropertyName("ksig")
    public String getKsiBase64() {
        return ksig;
    }

    @JSONPropertyIgnore
    public byte[] getKsiBin() {
        return base64ToBin(ksig);
    }

    @JSONPropertyName("extended")
    public boolean isExtended() {
        return extended;
    }

    @JSONPropertyName("block")
    public long getBlockNumber() {
        return block;
    }

    public static KsiSignatureWrapper deserialize(byte[] data) {
        String j = new String(data, UTF_8);
     
        if (j.isEmpty()) {
           throw new IllegalArgumentException("Empty base64 string can not be parsed to KSI signature.");
        }

        System.out.println("==== my json ====");
        System.out.println("'" + j + "'");
        System.out.println("==== my json ====");


        JSONObject json = new JSONObject(j);
        String state = json.getString("ksig");
        long blockNumber = json.getLong("block");
        String org = json.getString("org");


        try {
            return newFromBase64(state, blockNumber, org);
        } catch (Exception e) {
            throw new ChaincodeException("Unable to parse KSI signature: " + e.toString());
        }
    }

    public static byte[] serialize(KsiSignatureWrapper sig) {
        return State.serialize(sig);
    }


    private KSISignature sig;

    private static KsiSignatureWrapper newFromBin(byte[] bin, String base64, long blockNumber, String org) {
        try {
            SignatureReader rdr = new SignatureReader();
            KSISignature sig = rdr.read(bin);
            KsiSignatureWrapper tmp = new KsiSignatureWrapper();
            
            tmp.ksig = base64;
            tmp.extended = sig.isExtended();
            tmp.block = blockNumber;
            tmp.sig = sig;
            tmp.key = KsiSignatureWrapper.getKey(blockNumber, org);
            tmp.org = org;
    
            return tmp;
        } catch (Exception e) {
            throw new ChaincodeException("Unable to parse KSI signature:" + e.toString());
        }
    } 

    private static byte[] hexToBin(String str) {
        byte[] val = new byte[str.length() / 2];
          for (int i = 0; i < val.length; i++) {
             int index = i * 2;
             int j = Integer.parseInt(str.substring(index, index + 2), 16);
             val[i] = (byte) j;
          }

          return val;
    }

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    private static String binToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    private static byte[] base64ToBin(String str) {
        return Base64.getDecoder().decode(str);
    }

    private static String binToBase64(byte[] bin) {
        return new String(Base64.getEncoder().encode(bin));
    }

    @Override
    public String toString() {
        byte[] tmp = this.serialize(this);
        return new String(tmp, UTF_8);
    }
}