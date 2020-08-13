/*
SPDX-License-Identifier: Apache-2.0
*/
package org.guardtime.ksi.hlf.contract;

import java.util.Iterator;
import java.util.logging.Logger;

import com.guardtime.ksi.hashing.DataHash;
import com.guardtime.ksi.unisignature.KSISignature;

import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contact;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Default;
import org.hyperledger.fabric.contract.annotation.Info;
import org.hyperledger.fabric.contract.annotation.License;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.CompositeKey;
import org.hyperledger.fabric.shim.ledger.KeyModification;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;
import org.json.JSONArray;
import org.json.JSONObject;

import org.guardtime.ksi.hlf.wrapper.KsiWrapper;
/**
 * Define Guardtime KSI Signature smart contract.
 */
@Contract(name = "org.guardtime.ksi.hlf.contract",
          info = @Info(title = "KSI on fabric block contract",
                       description = "",
                       version = "0.0.1",
                       license = @License(name = "SPDX-License-Identifier: Apache-2.0", url = ""),
                                          contact = @Contact(email = "support@guardtime.com",
                                                             name = "java-contract",
                                                            url = "http://java-contract.me")))
@Default
public class KsiHlfContract implements ContractInterface {
    // use the classname for the logger, this way you can refactor
    private final static Logger LOG = Logger.getLogger(KsiHlfContract.class.getName());

    @Override
    public Context createContext(ChaincodeStub stub) {
        return new KSIContext(stub);
    }

    /**
     * Empty constructor is needed for any contract.
     */
    public KsiHlfContract() {

    }

    /**
     * Instantiate to perform any setup of the ledger that might be required.
     *
     * @param {Context} ctx is the transaction context.
     */
    @Transaction
    public void instantiate(KSIContext ctx) {
        // No implementation required with this example
        // It could be where data migration is performed, if necessary
        LOG.info("No data migration to perform");
    }

    /**
     * A dummy function for sanyty check that echos input string back.
     * @param {Context} ctx is the transaction context.
     * @param {String} input is string that is echoed back.
     * @return A String.
     */
    @Transaction
    public String myecho(KSIContext ctx, String input) {
        System.out.println("!");
        return "echo: " + input;
    }

    @Transaction
    public String myechoarray(KSIContext ctx, String[] input) {
        String tmp = "";
        for (int i = 0; i < input.length; i++) {
            tmp += "" + i + " >> " + input[i] + "!";
        }
        return tmp;
    }

    /**
     * A dummy function that pushes input string to constant ledger key.
     * Can be used to create new ledger blocks for testing automatic KSI signing,
     * without installing any new contracts. Note that when KSI signature is pushed
     * to the ledger and it is the only change, that block is not signed with KSI!
     * @param {Context} ctx is the transaction context.
     * @param {String} input is string that pushed to the ledger.
     * @return String containing ledger key and its new value.
     */
    @Transaction
    public String putdummy(KSIContext ctx, String input) {
        System.out.println("putdummy: adding some data!");
        ChaincodeStub stub = ctx.getStub();

        System.out.println("putdummy: client id:      " + ctx.getClientIdentity().getId());
        System.out.println("putdummy: channel id:     " + ctx.getStub().getChannelId());
        System.out.println("putdummy: ctx stub class: " + ctx.getStub().getClass().getName());
        System.out.println("putdummy: ctx class:      " + ctx.getClass().getName());

        CompositeKey ledgerKey = stub.createCompositeKey("gt", new String[] { "dummy", "key" });
        System.out.println("putdummy: ledgerkey is ");
        System.out.println(ledgerKey);

        byte[] data = input.getBytes();
        System.out.println("putdummy: put state ");
        ctx.getStub().putState(ledgerKey.toString(), data);
        System.out.println("putdummy: state finished ");

        return ledgerKey.toString() + " = " + input;
    }

