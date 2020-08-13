// package org.guardtime.ksihlf;
package org.guardtime.ksi.hlf.wrapper;

import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.Base64;
import java.io.ByteArrayOutputStream;

import org.example.ledgerapi.State;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONPropertyIgnore;
import org.json.JSONPropertyName;

import com.guardtime.ksi.unisignature.KSISignature;
import com.guardtime.ksi.SignatureReader;
import com.guardtime.ksi.hashing.DataHash;

import com.guardtime.ksi.tree.ImprintNode;
import com.guardtime.ksi.tree.HashTreeBuilder;



// /root/.m2/repository/org/hyperledger/fabric-chaincode-java/fabric-chaincode-shim
// /root/.gradle/caches/modules-2/metadata-2.71/descriptors/org.hyperledger.fabric-chaincode-java/fabric-chaincode-shim

/**
 * This is KSI Signature wrapper for holding KSI signature issued to HLF block.
 * KSI blocksigner is used to create a signature from block header and metadata
 * with local aggregation so that both components are idependendly verifyable.
 * 
 * This object is stored in ledger and is bound with one block. The object is
 * stored in JSON encoding:
 * 
 * {
 *  "block":    <int>,          // The number of the HLF block.
 *  "org":      <str>,          // Organization ID that is issuing KSI signatures.
 *                              // Value is used in HLF ledger key construction.
 *  "ksig":     <base64 str>,   // KSI signature in base64 encoding.
 *  "rechash":  [<base64 str>], // Optional list of record hashes used in local
 *                              // aggregation. 
 *  "extended": <bool>          // Boolean value set true if "ksig" is extended.
 *  }
 * 
 * Notes:
 *  Json field name must match with the variable. In that case Java Object returned by the
 *  transaction is represented correctly.
 */
@DataType()
public class KsiWrapper extends State {
    @Property()
    private String ksig;

    @Property()
    private String[] rechash;
    
    @Property()
    private boolean extended;
    
    @Property()
    private long block;
    
    @Property()
    private long ver;
    
    @Property()
    private String org;
    
    
    private DataHash[] recordHash;
    private KSISignature sig;
    private static final long CURRENT_VERSION = 1;

    public static String getKey(long block, String org) {
        return "." + org + "." + block;
    }

    public static KsiWrapper newFromBase64(String base64, String[] recHash, long blockNumber, String org) {
        return newFromBin(CURRENT_VERSION, base64ToBin(base64), base64, recHash, blockNumber, org);
    }

    public static KsiWrapper newFromBase64(String base64, long blockNumber, String org) {
        return newFromBin(CURRENT_VERSION, base64ToBin(base64), base64, new String[0], blockNumber, org);
    }
    
    public static KsiWrapper newFromKSI(KSISignature sig, DataHash[] recHash, long blockNumber, String org) {
        if (blockNumber < 0) {
            throw new ChaincodeException("Block number can not be negative! " + blockNumber);
        } else if (org == null || org == "") {
            throw new ChaincodeException("Org can not be null nor empty string!");
        }
        
        try {
            ByteArrayOutputStream arrayBuilder = new ByteArrayOutputStream(0x2000);
            sig.writeTo(arrayBuilder);
            byte[] bin = arrayBuilder.toByteArray();
            return newFromBin(CURRENT_VERSION, bin, binToBase64(bin), recordHashesToBase64(recHash), blockNumber, org);
        } catch (Exception e) {
            throw new ChaincodeException("Unable to parse KSI signature:" + e.toString());
        }
    }

    public static KsiWrapper newFromKSI(KSISignature sig, long blockNumber, String org) {
        return newFromKSI(sig, new DataHash[0], blockNumber, org);
    }

