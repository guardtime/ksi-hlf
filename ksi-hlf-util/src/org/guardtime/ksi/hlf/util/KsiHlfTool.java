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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.pattern.LogEvent;

public class KsiHlfTool {
    private static Options options;

    private static void printHelp() {
        String name = KsiHlfTool.class.getName();
        System.out.println("Usage:");
        System.out.println("  " + name + " -verify -c <conf> -f <first block> [-l <last block>]");
        System.out.println("  " + name + " -extend -c <conf> -f <first block> [-l <last block>]");
        System.out.println("  " + name + " -sign   -c <conf> -f <first block> [-l <last block>]");
        System.out.println("");
        System.out.println("Guardtime's KSI Blockchain is an industrial scale blockchain platform that");
        System.out.println("cryptographically ensures data integrity and proves time of existence. The KSI");
        System.out.println("signatures, based on hash chains, link data to this global calendar blockchain.");
        System.out.println("");
        System.out.println("KsiHlfTool enables verifying and extending KSI signatures integrated into");
        System.out.println("Hyperledger Fabric (HLF) by KsiHlf. KsiHlf is a daemon for signing HLF blocks");
        System.out.println("with KSI.");
        System.out.println("");
        System.out.println("It also provides additional functionality to sign unsigned blocks.");
        System.out.println("");
        System.out.println("Options:");

        HelpFormatter formatter = new HelpFormatter();
        PrintWriter pw = new PrintWriter(System.out);
        formatter.printOptions(pw, 80, options, 2, 2);
        pw.flush();
        System.out.println("");
    }

    private static CommandLine defineOptions(String[] args) {
        CommandLineParser parser = new DefaultParser();

        options = new Options();

        OptionGroup command = new OptionGroup()
                .addOption(Option.builder().longOpt("verify").hasArg(false).desc("Verify blocks.").build())
                .addOption(Option.builder().longOpt("extend").hasArg(false).desc("Extend blocks.").build())
                .addOption(Option.builder().longOpt("sign").hasArg(false).desc("Resign blocks.").build());

        OptionGroup policy = new OptionGroup()
                .addOption(Option.builder().longOpt("ver-int").hasArg(false).desc("Perform internal verification.")
                        .build())
                .addOption(Option.builder().longOpt("ver-cal").hasArg(false)
                        .desc("Perform calendar-based verification (use extending service).").build())
                .addOption(Option.builder().longOpt("ver-key").hasArg(false).desc("Perform key-based verification.")
                        .build())
                .addOption(Option.builder().longOpt("ver-pub").hasArg(false)
                        .desc("Perform publication-based verification (use with -x to permit extending).").build());

        Option oPubStr = Option.builder().longOpt("pub-str").hasArg(true).desc("Publication string to verify with.")
                .build();
        Option oPermitExtend = new Option("x", false, "Permit to use extender for publication-based verification.");

        Option oConf = new Option("c", "conf", true, "Configuration file.");
        Option oLogCmd = new Option("logcmd", false, "Log to stdout.");
        Option oLog = new Option("log", true, "Log to file.");

        Option oHelp = new Option("h", "help", false, "Displayes this help.");
        Option oFirst = new Option("f", "first", true, "First block to process.");
        Option oLast = new Option("l", "last", true, "Last block to process.");

        oConf.setRequired(true);
        oFirst.setRequired(true);
        command.setRequired(true);

        options.addOptionGroup(command);
        options.addOption(oConf);
        options.addOption(oLogCmd);
        options.addOption(oLog);
        options.addOption(oHelp);
        options.addOption(oFirst);
        options.addOption(oLast);
        options.addOption(oPubStr);
        options.addOption(oPermitExtend);
        options.addOptionGroup(policy);

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
        Logger logger = Logger.getLogger("ksihlftool");
        logger.setLevel(Level.FINER);
        logger.setUseParentHandlers(false);

        if (!(cmd.hasOption("verify") || cmd.hasOption("extend") || cmd.hasOption("sign"))) {
            printHelp();
            System.exit(1);
        }

        String cnfFile = cmd.getOptionValue("conf");

        try {
            String confFile = cnfFile;
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

            // Use every conf to setup a network listener for new blocks commited to
            // the ledger.
            conf.forEach(c -> {
                Logger subLogger = Logger.getLogger(logger.getName() + "." + c.getCommitOrg());
                if (c.isDisabled()) {
                    subLogger.log(Level.INFO, "Conf for organization " + c.getCommitOrg() + " is disabled!");
                    return;
                }

                try {
                    if (cmd.hasOption("sign")) {
                        new ToolTaskSign(c, subLogger, cmd).run();
                    } else if (cmd.hasOption("extend")) {
                        new ToolTaskExtend(c, subLogger, cmd).run();
                    } else if (cmd.hasOption("verify")) {
                        new ToolTaskVerify(c, subLogger, cmd).run();
                    } else {

                    }
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Unexpected: Unable to init task!", e);
                }

            });

        } catch (Exception e) {
            // e.printStackTrace();
            logger.log(Level.INFO, "Unable to run KsiHlfTool!", e);
            System.exit(-1);
        }
    }
}
