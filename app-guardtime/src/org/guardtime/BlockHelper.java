package org.guardtime;

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

    private final HashAlgorithm fabricHardcodedHashAlgo = HashAlgorithm.SHA2_256;
    /**
     * 
     */
    public BlockHelper(BlockInfo binf, HashAlgorithm hashAlgo) {
        this.binf = binf;
        this.hashAlgo = hashAlgo;
    }


    public DataHash getMetadataHash() {
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
    
          return hsr.getHash();
        } catch (Exception e) {
          throw e;
        }
      }
    
    public DataHash getHeaderHash() throws Exception {
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
          return hsr.getHash();
    
        } catch (Exception e) {
          throw e;
        }
    
      }
 
      public DataHash getPreviousHeaderHash() {
        return new DataHash(this.fabricHardcodedHashAlgo, this.binf.getPreviousHash());
      }

      public DataHash getDataHash() {
        return new DataHash(this.fabricHardcodedHashAlgo, this.binf.getDataHash());
      }

      private void aggregate() throws Exception {
          DataHash headerHash = this.getHeaderHash();
          DataHash fabricMetaDataHash = this.getMetadataHash();
          ImprintNode hn = new ImprintNode(headerHash);
          ImprintNode mn = new ImprintNode(fabricMetaDataHash);
          // AggregationHashChainBuilder cb = new AggregationHashChainBuilder();

          HashTreeBuilder tb = new HashTreeBuilder();
          tb.add(hn);
          tb.add(mn);
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
        String chaincodeName = "ksionblock";
        String contractName = "org.guardtime.ksionblock";
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