    /**
     * This function is used to push a KSI signature related to the block to ledger.
     * Function takes block number, issuing organizations identifier and base64 encoded
     * KSI signature as input.
     * 
     * Block number and organizations identifier are used to construct the ledger key.
     * For example values "10" and "gt" will create key blocksig.ksi.gt.10.
     * 
     * Function parses KSI signature and runs internal verification on that. In case of
     * parsing or verification error new state is not pushed.
     * 
     * NOTE:
     * NOTE:
     * NOTE: that KSI signature is not verified against actual ledger block, as it is
     *       not possible to retrieve block info via smart contract api nor via system
     *       chaincode (qscc.GetBlockByNumber)!
     * 
     * @param {Context} ctx is the transaction context.
     * @param {Integer} blockNr is the block number.
     * @param {String}  org is the organization identifier.
     * @param {String}  base64ksig is KSI signature in base64 encoding.
     */
    @Transaction
    public KsiWrapper setKsi(KSIContext ctx, int blockNr, String org, String base64ksig, String[] recHash) {

        // fabric/core/scc/qscc/query.go
        // https://www.edureka.co/community/2836/how-can-traverse-the-blocks-transactions-hyperledger-fabric

        System.out.println(ctx);

        System.out.println("Creating signature...");
        
        KsiWrapper sig = KsiWrapper.newFromBase64(base64ksig, recHash, blockNr, org);

        /*
         * TODO: 1) Somehow get block by its nr. 2) Verify if KSI signature matches with
         * the block.
         * 
         * ... Dont know how to get a block!?!
         */
        System.out.println("Adding sig to list:");

        // Add the signature to the list.
        ctx.ksiList.addKsiSignature(sig);
        return sig;
    }

    /**
     * This function is similar to {@link #setKsi(KSIContext, int, String, String) setKsi}
     * but is used to push extended signatures to the ledger. It makes some extra checks:
     * <ul>
     * <li> Verifies that a signature already exists.
     * <li> Verifies that input hashes are equal.
     * <li> Verifies that aggregation times are equal. 
     * <li> Verifies that signature to be pushed is actually extended. 
     * </ul>
     * <br>
     * @param {Context} ctx is the transaction context.
     * @param {Integer} blockNr is the block number.
     * @param {String}  org is the organization identifier.
     * @param {String}  base64extksig is extended KSI signature in base64 encoding.
     */
    @Transaction
    public KsiWrapper updateWithExtended(KSIContext ctx, long blockNr, String org, String base64extksig) {
        KsiWrapper extSig = KsiWrapper.newFromBase64(base64extksig, blockNr, org);
        ctx.ksiList.updateExtended(blockNr, org, extSig);
        return extSig;
    }

    /**
     * This function is used to get KSI signature related to the block and signature issuing
     * organization. It handles input parameters like {@link #setKsi(KSIContext, int, String, String) setKsi}
     * to construct ledger key.
     *
     * @param {Context} ctx is the transaction context.
     * @param {Integer} blockNr is the block number.
     * @param {String}  org is the organization identifier.
     */
    @Transaction
    public String getKsi(KSIContext ctx, int blockNr, String org) {
        System.out.println("Getting signature...");

        try {
            return ctx.ksiList.getKsiSignature(blockNr, org).toString();
        } catch (Exception e) {
            System.out.println("TODO: Filter out appropirate exceptions.");
            System.out.println("TODO:" + e);
            System.out.println("TODO END");
            return "{\"ksig\":\"\", \"error\":\"" + e.toString() + "\"}";
        }

    }