    @JSONPropertyIgnore
    public KSISignature getKsi() {
        return sig;
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
    
    @JSONPropertyName("ver")
    public long getVersion() {
        return ver;
    }

    @JSONPropertyName("rechash")
    public String[] getRecHash() {
        return this.rechash;
    }
  
    @JSONPropertyIgnore
    public DataHash[] getRecordHash() {
        if (this.recordHash == null) {
            DataHash[] tmp = new DataHash[this.rechash.length];
            for (int i = 0; i < this.rechash.length; i++) {
                tmp[i] = new DataHash(base64ToBin(this.rechash[i]));
            }

            this.recordHash = tmp;
        }
        return this.recordHash;
    }

    @Override
    public String toString() {
        byte[] tmp = KsiWrapper.serialize(this);
        return new String(tmp, UTF_8);
    }

    public static KsiWrapper deserialize(byte[] data) {
        String j = new String(data, UTF_8);
     
        if (j.isEmpty()) {
           throw new IllegalArgumentException("Empty base64 string can not be parsed to KSI signature.");
        }

        // System.out.println("==== KSI Wrapper in json ====");
        // System.out.println("'" + j + "'");
        // System.out.println("==== KSI Wrapper in json ====");

        JSONObject json = new JSONObject(j);
        long ver = json.getLong("ver");
        if (ver != CURRENT_VERSION) {
            throw new ChaincodeException("Unexpected version " + ver + ". Only version " + CURRENT_VERSION + " is supported.");
        }
        
        String state = json.getString("ksig");
        long blockNumber = json.getLong("block");
        String org = json.getString("org");

        String[] recHash = new String[0];
        if (json.has("rechash")) {
            JSONArray jsonArray = json.getJSONArray("rechash");
            recHash = new String[jsonArray.length()];
            for (int i = 0; i < jsonArray.length(); i++) {
                recHash[i] = jsonArray.getString(i);
            }
        }

        try {
            return newFromBase64(state, recHash, blockNumber, org);
        } catch (Exception e) {
            throw new ChaincodeException("Unable to parse KSI signature: " + e.toString());
        }
    }

    public static byte[] serialize(KsiWrapper sig) {
        return State.serialize(sig);
    }


    private static void verifyRecordHashes(KSISignature ksig, String[] recHash) {
        if (recHash.length != 2) {
            throw new ChaincodeException("The count of recordhashes must be 2, but is " + recHash.length);
        }
        
        // System.out.println("Verifying KSI signature input hash.");
        DataHash headerHash = new DataHash(base64ToBin(recHash[0]));
        DataHash fabricMetaDataHash = new DataHash(base64ToBin(recHash[1]));
        ImprintNode hn = new ImprintNode(headerHash);
        ImprintNode mn = new ImprintNode(fabricMetaDataHash);
        
        HashTreeBuilder tb = new HashTreeBuilder();
        tb.add(hn);
        tb.add(mn);
        ImprintNode root = tb.build();
        DataHash rootHash = new DataHash(root.getValue());

        // System.out.println("  Record hash 1    : " + headerHash);
        // System.out.println("  Record hash 2    : " + fabricMetaDataHash);
        // System.out.println("  Input hash calc. : " + rootHash);
        // System.out.println("  Input hash       : " + ksig.getInputHash());

        if (!ksig.getInputHash().equals(rootHash)) {
            throw new ChaincodeException("KSI Signature input hash calculated from recod hashes does not match with the signature!");
        }

        return;
    }

    private static KsiWrapper newFromBin(long ver, byte[] bin, String base64, String[] recHash, long blockNumber, String org) {
        try {
            SignatureReader rdr = new SignatureReader();
            KSISignature sig = rdr.read(bin);
            KsiWrapper tmp = new KsiWrapper();
            
            tmp.ksig = base64;
            tmp.rechash = recHash;
            tmp.extended = sig.isExtended();
            tmp.block = blockNumber;
            tmp.sig = sig;
            tmp.key = KsiWrapper.getKey(blockNumber, org);
            tmp.org = org;
            tmp.ver = ver;

            if (recHash != null && recHash.length != 0) {
                verifyRecordHashes(sig, recHash);
            }

            return tmp;
        } catch (Exception e) {
            throw new ChaincodeException("Unable to parse KSI signature:" + e.toString());
        }
    } 

    private static byte[] base64ToBin(String str) {
        return Base64.getDecoder().decode(str);
    }

    private static String binToBase64(byte[] bin) {
        return new String(Base64.getEncoder().encode(bin));
    }

    private static String[] recordHashesToBase64(DataHash[] hshl) {
        String[] tmp = new String[hshl.length];
        for (int i = 0; i < hshl.length; i++) {
          tmp[i] = binToBase64(hshl[i].getImprint());
        }
  
        return tmp;
      } 
}