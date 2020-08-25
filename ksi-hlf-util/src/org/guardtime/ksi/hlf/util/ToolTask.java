package org.guardtime.ksi.hlf.util;

import java.util.logging.Logger;
import java.util.logging.Level;
import org.apache.commons.cli.CommandLine;
import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.Network;
import org.guardtime.ksi.hlf.wrapper.KsiWrapper;
import org.guardtime.ksi.hlf.contract.api.KsiContractApi;
import org.guardtime.ksi.hlf.contract.api.BlockHelper;

abstract class ToolTask extends Thread {
    private Conf c;
    protected Logger log;
    private int firstBlock;
    private int lastBlock;

    public ToolTask(Conf c, Logger log, CommandLine cmd) {
        this.c = c;
        this.log = log;

        int fb = Integer.parseInt(cmd.getOptionValue("first"));
        int lb = fb;

        if (cmd.hasOption("last")) {
            lb = Integer.parseInt(cmd.getOptionValue("last"));
        }

        this.firstBlock = fb;
        this.lastBlock = lb;
    }

    abstract public void doTask(KsiContractApi ksicontract, BlockHelper bh, int blockNr, KsiWrapper sig)
            throws Exception;

    @Override
    public void run() {
        log.log(Level.FINE, "Starting thread!");

        if (c.isDisabled()) {
            log.log(Level.WARNING, "Unexpected, disabled configuration was used to start the thread!");
            return;
        }

        log.log(Level.INFO, "Read wallet info from: " + c.getWalletPath());
        log.log(Level.INFO, "Using connection profile " + c.getConnectionProfile());
        log.log(Level.INFO, "Using connection profile " + c.getUser());
        try (Gateway gateway = c.getGateway()) {
            // Add handler for kill -9
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    log.log(Level.FINE, "Closing Gateway");
                    gateway.close();
                };
            });

            // Access PaperNet network
            log.log(Level.INFO, "Use network channel: " + c.getNetwork());
            Network network = gateway.getNetwork(c.getNetwork());
            KsiContractApi ksicontract = new KsiContractApi(network);

            for (int i = this.firstBlock; i < this.lastBlock + 1; i++) {
                try {
                    log.log(Level.INFO, "Getting block " + i);
                    BlockHelper bh = ksicontract.getBlock(i);

                    if (bh.isOnlyKsiTransaction()) {
                        log.log(Level.INFO,
                                "  Skipping block " + i + " as it contains only KSI signature transaction.");
                        continue;
                    }

                    log.log(Level.INFO, "  Getting KSI signature " + i);
                    KsiWrapper sig = ksicontract.getSignature((long) i, this.getConf().getCommitOrg());

                    /* Skip not existing signature, if not signing task. */
                    if (sig == null && !this.getClass().equals(ToolTaskSign.class)) {
                        log.log(Level.INFO, "  Skipping block " + i + " as it does not contain KSI signature.");
                        // log.log(Level.INFO, " Class: " + this.getClass());
                        // log.log(Level.INFO, " Class: " + BlockSignTask.class);
                        continue;
                    }

                    doTask(ksicontract, bh, i, sig);

                } catch (Exception e) {
                    log.log(Level.INFO, e.getMessage(), e);
                    // e.printStackTrace();
                }
            }

        } catch (Exception e) {
            log.log(Level.SEVERE, "Failure to setup block listener!", e);
            log.log(Level.SEVERE, e.getMessage());
            e.printStackTrace();
        }
    }

    public Conf getConf() {
        return this.c;
    }
}