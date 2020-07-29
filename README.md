# KSI on Block

This repository contains experimental code for integrating KSI signatures to Hyperledger Fabric (HLF) via chaincode and HLF applications. Repository contains 3 programs:
* org.guardtime.KsiHlf - daemon for listening HLF block commitments.
* org.guardtime.KsiHlfTool - a tool to verify, extend blocks and sign unsigned blocks.
* org.guardtime.KsiContract - HLF chaincode that commits KSI signatures to world state (chaincode name org.guardtime.ksionblock).

# Setup test environment:
For test environment HLF test network is used. At first all the Prerequisites for HLF must be satisfied. Follow directions on following links:

[Prerequisites](https://hyperledger-fabric.readthedocs.io/en/release-1.4/prereqs.html)
[Install samples](https://hyperledger-fabric.readthedocs.io/en/release-1.4/install.html)
(Don't forget to fetch and execute bash script. Just cloning repository is not enough)

Next test network must be started and chaincode must be installed:
```
 # cd to the location of example project.
 # Put fabric samples under go path
 cd <root path>/go/fabric-samples/commercial-paper
 ./network-starter.sh
 
 # See the nodes created.
 docker ps
 
 # To get network log, open another terminal and run
 # ./organization/magnetocorp/configuration/cli/monitordocker.sh net_test
 
 
 ############################
 # organization magnetocorp #
 ############################
 
 # Run command to initalize some env. variables.
 cd organization/magnetocorp
 source magnetocorp.sh
 
 # Copy our example application and chaincode under magnetocorp.
 cp <path to contract-guardtime> .
 cp <path to app-guardtime> .
 
 peer lifecycle chaincode package gt.tar.gz --lang java --path ./contract-guardtime --label gt_0
 peer lifecycle chaincode install gt.tar.gz
 
 # Copy the chaincode ID to environment variable
 # Example: (USE  YOUR PACKAGE ID!)export PACKAGE_ID=gt_0:0a77f9cc052800971961ad47d791b8323c74558d5534af4dc518e03a74e6dd09
 export PACKAGE_ID=
 
 # Approve the chaincode within magnetocorp organization.
 peer lifecycle chaincode approveformyorg --orderer localhost:7050 --ordererTLSHostnameOverride orderer.example.com --channelID mychannel --name ksionblock -v 0 --package-id $PACKAGE_ID --sequence 1 --tls --cafile $ORDERER_CA
 
 ############################
 #   organization digibank  #
 ############################
 
 # Open another terminal.
 cd <root path>/go/fabric-samples/commercial-paper
 cd organization/digibank
 source digibank.sh
 
 # Follow same steps as under magnetocorp
 cp ../magnetocorp/gt.tar.gz .
 peer lifecycle chaincode install gt.tar.gz
 export PACKAGE_ID=
 peer lifecycle chaincode approveformyorg --orderer localhost:7050 --ordererTLSHostnameOverride orderer.example.com --channelID mychannel --name ksionblock -v 0 --package-id $PACKAGE_ID --sequence 1 --tls --cafile $ORDERER_CA
 
 
 #####################################
 ## Commit the chaincode to channel ##
 #####################################
 
 peer lifecycle chaincode commit -o localhost:7050 --ordererTLSHostnameOverride orderer.example.com --peerAddresses localhost:7051 --tlsRootCertFiles ${PEER0_ORG1_CA} --peerAddresses localhost:9051 --tlsRootCertFiles ${PEER0_ORG2_CA} --channelID mychannel --name ksionblock -v 0 --sequence 1 --tls --cafile $ORDERER_CA --waitForEvent
```

Now the chaincode should be installed and redy to use. Try some test commands to check if it really works.
```
 # Test command to push a state to the ledger.
 # Useful when non KSI related update is needed.
 peer chaincode invoke -o localhost:7050 --ordererTLSHostnameOverride orderer.example.com --tls --cafile $ORDERER_CA -C mychannel --name ksionblock --peerAddresses localhost:7051 --tlsRootCertFiles ${PEER0_ORG1_CA} --peerAddresses localhost:9051 --tlsRootCertFiles ${PEER0_ORG2_CA} -c '{"function":"putdummy","Args":["My data!"]}'

 # Export a KSI signature as env. variabel.
 export TESTKSISIG=iAAE5YgBAF0CBFPbpwIDAQ8DBANu/98DAQMFIQERpwCwyAZsR+y6Be03vBTcrbI4VS2Gxlk0LR1+h7h3LQYBAQcjAiEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQGIAQPYAgRT26cCAwEPAwQDbv/fBSEB0fUFNWEdvsdx96L1Qu/GY6atjCUMjo8sJ26pulXSfaEGAQEHIgEBDQMdAwAHMzYtdGVzdAAAAAAAAAAAAAAAAAAAAAAAAAAHJgEBAQIhAeyX7jHMcb3ITZWCpPiMflykQ87Q5VqelxaLC2iASKduByMCIQEAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAcjAiEBAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAHIwIhAQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACCMCIQEAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAcjAiEBAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAHIwIhAQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAByIBAQYDHQMABXRlc3RBAAAAAAAAAAAAAAAAAAAAAAAAAAAAByYBAQcCIQEAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAcjAiEBAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAHIwIhAQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAByMCIQEAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAcjAiEBAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAHIwIhASVwk89CKMWzkzhwkFbzBSc5PwUjvjaWQK1BzdW6d+IkByIBAQcDHQMAAkdUAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACCYBAQcCIQEcolXjqqvxrhe4DIgamBl2q8BTSIl0eXMcpsnYnkEb1QcjAiEBAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAHIwIhAQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAByMCIQEAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAgjAiEBp80P7jSUU9gFeLWEFPQcJiPTchxPN431aw53a3LdFEoHIwIhAQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAByMCIQGSHSy+Twv5NMnDymxKadFTTeZFrih7DcbAU9tJzhXypggjAiEB1G4vssrBl2NPKMosFyGmcIsyfWoPjqwxqO7q6pRyiaMHIwIhAcuhQ6JujLbx9XJjInDf5iadYS0lZPIV6G4TY3lvSuGdiAEApAIEU9unAgMBDwUhAXpVPUrJmQcljhgV7D0VbwfMGwrorLrzyi8CAbzyU1c6BgEBByYBAQ0CIQEAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAcmAQEuAiEBIiZVeEZBFPE+RS29ABOv2V7ZGJ/4jzaP1paZsYzEqq0HIwIhAWQYEq2N7RAkCJoBjm/ADZJ2b5qQHT3rMCaW4T86sH5I

 # Push a KSI signature under organization mycompany.
 peer chaincode invoke -o localhost:7050 --ordererTLSHostnameOverride orderer.example.com --tls --cafile $ORDERER_CA -C mychannel --name ksionblock --peerAddresses localhost:7051 --tlsRootCertFiles ${PEER0_ORG1_CA} --peerAddresses localhost:9051 --tlsRootCertFiles ${PEER0_ORG2_CA} -c '{"function":"setKsi","Args":["00001", "mycompany", "'${TESTKSISIG}'"]}'

 # Get the signature pushed.
 peer chaincode query -C mychannel --name ksionblock  -c '{"function":"getKsi","Args":["00001", "mycompany"]}'
```

To start signing HLF blocks some applications must be built:
```
 # Open another terminal:
 cd <root path>/go/fabric-samples/commercial-paper
 cd organization/magnetocorp/app-guardtime
 mvn package
 
 # Use HLF example tool to create a wallet.
 java -cp target/gt-ksi-on-block-0.0.1-SNAPSHOT.jar org.guardtime.AddToWallet
 
 # Run HLF Block signer with predefined internal conf.
 java -cp target/gt-ksi-on-block-0.0.1-SNAPSHOT.jar org.guardtime.KsiHlf -c testksiconf.yaml -logcmd
 
 
 # Open another terminal under magnetocorp (see above).
 # Run some commands to create new blocks and see what happens
 peer chaincode invoke -o localhost:7050 --ordererTLSHostnameOverride orderer.example.com --tls --cafile $ORDERER_CA -C mychannel --name ksionblock --peerAddresses localhost:7051 --tlsRootCertFiles ${PEER0_ORG1_CA} --peerAddresses localhost:9051 --tlsRootCertFiles ${PEER0_ORG2_CA} -c '{"function":"putdummy","Args":["My data!"]}'
```

After some testing some blocks have been created and signed with KSI (e.g. 10 and 11). Lets use `KsiHlfTool` to verify and extend the blocks created. Note that immediate extending is possible as for testing http://nerf.ee.guardtime.com/cgi-bin/dummy-publications.bin is used. 
```  
 # Open another terminal:
 cd <root path>/go/fabric-samples/commercial-paper
 cd organization/magnetocorp/app-guardtime
 
 java -cp target/gt-ksi-on-block-0.0.1-SNAPSHOT.jar org.guardtime.KsiHlfTool -c testksiconf.yaml -logcmd -f 10 -l 11 -verify
 java -cp target/gt-ksi-on-block-0.0.1-SNAPSHOT.jar org.guardtime.KsiHlfTool -c testksiconf.yaml -logcmd -f 10 -l 11 -extend
 java -cp target/gt-ksi-on-block-0.0.1-SNAPSHOT.jar org.guardtime.KsiHlfTool -c testksiconf.yaml -logcmd -f 10 -l 11 -verify
```
