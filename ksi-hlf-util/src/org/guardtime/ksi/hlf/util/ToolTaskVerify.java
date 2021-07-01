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

package org.guardtime.ksi.hlf.util;

import java.util.logging.Logger;
import java.util.List;
import java.util.logging.Level;
import java.util.Iterator;

import com.guardtime.ksi.hashing.DataHash;
import com.guardtime.ksi.publication.PublicationData;
import com.guardtime.ksi.unisignature.KSISignature;
import com.guardtime.ksi.unisignature.verifier.PolicyVerificationResult;
import com.guardtime.ksi.unisignature.verifier.VerificationResult;
import com.guardtime.ksi.unisignature.verifier.VerificationResultCode;
import com.guardtime.ksi.unisignature.verifier.policies.ContextAwarePolicy;
import com.guardtime.ksi.unisignature.verifier.policies.ContextAwarePolicyAdapter;
import com.guardtime.ksi.SignatureVerifier;

import org.apache.commons.cli.CommandLine;
import org.guardtime.ksi.hlf.contract.api.KsiContractApi;
import org.guardtime.ksi.hlf.wrapper.KsiWrapper;
import org.guardtime.ksi.hlf.contract.api.BlockHelper;

class ToolTaskVerify extends ToolTask {
    ContextAwarePolicy policy;

    public ToolTaskVerify(Conf c, Logger log, CommandLine cmd) throws Exception {
        super(c, log, cmd);

        /* Create verification policy: */
        if (cmd.hasOption("ver-int")) {
            policy = ContextAwarePolicyAdapter.createInternalPolicy();
        } else if (cmd.hasOption("ver-cal")) {
            policy = ContextAwarePolicyAdapter.createCalendarPolicy(c.getExtender());
        } else if (cmd.hasOption("ver-key")) {
            policy = ContextAwarePolicyAdapter.createKeyPolicy(c.getPubHandler());
        } else if (cmd.hasOption("ver-pub") && cmd.hasOption("pub-str")) {
            PublicationData pubStr = new PublicationData(cmd.getOptionValue("pub-str"));
            if (cmd.hasOption("x")) {
                policy = ContextAwarePolicyAdapter.createUserProvidedPublicationPolicy(pubStr, c.getExtender());
            } else {
                policy = ContextAwarePolicyAdapter.createUserProvidedPublicationPolicy(pubStr);
            }
        } else if (cmd.hasOption("ver-pub")) {
            if (cmd.hasOption("x")) {
                policy = ContextAwarePolicyAdapter.createPublicationsFilePolicy(c.getPubHandler(), c.getExtender());
            } else {
                policy = ContextAwarePolicyAdapter.createPublicationsFilePolicy(c.getPubHandler());
            }
        } else {
            policy = ContextAwarePolicyAdapter.createDefaultPolicy(c.getPubHandler(), c.getExtender());
        }

    }

    @Override
    public void doTask(KsiContractApi ksicontract, BlockHelper bh, int blockNr, KsiWrapper sig) throws Exception {
        if (sig == null) {
            log.log(Level.INFO, "  Unsigned block! Skipping verification.");
            return;
        }

        KSISignature ksig = sig.getKsi();

        log.log(Level.INFO, "  Verifying:");
        log.log(Level.INFO, "    Sig time     : " + ksig.getAggregationTime());
        if (sig.isExtended()) {
            log.log(Level.INFO, "    Sig extended : " + ksig.getPublicationTime());
        } else {
            log.log(Level.INFO, "    Sig extended : -");
        }
        log.log(Level.INFO, "    Sig in hash  : " + ksig.getInputHash());
        try {
            log.log(Level.INFO, "    Block hash   : " + bh.getRootHash());
        } catch (Exception e) {
            ; // TODO: Handle exception.
        }
        verifyBlock(sig, bh, this.getConf());
    }

    private void compareRecordHashes(KsiWrapper sig, BlockHelper bh) throws Exception {
        DataHash[] sigRecHash = sig.getRecordHash();
        DataHash[] blockRecHash = bh.getRecordHashes();

        int loopSize = sigRecHash.length;
        if (sigRecHash.length != blockRecHash.length) {
            if (sigRecHash.length > blockRecHash.length)
                loopSize = blockRecHash.length;
                log.log(Level.INFO, "Signature and block contains different amount of record hashes!");
        }

        String msg = "[";
        String res = "ok  ";
        for (int i = 0; i < loopSize; i++) {
            if (!sigRecHash[i].equals(blockRecHash[i])) {
                msg += "x";
                res = "fail";
            } else {
                msg += ".";
            }
        }
        msg += "]";
        log.log(Level.INFO, "    Rec hash " + res + ": " + msg);


        return;
    }

    private void verifyBlock(KsiWrapper sig, BlockHelper bh, Conf c) {
        try {
            SignatureVerifier verifier = new SignatureVerifier();
            KSISignature ksig = sig.getKsi();

            /* Compare the record hashes. */
            compareRecordHashes(sig, bh);

            /* Verify KSI signature and the root of the local aggregation. */
            VerificationResult verificationResult = verifier.verify(ksig, bh.getRootHash(), (long) bh.getLevel(),
                    this.policy);

            List<PolicyVerificationResult> rl = verificationResult.getPolicyVerificationResults();
            Iterator<PolicyVerificationResult> rli = rl.iterator();
            while (rli.hasNext()) {
                PolicyVerificationResult r = rli.next();
                VerificationResultCode rps = r.getPolicyStatus();

                if (rps.equals(VerificationResultCode.OK)) {
                    log.log(Level.INFO, "  OK: " + r.getPolicy().getName());
                    return;
                } else if (rps.equals(VerificationResultCode.FAIL)) {
                    log.log(Level.INFO, "X FAILED: " + r.getPolicy().getName());
                    return;
                }
            }
            log.log(Level.INFO, "    Verifying NA!");
        } catch (Exception e) {
            log.log(Level.INFO, "    Failure during verification!");
            log.log(Level.INFO, e.toString());
            log.log(Level.INFO, e.getMessage());
        }
    }
}