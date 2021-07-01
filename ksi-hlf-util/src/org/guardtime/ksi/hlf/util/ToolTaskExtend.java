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
import org.apache.commons.cli.CommandLine;

import com.guardtime.ksi.Extender;
import com.guardtime.ksi.unisignature.KSISignature;

import org.guardtime.ksi.hlf.wrapper.KsiWrapper;
import org.guardtime.ksi.hlf.contract.api.KsiContractApi;
import org.guardtime.ksi.hlf.contract.api.BlockHelper;

class ToolTaskExtend extends ToolTask {
    public ToolTaskExtend(Conf c, Logger log, CommandLine cmd) {
        super(c, log, cmd);
    }

    @Override
    public void doTask(KsiContractApi ksicontract, BlockHelper bh, int blockNr, KsiWrapper sig) throws Exception {
        KSISignature ksig = sig.getKsi();
        log.log(Level.INFO, "  Extending:");
        log.log(Level.INFO, "    Sig time     : " + ksig.getAggregationTime());
        Extender extender = this.getConf().getExtender();
        KSISignature extksig = extender.extend(ksig);
        log.log(Level.INFO, "    Sig extended : " + extksig.getPublicationTime());

        ksicontract.pushExtended(KsiWrapper.newFromKSI(extksig, blockNr, this.getConf().getCommitOrg()));
    }
}