    /**
     * This function is used to get brief history related to the block and signature
     * issuing organization. It handles input parameters like
     * {@link #setKsi(KSIContext, int, String, String) setKsi} to construct ledger key.
     * 
     * The result is returned in JSON that is array of objects. Every object has
     * one common field:
     * 
     * index   - where 0 is the first value pushed to the state and N is the recent
     *           value.
     * 
     * If key value is valid KSI signture it is parsed and its summary is printed
     * containig following fields:
     * sigtime - signing time.
     * pubtime - publication time if extended or empty string if not extended.
     * inhash  - input hash imprint.
     * 
     * If key value is not a valid KSI signature (e.g. corrupted signature or invalid
     * data) only one additional field is presented:
     * error   - error returned during parsing of the value. 
     * 
     * @param {Context} ctx is the transaction context.
     * @param {Integer} blockNr is the block number.
     * @param {String}  org is the organization identifier.
     * @return
     */
    @Transaction
    public String getHistoryOfBlock(KSIContext ctx, int blockNr, String org) {
        System.out.println("Getting signature history...");
        // String key = KsiWrapper.getKey(blockNr, org);
        String key = ctx.ksiList.getFullKey(KsiWrapper.getKey(blockNr, org));
        ChaincodeStub stub = ctx.getStub();
        System.out.println(" key is: " + key);

        QueryResultsIterator<KeyModification> history = stub.getHistoryForKey(key);
        Iterator<KeyModification> itr = history.iterator();

        int count = 0;
        for (Iterator<KeyModification> x = stub.getHistoryForKey(key).iterator(); x.hasNext(); count++) {x.next();}

        int i = 0;
        JSONObject json = new JSONObject();
        JSONArray array = new JSONArray();

        while(itr.hasNext()) {
            JSONObject item = new JSONObject();
            item.put("index", "" + (count - i));

            try {
                KsiWrapper sigw = KsiWrapper.deserialize(itr.next().getValue());
                KSISignature ksig = sigw.getKsi();
                
                ksig.getAggregationTime();
                ksig.getInputHash();
    
                
                item.put("sigtime", ksig.getAggregationTime().toString());
                
                if (ksig.isExtended()) {
                    item.put("pubtime", ksig.getPublicationTime());
                } else {
                    item.put("pubtime", "");
                }
                item.put("inhash", ksig.getInputHash().toString());
            } catch (Exception e) {
                item.put("error", "" + e.getMessage());
            }
            
            array.put(item);
            i++;
        }

        
        json.put("history", array);
        return json.toString();
    }



    /**
     * This function is used to get brief summary of blocks within given rage.
     * It handles input parameters like 
     * {@link #setKsi(KSIContext, int, String, String) setKsi} to construct ledger key
     * but for a single block number this function takes a range.
     *
     * The result is returned in JSON that is array of objects. Every object has
     * one common field:
     * 
     * block - number of the block.
     * 
     * When parsing of signature is successful object contains following fields:
     * 
     * sigtime - signing time.
     * pubtime - publication time if extended or empty string if not extended.
     * inhash  - input hash imprint.
     * 
     * If key value is not a valid KSI signature (e.g. corrupted signature or invalid
     * data) only one additional field is presented:
     * error   - error returned during parsing of the value. 
     * 
     * @param {Context} ctx the transaction context
     * @param {Integer} min block included 
     * @param {Integer} max block included
     */
    @Transaction
    public String getBlockSummary(KSIContext ctx, int blockMin, int blockMax, String org) {
        int i;
        JSONObject json = new JSONObject();
        JSONArray array = new JSONArray();



        for (i = blockMin; i < blockMax + 1; i++) {
            JSONObject item = new JSONObject();
            
            item.put("block", "" + i);
            try {
                KsiWrapper ksiw = ctx.ksiList.getKsiSignature(i, org);
                KSISignature ksig = ksiw.getKsi();
                
                item.put("inhash", ksig.getInputHash().toString());

                if (ksig.isExtended()) {
                    item.put("pubtime", ksig.getPublicationTime());
                } else {
                    item.put("pubtime", "");
                }
                item.put("sigtime", ksig.getAggregationTime().toString());
            } catch (Exception e) {
                item.put("error", e.toString());
            }

            
            array.put(item);
        }

        json.put("summary", array);
        return json.toString();
    }
}