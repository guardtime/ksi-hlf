package org.guardtime.ksi.hlf.contract.api;

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

/**
 * BlockHelper is a wrapper class for BlockInfo. It provides: 1) computation of
 * block and metadata hash. 2) checking if block contains only KSI transaction.
 * 3) aggregating block info to root hash value with level.
 */
public class BlockHelper {
    private BlockInfo binf;
    private HashAlgorithm aggrHashAlgo;

    private int level;
    private boolean isAggregated;
    private DataHash rootHsh;

    private DataHash headerHash;
    private DataHash metaHash;

    private final HashAlgorithm fabricHardcodedHashAlgo = HashAlgorithm.SHA2_256;

    /**
     * Create new BlockHelper with specified local aggregation hash algorithm. Note
     * that HLF hash algorithm is hardcoded.
     * 
     * @param binf         BlockInfo object.
     * @param aggrHashAlgo Hash Algorithm for local aggregation.
     */
    public BlockHelper(BlockInfo binf, HashAlgorithm aggrHashAlgo) {
        this.binf = binf;
        this.aggrHashAlgo = aggrHashAlgo;
        this.headerHash = null;
        this.metaHash = null;
    }

    public DataHash getMetadataHash() throws KsiContractException {
        if (this.metaHash == null) {
            try {
                Common.Block rawBlock = binf.getBlock();
                BlockMetadata md = rawBlock.getMetadata();
                List<ByteString> dataList = md.getMetadataList();
                DataHasher hsr = new DataHasher(this.fabricHardcodedHashAlgo);

                dataList.forEach(a -> {
                    byte[] bytes = a.toByteArray();
                    hsr.addData(bytes);
                });

                this.metaHash = hsr.getHash();
            } catch (Exception e) {
                throw new KsiContractException("Unable to calculate block metadata hash!", e);
            }
        }

        return this.metaHash;
    }

    public DataHash getHeaderHash() throws KsiContractException {
        /*
         * For some reason there is no getter for the header hash (only previous block
         * hash is available) it has to be calculated.
         */

        if (this.headerHash == null) {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DERSequenceGenerator seq = new DERSequenceGenerator(baos);

                seq.addObject(new ASN1Integer(binf.getBlockNumber()));
                seq.addObject(new DEROctetString(binf.getPreviousHash()));
                seq.addObject(new DEROctetString(binf.getDataHash()));
                seq.close();

                DataHasher hsr = new DataHasher(this.fabricHardcodedHashAlgo);
                hsr.addData(baos.toByteArray());
                this.headerHash = hsr.getHash();
            } catch (Exception e) {
                throw new KsiContractException("Unable to calculate block header hash!", e);
            }
        }

        return this.headerHash;
    }

    public DataHash getPreviousHeaderHash() throws KsiContractException {
        return new DataHash(this.fabricHardcodedHashAlgo, this.binf.getPreviousHash());
    }

    public DataHash getDataHash() throws KsiContractException {
        return new DataHash(this.fabricHardcodedHashAlgo, this.binf.getDataHash());
    }

    public DataHash[] getRecordHashes() throws KsiContractException {
        DataHash[] recHash = { this.getHeaderHash(), this.getMetadataHash() };
        return recHash;
    }

    private void aggregate() throws KsiContractException {
        try {
            DataHash[] rechash = this.getRecordHashes();
            HashTreeBuilder tb = new HashTreeBuilder(this.aggrHashAlgo);
            for (int i = 0; i < rechash.length; i++) {
                tb.add(new ImprintNode(rechash[i]));
            }

            ImprintNode root = tb.build();
            this.rootHsh = new DataHash(root.getValue());
            this.level = (int) root.getLevel();
            this.isAggregated = true;

            return;
        } catch (Exception e) {
            throw new KsiContractException("Unable to perform local aggregation on block!", e);
        }
    }

    public int getLevel() throws KsiContractException {
        if (!this.isAggregated) {
            this.aggregate();
        }
        return this.level;
    }

    public DataHash getRootHash() throws KsiContractException {
        if (!this.isAggregated) {
            this.aggregate();
        }
        return this.rootHsh;
    }

    // TODO: this function is a hardcoded hack!
    public boolean isOnlyKsiTransaction() {
        try {
            String chaincodeName = "ksi.hlf.contract";
            String contractName = "org.guardtime.ksi.hlf.contract";
            String funcName = "setKsi";

            for (int i = 0; i < binf.getBlock().getData().getDataCount(); i++) {
                String data = binf.getBlock().getData().getData(i).toStringUtf8();

                // If it does not contain KSI related substrings, it must be something different
                if (!data.contains(chaincodeName) || !data.contains(contractName + ":" + funcName)) {
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            throw new KsiContractException("Unable to check if block contains only KSI signature transaction!", e);
        }
    }
}