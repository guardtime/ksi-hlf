package org.guardtime.ksi.hlf.util;

import java.io.ByteArrayOutputStream;
import java.util.List;

import com.google.protobuf.ByteString;
import com.guardtime.ksi.hashing.DataHash;
import com.guardtime.ksi.hashing.DataHasher;
import com.guardtime.ksi.hashing.HashAlgorithm;
import com.guardtime.ksi.tree.HashTreeBuilder;
import com.guardtime.ksi.tree.ImprintNode;

import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequenceGenerator;
import org.hyperledger.fabric.protos.common.Common;
import org.hyperledger.fabric.protos.common.Common.BlockMetadata;
import org.hyperledger.fabric.sdk.BlockInfo;

public class BlockHelper {
    private BlockInfo binf;
    private HashAlgorithm hashAlgo;
    
    private int level;
    private boolean isAggregated;
    private DataHash rootHsh;
    
    private DataHash headerHash;
    private DataHash metaHash;

    private final HashAlgorithm fabricHardcodedHashAlgo = HashAlgorithm.SHA2_256;
    /**
     * 
     */
    public BlockHelper(BlockInfo binf, HashAlgorithm hashAlgo) {
        this.binf = binf;
        this.hashAlgo = hashAlgo;
        this.headerHash = null;
        this.metaHash = null;
    }


    public DataHash getMetadataHash() {
      if (this.metaHash == null) {
        try {
          Common.Block rawBlock = binf.getBlock();
          BlockMetadata md = rawBlock.getMetadata();
          List<ByteString> dataList = md.getMetadataList();
          
          DataHasher hsr = new DataHasher(hashAlgo);
          // MessageDigest hsr = MessageDigest.getInstance(hashAlgo);
    
          dataList.forEach(a -> {
            byte[] bytes = a.toByteArray();
            hsr.addData(bytes);
          });
    
          this.metaHash = hsr.getHash();
        } catch (Exception e) {
          throw e;
        }
      }

    return this.metaHash;
    }
    


    public DataHash getHeaderHash() throws Exception {
      if (this.headerHash == null) {
        try {
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          DERSequenceGenerator seq = new DERSequenceGenerator(baos);
    
          seq.addObject(new ASN1Integer(binf.getBlockNumber()));
          seq.addObject(new DEROctetString(binf.getPreviousHash()));
          seq.addObject(new DEROctetString(binf.getDataHash()));
          seq.close();
    
          DataHasher hsr = new DataHasher(hashAlgo);
          hsr.addData(baos.toByteArray());
          // MessageDigest hsr = MessageDigest.getInstance(hashAlgo);
          // return hsr.digest(baos.toByteArray());
          this.headerHash = hsr.getHash();
        } catch (Exception e) {
          throw e;
        }
      }

    return this.headerHash;
    }
 
      public DataHash getPreviousHeaderHash() {
        return new DataHash(this.fabricHardcodedHashAlgo, this.binf.getPreviousHash());
      }

      public DataHash getDataHash() {
        return new DataHash(this.fabricHardcodedHashAlgo, this.binf.getDataHash());
      }

      public DataHash[] getRecordHashes() throws Exception {
        DataHash[] recHash = {this.getHeaderHash(), this.getMetadataHash()};
        return recHash;
      }

      private void aggregate() throws Exception {
          // DataHash headerHash = this.getHeaderHash();
          // DataHash fabricMetaDataHash = this.getMetadataHash();
          // ImprintNode hn = new ImprintNode(headerHash);
          // ImprintNode mn = new ImprintNode(fabricMetaDataHash);
         
          DataHash[] rechash = this.getRecordHashes();
          HashTreeBuilder tb = new HashTreeBuilder();
          for (int i = 0; i < rechash.length; i++) {
            tb.add(new ImprintNode(rechash[i]));
          }
          // AggregationHashChainBuilder cb = new AggregationHashChainBuilder();

          // tb.add(hn);
          // tb.add(mn);
          ImprintNode root = tb.build();
          this.rootHsh = new DataHash(root.getValue());
          this.level = (int)root.getLevel();
          this.isAggregated = true;
          return;
      }

      public int getLevel() throws Exception {
        if (!this.isAggregated) {
          this.aggregate();
        }
        return this.level;
      }

      public DataHash getRootHash() throws Exception {
        if (!this.isAggregated) {
          this.aggregate();
        }
        return this.rootHsh;
      }

      public boolean isOnlyKsiTransaction() {
        String chaincodeName = "ksi.hlf.contract";
        String contractName = "org.guardtime.ksi.hlf.contract";
        String funcName = "setKsi";
        // String org = "gt";
        // String keyRoot = "blocksig.ksi";
    
        for (int i = 0; i < binf.getBlock().getData().getDataCount(); i++) {
          String data = binf.getBlock().getData().getData(i).toStringUtf8();
    
          // If it does not contain KSI related substrings, it must be something different
          if (!data.contains(chaincodeName) ||
              !data.contains(contractName + ":" + funcName)/* ||
              !data.contains("blocksig.ksi.gt")*/) {
                return false;
              } 
        }
    
        return true;
      }
}