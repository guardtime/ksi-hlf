package org.guardtime;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.Base64;
import java.io.ByteArrayOutputStream;

import org.json.JSONObject;
import org.hyperledger.fabric.gateway.Contract;
import org.hyperledger.fabric.gateway.Network;

import com.guardtime.ksi.unisignature.KSISignature;
import com.guardtime.ksi.SignatureReader;



public class KsiContract {
    private static final String chaincodeName = "ksionblock";
    private static final String className = "org.guardtime.ksionblock";
    
    private static final String fGetKsi = "getKsi";
    private static final String fSetKsi = "setKsi";
    private static final String fSetExtKsi = "updateWithExtended";

    private static String binToBase64(byte[] bin) {
        return new String(Base64.getEncoder().encode(bin));
      }
    
    private static byte[] base64ToBin(String str) {
        return Base64.getDecoder().decode(str);
    }
    
    private static String signatureToBase64(KSISignature sig) {
        try {
            ByteArrayOutputStream arrayBuilder = new ByteArrayOutputStream(0x2000);
            sig.writeTo(arrayBuilder);
            byte[] bin = arrayBuilder.toByteArray();
            return binToBase64(bin);
        } catch (Exception e) {
            throw new RuntimeException("Unable to serialize KSI signature:" + e.toString());
          }
      }
      
      private static KSISignature signatureFromBase64(String base64) {
        try {
            SignatureReader rdr = new SignatureReader();
            KSISignature sig = rdr.read(base64ToBin(base64));
            return sig;
        } catch (Exception e) {
            throw new RuntimeException("Unable to parse KSI signature:" + e.toString());
        }
    } 
      
    
    
      public static void pushSignature(Network network, Long block, String org, KSISignature sig) {
        // System.out.println("pushing signature: " + block + ".org: " + org);
        try {
          Contract contract = network.getContract(chaincodeName, className);
          byte[] response = contract.submitTransaction(fSetKsi, ""+block, org, signatureToBase64(sig));
        } catch (Exception e) {
          throw new RuntimeException("Unable to push KSI signature: " + org + "." + block);
        }
      }
      
      public static void pushExtended(Network network, Long block, String org, KSISignature sig) {
        // System.out.println("pushing extended signature: " + block + ".org: " + org);
        try {
          Contract contract = network.getContract(chaincodeName, className);
          byte[] response = contract.submitTransaction(fSetExtKsi, ""+block, org, signatureToBase64(sig));
        } catch (Exception e) {
          throw new RuntimeException("Unable to extend KSI signature: " + org + "." + block);
        }
      }
      
      public static KSISignature getSignature(Network network, Long block, String org) {
        try {
          Contract contract = network.getContract(chaincodeName, className);
          byte[] response = contract.submitTransaction(fGetKsi, ""+block, org);
          String j = new String(response, UTF_8);
            
          JSONObject json = new JSONObject(j);
          
          String sigBase64 = json.getString("ksig");
          if (sigBase64.isEmpty()) {
            // System.out.println("  Signature not available.");
            // System.out.println("  Error: " + json.getString("error"));
            return null;
          }

          long blockNumber = json.getLong("block");
          String orgName = json.getString("org");
    
          if (blockNumber != block) {
            throw new RuntimeException("Unexpected block number retreieved. Expecting " + block + " but got " + blockNumber);
          }
          
          if (!org.equals(orgName)) {
            throw new RuntimeException("Unexpected org name retreieved. Expecting '" + org + "' but got '" + orgName + "'");
          }
    
          // System.out.println("===== base64 =====");
          // System.out.println(sigBase64);
          // System.out.println("===== base64 =====");

          return signatureFromBase64(sigBase64);
        } catch (Exception e) {
          throw new RuntimeException("Unable to get KSI signature: " + org + block + "\n" + e);
        }
      }
}