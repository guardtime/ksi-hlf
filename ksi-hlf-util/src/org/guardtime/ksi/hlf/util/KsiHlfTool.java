/*
SPDX-License-Identifier: Apache-2.0
*/

package org.guardtime.ksi.hlf.util;

import java.util.Iterator;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.io.PrintWriter;
import java.lang.System;
import java.io.ByteArrayOutputStream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
// import org.graalvm.compiler.lir.amd64.AMD64Binary.DataThreeOp;
import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.Network;

import com.guardtime.ksi.unisignature.KSISignature;
import com.guardtime.ksi.unisignature.verifier.PolicyVerificationResult;
import com.guardtime.ksi.unisignature.verifier.VerificationResult;
import com.guardtime.ksi.unisignature.verifier.VerificationResultCode;
import com.guardtime.ksi.unisignature.verifier.policies.ContextAwarePolicy;
import com.guardtime.ksi.unisignature.verifier.policies.ContextAwarePolicyAdapter;
import com.guardtime.ksi.Extender;
import com.guardtime.ksi.SignatureVerifier;
import com.guardtime.ksi.Signer;
import com.guardtime.ksi.hashing.DataHash;
import com.guardtime.ksi.hashing.HashAlgorithm;
import com.guardtime.ksi.PublicationsHandler;

import org.guardtime.ksi.hlf.wrapper.KsiWrapper;

public class KsiHlfTool {
  private static Options options;

  private static void printHelp() {
    String name = KsiHlfTool.class.getName();
    System.out.println("Usage:");
    System.out.println("  " + name + " -verify -c <conf> -f <first block> -l <last block>");
    System.out.println("  " + name + " -extend -c <conf> -f <first block> -l <last block>");
    System.out.println("  " + name + " -sign   -c <conf> -f <first block> -l <last block>");
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
    Option oVerify = new Option("verify", false, "Verify blocks.");
    Option oExtend = new Option("extend", false, "Extend blocks.");
    Option oSign = new Option("sign", false, "Resign blocks.");

    Option oConf = new Option("c", "conf", true, "Configuration file.");
    Option oLogCmd = new Option("logcmd", false, "Log to stdout.");
    Option oLog = new Option("log", true, "Log to file.");

    Option oHelp = new Option("h", "help", false, "Displayes this help.");
    Option oFirst = new Option("f", "first", true, "First block to process.");
    Option oLast = new Option("l", "last", true, "Last block to process.");

    oConf.setRequired(true);
    oFirst.setRequired(true);
    oLast.setRequired(true);

    options.addOption(oVerify);
    options.addOption(oExtend);
    options.addOption(oSign);
    options.addOption(oConf);
    options.addOption(oLogCmd);
    options.addOption(oLog);
    options.addOption(oHelp);
    options.addOption(oFirst);
    options.addOption(oLast);

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
    Logger logger = Logger.getLogger("plahh");
    logger.setLevel(Level.FINER);
    logger.setUseParentHandlers(false);

    if (!(cmd.hasOption("verify") || cmd.hasOption("extend") || cmd.hasOption("sign"))) {
      printHelp();
      System.exit(1);
    }

    String cnfFile = cmd.getOptionValue("conf");
    int fb = Integer.parseInt(cmd.getOptionValue("first"));
    int lb = Integer.parseInt(cmd.getOptionValue("last"));

    try {
      String confFile = cnfFile;
      List<Conf> conf = Conf.ConfFromFile(confFile);

      if (cmd.hasOption("logcmd")) {
        ConsoleHandler h = new ConsoleHandler();
        h.setFormatter(new ToolFormatter());
        h.setLevel(Level.FINE);
        logger.addHandler(h);
      } else if (cmd.hasOption("log")) {
        Handler h = new FileHandler(cmd.getOptionValue("log"), 2 ^ 20, 1);
        h.setFormatter(new ToolFormatter());
        logger.addHandler(h);
      }

      // Use every conf to setup a network listener for new blocks commited to
      // the ledger.
      conf.forEach(c -> {
        if (c.isDisabled()) {
          logger.log(Level.INFO, "Conf for organization " + c.getCommitOrg() + " is disabled!");
          return;
        }

        if (cmd.hasOption("sign")) {
          new BlockSignTask(c, logger, fb, lb).run();
        } else if (cmd.hasOption("extend")) {
          new BlockExtendTask(c, logger, fb, lb).run();
        } else if (cmd.hasOption("verify")) {
          new BlockVerifyTask(c, logger, fb, lb).run();
        } else {

        }

      });

    } catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }
}

