package org.guardtime.ksi.hlf.util;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.Base64;

import org.json.JSONObject;
import org.hyperledger.fabric.gateway.Contract;
import org.hyperledger.fabric.gateway.Network;

import org.guardtime.ksi.hlf.wrapper.KsiWrapper;

public class KsiContractApi {
    private static final String chaincodeName = "ksi-hlf-contract";
    private static final String className = "org.guardtime.ksi.hlf.contract";
    
    private static final String fGetKsi = "getKsi";
    private static final String fSetKsi = "setKsi";
    private static final String fSetExtKsi = "updateWithExtended";

    public static String binToBase64(byte[] bin) {
        return new String(Base64.getEncoder().encode(bin));
      }
    
      public static byte[] base64ToBin(String str) {
        return Base64.getDecoder().decode(str);
    }
    
    private static String stringArrayToJsonArray(String[] hshl) {
      StringBuilder sb = new StringBuilder();

      String[] tmp = new String[hshl.length];
      sb.append("[");
      for (int i = 0; i < hshl.length; i++) {
        if (i > 0) sb.append(", ");
        sb.append("\"" + hshl[i] + "\"");
      }
      sb.append("]");

      return sb.toString();
    } 

    public static void pushSignature(Network network, KsiWrapper sig) {
      try {
        Contract contract = network.getContract(chaincodeName, className);
        byte[] response = contract.submitTransaction(fSetKsi, "" + sig.getBlockNumber(), sig.getOrg(), sig.getKsiBase64(), stringArrayToJsonArray(sig.getRecHash()));
      } catch (Exception e) {
        throw new RuntimeException("Unable to push KSI signature: " + sig.getOrg() + "." + sig.getBlockNumber());
      }    
    }

    public static void pushExtended(Network network, KsiWrapper sig) {
      // System.out.println("pushing extended signature: " + block + ".org: " + org);
      try {
        Contract contract = network.getContract(chaincodeName, className);
        byte[] response = contract.submitTransaction(fSetExtKsi, ""+sig.getBlockNumber(), sig.getOrg(), sig.getKsiBase64());
      } catch (Exception e) {
        throw new RuntimeException("Unable to extend KSI signature: " + sig.getOrg() + "." + sig.getBlockNumber());
      }
    }

    private static String getErrorString(byte[] response) {
      JSONObject json = new JSONObject(new String(response, UTF_8));
      if (json.has("error")) {
        return json.getString("error");
      }
      
      return "";
    } 

    public static KsiWrapper getSignature(Network network, Long block, String org) {
      try {
        Contract contract = network.getContract(chaincodeName, className);
        byte[] response = contract.evaluateTransaction(fGetKsi, ""+block, org);

        /* If signature does not exist return null */
        if (!getErrorString(response).isEmpty()) {
          return null;
        }

        KsiWrapper sig = KsiWrapper.deserialize(response);

        if (sig.getBlockNumber() != block) {
          throw new RuntimeException("Unexpected block number retreieved. Expecting " + block + " but got " + sig.getBlockNumber());
        }
        
        if (!sig.getOrg().equals(org)) {
          throw new RuntimeException("Unexpected org name retreieved. Expecting '" + org + "' but got '" + sig.getOrg() + "'");
        }

          return sig;
      } catch (Exception e) {
        throw new RuntimeException("Unable to get KSI signature: " + org + "." + block + "\n" + e);
      }
    }
}