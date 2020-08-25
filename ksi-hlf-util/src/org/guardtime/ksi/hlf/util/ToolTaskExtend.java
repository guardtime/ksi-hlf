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