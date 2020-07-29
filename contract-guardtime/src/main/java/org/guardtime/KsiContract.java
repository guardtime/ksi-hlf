/*
SPDX-License-Identifier: Apache-2.0
*/
package org.guardtime;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import com.guardtime.ksi.unisignature.KSISignature;

import org.example.ledgerapi.State;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contact;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Default;
import org.hyperledger.fabric.contract.annotation.Info;
import org.hyperledger.fabric.contract.annotation.License;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.Chaincode.Response;
import org.hyperledger.fabric.shim.ledger.CompositeKey;
import org.json.JSONArray;
import org.json.JSONObject;



// Query system chaincode (QSCC)

/**
 * A custom context provides easy access to list of all commercial papers
 */

/**
 * Define commercial paper smart contract by extending Fabric Contract class
 *
 */
@Contract(name = "org.guardtime.ksionblock",
        info = @Info(
                title = "KSI on fabric block contract",
                description = "",
                version = "0.0.1",
                license = @License(name = "SPDX-License-Identifier: Apache-2.0", url = ""),
                contact = @Contact(email = "support@guardtime.com", name = "java-contract", url = "http://java-contract.me")
                )
        )
@Default
public class KsiContract implements ContractInterface {

    // use the classname for the logger, this way you can refactor
    private final static Logger LOG = Logger.getLogger(KsiContract.class.getName());
    @Override
    public Context createContext(ChaincodeStub stub) {
        return new KSIContext(stub);
    }

    public KsiContract() {

    }

    /**
     * Define a custom context for commercial paper
     */

    /**
     * Instantiate to perform any setup of the ledger that might be required.
     *
     * @param {Context} ctx the transaction context
     */
    @Transaction
    public void instantiate(KSIContext ctx) {
        // No implementation required with this example
        // It could be where data migration is performed, if necessary
        LOG.info("No data migration to perform");
    }

    @Transaction
    public String donothing(KSIContext ctx, String input) {
        System.out.println("This is my debug text!");
        // String is returned.
       return "I AM DOING NOTHING!" + input;
    }

    @Transaction
    public String putdummy(KSIContext ctx, String input) {
        System.out.println("putdummy: adding some data!");
        ChaincodeStub stub = ctx.getStub();
        
        System.out.println("putdummy: client id:      " + ctx.getClientIdentity().getId());
        System.out.println("putdummy: channel id:     " + ctx.getStub().getChannelId());
        System.out.println("putdummy: ctx stub class: " + ctx.getStub().getClass().getName());
        System.out.println("putdummy: ctx class:      " + ctx.getClass().getName());


        CompositeKey ledgerKey = stub.createCompositeKey("gt", new String[]{"dummy", "key"});
        System.out.println("putdummy: ledgerkey is ");
        System.out.println(ledgerKey);
        
        byte[] data = input.getBytes();
        System.out.println("putdummy: put state ");
        ctx.getStub().putState(ledgerKey.toString(), data);
        System.out.println("putdummy: state finished ");

       return "I AM DOING NOTHING!" + input;
    }


    /**
     * Set KSI signature for block. If already exists, exception is thrown.
     *
     * @param {Context} ctx the transaction context
     * @param {Integer} block number
     * @param {String} KSI signature in base64 encoding
     */
    @Transaction
    public KsiSignatureWrapper setKsi(KSIContext ctx, int blockNr, String org, String base64ksig) {

        // fabric/core/scc/qscc/query.go
        // https://www.edureka.co/community/2836/how-can-traverse-the-blocks-transactions-hyperledger-fabric


        System.out.println(ctx);

        System.out.println("Creating signature...");
        KsiSignatureWrapper sig = KsiSignatureWrapper.newFromBase64(base64ksig, blockNr, org);

        /* 
            TODO:
                1) Somehow get block by its nr.
                2) Verify if KSI signature matches with the block. 

                ... Dont know how to get a block!?!
        */
        System.out.println("Adding sig to list:");
        
        // Add the signature to the list.
        ctx.ksiList.addKsiSignature(sig);

        // Must return a serialized paper to caller of smart contract
        return sig;
    }

    /**
     * Update KSI signature with extended version.
     *
     * @param {Context} ctx the transaction context
     * @param {Long} block number
     * @param {String} extended KSI signature in base64 encoding
     */
    @Transaction
    public KsiSignatureWrapper updateWithExtended(KSIContext ctx, long blockNr, String org, String base64extksig) {

        KsiSignatureWrapper extSig = KsiSignatureWrapper.newFromBase64(base64extksig, blockNr, org);
        // Retrieve the current paper using key fields provided

        ctx.ksiList.updateExtended(blockNr, org, extSig);

        // Update the paper
        return extSig;
    }

    /**
     * Get KSI signature by block number.
     *
     * @param {Context} ctx the transaction context
     * @param {Integer} block number 
     * @param {String} KSI signature in base64 encoding
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
            return "{\"ksig\":\"\", \"error\":\"" + e.toString() +     "\"}";
        }

    }

    @Transaction
    public String hasKsi(KSIContext ctx, int blockNr, String org) {
        System.out.println("Getting signature...");
        return ctx.ksiList.getKsiSignature(blockNr, org).toString();
    }

    /**
     * Get KSI signature by block number.
     *
     * @param {Context} ctx the transaction context
     * @param {Integer} block number 
     * @param {String} KSI signature in base64 encoding
     */
    @Transaction
    public Response getBlock(KSIContext ctx, int blockNr) {
        System.out.println("Getting block...");
        
        try {
            List<String> arg = Arrays.asList("GetBlockByNumber", "mychannel", "00001");
            ChaincodeStub stub = ctx.getStub(); 
            System.out.println("Args: " + arg + " Stub: " + stub);
            
            Response resp = stub.invokeChaincodeWithStringArgs("qscc", arg);
            System.out.println("Response donex: ");
            System.out.println("Response stat: " + resp.getStatusCode());
            System.out.println("Response msg: " + resp.getMessage());

            return resp;
        } catch (Exception e) {
            System.out.println("ERROR: " + e);
            throw e;
            //TODO: handle exception
        }
    }

    /**
     * Get KSI signature summary.
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
            KsiSignatureWrapper ksiw = ctx.ksiList.getKsiSignature(i, org);
            KSISignature ksig = ksiw.getKsi();
            
            item.put("block", "" + i);
            item.put("hash", ksig.getInputHash().toString());
            item.put("extended", ksig.isExtended());
            item.put("sigtime", ksig.getAggregationTime().toString());
            
            array.put(item);
        }

        json.put("ksionb", array);
        return json.toString();
    }



}