abstract class ToolTask extends Thread {
  private Conf c;
  protected Logger log;
  private int firstBlock;
  private int lastBlock;

  public ToolTask(Conf c, Logger log, int firstBlock, int lastBlock) {
    this.c = c;
    this.log = log;
    this.firstBlock = firstBlock;
    this.lastBlock = lastBlock;
  }

  abstract public void doTask(Network networ, BlockHelper bh, int blockNr, KsiWrapper sig) throws Exception;

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

      for (int i = this.firstBlock; i < this.lastBlock + 1; i++) {
        log.log(Level.INFO, "Getting block " + i);
        BlockHelper bh = new BlockHelper(network.getChannel().queryBlockByNumber(i), HashAlgorithm.SHA2_256);

        if (bh.isOnlyKsiTransaction()) {
          log.log(Level.INFO, "  Skipping block " + i + " as it contains only KSI signature transaction.");
          continue;
        }
        
        log.log(Level.INFO, "  Getting KSI signature " + i);
        KsiWrapper sig = KsiContractApi.getSignature(network, (long) i, this.getConf().getCommitOrg());
        
        /* Skip not existing signature, if not signing task. */
        if (sig == null && !this.getClass().equals(BlockSignTask.class)) {
          log.log(Level.INFO, "  Skipping block " + i + " as it does not contain KSI signature.");
          // log.log(Level.INFO, "  Class: " + this.getClass());
          // log.log(Level.INFO, "  Class: " + BlockSignTask.class);
          continue;
        }

        doTask(network, bh, i, sig);

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

class BlockSignTask extends ToolTask {
  public BlockSignTask(Conf c, Logger log, int firstBlock, int lastBlock) {
    super(c, log, firstBlock, lastBlock);
  }

  @Override
  public void doTask(Network network, BlockHelper bh, int blockNr, KsiWrapper sig) throws Exception {
    log.log(Level.INFO, "  Signing unsigned block " + blockNr + ":");
    KsiWrapper newSig = signBlock(bh, blockNr);
    KsiContractApi.pushSignature(network, newSig);
    // KsiContractApi.pushSignature(network, (long) blockNr, this.getConf().getCommitOrg(), newSig, bh.getRecordHashes());
  }

  private KsiWrapper signBlock(BlockHelper bh, int blockNr) throws Exception {
    Signer signer = this.getConf().getSigner();
    KSISignature sig = signer.sign(bh.getRootHash(), bh.getLevel());

    ByteArrayOutputStream os = new ByteArrayOutputStream(0xffff);
    DataHash hH = bh.getHeaderHash();
    DataHash mH = bh.getMetadataHash();
    sig.writeTo(os);
    
    String sigBase64 = KsiContractApi.binToBase64(os.toByteArray());
    String headerHaseBase64 = KsiContractApi.binToBase64(hH.getImprint());
    String metaHashBase64 = KsiContractApi.binToBase64(mH.getImprint());
    String inHashBase64 = KsiContractApi.binToBase64(sig.getInputHash().getImprint());

    log.log(Level.INFO, "  sigBase64         :" + sigBase64);
    log.log(Level.INFO, "  headerHaseBase64  :" + headerHaseBase64);
    log.log(Level.INFO, "  metaHashBase64    :" + metaHashBase64);
    log.log(Level.INFO, "  inHashBase64      :" + inHashBase64);

    return KsiWrapper.newFromKSI(sig, bh.getRecordHashes(), blockNr, this.getConf().getCommitOrg());
  }
}


class BlockVerifyTask extends ToolTask {
  public BlockVerifyTask(Conf c, Logger log, int firstBlock, int lastBlock) {
    super(c, log, firstBlock, lastBlock);
  }

  @Override
  public void doTask(Network network, BlockHelper bh, int blockNr, KsiWrapper sig) throws Exception {
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
      if (sigRecHash.length > blockRecHash.length) loopSize = blockRecHash.length;
      System.out.println("Signature and block contains differnet amount of record hashes!");
    }
    
    for (int i = 0; i < loopSize; i++) {
      if (!sigRecHash[i].equals(blockRecHash[i])) {
        System.out.println("Record hash " + i + " differs!");
      } else {
        System.out.println("Record hash " + i + " OK!");
      }
    }

    return;
  }

  private void verifyBlock(KsiWrapper sig, BlockHelper bh, Conf c) {
    try {
      SignatureVerifier verifier = new SignatureVerifier();
      KSISignature ksig = sig.getKsi();
      // ContextAwarePolicy contextAwarePolicy =
      // ContextAwarePolicyAdapter.createKeyPolicy(c.getPubHandler());

      /* Compare the record hashes. */
      compareRecordHashes(sig, bh);

      /* Verify KSI signature and the root of the local aggregation. */
      PublicationsHandler pubHandler = c.getPubHandler();
      ContextAwarePolicy contextAwarePolicy = ContextAwarePolicyAdapter.createDefaultPolicy(pubHandler, c.getExtender());
      VerificationResult verificationResult = verifier.verify(ksig, bh.getRootHash(), (long) bh.getLevel(), contextAwarePolicy);

      
      List<PolicyVerificationResult> rl = verificationResult.getPolicyVerificationResults();
      Iterator<PolicyVerificationResult> rli = rl.iterator();
      while (rli.hasNext()) {
        PolicyVerificationResult r = rli.next();
        VerificationResultCode rps = r.getPolicyStatus();
          
        if (rps.equals(VerificationResultCode.OK)) {
          log.log(Level.INFO, "    Verifying OK: " + r.getPolicy().getName());
          return;
        } else if (rps.equals(VerificationResultCode.FAIL)) {
          log.log(Level.INFO, "    Verifying FAILED: " + r.getPolicy().getName());
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

class BlockExtendTask extends ToolTask {
  public BlockExtendTask(Conf c, Logger log, int firstBlock, int lastBlock) {
    super(c, log, firstBlock, lastBlock);
  }

  @Override
  public void doTask(Network network, BlockHelper bh, int blockNr, KsiWrapper sig) throws Exception {
    KSISignature ksig = sig.getKsi();
    log.log(Level.INFO, "  Extending:");
    log.log(Level.INFO, "    Sig time     : " + ksig.getAggregationTime());
    Extender extender = this.getConf().getExtender();
    KSISignature extksig = extender.extend(ksig);
    log.log(Level.INFO, "    Sig extended : " + extksig.getPublicationTime());

    KsiContractApi.pushExtended(network, KsiWrapper.newFromKSI(extksig, blockNr, this.getConf().getCommitOrg()));
  }
}

class ToolFormatter extends Formatter {
  @Override
  public String format(LogRecord record) {
      return record.getThreadID() + ":" + record.getMessage()+"\n";
  }
}

// // If KSI signature does not exist, null is returned.
// if (!command.equals("sign") && sig == null) {
//   System.out.println("  Skipping block " + i + " as it is unsigned.");
//   continue;
// }

// if (command.equals("verify")) {
//   System.out.println("  Sig time     : " + sig.getAggregationTime());
//   if (sig.isExtended()) {
//     System.out.println("  Sig extended : " + sig.getPublicationTime());
//   } else {
//     System.out.println("  Sig extended : -");
//   }
//   System.out.println("  Sig in hash  : " + sig.getInputHash());
//   System.out.println("  Block hash   : " + bh.getRootHash());
//   verifyBlock(sig, bh, c);
// } else if (command.equals("sign") && sig == null) {
//   System.out.println("  signing unsigned block " + i);
//   Signer s = c.getSigner();
//   KSISignature newSig = s.sign(bh.getRootHash(), bh.getLevel());
//   KsiContractApi.pushSignature(network, (long)i, "gt", newSig);
// } else if (command.equals("extend")) {
//   System.out.println("  Sig time     : " + sig.getAggregationTime());
//   System.out.println("  extending ");
//   Extender extender = c.getExtender();
//   KSISignature extksig = extender.extend(sig);
//   System.out.println("  Sig extended : " + extksig.getPublicationTime());

//   KsiContractApi.pushExtended(network, (long)i, "gt", extksig);
// }