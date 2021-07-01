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

/*
SPDX-License-Identifier: Apache-2.0
*/

package org.guardtime.ksi.hlf.util;

import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.PrintWriter;
import java.lang.System;
import java.util.Arrays;
import org.apache.commons.cli.*;

import org.hyperledger.fabric.sdk.BlockEvent;
import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.Network;

import com.guardtime.ksi.unisignature.KSISignature;
import com.guardtime.ksi.Signer;
import com.guardtime.ksi.hashing.HashAlgorithm;

import org.guardtime.ksi.hlf.wrapper.KsiWrapper;
import org.guardtime.ksi.hlf.contract.api.KsiContractApi;
import org.guardtime.ksi.hlf.contract.api.BlockHelper;

public class KsiHlf {
    private static Options options;

    private static void printHelp() {
        String name = KsiHlfTool.class.getName();

        System.out.println("Usage:");
        System.out.println("  " + name + " -c <conf> [<options>]");

        System.out.println("");
        System.out.println("Guardtime's KSI Blockchain is an industrial scale blockchain platform that");
        System.out.println("cryptographically ensures data integrity and proves time of existence. The KSI");
        System.out.println("signatures, based on hash chains, link data to this global calendar blockchain.");
        System.out.println("");

        HelpFormatter formatter = new HelpFormatter();
        PrintWriter pw = new PrintWriter(System.out);
        formatter.printOptions(pw, 80, options, 2, 2);
        pw.flush();

    }

    private static CommandLine defineOptions(String[] args) {
        CommandLineParser parser = new DefaultParser();

        options = new Options();
        Option oConf = new Option("c", "conf", true, "Configuration file.");
        Option oLogcmd = new Option("logcmd", false, "Log to stdout.");
        Option oLog = new Option("log", true, "Log to file.");
        Option oHelp = new Option("h", "help", false, "Displayes this help.");

        oConf.setRequired(true);

        options.addOption(oConf);
        options.addOption(oLogcmd);
        options.addOption(oLog);
        options.addOption(oHelp);

        try {
            return parser.parse(options, args);
        } catch (ParseException e) {
            printHelp();
            System.out.println("Error during parsing of command line:" + e);
            System.exit(1);
            return null;
        }
    }

    public static void main(String[] args) {
        CommandLine cmd = defineOptions(args);
        Logger logger = Logger.getLogger("ksihlf");
        logger.setLevel(Level.FINER);
        logger.setUseParentHandlers(false);

        if (cmd.hasOption("help")) {
            printHelp();
            System.exit(0);
        }

        try {
            String confFile = cmd.getOptionValue("c");
            List<Conf> conf = Conf.ConfFromFile(confFile);

            if (cmd.hasOption("logcmd")) {
                ConsoleHandler h = new ConsoleHandler();
                h.setFormatter(new FormatterCmd());
                h.setLevel(Level.FINE);
                logger.addHandler(h);
            }

            if (cmd.hasOption("log")) {
                Handler h = new FileHandler(cmd.getOptionValue("log"));
                h.setFormatter(new FormatterFile());
                logger.addHandler(h);
            }

            // Use every conf to setup a network listener for
            // new blocks commited to the ledger.
            conf.forEach(c -> {
                Logger subLogger = Logger.getLogger(logger.getName() + "." + c.getCommitOrg());
                if (c.isDisabled()) {
                    subLogger.log(Level.INFO, "Conf for organization " + c.getCommitOrg() + " is disabled!");
                    return;
                }

                new SignTask(c, subLogger).run();

            });

            while (true)
                ;
        } catch (Exception e) {
            // e.printStackTrace();
            logger.log(Level.INFO, "Unable to run KsiHlf!", e);
            System.exit(-1);
        }

        while (true)
            ;
    }
}

class SignTask extends Thread {
    private Conf c;
    private Logger log;

    public SignTask(Conf c, Logger log) {
        this.c = c;
        this.log = log;
    }

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
                    log.log(Level.INFO, "Closing Gateway");
                    gateway.close();
                };
            });

            // Access PaperNet network

            log.log(Level.INFO, "Use network channel: " + c.getNetwork());
            Network network = gateway.getNetwork(c.getNetwork());

            network.addBlockListener((BlockEvent be) -> {
                /* HF has hardcoded hashig algorithm?? */
                BlockHelper bh = new BlockHelper(be, HashAlgorithm.SHA2_256);
                KsiContractApi ksicontract = new KsiContractApi(network);

                if (bh.isOnlyKsiTransaction()) {
                    log.log(Level.INFO, "Ignoring block " + be.getBlockNumber() + " with only KSI transaction!");
                    return;
                }

                KsiWrapper sig;
                try {
                    log.log(Level.INFO, "Block: " + be.getBlockNumber());
                    log.log(Level.INFO, "  Hash to be signed: " + bh.getRootHash() + "(lvl:" + bh.getLevel() + ")");
                    log.log(Level.FINE, "  RecHash: " + Arrays.toString(bh.getRecordHashes()));

                    // System.out.println("======= DATA =======");
                    // System.out.println(be.getBlock().getData().getData(0).toStringUtf8());
                    // System.out.println("======= DATA =======");

                    sig = signBlock(bh, be.getBlockNumber());
                    log.log(Level.INFO,
                            "  Pushing KSI[" + be.getBlockNumber() + "] " + sig.getKsi().getAggregationTime());
                    ksicontract.pushSignature(sig);
                } catch (Exception e) {
                    log.log(Level.SEVERE, "  Signing failed!", e);
                    log.log(Level.SEVERE, e.getMessage());
                    log.log(Level.SEVERE, e.toString());
                }
            });
            log.log(Level.FINE, "Block Event Listener added.");
            // DO not exit and close the thread.
            while (true)
                ;
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failure to setup block listener!", e);
        }
    }

    private KsiWrapper signBlock(BlockHelper bh, long blockNr) throws Exception {
        try {
            Signer signer = this.c.getSigner();
            KSISignature sig = signer.sign(bh.getRootHash(), bh.getLevel());
            return KsiWrapper.newFromKSI(sig, bh.getRecordHashes(), blockNr, c.getCommitOrg());
        } catch (Exception e) {
            throw e;
        }
    }
}
