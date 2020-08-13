# KSI HLF Integration

This repository contains code for integrating KSI signatures into Hyperledger Fabric (HLF) via chaincode and HLF applications. KSI signatures are created by a daemon that listens for HLF new block events. After the new block is created it is signed by KSI and resulting signature with additional meta-data is committed to the ledger using the chaincode. This approach leverage KSI technology without changing the HLF source code.


# How to build and install.

Build is managed by maven. Calling `mvn clean package` should resolve all the dependencies and build the project. The build will produce a daemon for issuing KSI signatures (`KsiHlf`), a tool for verifying and extending KSI signatures (`KsiHlfTool`) and HLF smart contract (`KsiHlfContract` with chaincode name `org.guardtime.ksi.hlf.contract`).

Note that `KsiHlfContract` is packaged by the HLF tool `peer lifecycle chaincode package` and is built on the peer node. In spite of that it is required to build entire project before packaging as the build will include some dependencies to the chaincode folder. See rough guidelines below how to build tools and install contract.

```
# Go to project root.
  cd ksi-fab-integration

# Build the tools and prepare contract for packaging.
  mvn clean package

# Package chaincode.
  peer lifecycle chaincode package ksi-hlf-contract1.tar.gz --lang java --path ksi-hlf-contract --label ksi_hlf_1_1
  peer lifecycle chaincode install ksi-hlf-contract1.tar.gz

# Get the package ID returned by install command and put to PACKAGE_ID variable.
# Approve the chaincode.
  peer lifecycle chaincode approveformyorg <orderer info> <channel info> --package-id $PACKAGE_ID --name ksi-hlf-contract -v 1 --sequence 1

# Install the package for all the peers.
# Approve the chaincode within every organization.
  ...

# Commit the chaincode to the channel.
  peer lifecycle chaincode commit <orderer info> <peer info>... <channel info> --name ksi-hlf-contract -v 1 --sequence 1 --waitForEvent

```


# How to test.

The simplest way to start testing `KsiHlf` is to set up HLF test network. See [HLF Getting Started](https://hyperledger-fabric.readthedocs.io/en/release-2.1/getting_started.html) to fulfill the prerequisites, install the sample binaries and run the test network. Follow examples and install `ksi-hlf-contract` using guidelines from example and from above. HLF example applications require a wallet (ID for the user) and connection profile to connect to the channel and invoke the chaincode. Extract the wallet and connection profile for using with `KsiHlf` and `KsiHlfTool`. Both tools use a common yaml configuration file. An example configuration file is:

```
- !!org.guardtime.ksi.hlf.util.Conf
# Specify KSI aggregator.
  aggrUrl: http://example.gateway.com:3333/gt-signingservice
  aggrKey: exampleKey
  aggrUser: exampleUser

# Specify KSI extender.
  extUrl: http://example.gateway.com:8081/gt-extendingservice
  extKey: exampleKey
  extUser: exampleUser

# Specify KSI pulications file.
  pubfileUrl: http://verify.guardtime.com/ksi-publications.bin
  pubfileConstraint: E=publications@guardtime.com
# pubfileCert: additionalCerts.jks

# Specify HLF user and network info.
  network: mychannel
  user: User1@org2.example.com
  walletPath: ./wallet/
  connectionProfile: ./connection-org2.yaml

# Specify organization name used in constructing of the ledger key.
  commitOrg: gt

  disabled: no
```

To test `KsiHlf` and see it signing the HLF blocks call:

```
# Start KsiHlf using testConf.yaml.
# Use HLF example project to invoke some chaincode that changes the ledger.
# That change should generate a new HLF block that is going to be signed.

java -cp ksi-hlf-util/target/ksi-hlf-util-0.0.1.jar org.guardtime.ksi.hlf.util.KsiHlf -c testConf.yaml -logcmd
```

Example above signs the blocks created and displays the number of the bocks processed. These index values can be used by `KsiHlfTool` to verify and extend the KSI signatures issued.

```
# Verify blocks 4, 5 and 6.
java -cp ksi-hlf-util/target/ksi-hlf-util-0.0.1.jar org.guardtime.ksi.hlf.util.KsiHlfTool -c testConf.yaml -logcmd -verify -f 4 -l 6

# Sign unsigned block 4.
java -cp ksi-hlf-util/target/ksi-hlf-util-0.0.1.jar org.guardtime.ksi.hlf.util.KsiHlfTool -c testConf.yaml -logcmd -sign -f 4 -l 4

# Extend blocks 5 and 6 (note that extending can only be performed when a valid publication is published in publications file).
java -cp ksi-hlf-util/target/ksi-hlf-util-0.0.1.jar org.guardtime.ksi.hlf.util.KsiHlfTool -c testConf.yaml -logcmd -sign -f 5 -l 6