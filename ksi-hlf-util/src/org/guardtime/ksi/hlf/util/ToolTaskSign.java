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
import java.util.logging.Level;
import java.io.ByteArrayOutputStream;
import org.apache.commons.cli.CommandLine;
import com.guardtime.ksi.Signer;
import com.guardtime.ksi.unisignature.KSISignature;
import org.guardtime.ksi.hlf.wrapper.KsiWrapper;
import org.guardtime.ksi.hlf.contract.api.KsiContractApi;
import org.guardtime.ksi.hlf.contract.api.BlockHelper;

class ToolTaskSign extends ToolTask {
    public ToolTaskSign(Conf c, Logger log, CommandLine cmd) {
        super(c, log, cmd);
    }

    @Override
    public void doTask(KsiContractApi ksicontract, BlockHelper bh, int blockNr, KsiWrapper sig) throws Exception {
        log.log(Level.INFO, "  Signing unsigned block " + blockNr + ":");
        KsiWrapper newSig = signBlock(bh, blockNr);
        ksicontract.pushSignature(newSig);
        // KsiContractApi.pushSignature(network, (long) blockNr,
        // this.getConf().getCommitOrg(), newSig, bh.getRecordHashes());
    }

    private KsiWrapper signBlock(BlockHelper bh, int blockNr) throws Exception {
        Signer signer = this.getConf().getSigner();
        KSISignature sig = signer.sign(bh.getRootHash(), bh.getLevel());
        ByteArrayOutputStream os = new ByteArrayOutputStream(0xffff);
        // DataHash hH = bh.getHeaderHash();
        // DataHash mH = bh.getMetadataHash();
        sig.writeTo(os);

        // String sigBase64 = KsiContractApi.binToBase64(os.toByteArray());
        // String headerHaseBase64 = KsiContractApi.binToBase64(hH.getImprint());
        // String metaHashBase64 = KsiContractApi.binToBase64(mH.getImprint());
        // String inHashBase64 =
        // KsiContractApi.binToBase64(sig.getInputHash().getImprint());

        // log.log(Level.INFO, " sigBase64 :" + sigBase64);
        // log.log(Level.INFO, " headerHaseBase64 :" + headerHaseBase64);
        // log.log(Level.INFO, " metaHashBase64 :" + metaHashBase64);
        // log.log(Level.INFO, " inHashBase64 :" + inHashBase64);

        return KsiWrapper.newFromKSI(sig, bh.getRecordHashes(), blockNr, this.getConf().getCommitOrg());
    }
}