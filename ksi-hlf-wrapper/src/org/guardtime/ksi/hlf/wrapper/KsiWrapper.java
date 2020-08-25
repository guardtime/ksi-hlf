package org.guardtime.ksi.hlf.wrapper;

import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.Base64;
import java.io.ByteArrayOutputStream;

import org.guardtime.ksi.hlf.ledgerapi.State;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONPropertyIgnore;
import org.json.JSONPropertyName;

import com.guardtime.ksi.unisignature.KSISignature;
import com.guardtime.ksi.SignatureReader;
import com.guardtime.ksi.exceptions.KSIException;
import com.guardtime.ksi.hashing.DataHash;

import com.guardtime.ksi.tree.ImprintNode;
import com.guardtime.ksi.tree.HashTreeBuilder;


/**
 * This is KSI Signature wrapper for holding KSI signature issued to HLF block.
 * KSI blocksigner is used to create a signature from block header and metadata
 * with local aggregation so that both components are independently verifiable.
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
 *  "version":  <int>           // Version of the data struct (1).
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
    private static final long CURRENT_VERSION = Version.VER_1;

    public static String getKey(long block, String org) {
        if (org == null) throw new NullPointerException("Unable to construct KsiWrapper key as org is null!");
        if (org.isEmpty()) throw new IllegalArgumentException("Unable to construct KsiWrapper key as org is empty string!");
        if (block < 0) throw new IllegalArgumentException("Unable to construct KsiWrapper key as block " + block + " < 0!");
        return org + "." + block;
    }

    public static KsiWrapper newFromBase64(String base64, String[] recHash, long blockNumber, String org) throws NullPointerException, IllegalArgumentException, KsiWrapperException {
        return newFromBin(CURRENT_VERSION, base64ToBin(base64), base64, recHash, blockNumber, org);
    }

    public static KsiWrapper newFromBase64(String base64, long blockNumber, String org) throws NullPointerException, IllegalArgumentException, KsiWrapperException {
        return newFromBase64(base64, new String[0], blockNumber, org);
        // return newFromBin(CURRENT_VERSION, base64ToBin(base64), base64, new String[0], blockNumber, org);
    }
    
    public static KsiWrapper newFromKSI(KSISignature sig, DataHash[] recHash, long blockNumber, String org) throws NullPointerException, IllegalArgumentException, KsiWrapperException {
        ByteArrayOutputStream arrayBuilder = new ByteArrayOutputStream(0x2000);
        try {
            sig.writeTo(arrayBuilder);
        } catch (KSIException e) {
            throw new KsiWrapperException("Unable to serialize KSI signature!", ErrCodeEnum.ERR_KSI, e);
        }
        byte[] bin = arrayBuilder.toByteArray();
        return newFromBin(CURRENT_VERSION, bin, binToBase64(bin), recordHashesToBase64(recHash), blockNumber, org);
    }

    public static KsiWrapper newFromKSI(KSISignature sig, long blockNumber, String org) throws NullPointerException, IllegalArgumentException, KsiWrapperException {
        return newFromKSI(sig, new DataHash[0], blockNumber, org);
    }

    @JSONPropertyIgnore
    public KSISignature isInitialized() {
        return sig;
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
        byte[] tmp = this.serialize();
        return new String(tmp, UTF_8);
    }

    @Override
    public KsiWrapper parse(byte[] data) throws RuntimeException {
        KsiWrapper tmp = deserialize(data);
        copy(tmp, this);
        return this;
    }

    private static long getLong(JSONObject json, String key) throws KsiWrapperException {
        try {
            return json.getLong(key);
        } catch (Exception e) {
            throw new KsiWrapperException("Unable to parse KSI Wrapper object" + key + ": '" + json.getString(key) + "'!", ErrCodeEnum.ERR_INVALID_JSON_OBJECT, e);
        }
    }

    private static void checkMandatoryKey(JSONObject json, String key) throws KsiWrapperException {
        if (!json.has(key)) {
            throw new KsiWrapperException("KSI Wrapper object missing mandatory key '" + key + "'!", ErrCodeEnum.ERR_INVALID_JSON_OBJECT);
        }
    }

    private static long checkVersion(JSONObject json) throws KsiWrapperException {
        checkMandatoryKey(json, "ver");
        long ver = getLong(json, "ver");
        
        if (ver == Version.VER_1) {
            checkMandatoryKey(json, "ksig");
            checkMandatoryKey(json, "block");
            getLong(json, "block");
            checkMandatoryKey(json, "org");
            checkMandatoryKey(json, "rechash");
        } else {
            throw new KsiWrapperException(
                        "Unsupported KSI Wrapper version: " + ver + "! Supported versions: " + Version.getSupportedVersionsString() + ".",
                        ErrCodeEnum.ERR_INVALID_OBJECT_VERSION);
            
        }

        return ver;
    }

    private static String[] getRecHashList(JSONObject json, String key) {
        try {
            String[] recHash = new String[0];
            JSONArray jsonArray = json.getJSONArray("rechash");
            recHash = new String[jsonArray.length()];
            if (jsonArray.length() == 0) {
                throw new KsiWrapperException("Unable to parse KSI Wrapper object '" + key + "'! Array length is 0!", ErrCodeEnum.ERR_INVALID_JSON_OBJECT);
            }
            
            for (int i = 0; i < jsonArray.length(); i++) {
                recHash[i] = jsonArray.getString(i);
            }
            return recHash;
        } catch (Exception e) {
            throw new KsiWrapperException("Unable to parse KSI Wrapper object '" + key + "'!", ErrCodeEnum.ERR_INVALID_JSON_OBJECT, e);
        }
    }

    // public static byte[] serialize(KsiWrapper sig) {
    //     return State.serialize(sig);
    // }

    private static void verifyRecordHashes(KSISignature ksig, String[] recHash) {
        // if (recHash.length == 0) {
        //     throw new KsiWrapperException("The count of record hashes must be 2, but is " + recHash.length);
        // }
        
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

        if (!ksig.getInputHash().equals(rootHash)) {
            throw new KsiWrapperException("KSI Signature input hash calculated from record hashes does not match with the signature!", ErrCodeEnum.ERR_WRAP_VERIFICATION_FAILURE);
        }

        return;
    }

    /**
     * Creates new KsiWrapper from binary input. Note that also base64 representation is needed for internal use.
     * This approach is used to be able to create new wrapper from KSI signature or from base64 string.
     * See {@link #newFromBase64} and {@link #newFromKsi} for public functions to construct wrapper.
     * 
     */
    private static KsiWrapper newFromBin(long ver, byte[] bin, String base64, String[] recHash, long blockNumber, String org) throws KsiWrapperException {
        if (bin == null) throw new NullPointerException("KSI signature binary array for parsing is null!");
        if (bin.length == 0) throw new IllegalArgumentException("KSI signature binary array for parsing is empty!");
        
        if (base64 == null) throw new NullPointerException("KSI signature base64 representation is null!");
        if (base64.isEmpty()) throw new IllegalArgumentException("KSI signature base64 representation is empty!");
        
        if (org == null) throw new NullPointerException("KSI signature wrapper org is null!");
        if (org.isEmpty()) throw new IllegalArgumentException("KSI signature wrapper org is empty!");
        
        if (blockNumber < 0) throw new IllegalArgumentException("KSI signature wrapper block can not be negative!");

        if (!Version.isSupported(ver)) throw new KsiWrapperException(
            "Unsupported KSI Wrapper version: " + ver + "! Supported versions: " + Version.getSupportedVersionsString() + ".",
            ErrCodeEnum.ERR_INVALID_OBJECT_VERSION);

        KSISignature sig;
        try {
            SignatureReader rdr = new SignatureReader();
            sig = rdr.read(bin);
        } catch (Exception e) {
            throw new KsiWrapperException("Unable to parse KSI signature.", ErrCodeEnum.ERR_KSI, e);
        }
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
    } 

    private static byte[] base64ToBin(String str) {
        return Base64.getDecoder().decode(str);
    }

    private static String binToBase64(byte[] bin) {
        return new String(Base64.getEncoder().encode(bin));
    }

    private static String[] recordHashesToBase64(DataHash[] hshList) {
        if (hshList == null) {
            return null;
        }

        String[] tmp = new String[hshList.length];
        for (int i = 0; i < hshList.length; i++) {
            tmp[i] = binToBase64(hshList[i].getImprint());
        }
  
        return tmp;
      }

      private static void copy(KsiWrapper source, KsiWrapper target) {
        target.block = source.block;
        target.extended = source.extended;
        target.key = source.key;
        target.ksig = source.ksig;
        target.org = source.org;
        target.rechash = source.rechash;
        target.recordHash = source.recordHash;
        target.sig = source.sig;
        target.ver = source.ver;
      } 

      private static KsiWrapper deserialize(byte[] data) throws NullPointerException, IllegalArgumentException, KsiWrapperException {
        if (data == null) throw new NullPointerException("Deserializing of " + KsiWrapper.class.getName() + " failed as input is null!");
        if (data.length == 0) throw new IllegalArgumentException("Deserializing of " + KsiWrapper.class.getName() + " failed as input is empty!");
        
        String j = new String(data, UTF_8);
     
        JSONObject json = new JSONObject(j);

        // Check and get version. Verify mandatory keys.
        long ver = checkVersion(json);
        
        if (ver == Version.VER_1) {
            String state = json.getString("ksig");
            long blockNumber = getLong(json, "block");
            String org = json.getString("org");
            String[] recHash = getRecHashList(json, "rechash");
        
            try {
                return newFromBase64(state, recHash, blockNumber, org);
            } catch (Exception e) {
                throw new KsiWrapperException("Unable to parse KSI signature!", ErrCodeEnum.ERR_UNEXPECTED, e);
            }
        } else {
            throw new KsiWrapperException("Unexpected failure. Deserializing version: " + ver + " not implemented.", ErrCodeEnum.ERR_UNEXPECTED);
        }
    }
}