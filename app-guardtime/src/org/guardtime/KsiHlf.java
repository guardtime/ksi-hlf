/*
SPDX-License-Identifier: Apache-2.0
*/

package org.guardtime;

import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.lang.System;
import java.util.Date;

import org.hyperledger.fabric.sdk.BlockEvent;
import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.Network;


import com.guardtime.ksi.unisignature.KSISignature;
import com.guardtime.ksi.Signer;
import com.guardtime.ksi.hashing.HashAlgorithm;

import org.apache.commons.cli.*;

public class KsiHlf {
  private static Options options;

  private static void printHelp() {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp(KsiHlf.class.getName(), options);

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
        h.setFormatter(new MyFormatter());
        h.setLevel(Level.FINE);
        logger.addHandler(h);
      } else if (cmd.hasOption("log")) {
        Handler h = new FileHandler(cmd.getOptionValue("log"), 2^20, 1);
        h.setFormatter(new MyFormatter());
        logger.addHandler(h);
      }

      logger.config(conf.toString());

      // Use every conf to setup a network listener for new blocks commited to
      // the ledger.
      conf.forEach(c -> {
        if (c.isDisabled()) {
          logger.log(Level.INFO, "Conf for organization " + c.getCommitOrg() + " is disabled!");
          return;
        }

        new SignTask(c, logger).run();
        
      });
      
      while(true);
      /* SIIA WHILE? */
    } catch (Exception e) {
        e.printStackTrace();
        System.exit(-1);
    }
  
    while (true);
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
          log.log(Level.FINE, "Closing Gateway");
          gateway.close();
          };
      });


      // Access PaperNet network

      log.log(Level.INFO, "Use network channel: " + c.getNetwork());
      Network network = gateway.getNetwork(c.getNetwork());

      
      network.addBlockListener((BlockEvent be) -> {
                    /* HF has hardcoded hashig algorithm?? */
                    BlockHelper bh = new BlockHelper(be, HashAlgorithm.SHA2_256);

                    if (bh.isOnlyKsiTransaction()) {
                      log.log(Level.INFO, "Ignoring block " + be.getBlockNumber() + " with only KSI transaction!");
                      return;
                    }

                    KSISignature sig;
                    try {
                      log.log(Level.INFO, "Block: " + be.getBlockNumber());
                      log.log(Level.INFO, "  Hash to be signed : " + bh.getRootHash() + "(lvl:" + bh.getLevel() + ")");
                      
                      log.log(Level.FINE, "  Header hash       : " + bh.getHeaderHash());
                      log.log(Level.FINE, "  Prev hash         : " + bh.getPreviousHeaderHash());
                      log.log(Level.FINE, "  Data hash         : " + bh.getDataHash());
                      log.log(Level.FINE, "  Transaction count : " + be.getTransactionCount());
  
                      // System.out.println("======= DATA =======");
                      // System.out.println(be.getBlock().getData().getData(0).toStringUtf8());
                      // System.out.println("======= DATA =======");

                      sig = signBlock(bh);
                      log.log(Level.INFO, "  Pushing KSI[" + be.getBlockNumber() + "]" + sig.getAggregationTime());
                      KsiContract.pushSignature(network, be.getBlockNumber(), c.getCommitOrg(), sig);
                    } catch (Exception e) {
                      log.log(Level.SEVERE, "  Signing failed!", e);
                    }
                  }
        );
        log.log(Level.FINE, "Block Event Listnere added.");
        // DO not exit and close the  thread.
        while (true);
      } catch (Exception e) {
        log.log(Level.SEVERE, "Failure to setup block listener!", e);
      }
  }

  private KSISignature signBlock(BlockHelper bh) throws Exception {
    try {
      Signer signer = this.c.getSigner();
      KSISignature sig = signer.sign(bh.getRootHash(), bh.getLevel());
      return sig;
    } catch (Exception e) {
      throw e;
    }
  } 
}

class MyFormatter extends Formatter {

  // public MyFormatter(String org)

  @Override
  public String format(LogRecord record) {
      return record.getThreadID()+"::"+record.getSourceClassName()+"::"
              +record.getSourceMethodName()+"::"
              +new Date(record.getMillis())+"::"
              +record.getMessage()+"\n";
  }

}
