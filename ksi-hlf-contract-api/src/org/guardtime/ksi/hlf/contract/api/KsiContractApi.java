/*
 * Copyright 2021 Guardtime, Inc.
 *
 * This file is part of the KSI-HLF integration toolkit.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES, CONDITIONS, OR OTHER LICENSES OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 * "Guardtime" and "KSI" are trademarks or registered trademarks of
 * Guardtime, Inc., and no license to trademarks is granted; Guardtime
 * reserves and retains all trademark rights.
 */

package org.guardtime.ksi.hlf.contract.api;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.Base64;

import com.guardtime.ksi.hashing.HashAlgorithm;

import org.json.JSONObject;
import org.hyperledger.fabric.gateway.Contract;
import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.guardtime.ksi.hlf.wrapper.KsiWrapper;

/**
 * KsiContractApi is module between KSI+HLF contract and HLF application.
 * It provides:
 *   1) Pushing KsiWrapper object to the ledger.
 *   2) Pushing extended KsiWrapper objects to the ledger.
 *   3) Getting KsiWrapper objects from the ledger.
 *   4) Getting block from the ledger.
 */
public class KsiContractApi {
    private static final String chaincodeName = "ksi-hlf-contract";
    private static final String className = "org.guardtime.ksi.hlf.contract";
    private static final String fGetKsi = "getKsi";
    private static final String fSetKsi = "setKsi";
    private static final String fSetExtKsi = "updateWithExtended";

    private Network network;

    /**
     * Create new contract api to access KSI+HLF contract.
     * @param network Network object that has KSI+HLF contract installed.
     */
    public KsiContractApi(Network network) {
        this.network = network;
    }

    private static void validateKsiWrapper(String msg, KsiWrapper sig) {
        if (sig == null) throw new NullPointerException(msg + " as sig is null!");
        if (!sig.isInit()) throw new IllegalArgumentException(msg + "as sig is not initialized!");
    }

    public void pushSignature(KsiWrapper sig) {
        validateKsiWrapper("Unable to push KSI signature to the ledger", sig);

        try {
            Contract contract = this.network.getContract(chaincodeName, className);
            contract.submitTransaction(fSetKsi, "" + sig.getBlockNumber(), sig.getOrg(), sig.getKsiBase64(),
                    stringArrayToJsonArray(sig.getRecHash()));
        } catch (Exception e) {
            throw new KsiContractException("Unable to push KSI signature: " + sig.getOrg() + "." + sig.getBlockNumber(), e);
        }
    }

    public void pushExtended(KsiWrapper sig) {
        validateKsiWrapper("Unable to push extended KSI signature to the ledger", sig);

        try {
            Contract contract = this.network.getContract(chaincodeName, className);
            contract.submitTransaction(fSetExtKsi, "" + sig.getBlockNumber(), sig.getOrg(), sig.getKsiBase64());
        } catch (Exception e) {
            throw new KsiContractException("Unable to extend KSI signature: " + sig.getOrg() + "." + sig.getBlockNumber(), e);
        }
    }

    /**
     * Return KsiWrapper for the block issued by org.
     * @param block
     * @param org
     * @return KsiWrapper object if valid object is available, null otherwise.
     */
    public KsiWrapper getSignature(Long block, String org) {
        try {
            Contract contract = this.network.getContract(chaincodeName, className);
            byte[] response = contract.evaluateTransaction(fGetKsi, "" + block, org);

            JSONObject json = new JSONObject(new String(response, UTF_8));
            if (json.isEmpty()) {
                return null;
            }

            
            KsiWrapper sig = new KsiWrapper().parse(response);

            if (sig.getBlockNumber() != block) {
                throw new KsiContractException(
                        "Unexpected block number retrieved. Expecting " + block + " but got " + sig.getBlockNumber());
            }

            if (!sig.getOrg().equals(org)) {
                throw new KsiContractException(
                        "Unexpected org name retrieved. Expecting '" + org + "' but got '" + sig.getOrg() + "'");
            }

            return sig;
        } catch (Exception e) {
            throw new KsiContractException("Unable to get KSI signature: " + org + "." + block + "\n", e);
        }
    }

    public BlockHelper getBlock(long block) {
        try {
            return new BlockHelper(this.network.getChannel().queryBlockByNumber(block), HashAlgorithm.SHA2_256);
        } catch (ProposalException e) {
            if (e.getMessage().contains("error Entry not found in index")) throw new KsiContractException("Block: " + block + " does not exist!", e);
            throw new KsiContractException("Unable to get block: " + block, e);
        } catch (Exception e) {
            throw new KsiContractException("Unable to get block: " + block, e);
        }
    }

    public static String binToBase64(byte[] bin) {
        return new String(Base64.getEncoder().encode(bin));
    }

    public static byte[] base64ToBin(String str) {
        return Base64.getDecoder().decode(str);
    }

    private static String stringArrayToJsonArray(String[] hshl) {
        StringBuilder sb = new StringBuilder();

        sb.append("[");
        for (int i = 0; i < hshl.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append("\"" + hshl[i] + "\"");
        }
        sb.append("]");

        return sb.toString();
    }
}